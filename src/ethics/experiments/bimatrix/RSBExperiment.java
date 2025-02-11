package ethics.experiments.bimatrix;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.history.SGQWActionHistoryFactory;
import burlap.debugtools.DPrint;
import burlap.oomdp.stochasticgames.Agent;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.World;
import ethics.experiments.bimatrix.FixedHistory.FixedHistoryDeterministicAgentFactory;

import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

/**
 * RSBGame (short for repeated symmetric bimatrix game) 
 * NOTE: The "order" of states (for arrays & such) is [(action0,action0),(action0,action1),(action1,action0),(action1,action1)]
 * @author Adam Morris
 *
 */
public class RSBExperiment implements Experiment {
	
	private boolean practice = false;
	
	private static String dirSep = "/"; // set this to \ if on windows..

	protected final int numParams = 8;
	protected boolean runParallel = true;
	protected FHSingleStageNormalFormGame game;
	protected SGDomain domain;
	
	public int numBonusVectors; // this must be at least numParams + 1
	public int numRoundsPerMatch;
	public int numMatchesPerTourn ;
	
	protected boolean printStuff;
	protected final int debugId = 0;
	
	protected double gamma;
	protected double lr;
	protected double epsilon;
	protected double r_max;
	protected double q_max;
	protected double q_init;
	
	protected SGQWActionHistoryFactory qFactory;
	protected FixedHistoryDeterministicAgentFactory all0Factory;
	protected FixedHistoryDeterministicAgentFactory all1Factory;
	
	//protected List<double[]> bonusVectors;

	protected String pathToResultCache;
	
	public static void main(String[] args) {
		//long startTime = System.nanoTime();
		//System.out.println(args[1]);
		if(args.length != 1 && args.length != 2){
			System.out.println("Wrong format. Use:\n\tpathToResultFolder (pathToVectorCache)");
			System.exit(-1);
		}
		
		args[0] = args[0]+dirSep;
		
		// Set up payoff matrix
		// Note: action 0 is cooperate, action 1 is defect
		String[][] actionNames = new String[][]{{FHSingleStageNormalFormGame.ACTION0NAME, FHSingleStageNormalFormGame.ACTION1NAME},{FHSingleStageNormalFormGame.ACTION0NAME, FHSingleStageNormalFormGame.ACTION1NAME}};
		double[][][] payoffMatrix = FHSingleStageNormalFormGame.getChickenPayoff();

		FHSingleStageNormalFormGame game = new FHSingleStageNormalFormGame(actionNames,payoffMatrix);

		RSBExperiment experiment = new RSBExperiment(game);
		
		BonusVectorList bonusVectors;
		// Does the bonus vector list exist already?
		File file = new File(args[0]+"vectors.txt");
		//System.out.println(file.getAbsolutePath());
		if (file.exists()) {
			bonusVectors = new BonusVectorList(args[0]+"vectors.txt",experiment.numParams);
		} else {
			HashMap<Integer,Integer> paramToActionNum = new HashMap<Integer,Integer>();
			paramToActionNum.put(0, 0);
			paramToActionNum.put(1, 0);
			paramToActionNum.put(2, 0);
			paramToActionNum.put(3, 0);
			paramToActionNum.put(4, 1);
			paramToActionNum.put(5, 1);
			paramToActionNum.put(6, 1);
			paramToActionNum.put(7, 1);
			HashMap<Integer,Integer> paramToPlayerNum = new HashMap<Integer,Integer>();
			paramToPlayerNum.put(0, 0);
			paramToPlayerNum.put(1, 0);
			paramToPlayerNum.put(2, 0);
			paramToPlayerNum.put(3, 0);
			paramToPlayerNum.put(4, 0);
			paramToPlayerNum.put(5, 0);
			paramToPlayerNum.put(6, 0);
			paramToPlayerNum.put(7, 0);
			bonusVectors = new BonusVectorList(experiment.numBonusVectors,experiment.numParams,experiment.game,paramToPlayerNum,paramToActionNum);
			bonusVectors.writeVectorList(args[0]+"vectors.txt");
		}
		
		// Are we running on the grid?
		// Should be 5250 games
		if (args.length == 2) {
			int tasknum = Integer.parseInt(args[1]); // 1-indexed
			if (tasknum < 1 || tasknum > 5250) {
				System.out.println("Error: Tasknum is out of range");
				System.exit(-1);
			}
			
			// Are we doing Q vs FHs?
			if (tasknum <= experiment.numBonusVectors*2) {
				double winnings[] = null;
				String title = null;

				if (tasknum <= experiment.numBonusVectors) { // First FH
					winnings = experiment.runTourn_QvsFH(bonusVectors.getVector(tasknum),experiment.all0Factory);
					title = ""+(tasknum)+"vAll0";
				} else if (tasknum <= (experiment.numBonusVectors*2)) { // Second FH
					tasknum = tasknum - 100;
					winnings = experiment.runTourn_QvsFH(bonusVectors.getVector(tasknum),experiment.all1Factory);
					title = ""+(tasknum)+"vAll1";
				}
				
				experiment.writeResults(args[0]+title+".txt", winnings, title);
			} else { // QvsQs
				tasknum = tasknum - 200;
				int start = 0;
				int Q1 = 0;
				int Q2 = 0;
				for (int i = 1; i <= experiment.numBonusVectors; i++) {
					start = (202-i)*(i-1)/2;
					if (start < tasknum && tasknum <= (start+100-i+1)) {
						Q1 = i;
						break;
					}
				}
				Q2 = tasknum - start - 1 + Q1;
				double[][] winnings_tp = transpose(experiment.runTourn_QvsQ(bonusVectors.getVector(Q1),bonusVectors.getVector(Q2)));
				String title1 = ""+Q1+"v"+Q2;
				String title2 = ""+Q2+"v"+Q1;
				
				experiment.writeResults(args[0]+title1+".txt", winnings_tp[0], title1);
				if (Q1!=Q2) experiment.writeResults(args[0]+title2+".txt", winnings_tp[1], title2);
			}
		}
		
		//double[] bonusVector = {100,100,100,100,0,0,0,0};
		//experiment.runTourn_QvsQ(bonusVectors.getVector(8),bonusVectors.getVector(89));
		//System.out.println(System.nanoTime() - experiment.startTime);
		
		/*double[] bonusVector1 = {100,100,100,100,0,0,0,0};
		double[] bonusVector2 = {0,0,0,0,100,100,100,100};
		//experiment.runTourn_QvsFH(bonusVector, experiment.all1Factory);
		experiment.runTourn_QvsQ(bonusVector1, bonusVector2);*/
		
		/*List<double[]> bonusVectorList = new ArrayList<double[]>();
		double[] bonusVector1 = {100,100,100,100,0,0,0,0};
		double[] bonusVector2 = {0,0,0,0,100,100,100,100};
		bonusVectorList.add(bonusVector1);
		bonusVectorList.add(bonusVector2);
		bonusVectors = new BonusVectorList(bonusVectorList);*/
		
		/*List<List<double[]>> winnings = experiment.runQAgainstAllsAndSelf(bonusVectors.getVectorList());
		String[] titles = {"QvsAll0","QvsAll1","QvsSelf"};
		experiment.writeResults(args[0]+"/QAgainstAllsAndSelf.txt",winnings, titles);
		
		List<List<double[]>> winnings2 = experiment.runQAgainstOtherQs(bonusVectors.getVectorList());
		String[] titles2 = new String[numBonusVectors];
		for (int i = 0; i < numBonusVectors; i++) titles2[i] = "Qvs"+i;
		experiment.writeResults(args[0]+"/QAgainstOtherQs.txt",winnings2, titles2);*/
		
		//System.out.println(System.nanoTime() - startTime);
	}
		
	public RSBExperiment(FHSingleStageNormalFormGame game) {
		// Set up domain
		this.game = game;
		this.domain = (SGDomain)game.generateDomain();
			
		// Set parameters
		this.gamma = .99;
		this.lr = .1;
		this.epsilon = .1;
				
		if (this.practice) {
			// PRACTICE NUMBERS
			this.numBonusVectors = 100;
			this.numRoundsPerMatch = 2500;
			this.numMatchesPerTourn = 1;
			
			this.printStuff = true;
		} else {
			// REAL NUMBERS
			this.numBonusVectors = 100;
			this.numRoundsPerMatch = 2500;
			this.numMatchesPerTourn = 5000;
			
			this.printStuff = false;
		}
		
		// Set up printing stuff
		if (!this.printStuff) {
			DPrint.toggleCode(this.debugId, false);
		} else {
			DPrint.toggleCode(this.debugId, true);
		}
		
		this.r_max = this.game.getMaxAbsPayout();
		this.q_max = this.r_max / (1-this.gamma);
		
		this.q_init = (this.r_max+this.q_max)/50;
		
		// Set up Q-factory
		this.qFactory = new SGQWActionHistoryFactory(domain, gamma, lr, new DiscreteStateHashFactory(), 1);
		this.qFactory.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(q_init)); //optional Qinit value to something other than 0
		this.qFactory.setEpsilon(epsilon); //optional epsilon greedy parameter setting

		// Set up fixed-history factories
		int[] policy_all0 = {0,0,0,0};
		int[] policy_all1 = {1,1,1,1};
		all0Factory = this.getFHFactory(policy_all0, 0);
		all1Factory = this.getFHFactory(policy_all1, 1);
	}
	
	/**
	 * 
	 * @param bonusVectors
	 * @return Outer list is rows (vectors), inner list is columns (opponents).
	 */
	protected List<List<double[]>> runQAgainstAllsAndSelf(List<double[]> bonusVectors) {
		List<List<double[]>> winnings = new ArrayList<List<double[]>>();
		
		// Run Q vs. All0
		for (int vectorNum = 0; vectorNum < bonusVectors.size(); vectorNum++) {
			List<double[]> winnings_i = new ArrayList<double[]>();
			winnings_i.add(runTourn_QvsFH(bonusVectors.get(vectorNum),all0Factory));
			winnings_i.add(runTourn_QvsFH(bonusVectors.get(vectorNum),all1Factory));
			winnings_i.add(runTourn_QvsQ(bonusVectors.get(vectorNum),bonusVectors.get(vectorNum))[0]);
			winnings.add(winnings_i);
		}

		return winnings;
	}
	
	/**
	 * 
	 * @param bonusVectors
	 * @return Outer list is rows (agent0 in each game), inner list is columns (the opponent of agent0 in each game), double[] is all the match results
	 */
	protected List<List<double[]>> runQAgainstOtherQs(List<double[]> bonusVectors) {
		List<List<double[]>> winnings = new ArrayList<List<double[]>>();
		
		double[] na = new double[numMatchesPerTourn];
		for (int i = 0; i < numMatchesPerTourn; i++) na[i] = -999;
		
		double[][][] storage = new double[numBonusVectors][numBonusVectors][numMatchesPerTourn];
		
		for (int row = 0; row < bonusVectors.size(); row++) {
			// Start at i+1, because we already have all Qs versus themselves from first step
			for (int col = 0; col < bonusVectors.size(); col++) {
				if (row<col) {
					double[][] results = runTourn_QvsQ(bonusVectors.get(row),bonusVectors.get(col));
					double[][] results_tp = transpose(results);
					storage[row][col] = results_tp[0];
					storage[col][row] = results_tp[1];
				}
				else if (row==col) storage[row][col] = na;
			}
		}
		
		for (int row = 0; row < bonusVectors.size(); row++) {
			List<double[]> winnings_row = new ArrayList<double[]>();
			for (int col = 0; col < bonusVectors.size(); col++) {
				winnings_row.add(storage[row][col]);
			}
			winnings.add(winnings_row);
		}
		return winnings;
	}
	
	protected double[] runTourn_QvsFH(double[] bonusVector, FixedHistoryDeterministicAgentFactory fixedFactory) {

		double[] winnings = new double[numMatchesPerTourn];
		for (int match = 0; match < numMatchesPerTourn; match++) {
			winnings[match] = runMatch_QvsFH(bonusVector,fixedFactory);
		}
			
		return winnings;
	}
	
	protected double[][] runTourn_QvsQ(double[] bonusVector0, double[] bonusVector1) {
		double[][] winnings = new double[numMatchesPerTourn][2];

		if (!runParallel) {
			for (int match = 0; match < numMatchesPerTourn; match++) {
				//System.out.println("Elapsed time (beginning match " + match + "): " + (System.nanoTime() - startTime));
				winnings[match] = runMatch_QvsQ(bonusVector0,bonusVector1);
				//System.out.println("Elapsed time (ending match " + match + "): " + (System.nanoTime() - startTime));
			}
			/*double[][] winnings_tp = transpose(winnings);
			DescriptiveStatistics stats0 = new DescriptiveStatistics(winnings_tp[0]);
			DescriptiveStatistics stats1 = new DescriptiveStatistics(winnings_tp[1]);
			System.out.println("On average, Agent 0 scored: " + stats0.getMean() + " with std dev " + stats0.getStandardDeviation());
			System.out.println("On average, Agent 1 scored: " + stats1.getMean() + " with std dev " + stats1.getStandardDeviation());*/
			return winnings;
		} else {
			RealLoopBody loop = new RealLoopBody(this,bonusVector0,bonusVector1);
			Parallel.withIndex(0, numMatchesPerTourn, loop);
			/*double[][] winnings_tp = transpose(loop.winnings);
			DescriptiveStatistics stats0 = new DescriptiveStatistics(winnings_tp[0]);
			DescriptiveStatistics stats1 = new DescriptiveStatistics(winnings_tp[1]);
			System.out.println("On average, Agent 0 scored: " + stats0.getMean() + " with std dev " + stats0.getStandardDeviation());
			System.out.println("On average, Agent 1 scored: " + stats1.getMean() + " with std dev " + stats1.getStandardDeviation());*/
			return loop.winnings;
		}
	}
	
	protected double runMatch_QvsFH(double[] bonusVector, FixedHistoryDeterministicAgentFactory fixedFactory) {
		// Create the subjective RF construct
		RSBSubjectiveRF subjRF = new RSBSubjectiveRF(game.getJointRewardFunction(),bonusVector);
		
		// Generate our agents
		Agent a0 = qFactory.generateAgent();
		a0.setInternalRewardFunction(subjRF);
		
		Agent a1 = fixedFactory.generateAgent();

		World w = game.createRepeatedGameWorld(domain, a0, a1);
		w.setDebugId(debugId);

		//have our world run for 1000 time steps
		w.runGame(numRoundsPerMatch);
		
		//print final performance (as cumulative reward)
		//System.out.println("Agent 0 scored: " + w.getCumulativeRewardForAgent(a0.getAgentName()));
		//System.out.println("Agent 1 scored: " + w.getCumulativeRewardForAgent(a1.getAgentName()));
		
		//List<QValue> Qs = a1.getQs(w.getCurrentWorldState());
		//for (QValue q : Qs) System.out.println(q.q);
		
		return w.getCumulativeRewardForAgent(a0.getAgentName());
	}
	
	public double[] runMatch_QvsQ(double[] bonusVector0, double[] bonusVector1) {
		// Create the subjective RF construct
		RSBSubjectiveRF subjRF0 = new RSBSubjectiveRF(game.getJointRewardFunction(),bonusVector0);
		RSBSubjectiveRF subjRF1 = new RSBSubjectiveRF(game.getJointRewardFunction(),bonusVector1);
		
		// Generate our agents
		Agent a0 = qFactory.generateAgent();
		a0.setInternalRewardFunction(subjRF0);
		
		Agent a1 = qFactory.generateAgent();
		a1.setInternalRewardFunction(subjRF1);

		World w = game.createRepeatedGameWorld(domain, a0, a1);
		w.setDebugId(debugId);

		//have our world run for 1000 time steps
		w.runGame(numRoundsPerMatch);
		
		//List<JointAction> actions = gameAnalysis.getJointActions();
		/*for (int i = 0; i < actions.size(); i++) {
			System.out.println(gameAnalysis.getState(i).toString());
			System.out.println(gameAnalysis.getJointAction(i).toString());
		}*/
		//for (JointAction action : actions) {
		//	System.out.println(action.toString());
		//}
		
		//print final performance (as cumulative reward)
		//System.out.println("Agent 0 scored: " + w.getCumulativeRewardForAgent(a0.getAgentName()));
		//System.out.println("Agent 1 scored: " + w.getCumulativeRewardForAgent(a1.getAgentName()));
		
		//List<QValue> Qs = a1.getQs(w.getCurrentWorldState());
		//for (QValue q : Qs) System.out.println(q.q);
		
		double[] winnings = {w.getCumulativeRewardForAgent(a0.getAgentName()),w.getCumulativeRewardForAgent(a1.getAgentName())};
		return winnings;
	}
	
	protected FixedHistoryDeterministicAgentFactory getFHFactory(int[] policy, int firstMove) {
		return new FixedHistoryDeterministicAgentFactory(domain,domain.getSingleAction(FHSingleStageNormalFormGame.ACTION0NAME),domain.getSingleAction(FHSingleStageNormalFormGame.ACTION1NAME),policy,domain.getSingleAction(FHSingleStageNormalFormGame.getActionName(firstMove)));
	}
	
	protected void writeResults(String path, List<List<double[]>> winnings, String[] winningTitles) {
		// Print/write results
		//DecimalFormat dec = new DecimalFormat("#.##");
		BufferedWriter out = null;
		File file = null;
		
		try {
			file = new File(path);
			// Don't want to override..
			if (!file.exists()) {
				out = new BufferedWriter(new FileWriter(path));
				System.out.println("Writing results to file..");
				
				// Header
				String toWrite = "vectorNum";
				for (String title : winningTitles) toWrite += "," + title;
				out.write(toWrite + "\n");
	
				for (int row = 0; row < winnings.size(); row++) {
					String toPrint = new String(String.valueOf(row));
					for (int col = 0; col < winnings.get(0).size(); col++) {
						DescriptiveStatistics stats = new DescriptiveStatistics(winnings.get(row).get(col));
						double avg = stats.getMean(); // only recording mean right now
						toPrint += "," + String.valueOf(avg);
					}
	
					//System.out.println(toPrint);
					out.write(toPrint);
					out.write("\n");
				}
	
				System.out.println("Finished");
				out.close();
			} else {
				System.out.println("Output file already exists, did not write results");
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	/**
	 * Writes the result of a single tournament.  This is useful for grid stuff.
	 * @param path
	 * @param winnings
	 * @param winningTitle
	 */
	protected void writeResults(String path, double[] winnings, String winningTitle) {
		// Print/write results
		//DecimalFormat dec = new DecimalFormat("#.##");
		BufferedWriter out = null;
		File file = null;
		
		try {
			file = new File(path);
			// Don't want to override..
			if (!file.exists()) {
				out = new BufferedWriter(new FileWriter(path));
				System.out.println("Writing results of " + winningTitle + " to file..");
				
				// Header
				String toWrite = "vectorNum,"+winningTitle+"\n";
				out.write(toWrite);
	
				DescriptiveStatistics stats = new DescriptiveStatistics(winnings);
				String avg = String.valueOf(stats.getMean()); // only recording mean right now
				out.write(avg);
				System.out.println(avg);
	
				System.out.println("Finished");
				out.close();
			} else {
				System.out.println("Output file already exists, did not write results of " + winningTitle + "");
			}
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
	
	protected static double[][] transpose(double[][] input) {
		int size1 = input.length;
		int size2 = input[0].length;
		double[][] output = new double[size2][size1];
		for (int i = 0; i < size1; i++) {
			for (int j = 0; j < size2; j++) {
				output[j][i] = input[i][j];
			}
		}
		return output;
	}

	@Override
	public int getNumMatchesPerTourn() {
		return numMatchesPerTourn;
	}
}
