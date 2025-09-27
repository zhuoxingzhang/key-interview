package exp;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import entity.FD;
import entity.Key;
import entity.Parameter;
import util.Utils;

public class statMinMaxSize4MinimalKeys {
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
	
//	public static List<Double> computeKeyStats(List<Key> trueMinimalKeys) {
//        if (trueMinimalKeys == null || trueMinimalKeys.isEmpty()) {
//            throw new IllegalArgumentException("Key list is empty!");
//        }
//
//        int minSize = Integer.MAX_VALUE;
//        int maxSize = Integer.MIN_VALUE;
//        double sum = 0.0;
//
//        for (Key key : trueMinimalKeys) {
//            int size = key.size();  
//            minSize = Math.min(minSize, size);
//            maxSize = Math.max(maxSize, size);
//            sum += size;
//        }
//
//        double mean = sum / trueMinimalKeys.size();
//
//        double varianceSum = 0.0;
//        for (Key key : trueMinimalKeys) {
//            int size = key.size();
//            varianceSum += Math.pow(size - mean, 2);
//        }
//        double variance = varianceSum / trueMinimalKeys.size();
//        
//        List<Double> res = new ArrayList<>();
//        res.add((double)minSize);
//        res.add((double)maxSize);
//        res.add(variance);
//        return res;
//    }
	
	public static String computeKeyStatsString(List<Key> trueMinimalKeys) {
	    if (trueMinimalKeys == null || trueMinimalKeys.isEmpty()) {
	        throw new IllegalArgumentException("Key list is empty!");
	    }

	    int n = trueMinimalKeys.size();
	    double sum = 0.0;

	    for (Key key : trueMinimalKeys) {
	        sum += key.size();
	    }

	    double mean = sum / n;

	    double varianceSum = 0.0;
	    for (Key key : trueMinimalKeys) {
	        varianceSum += Math.pow(key.size() - mean, 2);
	    }
	    double variance = varianceSum / n;
	    double stdDev = Math.sqrt(variance);

	    
	    DecimalFormat df = new DecimalFormat("0.00");
	    return df.format(mean) + " ± " + df.format(stdDev);
	}
	
	public static void runExp(String path, String givenFDCoverType, Parameter para, double k) {
		List<Object> info = Utils.getFDCover(givenFDCoverType, para);
		if(info == null)//not exist corresponding cover
			return;
		List<String> R = (List<String>) info.get(0);
		List<FD> cover = (List<FD>) info.get(1);
		
		//get top k FDs of redundancy
		List<Object> results = getTopKFDs4LHS(k, cover);
		List<FD> topKFDs = (List<FD>) results.get(0);
		
		int FDSize = Utils.compFDAttrSymbNum(topKFDs);
		
		List<Key> trueMinimalKeys = Utils.getMinimalKeys(R, topKFDs);
		int minKeyAttrSymNum = Utils.compKeyAttrSymbNum(trueMinimalKeys);
		
		String stat = computeKeyStatsString(trueMinimalKeys);
		
		String output = para.dataset.name+","+R.size()+","+(int)(k*100)+"%,"+topKFDs.size()+","+FDSize+","
				+trueMinimalKeys.size()+","+minKeyAttrSymNum+","+stat;
		Utils.writeContent(Arrays.asList(output), path, true);
	}
	
	public static void main(String[] args) {
		int repeat = 1;
		List<Double> topKList = Arrays.asList(0.2, 0.4, 0.6, 0.8, 1.0);//0.2, 0.4, 0.6, 0.8, 1.0; top 20%, 40%, 60%, 80%, 100%
		int max_col_num = 20;
		String root = "";
		String outputPath = root + "\\Exp Results\\key_stat_rw_lhs.csv";
		List<Parameter> paras = Utils.getParameterListV1(null);
		paras.sort(new Comparator<Parameter>() {
			@Override
			public int compare(Parameter o1, Parameter o2) {
				return o1.dataset.col_num - o2.dataset.col_num;
			}
		});
		for(Parameter para : paras) {
			if(para.dataset.col_num > 20)
				continue;
			
			
			for(String coverType : Arrays.asList("reduced minimal")) {//"reduced minimal","optimal"
				for(double topK : topKList) {
					System.out.println("\n" + para.dataset.name + " | " + topK + " | "+ coverType);
					runExp(outputPath, coverType, para, topK);
				}
			}
		}

	}


}
