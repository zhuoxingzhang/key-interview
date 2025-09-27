package entity;

import java.util.List;
/**
 * To store some temporary information of a schema
 *
 */
public class SchemaInfo implements SchemaInterface{
	private FD fd;
	private List<String> subschema;
	private int n_key;//key number
	
	public SchemaInfo(FD fd,List<String> subschema,int n_key) {
		this.fd = fd;
		this.subschema = subschema;
		this.n_key = n_key;
	}
	
	public SchemaInfo() {
		
	}
	
	
	public FD getFd() {
		return fd;
	}

	public void setFd(FD fd) {
		this.fd = fd;
	}

	public List<String> getSubschema() {
		return subschema;
	}



	public void setSubschema(List<String> subschema) {
		this.subschema = subschema;
	}



	public int getN_key() {
		return n_key;
	}



	public void setN_key(int n_key) {
		this.n_key = n_key;
	}



	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SchemaInfo) {
			SchemaInfo schema = (SchemaInfo)obj;
			if(this.subschema.containsAll(schema.getSubschema()) && this.subschema.size() == schema.getSubschema().size())
				return true;
			else
				return false;
				
		}else
			return false;
	}
	

}
