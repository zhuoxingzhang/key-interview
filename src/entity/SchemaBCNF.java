package entity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;

import util.Utils;

public class SchemaBCNF implements SchemaInterface{
	/**
	 * store a bcnf schema info
	 */
	private List<String> attr_set;
//	private List<FD> fd_set;
	private List<Key> min_key_list;
	
	public SchemaBCNF() {
		this.attr_set = new ArrayList<String>();
//		this.fd_set = new ArrayList<FD>();
		this.min_key_list = new ArrayList<Key>();
	}
	public SchemaBCNF(List<String> attr_set,List<Key> keys) {
		this.attr_set = attr_set;
//		this.fd_set = fd_set;
		this.min_key_list = keys;
	}
	
	
//	public SchemaBCNF(List<String> attr_set,List<FD> fd_set,List<Key> key_set) {
//		this.attr_set = attr_set;
//		this.fd_set = fd_set;
//		this.min_key_list = key_set;
//		this.sortForIndexGreedy();
//	}
//	public SchemaBCNF(List<String> attr_set,List<Key> key_set) {
//		this.attr_set = attr_set;
//		this.min_key_list = key_set;
//	}
	
	
	public List<String> getAttr_set() {
		return attr_set;
	}
	public void setAttr_set(List<String> attr_set) {
		this.attr_set = attr_set;
	}
//	public List<FD> getFd_set() {
//		return fd_set;
//	}
//	public void setFd_set(List<FD> fd_set) {
//		this.fd_set = fd_set;
//	}

	public List<Key> getMin_key_list() {
		return min_key_list;
	}

	public void setMin_key_list(List<Key> min_key_list) {
		this.min_key_list = min_key_list;
	}
	

	/**
	 * if level and key number is null(0), calculate them
	 * at the same time, we select which fds are key or not
	 * @throws SQLException 
	 */
//	public void update() throws SQLException {
//		if(true) {
//			ArrayList<String> minKey =  Utils.getRefinedMinKey(fd_set, attr_set, attr_set);
//			List<List<String>> minKeys =  Utils.getMinimalKeys(fd_set, attr_set, minKey);
//			this.min_key_list.addAll(minKeys);
//			this.n_key = minKeys.size();
//		}
//		if(true) {
//			if(fd_set.isEmpty()) {//the entire schema is a key
//				schema_level = 1;
//				return;
//			}
//			for(FD fd : fd_set) {
//				List<String> lhs = fd.getLeftHand();
//				if(Utils.getAttrSetClosure(this.attr_set, lhs, fd_set).containsAll(this.attr_set)) {//if fd is a key
//					this.key_fd_list.add(fd);//add the key fd into the list
//					continue;
//				}else {//if fd is non-key over the schemata
//					this.non_key_fd_list.add(fd);//add the non-key fd into the list
//					int level = fd.getLevel() > 0 ? fd.getLevel() : Utils.get_lhs_level_from_database(lhs);
//					schema_level = level > schema_level ? level : schema_level;
//				}
//			}
//			if(schema_level == 0)//if the schema is in BCNF
//				schema_level = 1;
//		}
//	}

//	@Override
//	public boolean equals(Object obj) {
//		if(obj instanceof SchemaBCNF) {
//			SchemaBCNF pair = (SchemaBCNF)obj;
//			if(this.attr_set.containsAll(pair.getAttr_set()) && this.attr_set.size() == pair.getAttr_set().size()
//					&& this.fd_set.containsAll(pair.getFd_set()) && this.fd_set.size() == pair.getFd_set().size())
//				return true;
//			else
//				return false;
//				
//		}else
//			return false;
//	}
	
	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SchemaBCNF) {
			SchemaBCNF schema = (SchemaBCNF)obj;
			if(this.attr_set.containsAll(schema.getAttr_set()) && this.attr_set.size() == schema.getAttr_set().size())
				return true;
			else
				return false;
				
		}else
			return false;
	}


	
	@Override
	public int hashCode() {
		return Objects.hash(new HashSet<String>(attr_set), new HashSet<Key>(min_key_list));
	}
	
	@Override
	public String toString() {
		String output = "BCNF schema: \n";
		output += "Schema: "+attr_set.toString()+"\n";
		output += "Keys: \n";
		for(Key key : min_key_list) {
			output += key.toString()+"\n";
		}
		output += "**********\n\n";
		return output;
	}
	
	
	
}
