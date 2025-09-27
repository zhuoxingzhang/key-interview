package entity;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import util.Utils;

public class Schema {
	/**
	 * store a schema info
	 */
	private List<String> attr_set;
	private List<FD> fd_set;
	private List<Key> min_key_list;
	
	public Schema() {
	}
	
	
	public Schema(List<String> attr_set,List<FD> fd_set) {
		this.attr_set = attr_set;
		this.fd_set = fd_set;
		this.min_key_list = new ArrayList<Key>();
	}
	
	public Schema(List<String> attr_set,List<FD> fd_set, Boolean sortByNum) {
		if(sortByNum)
			this.attr_set = Utils.sortByNumbers(attr_set);
		else
			this.attr_set = attr_set;
		this.fd_set = fd_set;
		this.min_key_list = new ArrayList<Key>();
	}
	
	public Schema(List<String> attr_set,List<FD> fd_set,List<Key> key_set) {
		this.attr_set = attr_set;
		this.fd_set = fd_set;
		this.min_key_list = key_set;
		this.sortForIndexGreedy();
	}
	
	public Schema(List<String> attr_set,List<FD> fd_set,List<Key> key_set, Boolean sortByNum) {
		if(sortByNum) {
			this.attr_set = Utils.sortByNumbers(attr_set);
		}else
			this.attr_set = attr_set;
		this.fd_set = fd_set;
		this.min_key_list = key_set;
		this.sortForIndexGreedy();
	}
	
	public List<String> getAttr_set() {
		return attr_set;
	}
	public void setAttr_set(List<String> attr_set) {
		this.attr_set = attr_set;
	}
	public List<FD> getFd_set() {
		return fd_set;
	}
	public void setFd_set(List<FD> fd_set) {
		this.fd_set = fd_set;
	}

	public List<Key> getMin_key_list() {
		return min_key_list;
	}

	public void setMin_key_list(List<Key> min_key_list) {
		this.min_key_list = min_key_list;
	}
	
	public void sortForIndexGreedy() {
		List<List<Integer>> key_FD_comm_attrs_rec = new ArrayList<>();
		for(Key key : this.min_key_list) {
			List<Integer> rec = new ArrayList<>();
			for(FD fd : this.fd_set) {
				rec.add(this.countCommonAttrsNum(key, fd));
			}
			key_FD_comm_attrs_rec.add(rec);
		}
		List<Integer> usedFDs = new ArrayList<>();
		List<Integer> usedKeys = new ArrayList<>();
		while(true) {
			//search most common attributes between remaining FD and remaining Key for each loop
			int maxCommonAttrNum = 0;
			int X = -1,Y = -1;
			for(int i = 0;i < key_FD_comm_attrs_rec.size();i ++) {//Key ID
				List<Integer> commonAttrListForAKey = key_FD_comm_attrs_rec.get(i);
				if(usedKeys.contains(i))
					continue;
				for(int j = 0;j < commonAttrListForAKey.size();j ++) {//FD ID
					if(commonAttrListForAKey.get(j) > maxCommonAttrNum && !usedFDs.contains(j)) {
						maxCommonAttrNum = commonAttrListForAKey.get(j);
						X = i;
						Y = j;
					}
				}
			}
			if(X == -1 && Y == -1) {//NOT FOUND
				break;
			}else {
				Key key = this.min_key_list.get(X);
				FD fd = this.fd_set.get(Y);
				this.findCommonAttrsToResort(key, fd);
				usedKeys.add(X);
				usedFDs.add(Y);
			}
		}
	}
	
	public  void sortForIndex() {
		List<FD> usedFDs = new ArrayList<>();
		for(Key key : this.min_key_list) {
			int mark = -1;
			int maxCommonNum = 0;
			for(int i = 0;i < this.fd_set.size();i ++) {
				FD fd = this.fd_set.get(i);
				if(!usedFDs.contains(fd)) {
					int commonNum = this.countCommonAttrsNum(key, fd);
					if(commonNum > maxCommonNum) {
						maxCommonNum = commonNum;
						mark = i;
					}
				}
			}
			if(mark > -1) {
				FD f = this.fd_set.get(mark);
				this.findCommonAttrsToResort(key, f);
				usedFDs.add(f);
			}
		}
	}
	
	/**
	 * 
	 * @return true if the schema is BCNF, false otherwise
	 */
	public boolean isBCNF() {
		return Utils.isBCNF(attr_set, fd_set);
	}
	
	public Schema3NF to3NF() {
		return new Schema3NF(this.attr_set, this.fd_set);
	}
	
	/**
	 * convert FDs in 3NF into specific FD/mixed cover
	 * @param coverType
	 * @return
	 */
	public Schema3NF to3NF(String coverType) {
		return new Schema3NF(this.attr_set, Utils.compFDCover(this.fd_set, coverType));
	}
	
	public Schema3NF to3NFWithMixedCover(String coverType) {
		int idx = coverType.indexOf(" keyfd");
		String fdcover1 = coverType.substring(0, idx);//get corresponding fd cover
		List<Object> info = (List<Object>) Utils.compKeyFDCover(this.attr_set, Utils.compFDCover(this.fd_set, fdcover1));
		List<Key> minKeys = (List<Key>) info.get(0);
		List<FD> remainingFDs = (List<FD>) info.get(1);
		remainingFDs = Utils.compFDCover(remainingFDs, fdcover1);
		return new Schema3NF(this.attr_set, remainingFDs, minKeys);
	}
	
	/**
	 * 
	 * @return 3NF if possible, with mixed cover
	 */
	public Schema3NF to3NFWithMixedCover() {
		List<Object> info = Utils.compKeyFDCover(attr_set, fd_set);
		List<Key> minKeys = (List<Key>) info.get(0);
		List<FD> remainingFDs = (List<FD>) info.get(1);
		return new Schema3NF(this.attr_set, remainingFDs, minKeys);
	}
	
	public SchemaBCNF toBCNF() {
		return new SchemaBCNF(this.attr_set, Utils.getMinimalKeys(this.attr_set, this.fd_set));
	}
	
	/**
	 * count the number of common attributes between FD's left and key attributes
	 * @param key
	 * @param fd
	 * @return number of common attributes
	 */
	public int countCommonAttrsNum(Key key, FD fd) {
		List<String> keyAttrs = key.getAttributes();
		List<String> FDLeftAttrs = fd.getLeftHand();
		int count = 0;
		for(String a : keyAttrs) {
			if(FDLeftAttrs.contains(a))
				count ++;
		}
		return count;
	}
	
	/**
	 * if FD's left hand and key attributes have common attributes,
	 * let the attributes rank top.
	 * @param key
	 * @param fd
	 * @return
	 */
	public boolean findCommonAttrsToResort(Key key, FD fd) {
		List<String> keyAttrs = key.getAttributes();
		List<String> FDLeftAttrs = fd.getLeftHand();
		List<String> commonAttrs = new ArrayList<>();
		for(String a : keyAttrs) {
			if(FDLeftAttrs.contains(a))
				commonAttrs.add(a);
		}
		
		if(commonAttrs.isEmpty())
			return false;
		
		List<String> newKey = new ArrayList<String>(commonAttrs);
		for(String a : keyAttrs) {
			if(!newKey.contains(a))
				newKey.add(a);
		}
		key.setAttributes(newKey);//resort key attributes
		
		List<String> newFDLeft = new ArrayList<String>(commonAttrs);
		for(String a : FDLeftAttrs) {
			if(!newFDLeft.contains(a))
				newFDLeft.add(a);
		}
		fd.setLeftHand(newFDLeft);//resort FD left attributes
		
		return true;
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

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof Schema) {
			Schema pair = (Schema)obj;
			if(this.attr_set.containsAll(pair.getAttr_set()) && this.attr_set.size() == pair.getAttr_set().size()
					&& this.fd_set.containsAll(pair.getFd_set()) && this.fd_set.size() == pair.getFd_set().size())
				return true;
			else
				return false;
				
		}else
			return false;
	}


	
	@Override
	public int hashCode() {
		return Objects.hash(new HashSet<String>(attr_set), new HashSet<FD>(fd_set));
	}
	
	
	
}
