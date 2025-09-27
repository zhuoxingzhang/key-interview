package test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.TreeMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import entity.FD;
import exp.Interview;
import exp.SyntheticExp4Sp;
import util.Utils;

public class TestBottomUp {

	public static void runExp(int p, String givenFDCoverType, String strategy, int repeat, String path) {
		List<String> R = SyntheticExp4Sp.genSpSchema(p);
		List<FD> cover = null;
		if("optimal".equals(givenFDCoverType))
			cover = SyntheticExp4Sp.genSpOptimalFDs(p);
		else if("reduced minimal".equals(givenFDCoverType))
			cover = SyntheticExp4Sp.genSpRedMinFDs(p);
		else
			new Exception("cover type error!");
		List<Object> res = null;
		long start = System.currentTimeMillis();
		for(int i = 0;i < repeat;i ++) {
			if(strategy.equals("topdown dfs"))
				res = Interview.interview("topdown", "dfs", R, cover, false);
			else if(strategy.equals("topdown bfs"))
				res = Interview.interview("topdown", "bfs", R, cover, false);
			else if(strategy.equals("bottomup dfs"))
				res = Interview.interview("bottomup", "dfs",R, cover, false);
			else if(strategy.equals("bottomup bfs"))
				res = Interview.interview("bottomup", "bfs", R, cover, false);
		}
		long end = System.currentTimeMillis();
		System.out.println("strategy: "+strategy + " | avg cost: " + (end - start)/(double)repeat);
		List<FD> minedFDs = (List<FD>) res.get(0);
		int num4NOAnswers = (int) res.get(1);
		int num4AllAnswers = (int) res.get(2);
		int interviewRound = (int) res.get(3);

		System.out.println("Is given FD set is equivalent with mined FD set?");
		if(SyntheticExp4Sp.isTwoFDSetsEquivalent(cover, minedFDs))
			System.out.println("Yes, they are equivalent!");
		else
			System.out.println("No, they are NOT equivalent!");
		int ArmRelNum = Interview.genArmstrongRelation(R, cover).size();//number of Armstrong relation
		String output = p+","+givenFDCoverType+","+strategy+","+R.size()+","+cover.size()+","+Utils.compFDAttrSymbNum(cover)+","+ArmRelNum+","+num4NOAnswers+","+num4AllAnswers
				+","+num4NOAnswers/(double)num4AllAnswers+","+(end - start)/(double)repeat+","+interviewRound;
		Utils.writeContent(Arrays.asList(output), path, true);
		System.out.println(output);
	}
	
	
	public static void main(String[] args) {
		int repeat=1;//1000000
		String path = "C:\\Users\\zzha969\\OneDrive - The University of Auckland\\Desktop\\PhD\\Armstrong interviews(FD)\\Exp Results\\bottomup_result_sp_schemata.csv";
		for(int p = 9;p <= 9;p ++) {
//			if(p == 1)
//				repeat = 100000;
//			else if(p >= 2 & p < 4)
//				repeat = 2000;
//			else if(p >= 4 && p <= 6)
//				repeat = 200;
//			else if(p == 7)
//				repeat = 20;
//			else
//				repeat = 1;
					
//			String strategy = "bottomup bfs";
			String strategy = "bottomup dfs";
			String inputFDCover = "reduced minimal";//optimal & reduced minimal
			TestBottomUp.runExp(p, inputFDCover, strategy, repeat, path);//depth first & breadth first
		}
	}
	
	
//	public static void main(String[] args) {
////		List<String> R = new ArrayList<>(Arrays.asList("E", "S", "B1", "B2", "I1", "I2"));
////		List<FD> givenFDs = new ArrayList<>();
////		givenFDs.add(new FD(Arrays.asList("B1", "B2"), Arrays.asList("E")));
////		givenFDs.add(new FD(Arrays.asList("E"), Arrays.asList("B1", "B2")));
////		givenFDs.add(new FD(Arrays.asList("E", "S"), Arrays.asList("I1", "I2")));
////		givenFDs.add(new FD(Arrays.asList("I1"), Arrays.asList("B1", "B2")));
////		givenFDs.add(new FD(Arrays.asList("I2"), Arrays.asList("B1", "B2")));
//		
//		List<String> R = new ArrayList<>(Arrays.asList("A", "B", "C", "D", "E", "F"));
//		List<FD> givenFDs = new ArrayList<>();
//		givenFDs.add(new FD(Arrays.asList("A", "B"), Arrays.asList("C")));
//		givenFDs.add(new FD(Arrays.asList("C"), Arrays.asList("A", "B")));
//		givenFDs.add(new FD(Arrays.asList("C", "F"), Arrays.asList("D", "E")));
//		givenFDs.add(new FD(Arrays.asList("D"), Arrays.asList("A", "B")));
//		givenFDs.add(new FD(Arrays.asList("E"), Arrays.asList("A", "B")));
////		
//////		List<String> R = SyntheticExp4Sp.genSpSchema(2);
//////		List<FD> givenFDs = SyntheticExp4Sp.genSpRedMinFDs(2);
//		
//		List<Object> res = TestBottomUp.interview_bst_bottomup(R, givenFDs, false);
//		List<FD> minedFDs = (List<FD>) res.get(0);
//		int num4NOAnswers = (int) res.get(1);
//		int num4AllAnswers = (int) res.get(2);
//		int interviewRound = (int) res.get(3);
//		System.out.println("schema: " + R +"\nInput FD cover:");
//		Utils.printFDs(givenFDs);
//		System.out.println("Mined FD cover:");
//		minedFDs = Utils.compFDCover(minedFDs, "reduced minimal");
//		Utils.printFDs(minedFDs);
//		System.out.println("Is given FD set is equivalent with mined FD set?");
//		if(SyntheticExp4Sp.isTwoFDSetsEquivalent(givenFDs, minedFDs))
//			System.out.println("Yes, they are equivalent!");
//		else
//			System.out.println("No, they are NOT equivalent!");
//
//	}

}
