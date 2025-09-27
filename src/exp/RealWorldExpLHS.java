package exp;

import java.io.FileReader;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;

import java.io.BufferedReader;
import entity.FD;
import entity.Key;
import entity.Parameter;
import exp.Interview;
import util.Utils;
/**
 * simulate FD interviews for real world data sets,
 * and using top-k FDs of EACH fd set that has same number n (1 - max number) on LHS as input FD set to mine FDs.
 * e.g., for top 5, we have fds of top 5 of size 1 on LHS, plus top 5 of size 2 on LHS, ...
 */
public class RealWorldExpLHS {
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
	
	
	
	
	public static void runExp(int repeat, String path, String givenFDCoverType, Parameter para, String strategy, double k) {
		List<Object> info = Utils.getFDCover(givenFDCoverType, para);
		if(info == null)//not exist corresponding cover
			return;
		List<String> R = (List<String>) info.get(0);
		List<FD> cover = (List<FD>) info.get(1);
		
		//get top k FDs of redundancy
		List<Object> results = getTopKFDs4LHS(k, cover);
		List<FD> topKFDs = (List<FD>) results.get(0);
//		double avgNumAttrOnLHS = (double) results.get(1);
		
		List<Object> res = null;
		long start = System.currentTimeMillis();
		for(int i = 0;i < repeat;i ++) {
			if(strategy.equals("topdown dfs"))
				res = Interview.interview("topdown", "dfs", R, topKFDs, false);
			else if(strategy.equals("topdown bfs"))
				res = Interview.interview("topdown", "bfs", R, topKFDs, false);
			else if(strategy.equals("bottomup dfs"))
				res = Interview.interview("bottomup", "dfs",R, topKFDs, false);
			else if(strategy.equals("bottomup bfs"))
				res = Interview.interview("bottomup", "bfs", R, topKFDs, false);
		}
		long end = System.currentTimeMillis();
		
		Set<Key> minedKeys = (Set<Key>) res.get(0);
		Set<Key> refinedMinKeys = Utils.refineToMinimalKeys(new ArrayList<Key>(minedKeys));
		
		int num4NOAnswers = (int) res.get(1);
		int num4AllAnswers = (int) res.get(2);
		int interviewRound = (int) res.get(3);
		if(Utils.KeysAreMinimal(topKFDs, refinedMinKeys, R)) {
			System.out.println("Yes, mined key set is minimal and complete!");
		}else {
			System.out.println("No, mined key set is neither minimal nor complete!");
		}
		int schemaSize = R.size();
		int FDNum = topKFDs.size();
		int FDSize = Utils.compFDAttrSymbNum(topKFDs);
		int LHSSize = Utils.compFDLHSAttrSymbNum(topKFDs);
		
		int minedKeyAttrSymNum = Utils.compKeyAttrSymbNum(minedKeys);
		int minKeyAttrSymNum = Utils.compKeyAttrSymbNum(refinedMinKeys);
		
//		int ArmRelNum = Interview.genArmstrongRelation(R, topKFDs).size();//number of Armstrong relation
		
		DecimalFormat df = new DecimalFormat("#.####");
	    double avgRatio = Double.parseDouble(df.format(num4NOAnswers / (double)num4AllAnswers));
	    double avgCost = Double.parseDouble(df.format((end - start)/(double)repeat));
		System.out.println("dataset="+para.dataset.name+" | strategy="+strategy+" | cover type="+givenFDCoverType + "| FDLHS topk=" + (int)(k*100)+"% | |R|="+schemaSize+" | FD num="+FDNum+" | FD size="+FDSize+" | LHS size="+LHSSize
				+" | Mined key num=" + minedKeys.size() + " | Mined key size=" + minedKeyAttrSymNum
				+" | Minimal key num=" + refinedMinKeys.size() + " | Minimal key size=" + minKeyAttrSymNum
				+ " | interview round=" + interviewRound
//				+ " | Arm rel num="+ArmRelNum
				+ " | no="+num4NOAnswers +" | all="+num4AllAnswers+" | no/all="+avgRatio+" | cost="+avgCost);
		String output = para.dataset.name+","+strategy+","+givenFDCoverType+","+(int)(k*100)+"%,"+schemaSize+","+FDNum+","+FDSize+","+LHSSize+","
				+minedKeys.size()+","+minedKeyAttrSymNum+","
				+refinedMinKeys.size()+","+minKeyAttrSymNum+","
				+interviewRound+","
//				+ArmRelNum+","
				+num4NOAnswers+","+num4AllAnswers+","+avgRatio+","+avgCost;
		Utils.writeContent(Arrays.asList(output), path, true);
	}
	
	public static void main(String[] args) {
		int repeat = 1;
		List<Double> topKList = Arrays.asList(0.2, 0.4, 0.6, 0.8, 1.0);//0.2, 0.4, 0.6, 0.8, 1.0; top 20%, 40%, 60%, 80%, 100%
//		String strategy = "bottomup dfs";
		String strategy = "bottomup bfs";
//		String strategy = "topdown dfs";
//		String strategy = "topdown bfs";
		int max_col_num = 20;
		String root = "C:\\Users\\zzha969\\OneDrive - The University of Auckland\\Desktop\\PhD\\Armstrong interviews(Key)";
		String outputPath = root + "\\Exp Results\\"+strategy+"_rw_lhs.csv";
		List<Parameter> paras = Utils.getParameterListV1(null);
		paras.sort(new Comparator<Parameter>() {
			@Override
			public int compare(Parameter o1, Parameter o2) {
				return o1.dataset.col_num - o2.dataset.col_num;
			}
		});
		for(Parameter para : paras) {
//			if(para.dataset.col_num > max_col_num)
//				continue;
			if(para.dataset.col_num != 20)
				continue;
			
			if(para.dataset.col_num <= 13)
				repeat = 1000;
			else if(13 < para.dataset.col_num && para.dataset.col_num <= 15)
				repeat = 500;
			else if(15 < para.dataset.col_num && para.dataset.col_num <= 17)
				repeat = 100;
			else if(17 < para.dataset.col_num && para.dataset.col_num <= 19)
				repeat = 50;
			else
				repeat = 5;
			
			for(String coverType : Arrays.asList("reduced minimal")) {//"reduced minimal","optimal"
				for(double topK : topKList) {
					System.out.println("\n" + para.dataset.name + " | " + topK + " | "+ coverType + " | " + strategy);
					RealWorldExpLHS.runExp(repeat, outputPath, coverType, para, strategy, topK);
				}
			}
		}

	}

}
