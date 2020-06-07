package bit.minisys.minicc.ncgen;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import bit.minisys.minicc.parser.ast.*;

public class asmbuilder implements ASTVisitor{

	private Map<ASTNode, ASTNode> map;				// ʹ��map�洢�ӽڵ�ķ���ֵ��key��Ӧ�ӽڵ㣬value��Ӧ����ֵ��valueĿǰ������ASTIdentifier,ASTIntegerConstant,TemportaryValue...
	private Integer tmpId;							// ��ʱ�������
	private Vector<Vector<Pair>> easy_symbol_table = new Vector<Vector<Pair>>();	// 简化版的符号表，只将运行时变量与寄存器绑定
	private String data_seg ="";
	private String code_seg ="";
	private String ass_code ="";
	private int flag_func_th = -1;	
	private int stringtmpId;
	private int iftmpId;
	private int fortmpId;
	private String current_funcname = null;
	
	public asmbuilder() {
		map = new HashMap<ASTNode, ASTNode>();
		tmpId = 0;
		stringtmpId = 0;
		iftmpId = 0;
		fortmpId = 0;
	}
	public asmbuilder(String tem1,String tem2,String tem3) {
		map = new HashMap<ASTNode, ASTNode>();
		tmpId = 0;
		ass_code = tem1;
		data_seg = tem2;
		code_seg = tem3;
		stringtmpId = 0;
		iftmpId = 0;
		fortmpId = 0;
	}
	public String get_ass_code() {
		return ass_code+data_seg+code_seg+"end __init";
	}

	@Override
	public void visit(ASTCompilationUnit program) throws Exception {
		for (ASTNode node : program.items) {
			if(node instanceof ASTFunctionDefine)
				visit((ASTFunctionDefine)node);
		}
	}

	@Override
	public void visit(ASTDeclaration declaration) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0;i<declaration.initLists.size();++i) {
			if(declaration.initLists.get(i).declarator.getClass().getSimpleName().equals("ASTArrayDeclarator")){
				visit((ASTArrayDeclarator)declaration.initLists.get(i).declarator);
				continue;
			}
			String ans = "";
			String reg = null;
			if(declaration.specifiers.get(0).value.equals("int")) {
				reg = "\tdword\t?\n";
			}
			String var_name = declaration.initLists.get(i).declarator.getName();
			var_name = "_"+current_funcname+var_name;
			if(!declaration.initLists.get(i).exprs.isEmpty()) {
				// 有初始化
//				System.out.println(declaration.initLists.get(i).exprs.get(0).getClass().getSimpleName());
				if(declaration.initLists.get(i).exprs.get(0).getClass().getSimpleName().equals("ASTFunctionCall")) {
					visit((ASTFunctionCall)declaration.initLists.get(i).exprs.get(0));
					//初始化的值保存到了特殊寄存器中,这里用eax替代
					String init_asm = "\tmov "+var_name+", eax\n";
					code_seg += init_asm;
				}
				else if(declaration.initLists.get(i).exprs.get(0).getClass().getSimpleName().equals("ASTIntegerConstant")){
					String init_asm = "\tmov "+var_name+", "+((ASTIntegerConstant)declaration.initLists.get(i).exprs.get(0)).value + "\n";
					code_seg += init_asm;
				}
			}
			ans = var_name+reg;
			data_seg += ans;
		}
		
	}

	@Override
	public void visit(ASTArrayDeclarator arrayDeclarator) throws Exception {
		// TODO Auto-generated method stub
		String array_name = "_"+current_funcname+arrayDeclarator.getName();
		if(arrayDeclarator.expr instanceof ASTIntegerConstant) {
			String reg = "\tdword\t"+ ((ASTIntegerConstant)arrayDeclarator.expr).value +" dup(?)\n";
			data_seg += (array_name+reg);
		}
		
	}

	@Override
	public void visit(ASTVariableDeclarator variableDeclarator) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTFunctionDeclarator functionDeclarator) throws Exception {
		// TODO Auto-generated method stub
		for(int i=0;i<functionDeclarator.params.size();++i) {
//			String ans = "";
//			String reg = null;
//			if(functionDeclarator.declarator.get(i).value.equals("int")) {
//				reg = "\tdword\t?\n";
//			}
//			String var_name = declaration.initLists.get(0).declarator.getName();
//			var_name = "&"+current_funcname+var_name;
//			if(!declaration.initLists.get(i).exprs.isEmpty()) {
//				// 有初始化
//			}
//			ans = var_name+reg;
//			data_seg += ans;
			visit(functionDeclarator.params.get(i));
		}

		
	}

	@Override
	public void visit(ASTParamsDeclarator paramsDeclarator) throws Exception {
		// TODO Auto-generated method stub
		String ans = "";
		String reg = null;
		if(paramsDeclarator.specfiers.get(0).value.equals("int")) {
			reg = "\tdword\t?\n";
		}
		String var_name = paramsDeclarator.declarator.getName();
		var_name = "_"+current_funcname+var_name;
		ans = var_name+reg;
		data_seg += ans;
		// 函数的形参要有传递的过程，出入stack
		String get_param = "\tmov esi, [esp+8]\n"	//只考虑一个参数 是8不是4的原因在于在函数的调用的时候先push了ebp，所以实际上是栈的第二个参数
							+"\tmov "+var_name+", esi\n";
		code_seg+=get_param;
	}

	@Override
	public void visit(ASTArrayAccess arrayAccess) throws Exception {
		// TODO Auto-generated method stub
			
	}

	@Override
	public void visit(ASTBinaryExpression binaryExpression) throws Exception {
		String op = binaryExpression.op.value;
		if (op.equals("=")) {
			String id1="";
			String id2="";
			String ass_asm = "";
			if(binaryExpression.expr1 instanceof ASTIdentifier) {
				id1 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr1).value;
			}
			else if(binaryExpression.expr1 instanceof ASTBinaryExpression) {
				String tmpreg = "_tmpreg"+(++tmpId)+"\tdword\t?\n";
				data_seg += tmpreg;
				visit((ASTBinaryExpression)binaryExpression.expr1);
				id1 = "_tmpreg"+(tmpId);
			}
			else if(binaryExpression.expr1 instanceof ASTArrayAccess) {
				ASTArrayAccess temp = (ASTArrayAccess)binaryExpression.expr1;
				String arrayname = ((ASTIdentifier)((ASTArrayAccess)binaryExpression.expr1).arrayName).value;
				String ele = "";
				if (temp.elements.get(0).getClass().getSimpleName().equals("ASTPostfixExpression")) {
					visit((ASTPostfixExpression)temp.elements.get(0));
					ele = ((ASTIdentifier)((ASTPostfixExpression)temp.elements.get(0)).expr).value;
				}
				else { 
					// 可能为其他的种类	
				}
				String arracc_asm = "\tmov edx, "+ "_"+current_funcname+ele +"\n"+
									"\tmov eax, 4\n"+
									"\timul edx, eax\n";
				
				code_seg+=arracc_asm;	//存入edx中备用
				id1 = "_"+ current_funcname+ arrayname+"[edx]";
			}
			
			if (binaryExpression.expr2 instanceof ASTIntegerConstant) {
				id2 = ((ASTIntegerConstant)binaryExpression.expr2).value.toString();
				ass_asm = "\tmov "+ id1+", "+ id2 +"\n";
			}
			else if(binaryExpression.expr2 instanceof ASTIdentifier)  {
				// 两个data的部分不能够直接ass
				id2 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr2).value;
				ass_asm = "\tmov eax, "+ id2 +"\n"+
							"\tmov "+id1+", eax\n"	;
			}
			
			code_seg+=ass_asm;
		}
		else if(op.equals("<=")) {
			String id1="";
			String id2="";
			if(binaryExpression.expr1 instanceof ASTIdentifier) {
				id1 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr1).value;
			}
			else if(binaryExpression.expr1 instanceof ASTBinaryExpression) {
				String tmpreg = "_tmpreg"+(++tmpId)+"\tdword\t?\n";
				data_seg += tmpreg;
				visit((ASTBinaryExpression)binaryExpression.expr1);
				id1 = "_tmpreg"+(tmpId);
			}
			if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
				id2 = ((ASTIntegerConstant)binaryExpression.expr2).value.toString();
			}
			else if(binaryExpression.expr2 instanceof ASTIdentifier)  {
				id2 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr2).value;
			}
			String comp_ass = "\tmov eax, "+id1+"\n"+
							"\tmov ebx, "+id2+"\n"+
							"\tcmp eax, ebx\n"+
							"\tjg ";	//最后接跳转的指令
			
			code_seg+=comp_ass;
		}
		else if(op.equals("<")) {
			String id1="";
			String id2="";
			if(binaryExpression.expr1 instanceof ASTIdentifier) {
				id1 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr1).value;
			}
			else if(binaryExpression.expr1 instanceof ASTBinaryExpression) {
				String tmpreg = "_tmpreg"+(++tmpId)+"\tdword\t?\n";
				data_seg += tmpreg;
				visit((ASTBinaryExpression)binaryExpression.expr1);
				id1 = "_tmpreg"+(tmpId);
			}
			if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
				id2 = ((ASTIntegerConstant)binaryExpression.expr2).value.toString();
			}
			else if(binaryExpression.expr2 instanceof ASTIdentifier)  {
				id2 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr2).value;
			}
			else if(binaryExpression.expr2 instanceof ASTBinaryExpression) {
				String tmpreg = "_tmpreg"+(++tmpId)+"\tdword\t?\n";
				data_seg += tmpreg;
				visit((ASTBinaryExpression)binaryExpression.expr2);
				id2 = "_tmpreg"+(tmpId);
			}
			String comp_ass = "\tmov eax, "+id1+"\n"+
							"\tmov ebx, "+id2+"\n"+
							"\tcmp eax, ebx\n"+
							"\tjg ";	//最后接跳转的label
			
			code_seg+=comp_ass;
		}
		else if(op.equals("*")) {
			String id1="";
			String id2="";
			if(binaryExpression.expr1 instanceof ASTIdentifier) {
				id1 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr1).value;
			}
			if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
				id2 = ((ASTIntegerConstant)binaryExpression.expr2).value.toString();
			}
			else if(binaryExpression.expr2 instanceof ASTIdentifier)  {
				id2 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr2).value;
			}
			String tmpreg = "_tmpreg"+(tmpId);
			String mul_ass = "\tmov eax, "+id1+"\n"+
							"\tmov ebx, "+id2+"\n"+
							"\timul eax, ebx\n"+
							"\tmov "+ tmpreg + ", eax\n";
			code_seg+=mul_ass;

		}
		else if(op.equals("/")) {
			String id1="";
			String id2="";
			if(binaryExpression.expr1 instanceof ASTIdentifier) {
				id1 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr1).value;
			}
			if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
				id2 = ((ASTIntegerConstant)binaryExpression.expr2).value.toString();
			}
			else if(binaryExpression.expr2 instanceof ASTIdentifier)  {
				id2 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr2).value;
			}
			String tmpreg = "_tmpreg"+(tmpId);
			String div_ass = "\tmov eax, "+id1+"\n"+
							"\tmov ebx, "+id2+"\n"+
							"\txor edx, edx\n"+			//要注意清空edx
							"\tdiv ebx\n"+
							"\tmov "+ tmpreg + ", eax\n";
			code_seg+=div_ass;
		}
		else if(op.equals("+")) {
			String id1="";
			String id2="";
			if(binaryExpression.expr1 instanceof ASTIdentifier) {
				id1 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr1).value;
			}
			else if(binaryExpression.expr1 instanceof ASTBinaryExpression) {
				String tmpreg = "_tmpreg"+(++tmpId)+"\tdword\t?\n";
				data_seg += tmpreg;
				visit((ASTBinaryExpression)binaryExpression.expr1);
				id1 = "_tmpreg"+(tmpId);
			}
			
			if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
				id2 = ((ASTIntegerConstant)binaryExpression.expr2).value.toString();
			}
			else if(binaryExpression.expr2 instanceof ASTIdentifier)  {
				id2 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr2).value;
			}
			String tmpreg = "_tmpreg"+(tmpId);
			String add_ass = "\tmov eax, "+id1+"\n"+
							"\tmov ebx, "+id2+"\n"+
							"\tadd eax,ebx\n"+			//要注意清空edx
							"\tmov "+ tmpreg + ", eax\n";
			code_seg+=add_ass;

		}
		else if(op.equals("%")) {
			String id1="";
			String id2="";
			if(binaryExpression.expr1 instanceof ASTIdentifier) {
				id1 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr1).value;
			}
			if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
				id2 = ((ASTIntegerConstant)binaryExpression.expr2).value.toString();
			}
			else if(binaryExpression.expr2 instanceof ASTIdentifier)  {
				id2 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr2).value;
			}
			String tmpreg = "_tmpreg"+(tmpId);
			String mod_ass = "\tmov eax, "+id1+"\n"+
							"\tmov ebx, "+id2+"\n"+
							"\txor edx, edx\n"+			//要注意清空edx
							"\tdiv ebx\n"+
							"\tmov "+ tmpreg + ", edx\n";
			code_seg+=mod_ass;

		}
		else if(op.equals("==")) {
			String id1="";
			String id2="";
			if(binaryExpression.expr1 instanceof ASTIdentifier) {
				id1 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr1).value;
			}
			else if(binaryExpression.expr1 instanceof ASTBinaryExpression) {
				String tmpreg = "_tmpreg"+(++tmpId)+"\tdword\t?\n";
				data_seg += tmpreg;
				visit((ASTBinaryExpression)binaryExpression.expr1);
				id1 = "_tmpreg"+(tmpId);
			}
			else if(binaryExpression.expr1 instanceof ASTIntegerConstant) {
				id1 = ((ASTIntegerConstant)binaryExpression.expr1).value.toString();
			}

			if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
				id2 = ((ASTIntegerConstant)binaryExpression.expr2).value.toString();
			}
			else if(binaryExpression.expr2 instanceof ASTIdentifier)  {
				id2 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr2).value;
			}
			String comp_ass = "\tmov eax, "+id1+"\n"+
					"\tmov ebx, "+id2+"\n"+
					"\tcmp eax, ebx\n"+
					"\tjne ";	//最后接跳转的label
			code_seg+=comp_ass;
		}
		else if(op.equals("-=")) {
			String id1="";
			String id2="";
			if(binaryExpression.expr1 instanceof ASTIdentifier) {
				id1 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr1).value;
			}
			else if(binaryExpression.expr1 instanceof ASTBinaryExpression) {
				String tmpreg = "_tmpreg"+(++tmpId)+"\tdword\t?\n";
				data_seg += tmpreg;
				visit((ASTBinaryExpression)binaryExpression.expr1);
				id1 = "_tmpreg"+(tmpId);
			}
			else if(binaryExpression.expr1 instanceof ASTIntegerConstant) {
				id1 = ((ASTIntegerConstant)binaryExpression.expr1).value.toString();
			}

			if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
				id2 = ((ASTIntegerConstant)binaryExpression.expr2).value.toString();
			}
			else if(binaryExpression.expr2 instanceof ASTIdentifier)  {
				id2 = "_"+ current_funcname+ ((ASTIdentifier)binaryExpression.expr2).value;
			}
			String me_ass = "\tmov eax, "+id1+"\n"+
					"\tmov ebx, "+id2+"\n"+
					"\tsub eax, ebx\n"+
					"\tmov "+id1 +", eax\n";
			code_seg+=me_ass;
		}
//		ASTNode res = null;
//		ASTNode opnd1 = null;
//		ASTNode opnd2 = null;
//		
//		if (op.equals("=")) {
//			// ��ֵ����
//			// ��ȡ����ֵ�Ķ���res
//			visit(binaryExpression.expr1);
//			res = map.get(binaryExpression.expr1);
//			// �ж�Դ����������, Ϊ�˱������a = b + c; ����������Ԫʽ��tmp1 = b + c; a = tmp1;�������Ҳ�����ñ�ķ������
//			if (binaryExpression.expr2 instanceof ASTIdentifier) {
//				opnd1 = binaryExpression.expr2;
//			}else if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
//				opnd1 = binaryExpression.expr2;
//			}else if(binaryExpression.expr2 instanceof ASTBinaryExpression) {
//				ASTBinaryExpression value = (ASTBinaryExpression)binaryExpression.expr2;
//				op = value.op.value;
//				visit(value.expr1);
//				opnd1 = map.get(value.expr1);
//				visit(value.expr2);
//				opnd2 = map.get(value.expr2);
//			}else {
//				// else ...
//			}
//			
//		}else if (op.equals("+")) {
//			// �ӷ�����������洢���м����
//			res = new TemporaryValue(++tmpId);
//			visit(binaryExpression.expr1);
//			opnd1 = map.get(binaryExpression.expr1);
//			visit(binaryExpression.expr2);
//			opnd2 = map.get(binaryExpression.expr2);
//		}else {
//			// else..
//		}
//		
//		// build quat
//		Quat quat = new Quat(op, res, opnd1, opnd2);
//		quats.add(quat);
//		map.put(binaryExpression, res);
	}

	@Override
	public void visit(ASTBreakStatement breakStat) throws Exception {
		// TODO Auto-generated method stub
		String current_loop = "_"+fortmpId+"LoopEndLabel";
		String break_asm = "\tjmp "+current_loop+"\n";
		code_seg += break_asm;
	}

	@Override
	public void visit(ASTContinueStatement continueStatement) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTCastExpression castExpression) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTCharConstant charConst) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTCompoundStatement compoundStat) throws Exception {
		for (ASTNode node : compoundStat.blockItems) {
			if(node instanceof ASTDeclaration) {
				visit((ASTDeclaration)node);
			}else if (node instanceof ASTStatement) {
				visit((ASTStatement)node);
			}
		}
	}

	@Override
	public void visit(ASTConditionExpression conditionExpression) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTExpression expression) throws Exception {
		if(expression instanceof ASTArrayAccess) {
			visit((ASTArrayAccess)expression);
		}else if(expression instanceof ASTBinaryExpression) {
			visit((ASTBinaryExpression)expression);
		}else if(expression instanceof ASTCastExpression) {
			visit((ASTCastExpression)expression);
		}else if(expression instanceof ASTCharConstant) {
			visit((ASTCharConstant)expression);
		}else if(expression instanceof ASTConditionExpression) {
			visit((ASTConditionExpression)expression);
		}else if(expression instanceof ASTFloatConstant) {
			visit((ASTFloatConstant)expression);
		}else if(expression instanceof ASTFunctionCall) {
			visit((ASTFunctionCall)expression);
		}else if(expression instanceof ASTIdentifier) {
			visit((ASTIdentifier)expression);
		}else if(expression instanceof ASTIntegerConstant) {
			visit((ASTIntegerConstant)expression);
		}else if(expression instanceof ASTMemberAccess) {
			visit((ASTMemberAccess)expression);
		}else if(expression instanceof ASTPostfixExpression) {
			visit((ASTPostfixExpression)expression);
		}else if(expression instanceof ASTStringConstant) {
			visit((ASTStringConstant)expression);
		}else if(expression instanceof ASTUnaryExpression) {
			visit((ASTUnaryExpression)expression);
		}else if(expression instanceof ASTUnaryTypename){
			visit((ASTUnaryTypename)expression);
		}
	}

	@Override
	public void visit(ASTSelectionStatement selectionStat) throws Exception {
		// TODO Auto-generated method stub
		int othertmpId = 0;	//可能有多个otherwise
		String labelotherwise = "_"+(++iftmpId)+"otherwise";
		String labelendif = "_"+(iftmpId)+"endif";
		//选择判断
		for(int i=0;i<selectionStat.cond.size();++i) {
			if (selectionStat.cond.get(i)instanceof ASTBinaryExpression) {
				visit((ASTBinaryExpression)selectionStat.cond.get(i));
				code_seg += labelendif+"\n";
			}
		}
		
		//选择内部
		visit((ASTCompoundStatement)selectionStat.then);
		
		// 跳过了otherwise的寻找，直接添加了标签
		code_seg+= (labelotherwise+(++othertmpId)+":\n");
		//选择结束
		code_seg+= (labelendif+":\n");
	}

	@Override
	public void visit(ASTIterationStatement iterationStat) throws Exception {
		// TODO Auto-generated method stub
		String LoopCheckLabel = "_"+(++fortmpId)+"LoopCheckLabel";
		String LoopStepLabel = "_"+(fortmpId)+"LoopStepLabel";
		String LoopEndLabel = "_"+(fortmpId)+"LoopEndLabel";
		
		//初始化for循环
		for(int i=0;i<iterationStat.init.size();++i){
			if(iterationStat.init.get(i) instanceof ASTBinaryExpression) {
				visit((ASTBinaryExpression)iterationStat.init.get(i));
			}
		}
		String jump = "\tjmp ";
		
		code_seg+=(LoopCheckLabel+":\n");
		//边界比较
		for(int i=0;i<iterationStat.cond.size();++i){
			if(iterationStat.cond.get(i) instanceof ASTBinaryExpression) {
				visit((ASTBinaryExpression)iterationStat.cond.get(i));
				code_seg+= LoopEndLabel+"\n";
			}
		}
		
		//循环体
		visit((ASTCompoundStatement)iterationStat.stat);
		
		//循环递增
		code_seg+=(LoopStepLabel+":\n");
		for(int i=0;i<iterationStat.step.size();++i){
			if(iterationStat.step.get(i) instanceof ASTPostfixExpression) {
				visit((ASTPostfixExpression)iterationStat.step.get(i));
			}
		}
		code_seg+=(jump+LoopCheckLabel+"\n");
		//循环结束
		code_seg+= (LoopEndLabel+":\n");
	}
	
	@Override
	public void visit(ASTPostfixExpression postfixExpression) throws Exception {
		// TODO Auto-generated method stub
		String op = postfixExpression.op.value;
		if(op.equals("++")) {
			String var_name = "_"+current_funcname+((ASTIdentifier)postfixExpression.expr).value;
			String pos_asm = "\tmov eax, "+var_name +"\n"+
							"\tinc eax\n"+
							"\tmov "+var_name+", eax\n";
			code_seg += pos_asm;
		}
	}
	
	@Override
	public void visit(ASTExpressionStatement expressionStat) throws Exception {
		for (ASTExpression node : expressionStat.exprs) {
			visit((ASTExpression)node);
		}
	}

	@Override
	public void visit(ASTFloatConstant floatConst) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTFunctionCall funcCall) throws Exception {
		// TODO Auto-generated method stub
		String func_call = "\tcall ";
		String func_name = ((ASTIdentifier)funcCall.funcname).value;
		func_call +=(func_name+"\n");
		String push_stat = "\tpush edx\n";	// 统一用edx
		
		String expand_stack_4 = "\tsub esp, 4\n";
		
		// 需要将输入的参数输入到edx中 或者进一步处理
		if(funcCall.argList.isEmpty()) {
			//没有参数 
			//有可能初始化，如何获得需要初始化后保存的寄存器的名字是一个问题
			//将函数调用返回值保存在一个特殊的寄存器中，然后在上一层搞就没什么问题了
			// 类似于MapleIR思想
//			System.out.println("hello");
			code_seg = code_seg + expand_stack_4 + func_call;
			return;
		}
		else if(funcCall.argList.get(0).getClass().getSimpleName().equals("ASTStringConstant")) {
			String str_con = ((ASTStringConstant)funcCall.argList.get(0)).value;
			str_con = str_con.substring(1, str_con.length()-3);
			str_con = "sc\tdb\t\"" +str_con+"\",0ah,0\n";
			str_con = "_"+(++stringtmpId)+ str_con;
			data_seg += str_con;
			String getaddr = "\tlea edx, _"+stringtmpId+"sc\n";
			code_seg = code_seg + getaddr + expand_stack_4 + push_stat + func_call;
			return;
		}
		else if(funcCall.argList.get(0).getClass().getSimpleName().equals("ASTIdentifier")) {
			String var = ((ASTIdentifier)funcCall.argList.get(0)).value;
			String move2edx = "\tmov edx, _"+ current_funcname+var+"\n";
			code_seg = code_seg + move2edx + expand_stack_4 + push_stat + func_call;
			return;
		}
		else if(funcCall.argList.get(0).getClass().getSimpleName().equals("ASTIntegerConstant")) {
			int var = ((ASTIntegerConstant)funcCall.argList.get(0)).value;
			String move2edx = "\tmov edx, "+var+"\n";
			code_seg = code_seg + move2edx + expand_stack_4 + push_stat + func_call;
			return;
		}
		
		
	}

	@Override
	public void visit(ASTGotoStatement gotoStat) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTIdentifier identifier) throws Exception {
		map.put(identifier, identifier);
	}

	@Override
	public void visit(ASTInitList initList) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTIntegerConstant intConst) throws Exception {
		map.put(intConst, intConst);
	}

	@Override
	public void visit(ASTIterationDeclaredStatement iterationDeclaredStat) throws Exception {
		// TODO Auto-generated method stub
		
	}

 
	@Override
	public void visit(ASTLabeledStatement labeledStat) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTMemberAccess memberAccess) throws Exception {
		// TODO Auto-generated method stub
		
	}



	@Override
	public void visit(ASTReturnStatement returnStat) throws Exception {
		// TODO Auto-generated method stub
		String res = "\tmov edx, ";	// 要搞清楚返回值是什么然后拼上去
		String res_sp = "\tmov __retval, edx\n";	// 保存到特殊寄存器中
		String res2 = "\tmov eax, edx\n" + 
				"	mov esp, ebp\n" + 
				"	pop ebp\n" + 
				"	ret\n";
		String ret_num = "";
		if(returnStat.expr == null) {
			code_seg = code_seg+res2;
			return;
		}
		else if(returnStat.expr.getFirst().getClass().getSimpleName().equals("ASTIntegerConstant")) {
			ret_num += ((ASTIntegerConstant)returnStat.expr.getFirst()).value;
			ret_num +="\n";
		}
		else if(returnStat.expr.getFirst().getClass().getSimpleName().equals("ASTIdentifier")) {
			ret_num += ("_"+current_funcname+(((ASTIdentifier)returnStat.expr.getFirst()).value));
			ret_num +="\n";
		}
		
		// TODO 寄存器
		
		code_seg = code_seg+res+ret_num+res_sp+res2;
	}

	

	@Override
	public void visit(ASTStringConstant stringConst) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTTypename typename) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTUnaryExpression unaryExpression) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTUnaryTypename unaryTypename) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTFunctionDefine functionDefine) throws Exception {
		// 访问到当前函数命名的地方
		++flag_func_th;
		String func_init = ":\n	push ebp\n" + 
				"	mov ebp, esp\n";
		String func_name = functionDefine.declarator.getName();
		code_seg = code_seg+func_name+func_init;
		current_funcname = func_name;
		
		visit((ASTFunctionDeclarator)functionDefine.declarator);
		visit(functionDefine.body);
	}

	@Override
	public void visit(ASTDeclarator declarator) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTStatement statement) throws Exception {
		if(statement instanceof ASTIterationDeclaredStatement) {
			visit((ASTIterationDeclaredStatement)statement);
		}else if(statement instanceof ASTIterationStatement) {
			visit((ASTIterationStatement)statement);
		}else if(statement instanceof ASTCompoundStatement) {
			visit((ASTCompoundStatement)statement);
		}else if(statement instanceof ASTSelectionStatement) {
			visit((ASTSelectionStatement)statement);
		}else if(statement instanceof ASTExpressionStatement) {
			visit((ASTExpressionStatement)statement);
		}else if(statement instanceof ASTBreakStatement) {
			visit((ASTBreakStatement)statement);
		}else if(statement instanceof ASTContinueStatement) {
			visit((ASTContinueStatement)statement);
		}else if(statement instanceof ASTReturnStatement) {
			visit((ASTReturnStatement)statement);
		}else if(statement instanceof ASTGotoStatement) {
			visit((ASTGotoStatement)statement);
		}else if(statement instanceof ASTLabeledStatement) {
			visit((ASTLabeledStatement)statement);
		}
	}

	@Override
	public void visit(ASTToken token) throws Exception {
		// TODO Auto-generated method stub
		
	}

}
