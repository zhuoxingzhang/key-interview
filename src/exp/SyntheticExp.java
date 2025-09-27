package exp;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.text.DecimalFormat;

import java.util.Set;
import java.util.HashSet;

import entity.FD;
import entity.Key;
import util.Utils;

/**
 * 
 *	Synthetic experiments: Vary size of relation schema Vary probability p of ‘No’ answer in percentiles of 10
 */
public class SyntheticExp {
    
    public static void runExp(int startSize, int endSize, String path, String strategy) {
    	for(int schemaSize = startSize; schemaSize <= endSize; schemaSize ++) {
    		int repeatBound = 0;
    		if(schemaSize <= 6)
    			repeatBound = 50000;
    		else if(schemaSize <= 8)
    			repeatBound = 15000;
    		else if(schemaSize == 9)
    			repeatBound = 10000;
    		else if(schemaSize == 10)
    			repeatBound = 5000;
    		else if(schemaSize == 11)
    			repeatBound = 2000;
    		else if(schemaSize >= 12 && schemaSize <= 13)
    			repeatBound = 200;//300
    		else if(schemaSize >= 14 && schemaSize <= 15)
    			repeatBound = 5;//300
//    		else if(schemaSize == 16)
//    			repeatBound = 5;//100
//    		else if(schemaSize >= 17 && schemaSize <= 18)
//    			repeatBound = 30;
//    		else
//    			repeatBound = 5;
    		
			List<String> R = new ArrayList<>();
			for(int i = 0;i < schemaSize;i ++) {
				R.add("a"+i);
			}
			for(double p = 0; p <= 1; p += 0.1) {//probability to No(key)
				List<Integer> mined_key_num_list = new ArrayList<>();
				List<Integer> mined_key_size_list = new ArrayList<>();
				
				List<Integer> min_key_num_list = new ArrayList<>();
				List<Integer> min_key_size_list = new ArrayList<>();

				List<Integer> no_answer_num = new ArrayList<>();
				List<Integer> all_answer_num = new ArrayList<>();
				List<Double> answer_no_ratio = new ArrayList<>();
				List<Integer> interview_round_num = new ArrayList<>();
				List<Long> cost_list = new ArrayList<>();
				for(int repeat = 1; repeat <= repeatBound; repeat ++) {
					long start = System.currentTimeMillis();
					List<Object> res = null;
					if(strategy.equals("topdown dfs"))
						res = Interview.interview("topdown", "dfs", R, p, false);
					else if(strategy.equals("topdown bfs"))
						res = Interview.interview("topdown", "bfs", R, p, false);
					else if(strategy.equals("bottomup dfs"))
						res = Interview.interview("bottomup", "dfs",R, p, false);
					else if(strategy.equals("bottomup bfs"))
						res = Interview.interview("bottomup", "bfs", R, p, false);
					long end = System.currentTimeMillis();
					//collect the results
					Set<Key> minedKeys = (Set<Key>) res.get(0);

					int num4NOAnswers = (int) res.get(1);
					int num4AllAnswers = (int) res.get(2);
					int interviewRound = (int) res.get(3);
					int minedKeyNum = minedKeys.size();
					int minedKeySize = Utils.compKeyAttrSymbNum(minedKeys);
					
					Set<Key> refinedMinKeys = Utils.refineToMinimalKeys(new ArrayList<Key>(minedKeys));
					int minKeyNum = refinedMinKeys.size();
					int minKeySize = Utils.compKeyAttrSymbNum(refinedMinKeys);
					
					long cost = end - start;
					mined_key_num_list.add(minedKeyNum);
					mined_key_size_list.add(minedKeySize);
					
					min_key_num_list.add(minKeyNum);
					min_key_size_list.add(minKeySize);
					
					cost_list.add(cost);
					no_answer_num.add(num4NOAnswers);
					all_answer_num.add(num4AllAnswers);
					answer_no_ratio.add(num4NOAnswers/(double)num4AllAnswers);
					interview_round_num.add(interviewRound);
				}
				int mined_key_num_min = Utils.calculateMin(mined_key_num_list);
				int mined_key_num_max = Utils.calculateMax(mined_key_num_list);
				double mined_key_num_avg = Utils.calculateAvg(mined_key_num_list);
				
				int mined_key_size_min = Utils.calculateMin(mined_key_size_list);
				int mined_key_size_max = Utils.calculateMax(mined_key_size_list);
				double mined_key_size_avg = Utils.calculateAvg(mined_key_size_list);
				
				
				int min_key_num_min = Utils.calculateMin(min_key_num_list);
				int min_key_num_max = Utils.calculateMax(min_key_num_list);
				double min_key_num_avg = Utils.calculateAvg(min_key_num_list);
				
				int min_key_size_min = Utils.calculateMin(min_key_size_list);
				int min_key_size_max = Utils.calculateMax(min_key_size_list);
				double min_key_size_avg = Utils.calculateAvg(min_key_size_list);
				
				
				int no_answer_num_min = Utils.calculateMin(no_answer_num);
				int no_answer_num_max = Utils.calculateMax(no_answer_num);
				double no_answer_num_avg = Utils.calculateAvg(no_answer_num);
				
				int all_answer_num_min = Utils.calculateMin(all_answer_num);
				int all_answer_num_max = Utils.calculateMax(all_answer_num);
				double all_answer_num_avg = Utils.calculateAvg(all_answer_num);
				
				double answer_no_ratio_min = Utils.calculateMin(answer_no_ratio);
				double answer_no_ratio_max = Utils.calculateMax(answer_no_ratio);
				double answer_no_ratio_avg = Utils.calculateAvg(answer_no_ratio);
				
				int interview_round_num_min = Utils.calculateMin(interview_round_num);
				int interview_round_num_max = Utils.calculateMax(interview_round_num);
				double interview_round_num_avg = Utils.calculateAvg(interview_round_num);
				
				long cost_min = Utils.calculateMin(cost_list);
				long cost_max = Utils.calculateMax(cost_list);
				double cost_avg = Utils.calculateAvg(cost_list);
				
				System.out.println("\nstrategy: " + strategy + " | |R|: " + schemaSize + " | p (No): " + p
						+ " | mined key num min\\max\\avg: " + mined_key_num_min+"\\"+mined_key_num_max+"\\"+mined_key_num_avg 
						+ " | mined key size min\\max\\avg: "+ mined_key_size_min+"\\"+mined_key_size_max+"\\"+mined_key_size_avg
						+ " | min key num min\\max\\avg: " + min_key_num_min+"\\"+min_key_num_max+"\\"+min_key_num_avg 
						+ " | min key size min\\max\\avg: "+ min_key_size_min+"\\"+min_key_size_max+"\\"+min_key_size_avg
						+ " | no answer min\\max\\avg: " + no_answer_num_min +"\\"+no_answer_num_max+"\\"+no_answer_num_avg
						+ " | all answer min\\max\\avg: " + all_answer_num_min +"\\"+all_answer_num_max+"\\"+all_answer_num_avg
						+ " | ratio min\\max\\avg: " + answer_no_ratio_min +"\\"+answer_no_ratio_max+"\\"+answer_no_ratio_avg
						+ " | round min\\max\\avg: " + interview_round_num_min +"\\"+interview_round_num_max+"\\"+interview_round_num_avg
						+ " | cost min\\max\\avg: " + cost_min+"\\"+cost_max+"\\"+cost_avg);
				String result = strategy + "," + schemaSize + "," + p + "," + (1-p) + "," + 
						mined_key_num_min+","+mined_key_num_max+","+mined_key_num_avg + ","+ 
						mined_key_size_min+","+mined_key_size_max+","+mined_key_size_avg + "," + 
						min_key_num_min+","+min_key_num_max+","+min_key_num_avg + ","+ 
						min_key_size_min+","+min_key_size_max+","+min_key_size_avg + "," + 
						no_answer_num_min + "," + no_answer_num_max + "," + no_answer_num_avg + "," +
						all_answer_num_min + "," + all_answer_num_max + "," + all_answer_num_avg + "," +
						answer_no_ratio_min + "," + answer_no_ratio_max + "," + answer_no_ratio_avg + "," +
						interview_round_num_min + "," + interview_round_num_max + "," + interview_round_num_avg + "," +
						cost_min+","+cost_max+","+cost_avg;
				Utils.writeContent(Arrays.asList(result), path, true);
			}
		}
    }
    
	public static void main(String[] args) {
		String strategy = "bottomup dfs";
//		String strategy = "bottomup bfs";
//		String strategy = "topdown dfs";
//		String strategy = "topdown bfs";
		String outputPath = "C:\\Users\\zzha969\\OneDrive - The University of Auckland\\Desktop\\PhD\\Armstrong interviews(Key)\\Exp Results\\"+strategy+"_p.csv";
		SyntheticExp.runExp(15, 15, outputPath, strategy);
	}

}
