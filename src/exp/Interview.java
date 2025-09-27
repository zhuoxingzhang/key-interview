package exp;

import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import java.util.Random;

import entity.FD;
import entity.Key;
import test.TestBottomUp;
import util.DBUtils;
import util.Utils;

public class Interview {
	/**
	 * check if the maximal set candidate w.r.t the target attribute and FD set Theta is a maximal set over schema R 
	 * @param maximalSetCand
	 * @param targetAttr
	 * @param R
	 * @param Theta
	 * @return
	 */
	public static boolean mtest(Set<String> maximalSetCand, String targetAttr, List<String> R, List<FD> Theta){
		FD X_A = new FD(new ArrayList<String>(maximalSetCand), new ArrayList<String>(Arrays.asList(targetAttr)));//FD: X -> A
		if(Utils.isImplied(Theta, X_A)) {
			return false;
		}
		List<String> otherAttrs = new ArrayList<>();
		for(String a : R) {
			if(!a.equals(targetAttr) && !maximalSetCand.contains(a)) {
				otherAttrs.add(a);
			}
		}
		for(String B : otherAttrs) {
			List<String> lhs1 = new ArrayList<>(maximalSetCand);
			lhs1.add(B);
			FD fd1 = new FD(lhs1, new ArrayList<>(Arrays.asList(targetAttr)));//XB -> A
			if(!Utils.isImplied(Theta, fd1)) {
				return false;
			}
		}
		return true;
	}
	
//	public static boolean keyTest(Key candKey, List<String> R, List<FD> Theta){
//		FD X_A = new FD(new ArrayList<String>(maximalSetCand), new ArrayList<String>(Arrays.asList(targetAttr)));//FD: X -> A
//		if(Utils.isImplied(Theta, X_A)) {
//			return false;
//		}
//		return true;
//	}
	
	public static Set<Set<String>> deepCopy(Set<Set<String>> original) {
        Set<Set<String>> copy = new HashSet<>();
        for (Set<String> subset : original) {
            Set<String> newSubset = new HashSet<>(subset);
            copy.add(newSubset);
        }
        return copy;
    }
	
	public static <T> Set<T> union(Set<T> set1, Set<T> set2) {
        Set<T> unionSet = new HashSet<>(set1);
        unionSet.addAll(set2);
        return unionSet;
    }
	
	public static <T> Set<T> intersect(Collection<T> set1, Collection<T> set2) {
        Set<T> intersection = new HashSet<>(set1);
        intersection.retainAll(set2);
        return intersection;
    }
	
	/**
	 * check if a set is subset of any set of sets
	 * @param sets
	 * @param oneSet
	 * @return
	 */
	public static boolean isSubsetOfAnySets(Set<Set<String>> sets, Set<String> oneSet) {
		boolean isSubset = false;
		for(Set<String> set : sets) {
			if(set.containsAll(oneSet)) {
				isSubset = true;
				break;
			}
		}
		return isSubset;
	}
	
	public static boolean isSupersetOfSomeSets(Set<Set<String>> sets, Set<String> oneSet) {
		boolean isSuperset = false;
		for(Set<String> set : sets) {
			if(oneSet.containsAll(set)) {
				isSuperset = true;
				break;
			}
		}
		return isSuperset;
	}
	
	public static boolean isSuperkeyOfSomeKeys(Collection<Key> keys, Key oneKey) {
		boolean isSuperkey = false;
		for(Key key : keys) {
			if(oneKey.contains(key)) {
				isSuperkey = true;
				break;
			}
		}
		return isSuperkey;
	}
	
	public static Map<String, Set<Set<String>>> computeMaximalSet(List<String> R, List<FD> Sigma) {
		Map<String, Set<Set<String>>> maxSetMap = new HashMap<>();
		for(String A : R) {
			Set<String> s = new HashSet<>(R);
			s.remove(A);//R - A
			Set<Set<String>> cands = new HashSet<>();
			cands.add(s);
			maxSetMap.put(A, cands);
		}
		
		List<FD> Theta = new ArrayList<>();// initialize theta
		
		for(FD X_A : Sigma) {
			Theta.add(X_A);//Theta U X -> A
			
			Map<String, Set<Set<String>>> maxSetMapTemp = new HashMap<>();
			for(String C : R) {
				Set<Set<String>> maxC = maxSetMap.get(C);
				Set<Set<String>> nextMaxC = deepCopy(maxC);
				for(Set<String> W : maxC) {
					if(!mtest(W, C, R, Theta)) {
						nextMaxC.remove(W);//nmax(C) - {W}
						for(String B : X_A.getLeftHand()) {//B \in X
							for(Set<String> Z : maxSetMap.get(B)) {//Z \in max(B)
								Set<String> WIntersectZ = intersect(W,Z);
								if(mtest(WIntersectZ, C, R, Theta)) {
									nextMaxC.add(WIntersectZ);
								}
							}
						}
					}
				}
				maxSetMapTemp.put(C, nextMaxC);
			}
			for(String C : R) {
				maxSetMap.put(C, maxSetMapTemp.get(C));
			}
		}
		
		return maxSetMap;
	}
	
	public static Set<Key> computeCandKeySet4TopDown(String traversal, List<Key> MinedKeys, Set<Key> CandKeySet, Set<Key> NonCandKeySet) {
		for(Key nonKey : NonCandKeySet) {//remove
			CandKeySet.remove(nonKey);
		}
		
		for(Key key : MinedKeys) {//refine
			CandKeySet.remove(key);
			
			if(traversal.equals("dfs")) {//additional refine for dfs
				Set<Key> superKeys = new HashSet<>();
				for(Key candKey : CandKeySet) {
					if(candKey.contains(key)) {
						superKeys.add(candKey);
					}
				}
				
				for(Key superKey : superKeys) {
					CandKeySet.remove(superKey);
					for(String e : key.getAttributes()) {
						Set<String> newCandKey = new HashSet<>(superKey.getAttributes());
						newCandKey.remove(e);
						CandKeySet.add(new Key(newCandKey));
					}
				}
			}
			
			for(String e : key.getAttributes()) {
				Set<String> newCandKey = new HashSet<>(key.getAttributes());
				newCandKey.remove(e);
				CandKeySet.add(new Key(newCandKey));
			}
		}
		
		
		return CandKeySet;
	}
	
	
	public static Set<Key> computeCandKeySet4BottomUp(List<String> R, String traversal, List<Key> MinedKeys, Set<Key> CandKeySet, Set<Key> NonCandKeySet) {
		CandKeySet.removeAll(MinedKeys);//remove
		
		for(Key nonKey : NonCandKeySet) {//extend
			CandKeySet.remove(nonKey);//remove non-keys
			
			Set<String> diff = new HashSet<>(R);
			diff.removeAll(nonKey.getAttributes());
			
			for(String e : diff) {//extend current level into next level based on current non-key
				Set<String> newCandSet = new HashSet<>(nonKey.getAttributes());
				newCandSet.add(e);
				Key newCandKey = new Key(newCandSet);
				
				if(traversal.equals("dfs")) {
					if(!isSuperkeyOfSomeKeys(MinedKeys, newCandKey))//judge if new key is a super key of some mined keys
						CandKeySet.add(newCandKey);
				}else
					CandKeySet.add(newCandKey);
			}
		}
		
		return CandKeySet;
	}
	
	
	/**
	 * create a relation that contains 2 tuples with a specific ratio of columns having matching values
	 * @param tableName
	 * @param R
	 * @param sameValueRatio
	 * @throws SQLException
	 */
	public static void createRelationToDB(String tableName, List<String> R, double sameValueRatio) throws SQLException {
		DBUtils.dropTable(tableName);
		DBUtils.createTableWithouID(tableName, R);
		
		List<List<String>> r = new ArrayList<>();//relation to be inserted into db
		List<String> firstTuple = new ArrayList<>();
		Random rand = new Random();
		for(int i = 0;i < R.size();i ++) {
			firstTuple.add("0");
		}
		r.add(firstTuple);
		//same value ratio
		List<String> sameValueAttrs = new ArrayList<>(R);
		while(sameValueAttrs.size() > (int)(sameValueRatio*R.size())) {
			int idx = rand.nextInt(sameValueAttrs.size());
			sameValueAttrs.remove(idx);
		}
		List<String> secondTuple = new ArrayList<>();
		for(int i = 0;i < R.size();i ++) {
			if(sameValueAttrs.contains(R.get(i)))
				secondTuple.add(firstTuple.get(i));
			else
				secondTuple.add((Integer.parseInt(firstTuple.get(i))+1)+"");
		}
		r.add(secondTuple);
		DBUtils.insertDataWithoutID("freeman", tableName, r);
	}
	
	public static void printSampleRelation(List<String> R, Collection<String> set) {
		String col = "";
		for(int i = 0;i < R.size();i ++) {
			if(i != R.size()-1)
				col += R.get(i)+" | ";
			else
				col += R.get(i);
		}
		String line = "+";
		for(int i = 0;i < col.length();i ++) {
			line += "-";
		}
		line += "+";
		System.out.println(line);
		System.out.println(col);
		System.out.println(line);
		String firstRow = "";
		for(int i = 0;i < R.size();i ++) {
			String attr = R.get(i);
			int charNum = attr.length();
			String space = "";
			for(int j = 0;j < charNum;j ++) {
				space += " ";
			}
			if(i != R.size()-1)
				firstRow += "0"+space+"| ";
			else
				firstRow += "0";
		}
		System.out.println(firstRow);
		System.out.println(line);
		String secRow = "";
		for(int i = 0;i < R.size();i ++) {
			String attr = R.get(i);
			int charNum = attr.length();
			String space = "";
			for(int j = 0;j < charNum;j ++) {
				space += " ";
			}
			if(i != R.size()-1) {
				secRow += set.contains(attr) ? "0"+space+"| " : "1"+space+"| ";
			}else {
				secRow += set.contains(attr) ? "0" : "1";
			}
		}
		System.out.println(secRow);
		System.out.println(line);
	}
	
	public static List<List<Integer>> genArmstrongRelation(List<String> R, List<FD> FDs){
		Map<String, Set<Set<String>>> maxSetMap = computeMaximalSet(R, FDs);
		List<List<Integer>> armstrongRel = new ArrayList<>();
		List<Integer> firstRow = new ArrayList<>();
		for(int i = 0;i < R.size();i ++) {
			firstRow.add(0);
		}
		armstrongRel.add(firstRow);
		for(Map.Entry<String, Set<Set<String>>> entry : maxSetMap.entrySet()) {
			Set<Set<String>> maxSets = entry.getValue();
			for(Set<String> maxSet : maxSets) {//keep same value with corresponding value of last row if the attribute in maximal set, keep different value otherwise
				List<Integer> nextRow = new ArrayList<>();
				for(int i = 0;i < R.size();i ++) {
					String attr = R.get(i);
					List<Integer> lastRow = armstrongRel.get(armstrongRel.size() - 1);
					int lastValue = lastRow.get(i);
					if(maxSet.contains(attr))
						nextRow.add(lastValue);
					else
						nextRow.add(lastValue + 1);
				}
				armstrongRel.add(nextRow);
			}
		}
		return armstrongRel;
	}
	
	public static void printRelation(List<String> R, List<List<Integer>> relation) {
		String col = "";
		for(int i = 0;i < R.size();i ++) {
			if(i != R.size()-1)
				col += R.get(i)+" | ";
			else
				col += R.get(i);
		}
		String line = "+";
		for(int i = 0;i < col.length();i ++) {
			line += "-";
		}
		line += "+";
		System.out.println(line);
		System.out.println(col);
		System.out.println(line);
		for(List<Integer> row : relation) {
			String l = "";
			for(int i = 0;i < R.size();i ++) {
				String attr = R.get(i);
				int charNum = attr.length();
				String space = "";
				for(int j = 0;j < charNum;j ++) {
					space += " ";
				}
				if(i != R.size()-1)
					l += row.get(i)+space+"| ";
				else
					l += row.get(i);
			}
			System.out.println(l);
			System.out.println(line);
		}
	}
	
	public static boolean isEmptyValue(Set<Key> keySet) {
		boolean isAllEmpty = true;
		for(Key key : keySet) {
			if(!key.getAttributes().isEmpty()) {
				isAllEmpty = false;
				break;
			}
		}
		return isAllEmpty;
	}
	
	public static boolean isSuperKey(Key candidateKey, List<Key> minimalKeys) {
        for (Key minKey : minimalKeys) {
            if (candidateKey.getAttributes().containsAll(minKey.getAttributes())) {
                return true;
            }
        }
        return false;
    }
	
	/**
	 * 
	 * @param interview topdown/bottomup
	 * @param traversal dfs/bfs
	 * @param R
	 * @param givenFDs
	 * @param print
	 * @return mined FDs
	 */
	public static List<Object> interview(String interview, String traversal, List<String> R, List<FD> givenFDs, boolean print) {
		if(print)
			System.out.println("Given schema: "+R.toString());
		
		Set<Key> candKeySet = new HashSet<>();
		if(interview.equals("topdown")) {
			candKeySet.add(new Key(R));//R
		}else if(interview.equals("bottomup")){
			for(String A : R) {
				candKeySet.add(new Key(Arrays.asList(A)));
			}
		}else
			new Exception("Not supported interview strategy! (topdown/bottomup only)");
		
		int num4NOAnswers = 0;//number of "No" answers
		int num4AllAnswers = 0;//all question number
		Set<Key> minedKeys = new HashSet<>();//mined FDs
		int round = 0;
		
		while(true) {
			round ++;
			
			if(print) {
				System.out.println("\n***************Round "+ round +"***************");
				System.out.println("current candidate key sets num: " + candKeySet.size());
				System.out.println("current candidate key sets:");
				for(Key candKey : candKeySet) {
					System.out.println("candidate key: "+candKey.toString());
				}
				System.out.println("-----------------------------\n");
			}
			
			Set<Key> interviewedNonKeys = new HashSet<>();
			List<Key> minedKeysInTheRound = new ArrayList<>();//mined keys in the current round
			flag:
			for(Key candKey : candKeySet) {		
				if(print) {
					System.out.println("Interviewing \""+candKey.toString()+"\" if it is a minimal key...\nGiven the sample:");
					Interview.printSampleRelation(R, candKey.getAttributes());
					System.out.println("Is it possible whether there are two records that have values in "+candKey.toString()+" that are matching?");
				}
				
				num4AllAnswers ++;//count all questions
				List<String> attrSetClo = Utils.getAttrSetClosure(candKey.getAttributes(), givenFDs);//the candidate key's attribute closure
				if(attrSetClo.size() == R.size() && attrSetClo.containsAll(R) && R.containsAll(attrSetClo)) {//answer NO iff the Key is implied by given FDs
					minedKeys.add(candKey);
					minedKeysInTheRound.add(candKey);
					num4NOAnswers ++;//count the No answer
					
					if(print) {
						System.out.println("Answer: No");
						System.out.println("current Key in the round:");
						Utils.printKeys(minedKeysInTheRound);
					}
					
					if(traversal.equals("dfs"))
						break flag;
				}else {
					interviewedNonKeys.add(candKey);
					
					if(print)
						System.out.println("Answer: Yes\n");
				}
			}
			
			if(print) {
				System.out.println("Given input FDs:");
				Utils.printFDs(givenFDs);
				System.out.println("The candidate key sets for current round below: ");
				for(Key candKey : candKeySet) {	
					System.out.println(candKey.toString());
				}
				System.out.println("newly mined Keys in this round below: ");
				Utils.printKeys(minedKeysInTheRound);
			}
			
			if(interview.equals("topdown"))//update candidate key set
				candKeySet = computeCandKeySet4TopDown(traversal, minedKeysInTheRound, candKeySet, interviewedNonKeys);
			else if(interview.equals("bottomup"))
				candKeySet = computeCandKeySet4BottomUp(R, traversal, minedKeysInTheRound, candKeySet, interviewedNonKeys);
			
			if(isEmptyValue(candKeySet)) {
				if(print)
					System.out.println("\n\nInterview is finished because candidate key set for next round is empty!\n\n");
				break;
			}else {
				if(print) {
					System.out.println("Based on candidate key set of current round, next round's candidate key set for only newly mined keys: ");
					for(Key candKey : candKeySet) {
						System.out.println("candidate key: "+candKey.toString());
					}
					System.out.println("+++++++++++++++++++++++++++++\n");
				}
			}
		}
		List<Object> res = new ArrayList<>();
		res.add(minedKeys);
		res.add(num4NOAnswers);
		res.add(num4AllAnswers);
		res.add(round);
		return res;
	}
	
	/**
	 * 
	 * @param interview
	 * @param traversal
	 * @param R
	 * @param p is the probability for NO answer
	 * @param print
	 * @return
	 */
	public static List<Object> interview(String interview, String traversal, List<String> R, Double p, boolean print) {
		if(print)
			System.out.println("Given schema: "+R.toString());
		
		Set<Key> candKeySet = new HashSet<>();
		if(interview.equals("topdown")) {
			candKeySet.add(new Key(R));//R
		}else if(interview.equals("bottomup")){
			for(String A : R) {
				candKeySet.add(new Key(Arrays.asList(A)));
			}
		}else
			new Exception("Not supported interview strategy! (topdown/bottomup only)");
		
		int num4NOAnswers = 0;//number of "No" answers
		int num4AllAnswers = 0;//all question number
		Set<Key> minedKeys = new HashSet<>();//mined FDs
		int round = 0;
		Random rand = new Random();
		
		while(true) {
			round ++;
			
			if(print) {
				System.out.println("\n***************Round "+ round +"***************");
				System.out.println("current candidate key sets num: " + candKeySet.size());
				System.out.println("current candidate key sets:");
				for(Key candKey : candKeySet) {
					System.out.println("candidate key: "+candKey.toString());
				}
				System.out.println("-----------------------------\n");
			}
			
			Set<Key> interviewedNonKeys = new HashSet<>();
			List<Key> minedKeysInTheRound = new ArrayList<>();//mined keys in the current round
			flag:
			for(Key candKey : candKeySet) {		
				if(print) {
					System.out.println("Interviewing \""+candKey.toString()+"\" if it is a minimal key...\nGiven the sample:");
					Interview.printSampleRelation(R, candKey.getAttributes());
					System.out.println("Is it possible whether there are two records that have values in "+candKey.toString()+" that are matching?");
				}
				
				num4AllAnswers ++;//count all questions
				if(rand.nextDouble(1.0) < p) {//p is the probability for NO answer
					minedKeys.add(candKey);
					minedKeysInTheRound.add(candKey);
					num4NOAnswers ++;//count the No answer
					
					if(print) {
						System.out.println("Answer: No");
						System.out.println("current Key in the round:");
						Utils.printKeys(minedKeysInTheRound);
					}
					
					if(traversal.equals("dfs"))
						break flag;
				}else {
					interviewedNonKeys.add(candKey);
					
					if(print)
						System.out.println("Answer: Yes\n");
				}
			}
			
			if(print) {
				System.out.println("The candidate key sets for current round below: ");
				for(Key candKey : candKeySet) {	
					System.out.println(candKey.toString());
				}
				System.out.println("newly mined Keys in this round below: ");
				Utils.printKeys(minedKeysInTheRound);
			}
			
			if(interview.equals("topdown"))//update candidate key set
				candKeySet = computeCandKeySet4TopDown(traversal, minedKeysInTheRound, candKeySet, interviewedNonKeys);
			else if(interview.equals("bottomup"))
				candKeySet = computeCandKeySet4BottomUp(R, traversal, minedKeysInTheRound, candKeySet, interviewedNonKeys);
			
			if(isEmptyValue(candKeySet)) {
				if(print)
					System.out.println("\n\nInterview is finished because candidate key set for next round is empty!\n\n");
				break;
			}else {
				if(print) {
					System.out.println("Based on candidate key set of current round, next round's candidate key set for only newly mined keys: ");
					for(Key candKey : candKeySet) {
						System.out.println("candidate key: "+candKey.toString());
					}
					System.out.println("+++++++++++++++++++++++++++++\n");
				}
			}
		}
		List<Object> res = new ArrayList<>();
		res.add(minedKeys);
		res.add(num4NOAnswers);
		res.add(num4AllAnswers);
		res.add(round);
		return res;
	}
	
	public static List<Object> interview(String interview, String traversal, List<String> R, boolean print, List<Key> givenMinimalKeys) {
		if(print)
			System.out.println("Given schema: "+R.toString());
		
		Set<Key> candKeySet = new HashSet<>();
		if(interview.equals("topdown")) {
			candKeySet.add(new Key(R));//R
		}else if(interview.equals("bottomup")){
			for(String A : R) {
				candKeySet.add(new Key(Arrays.asList(A)));
			}
		}else
			new Exception("Not supported interview strategy! (topdown/bottomup only)");
		
		int num4NOAnswers = 0;//number of "No" answers
		int num4AllAnswers = 0;//all question number
		Set<Key> minedKeys = new HashSet<>();//mined FDs
		int round = 0;
		
		while(true) {
			round ++;
			
			if(print) {
				System.out.println("\n***************Round "+ round +"***************");
				System.out.println("current candidate key sets num: " + candKeySet.size());
				System.out.println("current candidate key sets:");
				for(Key candKey : candKeySet) {
					System.out.println("candidate key: "+candKey.toString());
				}
				System.out.println("-----------------------------\n");
			}
			
			Set<Key> interviewedNonKeys = new HashSet<>();
			List<Key> minedKeysInTheRound = new ArrayList<>();//mined keys in the current round
			flag:
			for(Key candKey : candKeySet) {		
				if(print) {
					System.out.println("Interviewing \""+candKey.toString()+"\" if it is a minimal key...\nGiven the sample:");
					Interview.printSampleRelation(R, candKey.getAttributes());
					System.out.println("Is it possible whether there are two records that have values in "+candKey.toString()+" that are matching?");
				}
				
				num4AllAnswers ++;//count all questions
				if(isSuperKey(candKey, givenMinimalKeys)) {//candKey is a super key
					minedKeys.add(candKey);
					minedKeysInTheRound.add(candKey);
					num4NOAnswers ++;//count the No answer
					
					if(print) {
						System.out.println("Answer: No");
						System.out.println("current Key in the round:");
						Utils.printKeys(minedKeysInTheRound);
					}
					
					if(traversal.equals("dfs"))
						break flag;
				}else {
					interviewedNonKeys.add(candKey);
					
					if(print)
						System.out.println("Answer: Yes\n");
				}
			}
			
			if(print) {
				System.out.println("The candidate key sets for current round below: ");
				for(Key candKey : candKeySet) {	
					System.out.println(candKey.toString());
				}
				System.out.println("newly mined Keys in this round below: ");
				Utils.printKeys(minedKeysInTheRound);
			}
			
			if(interview.equals("topdown"))//update candidate key set
				candKeySet = computeCandKeySet4TopDown(traversal, minedKeysInTheRound, candKeySet, interviewedNonKeys);
			else if(interview.equals("bottomup"))
				candKeySet = computeCandKeySet4BottomUp(R, traversal, minedKeysInTheRound, candKeySet, interviewedNonKeys);
			
			if(isEmptyValue(candKeySet)) {
				if(print)
					System.out.println("\n\nInterview is finished because candidate key set for next round is empty!\n\n");
				break;
			}else {
				if(print) {
					System.out.println("Based on candidate key set of current round, next round's candidate key set for only newly mined keys: ");
					for(Key candKey : candKeySet) {
						System.out.println("candidate key: "+candKey.toString());
					}
					System.out.println("+++++++++++++++++++++++++++++\n");
				}
			}
		}
		List<Object> res = new ArrayList<>();
		res.add(minedKeys);
		res.add(num4NOAnswers);
		res.add(num4AllAnswers);
		res.add(round);
		return res;
	}
}
