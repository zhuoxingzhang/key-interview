package exp;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import entity.FD;
import entity.Key;
import entity.Parameter;
import util.Utils;

public class RWDesignedKeys {
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




	public static void runExp(int repeat, String path, String dataset,String strategy, List<String> R, List<Key> minimalKeys, List<Key> preDeterminedKeys) {
		List<Object> res = null;
		long start = System.currentTimeMillis();
		for(int i = 0;i < repeat;i ++) {
			res = Interview.interview(strategy.split(" ")[0], strategy.split(" ")[1], R, false, minimalKeys, preDeterminedKeys);
		}
		long end = System.currentTimeMillis();
		
		Set<Key> minedKeys = (Set<Key>) res.get(0);
		int num4NOAnswers = (int) res.get(1);
		int num4AllAnswers = (int) res.get(2);
		int interviewRound = (int) res.get(3);
		if(Interview.isTwoKeySetsEqual(minimalKeys, new ArrayList<>(minedKeys)))
			System.out.println("Success! Have interviewed all mininal keys!");
		else
			System.out.println("Failure! Have not interviewed all mininal keys!");
		
		int schemaSize = R.size();
		int keySize = Utils.compKeyAttrSymbNum(minimalKeys);
		String keyDist = getMinAvgMaxWithRegionAndBound(minimalKeys, schemaSize);
		double LP = (((double)keySize)/minimalKeys.size())/schemaSize;
		String lpStr = String.format("%.2f", LP);
		
		int preKeySize = Utils.compKeyAttrSymbNum(preDeterminedKeys);
		String preKeyDist = getMinAvgMaxWithRegionAndBound(preDeterminedKeys, schemaSize);
		
		DecimalFormat df = new DecimalFormat("#.####");
	    double avgRatio = Double.parseDouble(df.format(num4NOAnswers / (double)num4AllAnswers));
	    double avgCost = Double.parseDouble(df.format((end - start)/(double)repeat));
		String output = dataset+","+strategy+","+schemaSize+","+minimalKeys.size()+","+keySize+","+keyDist+","+lpStr+","
				+preDeterminedKeys.size()+","+preKeySize+","+preKeyDist+","
				+num4AllAnswers;
		System.out.println(output);
		Utils.writeContent(Arrays.asList(output), path, true);
	}
	
	public static List<Object> getSchema(String name) throws Exception{
		List<String> R = null;
		List<Key> minimalKeys = new ArrayList<>();

		if(name.equals("AwardsMisc")) {//
			R = new ArrayList<>(Arrays.asList("name", "ID", "award", "year"));
			Key k1 = new Key(Arrays.asList("name"));
			Key k2 = new Key(Arrays.asList("ID"));
			minimalKeys.addAll(Arrays.asList(k1, k2));
		}else if(name.equals("AwardsMisc full")) {//
			R = new ArrayList<>(Arrays.asList("name", "ID", "award", "year", "lgID", "note"));
			Key k1 = new Key(Arrays.asList("name"));
			Key k2 = new Key(Arrays.asList("ID"));
			minimalKeys.addAll(Arrays.asList(k1, k2));
		}else if(name.equals("TeamsHalf")) {//
			R = new ArrayList<>(Arrays.asList("year", "tmID", "half", "lgID", "rank"));
			Key k1 = new Key(Arrays.asList("year", "tmID", "half"));
			Key k2 = new Key(Arrays.asList("year", "half", "rank"));
			minimalKeys.addAll(Arrays.asList(k1, k2));
		}else if(name.equals("TeamsHalf full")) {//
			R = new ArrayList<>(Arrays.asList("year", "tmID", "half", "lgID", "rank", "G", "W", "L", "T", "GF", "GA"));
			Key k1 = new Key(Arrays.asList("year", "tmID", "half"));
			Key k2 = new Key(Arrays.asList("year", "half", "rank"));
			minimalKeys.addAll(Arrays.asList(k1, k2));
		}else if(name.equals("SeriesPost")) {//
			R = new ArrayList<>(Arrays.asList("year", "round", "series", "tmIDWinner", "tmIDLoser"));
			Key k1 = new Key(Arrays.asList("year", "tmIDWinner", "tmIDLoser"));
			Key k2 = new Key(Arrays.asList("year", "round", "tmIDWinner"));
			Key k3 = new Key(Arrays.asList("year", "round", "tmIDLoser"));
			Key k4 = new Key(Arrays.asList("year", "series"));
			minimalKeys.addAll(Arrays.asList(k1, k2, k3, k4));
		}else if(name.equals("SeriesPost full")) {//
			R = new ArrayList<>(Arrays.asList("year", "round", "series", "tmIDWinner", "tmIDLoser",
					"lgIDWinner", "lgIDLoser", "W", "L", "T", "GoalsWinner", "GoalsLoser", "note"));
			Key k1 = new Key(Arrays.asList("year", "tmIDWinner", "tmIDLoser"));
			Key k2 = new Key(Arrays.asList("year", "round", "tmIDWinner"));
			Key k3 = new Key(Arrays.asList("year", "round", "tmIDLoser"));
			Key k4 = new Key(Arrays.asList("year", "series"));
			minimalKeys.addAll(Arrays.asList(k1, k2, k3, k4));
		}else if(name.equals("TeamsPost")) {//
			R = new ArrayList<>(Arrays.asList("year", "tmID", "lgID"));
			Key k1 = new Key(Arrays.asList("year", "tmID"));
			minimalKeys.add(k1);
		}else if(name.equals("TeamsPost full")) {//
			R = new ArrayList<>(Arrays.asList("year", "tmID", "lgID", "G", "W", "L", "T", "GF", "GA", "PIM", 
					"BenchMinor", "PPG", "PPC", "SHA", "PKG", "PKC", "SHF"));
			Key k1 = new Key(Arrays.asList("year", "tmID"));
			minimalKeys.add(k1);
		}else if(name.equals("lineitem-0 full")) {
			R = new ArrayList<>(Arrays.asList(
				    "l_orderkey",
				    "l_partkey",
				    "l_suppkey",
				    "l_linenumber",
				    "l_quantity",
				    "l_extendedprice",
				    "l_discount",
				    "l_tax",
				    "l_returnflag",
				    "l_linestatus",
				    "l_shipdate",
				    "l_commitdate",
				    "l_receiptdate",
				    "l_shipinstruct",
				    "l_shipmode",
				    "l_comment"
				));
			Key k1 = new Key(Arrays.asList("l_orderkey","l_linenumber"));
			Key k2 = new Key(Arrays.asList("l_orderkey","l_partkey"));
			Key k3 = new Key(Arrays.asList("l_shipdate","l_orderkey","l_suppkey"));
			Key k4 = new Key(Arrays.asList("l_orderkey","l_suppkey","l_commitdate"));
			Key k5 = new Key(Arrays.asList("l_orderkey","l_suppkey","l_quantity"));
			Key k6 = new Key(Arrays.asList("l_orderkey","l_suppkey","l_receiptdate"));
			minimalKeys.addAll(Arrays.asList(k1,k2,k3,k4,k5,k6));
		}else if(name.equals("lineitem-0")) {
			R = new ArrayList<>(Arrays.asList("l_orderkey","l_linenumber","l_partkey","l_shipdate","l_suppkey","l_commitdate","l_quantity","l_receiptdate"));
			Key k1 = new Key(Arrays.asList("l_orderkey","l_linenumber"));
			Key k2 = new Key(Arrays.asList("l_orderkey","l_partkey"));
			Key k3 = new Key(Arrays.asList("l_shipdate","l_orderkey","l_suppkey"));
			Key k4 = new Key(Arrays.asList("l_orderkey","l_suppkey","l_commitdate"));
			Key k5 = new Key(Arrays.asList("l_orderkey","l_suppkey","l_quantity"));
			Key k6 = new Key(Arrays.asList("l_orderkey","l_suppkey","l_receiptdate"));
			minimalKeys.addAll(Arrays.asList(k1,k2,k3,k4,k5,k6));
		}else if(name.equals("lineitem-1 full")) {
			R = new ArrayList<>(Arrays.asList(
				    "l_orderkey",
				    "l_partkey",
				    "l_suppkey",
				    "l_linenumber",
				    "l_quantity",
				    "l_extendedprice",
				    "l_discount",
				    "l_tax",
				    "l_returnflag",
				    "l_linestatus",
				    "l_shipdate",
				    "l_commitdate",
				    "l_receiptdate",
				    "l_shipinstruct",
				    "l_shipmode",
				    "l_comment"
				));
			Key k1 = new Key(Arrays.asList("l_orderkey","l_linenumber"));
			Key k2 = new Key(Arrays.asList("l_orderkey","l_partkey"));
			minimalKeys.addAll(Arrays.asList(k1,k2));
		}else if(name.equals("lineitem-1")) {
			R = new ArrayList<>(Arrays.asList("l_orderkey","l_linenumber","l_partkey","l_suppkey"));
			Key k1 = new Key(Arrays.asList("l_orderkey","l_linenumber"));
			Key k2 = new Key(Arrays.asList("l_orderkey","l_partkey"));
			minimalKeys.addAll(Arrays.asList(k1,k2));
		}
		else
			throw new Exception("unexcepted name: " + name);
		
		List<Object> res = new ArrayList<>();
		res.add(R);
		res.add(minimalKeys);
		return res;
	}
	
	public static void main(String[] args) throws Exception {
//		String strategy = "bottomup dfs";
//		String strategy = "bottomup bfs";
//		String strategy = "topdown dfs";
//		String strategy = "topdown bfs";
		
		String root = "";
		String outputPath = root + "\\Exp Results New\\rw_designed_keys_full_schema_reduced_key.csv";
		
//		for(String tableName : Arrays.asList("AwardsMisc", "AwardsMisc full", "TeamsHalf", "TeamsHalf full", "SeriesPost", "SeriesPost full")) {
//		for(String tableName : Arrays.asList("TeamsPost", "TeamsPost full", "lineitem-0", "lineitem-0 full", "lineitem-1", "lineitem-1 full")) {
//		for(String tableName : Arrays.asList("TeamsPost", "AwardsMisc", "lineitem-0", "lineitem-1", "TeamsHalf", "SeriesPost")) {
		for(String tableName : Arrays.asList("TeamsPost full", "AwardsMisc full", "lineitem-0 full", "lineitem-1 full", "TeamsHalf full", "SeriesPost full")) {
			int repeat = 1;
			List<Object> res = getSchema(tableName);
			List<String> R = (List<String>) res.get(0);
			List<Key> minimalKeys = (List<Key>) res.get(1);
			List<Key> preDeterminedKeys = new ArrayList<>();
			
			//reduced 50% key as preDetermined keys
			preDeterminedKeys.addAll(minimalKeys.subList(0, minimalKeys.size()/2));
			
//			if(R.size() < 7)
//				repeat = 50000;
//			else if (R.size() < 10)
//				repeat = 500;
//			else if (R.size() < 13)
//				repeat = 50;
//			else
//				repeat = 5;
			for(String strategy : Arrays.asList("bottomup dfs", "bottomup bfs", "topdown dfs", "topdown bfs")) {
				System.out.println("table name: "+tableName+" | strategy: "+strategy+" | |R|: "+R.size()+" | min key num: "+minimalKeys.size()+" | repeat: "+repeat);
				runExp(repeat, outputPath, tableName,strategy, R, minimalKeys, preDeterminedKeys);
			}
		}

	}

}
