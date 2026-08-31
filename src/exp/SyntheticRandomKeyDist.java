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
public class SyntheticRandomKeyDist {
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
	 * random number of minimal keys randomly distribute in random layers
	 * @param R
	 * @return
	 */
    public static List<Key> generateRandomMinimalKeys(List<String> R) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = R.size();

        int m = rnd.nextInt(1, n + 1);
        Set<Integer> chosenLevels = new HashSet<>();
        while (chosenLevels.size() < m) {
            chosenLevels.add(rnd.nextInt(1, n + 1));
        }
        List<Integer> levels = new ArrayList<>(chosenLevels);
        Collections.shuffle(levels);

        List<Key> minimalKeys = new ArrayList<>();

        for (int level : levels) {
            int numKeys = rnd.nextInt(1, combination(n, level) + 1);//1 - C(n, level)

            int attempts = 0;
            while (numKeys > 0 && attempts < 5000) {
                attempts++;

                List<String> shuffled = new ArrayList<>(R);
                Collections.shuffle(shuffled, rnd);
                Set<String> candidate = new HashSet<>(shuffled.subList(0, level));

                boolean valid = true;
                for (Key key : minimalKeys) {
                    if (isSubset(key.getAttributes(), candidate) || isSubset(candidate, key.getAttributes())) {
                        valid = false;
                        break;
                    }
                }

                if (valid) {
                    minimalKeys.add(new Key(candidate));
                    numKeys--;
                }
            }
        }

//        System.out.println("Selected layers: " + levels);
        return minimalKeys;
    }
    
    /**
     * Generate random minimal keys within a specific lattice region.
     * 
     * @param R        schema attributes
     * @param region   region of lattice: "bottom", "middle", or "top"
     * @return         list of randomly generated minimal keys
     */
    public static List<Key> generateRandomMinimalKeys(List<String> R, String region) {
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        int n = R.size();

        // Step 1: divide lattice into 3 regions
        int firstEnd = Math.max(1, n / 3);
        int secondEnd = Math.max(firstEnd + 1, (2 * n) / 3);

        // Step 2: determine the range based on the given region
        int startLayer, endLayer;
        switch (region.toLowerCase()) {
            case "bottom":
                startLayer = 0;
                endLayer = firstEnd;
                break;
            case "middle":
                startLayer = firstEnd + 1;
                endLayer = secondEnd;
                break;
            case "top":
                startLayer = secondEnd + 1;
                endLayer = n;
                break;
            default:
                throw new IllegalArgumentException("Invalid region: " + region +
                        ". Expected 'bottom', 'middle', or 'top'.");
        }

        // Step 3: randomly pick some layers within the chosen region
        int numLayers = rnd.nextInt(1, Math.max(2, endLayer - startLayer + 1)); // 1 ~ region size
        Set<Integer> chosenLevels = new HashSet<>();
        while (chosenLevels.size() < numLayers) {
            chosenLevels.add(rnd.nextInt(startLayer, endLayer + 1));
        }
        List<Integer> levels = new ArrayList<>(chosenLevels);
        Collections.shuffle(levels, rnd);

        List<Key> minimalKeys = new ArrayList<>();

        // Step 4: For each chosen level, generate random minimal keys
        for (int level : levels) {
            int maxPossible = combination(n, level);
            int numKeys = rnd.nextInt(1, maxPossible + 1); //

            int attempts = 0;
            while (numKeys > 0 && attempts < 5000) {
                attempts++;

                List<String> shuffled = new ArrayList<>(R);
                Collections.shuffle(shuffled, rnd);
                Set<String> candidate = new HashSet<>(shuffled.subList(0, level));

                boolean valid = true;
                for (Key key : minimalKeys) {
                    if (isSubset(key.getAttributes(), candidate) || isSubset(candidate, key.getAttributes())) {
                        valid = false;
                        break;
                    }
                }

                if (valid) {
                    minimalKeys.add(new Key(candidate));
                    numKeys--;
                }
            }
        }

        return minimalKeys;
    }


    private static boolean isSubset(Collection<String> a, Collection<String> b) {
        return b.containsAll(a);
    }
    
	public static void printProgress(int current, int total) {
        int barLength = 50;
        int filledLength = (int) (barLength * current / (double) total);
        String bar = "=".repeat(filledLength) + " ".repeat(barLength - filledLength);
        double percent = current * 100.0 / total;
        System.out.printf("\rProgress: [%s] %.2f%%", bar, percent);
    }
	
    public static List<Key> generateMinimalKeysPool(List<String> schema, int keySize) {
        if (schema == null || schema.isEmpty() || keySize <= 0 || keySize > schema.size()) {
            throw new IllegalArgumentException("Invalid schema or key size");
        }

        List<Key> allKeys = new ArrayList<>();
        combine(schema, keySize, 0, new LinkedHashSet<>(), allKeys);

        return allKeys;
    }
    
    public static Set<Integer> randomSubset(int level) {
        if (level < 1) throw new IllegalArgumentException("level must be >= 1");
        ThreadLocalRandom rnd = ThreadLocalRandom.current();
        while (true) {
            Set<Integer> set = new HashSet<>();
            for (int i = 1; i <= level; i++) {
                if (rnd.nextBoolean()) { // 50% selected
                    set.add(i);
                }
            }
            if (!set.isEmpty()) return set;
        }
    }
    
    

    private static void combine(List<String> schema, int keySize, int start, Set<String> current, List<Key> result) {
        if (current.size() == keySize) {
            result.add(new Key(new LinkedHashSet<>(current)));
            return;
        }
        for (int i = start; i < schema.size(); i++) {
            current.add(schema.get(i));
            combine(schema, keySize, i + 1, current, result);
            current.remove(schema.get(i));
        }
    }
    
    public static List<Key> selectKeysByRandom(List<Key> minimalKeyPool) {
    	if (minimalKeyPool == null || minimalKeyPool.isEmpty()) {
            return new ArrayList<>();
        }

        Random random = new Random();

        List<Key> shuffled = new ArrayList<>(minimalKeyPool);
        Collections.shuffle(shuffled, random);

        int n = shuffled.size();
        int k = random.nextInt(n) + 1; // [1, n]

        return new ArrayList<>(shuffled.subList(0, k));
    }
    
    public static double getAvgLP(List<Key> keys, int schemaSize) {
    	double sum = 0d;
    	for(Key k : keys) {
    		sum += k.size()/(double)schemaSize;//LP of a key
    	}
    	return sum/keys.size();
    }
    
    public static int getKeySizeRange(List<Key> keys) {
        if (keys == null || keys.isEmpty()) {
            return 0;
        }

        int min = Integer.MAX_VALUE;
        int max = Integer.MIN_VALUE;

        for (Key k : keys) {
            int size = k.size();
            if (size < min) min = size;
            if (size > max) max = size;
        }

        return max - min;
    }


	public static void runExp(List<Integer> schemaSizeList, String region) {
    	for(int schemaSize : schemaSizeList) {
    		System.out.println("Current schema size: " + schemaSize);
    		int outerRepeat = 200;//diversity number
    		
    		int innerRepeat = 1;//if no need time measure, set 1 only
//    		if(schemaSize <= 11)
//    			innerRepeat = 2000;//1000
//    		if(schemaSize > 11 && schemaSize <= 13)
//    			innerRepeat = 100;//50
//    		if(schemaSize > 13 && schemaSize <= 15)
//    			innerRepeat = 5;//4
    		
			List<String> R = new ArrayList<>();
			for(int i = 0;i < schemaSize;i ++) {
				R.add("a"+i);
			}
						
			List<Double> TIME_TD_LIST = new ArrayList<>();
			List<Double> TIME_TB_LIST = new ArrayList<>();
			List<Double> TIME_BD_LIST = new ArrayList<>();
			List<Double> TIME_BB_LIST = new ArrayList<>();
			List<Double> TIME_DA_LIST = new ArrayList<>();
			List<Integer> SIZE_RANGE_LIST = new ArrayList<>();
			List<Double> AVG_LP_LIST = new ArrayList<>();//AVG LATTICE POSITION
			List<Double> DENSITY_LIST = new ArrayList<>();//MINIMAL KEYS DENSITY IN LATTICE
			List<Integer> NUM_QUESTION_TD = new ArrayList<>();
			List<Integer> NUM_QUESTION_TB = new ArrayList<>();
			List<Integer> NUM_QUESTION_BD = new ArrayList<>();
			List<Integer> NUM_QUESTION_BB = new ArrayList<>();
			List<Integer> NUM_QUESTION_DA = new ArrayList<>();
			
			long maxNumMinKey = maxNumMinimalKeys(schemaSize);
			
			for(int repeat = 1; repeat <= outerRepeat; repeat ++) {
				printProgress(repeat, outerRepeat);
				List<Key> minimalKeys = null;
				if(region.equals("all")) {
					minimalKeys = generateRandomMinimalKeys(R);//random in all layers;maxNumPerLayer=1-C(n,level); maxAttemptNum=5000
				}else {
					minimalKeys = generateRandomMinimalKeys(R, region);//bottom/middle/top regions;maxNumPerLayer=1-C(n,level); maxAttemptNum=5000
				}
				
				int sizeRange = getKeySizeRange(minimalKeys);
				double avgLP = getAvgLP(minimalKeys, schemaSize);
				double density = minimalKeys.size()/(double)maxNumMinKey;
				List<Object> res = null;
				
				long startTD = System.currentTimeMillis();
				for(int i = 0;i < innerRepeat; i ++) {
					res = Interview.interview("topdown", "dfs", R, false, minimalKeys);
				}
				long endTD = System.currentTimeMillis();
				double costTD = (endTD - startTD)/(double)innerRepeat;
				TIME_TD_LIST.add(costTD);
				NUM_QUESTION_TD.add((int) res.get(2));//number of questions
				
				
				long startTB = System.currentTimeMillis();
				for(int i = 0;i < innerRepeat; i ++) {
					res = Interview.interview("topdown", "bfs", R, false, minimalKeys);
				}
				long endTB = System.currentTimeMillis();
				double costTB = (endTB - startTB)/(double)innerRepeat;
				TIME_TB_LIST.add(costTB);
				NUM_QUESTION_TB.add((int) res.get(2));//number of questions
				
				long startBD = System.currentTimeMillis();
				for(int i = 0;i < innerRepeat; i ++) {
					res = Interview.interview("bottomup", "dfs", R, false, minimalKeys);
				}
				long endBD = System.currentTimeMillis();
				double costBD = (endBD - startBD)/(double)innerRepeat;
				TIME_BD_LIST.add(costBD);
				NUM_QUESTION_BD.add((int) res.get(2));//number of questions
				
				long startBB = System.currentTimeMillis();
				for(int i = 0;i < innerRepeat; i ++) {
					res = Interview.interview("bottomup", "bfs", R, false, minimalKeys);
				}
				long endBB = System.currentTimeMillis();
				double costBB = (endBB - startBB)/(double)innerRepeat;
				TIME_BB_LIST.add(costBB);
				NUM_QUESTION_BB.add((int) res.get(2));//number of questions

				long startDA = System.currentTimeMillis();
				for(int i = 0;i < innerRepeat; i ++) {
					res = Interview.interviewDualize(R, false, minimalKeys);
				}
				long endDA = System.currentTimeMillis();
				double costDA = (endDA - startDA)/(double)innerRepeat;
				TIME_DA_LIST.add(costDA);
				NUM_QUESTION_DA.add((int) res.get(2));//number of questions

				SIZE_RANGE_LIST.add(sizeRange);
				AVG_LP_LIST.add(avgLP);
				DENSITY_LIST.add(density);
			}
			List<String> result = new ArrayList<>();
			for(int i = 0;i < DENSITY_LIST.size();i ++) {
				String line = "";
				line += SIZE_RANGE_LIST.get(i) + ",";
				line += AVG_LP_LIST.get(i) + ",";
				line += DENSITY_LIST.get(i) + ",";
				line += TIME_TD_LIST.get(i) + ",";
				line += TIME_TB_LIST.get(i) + ",";
				line += TIME_BD_LIST.get(i) + ",";
				line += TIME_BB_LIST.get(i) + ",";
				line += TIME_DA_LIST.get(i) + ",";
				line += NUM_QUESTION_TD.get(i) + ",";
				line += NUM_QUESTION_TB.get(i) + ",";
				line += NUM_QUESTION_BD.get(i) + ",";
				line += NUM_QUESTION_BB.get(i) + ",";
				line += NUM_QUESTION_DA.get(i);
				result.add(line);
			}
			
			String root = "";
			String outputPath = root + "\\Exp Results New\\syn_key_dist_"+region+"_"+schemaSize+".csv";
			Utils.writeContent(result, outputPath, true);
		}
    }
    
	public static void main(String[] args) {
//		int schemaSize = 11;
//		int schemaSize = 13;
		int schemaSize = 15;
		
//		String region = "bottom";
//		String region = "middle";
//		String region = "top";
		
		String region = "all";
		
		runExp(Arrays.asList(schemaSize), region);
	}

}
