package bit.minisys.minicc.ncgen;

import bit.minisys.minicc.parser.ast.ASTNode;
public class Pair {
	private int func_num;
	private String var;	
	private String reg;
	public Pair(String temp1,String temp2,int num){
		var = temp1;
		reg = temp2;
		func_num = num;
	}
	public String getvar() {
		return var;
	}
	public String getreg() {
		return reg;
	}
	public int getnum() {
		return func_num;
	}
}

