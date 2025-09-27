package entity;

import java.util.List;
/**
 * To store some temporary information of a 3NF schema
 *
 */
public class SchemaInfoCritical implements SchemaInterface{
	private FD fd;
	private List<String> subschema;
	private int optimTarget;//optimization target, e.g. FD number or FD attribute symbol number
	private int keyNum;//record the subschemata's number of minimal keys
	
	public SchemaInfoCritical(FD fd,List<String> subschema,int optimTarget) {
		this.fd = fd;
		this.subschema = subschema;
		this.optimTarget = optimTarget;
	}
	
	public SchemaInfoCritical(FD fd,List<String> subschema,int optimTarget,int keyNum) {
		this.fd = fd;
		this.subschema = subschema;
		this.optimTarget = optimTarget;
		this.keyNum = keyNum;
	}
	
	public SchemaInfoCritical() {
		
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

	public int getOptimTarget() {
		return optimTarget;
	}

	public void setOptimTarget(int optimTarget) {
		this.optimTarget = optimTarget;
	}

	public int getKeyNum() {
		return keyNum;
	}

	public void setKeyNum(int keyNum) {
		this.keyNum = keyNum;
	}

	@Override
	public boolean equals(Object obj) {
		if(obj instanceof SchemaInfoCritical) {
			SchemaInfoCritical schema = (SchemaInfoCritical)obj;
			if(this.subschema.containsAll(schema.getSubschema()) && this.subschema.size() == schema.getSubschema().size())
				return true;
			else
				return false;
				
		}else
			return false;
	}
	

}
