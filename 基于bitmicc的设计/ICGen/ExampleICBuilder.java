package bit.minisys.minicc.icgen;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Stack;
import java.util.Vector;

import bit.minisys.minicc.parser.ast.*;
// 一个简单样例，只实现了加法
public class ExampleICBuilder implements ASTVisitor{

	private Map<ASTNode, ASTNode> map;				// 使用map存储子节点的返回值，key对应子节点，value对应返回值，value目前类别包括ASTIdentifier,ASTIntegerConstant,TemportaryValue...
	private List<Quat> quats;						// 生成的四元式列表
	private Integer tmpId;							// 临时变量编号

	// 符号表
	private Vector<Quat> Gloabl_SymbolTable = new Vector<Quat>();	//只存储全局符号表
	private Vector<Quat> Gloabl_funTable = new Vector<Quat>();  //全局函数表，在每次的functiondefine中填写检查
	
	private Vector<Vector<Quat>> local_quats_foricgen = new Vector<Vector<Quat>>();	
	private int flag_func_th = -1;					// 记录是第几个函数
	
	public ExampleICBuilder() {
		map = new HashMap<ASTNode, ASTNode>();
		quats = new LinkedList<Quat>();
		tmpId = 0;
	}
	public List<Quat> getQuats() {
		return quats;
	}
	public Vector<Vector<Quat>> getlocalsymbol() {
		return local_quats_foricgen;
	}
	public Vector<Quat> getglobalsymbol() {
		return Gloabl_SymbolTable;
	}
	public Vector<Quat> getfuncsymbol() {
		return Gloabl_funTable;
	}


	
	@Override
	public void visit(ASTCompilationUnit program) throws Exception {
		for (ASTNode node : program.items) {
			if(node instanceof ASTFunctionDefine) {
				visit((ASTFunctionDefine)node);
				Quat quat = new Quat("}", null,null, null);
				quats.add(quat);
			}
			else if(node instanceof ASTDeclaration){
				// 这个时候局部函数表的index为-1
				visit((ASTDeclaration)node);
			}
		}
		
	}

	@Override
	public void visit(ASTArrayDeclarator arrayDeclarator) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTVariableDeclarator variableDeclarator) throws Exception {
		// TODO Auto-generated method stub
		map.put(variableDeclarator, variableDeclarator);
	}

	@Override
	public void visit(ASTFunctionDeclarator functionDeclarator) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTParamsDeclarator paramsDeclarator) throws Exception {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void visit(ASTArrayAccess arrayAccess) throws Exception {
		// TODO Auto-generated method stub
		
	}
	
	@Override
	public void visit(ASTDeclaration declaration) throws Exception {
		// TODO Auto-generated method stub
		String type_name = declaration.specifiers.get(0).value;
		if(flag_func_th>=0) {
			Vector<Quat> local_symbol = local_quats_foricgen.get(flag_func_th);
			Quat quat = null;
			for(ASTInitList temp:declaration.initLists) {
				if(temp.declarator.getClass().getSimpleName().equals("ASTVariableDeclarator")){
					ASTVariableDeclarator temp1 = (ASTVariableDeclarator)temp.declarator;
					//这里不能仅用visit就实现生成声明节点，因为variableDeclarator这个节点是没有type_name这个信息的
					visit(temp1);
					ASTIdentifier tem1 = new ASTIdentifier();
					ASTIdentifier tem2 = new ASTIdentifier();
					// 在最终输出的时候检查是否为局部变量
					tem2.value =  temp1.getName();
					tem1.value = type_name;
					//声明节点进入局部符号表，初始化还需要添加一条语句
					quat = new Quat("var",tem2, tem1,null);			
	//				quats.add(quat);
					
					if (!temp.exprs.isEmpty()) {
						
						if(temp.exprs.get(0).getClass().getSimpleName().equals("ASTIntegerConstant")) {
							ASTIntegerConstant init_value = (ASTIntegerConstant) temp.exprs.get(0);
							
							ASTBinaryExpression init_temp = new ASTBinaryExpression();
							init_temp.op = new ASTToken();
							init_temp.op.value = "=";
	//						System.out.println(temp.exprs.get(0).getClass().getSimpleName());
							init_temp.expr2 = init_value;
	//						System.out.println(temp.exprs.get(0).getClass().getSimpleName());
							init_temp.expr1 = tem2;
							visit(init_temp);
						}
					}
					
				}
				//map 函数
				// 加入局部符号表
				local_symbol.add(quat);
			}
		}
		else {
			//全部变量声明
			Quat quat = null;
			for(ASTInitList temp:declaration.initLists) {
				if(temp.declarator.getClass().getSimpleName().equals("ASTVariableDeclarator")){
					ASTVariableDeclarator temp1 = (ASTVariableDeclarator)temp.declarator;
					//这里不能仅用visit就实现生成声明节点，因为variableDeclarator这个节点是没有type_name这个信息的
					visit(temp1);
					ASTIdentifier tem1 = new ASTIdentifier();
					ASTIdentifier tem2 = new ASTIdentifier();
					// 在最终输出的时候检查是否为局部变量
					tem2.value =  temp1.getName();
					tem1.value = type_name;
					//声明节点进入局部符号表，初始化还需要添加一条语句
					quat = new Quat("var",tem2, tem1,null);				
					if (!temp.exprs.isEmpty()) {
						
						if(temp.exprs.get(0).getClass().getSimpleName().equals("ASTIntegerConstant")) {
							ASTIntegerConstant init_value = (ASTIntegerConstant) temp.exprs.get(0);
							
							ASTBinaryExpression init_temp = new ASTBinaryExpression();
							init_temp.op = new ASTToken();
							init_temp.op.value = "=";
	//						System.out.println(temp.exprs.get(0).getClass().getSimpleName());
							init_temp.expr2 = init_value;
	//						System.out.println(temp.exprs.get(0).getClass().getSimpleName());
							init_temp.expr1 = tem2;
							visit(init_temp);
						}
					}
				}
				Gloabl_SymbolTable.add(quat);
			}
		}
	}

	@Override
	public void visit(ASTReturnStatement returnStat) throws Exception {
		// TODO Auto-generated method stub
		String op = "return";
		// 只考虑一个返回表达式
		ASTExpression expr = returnStat.expr.getFirst();
		ASTNode res = null;
		
		visit(expr);
		res = map.get(expr);
		Quat quat = new Quat(op,res, null,null);
		quats.add(quat);
		map.put(returnStat, res);
	}
	
	@Override
	public void visit(ASTFunctionDefine functionDefine) throws Exception {
		//确保选择某一个局部变量表
		++flag_func_th;
		Vector<Quat> here = new Vector<Quat>();
		local_quats_foricgen.add(here);
//		System.out.println("hello");
		
		// 添加函数名称节点
		String op = "func";
		String ret_type = functionDefine.specifiers.get(0).value;
		String func_name = "&" + functionDefine.declarator.getName();
		//转换成quat需要的节点
//		System.out.println(op+" "+ret_type+" "+func_name);
		// TODO 参数
		ASTIdentifier temp1 = new ASTIdentifier();
		ASTIdentifier temp2 = new ASTIdentifier();
		temp1.value = func_name;
		temp2.value = ret_type ;
		
		Quat quat = new Quat(op,temp1 , temp2, null);
		quats.add(quat);
		
		// 要检查符号表的话也在这里检查
		Gloabl_funTable.add(quat);
		
		//TODO map 需要有
		//map.put()...
//		map.put(ASTFunctionDefine, functionDefine.declarator);
		visit(functionDefine.body);
	}
	
	@Override
	public void visit(ASTBinaryExpression binaryExpression) throws Exception {
		String op = binaryExpression.op.value;
		ASTNode res = null;
		ASTNode opnd1 = null;
		ASTNode opnd2 = null;
		if (op.equals("=")) {
			// 赋值操作
			// 获取被赋值的对象res
			visit(binaryExpression.expr1);
			res = map.get(binaryExpression.expr1);
			// 判断源操作数类型, 为了避免出现a = b + c; 生成两个四元式：tmp1 = b + c; a = tmp1;的情况。也可以用别的方法解决
			if (binaryExpression.expr2 instanceof ASTIdentifier) {
				opnd1 = binaryExpression.expr2;
			}else if(binaryExpression.expr2 instanceof ASTIntegerConstant) {
				opnd1 = binaryExpression.expr2;
			}else if(binaryExpression.expr2 instanceof ASTBinaryExpression) {
				ASTBinaryExpression value = (ASTBinaryExpression)binaryExpression.expr2;
				op = value.op.value;
				visit(value.expr1);
				opnd1 = map.get(value.expr1);
				visit(value.expr2);
				opnd2 = map.get(value.expr2);
			}else if(binaryExpression.expr2 instanceof ASTFunctionCall){
				visit(binaryExpression.expr2);
				ASTIdentifier reg = new ASTIdentifier();
				reg.value = "%%retval";
				opnd1 = reg;
			}
		}else if (op.equals("+")) {
			// 加法操作，结果存储到中间变量
			res = new TemporaryValue(++tmpId);
			visit(binaryExpression.expr1);
			opnd1 = map.get(binaryExpression.expr1);
			visit(binaryExpression.expr2);
			opnd2 = map.get(binaryExpression.expr2);
		}else if (op.equals("<")){
			visit(binaryExpression.expr1);
			res = map.get(binaryExpression.expr1);
			visit(binaryExpression.expr2);
			opnd1 = map.get(binaryExpression.expr2);
		}
		
		// build quat
		Quat quat = new Quat(op, res, opnd1, opnd2);
		quats.add(quat);
		map.put(binaryExpression, res);
	}

	@Override
	public void visit(ASTFunctionCall funcCall) throws Exception {
		// TODO Auto-generated method stub
		// 没有考虑函数的参数
		String op = "call";
		Quat quat = new Quat(op,(ASTIdentifier)funcCall.funcname ,null,null);
		quats.add(quat);
//		System.out.println(quat.getRes().toString());
		map.put(funcCall,funcCall);
	}
	
	@Override
	public void visit(ASTIterationStatement iterationStat) throws Exception {
		// TODO Auto-generated method stub
		//在for转换到while之前初始化语句要放到while外面
		visit((ASTBinaryExpression)iterationStat.init.getFirst());
		
		String op = "while";
		Quat quat = new Quat(op,null ,null,null);
		quats.add(quat);
		// 循环判断 默认都是BinaryExpression
		visit((ASTBinaryExpression)iterationStat.cond.getFirst());
		op =  ") {";
		quat = new Quat(op,null ,null,null);
		quats.add(quat);
		
		//处理内部部分
		visit(iterationStat.stat);
		
		//增加一句step的变化
		visit(iterationStat.step.getFirst());
		
		//最后while的结尾
		op =  "}";
		quat = new Quat(op,null ,null,null);
		quats.add(quat);
	}
	
	@Override
	public void visit(ASTPostfixExpression postfixExpression) throws Exception {
		// TODO Auto-generated method stub
		String op = postfixExpression.op.value;
		// 只考虑++的后缀表达式
		if(op.equals("++")) {
			// TODO map
			Quat quat = new Quat(op,(ASTIdentifier)postfixExpression.expr,null,null);
			quats.add(quat);
		}
		
	}
	
	@Override
	public void visit(ASTSelectionStatement selectionStat) throws Exception {
		// TODO Auto-generated method stub
		// 只考虑if部分 else部分暂不考虑
		String op = "if";
		Quat quat = new Quat(op,null ,null,null);
		quats.add(quat);
		visit((ASTBinaryExpression)selectionStat.cond.getFirst());
		op =  ") {";
		quat = new Quat(op,null ,null,null);
		quats.add(quat);
		
		// then部分
		visit(selectionStat.then);
		
		op =  "}";
		quat = new Quat(op,null ,null,null);
		quats.add(quat);
		
	}
	
	@Override
	public void visit(ASTBreakStatement breakStat) throws Exception {
		// TODO Auto-generated method stub
		
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
			else if  (node instanceof ASTIterationStatement) {
				visit((ASTIterationStatement) node);
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
