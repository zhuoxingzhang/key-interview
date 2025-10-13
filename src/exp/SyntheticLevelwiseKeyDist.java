package exp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.HashSet;
import java.util.concurrent.ThreadLocalRandom;
import java.util.Random;
import java.util.Map;
import java.util.HashMap;

import entity.Key;
import util.Utils;

/**
 * 
 * study impact of key distribution on interview algorithm
 *
 */
public class SyntheticLevelwiseKeyDist {
	/**
	 * max number of minimal keys of schema size m
	 * @param m
	 * @return
	 */
	public static long maxNumMinimalKeys(int m) {
        int k = m / 2; // floor(m/2)
        return combination(m, k);
    }
	// compute C(n, k)
	public static int combination(int n, int k) {
	    if (k < 0 || k > n) return 0;
	    if (k == 0 || k == n) return 1;
	    int res = 1;
	    for (int i = 1; i <= k; i++) {
	        res = res * (n - i + 1) / i;
	    }
	    return res;
	}
	
	/**
     * Generate all possible minimal keys at a specific lattice layer.
     * For a schema of size m, this returns all combinations of attributes 
     * of size 'layerSize'.
     *
     * @param schema     List of attribute names, e.g. ["A1", "A2", "A3", "A4", "A5", "A6"]
     * @param layerSize  The target layer size (number of attributes in each key)
     * @return List<Key> containing all minimal keys at this layer
     */
    public static List<Key> generateKeysOfLayer(List<String> schema, int layerSize) {
        List<Key> result = new ArrayList<>();
        generateCombinations(schema, layerSize, 0, new ArrayList<>(), result);
        return result;
    }

    /**
     * Recursive helper function to generate combinations.
     */
    private static void generateCombinations(List<String> schema, int layerSize, int start,
                                             List<String> current, List<Key> result) {
        if (current.size() == layerSize) {
            result.add(new Key(new ArrayList<>(current)));
            return;
        }
        for (int i = start; i < schema.size(); i++) {
            current.add(schema.get(i));
            generateCombinations(schema, layerSize, i + 1, current, result);
            current.remove(current.size() - 1);
        }
    }
    
	public static void printProgress(int current, int total) {
        int barLength = 50;
        int filledLength = (int) (barLength * current / (double) total);
        String bar = "=".repeat(filledLength) + " ".repeat(barLength - filledLength);
        double percent = current * 100.0 / total;
        System.out.printf("\rProgress: [%s] %.2f%%", bar, percent);
    }
	
    
    public static double getAvgLP(List<Key> keys, int schemaSize) {
    	double sum = 0d;
    	for(Key k : keys) {
    		sum += k.size()/(double)schemaSize;//LP of a key
    	}
    	return sum/keys.size();
    }


	public static void runExp(List<Integer> schemaSizeList) {
    	for(int schemaSize : schemaSizeList) {
    		System.out.println("Current schema size: " + schemaSize);
   
    		int baseRepeat = 1;
    		if(schemaSize <= 11)
    			baseRepeat = 2000;//1000
    		if(schemaSize > 11 && schemaSize <= 13)
    			baseRepeat = 100;//50
    		if(schemaSize > 13 && schemaSize <= 15)
    			baseRepeat = 5;//4
    		
			List<String> R = new ArrayList<>();
			for(int i = 0;i < schemaSize;i ++) {
				R.add("a"+i);
			}
						
			List<Double> TIME_TD_LIST = new ArrayList<>();
			List<Double> TIME_TB_LIST = new ArrayList<>();
			List<Double> TIME_BD_LIST = new ArrayList<>();
			List<Double> TIME_BB_LIST = new ArrayList<>();	
			List<Double> AVG_LP_LIST = new ArrayList<>();//AVG LATTICE POSITION
			List<Double> DENSITY_LIST = new ArrayList<>();//MINIMAL KEYS DENSITY IN LATTICE
			List<Integer> NUM_QUESTION_TD = new ArrayList<>();
			List<Integer> NUM_QUESTION_TB = new ArrayList<>();
			List<Integer> NUM_QUESTION_BD = new ArrayList<>();
			List<Integer> NUM_QUESTION_BB = new ArrayList<>();
			
			long maxNumMinKey = maxNumMinimalKeys(schemaSize);
			
			for(int layerSize = 1; layerSize <= schemaSize; layerSize ++) {
				printProgress(layerSize, schemaSize);
				List<Key> minimalKeys = generateKeysOfLayer(R, layerSize);//all minimal keys in a layer, which number is m chose <layerSize>
				double avgLP = getAvgLP(minimalKeys, schemaSize);
				double density = minimalKeys.size()/(double)maxNumMinKey;
				List<Object> res = null;
				
				long startTD = System.currentTimeMillis();
				int repeatTD = (int)(baseRepeat * layerSize);
				for(int i = 0;i < repeatTD; i ++) {
					res = Interview.interview("topdown", "dfs", R, false, minimalKeys);
				}
				long endTD = System.currentTimeMillis();
				double costTD = (endTD - startTD)/(double)repeatTD;
				TIME_TD_LIST.add(costTD);
				NUM_QUESTION_TD.add((int) res.get(2));//number of questions
				
				
				long startTB = System.currentTimeMillis();
				int repeatTB = (int)(baseRepeat * layerSize);
				for(int i = 0;i < repeatTB; i ++) {
					res = Interview.interview("topdown", "bfs", R, false, minimalKeys);
				}
				long endTB = System.currentTimeMillis();
				double costTB = (endTB - startTB)/(double)repeatTB;
				TIME_TB_LIST.add(costTB);
				NUM_QUESTION_TB.add((int) res.get(2));//number of questions
				
				long startBD = System.currentTimeMillis();
				int repeatBD = (int)(15 * baseRepeat / (double)layerSize);
				for(int i = 0;i < repeatBD; i ++) {
					res = Interview.interview("bottomup", "dfs", R, false, minimalKeys);
				}
				long endBD = System.currentTimeMillis();
				double costBD = (endBD - startBD)/(double)repeatBD;
				TIME_BD_LIST.add(costBD);
				NUM_QUESTION_BD.add((int) res.get(2));//number of questions
				
				long startBB = System.currentTimeMillis();
				int repeatBB = (int)(15 * baseRepeat / (double)layerSize);
				for(int i = 0;i < repeatBB; i ++) {
					res = Interview.interview("bottomup", "bfs", R, false, minimalKeys);
				}
				long endBB = System.currentTimeMillis();
				double costBB = (endBB - startBB)/(double)repeatBB;
				TIME_BB_LIST.add(costBB);
				NUM_QUESTION_BB.add((int) res.get(2));//number of questions
				
				AVG_LP_LIST.add(avgLP);
				DENSITY_LIST.add(density);
			}
			
			
			List<String> result = new ArrayList<>();
			for(int i = 0;i < DENSITY_LIST.size();i ++) {
				String line = "";
				line += AVG_LP_LIST.get(i) + ",";
				line += DENSITY_LIST.get(i) + ",";
				line += TIME_TD_LIST.get(i) + ",";
				line += TIME_TB_LIST.get(i) + ",";
				line += TIME_BD_LIST.get(i) + ",";
				line += TIME_BB_LIST.get(i) + ",";
				line += NUM_QUESTION_TD.get(i) + ",";
				line += NUM_QUESTION_TB.get(i) + ",";
				line += NUM_QUESTION_BD.get(i) + ",";
				line += NUM_QUESTION_BB.get(i);
				result.add(line);
			}
			
			String root = "";
			String outputPath = root + "\\Exp Results New\\syn_key_dist_levelwise_"+schemaSize+".csv";
			Utils.writeContent(result, outputPath, true);
		}
    }
    
	public static void main(String[] args) {
//		int schemaSize = 11;
//		int schemaSize = 13;
		int schemaSize = 15;
		runExp(Arrays.asList(schemaSize));
	}

}
