package exp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

import entity.FD;
import entity.Key;
import util.Utils;

/**
 * Sp schemata experiments
 *
 */
public class SyntheticSp {
	
	public static List<String> genSpSchema(int p){
		List<String> R = new ArrayList<>();
		R.add("D");
		R.add("R");
		for(int i = 1;i <= p;i ++) {
			R.add("S"+i);
		}
		for(int i = 1;i <= p;i ++) {
			R.add("T"+i);
		}
		return R;
	}
	
	
	
	public static void runExp(int p, String strategy, int repeat, String path) {
		List<String> R = SyntheticSp.genSpSchema(p);
		List<Key> minimalKeys = new ArrayList<>(Arrays.asList(new Key(Arrays.asList("D", "R"))));
		List<Object> res = null;
		long start = System.currentTimeMillis();
		for(int i = 0;i < repeat;i ++) {
			if(strategy.equals("topdown dfs"))
				res = Interview.interview("topdown", "dfs", R, false, minimalKeys);
			else if(strategy.equals("topdown bfs"))
				res = Interview.interview("topdown", "bfs", R, false, minimalKeys);
			else if(strategy.equals("bottomup dfs"))
				res = Interview.interview("bottomup", "dfs",R, false, minimalKeys);
			else if(strategy.equals("bottomup bfs"))
				res = Interview.interview("bottomup", "bfs", R, false, minimalKeys);
		}
		long end = System.currentTimeMillis();
		System.out.println("strategy: "+strategy + " | avg cost: " + (end - start)/(double)repeat);
		Set<Key> minedKeys = (Set<Key>) res.get(0);
		int num4NOAnswers = (int) res.get(1);
		int num4AllAnswers = (int) res.get(2);
		int interviewRound = (int) res.get(3);
		
		if(Interview.isTwoKeySetsEqual(minimalKeys, new ArrayList<>(minedKeys)))
			System.out.println("Success! Have interviewed all mininal keys!");
		else
			System.out.println("Failure! Have not interviewed all mininal keys!");
		
		String output = p+","+strategy+","+R.size()
		+","+interviewRound+","+num4NOAnswers+","+num4AllAnswers+","+num4NOAnswers/(double)num4AllAnswers+","+(end - start)/(double)repeat;
		Utils.writeContent(Arrays.asList(output), path, true);
		System.out.println(output + "\n");
	}
	
	
	public static void main(String[] args) {
		int repeat=0;//196830
//		String strategy = "bottomup bfs";
//		String strategy = "bottomup dfs";
//		String strategy = "topdown dfs";
		String strategy = "topdown bfs";

		String root = "";
		String outputPath = root + "\\Exp Results New\\syn_sp_"+strategy+".csv";
		for(int p = 1;p <= 9;p ++) {
			if(p >= 1 & p <= 2)
				repeat = 10000;
			else if(p >= 3 & p <= 4)
				repeat = 4000;
			else if(p >= 5 && p <= 6)
				repeat = 1000;
			else if(p >= 7 && p < 8)
				repeat = 50;
			else if(p >= 8 && p <= 9)
				repeat = 1;
			else if(p == 10)
				repeat = 1;
			else
				repeat = 1;	
			System.out.println("p=" + p + " | repeat=" + repeat);
			runExp(p, strategy, repeat, outputPath);
//			repeat = (int)(repeat/3.0);
		}
	}

}
