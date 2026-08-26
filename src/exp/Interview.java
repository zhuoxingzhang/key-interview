package exp;

import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Random;

import entity.Key;
import util.Utils;

public class Interview {
    public static List<Key> computeMaximalAntiKeys(List<String> schema, List<Key> keys) {
        Set<String> schemaSet = new HashSet<>(schema);

        List<Set<String>> hyperEdges = new ArrayList<>();
        for (Key k : keys) {
            Collection<String> attrs = k.getAttributes();
            hyperEdges.add(new HashSet<>(attrs));
        }

        // enumerate minimal hitting sets
        Set<Set<String>> minimalHittingSets = new LinkedHashSet<>();
        Set<String> initialCandidates = new HashSet<>(schemaSet);
        mmcs(initialCandidates, hyperEdges, new HashSet<>(), minimalHittingSets);

        // minimal hitting sets -> maximal anti-keys
        List<Key> result = new ArrayList<>();
        for (Set<String> hs : minimalHittingSets) {
            Set<String> anti = new HashSet<>(schemaSet);
            anti.removeAll(hs);
            result.add(new Key(new ArrayList<>(anti)));
        }
        return result;
    }

    /**
     * MMCS 递归：生成 minimal hitting sets
     * candidates: 当前可以使用的属性（通常初始为 schema 全集）
     * hyperEdges: 每个 edge 是一个 Set<String>（即一个 key）
     * current: 当前构造的 hitting set
     * output: collect minimal hitting sets
     */
    private static void mmcs(Set<String> candidates, List<Set<String>> hyperEdges,
                             Set<String> current, Set<Set<String>> output) {
        // 检查 current 是否已经击中所有超边
        boolean allHit = true;
        for (Set<String> edge : hyperEdges) {
            if (Collections.disjoint(current, edge)) {
                allHit = false;
                break;
            }
        }

        if (allHit) {
            // 仅当 current 为 minimal hitting set 时加入
            if (isMinimal(current, hyperEdges)) {
                output.add(new HashSet<>(current));
            }
            return;
        }

        // 选择一个未被 current 击中的超边（启发式：选最小的）
        Set<String> edgeToCover = null;
        for (Set<String> edge : hyperEdges) {
            if (Collections.disjoint(current, edge)) {
                if (edgeToCover == null || edge.size() < edgeToCover.size()) {
                    edgeToCover = edge;
                }
            }
        }
        if (edgeToCover == null) return;

        // 对 edgeToCover 中的每个属性尝试扩展
        // 避免 ConcurrentModification：对 edgeToCover 的快照进行迭代
        for (String attr : new ArrayList<>(edgeToCover)) {
            if (!candidates.contains(attr)) continue;

            Set<String> newCurrent = new HashSet<>(current);
            newCurrent.add(attr);

            Set<String> newCandidates = new HashSet<>(candidates);
            newCandidates.remove(attr);

            mmcs(newCandidates, hyperEdges, newCurrent, output);
        }
    }

    /**
     * 判断 hitting set 是否 minimal（移除任一元素将不再是 hitting set）
     */
    private static boolean isMinimal(Set<String> S, List<Set<String>> hyperEdges) {
        for (String a : S) {
            Set<String> reduced = new HashSet<>(S);
            reduced.remove(a);
            if (isHittingSet(reduced, hyperEdges)) {
                return false; // 有子集仍然是 hitting set -> 不是 minimal
            }
        }
        return true;
    }

    private static boolean isHittingSet(Set<String> set, List<Set<String>> hyperEdges) {
        for (Set<String> e : hyperEdges) {
            if (Collections.disjoint(set, e)) return false;
        }
        return true;
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
	
	public static Set<Key> refineToMinimalKeys(Collection<Key> keys1){
    	Set<Key> minimalKeys = new HashSet<Key>();
    	List<Key> keys = new ArrayList<>(keys1);
    	keys.sort(Comparator.comparingInt(Key::size));//increasing order
    	
    	for (Key key : keys) {
            boolean isMinimal = true;
            for (Key existing : minimalKeys) {
                if (key.contains(existing)) {
                    isMinimal = false;
                    break;
                }
            }
            if (isMinimal) {
                minimalKeys.add(key);
            }
        }

        return minimalKeys;
    }
	
	public static Set<Key> refineToMaximalNonKeys(Collection<Key> keys1){
    	Set<Key> maximalNonKeys = new HashSet<Key>();
    	List<Key> keys = new ArrayList<>(keys1);
    	keys.sort(Comparator.comparingInt(Key::size).reversed());//decreasing order
    	
    	for (Key key : keys) {
            boolean isMaximal = true;
            for (Key existing : maximalNonKeys) {
                if (existing.contains(key)) {
                	isMaximal = false;
                    break;
                }
            }
            if (isMaximal) {
            	maximalNonKeys.add(key);
            }
        }

        return maximalNonKeys;
    }
	
//	public static List<Key> computeCandKeySet4TopDownOld(String traversalDirection, List<Key> MinedKeys, List<Key> CandKeySet, Set<Key> NonKeySet, Set<Key> allNonKeySet, Set<Key> allMinedKeySet, List<Key> preDeterminedKeys) {
//		CandKeySet.removeAll(NonKeySet);
//		CandKeySet.removeAll(MinedKeys);
//		
//		ArrayList<Key> tmp_MinedKeys = new ArrayList<>(MinedKeys);
//		MinedKeys.clear();
//		NonKeySet.clear();
//		
//		for(Key key : tmp_MinedKeys) {//refine
//			if("dfs".equals(traversalDirection)) {//if candidate set is super set of a key, refine
//				Set<Key> superKeys = new HashSet<>();
//				for(Key candKey : CandKeySet) {
//					if(candKey.contains(key)) {
//						superKeys.add(candKey);
//					}
//				}
//				for(Key superKey : superKeys) {
//					CandKeySet.remove(superKey);
//					for(String e : key.getAttributes()) {
//						Set<String> newCandKey = new HashSet<>(superKey.getAttributes());
//						newCandKey.remove(e);
//						Key CK = new Key(newCandKey);
//						if(!isSubsetOfAnySupersets(allNonKeySet, CK) && !CandKeySet.contains(CK) && !allMinedKeySet.contains(CK)) {
//							if(!isSuperkeyOfSomeKeys(preDeterminedKeys, CK) && !isSubsetOfAnySupersets(preDeterminedKeys, CK)) {
//								CandKeySet.add(CK);
//							}else if(isSuperkeyOfSomeKeys(preDeterminedKeys, CK)){
//								MinedKeys.add(CK);
//								allMinedKeySet.add(CK);
//							}
//						}
//					}
//				}
//			}
//			
//			for(String e : key.getAttributes()) {//refine key
//				Set<String> newCandKey = new HashSet<>(key.getAttributes());
//				newCandKey.remove(e);
//				Key CK = new Key(newCandKey);
//				if(!isSubsetOfAnySupersets(allNonKeySet, CK) && !CandKeySet.contains(CK) && !allMinedKeySet.contains(CK)){//the set is not any subset of Yes-answer sets
//					if(!isSuperkeyOfSomeKeys(preDeterminedKeys, CK) && !isSubsetOfAnySupersets(preDeterminedKeys, CK)) {
//						CandKeySet.add(CK);
//					}else if(isSuperkeyOfSomeKeys(preDeterminedKeys, CK)) {
//						MinedKeys.add(CK);
//						allMinedKeySet.add(CK);
//					}
//				}
//			}
//		}
//		if("dfs".equals(traversalDirection))
//			CandKeySet.sort(Comparator.comparingInt(Key::size));//increasing order
//		return CandKeySet;
//	}
	
	public static List<Key> computeCandKeySet4TopDown(String traversalDirection, List<Key> MinedKeySet, List<Key> CandKeySet, Set<Key> NonKeySet, Set<Key> allNonKeySet, Set<Key> allMinedKeySet, List<Key> preDeterminedKeys) {
		Set<Key> superKeys = new HashSet<>(MinedKeySet);
		for(Key key : MinedKeySet) {
			for(Key candKey : CandKeySet) {
				if(candKey.contains(key)) {
					superKeys.add(candKey);
				}
			}
		}
		CandKeySet.removeAll(superKeys);//remove super keys
		
		Set<Key> subsetNonKeySet = new HashSet<>(NonKeySet);
		for(Key nonKey : NonKeySet) {
			for(Key candKey : CandKeySet) {
				if(nonKey.contains(candKey)) {
					subsetNonKeySet.add(candKey);
				}
			}
		}
		CandKeySet.removeAll(subsetNonKeySet);//remove subsets of non-keys
		
		MinedKeySet.clear();
		NonKeySet.clear();
		
		Set<Key> minKeys = Interview.refineToMinimalKeys(allMinedKeySet);
		allMinedKeySet.clear();
		allMinedKeySet.addAll(minKeys);
		
		Set<Key> maxNonKeys = Interview.refineToMaximalNonKeys(allNonKeySet);
		allNonKeySet.clear();
		allNonKeySet.addAll(maxNonKeys);
		
		for(Key key : superKeys) {
			for(String e : key.getAttributes()) {
				Set<String> newCandKey = new HashSet<>(key.getAttributes());
				newCandKey.remove(e);
				Key CK = new Key(newCandKey);
				if(!isSubsetOfAnySupersets(allNonKeySet, CK) && !CandKeySet.contains(CK) && !isSuperkeyOfSomeKeys(allMinedKeySet, CK) 
						&& !isSubsetOfAnySupersets(preDeterminedKeys, CK)){//interview
					CandKeySet.add(CK);
				}else if(isSuperkeyOfSomeKeys(allMinedKeySet, CK)) {//refine for next round
					MinedKeySet.add(CK);
				}
			}
		}
		if("dfs".equals(traversalDirection))
			CandKeySet.sort(Comparator.comparingInt(Key::size));//increasing order
		return CandKeySet;
	}
	
	
//	public static List<Key> computeCandKeySet4BottomUp(List<String> R, String traversalDirection, List<Key> MinedKeySet, List<Key> CandKeySet, Set<Key> NonKeySet, Set<Key> allNonKeySet, Set<Key> allMinedKeySet, List<Key> preDeterminedKeys) {
//		CandKeySet.removeAll(MinedKeySet);//remove keys
//		CandKeySet.removeAll(NonKeySet);//remove non-keys
//		
//		ArrayList<Key> tmp_NonKeySet = new ArrayList<>(NonKeySet);
//		MinedKeySet.clear();
//		NonKeySet.clear();
//		
//		for(Key nonKey : tmp_NonKeySet) {//extend
//			if("dfs".equals(traversalDirection)) {//if candidate set is subset of a non-key, extend
//				Set<Key> subSets = new HashSet<>();//subset of non-keys
//				for(Key candKey : CandKeySet) {
//					if(nonKey.contains(candKey)) {
//						subSets.add(candKey);
//					}
//				}
//				for(Key subSet : subSets) {
//					CandKeySet.remove(subSet);
//					Set<String> diff = new HashSet<>(R);
//					diff.removeAll(nonKey.getAttributes());
//					for(String e : diff) {
//						Set<String> newCandSet = new HashSet<>(subSet.getAttributes());
//						newCandSet.add(e);
//						Key CK = new Key(newCandSet);
//						if(!isSuperkeyOfSomeKeys(allMinedKeySet, CK) && !CandKeySet.contains(CK) && !allNonKeySet.contains(CK)) {
//							if(!isSubsetOfAnySupersets(preDeterminedKeys, CK))
//								CandKeySet.add(CK);
//							else {
//								NonKeySet.add(CK);
//								allNonKeySet.add(CK);
//							}
//						}
//					}
//				}
//			}
//			
//			Set<String> diff = new HashSet<>(R);
//			diff.removeAll(nonKey.getAttributes());
//			for(String e : diff) {//extend current non-keys
//				Set<String> newCandSet = new HashSet<>(nonKey.getAttributes());
//				newCandSet.add(e);
//				Key CK = new Key(newCandSet);
//				if(!isSuperkeyOfSomeKeys(allMinedKeySet, CK) && !CandKeySet.contains(CK) && !allNonKeySet.contains(CK)) {
//					if(!isSubsetOfAnySupersets(preDeterminedKeys, CK))
//						CandKeySet.add(CK);
//					else {
//						NonKeySet.add(CK);
//						allNonKeySet.add(CK);
//					}
//				}
//			}
//		}
//		if("dfs".equals(traversalDirection))
//			CandKeySet.sort(Comparator.comparingInt(Key::size).reversed());//decreasing order
//		return CandKeySet;
//	}
	
	public static List<Key> computeCandKeySet4BottomUp(List<String> R, String traversalDirection, List<Key> MinedKeySet, List<Key> CandKeySet, Set<Key> NonKeySet, Set<Key> allNonKeySet, Set<Key> allMinedKeySet, List<Key> preDeterminedKeys) {
		Set<Key> superKeys = new HashSet<>(MinedKeySet);
		for(Key key : MinedKeySet) {
			for(Key candKey : CandKeySet) {
				if(candKey.contains(key)) {
					superKeys.add(candKey);
				}
			}
		}
		CandKeySet.removeAll(superKeys);//remove super keys
		
		Set<Key> subsetNonKeySet = new HashSet<>(NonKeySet);
		for(Key nonKey : NonKeySet) {
			for(Key candKey : CandKeySet) {
				if(nonKey.contains(candKey)) {
					subsetNonKeySet.add(candKey);
				}
			}
		}
		CandKeySet.removeAll(subsetNonKeySet);//remove subsets of non-keys
		
		MinedKeySet.clear();
		NonKeySet.clear();
		
		Set<Key> minKeys = Interview.refineToMinimalKeys(allMinedKeySet);
		allMinedKeySet.clear();
		allMinedKeySet.addAll(minKeys);
		
		Set<Key> maxNonKeys = Interview.refineToMaximalNonKeys(allNonKeySet);
		allNonKeySet.clear();
		allNonKeySet.addAll(maxNonKeys);
		
		for(Key nonKey : subsetNonKeySet) {//extend
			Set<String> diff = new HashSet<>(R);
			diff.removeAll(nonKey.getAttributes());
			for(String e : diff) {//extend current non-keys
				Set<String> newCandSet = new HashSet<>(nonKey.getAttributes());
				newCandSet.add(e);
				Key CK = new Key(newCandSet);
				if(!isSuperkeyOfSomeKeys(allMinedKeySet, CK) && !CandKeySet.contains(CK) && !isSubsetOfAnySupersets(allNonKeySet, CK) 
						&& !isSubsetOfAnySupersets(preDeterminedKeys, CK)) {//interview
					CandKeySet.add(CK);
				}else if(isSubsetOfAnySupersets(allNonKeySet, CK) || isSubsetOfAnySupersets(preDeterminedKeys, CK)){//extend for next round
					NonKeySet.add(CK);
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
                        if(!isSuperkeyOfSomeKeys(allMineKeys, CK) && !candKeys.contains(CK) && !isSubsetOfAnySupersets(allMineKeys, CK))//If not a superkey
                            candKeys.add(CK);
                        if(isSuperkeyOfSomeKeys(allMineKeys, CK))
                            minedKeysInTheRound.add(CK);
                    }
                }
                if(candKeys.isEmpty() && allSets.get(0).size() != 0)
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
                        if(!isSubsetOfAnySupersets(allMineKeys, CK) && !candKeys.contains(CK) && !isSuperkeyOfSomeKeys(allMineKeys, CK))//If not a superkey
                            candKeys.add(CK);
                        if(isSubsetOfAnySupersets(allMineKeys, CK))
                            nonKeyInTheRound.add(CK);
                    }
                }
                if(candKeys.isEmpty() && allSets.get(0).size() != R.size())
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

	// ===================== Dualize and Advance (DA) strategy =====================
	// Adapted from the Dualize and Advance algorithm (All_MSS/AMAK) of
	// Gunopulos, Khardon, Mannila, Saluja, Toivonen, Sharma, TODS 28(2), 2003.
	// The interview alternates between (i) dualization: computing the minimal
	// transversals of the complements of all maximal anti-keys found so far,
	// which are exactly the candidate minimal keys not excluded by any answer,
	// and (ii) advancing: extending a candidate answered as anti-key greedily
	// into a maximal anti-key. The interview ends when every candidate is
	// confirmed as a key; the confirmed candidates then form all minimal keys.

	/**
	 * Oracle answering the Boolean interview question for a candidate column set:
	 * true represents the answer "No" (the set is a key),
	 * false represents the answer "Yes" (the set is an anti-key).
	 */
	private interface KeyOracle {
		boolean isKey(Key candidate);
	}

	/**
	 * Dualization step: the candidate minimal keys are exactly the minimal
	 * transversals of the hypergraph whose edges are the complements of the
	 * maximal anti-keys found so far.
	 * With no anti-key found yet the empty set is the only candidate; once the
	 * full schema is an anti-key, no candidate remains (no key holds at all).
	 */
	public static List<Key> computeCandKeySet4Dualize(List<String> R, Collection<Key> maxAntiKeys) {
		List<Set<String>> hyperEdges = new ArrayList<>();
		for (Key antiKey : maxAntiKeys) {
			Set<String> complement = new HashSet<>(R);
			complement.removeAll(antiKey.getAttributes());
			hyperEdges.add(complement);
		}
		Set<Set<String>> minimalTransversals = new LinkedHashSet<>();
		mmcs(new HashSet<>(R), hyperEdges, new HashSet<>(), minimalTransversals);
		List<Key> candKeys = new ArrayList<>();
		for (Set<String> transversal : minimalTransversals) {
			candKeys.add(new Key(transversal));
		}
		candKeys.sort(Comparator.comparingInt(Key::size).thenComparing(k -> {
			List<String> attrs = new ArrayList<>(k.getAttributes());
			Collections.sort(attrs);
			return String.join(",", attrs);
		}));//increasing order, deterministic
		return candKeys;
	}

	/**
	 * Dual dualization step (top-down flavor): the candidate maximal anti-keys
	 * are exactly the complements of the minimal transversals of the hypergraph
	 * whose edges are the minimal keys found so far.
	 * With no key found yet the full schema is the only candidate; once the
	 * empty set is a key, no candidate remains (no anti-key exists).
	 */
	public static List<Key> computeCandKeySet4DualizeTopDown(List<String> R, Collection<Key> minKeys) {
		List<Set<String>> hyperEdges = new ArrayList<>();
		for (Key key : minKeys) {
			hyperEdges.add(new HashSet<>(key.getAttributes()));
		}
		Set<Set<String>> minimalTransversals = new LinkedHashSet<>();
		mmcs(new HashSet<>(R), hyperEdges, new HashSet<>(), minimalTransversals);
		List<Key> candAntiKeys = new ArrayList<>();
		for (Set<String> transversal : minimalTransversals) {
			Set<String> complement = new HashSet<>(R);
			complement.removeAll(transversal);
			candAntiKeys.add(new Key(complement));
		}
		candAntiKeys.sort(Comparator.comparingInt(Key::size).reversed().thenComparing(k -> {
			List<String> attrs = new ArrayList<>(((Key) k).getAttributes());
			Collections.sort(attrs);
			return String.join(",", attrs);
		}));//decreasing order, deterministic
		return candAntiKeys;
	}

	/**
	 * check if a set is a proper subset of some given minimal key,
	 * in which case it is an anti-key by minimality (no question needed)
	 */
	public static boolean isProperSubsetOfSomeKeys(Collection<Key> minimalKeys, Key oneSet) {
		for(Key key : minimalKeys) {
			if(key.contains(oneSet) && key.size() > oneSet.size()) {
				return true;
			}
		}
		return false;
	}

	/**
	 * dispatch by strategy string: "topdown dfs", "topdown bfs", "bottomup dfs", "bottomup bfs",
	 * or a member of the dualize family: "dualize" (= "dualize bottomup dfs"),
	 * "dualize topdown dfs", "dualize topdown bfs", "dualize bottomup dfs", "dualize bottomup bfs"
	 */
	public static List<Object> interviewByStrategy(String strategy, List<String> R, boolean print, List<Key> givenMinimalKeys) {
		if(strategy.startsWith("dualize")) {
			String[] parts = strategy.split(" ");
			if(parts.length >= 3)
				return interviewDualize(parts[1], parts[2], R, print, givenMinimalKeys);
			return interviewDualize(R, print, givenMinimalKeys);
		}
		return interview(strategy.split(" ")[0], strategy.split(" ")[1], R, print, givenMinimalKeys);
	}

	/**
	 * dispatch by strategy string: "topdown dfs", "topdown bfs", "bottomup dfs", "bottomup bfs",
	 * or a member of the dualize family: "dualize" (= "dualize bottomup dfs"),
	 * "dualize topdown dfs", "dualize topdown bfs", "dualize bottomup dfs", "dualize bottomup bfs"
	 */
	public static List<Object> interviewByStrategy(String strategy, List<String> R, boolean print, List<Key> givenMinimalKeys, List<Key> preDeterminedKeys) {
		if(strategy.startsWith("dualize")) {
			String[] parts = strategy.split(" ");
			if(parts.length >= 3)
				return interviewDualize(parts[1], parts[2], R, print, givenMinimalKeys, preDeterminedKeys);
			return interviewDualize(R, print, givenMinimalKeys, preDeterminedKeys);
		}
		return interview(strategy.split(" ")[0], strategy.split(" ")[1], R, print, givenMinimalKeys, preDeterminedKeys);
	}

	public static List<Object> interviewDualize(List<String> R, Double p, boolean print) {
		return interviewDualize("bottomup", "dfs", R, p, print);
	}

	public static List<Object> interviewDualize(List<String> R, boolean print, List<Key> givenMinimalKeys) {
		return interviewDualize("bottomup", "dfs", R, print, givenMinimalKeys);
	}

	/**
	 *
	 * @param R table schema
	 * @param print
	 * @param givenMinimalKeys all minimal keys
	 * @param preDeterminedKeys preDetermined minimal keys which are subset of 'givenMinimalKeys'
	 * @return
	 */
	public static List<Object> interviewDualize(List<String> R, boolean print, List<Key> givenMinimalKeys, List<Key> preDeterminedKeys) {
		return interviewDualize("bottomup", "dfs", R, print, givenMinimalKeys, preDeterminedKeys);
	}

	public static List<Object> interviewDualize(String traversalStart, String traversalDirection, List<String> R, Double p, boolean print) {
		Random rand = new Random();
		return interviewDualize(traversalStart, traversalDirection, R, candKey -> rand.nextDouble(1.0) < p, print, new ArrayList<>());
	}

	public static List<Object> interviewDualize(String traversalStart, String traversalDirection, List<String> R, boolean print, List<Key> givenMinimalKeys) {
		return interviewDualize(traversalStart, traversalDirection, R, candKey -> isSuperKey(candKey, givenMinimalKeys), print, new ArrayList<>());
	}

	public static List<Object> interviewDualize(String traversalStart, String traversalDirection, List<String> R, boolean print, List<Key> givenMinimalKeys, List<Key> preDeterminedKeys) {
		return interviewDualize(traversalStart, traversalDirection, R, candKey -> isSuperKey(candKey, givenMinimalKeys), print, preDeterminedKeys);
	}

	private static List<Object> interviewDualize(String traversalStart, String traversalDirection, List<String> R, KeyOracle oracle, boolean print, List<Key> preDeterminedKeys) {
		if(print)
			System.out.println("Given schema: "+R.toString());

		boolean topdown = traversalStart.equals("topdown");
		if(!topdown && !traversalStart.equals("bottomup"))
			new Exception("Not supported dualize strategy! (topdown/bottomup only)");
		boolean bfs = traversalDirection.equals("bfs");

		int[] counters = new int[2];//[0]: number of "No" answers (keys), [1]: all question number
		Set<Key> allMinedKeys = new HashSet<>(preDeterminedKeys);//known keys ("No" answers and predetermined keys)
		Set<Key> allNonKeys = new HashSet<>();//known anti-keys ("Yes" answers and implied anti-keys)
		Set<Key> maxAntiKeys = new HashSet<>();//maximal anti-keys discovered so far
		Set<Key> minKeysFound = new HashSet<>(preDeterminedKeys);//minimal keys discovered so far
		int round = 0;

		while(true) {
			round ++;
			List<Key> candKeySet = topdown ? computeCandKeySet4DualizeTopDown(R, minKeysFound)
					: computeCandKeySet4Dualize(R, maxAntiKeys);//dualize

			if(print) {
				System.out.println("\n***************Round "+ round +"***************");
				System.out.println("current candidate sets num: " + candKeySet.size());
				System.out.println(topdown ? "current candidate maximal anti-keys (complements of minimal transversals):"
						: "current candidate minimal keys (minimal transversals):");
				for(Key candKey : candKeySet) {
					System.out.println("candidate: "+candKey.toString());
				}
				System.out.println("-----------------------------\n");
			}

			List<Key> counterExamples = new ArrayList<>();
			flag:
			for(Key candKey : candKeySet) {
				if(topdown) {
					if(isSubsetOfAnySupersets(allNonKeys, candKey))//already confirmed as an anti-key
						continue;
					if(isProperSubsetOfSomeKeys(preDeterminedKeys, candKey)) {//maximal anti-key by minimality of an input key
						maxAntiKeys.add(candKey);
						allNonKeys.add(candKey);
						continue;
					}

					if(print) {
						System.out.println("Interviewing \""+candKey.toString()+"\" if it is a maximal anti-key...\nGiven the sample:");
						Interview.printSampleRelation(R, candKey.getAttributes());
						System.out.println("Is it possible whether there are two records that have values in "+candKey.toString()+" that are matching?");
					}

					counters[1] ++;//count all questions
					if(oracle.isKey(candKey)) {//counterexample: a key not containing any known minimal key
						allMinedKeys.add(candKey);
						counters[0] ++;//count the No answer
						counterExamples.add(candKey);

						if(print)
							System.out.println("Answer: No\n");

						if(!bfs)
							break flag;//shrink from the first counterexample
					}else {//candKey is an anti-key, and it is maximal by dualization
						allNonKeys.add(candKey);
						maxAntiKeys.add(candKey);

						if(print)
							System.out.println("Answer: Yes\n");
					}
				}else {
					if(isSuperkeyOfSomeKeys(allMinedKeys, candKey))//already confirmed as a key
						continue;
					if(isProperSubsetOfSomeKeys(preDeterminedKeys, candKey)) {//anti-key by minimality of an input key
						allNonKeys.add(candKey);
						counterExamples.add(candKey);
						if(!bfs)
							break flag;
						continue;
					}

					if(print) {
						System.out.println("Interviewing \""+candKey.toString()+"\" if it is a minimal key...\nGiven the sample:");
						Interview.printSampleRelation(R, candKey.getAttributes());
						System.out.println("Is it possible whether there are two records that have values in "+candKey.toString()+" that are matching?");
					}

					counters[1] ++;//count all questions
					if(oracle.isKey(candKey)) {//candKey is a key, and it is minimal by dualization
						allMinedKeys.add(candKey);
						minKeysFound.add(candKey);
						counters[0] ++;//count the No answer

						if(print)
							System.out.println("Answer: No\n");
					}else {//counterexample: an anti-key not contained in any known maximal anti-key
						allNonKeys.add(candKey);
						counterExamples.add(candKey);

						if(print)
							System.out.println("Answer: Yes\n");

						if(!bfs)
							break flag;//advance from the first counterexample
					}
				}
			}

			if(counterExamples.isEmpty()) {//every candidate is confirmed
				if(print)
					System.out.println("\n\nInterview is finished because every candidate is confirmed!\n\n");
				break;
			}

			for(Key counterExample : counterExamples) {
				if(topdown) {
					if(isSuperkeyOfSomeKeys(minKeysFound, counterExample))//covered by a minimal key found in this batch
						continue;
					Key minKey = shrinkToMinimalKey(counterExample, R, oracle, allMinedKeys, allNonKeys, preDeterminedKeys, counters, print);
					minKeysFound.add(minKey);
					allMinedKeys.add(minKey);

					if(print) {
						System.out.println("New minimal key: "+minKey.toString());
						System.out.println("All minimal keys so far: ");
						for(Key k : minKeysFound) {
							System.out.println(k.toString());
						}
						System.out.println("+++++++++++++++++++++++++++++\n");
					}
				}else {
					if(isSubsetOfAnySupersets(maxAntiKeys, counterExample))//covered by a maximal anti-key found in this batch
						continue;
					Key antiKey = advanceToMaximalAntiKey(counterExample, R, oracle, allMinedKeys, allNonKeys, preDeterminedKeys, counters, print);
					maxAntiKeys.add(antiKey);
					allNonKeys.add(antiKey);

					if(print) {
						System.out.println("New maximal anti-key: "+antiKey.toString());
						System.out.println("All maximal anti-keys so far: ");
						for(Key antiK : maxAntiKeys) {
							System.out.println(antiK.toString());
						}
						System.out.println("+++++++++++++++++++++++++++++\n");
					}
				}
			}
		}

		List<Object> res = new ArrayList<>();
		res.add(allMinedKeys);
		res.add(counters[0]);
		res.add(counters[1]);
		res.add(round);
		return res;
	}

	/** advance an anti-key greedily to a maximal anti-key by adding one column at a time */
	private static Key advanceToMaximalAntiKey(Key counterExample, List<String> R, KeyOracle oracle, Set<Key> allMinedKeys, Set<Key> allNonKeys, List<Key> preDeterminedKeys, int[] counters, boolean print) {
		Set<String> antiKey = new HashSet<>(counterExample.getAttributes());
		if(print)
			System.out.println("Advancing anti-key "+counterExample.toString()+" to a maximal anti-key...");
		for(String e : R) {
			if(antiKey.contains(e))
				continue;
			Set<String> extended = new HashSet<>(antiKey);
			extended.add(e);
			Key EK = new Key(extended);
			if(isSuperkeyOfSomeKeys(allMinedKeys, EK))//implied key: no question needed
				continue;
			if(isSubsetOfAnySupersets(allNonKeys, EK) || isProperSubsetOfSomeKeys(preDeterminedKeys, EK)) {//implied anti-key: no question needed
				antiKey = extended;
				allNonKeys.add(EK);
				continue;
			}

			if(print) {
				System.out.println("Interviewing \""+EK.toString()+"\" if it is an anti-key...\nGiven the sample:");
				Interview.printSampleRelation(R, EK.getAttributes());
				System.out.println("Is it possible whether there are two records that have values in "+EK.toString()+" that are matching?");
			}

			counters[1] ++;//count all questions
			if(oracle.isKey(EK)) {
				allMinedKeys.add(EK);
				counters[0] ++;//count the No answer

				if(print)
					System.out.println("Answer: No\n");
			}else {
				antiKey = extended;
				allNonKeys.add(EK);

				if(print)
					System.out.println("Answer: Yes\n");
			}
		}
		return new Key(antiKey);
	}

	/** shrink a key greedily to a minimal key by removing one column at a time */
	private static Key shrinkToMinimalKey(Key counterExample, List<String> R, KeyOracle oracle, Set<Key> allMinedKeys, Set<Key> allNonKeys, List<Key> preDeterminedKeys, int[] counters, boolean print) {
		Set<String> key = new HashSet<>(counterExample.getAttributes());
		if(print)
			System.out.println("Shrinking key "+counterExample.toString()+" to a minimal key...");
		for(String e : R) {
			if(!key.contains(e))
				continue;
			Set<String> shrunken = new HashSet<>(key);
			shrunken.remove(e);
			Key SK = new Key(shrunken);
			if(isSubsetOfAnySupersets(allNonKeys, SK) || isProperSubsetOfSomeKeys(preDeterminedKeys, SK))//implied anti-key: keep the column
				continue;
			if(isSuperkeyOfSomeKeys(allMinedKeys, SK)) {//implied key: remove the column without a question
				key = shrunken;
				continue;
			}

			if(print) {
				System.out.println("Interviewing \""+SK.toString()+"\" if it is a key...\nGiven the sample:");
				Interview.printSampleRelation(R, SK.getAttributes());
				System.out.println("Is it possible whether there are two records that have values in "+SK.toString()+" that are matching?");
			}

			counters[1] ++;//count all questions
			if(oracle.isKey(SK)) {
				allMinedKeys.add(SK);
				counters[0] ++;//count the No answer
				key = shrunken;

				if(print)
					System.out.println("Answer: No\n");
			}else {
				allNonKeys.add(SK);

				if(print)
					System.out.println("Answer: Yes\n");
			}
		}
		return new Key(key);
	}

}
