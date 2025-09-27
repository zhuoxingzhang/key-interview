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

public class RealWorldExpHockey {

	public static void runExp(int repeat, String path, String dataset,String strategy, List<String> R, List<Key> minimalKeys) {
		List<Object> res = null;
		long start = System.currentTimeMillis();
		for(int i = 0;i < repeat;i ++) {
			if(strategy.equals("topdown dfs"))
				res = Interview.interview("topdown", "dfs", R, false, minimalKeys);
			else if(strategy.equals("topdown bfs"))
				res = Interview.interview("topdown", "bfs", R, false, minimalKeys);
			else if(strategy.equals("bottomup dfs"))
				res = Interview.interview("bottomup", "dfs", R, false, minimalKeys);
			else if(strategy.equals("bottomup bfs"))
				res = Interview.interview("bottomup", "bfs", R, false, minimalKeys);
		}
		long end = System.currentTimeMillis();
		
		
		int num4NOAnswers = (int) res.get(1);
		int num4AllAnswers = (int) res.get(2);
		int interviewRound = (int) res.get(3);
		
		int schemaSize = R.size();
		
		DecimalFormat df = new DecimalFormat("#.####");
	    double avgRatio = Double.parseDouble(df.format(num4NOAnswers / (double)num4AllAnswers));
	    double avgCost = Double.parseDouble(df.format((end - start)/(double)repeat));
		System.out.println("dataset="+dataset+" | strategy="+strategy + " | |R|="+schemaSize
				+ " | interview round=" + interviewRound
				+ " | no="+num4NOAnswers +" | all="+num4AllAnswers+" | no/all="+avgRatio+" | cost="+avgCost);
		String output = dataset+","+strategy+","+schemaSize+","
				+interviewRound+","
				+num4NOAnswers+","+num4AllAnswers+","+avgRatio+","+avgCost;
		Utils.writeContent(Arrays.asList(output), path, true);
	}
	
	public static List<Object> getSchema(String name) throws Exception{
		List<String> R = null;
		List<Key> minimalKeys = new ArrayList<>();
		if(name.equals("TeamsHalf")) {
			R = new ArrayList<>(Arrays.asList("year", "tmID", "half", "lgID", "rank", "G", "W", "L", "T", "GF", "GA"));
			Key k1 = new Key(Arrays.asList("year", "tmID", "half"));
			Key k2 = new Key(Arrays.asList("year", "half", "rank"));
			minimalKeys.addAll(Arrays.asList(k1, k2));
		}else if(name.equals("TeamsPost")) {
			R = new ArrayList<>(Arrays.asList("year", "tmID", "lgID", "G", "W", "L", "T", "GF", "GA", "PIM", "BenchMinor", 
					"PPG", "PPC", "SHA", "PKG", "PKC", "SHF"));
			Key k1 = new Key(Arrays.asList("year", "tmID"));
			minimalKeys.add(k1);
		}else if(name.equals("SeriesPost")) {
			R = new ArrayList<>(Arrays.asList("year", "round", "series", "tmIDWinner", "lgIDWinner", "tmIDLoser", "lgIDLoser",
					"W", "L", "T", "GoalsWinner", "GoalsLoser", "note"));
			Key k1 = new Key(Arrays.asList("year", "tmIDWinner", "tmIDLoser"));
			Key k2 = new Key(Arrays.asList("year", "round", "tmIDWinner"));
			Key k3 = new Key(Arrays.asList("year", "round", "tmIDLoser"));
			Key k4 = new Key(Arrays.asList("year", "series"));
			minimalKeys.addAll(Arrays.asList(k1, k2, k3, k4));
		}else if(name.equals("AwardsMisc")) {
			R = new ArrayList<>(Arrays.asList("name", "ID", "award", "year", "lgID", "note"));
			Key k1 = new Key(Arrays.asList("name"));
			Key k2 = new Key(Arrays.asList("ID"));
			minimalKeys.addAll(Arrays.asList(k1, k2));
		}else
			throw new Exception("unexcepted name: " + name);
		
		List<Object> res = new ArrayList<>();
		res.add(R);
		res.add(minimalKeys);
		return res;
	}
	
	public static void main(String[] args) throws Exception {
		int repeat = 500000;
		
//		String strategy = "bottomup dfs";
//		String strategy = "bottomup bfs";
//		String strategy = "topdown dfs";
		String strategy = "topdown bfs";
		
//		String tableName = "TeamsHalf";//11
//		String tableName = "SeriesPost";//13
//		String tableName = "TeamsPost";//17
		String tableName = "AwardsMisc";//6
		List<Object> res = getSchema(tableName);
		
		List<String> R = (List<String>) res.get(0);
		List<Key> minimalKeys = (List<Key>) res.get(1);
		
		String root = "C:\\Users\\zzha969\\OneDrive - The University of Auckland\\Desktop\\PhD\\Armstrong interviews(Key)";
		String outputPath = root + "\\Exp Results\\rw_hockey.csv";
		System.out.println("table name: "+tableName+" | |R|: "+R.size()+" | min key num: "+minimalKeys.size());
		runExp(repeat, outputPath, tableName,strategy, R, minimalKeys);

	}

}
