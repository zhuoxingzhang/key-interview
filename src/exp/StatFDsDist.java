package exp;

import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

import entity.FD;
import entity.Parameter;
import util.Utils;

public class StatFDsDist {
    public static Map<Integer, Long> countLHSDistribution(List<FD> fdList) {
        return fdList.stream()
                .collect(Collectors.groupingBy(fd -> fd.getLeftHand().size(), Collectors.counting()));
    }
    
    public static void run(List<FD> fdList, String dataset, String path) {
    	Map<Integer, Long> dist = countLHSDistribution(fdList);
        Map<Integer, Long> sortedDist = new TreeMap<>(dist);
        
        List<String> LHSSize = new ArrayList<>();
        LHSSize.add("lhs size");
        List<String> LHSNum = new ArrayList<>();
        LHSNum.add("fd num");
        for(Map.Entry<Integer, Long> e : sortedDist.entrySet()) {
            LHSSize.add(e.getKey().toString());
            LHSNum.add(e.getValue().toString());
        }
        
        String lhsSizeStr = String.join(",", LHSSize);
        String lhsNumStr = String.join(",", LHSNum);
        
        System.out.println("dataset: " + dataset);
        System.out.println("LHS Dist: " + sortedDist);
        System.out.println(lhsSizeStr);
        System.out.println(lhsNumStr + "\n");
        
        Utils.writeContent(Arrays.asList(dataset, lhsSizeStr, lhsNumStr, "\n\n\n"), path, true);
    }
    
    public static void main(String[] args) {
         String path = "";
         for(Parameter para : Utils.getParameterListV1(null)) {
        	 List<Object> info = Utils.getFDCover("reduced minimal", para);
     		if(info == null)//not exist corresponding cover
     			continue;
     		List<String> R = (List<String>) info.get(0);
     		List<FD> cover = (List<FD>) info.get(1);
 			
     		run(cover, para.dataset.name, path);
 		}
         
    }
}
