package entity;

import java.util.HashSet;
import java.util.List;
import java.util.Objects;

import util.Utils;
/**
 * functional dependency
 * format:
 * leftHand -> rightHand
 */
public class FD {
	private List<String> leftHand;
	private List<String> rightHand;
	
	public FD() {
		
	}
	
	public FD(List<String> leftHand, List<String> rightHand) {
		this.leftHand = leftHand;
		this.rightHand = rightHand;
	}
	
	public FD(List<String> leftHand, List<String> rightHand, Boolean sortByNum) {
		if(sortByNum) {
			this.leftHand = Utils.sortByNumbers(leftHand);
			this.rightHand = Utils.sortByNumbers(rightHand);
		}else {
			this.leftHand = leftHand;
			this.rightHand = rightHand;
		}
	}
	
	public FD(List<String> leftHand, List<String> rightHand,int level,int n_key) {
		this.leftHand = leftHand;
		this.rightHand = rightHand;
	}

	public List<String> getLeftHand() {
		return leftHand;
	}

	public void setLeftHand(List<String> leftHand) {
		this.leftHand = leftHand;
	}

	public List<String> getRightHand() {
		return rightHand;
	}

	public void setRightHand(List<String> rightHand) {
		this.rightHand = rightHand;
	}
	


	@Override
	public boolean equals(Object obj) {
		if(obj instanceof FD) {
			FD fd = (FD)obj;
			if(fd.getLeftHand().containsAll(this.leftHand) && fd.getLeftHand().size() == this.leftHand.size() &&
					fd.getRightHand().containsAll(this.rightHand) && fd.getRightHand().size() == this.rightHand.size())
				return true;
			else
				return false;
				
		}else
			return false;
	}
	
	

	@Override
	public String toString() {
		return leftHand + "->" + rightHand;
	}

	@Override
	public int hashCode() {
		return Objects.hash(new HashSet<String>(leftHand), new HashSet<String>(rightHand));
	}

	
	
}
