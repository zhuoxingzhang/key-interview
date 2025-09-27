package entity;

import java.util.ArrayList;
import java.util.List;


public class FuncDepen{
	private List<Integer> lhs;
	private List<Integer> rhs;
	
	public FuncDepen() {
	}
	
	public FuncDepen(List<Integer> lhs, List<Integer> rhs) {
		this.lhs = lhs;
		this.rhs = rhs;
	}

	public List<Integer> getLhs() {
		return lhs;
	}

	public void setLhs(List<Integer> lhs) {
		this.lhs = lhs;
	}

	public List<Integer> getRhs() {
		return rhs;
	}

	public void setRhs(List<Integer> rhs) {
		this.rhs = rhs;
	}

	public static List<FuncDepen> convertFrom(List<FD> fds){
		List<FuncDepen> res = new ArrayList<FuncDepen>();
		for(FD fd : fds) {
			List<Integer> LHS = new ArrayList<Integer>();
			List<Integer> RHS = new ArrayList<Integer>();
			fd.getLeftHand().forEach(a -> LHS.add(Integer.parseInt(a)));
			fd.getRightHand().forEach(a -> RHS.add(Integer.parseInt(a)));
			res.add(new FuncDepen(LHS,RHS));
		}
		return res;
	}
}
