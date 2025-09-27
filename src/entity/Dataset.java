package entity;

/**
 * 
 *	a data set object with some information for experiments
 */
public class Dataset {
	public  String name;
	public  int col_num;
	public  int row_num;
	public  String split;//the split the data set is divided by
	public String nullMarker;//presentation of a null marker
	public DataTypeEnum DataType;//COMPLETE(no null values)/NULL EQUALITY(null equals null)/NULL UNCERTAINTY(null is uncertain)
	
	public Dataset() {}
	/**
	 * 
	 * @param name
	 * @param col_num
	 * @param row_num
	 * @param split the splitter between values of a row
	 * @param nullMarker
	 * @param dataType COMPLETE(no null values)/NULL EQUALITY(null equals null)/NULL UNCERTAINTY(null is uncertain)
	 */
	public Dataset(String name, int col_num, int row_num, String split, String nullMarker,DataTypeEnum dataType) {
		this.name = name;
		this.col_num = col_num;
		this.row_num = row_num;
		this.split = split;
		this.nullMarker = nullMarker;
		this.DataType = dataType;
		
	}
	
}
