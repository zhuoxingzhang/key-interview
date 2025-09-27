package util;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.alibaba.fastjson.JSON;
import com.alibaba.fastjson.JSONArray;
import com.alibaba.fastjson.JSONObject;
import com.alibaba.fastjson.serializer.SerializerFeature;
import com.sun.management.OperatingSystemMXBean;

import entity.DataTypeEnum;
import entity.Dataset;
import entity.FD;
import entity.FuncDepen;
import entity.Key;
import entity.Parameter;
import entity.Schema;


/**
 * some tools here
 *
 */
public class Utils {
	
	/**
	 * through a key, after removing some attributes, it gets a minimal key
	 * @param Sigma FD set
	 * @param key not minimal key
	 * @param R schema
	 * @return minimal key
	 */
	public static Key getRefinedMinKey(List<FD> Sigma,Key key,List<String> R){
		ArrayList<String> minKey = new ArrayList<String>();
		minKey.addAll(key.getAttributes());
		
		for(String a : key.getAttributes()) {
			List<String> minKey_no_a = new ArrayList<String>();
			minKey_no_a.addAll(minKey);
			minKey_no_a.remove(a);
			List<String> closure = getAttrSetClosure(minKey_no_a, Sigma);
			if(closure.containsAll(R) && closure.size() == R.size())//after deleting a, it still is a key
				minKey.remove(a);
		}
		
		return new Key(minKey);
	}
	
	/**
	 * get the closure of attribute set with given FDs
	 * @param attrSet attribute set
	 * @param Sigma functional dependencies
	 * @return the closure of specified attribute set
	 */
	public static List<String> getAttrSetClosure(Collection<String> attrSet,Collection<FD> Sigma){
		List<FD> Sigma_next = new ArrayList<FD>();
		ArrayList<String> X_n_next = new ArrayList<String>(attrSet);
		
		for(FD fd : Sigma) {//find 'abc' -> 'sth',then get 'sth'
			List<String> left = fd.getLeftHand();
			List<String> right = fd.getRightHand();
			if(X_n_next.containsAll(left)) {
				//find subset == left(must ensure each element of set is distinct)
				for(String a : right) {
					if(!X_n_next.contains(a)) {
						X_n_next.add(a);
					}
				}
			}else
				Sigma_next.add(fd);
		}

		if(X_n_next.size() == attrSet.size()) //X_n == X_n+1
			return X_n_next;
		else {
			return getAttrSetClosure(X_n_next, Sigma_next);
		}
	}
	
	/**
	 * through a minimal key, get all minimal keys in PTIME
	 * @param R schema
	 * @param Sigma FD set
	 * @return all minimal keys
	 */
	public static List<Key> getMinimalKeys(List<String> R, List<FD> Sigma) {
		Key firstMinKey = Utils.getRefinedMinKey(Sigma, new Key(R), R);
		List<Key> minimalKeys = new ArrayList<Key>();
		minimalKeys.add(firstMinKey);
		Key current_key = firstMinKey;
		while(current_key != null) {
			for(FD fd : Sigma) {
				List<String> left = fd.getLeftHand();
				List<String> right = fd.getRightHand();
				List<String> S = new ArrayList<String>();//S = left union (current_key - right)
				S.addAll(left);
				for(String a : current_key.getAttributes()) {
					if(!right.contains(a) && !S.contains(a))
						S.add(a);
				}

				minimalKeys = getNonRedundantKeys(R, Sigma, minimalKeys, new Key(S));
			}
			int index = minimalKeys.indexOf(current_key);
			if(index+1 >= minimalKeys.size())
				current_key = null;
			else
				current_key = minimalKeys.get(index+1);
		}
		
		return minimalKeys;
	}
	
	/**
	 * if the key is non-redundant, refine the key to minimal key and add it into minimal key set
	 * @param R
	 * @param Sigma
	 * @param minimalKeys
	 * @param key not minimal key
	 * @return minimal keys
	 */
	public static List<Key> getNonRedundantKeys(List<String> R, List<FD> Sigma, List<Key> minimalKeys,Key key) {
		boolean redundantKey = false;
		for(Key minimalKey : minimalKeys) {
			if(key.contains(minimalKey)) {
				redundantKey = true;
				break;
			}
		}
		if(!redundantKey) {
			Key minKey = Utils.getRefinedMinKey(Sigma, key, R);
			if(!minimalKeys.contains(minKey))
				minimalKeys.add(minKey);
		}
			
		return minimalKeys;
	}
	
	/**
	 * load FDs from local json file
	 * @param Fd_Json_path
	 * @return index 0: schema; index 1: FDs; if file path does not exist, return null
	 * @throws IOException
	 */
	public static List<Object> readFDs(String Fd_Json_path) {
		List<String> schema = new ArrayList<String>();
		List<FD> fd_list = new ArrayList<FD>();
		StringBuilder str = null;
		try {
			FileReader fr = new FileReader(Fd_Json_path);
			BufferedReader br = new BufferedReader(fr);
			String line;
			str = new StringBuilder();
			while((line = br.readLine()) != null) {
				str.append(line);
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
        JSONObject jsonObject = JSONObject.parseObject(str.toString());
        
        int schema_size = Integer.parseInt(jsonObject.get("R").toString());
        for(int j = 0;j < schema_size;j ++) {
        	schema.add(""+j);
        }
        String fd_objects = jsonObject.get("fds").toString();
        JSONArray fds = JSONArray.parseArray(fd_objects);
        for(int j = 0;j < fds.size();j ++) {
        	List<String> LHS = new ArrayList<String>();
        	List<String> RHS = new ArrayList<String>();
        	JSONObject fd_json = fds.getJSONObject(j);

        	String lhs_str = fd_json.get("lhs").toString();
        	JSONArray.parseArray(lhs_str).forEach(a -> LHS.add(a.toString()));
        	
        	String rhs_str = fd_json.get("rhs").toString();
        	JSONArray.parseArray(rhs_str).forEach(a -> RHS.add(a.toString()));
        	
        	fd_list.add(new FD(LHS,RHS));
        	
        }
        
        
        List<Object> result = new ArrayList<Object>();
        result.add(schema);
        result.add(fd_list);
        return result;
	}
	
	public static List<Object> readFDs(String Fd_Json_path,String left,String right) {
		List<String> schema = new ArrayList<String>();
		List<FD> fd_list = new ArrayList<FD>();
		StringBuilder str = null;
		try {
			FileReader fr = new FileReader(Fd_Json_path);
			BufferedReader br = new BufferedReader(fr);
			String line;
			str = new StringBuilder();
			while((line = br.readLine()) != null) {
				str.append(line);
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
        JSONObject jsonObject = JSONObject.parseObject(str.toString());
        
        int schema_size = Integer.parseInt(jsonObject.get("R").toString());
        for(int j = 0;j < schema_size;j ++) {
        	schema.add(""+j);
        }
        String fd_objects = jsonObject.get("fds").toString();
        JSONArray fds = JSONArray.parseArray(fd_objects);
        for(int j = 0;j < fds.size();j ++) {
        	List<String> LHS = new ArrayList<String>();
        	List<String> RHS = new ArrayList<String>();
        	JSONObject fd_json = fds.getJSONObject(j);

        	String lhs_str = fd_json.get(left).toString();
        	JSONArray.parseArray(lhs_str).forEach(a -> LHS.add(a.toString()));
        	
        	String rhs_str = fd_json.get(right).toString();
        	JSONArray.parseArray(rhs_str).forEach(a -> RHS.add(a.toString()));
        	
        	fd_list.add(new FD(LHS,RHS));
        	
        }
        
        
        List<Object> result = new ArrayList<Object>();
        result.add(schema);
        result.add(fd_list);
        return result;
	}
	
	/**
	 * load KeyFD cover from local json file
	 * @param Fd_Json_path
	 * @return index 0: schema; index 1: keys; index 2: FDs; if file path does not exist, return null
	 * @throws IOException
	 */
	public static List<Object> readKeyFDs(String Fd_Json_path) {
		List<String> schema = new ArrayList<String>();
		List<Key> key_list = new ArrayList<>();
		List<FD> fd_list = new ArrayList<FD>();
		StringBuilder str = null;
		try {
			FileReader fr = new FileReader(Fd_Json_path);
			BufferedReader br = new BufferedReader(fr);
			String line;
			str = new StringBuilder();
			while((line = br.readLine()) != null) {
				str.append(line);
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
        JSONObject jsonObject = JSONObject.parseObject(str.toString());
        
        int schema_size = Integer.parseInt(jsonObject.get("R").toString());
        for(int j = 0;j < schema_size;j ++) {
        	schema.add(""+j);
        }
        
        String key_objects = jsonObject.get("keys").toString();
        JSONArray keys = JSONArray.parseArray(key_objects);
        for(int i = 0;i < keys.size();i ++) {
        	JSONArray key = keys.getJSONArray(i);
        	List<String> kl = new ArrayList<>();
        	key.forEach(a -> kl.add(a.toString()));
        	key_list.add(new Key(kl));
        }
        
        String fd_objects = jsonObject.get("fds").toString();
        JSONArray fds = JSONArray.parseArray(fd_objects);
        for(int j = 0;j < fds.size();j ++) {
        	List<String> LHS = new ArrayList<String>();
        	List<String> RHS = new ArrayList<String>();
        	JSONObject fd_json = fds.getJSONObject(j);

        	String lhs_str = fd_json.get("lhs").toString();
        	JSONArray.parseArray(lhs_str).forEach(a -> LHS.add(a.toString()));
        	
        	String rhs_str = fd_json.get("rhs").toString();
        	JSONArray.parseArray(rhs_str).forEach(a -> RHS.add(a.toString()));
        	
        	fd_list.add(new FD(LHS,RHS));
        	
        }
        
        
        List<Object> result = new ArrayList<Object>();
        result.add(schema);
        result.add(key_list);
        result.add(fd_list);
        return result;
	}
	
	/**
	 * load KeyFD cover from local json file
	 * @param Fd_Json_path
	 * @return index 0: schema; index 1: keys; index 2: FDs; index 3: FD cover; if file path does not exist, return null
	 * @throws IOException
	 */
	public static List<Object> readKeyFDs(String Fd_Json_path, String FDCoverType) {
		List<String> schema = new ArrayList<String>();
		List<Key> key_list = new ArrayList<>();
		List<FD> fd_list = new ArrayList<FD>();
		List<FD> fdCover_list = new ArrayList<FD>();
		
		StringBuilder str = null;
		try {
			FileReader fr = new FileReader(Fd_Json_path);
			BufferedReader br = new BufferedReader(fr);
			String line;
			str = new StringBuilder();
			while((line = br.readLine()) != null) {
				str.append(line);
			}
			br.close();
			fr.close();
		} catch (IOException e) {
			e.printStackTrace();
			return null;
		}
        JSONObject jsonObject = JSONObject.parseObject(str.toString());
        
        int schema_size = Integer.parseInt(jsonObject.get("R").toString());
        for(int j = 0;j < schema_size;j ++) {
        	schema.add(""+j);
        }
        
        String key_objects = jsonObject.get("keys").toString();
        JSONArray keys = JSONArray.parseArray(key_objects);
        for(int i = 0;i < keys.size();i ++) {
        	JSONArray key = keys.getJSONArray(i);
        	List<String> kl = new ArrayList<>();
        	key.forEach(a -> kl.add(a.toString()));
        	key_list.add(new Key(kl));
        }
        
        String fd_objects = jsonObject.get("fds").toString();
        JSONArray fds = JSONArray.parseArray(fd_objects);
        for(int j = 0;j < fds.size();j ++) {
        	List<String> LHS = new ArrayList<String>();
        	List<String> RHS = new ArrayList<String>();
        	JSONObject fd_json = fds.getJSONObject(j);

        	String lhs_str = fd_json.get("lhs").toString();
        	JSONArray.parseArray(lhs_str).forEach(a -> LHS.add(a.toString()));
        	
        	String rhs_str = fd_json.get("rhs").toString();
        	JSONArray.parseArray(rhs_str).forEach(a -> RHS.add(a.toString()));
        	
        	fd_list.add(new FD(LHS,RHS));
        	
        }
        
        String fd_cover_objects = jsonObject.get(FDCoverType).toString();
        JSONArray fdCover = JSONArray.parseArray(fd_cover_objects);
        for(int j = 0;j < fdCover.size();j ++) {
        	List<String> LHS = new ArrayList<String>();
        	List<String> RHS = new ArrayList<String>();
        	JSONObject fd_json = fdCover.getJSONObject(j);

        	String lhs_str = fd_json.get("lhs").toString();
        	JSONArray.parseArray(lhs_str).forEach(a -> LHS.add(a.toString()));
        	
        	String rhs_str = fd_json.get("rhs").toString();
        	JSONArray.parseArray(rhs_str).forEach(a -> RHS.add(a.toString()));
        	
        	fdCover_list.add(new FD(LHS,RHS));
        }
        
        
        List<Object> result = new ArrayList<Object>();
        result.add(schema);
        result.add(key_list);
        result.add(fd_list);
        result.add(fdCover_list);
        return result;
	}
	
	/**
	 * output information of schema and FDs into file by json format
	 * @param schema_size if attribute from 0 - n-1, then schema size is n
	 * @param fds
	 * @throws IOException 
	 */
	public static void writeFDs(int schema_size,List<FD> fds, String path) {
		List<FuncDepen> fd_list = FuncDepen.convertFrom(fds);
		List<JSONObject> jobj_list = new ArrayList<JSONObject>();
		for(FuncDepen f : fd_list) {
			JSONObject jobj = (JSONObject)JSONObject.toJSON(f);
			jobj_list.add(jobj);
		}
		JSONObject output = new JSONObject();
		output.put("R", schema_size);
		output.put("fds", jobj_list);

		try {
			File file = new File(path);
			if(!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(path);
			fw.write(JSON.toJSONString(output, SerializerFeature.PrettyFormat));
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * FDs' attributes are letters
	 * @param schema_size
	 * @param fds
	 * @param path
	 */
	public static void writeFDsForString(int schema_size,List<FD> fds, String path) {
		List<JSONObject> jobj_list = new ArrayList<JSONObject>();
		for(FD f : fds) {
			JSONObject jobj = (JSONObject)JSONObject.toJSON(f);
			jobj_list.add(jobj);
		}
		JSONObject output = new JSONObject();
		output.put("R", schema_size);
		output.put("fds", jobj_list);

		try {
			File file = new File(path);
			if(!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(path);
			fw.write(JSON.toJSONString(output, SerializerFeature.PrettyFormat));
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * output information of schema and FDs into file by json format
	 * @param schemaSize if attribute from 0 - n-1, then schema size is n
	 * @param keyfd a list of object, first index is minimal keys, second index is FDs
	 * @throws IOException 
	 */
	public static void writeKeyFDs(int schemaSize,List<Object> keyfd, String path) {
		List<Key> minimalKeys = (List<Key>) keyfd.get(0);
		List<FD> fds = (List<FD>) keyfd.get(1);
		
		List<List<Integer>> keys = new ArrayList<>();
		for(Key k : minimalKeys) {
			List<Integer> key = new ArrayList<>();
			for(String a : k.getAttributes()) {
				key.add(Integer.parseInt(a));
			}
			keys.add(key);
		}
		
		List<FuncDepen> fd_list = FuncDepen.convertFrom(fds);
		List<JSONObject> jobj_list = new ArrayList<JSONObject>();
		for(FuncDepen f : fd_list) {
			JSONObject jobj = (JSONObject)JSONObject.toJSON(f);
			jobj_list.add(jobj);
		}
		
		JSONObject output = new JSONObject();
		output.put("R", schemaSize);
		output.put("keys", keys);
		output.put("fds", jobj_list);

		try {
			File file = new File(path);
			if(!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(path);
			fw.write(JSON.toJSONString(output, SerializerFeature.PrettyFormat));
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * output information of schema, keys, FDs and corresponding cover of FDs into file by json format
	 * @param schemaSize if attribute from 0 - n-1, then schema size is n
	 * @param keyfd a list of object, first index is minimal keys, second index is FDs, last index is FD cover
	 * @param path 
	 * @param FDCoverType original/nonredundant/...
	 * @throws IOException 
	 */
	public static void writeKeyFDs(int schemaSize,List<Object> keyfd, String path, String FDCoverType) {
		List<Key> minimalKeys = (List<Key>) keyfd.get(0);
		List<FD> fds = (List<FD>) keyfd.get(1);
		List<FD> fdCover = (List<FD>) keyfd.get(2);
		
		List<List<Integer>> keys = new ArrayList<>();
		for(Key k : minimalKeys) {
			List<Integer> key = new ArrayList<>();
			for(String a : k.getAttributes()) {
				key.add(Integer.parseInt(a));
			}
			keys.add(key);
		}
		
		List<FuncDepen> fd_list = FuncDepen.convertFrom(fds);
		List<JSONObject> jobj_list = new ArrayList<>();
		for(FuncDepen f : fd_list) {
			JSONObject jobj = (JSONObject)JSONObject.toJSON(f);
			jobj_list.add(jobj);
		}
		
		List<FuncDepen> fd_cover_list = FuncDepen.convertFrom(fdCover);
		List<JSONObject> jobj_list1 = new ArrayList<>();
		for(FuncDepen f : fd_cover_list) {
			JSONObject jobj = (JSONObject) JSONObject.toJSON(f);
			jobj_list1.add(jobj);
		}
		
		JSONObject output = new JSONObject();
		output.put("R", schemaSize);
		output.put("keys", keys);
		output.put("fds", jobj_list);
		output.put(FDCoverType, jobj_list1);

		try {
			File file = new File(path);
			if(!file.exists())
				file.createNewFile();
			FileWriter fw = new FileWriter(path);
			fw.write(JSON.toJSONString(output, SerializerFeature.PrettyFormat));
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	/**
	 * judge whether an FD is implied by FD set or not
	 * @param Sigma
	 * @param fd
	 * @return true if implied, false otherwise
	 */
	public static boolean isImplied(List<FD> Sigma, FD fd) {
		return Utils.getAttrSetClosure(fd.getLeftHand(), Sigma).containsAll(fd.getRightHand());
	}
	
	public static boolean isImplied(List<FD> Sigma, FD fd, Map<Set<String>,List<String>> closure_map) {
		Set<String> LHS = new HashSet<String>(fd.getLeftHand());
		if(closure_map.containsKey(LHS)) {
			return closure_map.get(LHS).containsAll(fd.getRightHand());
		}else {
			List<String> LHS_clo = getAttrSetClosure(LHS, Sigma);
			closure_map.put(LHS, LHS_clo);
			return LHS_clo.containsAll(fd.getRightHand());
		}
	}
	
	/**
	 * given an FD set,
	 * compute non-redundant FD cover
	 * @param fds
	 * @return non-redundant cover
	 */
	public static List<FD> compNonRedundantCover(List<FD> fds, List<Double> freeMemList,String memoryUnit) {
		List<FD> non_redundant = new ArrayList<FD>();
		non_redundant.addAll(fds);
		
		for(FD fd : fds) {
			freeMemList.add(getFreeMemory(memoryUnit));//record free memory size
			List<FD> non_redun_no_fd = new ArrayList<FD>();
			non_redun_no_fd.addAll(non_redundant);
			non_redun_no_fd.remove(fd);//FD set - fd
			
			if(Utils.isImplied(non_redun_no_fd, fd))//if fd is redundant in non_redundant
				non_redundant.remove(fd);
		}
		
		return non_redundant;
	}
	
	/**
	 * given an FD set,
	 * compute non-redundant FD cover
	 * @param fds
	 * @return non-redundant cover
	 */
	public static List<FD> compNonRedundantCover(List<FD> fds) {
		List<FD> non_redundant = new ArrayList<FD>();
		non_redundant.addAll(fds);
		
		for(FD fd : fds) {
			List<FD> non_redun_no_fd = new ArrayList<FD>();
			non_redun_no_fd.addAll(non_redundant);
			non_redun_no_fd.remove(fd);//FD set - fd
			
			if(Utils.isImplied(non_redun_no_fd, fd))//if fd is redundant in non_redundant
				non_redundant.remove(fd);
		}
		
		return non_redundant;
	}
	
	/**
	 * deeply copy FDs
	 * @param FDs
	 * @return FDs copy
	 */
	public static List<FD> deepCopyFDs(List<FD> FDs){
		List<FD> copy = new ArrayList<FD>();
		for(FD fd : FDs) {
			List<String> LHS  = new ArrayList<String>();
			List<String> RHS = new ArrayList<String>();
			LHS.addAll(fd.getLeftHand());
			RHS.addAll(fd.getRightHand());
			FD f = new FD(LHS, RHS);
			copy.add(f);
		}
		return copy;
	}
	
	/**
	 * get left reduced FD set
	 * @param fds
	 * @return left reduced FD set
	 */
	public static List<FD> LeftRed(List<FD> fds, List<Double> freeMemList,String memoryUnit) {
		List<FD> leftRed = Utils.deepCopyFDs(fds);
		
		Map<Set<String>,List<String>> closure_map = new HashMap<Set<String>,List<String>>();//key=attribute set, value=closure in FDs
		
		for(int i = 0;i < fds.size();i ++) {
			freeMemList.add(getFreeMemory(memoryUnit));//record free memory size
			FD CURRENT = leftRed.get(i);//current FD to reduce LHS
			List<String> LHS = CURRENT.getLeftHand();
			List<String> RHS = CURRENT.getRightHand();
			for(String a : fds.get(i).getLeftHand()) {//each a in FD's LHS
				List<String> LHS_no_a = new ArrayList<String>();
				LHS_no_a.addAll(LHS);
				LHS_no_a.remove(a);
				FD f = new FD(LHS_no_a,RHS);
				if(Utils.isImplied(leftRed, f, closure_map)) {
					LHS.remove(a);//remove redundant attribute from X
				}
					
			}
		}
		
		List<FD> leftReduced = new ArrayList<FD>();//remove redundant
		for(FD fd : leftRed) {
			if(!leftReduced.contains(fd))
				leftReduced.add(fd);
		}
		return leftReduced;
	}
	
	/**
	 * get left reduced FD set
	 * @param fds
	 * @return left reduced FD set
	 */
	public static List<FD> LeftRed(List<FD> fds) {
		List<FD> leftRed = Utils.deepCopyFDs(fds);
		
		Map<Set<String>,List<String>> closure_map = new HashMap<Set<String>,List<String>>();//key=attribute set, value=closure in FDs
		
		for(int i = 0;i < fds.size();i ++) {
			FD CURRENT = leftRed.get(i);//current FD to reduce LHS
			List<String> LHS = CURRENT.getLeftHand();
			List<String> RHS = CURRENT.getRightHand();
			for(String a : fds.get(i).getLeftHand()) {//each a in FD's LHS
				List<String> LHS_no_a = new ArrayList<String>();
				LHS_no_a.addAll(LHS);
				LHS_no_a.remove(a);
				FD f = new FD(LHS_no_a,RHS);
				if(Utils.isImplied(leftRed, f, closure_map)) {
					LHS.remove(a);//remove redundant attribute from X
				}
					
			}
		}
		
		List<FD> leftReduced = new ArrayList<FD>();//remove redundant
		for(FD fd : leftRed) {
			if(!leftReduced.contains(fd))
				leftReduced.add(fd);
		}
		return leftReduced;
	}
	
	/**
	 * get right reduced FD set
	 * @param fds
	 * @return
	 */
	public static List<FD> RightRed(List<FD> fds, List<Double> freeMemList,String memoryUnit){
		List<FD> rightRed = Utils.deepCopyFDs(fds);
		
		for(int i = 0;i < fds.size();i ++) {
			freeMemList.add(getFreeMemory(memoryUnit));//record free memory size
			FD CURRENT = rightRed.get(i);//current FD to reduce RHS
			List<String> LHS = CURRENT.getLeftHand();
			List<String> RHS = CURRENT.getRightHand();
			for(String a : fds.get(i).getRightHand()) {
				List<String> RHS_no_a = new ArrayList<String>();
				RHS_no_a.addAll(RHS);
				RHS_no_a.remove(a);
				List<FD> F_prime = new ArrayList<FD>();
				F_prime.addAll(rightRed);
				//F - X->Y U X->(Y - a)
				F_prime.remove(CURRENT);
				FD f1 = new FD(LHS,RHS_no_a);
				if(!F_prime.contains(f1))
					F_prime.add(f1);
				
				if(Utils.isImplied(F_prime, new FD(LHS,Arrays.asList(a)))) {
					RHS.remove(a);//remove redundant attribute from RHS
				}
				
			}
		}
		
		List<FD> rightReduced = new ArrayList<FD>();//remove redundant
		for(FD fd : rightRed) {
			if(!rightReduced.contains(fd))
				rightReduced.add(fd);
		}
		return rightReduced;
	}
	
	/**
	 * get right reduced FD set
	 * @param fds
	 * @return
	 */
	public static List<FD> RightRed(List<FD> fds){
		List<FD> rightRed = Utils.deepCopyFDs(fds);
		
		for(int i = 0;i < fds.size();i ++) {
			FD CURRENT = rightRed.get(i);//current FD to reduce RHS
			List<String> LHS = CURRENT.getLeftHand();
			List<String> RHS = CURRENT.getRightHand();
			for(String a : fds.get(i).getRightHand()) {
				List<String> RHS_no_a = new ArrayList<String>();
				RHS_no_a.addAll(RHS);
				RHS_no_a.remove(a);
				List<FD> F_prime = new ArrayList<FD>();
				F_prime.addAll(rightRed);
				//F - X->Y U X->(Y - a)
				F_prime.remove(CURRENT);
				FD f1 = new FD(LHS,RHS_no_a);
				if(!F_prime.contains(f1))
					F_prime.add(f1);
				
				if(Utils.isImplied(F_prime, new FD(LHS,Arrays.asList(a)))) {
					RHS.remove(a);//remove redundant attribute from RHS
				}
				
			}
		}
		
		List<FD> rightReduced = new ArrayList<FD>();//remove redundant
		for(FD fd : rightRed) {
			if(!rightReduced.contains(fd))
				rightReduced.add(fd);
		}
		return rightReduced;
	}
	
	/**
	 * compute reduced cover of FD set
	 * @param fds
	 * @return reduced cover
	 */
	public static List<FD> compReducedCover(List<FD> fds, List<Double> freeMemList,String memoryUnit){
		List<FD> reduced = RightRed(LeftRed(fds,freeMemList,memoryUnit), freeMemList,memoryUnit);
		List<FD> remove = new ArrayList<FD>();
		for(FD fd : reduced) {
			if(fd.getRightHand().isEmpty())//remove FD: X -> empty set
				remove.add(fd);
		}
		remove.forEach(r -> reduced.remove(r));
		
		return reduced;
	}
	
	/**
	 * compute reduced cover of FD set
	 * @param fds
	 * @return reduced cover
	 */
	public static List<FD> compReducedCover(List<FD> fds){
		List<FD> reduced = RightRed(LeftRed(fds));
		List<FD> remove = new ArrayList<FD>();
		for(FD fd : reduced) {
			if(fd.getRightHand().isEmpty())//remove FD: X -> empty set
				remove.add(fd);
		}
		remove.forEach(r -> reduced.remove(r));
		
		return reduced;
	}
	
	
	/**
	 * compute canonical cover
	 * @param reduced  reduced cover as input
	 * @return canonical cover
	 */
	public static List<FD> compCanonicalCover(List<FD> reduced, List<Double> freeMemList,String memoryUnit) {
		List<FD> reduced_copy = Utils.deepCopyFDs(reduced);
		List<FD> canoCover = new ArrayList<FD>();
		
		for(FD fd : reduced_copy) {
			freeMemList.add(getFreeMemory(memoryUnit));//record free memory size
			if(fd.getRightHand().size() == 1)
				canoCover.add(fd);
			else {
				List<String> LHS = fd.getLeftHand();
				List<String> RHS = fd.getRightHand();
				for(String a : RHS) {
					FD f = new FD(LHS, Arrays.asList(a));
					canoCover.add(f);
				}
			}
		}
		
		return canoCover;
	}
	
	/**
	 * compute canonical cover
	 * @param reduced  reduced cover as input
	 * @return canonical cover
	 */
	public static List<FD> compCanonicalCover(List<FD> reduced) {
		List<FD> reduced_copy = Utils.deepCopyFDs(reduced);
		List<FD> canoCover = new ArrayList<FD>();
		
		for(FD fd : reduced_copy) {
			if(fd.getRightHand().size() == 1)
				canoCover.add(fd);
			else {
				List<String> LHS = fd.getLeftHand();
				List<String> RHS = fd.getRightHand();
				for(String a : RHS) {
					FD f = new FD(LHS, Arrays.asList(a));
					canoCover.add(f);
				}
			}
		}
		
		return canoCover;
	}
	
	/**
	 * get all FDs with the equivalent LHS, "equivalent" means X and Y for given FDs, X <--> Y
	 * @param LHS
	 * @param candidate the candidate FDs to be tested
	 * @param LHS_clo key is LHS, value is its attribute closure
	 * @return 
	 */
	public static List<FD> equivLHSFDs(Set<String> LHS, List<FD> candidate,Map<Set<String>,List<String>> LHS_clo){
		List<FD> output = new ArrayList<FD>();
		List<String> LHS_closure = LHS_clo.get(LHS);//input LHS's attribute closure
		for(FD fd : candidate) {
			List<String> fd_closure = LHS_clo.get(new HashSet<String>(fd.getLeftHand()));//fd LHS's attribute closure
			
			if(fd_closure.containsAll(LHS) && LHS_closure.containsAll(fd.getLeftHand()))
				output.add(fd);
		}
		return output;
	}
	
	/**
	 * 
	 * @param FDs
	 * @return a set of an FD set that has equivalent LHS in each set.
	 */
	public static List<List<FD>> equivLHSFDSet(List<FD> FDs){
		List<FD> computedFDs = new ArrayList<FD>();//FD that has been processed into an E_X group
		List<FD> uncomputedFDs = new ArrayList<FD>(FDs);
		Set<Set<String>> LHS_set = new HashSet<Set<String>>();
		FDs.forEach(fd -> LHS_set.add(new HashSet<String>(fd.getLeftHand())));
		
		//compute attribute closure of LHS of all FDs
		Map<Set<String>,List<String>> LHS_clo_map = new HashMap<Set<String>,List<String>>();
		//key = LHS, value = LHS's attribute closure
		for(FD fd : FDs) {
			List<String> LHS = fd.getLeftHand();
			List<String> LHS_clo = Utils.getAttrSetClosure(LHS, FDs);
			LHS_clo_map.put(new HashSet<String>(LHS), LHS_clo);
		}
		
		List<List<FD>> res = new ArrayList<List<FD>>();
		int count = 0;
		for(Set<String> lhs : LHS_set) {
			
			boolean computed = false;
			for(FD f : computedFDs) {
				if(f.getLeftHand().containsAll(lhs) && f.getLeftHand().size() == lhs.size()) {
					computed = true;
					break;
				}
			}
			if(computed)
				continue;
			
//			System.out.println(++count + "/"+LHS_set.size()+" | computed FD : "+computedFDs.size());
			List<FD> E_lhs = Utils.equivLHSFDs(lhs, uncomputedFDs,LHS_clo_map);
			
			computedFDs.addAll(E_lhs);
			uncomputedFDs.removeAll(E_lhs);
			
			res.add(E_lhs);
		}
		
		
		return res;
	}
	
	/**
	 * judge whether X directly determines Y in non-redundant FD
	 * @param cloMap attribute sets' closure in F_prime
	 * @param F_prime F - E_X
	 * @param X
	 * @param Y
	 * @return true if direct determination, false otherwise
	 */
	public static boolean DDrive(Map<Set<String>,Set<String>> cloMap,List<FD> F_prime,Set<String> X,Set<String> Y) {
		Set<String> closure = null;//X's closure in F_prime
		Set<String> attr_set = new HashSet<String>(X);
		if(cloMap.containsKey(attr_set)) {
			closure = cloMap.get(attr_set);
		}else {
			closure = new HashSet<String>(Utils.getAttrSetClosure(new ArrayList<String>(X), F_prime));
			cloMap.put(attr_set, closure);
		}
		return closure.containsAll(Y);//X -> Y in F_prime ?
	}
	
	
	/**
	 * compute a minimal cover for given FD set
	 * @param non_redun non-redundant cover
	 * @return a minimal cover that has minimal number of FD
	 */
	public static List<FD> compMinimalCover(List<FD> non_redun, List<Double> freeMemList,String memoryUnit) {
		List<FD> copy = Utils.deepCopyFDs(non_redun);
		List<FD> F = new ArrayList<FD>(copy);
		List<List<FD>> E_bar = Utils.equivLHSFDSet(copy);//each element of the set is a set whose FDs in it have equivalent LHS
		
		for(List<FD> E_X : E_bar) {
			freeMemList.add(getFreeMemory(memoryUnit));//record free memory size
			List<FD> F_prime = new ArrayList<FD>();//F - E_X to test directly determination
			F_prime.addAll(F);
			F_prime.removeAll(E_X);
			
			Map<Set<String>,Set<String>> closureMap = new HashMap<Set<String>,Set<String>>();//attribute sets' closure in F_prime
			
			//key = LHS, value = corresponding FD
			Map<Set<String>,FD> LHS_FD_map = new HashMap<Set<String>,FD>();
			E_X.forEach(f -> LHS_FD_map.put(new HashSet<String>(f.getLeftHand()), f));
			
			//list that has all LHS set in E_X
			List<Set<String>> all_LHS = new ArrayList<Set<String>>();
			E_X.forEach(f -> all_LHS.add(new HashSet<String>(f.getLeftHand())));
			
			for(int i = 0;i < all_LHS.size();i ++) {
				
				Set<String> LHS1 = all_LHS.get(i);
				for(int j = 0;j < all_LHS.size();j ++) {
					if(i == j)
						continue;
					Set<String> LHS2 = all_LHS.get(j);
					
					if(LHS_FD_map.containsKey(LHS1) && LHS_FD_map.containsKey(LHS2)) {
						//if FDs with these two LHS both exist
						if(Utils.DDrive(closureMap,F_prime,LHS1, LHS2)) {//if LHS1 directly determine LHS2
//							System.out.println("FD reducing!!");
							
							FD Y_U = LHS_FD_map.get(LHS1);
							FD Z_V = LHS_FD_map.get(LHS2);
							Set<String> UV_set = new HashSet<String>();
							UV_set.addAll(Y_U.getRightHand());
							UV_set.addAll(Z_V.getRightHand());
							
							F.remove(Y_U);LHS_FD_map.remove(LHS1);//delete FD
							F.remove(Z_V);LHS_FD_map.remove(LHS2);//delete FD
							
							FD Z_UV = new FD(new ArrayList<String>(Z_V.getLeftHand()),new ArrayList<String>(UV_set));//Z -> UV
							F.add(Z_UV);LHS_FD_map.put(new HashSet<String>(Z_UV.getLeftHand()), Z_UV);
							
							break;
						}
					}
				}
			}
			
		}
		
		return F;
	}
	
	/**
	 * compute a minimal cover for given FD set
	 * @param non_redun non-redundant cover
	 * @return a minimal cover that has minimal number of FD
	 */
	public static List<FD> compMinimalCover(List<FD> non_redun) {
		List<FD> copy = Utils.deepCopyFDs(non_redun);
		List<FD> F = new ArrayList<FD>(copy);
		List<List<FD>> E_bar = Utils.equivLHSFDSet(copy);//each element of the set is a set whose FDs in it have equivalent LHS
		
		for(List<FD> E_X : E_bar) {
			List<FD> F_prime = new ArrayList<FD>();//F - E_X to test directly determination
			F_prime.addAll(F);
			F_prime.removeAll(E_X);
			
			Map<Set<String>,Set<String>> closureMap = new HashMap<Set<String>,Set<String>>();//attribute sets' closure in F_prime
			
			//key = LHS, value = corresponding FD
			Map<Set<String>,FD> LHS_FD_map = new HashMap<Set<String>,FD>();
			E_X.forEach(f -> LHS_FD_map.put(new HashSet<String>(f.getLeftHand()), f));
			
			//list that has all LHS set in E_X
			List<Set<String>> all_LHS = new ArrayList<Set<String>>();
			E_X.forEach(f -> all_LHS.add(new HashSet<String>(f.getLeftHand())));
			
			for(int i = 0;i < all_LHS.size();i ++) {
				
				Set<String> LHS1 = all_LHS.get(i);
				for(int j = 0;j < all_LHS.size();j ++) {
					if(i == j)
						continue;
					Set<String> LHS2 = all_LHS.get(j);
					
					if(LHS_FD_map.containsKey(LHS1) && LHS_FD_map.containsKey(LHS2)) {
						//if FDs with these two LHS both exist
						if(Utils.DDrive(closureMap,F_prime,LHS1, LHS2)) {//if LHS1 directly determine LHS2
//							System.out.println("FD reducing!!");
							
							FD Y_U = LHS_FD_map.get(LHS1);
							FD Z_V = LHS_FD_map.get(LHS2);
							Set<String> UV_set = new HashSet<String>();
							UV_set.addAll(Y_U.getRightHand());
							UV_set.addAll(Z_V.getRightHand());
							
							F.remove(Y_U);LHS_FD_map.remove(LHS1);//delete FD
							F.remove(Z_V);LHS_FD_map.remove(LHS2);//delete FD
							
							FD Z_UV = new FD(new ArrayList<String>(Z_V.getLeftHand()),new ArrayList<String>(UV_set));//Z -> UV
							F.add(Z_UV);LHS_FD_map.put(new HashSet<String>(Z_UV.getLeftHand()), Z_UV);
							
							break;
						}
					}
				}
			}
			
		}
		
		return F;
	}
	
	/**
	 * compute an optimal cover
	 * @param mini_cover mini cover is not minimal cover, the mini cover can be derived by bool expression
	 * @param freeMemList
	 * @param memoryUnit
	 * @return an optimal cover
	 */
	public static List<FD> compOptimalCover(List<FD> mini_cover, List<Double> freeMemList,String memoryUnit) {
		List<FD> copy = Utils.deepCopyFDs(mini_cover);
		List<FD> F = new ArrayList<FD>(copy);
		List<List<FD>> E_bar = Utils.equivLHSFDSet(copy);//each element of the set is a set whose FDs in it have equivalent LHS
		
		for(List<FD> E_X : E_bar) {
			freeMemList.add(getFreeMemory(memoryUnit));//record free memory size
			List<FD> F_prime = new ArrayList<FD>();//F - E_X to test directly determination
			F_prime.addAll(F);
			F_prime.removeAll(E_X);
			
			Map<Set<String>,Set<String>> closureMap = new HashMap<Set<String>,Set<String>>();//attribute sets' closure in F_prime
			
			//key = LHS, value = corresponding FD
			Map<Set<String>,FD> LHS_FD_map = new HashMap<Set<String>,FD>();
			E_X.forEach(f -> LHS_FD_map.put(new HashSet<String>(f.getLeftHand()), f));
			
			//list that has all LHS set in E_X
			List<Set<String>> all_LHS = new ArrayList<Set<String>>();
			E_X.forEach(f -> all_LHS.add(new HashSet<String>(f.getLeftHand())));
			
			for(int i = 0;i < all_LHS.size();i ++) {
				
				Set<String> LHS1 = all_LHS.get(i);
				for(int j = 0;j < all_LHS.size();j ++) {
					if(i == j)
						continue;
					Set<String> LHS2 = all_LHS.get(j);
					
					if(LHS_FD_map.containsKey(LHS1) && LHS_FD_map.containsKey(LHS2)) {
						//if FDs with these two LHS both exist
						if(Utils.DDrive(closureMap,F_prime,LHS1, LHS2)) {//if LHS1 directly determine LHS2
//							System.out.println("FD reducing!!");
							
							FD Y_U = LHS_FD_map.get(LHS1);
							FD Z_V = LHS_FD_map.get(LHS2);
							Set<String> UV_set = new HashSet<String>();
							UV_set.addAll(Y_U.getRightHand());
							UV_set.addAll(Z_V.getRightHand());
							
							F.remove(Y_U);LHS_FD_map.remove(LHS1);//delete FD
							F.remove(Z_V);LHS_FD_map.remove(LHS2);//delete FD
							
							FD Z_UV = new FD(new ArrayList<String>(Z_V.getLeftHand()),new ArrayList<String>(UV_set));//Z -> UV
							F.add(Z_UV);LHS_FD_map.put(new HashSet<String>(Z_UV.getLeftHand()), Z_UV);
							
							break;
						}
					}
				}
			}
			
		}
		
		return F;
	}
	
	/**
	 * compute an optimal cover
	 * @param mini_cover mini cover is not minimal cover, the mini cover can be derived by bool expression
	 * @param freeMemList
	 * @param memoryUnit
	 * @return an optimal cover
	 */
	public static List<FD> compOptimalCover(List<FD> mini_cover) {
		List<FD> copy = Utils.deepCopyFDs(mini_cover);
		List<FD> F = new ArrayList<FD>(copy);
		List<List<FD>> E_bar = Utils.equivLHSFDSet(copy);//each element of the set is a set whose FDs in it have equivalent LHS
		
		for(List<FD> E_X : E_bar) {
			List<FD> F_prime = new ArrayList<FD>();//F - E_X to test directly determination
			F_prime.addAll(F);
			F_prime.removeAll(E_X);
			
			Map<Set<String>,Set<String>> closureMap = new HashMap<Set<String>,Set<String>>();//attribute sets' closure in F_prime
			
			//key = LHS, value = corresponding FD
			Map<Set<String>,FD> LHS_FD_map = new HashMap<Set<String>,FD>();
			E_X.forEach(f -> LHS_FD_map.put(new HashSet<String>(f.getLeftHand()), f));
			
			//list that has all LHS set in E_X
			List<Set<String>> all_LHS = new ArrayList<Set<String>>();
			E_X.forEach(f -> all_LHS.add(new HashSet<String>(f.getLeftHand())));
			
			for(int i = 0;i < all_LHS.size();i ++) {
				
				Set<String> LHS1 = all_LHS.get(i);
				for(int j = 0;j < all_LHS.size();j ++) {
					if(i == j)
						continue;
					Set<String> LHS2 = all_LHS.get(j);
					
					if(LHS_FD_map.containsKey(LHS1) && LHS_FD_map.containsKey(LHS2)) {
						//if FDs with these two LHS both exist
						if(Utils.DDrive(closureMap,F_prime,LHS1, LHS2)) {//if LHS1 directly determine LHS2
							System.out.println("FD reducing!!");
							
							FD Y_U = LHS_FD_map.get(LHS1);
							FD Z_V = LHS_FD_map.get(LHS2);
							Set<String> UV_set = new HashSet<String>();
							UV_set.addAll(Y_U.getRightHand());
							UV_set.addAll(Z_V.getRightHand());
							
							F.remove(Y_U);LHS_FD_map.remove(LHS1);//delete FD
							F.remove(Z_V);LHS_FD_map.remove(LHS2);//delete FD
							
							FD Z_UV = new FD(new ArrayList<String>(Z_V.getLeftHand()),new ArrayList<String>(UV_set));//Z -> UV
							F.add(Z_UV);LHS_FD_map.put(new HashSet<String>(Z_UV.getLeftHand()), Z_UV);
							
							break;
						}
					}
				}
			}
			
		}
		
		return F;
	}
	
	public static void printFDs(Collection<FD> fds) {
		System.out.println("###########");
		fds.forEach(System.out::println);
		System.out.println("###########\n");
	}
	
	public static void printKeys(Collection<Key> keys) {
		System.out.println("###########");
		keys.forEach(System.out::println);
		System.out.println("###########\n");
	}
	
	/**
	 * e.g. FD AB -> C, the attribute symbol count is 3.
	 * @param FDs
	 * @return the number of attribute symbol of FDs
	 */
	public static int getFDAttrSymCount(List<FD> FDs) {
		int count = 0;
		for(FD fd : FDs) {
			count += fd.getLeftHand().size();
			count += fd.getRightHand().size();
		}
		return count;
	}
	
	/**
	 * given a list of data set name, return a list of parameter object
	 * @param nameList get all parameters on specified data type
	 * @param dataType COMPLETE(no null values)/NULL EQUALITY(null equals null)/NULL UNCERTAINTY(null is uncertain)
	 * @return return specified data type's parameters
	 */
	public static List<Parameter> getParameterList(List<String> nameList, DataTypeEnum dataType){
		List<Parameter> para_list_complete = Arrays.asList(
				new Parameter(new Dataset("abalone", 9, 4177, ",", null, DataTypeEnum.COMPLETE)),
				new Parameter(new Dataset("adult", 15, 32561, ";", null, DataTypeEnum.COMPLETE)),
				new Parameter(new Dataset("fd-red", 30, 250000, ",", null, DataTypeEnum.COMPLETE)),
				new Parameter(new Dataset("letter(non-dup)", 17, 18668, ",", null, DataTypeEnum.COMPLETE)),
				new Parameter(new Dataset("lineitem", 16, 6001215, ",", null, DataTypeEnum.COMPLETE))
				);
		List<Parameter> para_list_null_equality = Arrays.asList(
				new Parameter(new Dataset("breast", 11, 699, ",", "?", DataTypeEnum.NULL_EQUALITY)),
				new Parameter(new Dataset("bridges", 13, 108, ",", "?", DataTypeEnum.NULL_EQUALITY)),
				new Parameter(new Dataset("diabetic", 30, 101766, ",", "?", DataTypeEnum.NULL_EQUALITY)),
				new Parameter(new Dataset("echo", 13, 132, ",", "?", DataTypeEnum.NULL_EQUALITY)),
				new Parameter(new Dataset("hepatitis", 20, 155, ",", "?", DataTypeEnum.NULL_EQUALITY)),
				new Parameter(new Dataset("ncvoter", 19, 1000, ",", "\"\"", DataTypeEnum.NULL_EQUALITY)),
				new Parameter(new Dataset("pdbx", 13, 17305799, ";", "\"?\"", DataTypeEnum.NULL_EQUALITY)),
				new Parameter(new Dataset("uniprot", 30, 512000, ",", "\"\"", DataTypeEnum.NULL_EQUALITY)),
				new Parameter(new Dataset("china_weather", 18, 262920, ",", "NA", DataTypeEnum.NULL_EQUALITY))
				);
		List<Parameter> para_list_null_uncertainty = Arrays.asList(
				new Parameter(new Dataset("breast", 11, 699, ",", "?", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("bridges", 13, 108, ",", "?", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("diabetic", 30, 101766, ",", "?", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("echo", 13, 132, ",", "?", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("hepatitis", 20, 155, ",", "?", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("ncvoter", 19, 1000, ",", "\"\"", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("pdbx", 13, 17305799, ";", "\"?\"", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("uniprot", 30, 512000, ",", "\"\"", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("china_weather", 18, 262920, ",", "NA", DataTypeEnum.NULL_UNCERTAINTY))
				);
		
		if(nameList == null && dataType == null) {
			List<Parameter> all = new ArrayList<>();
			all.addAll(para_list_complete);
			all.addAll(para_list_null_equality);
			all.addAll(para_list_null_uncertainty);
			return all;
		}else if(dataType == DataTypeEnum.COMPLETE){
			if(nameList == null)
				return para_list_complete;
			List<Parameter> res = new ArrayList<>();
			for(Parameter para : para_list_complete) {
				//only get parameters that exist in para_list_complete
				if(nameList.contains(para.dataset.name))
					res.add(para);
			}
			return res;
		}else if(dataType == DataTypeEnum.NULL_EQUALITY) {
			if(nameList == null)
				return para_list_null_equality;
			List<Parameter> res = new ArrayList<>();
			for(Parameter para : para_list_null_equality) {
				//only get parameters that exist in para_list_null_equality
				if(nameList.contains(para.dataset.name))
					res.add(para);
			}
			return res;
		}else if(dataType == DataTypeEnum.NULL_UNCERTAINTY) {
			if(nameList == null)
				return para_list_null_uncertainty;
			List<Parameter> res = new ArrayList<>();
			for(Parameter para : para_list_null_uncertainty) {
				//only get parameters that exist in para_list_null_uncertainty
				if(nameList.contains(para.dataset.name))
					res.add(para);
			}
			return res;
		}
		return null;
	}
	
	public static List<Parameter> getParameterListV1(List<String> nameList){
		List<Parameter> para_list = Arrays.asList(
				new Parameter(new Dataset("abalone", 9, 4177, ",", null, DataTypeEnum.COMPLETE)),
				new Parameter(new Dataset("adult", 15, 32561, ";", null, DataTypeEnum.COMPLETE)),
				new Parameter(new Dataset("fd-red", 30, 250000, ",", null, DataTypeEnum.COMPLETE)),
				new Parameter(new Dataset("lineitem", 16, 6001215, ",", null, DataTypeEnum.COMPLETE)),
//				new Parameter(new Dataset("lineitem-500k", 16, 500000, ",", null, DataTypeEnum.COMPLETE)),
				new Parameter(new Dataset("breast", 11, 699, ",", "?", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("bridges", 13, 108, ",", "?", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("diabetic", 30, 101766, ",", "?", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("echo", 13, 132, ",", "?", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("hepatitis", 20, 155, ",", "?", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("ncvoter", 19, 1000, ",", "\"\"", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("pdbx", 13, 17305799, ";", "\"?\"", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("uniprot", 30, 512000, ",", "\"\"", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("china_weather", 18, 262920, ",", "NA", DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("claims", 13, 97031, ",", null, DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("dblp10k", 34, 10000, ",", null, DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("routes", 9, 67663, ",", null, DataTypeEnum.NULL_UNCERTAINTY)),
				new Parameter(new Dataset("hospital", 15, 114919, ",", null, DataTypeEnum.NULL_UNCERTAINTY))
				);
		
		if(nameList == null) {
			return para_list;
		}else {
			List<Parameter> res = new ArrayList<>();
			for(Parameter para : para_list) {
				//only get parameters that exist in para_list_complete
				if(nameList.contains(para.dataset.name))
					res.add(para);
			}
			return res;
		}
	}
	
	/**
	 * return current free memory of computer
	 * @param unit "GiB"/"MiB"/"KiB", "Byte" by default
	 * @return
	 */
	public static double getFreeMemory(String unit) {
		OperatingSystemMXBean memory = (OperatingSystemMXBean) ManagementFactory.getOperatingSystemMXBean();
		long freeMemory = memory.getFreeMemorySize();
		switch(unit){
		
			case "GiB" :
				return freeMemory/(1024.0*1024*1024);
				
			case "MiB" :
				return freeMemory/(1024.0*1024);
		
			case "KiB" :
				return freeMemory/1024.0;
		
			default :
				return freeMemory;
		}
	}
	
	/**
	 * 
	 * @param varNum No. of variable of a bool function
	 * @return truth table
	 */
	public static int[][] truthTable(int varNum){
		int bound = (int) Math.pow(2, varNum);
		int[][] truthTab = new int[bound][varNum];
		for(int i = 0;i < bound;i ++) {
			String binStr = Integer.toBinaryString(i);
//			System.out.println(i+" | "+binStr);
			while(binStr.length() != varNum) {
				binStr = "0"+binStr;
			}
			for(int j = 0;j < varNum;j ++) {
				truthTab[i][j] = Integer.parseInt(binStr.charAt(j)+"");
			}
		}
		return truthTab;
	}
	
	/**
	 * In this method, we enforce FD attribute is a continuous integer from 0.
	 * Given FD AB -> C, we transform it as a product term of bool expression : ABC'
	 * then get the product term's bool value with input variable value.
	 * e.g. for A=0 B=0 C=0, ABC'= 0 & 0 & 1 = 0, for A=1 B=1 C=0, ABC' = 1 & 1 & 1 = 1
	 * @param fd an FD that only has 1 attribute on its RHS, attributes of FD in this input should numeric attribute.
	 * @param boolList recording the attrbute's bool value 0/1, first attribute is index 0, second is index 1, ...
	 * @return the bool value (0/1) of the product term transformed by FD, return -1 if errors encounter
	 */
//	public static int compFDBoolValue(FD fd,int[] boolList) {
//		if(fd.getRightHand().size() > 1) {
//			System.out.println("FD : "+fd.toString()+" with more than 1 attribute!");
//			return -1;
//		}
//		List<String> LHS = fd.getLeftHand();
//		List<String> RHS = fd.getRightHand();
//		for(String a : LHS) {
//			int b = boolList[Integer.parseInt(a)];//attribute a's bool value
//			if(b == 0) {
//				return 0;
//			}
//		}
//		for(String a : RHS) {
//			int b = boolList[Integer.parseInt(a)];//attribute a's bool value
//			if(b == 1) {
//				return 0;
//			}
//		}
//		return 1;
//	}
	
	/**
	 * In this method, we don't enforce FD attribute is a continuous integer from 0.
	 * Given FD AB -> C, we transform it as a product term of bool expression : ABC'
	 * then get the product term's bool value with input variable value.
	 * e.g. for A=0 B=0 C=0, ABC'= 0 & 0 & 1 = 0, for A=1 B=1 C=0, ABC' = 1 & 1 & 1 = 1
	 * @param fd an FD that only has 1 attribute on its RHS
	 * @param boolList recording the attrbute's bool value 0/1, first attribute is index 0, second is index 1, ...
	 * @param attrMap FD attribute's index of the attrMap refers to the order
	 * @return the bool value (0/1) of the product term transformed by FD, return -1 if errors encounter
	 */
	public static int compFDBoolValue(FD fd,int[] boolList, List<String> attrMap) {
		if(fd.getRightHand().size() > 1) {
			System.out.println("FD : "+fd.toString()+" with more than 1 attribute!");
			return -1;
		}
		List<String> LHS = fd.getLeftHand();
		List<String> RHS = fd.getRightHand();
		for(String a : LHS) {
			int b = boolList[attrMap.indexOf(a)];//attribute a's bool value
			if(b == 0) {
				return 0;
			}
		}
		for(String a : RHS) {
			int b = boolList[attrMap.indexOf(a)];//attribute a's bool value
			if(b == 1) {
				return 0;
			}
		}
		return 1;
	}
	
	/**
	 * In this method, we enforce FD attribute is a continuous integer from 0. e.g. schemaSize = 10, the attribute is either 0,1,2,...,9
	 * Given FD set A -> B, A C -> D, first it computes the first Delobel�CCasey transform, i.e., bool(A,B,C,D) = AB' + ACD'
	 * A -> B ==> AB' ; AC -> D ==> ACD'. Each right side is called product term
	 * then compute each product term's bool value, finally we execute OR operation among all product terms to get final bool value
	 * @param FDList FD set that each FD must not be more than 1 attribute on its RHS
	 * @param schemaSize schema size and attribute number of FD set; we enforce FD attribute is natural Integer and starts from 0
	 * @return minTerms if running well, null if errors encounter
	 */
//	public static List<Integer> compMinTerms(List<FD> FDList,int schemaSize) {
//		int[][] truthTable = truthTable(schemaSize);
//		List<Integer> minTerms = new ArrayList<>();
//		for(int i = 0;i < truthTable.length;i ++) {
//			int[] input = truthTable[i];
//			for(FD fd : FDList) {
//				int boolVa = Utils.compFDBoolValue(fd, input);
//				if(boolVa == -1)
//					return null;
//				if(boolVa == 1){//due to OR relation among product terms, the whole bool expression will be 1
//					minTerms.add(i);
//					break;
//				}	
//			}
//		}
//		return minTerms;
//	}
	
	/**
	 * In this method, we don't enforce FD attribute is a continuous integer from 0.
	 * We firstly will get attribute's order as same as truth table. The first order is the most left of truth table...
	 * e.g. map = a : 0, c : 1, b : 2, we then get schema size = 3; An FD c -> a can be to a'c, input 010 if true for a'c
	 * Given FD set A -> B, A C -> D, first it computes the first Delobel�CCasey transform, i.e., bool(A,B,C,D) = AB' + ACD'
	 * A -> B ==> AB' ; AC -> D ==> ACD'. Each right side is called product term
	 * then compute each product term's bool value, finally we execute OR operation among all product terms to get final bool value
	 * @param FDList FD set that each FD must not be more than 1 attribute on its RHS
	 * @param attrMap the index means order of attribute
	 * @return minTerms if running well, null if errors encounter
	 */
	public static List<Integer> compMinTerms(List<FD> FDList, List<String> attrMap) {
		int[][] truthTable = truthTable(attrMap.size());
		List<Integer> minTerms = new ArrayList<>();
		for(int i = 0;i < truthTable.length;i ++) {
			int[] input = truthTable[i];
			for(FD fd : FDList) {
				int boolVa = Utils.compFDBoolValue(fd, input, attrMap);
				if(boolVa == -1)
					return null;
				if(boolVa == 1){//due to OR relation among product terms, the whole bool expression will be 1
					minTerms.add(i);
					break;
				}	
			}
		}
		return minTerms;
	}
	
	/**
	 * return an attribute map of FD set.
	 * e.g. given FDs a -> b, e -> d, the attribute set is [a,b,e,d],
	 * then sort it and get increasing order due to alphabet, like [a,b,d,e]
	 * @param FDList
	 * @return attribute map
	 */
	public static List<String> getAttributeMapFromFDs(List<FD> FDList){
		List<String> attrMap = new ArrayList<>();
		for(FD fd : FDList) {
			for(String a : fd.getLeftHand()) {
				if(!attrMap.contains(a))
					attrMap.add(a);
			}
			for(String a : fd.getRightHand()) {
				if(!attrMap.contains(a))
					attrMap.add(a);
			}
		}
		attrMap.sort(null);
		return attrMap;
	}
	
	/**
	 * convert a bool expression to FD set, the attribute is order value of truth table
	 * Given a bool expression bool = A,B,C' + A,D', we can convert it to FDs AB -> C and A -> D.
	 * the attribute with a prime will be placed to right side of an FD
	 * @param boolExpression
	 * @return FD list
	 */
//	public static List<FD> convertBoolExprToFDs(String boolExpression){
//		List<FD> FDs = new ArrayList<>();
//		String[] productTerms = boolExpression.split("\\+");
//		for(int i = 0;i < productTerms.length;i ++) {
//			String[] terms = productTerms[i].split(",");
//			List<String> LHS = new ArrayList<>();
//			List<String> RHS = new ArrayList<>();
//			for(int j = 0;j < terms.length;j ++) {
//				String term = terms[j];
//				if(term.contains("'"))
//					RHS.add(term.substring(0,term.length()-1));//remove last char "'" and then add the rest into RHS
//				else 
//					LHS.add(term);
//			}
//			FDs.add(new FD(LHS, RHS));
//		}
//		return FDs;
//	}
	
	/**
	 * convert a bool expression to FD set, the attribute is order value of truth table
	 * Given a bool expression bool = A,B,C' + A,D', we can convert it to FDs AB -> C and A -> D.
	 * the attribute with a prime will be placed to right side of an FD
	 * @param boolExpression
	 * @param attrMap the index of attrMap refers to order value of truth table
	 * @return FD list
	 */
	public static List<FD> convertBoolExprToFDs(String boolExpression,List<String> attrMap){
		List<FD> FDs = new ArrayList<>();
		String[] productTerms = boolExpression.split("\\+");
		for(int i = 0;i < productTerms.length;i ++) {
			String[] terms = productTerms[i].split(",");
			List<String> LHS = new ArrayList<>();
			List<String> RHS = new ArrayList<>();
			for(int j = 0;j < terms.length;j ++) {
				String term = terms[j];
				if(term.contains("'"))
					RHS.add(attrMap.get(Integer.parseInt(term.substring(0,term.length()-1))));//remove last char "'" and then add the rest into RHS
				else 
					LHS.add(attrMap.get(Integer.parseInt(term)));
			}
			FDs.add(new FD(LHS, RHS));
		}
		return FDs;
	}
	
	/**
	 * split FD by splitting RHS of the FD to return more FDs with only 1 attribute of RHS
	 * @param input
	 * @return
	 */
	public static List<FD> splitFDs(List<FD> input){
		List<FD> res = new ArrayList<>();
		for(FD fd : input) {
			for(String a : fd.getRightHand()) {
				FD newFD = new FD(new ArrayList<>(fd.getLeftHand()),new ArrayList<>(Arrays.asList(a)));
				res.add(newFD);
			}
		}
		return res;
	}
	
	/**
	 * combine FDs that they have same LHS
	 * @param input
	 * @return
	 */
	public static List<FD> combineFDs(List<FD> input){
		input = Utils.deepCopyFDs(input);
		List<FD> res = new ArrayList<>();
		Map<Set<String>,FD> fd_map = new HashMap<Set<String>,FD>();//key = LHS set,value = FD
		for(FD fd : input) {
			Set<String> LHS = new HashSet<>(fd.getLeftHand());
			if(fd_map.containsKey(LHS)) {
				FD f = fd_map.get(LHS);
				for(String a : fd.getRightHand()) {
					if(!f.getRightHand().contains(a))
						f.getRightHand().add(a);
				}
				fd_map.put(LHS, f);
			}else
				fd_map.put(LHS, fd);
		}
		for(Set<String> lhs : fd_map.keySet()){
			res.add(fd_map.get(lhs));
		}
		return res;
	}
	
	/**
	 * write lines into local file
	 * @param content
	 * @param outputPath
	 * @param append true if appending content into end of file
	 * @return true if write successfully, false otherwise
	 */
	public static boolean writeContent(List<String> content, String outputPath, boolean append) {
		FileWriter fw = null;
		BufferedWriter bw = null;
		try {
			File file = new File(outputPath);
			File parentDir = file.getParentFile();
			if(!parentDir.exists())
				parentDir.mkdirs();
			if(!file.exists())
				file.createNewFile();
			fw = new FileWriter(outputPath, append);
			bw = new BufferedWriter(fw);
			for(String line : content) {
				bw.write(line+"\n");
			}
			bw.close();
			fw.close();
		}catch(Exception e) {
			System.out.println(e.getMessage());
			System.out.println("write content unsuccessfully!");
			return false;
		}
		return true;
	}
	
	/**
	 * read lines into local file
	 * @param inputPath
	 * @return content list containing lines, null if file does not exist
	 */
	public static List<String> readContent(String inputPath) {
		FileReader fr = null;
		BufferedReader br = null;
		List<String> content = new ArrayList<>();
		try {
			File file = new File(inputPath);
			if(!file.exists())
				return null;
			fr = new FileReader(inputPath);
			br = new BufferedReader(fr);
			String line;
			while((line = br.readLine()) != null) {
				content.add(line);
			}
			br.close();
			fr.close();
		}catch(Exception e) {
			System.out.println("read content unsuccessfully!");
			return null;
		}
		return content;
	}
	
	/**
	 * according to cover type of the parameter, return corresponding cover path
	 * @param coverType
	 * @param para
	 * @return cover path
	 */
	public static String getCoverPath(String coverType, Parameter para) {
		String coverPath = null;
		switch(coverType) {
		case "original" :
			coverPath = para.fd_add;
			break;
		case "nonredundant" :
			coverPath = para.fd_nonredundant_cover_add;
			break;
		case "reduced" :
			coverPath = para.fd_reduced_cover_add;
			break;
		case "canonical" :
			coverPath = para.fd_canonical_cover_add;
			break;
		case "minimal" :
			coverPath = para.fd_minimal_cover_add;
			break;
		case "reduced minimal" :
			coverPath = para.fd_reduced_minimal_cover_add;
			break;
		case "optimal" :
			coverPath = para.fd_optimal_cover_add;
			break;
		case "original keyfd" :
			coverPath = para.fd_keyfd_add;
			break;
		case "nonredundant keyfd" :
			coverPath = para.fd_nonredundant_keyfd_cover_add;
			break;
		case "reduced keyfd" :
			coverPath = para.fd_reduced_keyfd_cover_add;
			break;
		case "canonical keyfd" :
			coverPath = para.fd_canonical_keyfd_cover_add;
			break;
		case "minimal keyfd" :
			coverPath = para.fd_minimal_keyfd_cover_add;
			break;
		case "reduced minimal keyfd" :
			coverPath = para.fd_reduced_minimal_keyfd_cover_add;
			break;
		case "optimal keyfd" :
			coverPath = para.fd_optimal_keyfd_cover_add;
			break;
		}
		return coverPath;
	}
	
	/**
	 * given an FD cover type, return an object list, including schema(index 0) and FD set(index 1)
	 * @param coverType "original"/"nonredundant"/"reduced"/"canonical"/"minimal"/"reduced minimal"/"optimal"/
	 * @param para
	 * @return an object list if the parameter's dataset's FD cover exists in local files; null otherwise
	 */
	public static List<Object> getFDCover(String coverType,Parameter para){
		String coverPath = getCoverPath(coverType, para);
		return readFDs(coverPath);
	}
	
	/**
	 * compute FD cover like original/nonredundant/reduced/canonical/minimal/reduced minimal/optimal
	 * @param FDs
	 * @param FDCoverType
	 * @return
	 */
	public static List<FD> compFDCover(List<FD> FDs, String FDCoverType){
		FDs = Utils.combineFDs(FDs);
		List<FD> fdcover = null;
		switch(FDCoverType) {
		case "original":
			fdcover = FDs;
			break;
		case "nonredundant":
			fdcover = Utils.compNonRedundantCover(FDs);
			break;
		case "reduced":
			fdcover = Utils.compReducedCover(FDs);
			fdcover = Utils.combineFDs(fdcover);
			break;
		case "canonical":
			fdcover = Utils.compCanonicalCover(Utils.compReducedCover(FDs));
			break;
		case "minimal":
			fdcover = Utils.compMinimalCover(Utils.compNonRedundantCover(FDs));
			fdcover = Utils.combineFDs(fdcover);
			break;
		case "reduced minimal":
			fdcover = Utils.compReducedCover(Utils.compMinimalCover(Utils.compNonRedundantCover(FDs)));
			fdcover = Utils.combineFDs(fdcover);
			break;
		case "optimal":
			List<FD> fd_set = Utils.splitFDs(FDs);
			List<String> attrMap = Utils.getAttributeMapFromFDs(fd_set);
			List<Integer> minTerms = Utils.compMinTerms(fd_set, attrMap);
			QuineMcCluskey engine = new QuineMcCluskey(minTerms,attrMap.size());
			String boolEx = engine.runQuineMcCluskey(false,null,null);
			List<FD> miniCover = Utils.convertBoolExprToFDs(boolEx,attrMap);
			miniCover = Utils.combineFDs(miniCover);
			fdcover = Utils.compOptimalCover(miniCover);
			break;
		}
		return fdcover;
	}
	
	/**
	 * given an FD cover type, return an object list, including schema(index 0), Key set(index 1), FD set(index 2) and FD cover(index 3 if newVersion is true)
	 * @param coverType "original keyfd"/"nonredundant keyfd"/"reduced keyfd"/"canonical keyfd"/"minimal keyfd"/"reduced minimal keyfd"/"optimal keyfd"
	 * @param para
	 * @param newVersion true if needing new version of keyfd cover file that has additional FD cover of remaining fds like nonredundat...
	 * @return index 0: schema; index 1: keys; index 2: FDs;(index 3: FD cover if new version is true);null if file does not exists in local files
	 */
	public static List<Object> getKeyFDCover(String coverType,Parameter para,boolean newVersion){
		String coverPath = getCoverPath(coverType, para);
		if(!newVersion)
			return readKeyFDs(coverPath);
		else {
			int index = coverType.indexOf(" keyfd");
			String Sigma_FD_cover = coverType.substring(0, index);
			return readKeyFDs(coverPath,Sigma_FD_cover);
		}
	}
	
	/**
	 * compute a key/FD cover for a given FD set
	 * @param schema relational schema
	 * @param Sigma FDs as input, such as non-redundant FD cover, reduced cover, ...
	 * @param freeMemList record memory use
	 * @param memoryUnit "GiB"/"MiB"/"KiB", "Byte" by default
	 * @return a key/FD cover, including minimal keys(index 0), {Sigma - keys}(index 1)
	 */
	public static List<Object> compKeyFDCover(List<String> schema, List<FD> Sigma, List<Double> freeMemList,String memoryUnit){
		//compute all minimal keys first
		List<Key> allMinimalKeys = Utils.getMinimalKeys(schema, Sigma);
		List<FD> Sigma_k = new ArrayList<>();//minimal key FDs
		for(Key key : allMinimalKeys) {
			List<String> left = new ArrayList<>(key.getAttributes());
			List<String> right = new ArrayList<>();
			for(String a : schema) {
				if(!left.contains(a))
					right.add(a);//right = schema - left
			}
			Sigma_k.add(new FD(left, right));
		}
		
		
		List<FD> Sigma_f = new ArrayList<>(Sigma);
		for(FD fd : Sigma) {
			freeMemList.add(getFreeMemory(memoryUnit));//record free memory size
			Sigma_f.remove(fd);//Sigma_f - fd
			List<FD> union = new ArrayList<>();//(Sigma_f - fd) U Sigma_k
			union.addAll(Sigma_k);
			union.addAll(Sigma_f);
			if(!Utils.isImplied(union, fd)) {//if [(Sigma_f - fd) U Sigma_k] not implies fd
				Sigma_f.add(fd);
			}
		}
		
		List<Object> res = new ArrayList<>();
		res.add(allMinimalKeys);
		res.add(Sigma_f);
		return res;
	}
	
	/**
	 * compute a key/FD cover for a given FD set
	 * @param schema relational schema
	 * @param Sigma FDs as input, such as non-redundant FD cover, reduced cover, ...
	 * @return a key/FD cover, including minimal keys(index 0), {Sigma - keys}(index 1)
	 */
	public static List<Object> compKeyFDCover(List<String> schema, List<FD> Sigma){
		//compute all minimal keys first
		List<Key> allMinimalKeys = Utils.getMinimalKeys(schema, Sigma);
		List<FD> Sigma_k = new ArrayList<>();//minimal key FDs
		for(Key key : allMinimalKeys) {
			List<String> left = new ArrayList<>(key.getAttributes());
			List<String> right = new ArrayList<>();
			for(String a : schema) {
				if(!left.contains(a))
					right.add(a);//right = schema - left
			}
			Sigma_k.add(new FD(left, right));
		}
		
		
		List<FD> Sigma_f = new ArrayList<>(Sigma);
		for(FD fd : Sigma) {
			Sigma_f.remove(fd);//Sigma_f - fd
			List<FD> union = new ArrayList<>();//(Sigma_f - fd) U Sigma_k
			union.addAll(Sigma_k);
			union.addAll(Sigma_f);
			if(!Utils.isImplied(union, fd)) {//if [(Sigma_f - fd) U Sigma_k] not implies fd
				Sigma_f.add(fd);
			}
		}
		
		List<Object> res = new ArrayList<>();
		res.add(allMinimalKeys);
		res.add(Sigma_f);
		return res;
	}
	
	
	
	/**
	 * get average number
	 * @param list
	 * @return
	 */
	public static double getAve(List<Double> list) {
		DecimalFormat df = new DecimalFormat("0.00");
		double sum = 0;
		for(double data : list) {
			sum += data;
		}
		return Double.parseDouble(df.format(sum/list.size()));
	}
	
	/**
	 * get median number
	 * @param list
	 * @return
	 */
	public static double getMedian(List<Double> list) {
		Collections.sort(list,new Comparator<Double>(){

			@Override
			public int compare(Double o1, Double o2) {
				if(o1 > o2)
					return 1;
				else if(o1 < o2)
					return -1;
				else
					return 0;
			}
		});
		int middleIndex = -1;
		if(list.size() % 2 == 0)
			middleIndex = list.size()/2 -1;
		else
			middleIndex = list.size()/2;

		return list.get(middleIndex);
	}
	
	/*
	 * given a matrix, we return a list that each element of the list will be average value of each column of matrix
	 */
	public static List<Double> getEachColValue(List<List<Double>> matrix) {
		List<Double> output = new ArrayList<Double>();
		for(int col = 0;col < matrix.get(0).size();col ++) {//column
			List<Double> col_value = new ArrayList<Double>();
			for(int row = 0;row < matrix.size();row ++) {//row
				col_value.add(matrix.get(row).get(col));
			}
			double a = getAve(col_value);
			output.add(a);
		}
		return output;
	}
	
	
	/**
	 * synthesize specified row number and column number's data set
	 * @param row_num
	 * @param col_num
	 * @return
	 */
	public static List<List<String>> synthesizeDataset(int row_num,int col_num){
		List<List<String>> dataset = new ArrayList<List<String>>();
		for(int i = 0;i < row_num;i ++) {
			List<String> data = new ArrayList<String>();
			for(int j = 0;j < col_num;j ++) {
				data.add(i+"_"+j);
			}
			dataset.add(data);
		}
		return dataset;
	}
	
	/**
	 * projection of attribute set 'XA', given FD set Sigma.
	 * e.g. Y -> B is one of projection of XA over Sigma, which XA includes YB
	 * @param Sigma
	 * @param XA
	 * @return Σ[XA] if exists, empty set otherwise
	 */
	public static  List<FD> getProjection(List<FD> Sigma,List<String> XA){
		List<FD> projection = new ArrayList<FD>();//project of attribute set 'XA', given FD set Sigma
		for(FD fd : Sigma) {
			List<String> left = fd.getLeftHand();
			List<String> right = fd.getRightHand();
			List<String> attrset_fd = new ArrayList<String>();
			for(String a : left) {
				if(!attrset_fd.contains(a))
					attrset_fd.add(a);
			}
			for(String a : right) {
				if(!attrset_fd.contains(a))
					attrset_fd.add(a);
			}
			if(XA.containsAll(attrset_fd) && !projection.contains(fd))
				projection.add(fd);
		}
		return projection;
	}
	
	/**
	 * judge whether a database is on BCNF or not
	 * @param R schema
	 * @param fds functional dependencies
	 * @return
	 */
	public static boolean isBCNF(List<String> R, List<FD> fds) {
		boolean is_BCNF = true;
		List<Key> minKeys = getMinimalKeys(R, fds);
		for(FD fd : fds) {//fd : X -> A
			List<String> X = fd.getLeftHand();
			List<String> A = fd.getRightHand();
			
			boolean XIsSuperKey = false;
			for(Key key : minKeys) {
				if(X.containsAll(key.getAttributes())) {//X is super key
					XIsSuperKey = true;
					break;
				}
			}
			if(A.containsAll(X) || XIsSuperKey) {//satisfy one of two conditions:1. FD  is trivial, 2. X is super key
				
			}else {
				is_BCNF = false;
				break;
			}
		}
		return is_BCNF;
	}
	
	public static Set<String> getPrimeAttrs(List<Key> keys) {
		Set<String> primeAttrs = new HashSet<String>();
		for(Key k : keys) {
			primeAttrs.addAll(k.getAttributes());
		}
		return primeAttrs;
	}
	
	/**
	 * compute attribute symbol number of keys,
	 * for instance, given keys [a,b], [a,c], return 4 of attribute symbol number
	 * @param keys 
	 * @return
	 */
	public static int compKeyAttrSymbNum(Collection<Key> keys) {
		int attrSymNum = 0;
		for(Key key : keys) {
			attrSymNum += key.size();
		}
		return attrSymNum;
	}
	
	/**
	 * compute attribute symbol number of fds,
	 * for instance, given fds [a,b -> e], [a,c -> e], return 6 of attribute symbol number
	 * @param keys 
	 * @return
	 */
	public static int compFDAttrSymbNum(List<FD> FDs) {
		int attrSymNum = 0;
		for(FD fd : FDs) {
			attrSymNum += fd.getLeftHand().size() + fd.getRightHand().size();
		}
		return attrSymNum;
	}
	
	public static int compFDLHSAttrSymbNum(List<FD> FDs) {
		int attrSymNum = 0;
		for(FD fd : FDs) {
			attrSymNum += fd.getLeftHand().size();
		}
		return attrSymNum;
	}
	
	
	/**
	 * input a number-like string list, like ["2","1","3","7","3"],then sort
	 * @param numberList
	 * @return
	 */
	public static List<String> sortByNumbers(List<String> numberList){
		numberList.sort(new Comparator<String>() {

			@Override
			public int compare(String o1, String o2) {
				int num1 = Integer.parseInt(o1);
				int num2 = Integer.parseInt(o2);
				if(num2 < num1)
					return 1;
				else if(num2 > num1)
					return -1;
				else
					return 0;
			}
			
		});
		return numberList;
	}
	
	/**
	 * judge an attribute set is a closure attribute set or not
	 * @param R
	 * @param FDs
	 * @param X
	 * @return true if X is closure attribute set, false otherwise
	 */
	public static boolean isClosureAttrSet(List<String> R, List<FD> FDs, List<String> X) {
		boolean result = true;
		for(FD fd : FDs) {
			List<String> left = fd.getLeftHand();
			List<String> right = fd.getRightHand();
			if(X.containsAll(left) && !X.containsAll(right)) {
				result = false;
				break;
			}
		}
		return result;
	}
	
	/**
	 * get specific sized attribute set from R
	 * @param R
	 * @param size
	 * @return
	 */
	public static List<List<String>> allSpecificSizedAttrSet(List<String> R,int size){
		if(size <= 0) {//result empty set
			List<List<String>> result = new ArrayList<>();
			result.add(new ArrayList<String>());
			return result;
		}
		List<List<String>> result = new ArrayList<List<String>>();
		ArrayList<ArrayList<Integer>> resultWithIndex = new ArrayList<ArrayList<Integer>>();
		ArrayList<Integer> start = new ArrayList<Integer>();//|R|=3 Size=1 start={0}; Size=2 start={0,1}
		ArrayList<Integer> current = new ArrayList<Integer>();
		for(int i = 0;i < size;i ++) {
			start.add(i);
			current.add(i);
		}
		resultWithIndex.add(start);
		while(true) {
			ArrayList<Integer> next = new ArrayList<Integer>();
			for(int i = current.size()-1;i >= 0;i --) {
				int value_i = current.get(i);
				int maxValue_i = (R.size() - (size - i));
				if(value_i < maxValue_i) {
					for(int j =0;j < current.size();j ++) {
						if(j < i) 
							next.add(current.get(j));
						if(j == i) 
							next.add(value_i+1);
						if(j > i)
							next.add(next.get(next.size()-1)+1);
					}
					resultWithIndex.add(next);
					current = next;
					break;
				}
			}
			int maxValue_0 = (R.size() - (size - 0));
			if(current.get(0) == maxValue_0) {//e.g. |R| = 6, keySize=3, current={3,4,5} then find
				break;
			}
		}
		
//		System.out.println("Given schema :"+R+" ,size : "+size+": ");
		for(ArrayList<Integer> Index : resultWithIndex) {
			List<String> attrSet = new ArrayList<String>();
			for(Integer index : Index) {
				attrSet.add(R.get(index));
			}
//			System.out.println(attrSet);
			result.add(attrSet);
		}
		
		return result;
	}
	
	/**
	 * compute all closure attribute set
	 * @param R
	 * @param FDs
	 * @return
	 */
	public static List<List<String>> computeCloAttrSets(List<String> R, List<FD> FDs){
		List<List<String>> T = new ArrayList<>();
		List<List<String>> S = new ArrayList<>();
		for(int i = 0;i <= R.size();i ++) {
			S.addAll(Utils.allSpecificSizedAttrSet(R, i));
		}
		
		for(List<String> X : S) {
			if(Utils.isClosureAttrSet(R, FDs, X)) {
				T.add(X);
			}
		}
		
		return T;
	}
	
	public static Set<String> intersection(Set<String> A, Set<String> B){
		Set<String> intersec = new HashSet<>();
		for(String a : A) {
			if(B.contains(a))
				intersec.add(a);
		}
		return intersec;
	}
	
	public static List<Set<String>> GEN(List<String> R, List<FD> FDs){
		List<List<String>> CL = Utils.computeCloAttrSets(R, FDs);
		List<Set<String>> GEN = new ArrayList<>();
		List<Set<String>> GEN_copy = new ArrayList<>();
		for(List<String> a : CL) {//GEN = CL - R
			if(!a.containsAll(R)) {
				GEN.add(new HashSet<String>(a));
				GEN_copy.add(new HashSet<String>(a));
			}
		}
		
		for(Set<String> X :GEN_copy) {
//			System.out.println("X : "+X.toString());
			Set<String> intersec = null;
			for(Set<String> Y : GEN) {
				if(!Y.equals(X) && Y.containsAll(X)) {
					if(intersec == null) {
						intersec = new HashSet<>(Y);
					}else {
						intersec = intersection(intersec, Y);
					}
						
				}
			}
			if(intersec == null)
				continue;
			if(intersec.equals(X)) {
//				System.out.println("remove X : "+X.toString());
				GEN.remove(X);
			}
		}
		return GEN;
	}
	
	public static List<List<String>> computeMinArmstrongRelation(List<String> R, List<FD> FDs){
		List<List<String>> minArmRel = new ArrayList<>();
		List<String> t0 = new ArrayList<>();
		for(int i = 0;i < R.size();i ++) {
			t0.add("0");
		}
		minArmRel.add(t0);
		
		List<Set<String>> GEN = Utils.GEN(R, FDs);
//		System.out.println("GEN :");
//		GEN.forEach(System.out::println);
		for(int i = 1;i <= GEN.size();i ++) {
			Set<String> Xi = GEN.get(i-1);//closure attribute set
			List<String> ti = new ArrayList<>();
			for(String a : R) {
				if(Xi.contains(a))
					ti.add("0");
				else
					ti.add(i+"");
			}
			minArmRel.add(ti);
		}
		
		return minArmRel;
	}
	
	/**
	 * return all  attributes of an FD
	 * @param fd
	 * @return
	 */
	public static List<String> getFDAttributes(FD fd){
		List<String> attrs = new ArrayList<>();
		for(String a : fd.getLeftHand()) {
			if(!attrs.contains(a))
				attrs.add(a);
		}
		for(String a : fd.getRightHand()) {
			if(!attrs.contains(a))
				attrs.add(a);
		}
		return attrs;
	}
	
	/**
	 * given a list of FDs, it constructs an FD list without the specific FD
	 * @param Sigma
	 * @param specificFD
	 * @return
	 */
	public static List<FD> getFDsWithoutSpecificFD(List<FD> Sigma, FD specificFD){
		List<FD> result = new ArrayList<>();
		for(FD fd : Sigma) {
			if(!fd.equals(specificFD))
				result.add(fd);
		}
		return result;
	}
	
	/**
	 * Algorithm 11
	 * given an FD cover F of type t (minimal-reduced and optimal)
	 * 
	 * three steps:
	 * 1) computing an FD cover G' of type t for F 
	 * 2) compute minimal keys K implied by G'
	 * 3) get FD cover G by removing key-FDs from G'
	 * return mixed cover (K, G)
	 * @param FDs
	 * @param FDCoverType
	 * @return K,G
	 */
	public static List<Object> comp_Mixed_Cover_Sequential(List<String> R, List<FD> F, String FDCoverType){
		List<FD> G_prime = Utils.compFDCover(F, FDCoverType);//FD cover with type-t for FDs
		
		List<Key> K = Utils.getMinimalKeys(R, G_prime);
		
		List<FD> removal = new ArrayList<>();//need to remove from G'
		for(FD Y_Z : G_prime) {
			for(Key X : K){
				//if X in K is a subset of Y, need to remove
				List<String> Y = Y_Z.getLeftHand();
				if(Y.containsAll(X.getAttributes())) {
					removal.add(Y_Z);
					break;
				}
			}
		}
		G_prime.removeAll(removal);
		
		List<Object> output = new ArrayList<Object>();
		output.add(K);
		output.add(G_prime);
		
		return output;
	}
	
	/**
	 * Algorithm 12
	 * given an FD set F of no specific type
	 * 
	 * process in parallel for 1) & 2):
	 * 1) the set K of minimal keys implied by F
	 * 2) the FD cover G' of type-t of F
	 * 
	 * 3) get FD cover G by removing key-FDs from G'
	 * return mixed cover (K, G)
	 * 
	 * @param R
	 * @param F
	 * @param FDCoverType
	 * @return K,G
	 */
	public static List<Object> comp_Mixed_Cover_Parallel(List<String> R, List<FD> F, String FDCoverType) {
		//computing keys and FD cover in parallel
		List<FD> G_prime = Utils.compFDCover(F, FDCoverType);
		
		List<Key> K = Utils.getMinimalKeys(R, F);
		
		//compute G
		List<FD> removal = new ArrayList<>();//need to remove from F_prime
		for(FD Y_Z : G_prime) {
			for(Key X : K){
				//if X in K is a subset of Y, need to remove
				List<String> Y = Y_Z.getLeftHand();
				if(Y.containsAll(X.getAttributes())) {
					removal.add(Y_Z);
					break;
				}
			}
		}
		G_prime.removeAll(removal);
		
		List<Object> output = new ArrayList<Object>();
		output.add(K);
		output.add(G_prime);
		
		return output;
	}
	
	public static <T extends Number> double calculateAvg(List<T> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return Double.NaN;
        }

        double sum = 0.0;
        for (T number : numbers) {
            sum += number.doubleValue();
        }
        DecimalFormat df = new DecimalFormat("#.####");
        return Double.parseDouble(df.format(sum / numbers.size()));
    }

    public static <T extends Number> T calculateMax(List<T> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return null;
        }

        T max = numbers.get(0);
        for (T number : numbers) {
            if (number.doubleValue() > max.doubleValue()) {
                max = number;
            }
        }
        return max;
    }

    public static <T extends Number> T calculateMin(List<T> numbers) {
        if (numbers == null || numbers.isEmpty()) {
            return null;
        }

        T min = numbers.get(0);
        for (T number : numbers) {
            if (number.doubleValue() < min.doubleValue()) {
                min = number;
            }
        }
        return min;
    }
    
    public static boolean isTwoFDSetsEquivalent(List<FD> G, List<FD> F) {
		for(FD fd : G) {//check if any FD in G can be implied by F
			if(!Utils.isImplied(F, fd)) {
				return false;
			}
		}
		for(FD fd : F) {//check if any FD in F can be implied by G
			if(!Utils.isImplied(G, fd)) {
				return false;
			}
		}
		return true;
	}
    
    public static Set<Key> refineToMinimalKeys(List<Key> keys){
    	Set<Key> minimalKeys = new HashSet<Key>();
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
    
    public static boolean KeysAreMinimal(List<FD> G, Collection<Key> keys, List<String> R) {
		if(keys.isEmpty())
			return false;
		List<Key> trueMinimalKeys = getMinimalKeys(R, G);
		if(trueMinimalKeys.size() == keys.size() && trueMinimalKeys.containsAll(keys) && keys.containsAll(trueMinimalKeys))
			return true;
		return false;
	}
	public static void main(String[] args) throws IOException {
	}

}
