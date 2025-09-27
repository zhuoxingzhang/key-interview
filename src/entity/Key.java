package entity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import util.Utils;

/**
 * a key refers to minimal key
 *
 */
public class Key {
	
	private List<String> key;
	
	public Key(Collection<String> key) {
		this.key = new ArrayList<String>();
		for(String a : key) {
			if(!this.key.contains(a))//keep non-redundant
				this.key.add(a);
		}
	}
	
	public Key(Collection<String> key, Boolean sortByNum) {
		this.key = new ArrayList<String>();
		for(String a : key) {
			if(!this.key.contains(a))//keep non-redundant
				this.key.add(a);
		}
		if(sortByNum)
			this.key = Utils.sortByNumbers(this.key);
	}

	public List<String> getAttributes() {
		return key;
	}
	
	public void setAttributes(List<String> newKeyAttrs) {
		this.key = newKeyAttrs;
	}
	
	public int size() {
		return this.key.size();
	}
	
	/**
	 * check if a key contains other key,
	 * e.g. key1={a,b,c} key2={a,b}, then key1 contains key2
	 * @param key
	 * @return true if it contains, false otherwise
	 */
	public boolean contains(Key key) {
		return this.key.containsAll(key.getAttributes());
	}

	@Override
	public int hashCode() {
		return Objects.hash(new HashSet<String>(key));
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Key other = (Key) obj;
		if(this.contains(other) && this.size() == other.size())
			return true;
		else
			return false;
	}
	
	
	@Override
	public String toString() {
		return key.toString();
	}
	

}
