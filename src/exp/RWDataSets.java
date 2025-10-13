package exp;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import entity.FD;
import entity.Key;
import entity.Parameter;
import util.Utils;

public class RWDataSets {
	/**
	 * Get top-k FDs of EACH fd set that has same number n (1 - max number) on LHS as input FD set to mine FDs.
	 * e.g., for top 5, we have fds of top 5 of size 1 on LHS, plus top 5 of size 2 on LHS, ...
	 */
	public static List<Object> getTopKFDs4LHS(double k, List<FD> FDs){
		List<FD> topk = new ArrayList<>();
		int sum = 0;//sum of number of attributes on LHS
		Map<Integer, List<FD>> fds_map = new HashMap<>();// key: number, value: FDs which number of attributes on LHS is k
		for(FD fd : FDs) {
			int numOnLHS = fd.getLeftHand().size();
			if(numOnLHS == 0)
				continue;
			if(!fds_map.containsKey(numOnLHS))
				fds_map.put(numOnLHS, new ArrayList<FD>());
			fds_map.get(numOnLHS).add(fd);
		}
		for(Map.Entry<Integer, List<FD>> entry : fds_map.entrySet()) {
			int num = entry.getKey();
			List<FD> fds_with_num = entry.getValue();
			//get top k of number num
			int k_num = (int)(k * fds_with_num.size());
			for(int i = 0;i < k_num;i ++) {
				sum += num;
				topk.add(fds_with_num.get(i));
			}
		}
		DecimalFormat df = new DecimalFormat("#.####");
        double avgNumAttrOnLHS =  Double.parseDouble(df.format(sum/(double)topk.size()));
		List<Object> res = new ArrayList<>();
        res.add(topk);
        res.add(avgNumAttrOnLHS);
        return res;
	}
	
	public static String getMeanAndStd(List<Key> keys) {
        if (keys == null || keys.isEmpty()) {
            return "0.00 ± 0.00";
        }

        int n = keys.size();
        double sum = 0.0;
        for (Key key : keys) {
            sum += key.size();
        }

        double mean = sum / n;
        
        double varianceSum = 0.0;
        for (Key key : keys) {
            double diff = key.size() - mean;
            varianceSum += diff * diff;
        }
        double std = Math.sqrt(varianceSum / n);
        return String.format("%.2f ± %.2f", mean, std);
    }
	
	public static String getMinAvgMaxWithRegionAndBound(List<Key> keys, int schemaSize) {
	    if (keys == null || keys.isEmpty()) {
	        return "0; 0.00; 0 (NoRegion; NoBoundary)";
	    }

	    int n = keys.size();
	    int min = Integer.MAX_VALUE;
	    int max = Integer.MIN_VALUE;
	    double sum = 0.0;

	    for (Key key : keys) {
	        int size = key.size();
	        sum += size;
	        if (size < min) min = size;
	        if (size > max) max = size;
	    }

	    double avg = sum / n;

	    // ===== Region =====
	    // Bottom: [0, n/3]
	    // Middle: (n/3, 2n/3]
	    // Top: (2n/3, n]
	    String region;
	    if (avg <= schemaSize / 3.0) {
	        region = "bottom";
	    } else if (avg <= (2.0 * schemaSize) / 3.0) {
	        region = "middle";
	    } else {
	        region = "top";
	    }

	    // ===== Boundary =====
	    // low: [0, (n - 2)/3]
	    // normal: ((n - 2)/3, (2n - 4)/3]
	    // high: ((2n - 4)/3, n - 2]
	    int boundaryRange = max - min;
	    String boundary;
	    if (boundaryRange <= (schemaSize - 2) / 3.0) {
	        boundary = "low";
	    } else if (boundaryRange <= (2.0 * schemaSize - 4) / 3.0) {
	        boundary = "normal";
	    } else {
	        boundary = "high";
	    }

	    return String.format("%d; %.2f; %d (%s; %s)", min, avg, max, region, boundary);
	}

	
	public static List<Key> filterKeysBySubschema(List<Key> keys, List<String> subschema) {
	    if (subschema == null || subschema.isEmpty()) {
	        return Collections.emptyList();
	    }

//	    int halfSize = attributes.size() / 2;
//	    List<String> topHalfAttrs = attributes.subList(0, halfSize);

	    List<Key> filteredKeys = new ArrayList<>();
	    for (Key key : keys) {
	        List<String> keyAttrs = new ArrayList<>(key.getAttributes());
	        if (subschema.containsAll(keyAttrs)) {
	            filteredKeys.add(key);
	        }
	    }

	    return filteredKeys;
	}

	
	
	public static void runExp(int repeat, String path, String givenFDCoverType, Parameter para, double k, boolean reducedSchema, boolean preDeterminedKey) {
		List<Object> info = Utils.getFDCover(givenFDCoverType, para);
		if(info == null)//not exist corresponding cover
			return;
		List<String> R = (List<String>) info.get(0);
		int originSchemaSize = R.size();
		List<FD> cover = (List<FD>) info.get(1);
		
		//get top k FDs of redundancy
		List<Object> results = getTopKFDs4LHS(k, cover);
		List<FD> topKFDs = (List<FD>) results.get(0);
		List<Key> minimalKeys = Utils.getMinimalKeys(R, topKFDs);
		List<Key> preDeterminedKeys = new ArrayList<>();
		List<Object> res = null;
		
		
		if(reducedSchema) {
			Set<String> meaningful = new HashSet<>();
			minimalKeys.forEach(k1 -> meaningful.addAll(k1.getAttributes()));
			int subschemaSize =  R.size()/2;
			List<String> subR = null;
			List<Key> minKeys = null;
			Random random = new Random();
			int minSize = minimalKeys.stream()
		               .mapToInt(e -> e.size())
		               .min()
		               .orElse(0);
			if(subschemaSize < minSize)
				subschemaSize = minSize;
			do {
				if(meaningful.size() < subschemaSize) {
					subR = new ArrayList<>();
					subR.addAll(meaningful);
					for(int i = 0;subR.size() != subschemaSize;i ++) {
						if(!subR.contains(R.get(i)))
							subR.add(R.get(i));
					}
				}else {
					List<String> meaningfulList = new ArrayList<>(meaningful);
			        Collections.shuffle(meaningfulList, random);
			        subR = new ArrayList<>(meaningfulList.subList(0, subschemaSize));
				}
				minKeys = filterKeysBySubschema(minimalKeys, subR);//get minimal keys with reduced schema
			}while(minKeys.isEmpty());
			R = subR;
			minimalKeys = minKeys;
		}
		
		for(String strategy : Arrays.asList("bottomup dfs", "bottomup bfs", "topdown dfs", "topdown bfs")) {
			//normal case
//			preDeterminedKeys.clear();
//			long start = System.currentTimeMillis();
//			for(int i = 0;i < repeat;i ++) {
//				if(strategy.equals("topdown dfs"))
//					res = Interview.interview("topdown", "dfs", R, false, minimalKeys, preDeterminedKeys);
//				else if(strategy.equals("topdown bfs"))
//					res = Interview.interview("topdown", "bfs", R, false, minimalKeys, preDeterminedKeys);
//				else if(strategy.equals("bottomup dfs"))
//					res = Interview.interview("bottomup", "dfs", R, false, minimalKeys, preDeterminedKeys);
//				else if(strategy.equals("bottomup bfs"))
//					res = Interview.interview("bottomup", "bfs", R, false, minimalKeys, preDeterminedKeys);
//			}
//			long end = System.currentTimeMillis();
//			
//			Set<Key> minedKeys = (Set<Key>) res.get(0);
//			Set<Key> refinedMinKeys = Utils.refineToMinimalKeys(new ArrayList<Key>(minedKeys));
//			
//			int num4NOAnswers = (int) res.get(1);
//			int num4AllAnswers = (int) res.get(2);
//			int interviewRound = (int) res.get(3);
//			if(Interview.isTwoKeySetsEqual(minimalKeys, new ArrayList<>(refinedMinKeys))) {
//				System.out.println("Yes, mined key set is minimal and complete!");
//			}else {
//				System.out.println("No, mined key set is neither minimal nor complete!");
//			}
//			int schemaSize = R.size();
//			int FDNum = topKFDs.size();
//			int FDSize = Utils.compFDAttrSymbNum(topKFDs);
//			int LHSSize = Utils.compFDLHSAttrSymbNum(topKFDs);
//			
//			int minedKeyAttrSymNum = Utils.compKeyAttrSymbNum(refinedMinKeys);
//			int minKeyAttrSymNum = Utils.compKeyAttrSymbNum(minimalKeys);
//			int preDeterKeyAttrSymNum = Utils.compKeyAttrSymbNum(preDeterminedKeys);
//			
//			DecimalFormat df = new DecimalFormat("#.####");
//		    double avgRatio = Double.parseDouble(df.format(num4NOAnswers / (double)num4AllAnswers));
//		    double avgCost = Double.parseDouble(df.format((end - start)/(double)repeat));
//			
//			String output = para.dataset.name+","+strategy+","+reducedSchema+",false,"+(int)(k*100)+"%,"+originSchemaSize+","+schemaSize+","
//					+minimalKeys.size()+","+minKeyAttrSymNum+","+getMinAvgMaxWithRegionAndBound(minimalKeys, schemaSize)+","+preDeterminedKeys.size()+","+preDeterKeyAttrSymNum+","+getMinAvgMaxWithRegionAndBound(preDeterminedKeys, schemaSize)+","
//					+num4NOAnswers+","+num4AllAnswers;
//			System.out.println(output + "\n");
//			Utils.writeContent(Arrays.asList(output), path, true);
			
			//case of having preDetermined keys
			if(preDeterminedKey) {
				preDeterminedKeys.clear();
				int preNum;
				if(minimalKeys.size() <= 10)
					preNum = minimalKeys.size()/2;
				else if(minimalKeys.size() > 10 && minimalKeys.size() <= 100)
					preNum = minimalKeys.size()/3;
				else
					preNum = minimalKeys.size()/4;
				preDeterminedKeys.addAll(minimalKeys.subList(0, preNum));
				long start = System.currentTimeMillis();
				for(int i = 0;i < repeat;i ++) {
					res = Interview.interview(strategy.split(" ")[0], strategy.split(" ")[1], R, false, minimalKeys, preDeterminedKeys);
				}
				long end = System.currentTimeMillis();
				
				Set<Key> minedKeys = (Set<Key>) res.get(0);
				Set<Key> refinedMinKeys = Utils.refineToMinimalKeys(new ArrayList<Key>(minedKeys));
				
				int num4NOAnswers = (int) res.get(1);
				int num4AllAnswers = (int) res.get(2);
				int interviewRound = (int) res.get(3);
				if(Interview.isTwoKeySetsEqual(minimalKeys, new ArrayList<>(refinedMinKeys))) {
					System.out.println("Yes, mined key set is minimal and complete!");
				}else {
					System.out.println("No, mined key set is neither minimal nor complete!");
				}
				int schemaSize = R.size();
				
				int minedKeyAttrSymNum = Utils.compKeyAttrSymbNum(refinedMinKeys);
				int minKeyAttrSymNum = Utils.compKeyAttrSymbNum(minimalKeys);
				int preDeterKeyAttrSymNum = Utils.compKeyAttrSymbNum(preDeterminedKeys);
				DecimalFormat df = new DecimalFormat("#.####");
			    double avgRatio = Double.parseDouble(df.format(num4NOAnswers / (double)num4AllAnswers));
			    double avgCost = Double.parseDouble(df.format((end - start)/(double)repeat));
				
				String output = para.dataset.name+","+strategy+","+reducedSchema+","+preDeterminedKey+","+(int)(k*100)+"%,"+originSchemaSize+","+schemaSize+","
						+minimalKeys.size()+","+minKeyAttrSymNum+","+getMinAvgMaxWithRegionAndBound(minimalKeys, schemaSize)+","+preDeterminedKeys.size()+","+preDeterKeyAttrSymNum+","+getMinAvgMaxWithRegionAndBound(preDeterminedKeys, schemaSize)+","
						+num4NOAnswers+","+num4AllAnswers;
				System.out.println(output + "\n");
				Utils.writeContent(Arrays.asList(output), path, true);
			}
		}
	}
	
	public static void main(String[] args) {
		int repeat = 1;
		List<Double> topKList = Arrays.asList(1.0);//0.2, 0.4, 0.6, 0.8, 1.0; top 20%, 40%, 60%, 80%, 100%
//		String strategy = "bottomup dfs";
//		String strategy = "bottomup bfs";
//		String strategy = "topdown dfs";
//		String strategy = "topdown bfs";
		
		boolean reducedSchema = false;//reduced 50% schema size
		boolean preDeterminedKey = true;//predetermined 50% keys of ground-truth minimal keys
		
		int max_col_num = 19;
		String root = "";
		String outputPath = root + "\\Exp Results New\\rw_lhs("+reducedSchema+","+preDeterminedKey+").csv";
		List<Parameter> paras = Utils.getParameterListV1(null, root);
		paras.sort(new Comparator<Parameter>() {
			@Override
			public int compare(Parameter o1, Parameter o2) {
				return o1.dataset.col_num - o2.dataset.col_num;
			}
		});
		for(Parameter para : paras) {
			if(para.dataset.col_num > max_col_num)
				continue;
			
//			if(para.dataset.col_num <= 13)
//				repeat = 100;
//			else if(13 < para.dataset.col_num && para.dataset.col_num <= 15)
//				repeat = 30;
//			else if(15 < para.dataset.col_num && para.dataset.col_num <= 17)
//				repeat = 5;
//			else if(17 < para.dataset.col_num && para.dataset.col_num <= 19)
//				repeat = 1;
//			else
//				repeat = 1;
			
			
			for(double topK : topKList) {
				String coverType = "reduced minimal";
				System.out.println("\n" + para.dataset.name + " | " + topK + " | "+ coverType );
				runExp(repeat, outputPath, coverType, para, topK, reducedSchema, preDeterminedKey);
			}
		}

	}

}
