package ethics.experiments.fssimple;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import optimization.OptVariables;
import burlap.behavior.learningrate.ExponentialDecayLR;
import burlap.behavior.singleagent.ValueFunctionInitialization;
import burlap.behavior.statehashing.DiscreteStateHashFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQFactory;
import burlap.behavior.stochasticgame.agents.naiveq.SGQLAgent;
import burlap.debugtools.DPrint;
import burlap.oomdp.auxiliary.common.NullTermination;
import burlap.oomdp.stochasticgames.AgentFactory;
import burlap.oomdp.stochasticgames.AgentType;
import burlap.oomdp.stochasticgames.JointActionModel;
import burlap.oomdp.stochasticgames.JointReward;
import burlap.oomdp.stochasticgames.SGDomain;
import burlap.oomdp.stochasticgames.SGStateGenerator;
import burlap.oomdp.stochasticgames.common.AgentFactoryWithSubjectiveReward;
import domain.stocasticgames.foragesteal.simple.FSSimple;
import domain.stocasticgames.foragesteal.simple.FSSimpleBTSJAM;
import domain.stocasticgames.foragesteal.simple.FSSimpleJR;
import domain.stocasticgames.foragesteal.simple.FSSimplePOJR;
import ethics.ParameterizedRFFactory;
import ethics.experiments.fssimple.auxiliary.ConsantPsudoTermWorldGenerator;
import ethics.experiments.fssimple.auxiliary.FSSimpleBTSG;
import ethics.experiments.fssimple.auxiliary.FSSubjectiveRFSplit;
import ethics.experiments.fssimple.auxiliary.PseudoGameCountWorld;
import ethics.experiments.fssimple.auxiliary.RNPseudoTerm;
import ethics.experiments.fssimple.specialagents.OpponentOutcomeDBLStealthAgent;
import ethics.experiments.tbforagesteal.TBFSOptimizerExp;
import ethics.experiments.tbforagesteal.auxiliary.RFParamVarEnumerator;

public class FSSMatchCaching {

	protected double								baseLearningRate;
	
	protected List<OptVariables>					rfParamSet;
	protected ConsantPsudoTermWorldGenerator		worldGenerator;
	protected FSSimpleJR							objectiveReward;
	protected ParameterizedRFFactory				rewardFactory;

	protected AgentFactory							baseFactory;
	
	protected AgentType								fsAgentType;
	
	protected int									nTries;
	protected int									nGames;
	protected int 									numVectors;
	
	protected SGDomain 								domain;
	
	
	
	
	protected boolean								replaceResultsWithAvgOnly = true;
		
	/**
	 * @param args Should be: [outputFile, row, learning-rate, probBackTurned, reward-initiator, reward-initiatee, reward-responder, reward-respondee, params-min, params-max, params-step, nParams].
	 * 
	 */
	public static void main(String[] args) {
		long start = System.nanoTime();
		
		/*if(args.length != 2 && args.length != 3 && args.length != 4){
			System.out.println("Wrong format. For full cache use:\n\tpathToCacheOutput learningRate\nFor row cache use:\n\t" +
								"pathToOutputDirectory learningRate cacheMatrixRow\nFor grid cache use:\n\tpathToOutputDirectory learningRate 0 tasknum\n" +
								"For grid compilation use:\n\tpathToOutputDirectory learningRate 0 0");
			System.exit(-1);
		}*/
		
		/*if(args.length != 11){
			System.out.println("Wrong format. For row cache use:\n\tpathToOutputDirectory cacheMatrixRow learningRate probBackTurned numTries" + 
							   "\nFor grid compilation use:\n\tpathToOutputDirectory 0 learningRate probBackTurned numTries");
			System.exit(-1);
		}*/
		
		DPrint.toggleCode(284673923, false); //world printing debug code
		DPrint.toggleCode(25633, false); //tournament printing debug code
		
		String outputFile = args[0];
		int row = Integer.parseInt(args[1])-1; // Why the -1? $SGE_TASK_ID is 1-indexed, but the Java arrays are 0-indexed
		double lr = Double.parseDouble(args[2]);
		double probBackTurned = Double.parseDouble(args[3]);
		double[] rewards = {Double.parseDouble(args[4]),Double.parseDouble(args[5]),Double.parseDouble(args[6]),Double.parseDouble(args[7]),0};
		double[] paramset = {Double.parseDouble(args[8]),Double.parseDouble(args[9]),Double.parseDouble(args[10])};
		int nParams = Integer.parseInt(args[11]);
		FSSMatchCaching mc = new FSSMatchCaching(lr,probBackTurned,rewards,paramset,nParams);
		
		System.out.println("Beginning");
		
		if (row >= 0) mc.cacheRow(outputFile, row);
		else mc.compileGridOutput(outputFile, "Cache.txt");
		
		/*if(args.length == 2){
			mc.cacheAll(outputFile);
		}
		else if(args.length == 3){
			int row = Integer.parseInt(args[2]);
			mc.cacheRow(outputFile, row);
		}
		else if(args.length==4){
			int tasknum = Integer.parseInt(args[3]);
			if (tasknum != 0) mc.cacheGrid(outputFile, tasknum);
			else mc.compileGridOutput(outputFile,"Cache.txt");
		}*/


		System.out.println("Elapsed time: " + (System.nanoTime()-start));
	}
	
	
	public FSSMatchCaching(double learningRate, double probBackTurned, double[] rewards, double[] paramset, int nParams){
		/* PARAMETERS TO SET BEFORE RUNNING */
		
		// Steal-punish values
		/*double[] rewards = {1.0,-1.0,-0.5,-2.5,0};
		double[] paramset = {-10,10,10};
		int nParams = 2;*/
		
		// Share-reciprocate values
		/*double[] rewards = {-.5,1.0,-.5,1.0,0.};
		double[] paramset = {-10,10,10};
		int nParams = 2;*/
		
		this.nTries = 100;
		this.nGames = 1000;

		/* PARAMETERS NOT TO TOUCH */
		
		this.baseLearningRate = learningRate;
		
		this.rfParamSet = (new RFParamVarEnumerator(paramset[0],paramset[1],paramset[2],nParams)).allRFs;

		this.numVectors = this.rfParamSet.size();
		
		this.objectiveReward = new FSSimpleJR(rewards[0],rewards[1],rewards[2],rewards[3],rewards[4]);
		
		this.rewardFactory = new FSSubjectiveRFSplit.FSSubjectiveRFSplitFactory(new FSSimplePOJR(rewards[0],rewards[1],rewards[2],rewards[3],rewards[4]));
				
		FSSimple dgen = new FSSimple(3);
		this.domain = (SGDomain)dgen.generateDomain();
		JointActionModel jam = new FSSimpleBTSJAM(probBackTurned, probBackTurned);
		
		DiscreteStateHashFactory hashingFactory = new DiscreteStateHashFactory();
		
		double discount = 0.95;
		
		this.baseFactory = new SGQFactory(domain, discount, learningRate, 1.5, hashingFactory);
		
		//SGStateGenerator sg = new FSSimpleSG(domain);
		SGStateGenerator sg = new FSSimpleBTSG(domain, probBackTurned);
		
		this.worldGenerator = new ConsantPsudoTermWorldGenerator(domain, jam, objectiveReward, new NullTermination(), sg, new RNPseudoTerm());
		
		this.fsAgentType = new AgentType("player", domain.getObjectClass(FSSimple.CLASSPLAYER), domain.getSingleActions());
		
	}
	
	
	protected void cacheAll(String outFilePath){
		
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(outFilePath));
			
			for(int i = 0; i < this.rfParamSet.size(); i++){
				System.out.println("beginning comparisons for " + i);
				OptVariables v1 = this.rfParamSet.get(i);
				for(int j = i; j < this.rfParamSet.size(); j++){
					OptVariables v2 = this.rfParamSet.get(j);
					String res = this.getMatchResultString(v1, v2);
					out.write(res);
					out.write("\n");
					
				}
				
			}
			
			System.out.println("Finished.");
			
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		
	}
	
	protected void cacheRow(String outputDirectoryPath, int row){
		
		if(!outputDirectoryPath.endsWith("/")){
			outputDirectoryPath = outputDirectoryPath + "/";
		}
		
		String pathName = outputDirectoryPath + row + ".txt";
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(pathName));
			
			System.out.println("beginning row comparisons for " + row);
			OptVariables v1 = this.rfParamSet.get(row);
			for(int j = row; j < this.rfParamSet.size(); j++){
				System.out.println("comparing against " + j);
				OptVariables v2 = this.rfParamSet.get(j);
				String res = this.getMatchResultString(v1, v2);
				out.write(res);
				out.write("\n");
				
			}
				
			
			
			System.out.println("Finished.");
			
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	protected void cacheGrid(String outputDirectoryPath,int tasknum){
		
		if(!outputDirectoryPath.endsWith("/")){
			outputDirectoryPath = outputDirectoryPath + "/";
		}
		
		String res = "";
		String pathName = "";
		
		int numFixedStrats = 0; // Ignoring fixed strats right now
		
		// The max tasknum should be n(n+1)/2 + n*k, where n is numVectors and k is numFixedStrats
		// For numVectors=81 and numFixedStrats = 0, this is 3321
		int maxTasknum = numVectors*(numVectors+1)/2+numVectors*numFixedStrats;
		
		if (tasknum < 1 || tasknum > maxTasknum) {
			System.out.println("Error: Tasknum is out of range");
			System.exit(-1);
		}
		
		// Are we doing Q vs FHs?
		if (tasknum <= numVectors*numFixedStrats) {
			// Do this
		} else { // QvsQs
			tasknum = tasknum - numVectors*numFixedStrats;
			int start = 0;
			int Q1 = 0;
			int Q2 = 0;
			for (int i = 1; i <= numVectors; i++) {
				start = (2*(numVectors+1)-i)*(i-1)/2; // (2(n+1)-i)*(i-1)/2
				if (start < tasknum && tasknum <= (start+numVectors-i+1)) {
					Q1 = i;
					break;
				}
			}
			Q2 = tasknum - start - 1 + Q1; // for reverse engineering: tasknum=Q2-Q1+start+1;
			
			System.out.println("beginning match for" + Q1 + " vs " + Q2);
			OptVariables v1 = this.rfParamSet.get(Q1-1); // rfParamSet is 0-indexed
			OptVariables v2 = this.rfParamSet.get(Q2-1);
			res = this.getMatchResultString(v1, v2);
			
			pathName = outputDirectoryPath + Q1 + "v" + Q2 + ".txt";
		}
		
		BufferedWriter out = null;
		try {
			out = new BufferedWriter(new FileWriter(pathName));
			
			
			out.write(res);
			out.write("\n");
			
			System.out.println("Finished.");
			
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
		if (tasknum==maxTasknum) {
			this.compileGridOutput(outputDirectoryPath, "Cache.txt");
		}
	}
	
	protected void compileGridOutput(String outputDirectoryPath, String cacheName){
		if(!outputDirectoryPath.endsWith("/")){
			outputDirectoryPath = outputDirectoryPath + "/";
		}
		
		String cachePath = outputDirectoryPath + cacheName;
		BufferedWriter out = null;
		BufferedReader in = null;
		
		try {
			out = new BufferedWriter(new FileWriter(cachePath));
			
			System.out.println("beginning compilation...");
			for(int i = 0; i < numVectors; i++){
				System.out.println("beginning compilations for row " + i);
				// Read file & copy, line by line, to cache
				String curPath = outputDirectoryPath+i+".txt";
					
				try {
					in = new BufferedReader(new FileReader(curPath));

					String line = in.readLine();
					while (line != null) {
						out.write(line);
						out.newLine();
						line = in.readLine();
					}
				} catch (IOException e) {
					e.printStackTrace();
					System.exit(-1);
				}
			}
				
			
			
			System.out.println("Finished.");
			
			out.close();
			
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
		
	}
	
	protected String getMatchResultString(OptVariables v1, OptVariables v2){
		
		MatchResult mr = this.getAverageMatch(v1, v2);
		
		StringBuffer buf = new StringBuffer();
		
		//format:
		//v11,v12,v13;v21,v22,v23::avgV1,stdV1
		//return11,return12,...,return1N
		//v21,v22,v23;v11,v12,v13::avgV2,stdV2
		//return21,return22,...,return2N
		
		buf.append(this.commaDelimString(v1)).append(";").append(this.commaDelimString(v2)).append("::").append(mr.avgA).append(",").append(mr.stdA).append("\n");
		if(!this.replaceResultsWithAvgOnly){
			for(int i = 0; i < mr.results.size(); i++){
				if(i > 0){
					buf.append(",");
				}
				buf.append(mr.results.get(i).a);
			}
		}
		else{
			buf.append(mr.avgA);
		}
		buf.append("\n");
		
		buf.append(this.commaDelimString(v2)).append(";").append(this.commaDelimString(v1)).append("::").append(mr.avgB).append(",").append(mr.stdB).append("\n");
		if(!this.replaceResultsWithAvgOnly){
			for(int i = 0; i < mr.results.size(); i++){
				if(i > 0){
					buf.append(",");
				}
				buf.append(mr.results.get(i).b);
			}
		}
		else{
			buf.append(mr.avgB);
		}
		
		
		return buf.toString();
	}
	
	protected MatchResult getAverageMatch(OptVariables v1, OptVariables v2){
		
		List <DoublePair> results = new ArrayList<DoublePair>(nTries);
		for(int i = 0; i < nTries; i++){
			results.add(runMatch(v1, v2));
		}
		
		return new MatchResult(results);
		
	}
	
	
	protected DoublePair runMatch(OptVariables v1, OptVariables v2){
		
		return this.runMatchLearning(v1, v2);
		//return this.runMatchHardCoded(v1, v2);
		
	}
	
	protected DoublePair runMatchLearning(OptVariables v1, OptVariables v2){
		JointReward subjectiveRewardV1 = this.rewardFactory.generateRF(v1.vars);
		AgentFactory factV1 = new AgentFactoryWithSubjectiveReward(baseFactory, subjectiveRewardV1);
		//FSRQInit v1QInit = new FSRQInit(this.objectiveReward, (FSSubjectiveRF)subjectiveRewardV1);
		
		JointReward subjectiveRewardV2 = this.rewardFactory.generateRF(v2.vars);
		AgentFactory factV2 = new AgentFactoryWithSubjectiveReward(baseFactory, subjectiveRewardV2);
		//FSRQInit v2QInit = new FSRQInit(this.objectiveReward, (FSSubjectiveRF)subjectiveRewardV2);
		//FSRPunisherQInit v2QInit = new FSRPunisherQInit((FSSubjectiveRF)subjectiveRewardV2, (FSSimpleJR)this.objectiveReward);
		
		
		
		//role 1
		
		SGQLAgent a1 = (SGQLAgent)factV1.generateAgent();
		//a1.setQValueInitializer(v1QInit);
		a1.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.));
		a1.setLearningRate(new ExponentialDecayLR(this.baseLearningRate, 0.999, 0.01));
		
		SGQLAgent a2 = (SGQLAgent)factV2.generateAgent();
		//a2.setQValueInitializer(v2QInit);
		a2.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(-6.5));
		a2.setLearningRate(new ExponentialDecayLR(this.baseLearningRate, 0.999, 0.01));
		
		PseudoGameCountWorld w1 = (PseudoGameCountWorld)this.worldGenerator.generateWorld();
		a1.joinWorld(w1, this.fsAgentType);
		a2.joinWorld(w1, this.fsAgentType);
		
		w1.runGame(Integer.MAX_VALUE, nGames);
		
		double a1r1 = w1.getCumulativeRewardForAgent(a1.getAgentName());
		double a2r1 = w1.getCumulativeRewardForAgent(a2.getAgentName());
		
		//role 2
		
		//FSRPunisherQInit v12QInit = new FSRPunisherQInit((FSSubjectiveRF)subjectiveRewardV1, (FSSimpleJR)this.objectiveReward);
		//FSRQInit v12QInit = new FSRQInit(this.objectiveReward, (FSSubjectiveRF)subjectiveRewardV1);
		//FSRQInit v22QInit = new FSRQInit(this.objectiveReward, (FSSubjectiveRF)subjectiveRewardV2);
		
		SGQLAgent a12 = (SGQLAgent)factV1.generateAgent();
		//a12.setQValueInitializer(v12QInit);
		a12.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(-6.5));
		a12.setLearningRate(new ExponentialDecayLR(this.baseLearningRate, 0.999, 0.01));
		
		SGQLAgent a22 = (SGQLAgent)factV2.generateAgent();
		//a22.setQValueInitializer(v22QInit);
		a22.setQValueInitializer(new ValueFunctionInitialization.ConstantValueFunctionInitialization(0.));
		a22.setLearningRate(new ExponentialDecayLR(this.baseLearningRate, 0.999, 0.01));
		
		PseudoGameCountWorld w2 = (PseudoGameCountWorld)this.worldGenerator.generateWorld();
		a22.joinWorld(w2, this.fsAgentType); //switch join order
		a12.joinWorld(w2, this.fsAgentType);
		
		w2.runGame(Integer.MAX_VALUE, nGames);
		
		double a1r2 = w2.getCumulativeRewardForAgent(a12.getAgentName());
		double a2r2 = w2.getCumulativeRewardForAgent(a22.getAgentName());
		
		double a1r = a1r1 + a1r2;
		double a2r = a2r1 + a2r2;
		
		DoublePair res = new DoublePair(a1r, a2r);
		
		if(Double.isNaN(a1r) || Double.isNaN(a2r)){
			throw new RuntimeException("NaN Return.");
		}
		
		return res;
	}
	
	
	protected DoublePair runMatchHardCoded(OptVariables v1, OptVariables v2){
		
		int [] hp1 = this.doubleToIntArray(v1.vars);
		int [] hp2 = this.doubleToIntArray(v2.vars);
		
		//OpponentOutcomeAgent a1 = new OpponentOutcomeAgent(this.domain, hp1);
		//OpponentOutcomeAgent a2 = new OpponentOutcomeAgent(this.domain, hp2);
		OpponentOutcomeDBLStealthAgent a1 = new OpponentOutcomeDBLStealthAgent(this.domain, hp1);
		OpponentOutcomeDBLStealthAgent a2 = new OpponentOutcomeDBLStealthAgent(this.domain, hp2);
		
		PseudoGameCountWorld w1 = (PseudoGameCountWorld)this.worldGenerator.generateWorld();
		a1.joinWorld(w1, this.fsAgentType);
		a2.joinWorld(w1, this.fsAgentType);
		
		w1.runGame(Integer.MAX_VALUE, nGames);
		
		double a1r1 = w1.getCumulativeRewardForAgent(a1.getAgentName());
		double a2r1 = w1.getCumulativeRewardForAgent(a2.getAgentName());
		
		
		//OpponentOutcomeAgent a12 = new OpponentOutcomeAgent(this.domain, hp1);
		//OpponentOutcomeAgent a22 = new OpponentOutcomeAgent(this.domain, hp2);
		OpponentOutcomeDBLStealthAgent a12 = new OpponentOutcomeDBLStealthAgent(this.domain, hp1);
		OpponentOutcomeDBLStealthAgent a22 = new OpponentOutcomeDBLStealthAgent(this.domain, hp2);
		
		PseudoGameCountWorld w2 = (PseudoGameCountWorld)this.worldGenerator.generateWorld();
		a22.joinWorld(w2, this.fsAgentType); //switch join order
		a12.joinWorld(w2, this.fsAgentType);
		
		w2.runGame(Integer.MAX_VALUE, nGames);
		
		double a1r2 = w2.getCumulativeRewardForAgent(a12.getAgentName());
		double a2r2 = w2.getCumulativeRewardForAgent(a22.getAgentName());
		
		double a1r = a1r1 + a1r2;
		double a2r = a2r1 + a2r2;
		
		DoublePair res = new DoublePair(a1r, a2r);
		
		
		return res;
		
	}
	
	protected int [] doubleToIntArray(double [] vars){
		int [] ia = new int[vars.length];
		for(int i = 0; i < vars.length; i++){
			ia[i] = (int)vars[i];
		}
		return ia;
	}
	
	
	protected String commaDelimString(OptVariables v){
		StringBuffer buf = new StringBuffer();
		for(int i = 0; i < v.vars.length; i++){
			if(i > 0){
				buf.append(",");
			}
			buf.append(v.vars[i]);
		}
		return buf.toString();
	}
	
	
	
	class MatchResult{
		
		public List <DoublePair> results;
		
		public double avgA;
		public double avgB;
		
		public double stdA;
		public double stdB;
		
		
		public MatchResult(List <DoublePair> results){
			this.results = results;
			
			
			double sumA = 0.;
			double sumB = 0.;
			for(DoublePair dp : results){
				sumA += dp.a;
				sumB += dp.b;
			}
			
			this.avgA = sumA / results.size();
			this.avgB = sumB / results.size();
			
			double sumVA = 0.;
			double sumVB = 0.;
			for(DoublePair dp : results){
				
				double diffA = dp.a - this.avgA;
				double diffB = dp.b - this.avgB;
				
				sumVA += diffA*diffA;
				sumVB += diffB*diffB;
				
			}
			
			this.stdA = Math.sqrt(sumVA / results.size());
			this.stdB = Math.sqrt(sumVB / results.size());
			
		}
		
		
	}
	
	
	class DoublePair{
		
		public double a;
		public double b;
		
		public DoublePair(double a, double b){
			this.a = a;
			this.b = b;
		}
		
		public DoublePair reverse(){
			return new DoublePair(this.b, this.a);
		}
		
	}
	

}
