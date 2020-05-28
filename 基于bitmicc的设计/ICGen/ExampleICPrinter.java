package bit.minisys.minicc.icgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Vector;

import bit.minisys.minicc.parser.ast.ASTIdentifier;
import bit.minisys.minicc.parser.ast.ASTIntegerConstant;
import bit.minisys.minicc.parser.ast.ASTNode;

public class ExampleICPrinter {
	private  List<Quat> quats;
	private  Vector<Vector<Quat>> local_quats_foricgen;	
	private Vector<Quat> Gloabl_SymbolTable;
	private Vector<Quat> Gloabl_funTable; 
	private  int flag_func_th = 0;	
	public ExampleICPrinter(List<Quat> quats) {
		this.quats = quats;
	}
	public ExampleICPrinter(List<Quat> quats,Vector<Vector<Quat>> local_quats_foricgen,Vector<Quat> Gloabl_SymbolTable,Vector<Quat> Gloabl_funTable) {
		this.quats = quats;
		this.local_quats_foricgen = local_quats_foricgen;
		this.Gloabl_funTable = Gloabl_funTable;
		this.Gloabl_SymbolTable = Gloabl_SymbolTable;
	}
	
	public void print(String filename) {
		StringBuilder sb = new StringBuilder();
		for (Quat quat : Gloabl_SymbolTable) {
			String tem_op = quat.getOp();
			String tem_res = astStr(quat.getRes());
			String tem_opnd1 = astStr(quat.getOpnd1());
			String tem_opnd2 = astStr(quat.getOpnd2());
			String tem_temp = "("+tem_op+","+ tem_opnd1+","+tem_opnd2 +",$" + tem_res+")\n";
			sb.append(tem_temp);
		}
		for (Quat quat : quats) {
			String op = quat.getOp();
			if (op.equals("}")||op.equals(") {")) {
				sb.append(op+"\n");
				continue;
			}
			// 所有的可执行的额操作数才需要判断
			else if(op.equals("=")||op.equals("+")||op.equals("-")||op.equals("<")||op.equals("++")) {
				String res = astStr(quat.getRes());
				String opnd1 = astStr(quat.getOpnd1());
				String opnd2 = astStr(quat.getOpnd2());
				String localorglobal1  = checkglobalorlocal(opnd1);
				String localorglobal2  = checkglobalorlocal(opnd2);
				String localorglobal3  = checkglobalorlocal(res);
				String temp = "("+op+","+ localorglobal1+ opnd1+","+ localorglobal2+opnd2 +"," + localorglobal3+ res+")\n";
				sb.append(temp);
			}
			else if(op.equals("func")){
				String res = astStr(quat.getRes());
				String opnd1 = astStr(quat.getOpnd1());
				String opnd2 = astStr(quat.getOpnd2());
				String temp = "("+op+","+ opnd1+","+opnd2 +"," + res+")";
				temp+="{\n";
				sb.append(temp);
				for(Quat temp1: local_quats_foricgen.get(flag_func_th)) {
					String tem_op = temp1.getOp();
					String tem_res = astStr(temp1.getRes());
					String tem_opnd1 = astStr(temp1.getOpnd1());
					String tem_opnd2 = astStr(temp1.getOpnd2());
					String tem_temp = "("+tem_op+","+ tem_opnd1+","+tem_opnd2 +",%" + tem_res+")\n";
					sb.append(tem_temp);
				}
				++flag_func_th;
			}
			else if(op.equals("call")){
				// 没有考虑 函数的参数
				String res = astStr(quat.getRes());
				String temp = "("+op+",&"+ res+")\n";
				sb.append(temp);
			}
			else if(op.equals("return")){
				String res = astStr(quat.getRes());
				String localorglobal3  = checkglobalorlocal(res);
				String temp = "("+op+","+localorglobal3+ res+")\n";
				sb.append(temp);
			}
			else if(op.equals("while")){
				sb.append(op+"(\n");
			}
			else if(op.equals("if")){
				sb.append(op+"(\n");
			}
			
		}
		// write
		try {
			FileWriter fileWriter = new FileWriter(new File(filename));
			fileWriter.write(sb.toString());
			fileWriter.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private String checkglobalorlocal(String var) {
		// 用于检查当前变量是局部变量还是全局变量
		// 方法：检查符号表
		String ans = "";
		Vector<Quat> localsymboltable = local_quats_foricgen.get(flag_func_th-1);
		for(Quat temp:localsymboltable) {
			if (astStr(temp.getRes()).equals(var)) {
				ans = "%";
				break;
			}
		}
		if(ans.equals("")) {
			//在全局符号表中检查
			for(Quat temp:Gloabl_SymbolTable) {
				if (astStr(temp.getRes()).equals(var)) {
					ans = "$";
					break;
				}
			}
		}
		return ans;
	}
	
	private String astStr(ASTNode node) {
		if (node == null) {
			return "";
		}else if (node instanceof ASTIdentifier) {
			return ((ASTIdentifier)node).value;
		}else if (node instanceof ASTIntegerConstant) {
			return ((ASTIntegerConstant)node).value+"";
		}else if (node instanceof TemporaryValue) {
			return ((TemporaryValue)node).name();
		}else {
			return "";
		}
	}
}
