package util;

import java.math.*;
import java.util.*;

import entity.*;

/**
 * The  QM represents the engine in performing the Quine-McCluskey Algorithm that is used
 * in generating a simplified boolean expression using the list of minterms as an input.
 */
public class QuineMcCluskey {
	/**
	 * Number of variables
	 */
	private final int LITERAL_COUNT;
	
	/**
	 * Array of Implicants See implicant class.
	 */
	private Implicant[] implicants;
	
	/**
	 * Array of minterms (in decimal representation).
	 */
	private int[] minterms;

	
	/**
	 * data structure used in storing the Prime Implicants
	 */
	private ArrayList<PrimeImplicant> primeImps, finalImps;
	
	/**
	 * The simplified boolean expression.
	 */
	private String output;
	
	/**
	 * Initializes the quine-mccluskey engine. 
	 * @param minTerms minTerms as input
	 * @param literalCount
	 */
	public QuineMcCluskey(List<Integer> minTerms,int literalCount) {
		int MINTERM_COUNT = minTerms.size();
		minterms = new int[MINTERM_COUNT];
		for(int i = 0; i < MINTERM_COUNT; i++){ //parse minterms
			minterms[i] = minTerms.get(i);
		}
		Arrays.sort(minterms); //sort minterms in ascending order to place the highest item in the last entry of the array 
		LITERAL_COUNT = literalCount;
		implicants = new Implicant[MINTERM_COUNT];
		String raw, formatted;
		int[] mintermList;
		for(int i = 0; i < MINTERM_COUNT; i++){
			mintermList = new int[]{minterms[i]};
			raw = Integer.toBinaryString(minterms[i]);
			formatted = String.format("%0"+LITERAL_COUNT+"d", new BigInteger(raw));
			implicants[i] = new Implicant(mintermList, formatted);
		}
		primeImps = new ArrayList<PrimeImplicant>();
		finalImps = new ArrayList<PrimeImplicant>();
		output = "";
	}

	/**
	 * Evaluates the number of literals/variables using the highest minterm value.
	 * @param maxMinterm the highest minterm value. 
	 * @return number of literals/variables to use for generating the simplified boolean expression.
	 */
	public int evaluateLiteralCount(int maxMinterm){
		int exponent = 0;
		while(maxMinterm >= Math.pow(2, exponent)){
			exponent++;
		}
		return exponent; 
	}

	/**
	 * Runs the core Quine-McCluskey algorithm in generating a simplified boolean expression.
	 */
	public String runQuineMcCluskey(boolean recMemory,List<Double> freeMemList,String memoryUnit){
		ArrayList<Implicant> primeImplicants = new ArrayList<Implicant>(); 
//		System.out.println("\nRunning Quine McCluskey...");
		String binaryResult;
		int[] mintermList;
		/*
		 * impList : first list's index refers to bit count,second list's index refers to implicant reduced mask value,
		 * inner list is implicant set that they have same bit count and reduced mask value. 
		 */
		List<List<List<Implicant>>> nextGroupedImps = new ArrayList<List<List<Implicant>>>();
		int nextGroupNum = 0;
		for(Implicant imp : implicants) {
			if(addImplicantToGroup(nextGroupedImps, imp))
				nextGroupNum ++;
		}
		QMTable bufferTable = new QMTable(nextGroupNum,nextGroupedImps);
		
		Implicant imp1, imp2;
		List<List<List<Implicant>>> sections;
		while(bufferTable.isComparable()){ //loop to compare all binary values. Needs refactoring.
			if(recMemory)
				freeMemList.add(Utils.getFreeMemory(memoryUnit));//record free memory size
			
			nextGroupedImps = new ArrayList<List<List<Implicant>>>();//reset
			nextGroupNum = 0;
			
			sections = bufferTable.getMcQuineSections();
//			System.out.println("table section number : "+sections.size());
//			System.out.println("current table's implicant number(non-redundant) : "+bufferTable.getImplicantNum());
			List<List<Implicant>> secAbove = null;
			List<List<Implicant>> secBelow = null;
			for(int s = 0;s < sections.size();s ++) {
				secBelow = sections.get(s);
				if(secAbove != null && !secAbove.isEmpty()){
//					System.out.println("section : "+ s +"/"+sections.size() +" | above : "+secAbove.size()+" | below : "+secBelow.size());
					int min_RM_value = secAbove.size() < secBelow.size() ? secAbove.size() : secBelow.size();
					for(int m = 0;m < min_RM_value;m ++) {
						//imps_above and imps_below both have reduced mask value of m
						List<Implicant> imps_above = secAbove.get(m);
						List<Implicant> imps_below = secBelow.get(m);
						if(imps_above.isEmpty() || imps_below.isEmpty()) {
							continue;
						}
						for(int i = 0; i < imps_above.size(); i++){
							for(int j = 0; j < imps_below.size(); j++){
								//imp1 and imp2 have the same reduced mask value
								imp1 = imps_above.get(i);
								imp2 = imps_below.get(j);
								if(this.isOneBitDiff(imp1.getTermValue(), imp2.getTermValue())) {
									binaryResult = evaluateBinaryValue(imp1.getBinaryValue(), imp2.getBinaryValue());
									mintermList = combineMinterms(imp1.getMinterms(), imp2.getMinterms());
									if(addImplicantToGroup(nextGroupedImps, new Implicant(mintermList, binaryResult)))
										nextGroupNum ++;
									imp1.setPaired(true);
									imp2.setPaired(true);
								}
							}
						}
					}
					for(List<Implicant> il : secAbove) {
						for(Implicant i : il) {
							if(!i.isPaired())
								primeImplicants.add(i);
						}
					}
				}
				secAbove = secBelow;
			}
			for(List<Implicant> il : secBelow) {
				for(Implicant i : il) {
					if(!i.isPaired())
						primeImplicants.add(i);
				}
			}
			bufferTable = new QMTable(nextGroupNum,nextGroupedImps);
		}
		//record last table's prime implicant
		for(Implicant imp : bufferTable.getImplicantList()) {
			if(!imp.isPaired())
				primeImplicants.add(imp);
		}

		primeImps = new ArrayList<PrimeImplicant>();
		for(Implicant imp : primeImplicants){
			primeImps.add(new PrimeImplicant(imp, imp.getMinterms()));
		}
		
		for(int mint : minterms){
			//find prime implicants that have only unique minterm
			List<PrimeImplicant> record = new ArrayList<>();
			for(PrimeImplicant prim : primeImps){
				if(prim.contains(mint)){
					record.add(prim);
					if(record.size() > 1)
						break;
				}
			}
			if(record.size() == 1) {
				PrimeImplicant pi = record.get(0);
				if(!finalImps.contains(pi)){
					finalImps.add(pi);
				}
			}
		}

		primeImps.removeAll(finalImps);
		for(PrimeImplicant fimp : finalImps){
			for(int m : fimp.getImplicant().getMinterms()){
				for(PrimeImplicant pi : primeImps){
					pi.markMinterm(m, false);
				}
			}
		}
		while(!primeImps.isEmpty()){
			PrimeImplicant dominant = null;//have the greatest mark value and least validBitCount value
			primeImps.sort(new Comparator<PrimeImplicant>() {//decreasing order of mark value of prime implicants
				@Override
				public int compare(PrimeImplicant o1, PrimeImplicant o2) {
					if(o1.getMarkCount() < o2.getMarkCount())
						return 1;
					else if(o1.getMarkCount() > o2.getMarkCount())
					    return -1;
					else
						return 0;
				}	
			});
		    int maxMarkCount = primeImps.get(0).getMarkCount();
		    int minValidBitCount = primeImps.get(0).getValidBitCount();
		    for(PrimeImplicant prime : primeImps) {
		    	if(prime.getMarkCount() == maxMarkCount) {
		    		if(minValidBitCount >= prime.getValidBitCount()) {
		    			minValidBitCount = prime.getValidBitCount();
		    			dominant = prime;
		    		}
		    	}else
		    		break;
		    }
			if(dominant.getMarkCount() != 0){
				finalImps.add(dominant);
				primeImps.remove(dominant);
			}
			for(int m : dominant.getImplicant().getMinterms()){//update mark/importance
				for(PrimeImplicant pi: primeImps){
					pi.markMinterm(m, false);
				}
			}
			boolean mark = true;
			for(PrimeImplicant pi : primeImps){
				if(pi.getMarkCount() > 0){
					mark = false;
					break;
				}
			}
			if(mark == true){
				break;
			}

		}
		if(!finalImps.isEmpty()){
			output = evaluateExpression(finalImps);
		}else{
			output = "1";
		}
		return output;
	}
	
	
	/**
	 * add a new implicant into group
	 * @param GroupedImps
	 * @param imp
	 */
	private boolean addImplicantToGroup(List<List<List<Implicant>>> GroupedImps, Implicant imp) {
		boolean success = false;
		int bitCount = imp.getBitCount();
		int reducedMask = imp.getReducedMask();
		int oldcap = GroupedImps.size();
		if(oldcap <= bitCount) {
			//extend capacity
			for(int i = 0;i <= bitCount - oldcap;i ++) {
				GroupedImps.add(new ArrayList<List<Implicant>>());
			}
		}
		List<List<Implicant>> sameBitCount = GroupedImps.get(bitCount);
		int oldsize = sameBitCount.size();
		if(oldsize > reducedMask) {
			List<Implicant> sameReducedMask = sameBitCount.get(reducedMask);
			if(!sameReducedMask.contains(imp)) {
				sameReducedMask.add(imp);
				success = true;
			}
		}else {
			//extend group
			for(int i = 0;i <= reducedMask - oldsize;i ++) {
				sameBitCount.add(new ArrayList<Implicant>());
			}
			List<Implicant> sameReducedMask = sameBitCount.get(reducedMask);
			sameReducedMask.add(imp);
			success = true;
		}
		return success;
	}
	
	/**
	 * Evaluates the resulting binary string after comparing two implicants' binary representations.   
	 * @param binary1 binary string to be compared. 
	 * @param binary2 binary string to be compared.
	 * @return binary string with characters either 1, 0, -
	 */
	public String evaluateBinaryValue(String binary1, String binary2) {
		StringBuilder result = new StringBuilder();
		for(int i = 0; i < binary1.length(); i++){
			if(binary1.charAt(i) == binary2.charAt(i)){
				result.append(binary1.charAt(i));
			}
			else{
				result.append("-");
			}
		}
		return new String(result);
	}

	/**
	 * Use in converting the binaryValue into a meaningful boolean expression.
	 * @param primeImplicants list of Prime Implicants 
	 * @return the final boolean expression.
	 */
	private String evaluateExpression(ArrayList<PrimeImplicant> primeImplicants) {
		String expression = new String("");
		String binaryBuffer;
		for(PrimeImplicant pi : primeImplicants){
			binaryBuffer = pi.getImplicant().getBinaryValue();
			for(int i = 0; i < binaryBuffer.length(); i++){
				if(binaryBuffer.charAt(i) != '-'){
					expression += getTerm(i);
					if(binaryBuffer.charAt(i) == '0'){
						expression += "'"; 
					}
					expression += ",";//splitter
				}
			}
			if(expression.charAt(expression.length() - 1) == ',')
				expression = expression.substring(0, expression.length()-1);//remove last splitter
			expression += "+";
		}

		return expression.substring(0, expression.length()-1);
	}

	/**
	 * Getting the character representation of the number.
	 * @param n the number to be converted as literal.
	 * @return literal character
	 */
	private String getTerm(int n){
		return ""+n;
	}

	
	private int[] combineMinterms(int[] mints1, int[] mints2) {
		int[] mints = new int[mints1.length + mints2.length];
		int index = 0;
		for(int i = 0; i < mints1.length; i++){
			mints[index++] = mints1[i];
		}

		for(int i = 0; i < mints2.length; i++){
			mints[index++] = mints2[i];
		}

		return mints;
	}

	
	/**
	 * judge if two binary values have difference on only 1 bit
	 * @param v1
	 * @param v2
	 * @return true if only one bit is different, false otherwise
	 */
	public boolean isOneBitDiff(int v1, int v2) {
		int xor = v1^v2;//exclusive OR operation
		return (xor & (xor - 1)) == 0;
	}

	/**
	 * Prints the list of minterms.
	 * @param minterms
	 */
	public void print(int[] minterms){
		for(int item : minterms){
			System.out.print(String.format("| %s |", item));
		}
		System.out.println();
	}

	/**
	 * Prints the list of implicants.
	 * @param implicants
	 */
	public void print(Implicant[] implicants) {
		for(Implicant item : implicants){
			System.out.println(item.toString());
		}
	}
	
	public void print(List<PrimeImplicant> implicants) {
		for(PrimeImplicant item : implicants){
			System.out.println(item.toString());
		}
	}
	/**
	 * main method
	 * @param args
	 */
	public static void main(String[] args) {
		/**
		 * minTerms as input
		 * e.g. given bool function f(a,b,c,d)
		 * if f(0,0,1,0) = 1 then [2] is one of minTerms for binary input [0010]
		 * 
		 */
		QuineMcCluskey engine = new QuineMcCluskey(Arrays.asList(0, 4, 5, 7, 8, 11, 12, 15),4);
		String result = engine.runQuineMcCluskey(false,null,null);
		System.out.println("The simplified expression is: "+result);
	}
}

