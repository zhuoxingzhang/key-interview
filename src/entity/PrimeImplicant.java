package entity;

import java.util.*;
import java.util.Map.Entry;

/**
 * Represents a prime implicant class.
 *
 */
public class PrimeImplicant {
	/**
	 * The given implicant.
	 */
	private Implicant imp;
	
	private int validBitCount;//if bianry value is 0 0 - 1,then validBitCount = 3, not count '-'
	
	/**
	 * A hashmap mapping the minterms and its corresponding mark given by the implicant.
	 */
	private HashMap<Integer, Boolean> mintermList;
	
	/**
	 * PrimeImplicant constructor.
	 * @param imp
	 * @param mintermList
	 */
	public PrimeImplicant(Implicant imp, int[] mintermList){
		this.imp = imp;
		this.mintermList= new HashMap<Integer, Boolean>();
		this.setValidBitCount(imp);
		initializeMintermList(mintermList);
	}

	/**
	 * Initializes the list of minterms.
	 * @param mintlist
	 */
	private void initializeMintermList(int[] mintlist) {
		for(Integer mint : mintlist){
			mintermList.put(mint, true);
		}
	}
	
	/**
	 * Marks a minterm.
	 * @param minterm
	 * @param key
	 */
	public void markMinterm(int minterm, boolean key){
		if(mintermList.containsKey(minterm))
			mintermList.put(minterm, key);
	}
	
	/**
	 * Checks if the given minterm is in the prime implicant's list.
	 * @param minterm
	 * @return true if the list contains the minterm, else false.
	 */
	public boolean contains(int minterm){
		for(int mint : imp.getMinterms()){
			if(mint == minterm){
				return true;
			}
		}
		return false;
	}
	
	public int getValidBitCount() {
		return validBitCount;
	}

	public void setValidBitCount(Implicant imp) {
		String binaryValue = imp.getBinaryValue();
		this.validBitCount = 0;
		for(char c : binaryValue.toCharArray()) {
			if(c != '-')
				this.validBitCount ++;
		}
	}

	/**
	 * Returns a string representation of a prime implicant.
	 */
//	public String toString(){
//		String s = new String("");
//		for(Entry<Integer, Boolean> mint : mintermList.entrySet()){
//			s+="| ";
//			if(mint.getValue() == true){
//				s += "x";
//			}
//			else{
//				s +="_";
//			}
//			s+=" |";
//		}
//		
//		s += "| " + Arrays.toString(imp.getMinterms());
//		s+= "  |  " + this.getMarkCount();
//		s+= "  |  " + this.validBitCount;
//		return s;
//	}
	public String toString(){
		String s = new String("");
		s += "| " + imp.getBinaryValue();
		s+= "  |  " + this.getMarkCount();
		s+= "  |  " + this.validBitCount;
		return s;
	}
	
	/**
	 * Gets the number of marks of a prime implicant.
	 * @return number of marks
	 */
	public int getMarkCount(){
		int count = 0;
		
		for(Entry<Integer, Boolean> mint : mintermList.entrySet()){
			if(mint.getValue() == true){
				count++;
			}
		}
		
		return count;
	}
	
	/**
	 * Gets the implicant data structure of the prime implicant. 
	 * @return Implicant
	 */
	public Implicant getImplicant(){
		return imp;
	}

	@Override
	public int hashCode() {
		return Objects.hash(imp);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		PrimeImplicant other = (PrimeImplicant) obj;
		return Objects.equals(imp, other.imp);
	}
	
	
}
