package entity;

import java.util.*;

/**
 * Represents an Implicant class.
 *
 */
public class Implicant{
	/**
	 * Number of 1-bits
	 */
	private int bitCount;
	/**
	 * Array of minterms in decimal representation.
	 */
	private int[] minterms;
	/**
	 * The binary representation of the Implicant.
	 */
	private String binaryValue;
	/**
	 * Boolean to check if the implicant is already compared. 
	 */
	private boolean isPaired; 
	
	/**
	 * a binary number with some bits masked like [1 - - 0]
	 * then the reduced mark is 4 + 2 = 6
	 */
	private int reducedMask;
	
	
	private int termValue;//binary integer '-' will be replaced by 0, then get int value
	/**
	 * Implicant constructor.
	 * @param minterms
	 * @param binaryValue
	 */
	public Implicant(int[] minterms, String binaryValue) {
		this.binaryValue = new String(binaryValue);
		this.minterms = minterms;
		setTermValue(binaryValue);
		setReducedMask(binaryValue);
		setPaired(false);
		setBitCount();
	}
	
	
	public void setTermValue(String binaryValue) {
		//binary value '-' will be replaced by 0
		termValue = 0;
		int index = -1;
		for(int i = binaryValue.length() - 1;i  >= 0; i --) {
			index ++;
			if(binaryValue.charAt(i) == '1') {
				termValue += Math.pow(2, index);
			}
		}
		
	}
	
	public int getTermValue() {
		return termValue;
	}
	


	public int getReducedMask() {
		return reducedMask;
	}

	private void setReducedMask(String binaryValue) {
		reducedMask = 0;
		char[] chars = binaryValue.toCharArray();
		for(int i = 0;i < chars.length;i ++){
			char digit = chars[i];
			if(digit == '-'){
				reducedMask += Math.pow(2, chars.length - i - 1);
			}
		}
	}
	


	/**
	 * Sets the number of bits based on the binary representation.
	 */
	private void setBitCount(){
		bitCount = 0;
		for(char digit : binaryValue.toCharArray()){
			if(digit == '1'){
				bitCount++;
			}
		}
	}
	
	/**
	 * Gets the bit count of the implicant.
	 * @return number of bits.
	 */
	public int getBitCount(){
		return bitCount;
	}
	
	/**
	 * Gets the first minterm.
	 * @return int
	 */
	public int getMinterm(){
		return minterms[0];
	}
	
	/**
	 * Gets the array of minterms.
	 * @return array of int
	 */
	public int[] getMinterms(){
		return minterms;
	}
	
	/**
	 * Gets the number of minterms.
	 * @return integer
	 */
	public int getMintermSize() {
		return minterms.length;
	}

	/**
	 * Gets the boolean that tells if the implicant is already compared or not.
	 * @return boolean
	 */
	public boolean isPaired() {
		return isPaired;
	}
	
	/**
	 * Sets if the boolean is already compared or not.
	 * @param isPaired
	 */
	public void setPaired(boolean isPaired) {
		this.isPaired = isPaired;
	}
	


	/**
	 * Gets the binary representation of the implicant.
	 * @return String
	 */
	public String getBinaryValue(){
		return binaryValue;
	}
	
	
	@Override
	public int hashCode() {
		return Objects.hash(binaryValue);
	}


	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Implicant other = (Implicant) obj;
		return Objects.equals(binaryValue, other.binaryValue);
	}


	/**
	 * Returns the string representation of the implicant. 
	 */
	public String toString(){
		return String.format("| %8d | %11s | %5s | %s ", bitCount, binaryValue, isPaired, Arrays.toString(minterms));
	}	
}
