package util;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;

import entity.FD;
import entity.Parameter;

import java.io.*;
import java.sql.*;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * compute and save fd redundancy to json file
 *
 */
public class FDRedundancyCalculator {
	
	public static void runExp(String FDCoverType, String outputDir) throws SQLException {
		for(Parameter para : Utils.getParameterListV1(null)) {
			List<Object> info = Utils.getFDCover(FDCoverType, para);
			if(info == null)//not exist corresponding cover
				return;
			List<String> R = (List<String>) info.get(0);
			List<FD> cover = (List<FD>) info.get(1);
			String tableName = DBUtils.getDBTableName(para);
			
			Connection conn = DBUtils.connectDB();//connect DB
			Statement statement = conn.createStatement();
			
			Map<String, Integer> redundancyResults = new HashMap<>();//key: FD string, value: redundancy
			
			for(int i = 0;i < cover.size();i ++) {
				if((i + 1) % 100 == 0 || (i + 1) == cover.size())
					//show progress
					System.out.println((i + 1) + "/" + cover.size() + " | "+  para.dataset.name);
					
				FD fd = cover.get(i);
				List<String> lhs = fd.getLeftHand();
				if(lhs.isEmpty())
					continue;
				List<String> rhs = fd.getRightHand();
				String lhsStr = "";
				for(int j = 0;j < lhs.size();j ++) {
					if(j != lhs.size() - 1)
						lhsStr += "`" + lhs.get(j) + "`, ";
					else
						lhsStr += "`" + lhs.get(j) + "` ";
				}
				String sql = String.format(
                        "SELECT %s, COUNT(*) AS frequency FROM %s " +
                        "GROUP BY %s ORDER BY frequency DESC LIMIT 1",
                        lhsStr, "`"+tableName+"`", lhsStr);
				
                ResultSet resultSet = statement.executeQuery(sql);
                if (resultSet.next()) {
                    int maxFrequency = resultSet.getInt("frequency");
                    redundancyResults.put(String.join(",", lhs) + " -> " + String.join(",", rhs), maxFrequency);
                }
			}
			
			saveResultsToJsonFile(redundancyResults, String.format(outputDir + "\\%s_fd_redundancy(%s).json", para.dataset.name, FDCoverType));//save results
			
			statement.close();
			conn.close();
		}
	}

    /**
     * save results to Json file
     * @param results
     * @param filePath
     */
    private static void saveResultsToJsonFile(Map<String, Integer> results, String filePath) {
    	File file = new File(filePath);
    	try {
            if (!file.exists()) {
                File parentDir = file.getParentFile();
                if (parentDir != null && !parentDir.exists()) {
                    parentDir.mkdirs(); // create dir
                }
                file.createNewFile(); // create file
            }
        } catch (IOException e) {
            System.err.println("Error creating file: " + e.getMessage());
            return;
        }
        JSONArray resultArray = new JSONArray();
        for (Map.Entry<String, Integer> entry : results.entrySet()) {
            JSONObject resultObj = new JSONObject();
            resultObj.put("fd", entry.getKey());
            resultObj.put("redundancy", entry.getValue());
            resultArray.add(resultObj);
        }

        try (BufferedWriter writer = new BufferedWriter(new FileWriter(filePath))) {
            writer.write(JSON.toJSONString(resultArray, SerializerFeature.PrettyFormat));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public static void main(String[] args) throws SQLException {
    	String outputDir = "C:\\Users\\zzha969\\OneDrive - The University of Auckland\\Desktop\\PhD\\Armstrong interviews\\FD Redundancy";
    	runExp("reduced minimal", outputDir);
    }
}
