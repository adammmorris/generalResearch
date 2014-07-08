package ethics.experiments.bimatrix;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.management.RuntimeErrorException;

public class BonusVectorList {
	protected List<double[]> bonusVectors;
	public int numVectors;
	public int numParams;
	
	/**
	 * Generates new bonus vector list.
	 * @param numVectors
	 * @param numParams
	 * @param game
	 * @param playerNum
	 */
	public BonusVectorList(int numVectors, int numParams, FHSingleStageNormalFormGame game, int playerNum) {
		this.numVectors = numVectors;
		this.numParams = numParams;
		this.bonusVectors = new ArrayList<double[]>();
		this.generateBonusVectors(game, playerNum);
	}
	
	/**
	 * Reads bonus vector list from csv file.  Only takes the first numParams columns.
	 * @param path
	 * @param numParams
	 */
	public BonusVectorList(String path, int numParams) {
		this.numParams = numParams;
		this.bonusVectors = new ArrayList<double[]>();
		this.loadBonusVectorsFromCache(path);
	}
	
	/**
	 * Makes a BonusVectorList from a List<double[]>.
	 * @param bonusVectors
	 */
	public BonusVectorList(List<double[]> bonusVectors) {
		this.bonusVectors = bonusVectors;
		this.numParams = bonusVectors.get(0).length;
		this.numVectors = bonusVectors.size();
	}
	
	/**
	 * Makes a BonusVectorList for a single bonus vector.
	 * @param bonusVector
	 */
	public BonusVectorList(double[] bonusVector) {
		this.bonusVectors = new ArrayList<double[]>();
		this.bonusVectors.add(bonusVector);
		this.numParams = bonusVector.length;
		this.numVectors = 1;
	}
	
	protected void loadBonusVectorsFromCache(String pathToCache) {
		try {

			BufferedReader in = new BufferedReader(new FileReader(pathToCache));
			
			String line = in.readLine();
			
			while(line != null){
				
				String[] row = line.split(",");
				String[] strVector = Arrays.copyOfRange(row, 0, this.numParams-1);
				double[] vector = new double[this.numParams];
				for (int i = 0; i < strVector.length; i++) {
					vector[i] = Double.parseDouble(strVector[i]);
				}
				this.bonusVectors.add(vector);
				
				//grab line of next entry
				line = in.readLine();

			}
			in.close();
		}catch(Exception e){
			System.out.println(e);
			throw new RuntimeErrorException(new Error("Parsing cache file failed"));
		}
		
		this.numVectors = this.bonusVectors.size();
	}

	protected void generateBonusVectors(FHSingleStageNormalFormGame game, int playerNum) {
		// 1st vector: all zeros
		double[] zeroVector = new double[]{0.,0.,0.,0.,0.,0.,0.,0.};
		this.bonusVectors.add(zeroVector);
		
		// Next vectors: 0 for everything except Rmax for (s,a)
		for(int i = 0; i < numParams; i++) {
			double[] vector = new double[numParams];
			for (int j = 0; j < numParams; j++) {
				if (i==j) vector[j] = game.getMaxPayout(playerNum, FHSingleStageNormalFormGame.getActionNumberFromSANumber(j));
				else vector[j] = 0;
			}
			
			this.bonusVectors.add(vector);
		}
		
		// Woo! Okay now do the rest randomly
		// Each param will be chosen uniformly at random between -r_max and r_max
		double limit = game.getMaxPayout()+1; // added +1
		for (int i = bonusVectors.size(); i < numVectors; i++) {
			double[] vector = new double[numParams];
			for (int j = 0; j < numParams; j++) {
				vector[j] = Math.random()*(2*limit)-limit;
			}
			
			this.bonusVectors.add(vector);
		}
	}

	public List<double[]> getVectorList() {
		return bonusVectors;
	}
	
	/**
	 * Returns the specified vector.
	 * @param vectorNum Should be the 1-indexed vectorNum (because the function subtracts 1 from vectorNum to get the 0-indexed number).
	 * @return
	 */
	public double[] getVector(int vectorNum) {
		return bonusVectors.get(vectorNum-1);
	}
	
	public void writeVectorList(String path) {
		// Print/write results
		DecimalFormat dec = new DecimalFormat("#.##");
		BufferedWriter out = null;

		try {
			out = new BufferedWriter(new FileWriter(path));
			//System.out.println("Writing results to file..");
			//out.write("Bias1,Bias2,Bias3,Bias4,Bias5,Bias6,Bias7,Bias8");

			for (int i = 0; i < this.numVectors; i++) {
				String toPrint = new String("");
				double[] vector = bonusVectors.get(i);

				for (int j = 0; j < this.numParams; j++) {
					toPrint = toPrint.concat(dec.format((vector[j])));
					if (j != (this.numParams - 1)) toPrint = toPrint.concat(",");
				}

				//System.out.println(toPrint);
				out.write(toPrint);
				out.write("\n");
			}

			//System.out.println("Finished");
			out.close();
		} catch (IOException e) {
			e.printStackTrace();
			System.exit(-1);
		}
	}
}