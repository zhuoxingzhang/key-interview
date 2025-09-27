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
public class SyntheticExp4Sp {
	/**
	 * generate minimal-reduced FDs with input p
	 * @param p
	 * @return
	 */
	public static List<FD> genSpRedMinFDs(int p){
		List<FD> fdSet = new ArrayList<>();
		List<String> lhs1 = new ArrayList<>();//lhs of first FD
		for(int i = 1;i <= p;i ++) {
			lhs1.add("B" + i);
		}
		FD fd1 = new FD(lhs1, Arrays.asList("E"));//FD B1,...,Bp -> E
		fdSet.add(fd1);
		
		List<String> rhs2 = new ArrayList<>(lhs1);//rhs of second FD
		FD fd2 = new FD(Arrays.asList("E"), rhs2);//FD E -> B1,...,Bp
		fdSet.add(fd2);
		
		List<String> rhs3 = new ArrayList<>();//rhs of third FD
		for(int i = 1;i <= p;i ++) {
			rhs3.add("I" + i);
		}
		FD fd3 = new FD(Arrays.asList("E", "S"), rhs3);//FD ES -> I1,...,Ip
		fdSet.add(fd3);
		
		for(int i = 1;i <= p;i ++) {//produce p FDs
			fdSet.add(new FD(Arrays.asList("I"+i), new ArrayList<>(lhs1)));//Ii -> B1,...,Bp
		}
		return fdSet;
	}
	
	/**
	 * generate optimal FDs with input p
	 * @param p
	 * @return
	 */
	public static List<FD> genSpOptimalFDs(int p){
		List<FD> fdSet = new ArrayList<>();
		List<String> lhs1 = new ArrayList<>();//lhs of first FD
		for(int i = 1;i <= p;i ++) {
			lhs1.add("B" + i);
		}
		FD fd1 = new FD(lhs1, Arrays.asList("E"));//FD B1,...,Bp -> E
		fdSet.add(fd1);
		
		List<String> rhs2 = new ArrayList<>(lhs1);//rhs of second FD
		FD fd2 = new FD(Arrays.asList("E"), rhs2);//FD E -> B1,...,Bp
		fdSet.add(fd2);
		
		List<String> rhs3 = new ArrayList<>();//rhs of third FD
		for(int i = 1;i <= p;i ++) {
			rhs3.add("I" + i);
		}
		FD fd3 = new FD(Arrays.asList("E", "S"), rhs3);//FD ES -> I1,...,Ip
		fdSet.add(fd3);
		
		for(int i = 1;i <= p;i ++) {//produce p FDs
			fdSet.add(new FD(Arrays.asList("I"+i), Arrays.asList("E")));//Ii -> E
		}
		return fdSet;
	}
	
	/**
	 * generate schema of Sp
	 * @param p
	 * @return E,S,B1,...,Bp,I1,...,Ip
	 */
	public static List<String> genSpSchema(int p){
		List<String> R = new ArrayList<>();
		R.add("E");
		R.add("S");
		for(int i = 1;i <= p;i ++) {
			R.add("B"+i);
		}
		for(int i = 1;i <= p;i ++) {
			R.add("I"+i);
		}
		return R;
	}
	
	
	public static boolean isTwoFDSetsEquivalent(List<FD> G, List<FD> F) {
		for(FD fd : G) {//check if any FD in G can be implied by F
			if(!Utils.isImplied(F, fd)) {
//				System.out.println(fd.toString() + " is not implied by： ");
//				Utils.printFDs(F);
				return false;
			}
		}
		for(FD fd : F) {//check if any FD in F can be implied by G
			if(!Utils.isImplied(G, fd)) {
//				System.out.println(fd.toString() + " is not implied by： ");
//				Utils.printFDs(G);
				return false;
			}
		}
		return true;
	}
	
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
		System.out.println("\nstrategy: "+strategy + " | avg cost: " + (end - start)/(double)repeat);
		Set<Key> minedKeys = (Set<Key>) res.get(0);
		Set<Key> refinedMinKeys = Utils.refineToMinimalKeys(new ArrayList<Key>(minedKeys));
		
		int num4NOAnswers = (int) res.get(1);
		int num4AllAnswers = (int) res.get(2);
		int interviewRound = (int) res.get(3);
		System.out.println("Is mined key set minimal and complete?");
		if(Utils.KeysAreMinimal(cover, refinedMinKeys, R))
			System.out.println("Yes, they are minimal and complete!");
		else
			System.out.println("No, they are neither minimal nor complete!");
		int ArmRelNum = Interview.genArmstrongRelation(R, cover).size();//number of Armstrong relation
		String output = p+","+givenFDCoverType+","+strategy+","+R.size()+","+cover.size()+","+Utils.compFDAttrSymbNum(cover)+","+Utils.compFDLHSAttrSymbNum(cover)
		+","+Utils.compFDAttrSymbNum(cover)/(double)cover.size()+","+Utils.compFDLHSAttrSymbNum(cover)/(double)cover.size()
		+","+minedKeys.size()+","+Utils.compKeyAttrSymbNum(minedKeys)
		+","+refinedMinKeys.size()+","+Utils.compKeyAttrSymbNum(refinedMinKeys)
		+","+interviewRound+","+ArmRelNum+","+num4NOAnswers+","+num4AllAnswers+","+num4NOAnswers/(double)num4AllAnswers+","+(end - start)/(double)repeat;
		Utils.writeContent(Arrays.asList(output), path, true);
		System.out.println(output);
	}
	
	
	public static void main(String[] args) {
		int repeat=196830;//1000000
//		String strategy = "bottomup bfs";
//		String strategy = "bottomup dfs";
//		String strategy = "topdown dfs";
		String strategy = "topdown bfs";
		String inputFDCover = "reduced minimal";//optimal & reduced minimal
		String path = "C:\\Users\\zzha969\\OneDrive - The University of Auckland\\Desktop\\PhD\\Armstrong interviews(Key)\\Exp Results\\"+strategy+"_sp_schemata.csv";
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
			SyntheticExp4Sp.runExp(p, inputFDCover, strategy, repeat, path);
			repeat = (int)(repeat/3.0);
		}
	}

}
