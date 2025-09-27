package entity;

import java.util.*;

/**
 * Represents a table especially designed for Quine-McCluskey
 *
 */
public class QMTable {
	/**
	 * Maps a list of implicant to its bit count( number of ones). 
	 * the first list is indexed by reduced mark
	 * second list is the implicants that has the same reduced mark value
	 */
	private List<List<List<Implicant>>> mcQuineSections; 
	
	/**
	 * the implicant number of the table
	 */
	private int implicantNum;
	
	
	/**
	 * McQuineTable constructor.
	 * @param size
	 */
	public QMTable(int implicantNum,List<List<List<Implicant>>> GroupedImps){
		this.implicantNum = implicantNum;
		this.mcQuineSections = GroupedImps;
	}
	/**
	 * Gets the number of sections of the table.
	 * @return section number
	 */
	public int getSectionNum(){
		int count = 0;
		for(List<List<Implicant>> i : this.mcQuineSections) {
			if(!i.isEmpty())
				count ++;
		}
		return count;
	}
	
	
	public int getImplicantNum() {
		return implicantNum;
	}

	/**
	 * Gets the sections of the table.
	 * @return the HashMap of the list of implicants and the corresponding bit count.
	 */
	public List<List<List<Implicant>>> getMcQuineSections(){
		return mcQuineSections;
	}
	/**
	 * Prints the section.
	 */
	public void printTable(){
		List<List<Implicant>> bufferGroup;
		for(int i = 0; i < mcQuineSections.size(); i++){
			
			bufferGroup = mcQuineSections.get(i);
			
			if(bufferGroup.isEmpty()){
				System.out.println("SECTION: "+i+"");
				System.out.println("EMPTY!");
			}
			else{
				System.out.println("SECTION: "+i+"");
				System.out.println("| BitCount | BinaryValue | Paired | MintermList");
				for(List<Implicant> impList :  bufferGroup){
					for(Implicant imp : impList) {
						System.out.println(imp.toString());
					}
					System.out.println("------------------");
				}
			}
			System.out.println();
		}
	}
	
	/**
	 * Gets the list of implicant.
	 * @return implicant list.
	 */
	public List<Implicant> getImplicantList(){
		List<Implicant> implicantList = new ArrayList<Implicant>();
		for(List<List<Implicant>> entry : mcQuineSections){
			for(List<Implicant> impList : entry){
				implicantList.addAll(impList);
			}
		}

		return implicantList;
	}

	/**
	 * Checks if sections contained in the table can be compared with each other. That is, if there exists
	 * two or more sections.
	 * @return boolean
	 */
	public boolean isComparable() {
			return getSectionNum() > 1;
	}
}
