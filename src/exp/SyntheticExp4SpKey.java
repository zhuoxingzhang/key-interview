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
 * with p=3, ..., we collect the number of questions for interviewing.
 * the answer is no for the interviewing question if corresponding FD is implied by given FD set.
 *
 */
public class SyntheticExp4SpKey {
	
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
		List<String> R = SyntheticExp4SpKey.genSpSchema(p);
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
		System.out.println("\nstrategy: "+strategy + " | avg cost: " + (end - start)/(double)repeat);
		
		int num4NOAnswers = (int) res.get(1);
		int num4AllAnswers = (int) res.get(2);
		int interviewRound = (int) res.get(3);
		
		String output = p+","+strategy+","+R.size()
		+","+interviewRound+","+num4NOAnswers+","+num4AllAnswers+","+num4NOAnswers/(double)num4AllAnswers+","+(end - start)/(double)repeat;
		Utils.writeContent(Arrays.asList(output), path, true);
		System.out.println(output);
	}
	
	
	public static void main(String[] args) {
		int repeat=196830;//1000000
//		String strategy = "bottomup bfs";
//		String strategy = "bottomup dfs";
//		String strategy = "topdown dfs";
		String strategy = "topdown bfs";

		String path = "C:\\Users\\zzha969\\OneDrive - The University of Auckland\\Desktop\\PhD\\Armstrong interviews(Key)\\Exp Results\\"+strategy+"_sp_schemata(key).csv";
		for(int p = 1;p <= 10;p ++) {
//			if(p == 1)
//				repeat = 100000;
//			else if(p >= 2 & p <= 4)
//				repeat = 20000;
//			else if(p >= 5 && p <= 6)
//				repeat = 2000;
//			else if(p >= 7 && p <= 8)
//				repeat = 100;
//			else if(p == 9)
//				repeat = 20;
//			else if(p == 10)
//				repeat = 5;
//			else
//				repeat = 1;	
			System.out.println("p=" + p + " | repeat=" + repeat);
			SyntheticExp4SpKey.runExp(p, strategy, repeat, path);
			repeat = (int)(repeat/3.0);
		}
	}

}
