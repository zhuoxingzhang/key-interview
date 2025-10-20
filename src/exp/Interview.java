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
	
	public static boolean isTwoKeySetsEqual(List<Key> keySet1, List<Key> keySet2) {
		Set<Key> minKeys1 = Utils.refineToMinimalKeys(keySet1);
		Set<Key> minKeys2 = Utils.refineToMinimalKeys(keySet2);
		if(minKeys1.containsAll(minKeys2) && minKeys2.containsAll(minKeys1))
			return true;
		return false;
		
	}
	
	public static boolean isSubsetOfAnySupersets(Collection<Key> sets, Key oneSet) {
		boolean isSubset = false;
		for(Key set : sets) {
			if(set.getAttributes().containsAll(oneSet.getAttributes())) {
				isSubset = true;
				break;
			}
		}
		return isSubset;
	}
	
	public static List<Key> computeCandKeySet4TopDown(String traversalDirection, List<Key> MinedKeys, List<Key> CandKeySet, Set<Key> NonKeySet, Set<Key> allNonKeySet, Set<Key> allMinedKeySet, List<Key> preDeterminedKeys) {
		CandKeySet.removeAll(NonKeySet);
		CandKeySet.removeAll(MinedKeys);
		
		ArrayList<Key> tmp_MinedKeys = new ArrayList<>(MinedKeys);
		MinedKeys.clear();
		NonKeySet.clear();
		
		for(Key key : tmp_MinedKeys) {//refine
			if("dfs".equals(traversalDirection)) {//if candidate set is super set of a key, refine
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
						Key CK = new Key(newCandKey);
						if(!isSubsetOfAnySupersets(allNonKeySet, CK) && !CandKeySet.contains(CK) && !allMinedKeySet.contains(CK)) {
							if(!isSuperkeyOfSomeKeys(preDeterminedKeys, CK) && !isSubsetOfAnySupersets(preDeterminedKeys, CK)) {
								CandKeySet.add(CK);
							}else if(isSuperkeyOfSomeKeys(preDeterminedKeys, CK)){
								MinedKeys.add(CK);
								allMinedKeySet.add(CK);
							}
						}
					}
				}
			}
			
			for(String e : key.getAttributes()) {//refine key
				Set<String> newCandKey = new HashSet<>(key.getAttributes());
				newCandKey.remove(e);
				Key CK = new Key(newCandKey);
				if(!isSubsetOfAnySupersets(allNonKeySet, CK) && !CandKeySet.contains(CK) && !allMinedKeySet.contains(CK)){//the set is not any subset of Yes-answer sets
					if(!isSuperkeyOfSomeKeys(preDeterminedKeys, CK) && !isSubsetOfAnySupersets(preDeterminedKeys, CK)) {
						CandKeySet.add(CK);
					}else if(isSuperkeyOfSomeKeys(preDeterminedKeys, CK)) {
						MinedKeys.add(CK);
						allMinedKeySet.add(CK);
					}
				}
			}
		}
		if("dfs".equals(traversalDirection))
			CandKeySet.sort(Comparator.comparingInt(Key::size));//increasing order
		return CandKeySet;
	}
	
	
	public static List<Key> computeCandKeySet4BottomUp(List<String> R, String traversalDirection, List<Key> MinedKeys, List<Key> CandKeySet, Set<Key> NonKeySet, Set<Key> allNonKeySet, Set<Key> allMinedKeySet, List<Key> preDeterminedKeys) {
		CandKeySet.removeAll(MinedKeys);//remove keys
		CandKeySet.removeAll(NonKeySet);//remove non-keys
		
		ArrayList<Key> tmp_NonKeySet = new ArrayList<>(NonKeySet);
		MinedKeys.clear();
		NonKeySet.clear();
		
		for(Key nonKey : tmp_NonKeySet) {//extend
			if("dfs".equals(traversalDirection)) {//if candidate set is subset of a non-key, extend
				Set<Key> subSets = new HashSet<>();//subset of non-keys
				for(Key candKey : CandKeySet) {
					if(nonKey.contains(candKey)) {
						subSets.add(candKey);
					}
				}
				for(Key subSet : subSets) {
					CandKeySet.remove(subSet);
					Set<String> diff = new HashSet<>(R);
					diff.removeAll(nonKey.getAttributes());
					for(String e : diff) {
						Set<String> newCandSet = new HashSet<>(subSet.getAttributes());
						newCandSet.add(e);
						Key CK = new Key(newCandSet);
						if(!isSuperkeyOfSomeKeys(allMinedKeySet, CK) && !CandKeySet.contains(CK) && !allNonKeySet.contains(CK)) {
							if(!isSubsetOfAnySupersets(preDeterminedKeys, CK))
								CandKeySet.add(CK);
							else {
								NonKeySet.add(CK);
								allNonKeySet.add(CK);
							}
						}
					}
				}
			}
			
			Set<String> diff = new HashSet<>(R);
			diff.removeAll(nonKey.getAttributes());
			for(String e : diff) {//extend current non-keys
				Set<String> newCandSet = new HashSet<>(nonKey.getAttributes());
				newCandSet.add(e);
				Key CK = new Key(newCandSet);
				if(!isSuperkeyOfSomeKeys(allMinedKeySet, CK) && !CandKeySet.contains(CK) && !allNonKeySet.contains(CK)) {
					if(!isSubsetOfAnySupersets(preDeterminedKeys, CK))
						CandKeySet.add(CK);
					else {
						NonKeySet.add(CK);
						allNonKeySet.add(CK);
					}
				}
			}
		}
		if("dfs".equals(traversalDirection))
			CandKeySet.sort(Comparator.comparingInt(Key::size).reversed());//decreasing order
		return CandKeySet;
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
	
	public static boolean isEmptyValue(Collection<Key> keySet) {
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
	
	public static  List<Key> genInitialCandidateKeysWithInputKeys(List<String> R, String traversalStart, List<Key> inputKeys, Set<Key> nonKeyInTheRound, List<Key> minedKeysInTheRound, Set<Key> allMineKeys, List<Key> initialCandKeys){
        nonKeyInTheRound.clear();
        minedKeysInTheRound.clear();
        if(traversalStart.equals("topdown")) {
            if(!inputKeys.isEmpty()){
                List<Key> candKeys = new ArrayList<>();
                List<Key> allSets = new ArrayList<>();
                for(Key set : initialCandKeys){
                    for(String a : set.getAttributes()){
                        Set<String> candK = new HashSet<>(set.getAttributes());
                        candK.remove(a);
                        Key CK = new Key(candK);
                        if(!allSets.contains(CK))
                            allSets.add(CK);
                        if(!isSuperkeyOfSomeKeys(allMineKeys, CK) && !candKeys.contains(CK))//If not a superkey
                            candKeys.add(CK);
                    }
                }
                minedKeysInTheRound.addAll(allSets);
                if(candKeys.isEmpty())
                    return genInitialCandidateKeysWithInputKeys(R, traversalStart, inputKeys, nonKeyInTheRound, minedKeysInTheRound, allMineKeys, allSets);
                return candKeys;
            }else
                return initialCandKeys;
        }else if(traversalStart.equals("bottomup")){
            if(!inputKeys.isEmpty()){
                List<Key> candKeys = new ArrayList<>();
                List<Key> allSets = new ArrayList<>();
                for(Key set : initialCandKeys){//extend
                    List<String> diff = new ArrayList<>(R);
                    diff.removeAll(set.getAttributes());
                    for(String a : diff){
                        Set<String> candK = new HashSet<>(set.getAttributes());
                        candK.add(a);
                        Key CK = new Key(candK);
                        if(!allSets.contains(CK))
                            allSets.add(CK);
                        if(!isSubsetOfAnySupersets(allMineKeys, CK) && !candKeys.contains(CK))//If not a superkey
                            candKeys.add(CK);
                    }
                }
                nonKeyInTheRound.addAll(allSets);
                if(candKeys.isEmpty())
                    return genInitialCandidateKeysWithInputKeys(R, traversalStart, inputKeys, nonKeyInTheRound, minedKeysInTheRound, allMineKeys, allSets);
                return candKeys;
            }else
                return initialCandKeys;
        }else
            new Exception("Not supported interview strategy! (topdown/bottomup only)");
        return null;
    }
	
	public static List<Object> interview(String traversalStart, String traversalDirection, List<String> R, Double p, boolean print) {
		if(print)
			System.out.println("Given schema: "+R.toString());
		
		List<Key> candKeySet = new ArrayList<>();
		if(traversalStart.equals("topdown")) {
			candKeySet.add(new Key(R));//R
		}else if(traversalStart.equals("bottomup")){
			candKeySet.add(new Key(new ArrayList<>()));
		}else
			new Exception("Not supported interview strategy! (topdown/bottomup only)");
		
		int num4NOAnswers = 0;//number of "No" answers
		int num4AllAnswers = 0;//all question number
		Set<Key> allMinedKeys = new HashSet<>();//mined Keys
		Set<Key> allNonKeys = new HashSet<>();
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
			
			Set<Key> NonKeys = new HashSet<>();
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
					allMinedKeys.add(candKey);
					minedKeysInTheRound.add(candKey);
					num4NOAnswers ++;//count the No answer
					
					if(print) {
						System.out.println("Answer: No");
						System.out.println("current Key in the round:");
						Utils.printKeys(minedKeysInTheRound);
					}
					
					if(traversalStart.equals("topdown") && traversalDirection.equals("dfs"))
						break flag;
				}else {
					NonKeys.add(candKey);
					allNonKeys.add(candKey);
					
					if(print)
						System.out.println("Answer: Yes\n");
					
					if(traversalStart.equals("bottomup") && traversalDirection.equals("dfs"))
						break flag;
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
			
			if(traversalStart.equals("topdown"))//update candidate key set
				candKeySet = computeCandKeySet4TopDown(traversalDirection, minedKeysInTheRound, candKeySet, NonKeys, allNonKeys, allMinedKeys, new ArrayList<>());
			else if(traversalStart.equals("bottomup"))
				candKeySet = computeCandKeySet4BottomUp(R, traversalDirection, minedKeysInTheRound, candKeySet, NonKeys, allNonKeys, allMinedKeys, new ArrayList<>());
			
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
		res.add(allMinedKeys);
		res.add(num4NOAnswers);
		res.add(num4AllAnswers);
		res.add(round);
		return res;
	}
	
	public static List<Object> interview(String traversalStart, String traversalDirection, List<String> R, boolean print, List<Key> givenMinimalKeys) {
		if(print)
			System.out.println("Given schema: "+R.toString());
		
		List<Key> candKeySet = new ArrayList<>();
		if(traversalStart.equals("topdown")) {
			candKeySet.add(new Key(R));//R
		}else if(traversalStart.equals("bottomup")){
			candKeySet.add(new Key(new ArrayList<>()));
		}else
			new Exception("Not supported interview strategy! (topdown/bottomup only)");
		
		int num4NOAnswers = 0;//number of "No" answers
		int num4AllAnswers = 0;//all question number
		Set<Key> allMinedKeys = new HashSet<>();//mined Keys
		Set<Key> allNonKeys = new HashSet<>();
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
			
			Set<Key> NonKeys = new HashSet<>();
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
					allMinedKeys.add(candKey);
					minedKeysInTheRound.add(candKey);
					num4NOAnswers ++;//count the No answer
					
					if(print) {
						System.out.println("Answer: No");
						System.out.println("current Key in the round:");
						Utils.printKeys(minedKeysInTheRound);
					}
					
					if(traversalStart.equals("topdown") && traversalDirection.equals("dfs"))
						break flag;
				}else {
					NonKeys.add(candKey);
					allNonKeys.add(candKey);
					
					if(print)
						System.out.println("Answer: Yes\n");
					
					if(traversalStart.equals("bottomup") && traversalDirection.equals("dfs"))
						break flag;
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
			
			if(traversalStart.equals("topdown"))//update candidate key set
				candKeySet = computeCandKeySet4TopDown(traversalDirection, minedKeysInTheRound, candKeySet, NonKeys, allNonKeys, allMinedKeys, new ArrayList<>());
			else if(traversalStart.equals("bottomup"))
				candKeySet = computeCandKeySet4BottomUp(R, traversalDirection, minedKeysInTheRound, candKeySet, NonKeys, allNonKeys, allMinedKeys, new ArrayList<>());
			
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
		res.add(allMinedKeys);
		res.add(num4NOAnswers);
		res.add(num4AllAnswers);
		res.add(round);
		return res;
	}
	
	/**
	 * 
	 * @param traversalStart
	 * @param traversalDirection
	 * @param R
	 * @param print
	 * @param givenMinimalKeys all  minimal keys
	 * @param preDeterminedKeys preDetermined minimal keys which are subset of 'givenMinimalKeys'
	 * @return
	 */
	public static List<Object> interview(String traversalStart, String traversalDirection, List<String> R, boolean print, List<Key> givenMinimalKeys, List<Key> preDeterminedKeys) {
		if(print)
			System.out.println("Given schema: "+R.toString());
		
		List<Key> candKeySet = new ArrayList<>();
		
		int num4NOAnswers = 0;//number of "No" answers
		int num4AllAnswers = 0;//all question number
		Set<Key> allMinedKeys = new HashSet<>(preDeterminedKeys);//mined Keys
		Set<Key> allNonKeys = new HashSet<>();
		Set<Key> NonKeysInTheRound = new HashSet<>();
		List<Key> minedKeysInTheRound = new ArrayList<>();//mined keys in the current round
		int round = 0;
		
		if(traversalStart.equals("topdown")) {
			candKeySet.add(new Key(R));//R
		}else if(traversalStart.equals("bottomup")){
			candKeySet.add(new Key(new ArrayList<>()));
		}else
			new Exception("Not supported interview strategy! (topdown/bottomup only)");
		
		candKeySet = genInitialCandidateKeysWithInputKeys(R, traversalStart, preDeterminedKeys, NonKeysInTheRound, minedKeysInTheRound, allMinedKeys, candKeySet);
		
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
			
			flag:
			for(Key candKey : candKeySet) {		
				if(print) {
					System.out.println("Interviewing \""+candKey.toString()+"\" if it is a key...\nGiven the sample:");
					Interview.printSampleRelation(R, candKey.getAttributes());
					System.out.println("Is it possible whether there are two records that have values in "+candKey.toString()+" that are matching?");
				}
				
				num4AllAnswers ++;//count all questions
				if(isSuperKey(candKey, givenMinimalKeys)) {//candKey is a super key
					allMinedKeys.add(candKey);
					minedKeysInTheRound.add(candKey);
					num4NOAnswers ++;//count the No answer
					
					if(print) {
						System.out.println("Answer: No");
						System.out.println("current Key in the round:");
						Utils.printKeys(minedKeysInTheRound);
					}
					
					if(traversalStart.equals("topdown") && traversalDirection.equals("dfs"))
						break flag;
				}else {
					NonKeysInTheRound.add(candKey);
					allNonKeys.add(candKey);
					
					if(print)
						System.out.println("Answer: Yes\n");
					
					if(traversalStart.equals("bottomup") && traversalDirection.equals("dfs"))
						break flag;
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
			
			if(traversalStart.equals("topdown"))//update candidate key set
				candKeySet = computeCandKeySet4TopDown(traversalDirection, minedKeysInTheRound, candKeySet, NonKeysInTheRound, allNonKeys, allMinedKeys, preDeterminedKeys);
			else if(traversalStart.equals("bottomup"))
				candKeySet = computeCandKeySet4BottomUp(R, traversalDirection, minedKeysInTheRound, candKeySet, NonKeysInTheRound, allNonKeys, allMinedKeys, preDeterminedKeys);
			
			if(isEmptyValue(candKeySet) && NonKeysInTheRound.isEmpty() && minedKeysInTheRound.isEmpty()) {
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
		res.add(allMinedKeys);
		res.add(num4NOAnswers);
		res.add(num4AllAnswers);
		res.add(round);
		return res;
	}
}
