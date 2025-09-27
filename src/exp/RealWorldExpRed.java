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
 * and using top-k FDs of redundancy as input FD set to mine FDs.
 *
 */
public class RealWorldExpRed {
	private static String readFileAsString(String filePath) {
        StringBuilder content = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line).append("\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
        return content.toString();
    }
	/**
	 * parse json files to get map which key is FD and value is its redundancy
	 * @return
	 */
	public static Map<FD, Integer> parseJSONToFDRedMap(String jsonPath) {
		Map<FD, Integer> fdRedMap = new HashMap<>();
        String jsonContent = readFileAsString(jsonPath);
        if (jsonContent != null) {
            List<JSONObject> fdRedList = JSONArray.parseArray(jsonContent, JSONObject.class);
            for (JSONObject fdRed : fdRedList) {
                int redundancy = fdRed.getInteger("redundancy");
                String fdString = fdRed.getString("fd");
                String[] fdStr = fdString.split(" -> ");
                List<String> lhs = new ArrayList<>(Arrays.asList(fdStr[0].split(",")));
                List<String> rhs = new ArrayList<>(Arrays.asList(fdStr[1].split(",")));
                FD fd = new FD(lhs, rhs);
                fdRedMap.put(fd, redundancy);
            }
        }
        return fdRedMap;
	}
	
	public static List<Object> getTopKFDs4Red(double k, String jsonPath){
		Map<FD, Integer> fdRedMap = parseJSONToFDRedMap(jsonPath);
        List<Map.Entry<FD, Integer>> entryList = new ArrayList<>(fdRedMap.entrySet());
        entryList.sort(Map.Entry.comparingByValue());//sort
        int topK = (int)(k * entryList.size());
        // top k of increasing order
        List<FD> topKFDList = new ArrayList<>();
        int sumFDRed = 0;//sum of fd redundancy
        for (int i = 0; i < topK; i++) {
        	topKFDList.add(entryList.get(i).getKey());
            sumFDRed += entryList.get(i).getValue();
        }
        DecimalFormat df = new DecimalFormat("#.####");
        double avgFDRed =  Double.parseDouble(df.format(sumFDRed/(double)topK));
        List<Object> res = new ArrayList<>();
        res.add(topKFDList);
        res.add(avgFDRed);
        return res;
	}
	
	
	public static void runExp(int repeat, String path, String givenFDCoverType, Parameter para, String strategy, double k, String FDRedJsonPath) {
		List<Object> info = Utils.getFDCover(givenFDCoverType, para);
		if(info == null)//not exist corresponding cover
			return;
		List<String> R = (List<String>) info.get(0);
//		List<FD> cover = (List<FD>) info.get(1);
		
		//get top k FDs of redundancy
		List<Object> results = getTopKFDs4Red(k, FDRedJsonPath);
		List<FD> topKFDs = (List<FD>) results.get(0);
		double avgFDRed = (double) results.get(1);
		
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
//		String strategy = "bottomup bfs";
//		String strategy = "topdown dfs";
		String strategy = "topdown bfs";
		int max_col_num = 20;
		String root = "C:\\Users\\zzha969\\OneDrive - The University of Auckland\\Desktop\\PhD\\Armstrong interviews(Key)";
		String outputPath = root + "\\Exp Results\\"+strategy+"_rw_red.csv";
		List<Parameter> paras = Utils.getParameterListV1(null);
		paras.sort(new Comparator<Parameter>() {
			@Override
			public int compare(Parameter o1, Parameter o2) {
				return o1.dataset.col_num - o2.dataset.col_num;
			}
		});
		for(Parameter para : paras) {
			if(para.dataset.col_num > max_col_num)
				continue;
			
			if(para.dataset.col_num <= 13)
				repeat = 1000;
			else if(13 < para.dataset.col_num && para.dataset.col_num <= 15)
				repeat = 500;
			else if(15 < para.dataset.col_num && para.dataset.col_num <= 17)
				repeat = 100;
			else if(17 < para.dataset.col_num && para.dataset.col_num <= 19)
				repeat = 20;
			else
				repeat = 5;
			
			for(String coverType : Arrays.asList("reduced minimal")) {//"reduced minimal","optimal"
				for(double topK : topKList) {
					System.out.println("\n" + para.dataset.name + " | " + topK + " | "+ coverType + " | " + strategy);
					RealWorldExpRed.runExp(repeat, outputPath, coverType, para, strategy, topK, String.format(root + "\\FD Redundancy\\%s_fd_redundancy(reduced minimal).json", para.dataset.name));
				}
			}
		}

	}

}
