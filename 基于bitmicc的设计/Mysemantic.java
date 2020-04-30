package bit.minisys.minicc.semantic;

import java.io.File;
import java.util.Iterator;
import java.util.Stack;
import java.util.Vector;

import com.fasterxml.jackson.databind.ObjectMapper;
import bit.minisys.minicc.parser.ast.*;

public class Mysemantic implements IMiniCCSemantic {
	// TODO 差错处理
	// 当前是否在for循环中
	boolean current_for = false;
	// 当前scope是否存在goto
	boolean current_goto = false;
	Stack<Vector> fun_table =new Stack<Vector>();
	Stack<String> error_table = new Stack<String>();
	@Override
	public String run(String iFile) throws Exception{
//		System.out.println(iFile);
		ObjectMapper mapper = new ObjectMapper();
		ASTCompilationUnit program = (ASTCompilationUnit)mapper.readValue(new File(iFile), ASTCompilationUnit.class);
		
//		ASTCompilationUnit declaration = (ASTCompilationUnit)mapper.readValue(new File(iFile), ASTDeclaration.class);
		error_table.add("-----------");
		visit(program);
		
		for(String item : error_table) {
			System.out.println(item);
		}
		return null;
	}
	
	void visit(ASTCompilationUnit program) throws Exception{
		// 维护唯一的符号表
		// 1. 名称 2.类型 3.值 / 参数 4.声明/定义
		Stack<Vector> scope_stack_total = new Stack<Vector>();
		for (ASTNode item:program.items) {
//			System.out.println(item.getClass().getSimpleName());
			String type_name = item.getClass().getSimpleName();
			if(type_name.equals( "ASTFunctionDefine") ){
//				System.out.println("here");
				ASTFunctionDefine item1 = (ASTFunctionDefine) item;
				// 检查是否有重复实现
				check_repeat(item1.declarator.getName(),scope_stack_total);
				Vector<String> temp = new Vector<String>(2);
				temp.add(item1.declarator.getName());
				temp.add(null);
				scope_stack_total.add(temp);
				Vector<String> temp1 = new Vector<String>(2);
				temp1.add(item1.declarator.getName());
				temp1.add("declear");
				scope_stack_total.add(temp1);
				// 处理参数
				ASTFunctionDeclarator item2 = (ASTFunctionDeclarator) item1.declarator;
				Vector<String> tem = new Vector<String>();
				tem.add(item1.declarator.getName());
				tem.add(item1.specifiers.get(0).value);
				for(ASTParamsDeclarator item3:item2.params) {
//					System.out.println("here "+ item3.specfiers.get(0).value);
					tem.add(item3.specfiers.get(0).value);
				}
				fun_table.add(tem);
//				System.out.println("func "+ fun_table.peek());
				visit(item1,scope_stack_total);
				if(!item1.body.blockItems.get(item1.body.blockItems.size()-1).getClass().getSimpleName().equals("ASTReturnStatement")) {
					error_table.add("ES08  " + item1.declarator.getName() +"函数不以return为结尾");
				}
			}
			else if(type_name.equals( "ASTDeclaration")) {
				ASTDeclaration item1 = (ASTDeclaration) item;
				for(ASTInitList item2:item1.initLists) {
					Vector<String> temp = new Vector<String>(2);
					temp.add(item2.declarator.getName());
					temp.add("declear");
					scope_stack_total.add(temp);
					// expr不匹配
						
				}
			}
//			System.out.println("  test");
		}
	}
	
	Vector<String> arraydec(Vector<String> tem,ASTDeclarator declarator){
		if(declarator.getClass().getSimpleName().equals("ASTArrayDeclarator")) {
			ASTArrayDeclarator item = (ASTArrayDeclarator) declarator;
			ASTIntegerConstant id = (ASTIntegerConstant) item.expr;
//			System.out.println("here "+id.value.toString());
			tem.add(id.value.toString());
			if(item.declarator.getClass().getSimpleName().equals("ASTArrayDeclarator")) {
				tem = arraydec(tem,item.declarator);
			}
		}
		else if(declarator.getClass().getSimpleName().equals("ASTVariableDeclarator")) {
//			System.out.println("aahere");
		}
//		System.out.println("aahere "+ declarator.getClass().getSimpleName());
		return tem;
	}
	
	boolean array_check(ASTArrayAccess declarator,Stack<Vector> current_scope) {
		boolean ans = false;
		Vector<String> tem = new Vector<String>();
		//先读到当前这个变量和范围 [0,3,a]
		tem = array_check_find(tem,declarator);
		
		String name = tem.get(tem.size()-1);
		Vector<String> tem1 = null;
		// TODO 检查是否合法
		for(Vector temp:current_scope) {
			if(temp.firstElement().getClass().getSimpleName().equals("String")&& temp.firstElement().equals(name)) {
				tem1 = temp;
				break;
			}
		}
//		System.out.println(tem1);
		if((tem1.size()-1 < tem.size())) {
			error_table.add("ES09 "+ name +"数组维度有误");
		}
		for(int i=2;i<tem1.size();++i) {
//			System.out.println(Integer.parseInt(tem1.get(i)));
			if(Integer.parseInt(tem.get(i-2)) >Integer.parseInt(tem1.get(i))-1 ) {
				error_table.add("ES06 "+ name +"数组的第"+(tem1.size()-1-i)+"维度访问越界");
			}
		}
		
		return ans;
	}
	
	Vector<String> array_check_find(Vector<String> tem,ASTExpression declarator) {
//		System.out.println("test "+ declarator.getClass().getSimpleName());
		if(declarator.getClass().getSimpleName().equals("ASTArrayAccess")) {
			ASTArrayAccess item = (ASTArrayAccess) declarator;
//			System.out.println("test "+ declarator.getClass().getSimpleName().equals("ASTArrayAccess"));
			ASTIntegerConstant id = (ASTIntegerConstant) item.elements.get(0);
//			System.out.println("here "+id.value.toString());
			tem.add(id.value.toString());
			if(item.arrayName.getClass().getSimpleName().equals("ASTArrayAccess")) {
//				System.out.println("test2 "+ item.arrayName.getClass().getSimpleName());
				tem = array_check_find(tem,item.arrayName);
			}
			else if(item.arrayName.getClass().getSimpleName().equals("ASTIdentifier")) {
				ASTIdentifier item1 = (ASTIdentifier) item.arrayName;
//				System.out.println("aahere");
				tem.add(item1.value);
			}
		}
//		System.out.println("test1 ");
		return tem;
	}
	
	void visit(ASTCompoundStatement temp,Stack<Vector> parent_scope)throws Exception{
		Stack<Vector> scope_stack_local = new Stack<Vector>();
		Stack<Vector> scope_stack_goto = new Stack<Vector>();
		
		for(ASTNode item:temp.blockItems) {
			String type_name = item.getClass().getSimpleName();
//			System.out.println(type_name);
			if(type_name.equals( "ASTDeclaration") ){
				ASTDeclaration item1 = (ASTDeclaration)item;
				// 拿到声明变量的type，目前在表中不需要
//				System.out.println("ok " + item1.specifiers.get(0).value);
				for(ASTInitList item2:item1.initLists) {
//					String type_name1 = item2.declarator.getClass().getSimpleName();
					Vector<String> tem = new Vector<String>();
					tem.add(item2.declarator.getName());
//					tem.add(null);
					tem.add(item1.specifiers.get(0).value);
					//是数组的声明，有多个维度
					if(item2.declarator.getClass().getSimpleName().equals("ASTArrayDeclarator")) {
						tem = arraydec(tem,item2.declarator);
					}
//					System.out.println("there "+ tem);
					// TODO 先检查后压入
					check_repeat(item2.declarator.getName(),scope_stack_local);
					
					// 检查没有的 检查重复的
					scope_stack_local.add(parent_scope);
					if(item2.exprs!=null){
						for(ASTExpression item3:item2.exprs) {
							count(item3,scope_stack_local);
						}
					}
					else {
						System.out.println("没有初始化");
					}
//					check("a",scope_stack_local);
					scope_stack_local.pop();
					scope_stack_local.add(tem);
				}
			}
			else if(type_name.equals("ASTExpressionStatement")){
				scope_stack_local.add(parent_scope);
				visit((ASTExpressionStatement)item,scope_stack_local);
				scope_stack_local.pop();
			}
			else if(type_name.equals("ASTCompoundStatement")) {
				scope_stack_local.add(parent_scope);
				visit((ASTCompoundStatement)item,scope_stack_local);
				scope_stack_local.pop();
			}
			else if(type_name.equals("ASTBreakStatement")) {
				if(!current_for) {
					//只检测了for循环，其他同理
//					System.out.println("这个break没有在循环当中");
					error_table.add("ES03  "+ "break没有在循环当中");
				}
			}
			else if(type_name.equals("ASTIterationStatement")) {
				current_for = true;
				scope_stack_local.add(parent_scope);
				visit((ASTIterationStatement)item,scope_stack_local);
				scope_stack_local.pop();
				current_for = false;
			}
			else if(type_name.equals("ASTReturnStatement")) {
				
			}
			else if(type_name.equals("ASTLabeledStatement")) {
				current_goto = true;
				ASTLabeledStatement item1 = (ASTLabeledStatement) item;
				check_repeat(item1.label.value,scope_stack_local);
				Vector<String> tem = new Vector<String>(2);
				tem.add(item1.label.value);
				tem.add(null);
				scope_stack_local.add(tem);
//				System.out.println(item1.label.value);
				//将待检查的押入goto栈
				ASTGotoStatement item2 = (ASTGotoStatement) item1.stat;
				Vector<String> tem1 = new Vector<String>(2);
				tem1.add(item2.label.value);
				tem1.add(null);
				scope_stack_goto.add(tem1);
			}
			else if(type_name.equals("ASTGotoStatement")) {
				ASTGotoStatement item1 = (ASTGotoStatement) item;
				Vector<String> tem = new Vector<String>(2);
				tem.add(item1.label.value);
				tem.add(null);
				scope_stack_goto.add(tem);
			}
		}
		if(current_goto) {
			// goto 要查看当前整个scope的符号表
			while(!scope_stack_goto.isEmpty()) {
				Vector<String> tem = scope_stack_goto.pop();
				int a = scope_stack_local.search(tem);
				if(a==-1) {
//					System.out.println(tem.firstElement()+" 不存在");
					error_table.add("ES07  "+tem.firstElement()+" 不存在");
				}
			}
		}
		current_goto = false;
	}
	void visit(ASTExpressionStatement exprstat,Stack<Vector> current_scope)throws Exception{
		// 表达式中不会有声明的
		if(exprstat.exprs!=null){
			for(ASTExpression item:exprstat.exprs) {
				count(item,current_scope);
			}
		}
		
	}
	void visit(ASTIterationStatement functionfor,Stack<Vector> parent_scope)throws Exception{
		Stack<Vector> scope_stack_local = new Stack<Vector>();
//		visit(functionDefine.body,parent_scope);
	}
	void visit(ASTFunctionDefine functionDefine,Stack<Vector> parent_scope)throws Exception{
		Stack<Vector> scope_stack_local = new Stack<Vector>();
		
		//处理参数
		ASTFunctionDeclarator item2 = (ASTFunctionDeclarator) functionDefine.declarator;
		for(ASTParamsDeclarator item3:item2.params) {
//			System.out.println("there "+ item3.declarator.getName());
			Vector<String> tem = new Vector<String>(2);
			tem.add(item3.declarator.getName());
			tem.add(null);
			scope_stack_local.add(tem);
		}
		scope_stack_local.add(parent_scope);
		visit(functionDefine.body,scope_stack_local);
//		System.out.println("teststs "+functionDefine.body.blockItems.get(functionDefine.body.blockItems.size()-1).getClass().getSimpleName());
//		if(!functionDefine.body.blockItems.get(functionDefine.body.blockItems.size()-1).getClass().getSimpleName().equals("ASTReturnStatement")) {
//			error_table.add("ES08 " + "函数不以return为结尾");
//		}
	}
	
	void count(ASTExpression item,Stack<Vector> current_scope) {
		// 查找当前表达式中所有出现的字母；
//		System.out.println(item.getClass().getSimpleName());
		if(item.getClass().getSimpleName().equals("ASTBinaryExpression")) {
			ASTBinaryExpression item1 = (ASTBinaryExpression) item;
			ASTIdentifier item2 = null;
			ASTIdentifier item3 = null;
//			System.out.println(item1.expr1.getClass().getSimpleName());
			String current_op = item1.op.value;
			
			//检查expr1是否定义
			if(item1.expr1.getClass().getSimpleName().equals( "ASTIdentifier")) {
				item2 = (ASTIdentifier) item1.expr1;
//				System.out.println("-----");
//				System.out.println(check(item2.value,current_scope));
				if (!check(item2.value,current_scope)) {
					error_table.add("ES01  "+"变量/函数 " + item2.value + " 使用前没有定义");
				}
				else {
					//检查符号（testcase4）
					if(current_op.equals("<<") || current_op.equals(">>")) {
						if(!check_op(item2.value,current_scope,"int")) {
							error_table.add("ES05  "+"运算符 " + current_op + " 的第一个参数应为int类型");
						}
					}
				}
//				System.out.println("here " +item2.value);
			}
			else if(item1.expr2.getClass().getSimpleName().equals("ASTFunctionCall")) {
				count(item1.expr2,current_scope);
				// 检查函数的返回类型
//				check_op(item1.expr2.,current_scope);
			}
			//检查expr2是否有问题
			if(item1.expr2.getClass().getSimpleName().equals("ASTIdentifier")) {
				item3 = (ASTIdentifier) item1.expr2;
//				check(item3.value,current_scope);
				if (!check(item3.value,current_scope)) {
					error_table.add("ES01  "+"变量/函数 " + item3.value + " 使用前没有定义");
				}
				else {
					//检查符号（testcase4）
					if(current_op.equals("<<") || current_op.equals(">>")) {
						if(!check_op(item3.value,current_scope,"int")) {
							error_table.add("ES05  "+"运算符 " + current_op + " 的第二个参数应为int类型");
						}
					}
				}
//				System.out.println(item2.value+" "+item3.value);
			}
			else if(item1.expr2.getClass().getSimpleName().equals("ASTFunctionCall")) {
				count(item1.expr2,current_scope);
				// TODO 检查函数返回类型
			}
		}
		else if(item.getClass().getSimpleName().equals("ASTFunctionCall")) {
			ASTFunctionCall item1 = (ASTFunctionCall) item;
			ASTIdentifier item2 = (ASTIdentifier) item1.funcname;
			// 检查是否存在f
//			System.out.println(check(item2.value,current_scope));
			if (!check(item2.value,current_scope)) {
				error_table.add("ES01  "+"函数 " + item2.value + " 使用前没有定义");
			}
			// 检查参数是否合法
			Vector<String> fun = new Vector<String>();
			fun.add(item2.value);
			//这里是不知道返回类型的
			fun.add(null);
			for(ASTExpression item3:item1.argList) {
				if(item3.getClass().getSimpleName().equals("ASTIntegerConstant")) {
//					System.out.println("okfine "+ item3.getClass().getSimpleName());
					fun.add("int");
				}
				else if(item3.getClass().getSimpleName().equals("ASTFloatConstant")) {
//					System.out.println("okfine "+ item3.getClass().getSimpleName());
					fun.add("float");
				}
			}
//			System.out.println("checkfun "+ );
			check_fun(fun);
//			System.out.println("func "+ item2.value);
		}
		else if(item.getClass().getSimpleName().equals("ASTArrayAccess")) {
			// TODO
			array_check((ASTArrayAccess)item,current_scope);
		}
	}
	
	boolean check_op(String name,Stack<Vector> current_scope,String type_name) {
		// 接受一个变量 / 函数以及对应的变量（局部） / 函数表（全局）以及应该的类型，检查返回类型时候合适。
		boolean ans = false;
		for(Vector temp:current_scope) {
			if(temp.firstElement().getClass().getSimpleName().equals("String")&& temp.firstElement().equals(name)) {
				if(type_name.equals(temp.get(1))) {
					ans = true;
				}
				break;
			}
		}
		return ans;
	}
	
	boolean check_fun(Vector<String> fun) {
		// 检查参数的数量是否合法，类型是否合法
		// 没有检查返回类型
		boolean ans = false;
		for(Vector<String> item : fun_table) {
			if(fun.firstElement().equals(item.firstElement())) {
				boolean flag_temp = false;
				
				if(fun.size() == item.size()) {
					for(int i = 2;i<fun.size();++i) {
						String temp1 = fun.get(i);
						String temp2 = item.get(i);
						if(!temp1.equals(temp2)) {
							ans = false;
//							System.out.println(fun.firstElement() +"函数的 "+ (i-2) +" 个参数类型不符合");
							error_table.add("ES04  "+ fun.firstElement() +"函数的 "+ (i-2) +" 个参数类型不符合");
							break;
						}
						ans = true;
					}
				}
				else
				{
//					System.out.println(fun.firstElement() + " 参数个数不符合");
					error_table.add("ES04  函数"+ fun.firstElement() + " 参数个数不符合");
				}
			}
		}
		
		return ans;
	}
	
	boolean check(String var,Stack<Vector> current_scope) {
		// 检查是否有miss，没有检查是否有重复
		// 检查重复不用递归
		boolean ans = false;
		//不用组装成Vector直接检查
//		Vector<String> temp = new Vector<String>(2);
//		temp.add(var);
//		temp.add(null);
//		int a = current_scope.search(temp);
		int a = -1;
		for(Vector temp:current_scope) {
			if(temp.firstElement().getClass().getSimpleName().equals("String")&& temp.firstElement().equals(var)) {
				a = 1;
				break;
			}
		}
		if(a>-1) {
//			System.out.println("check "+ a);
			ans = true;
		}
		// 上父亲的节点查找，是一个递归的过程
//		&& current_scope.peek().size()>1

//		System.out.println("1 "+current_scope.peek());
		if(a==-1  && (current_scope.peek().firstElement().getClass().getSimpleName().equals("Stack")||current_scope.peek().firstElement().getClass().getSimpleName().equals("Vector"))) {
//			System.out.println(current_scope.peek().firstElement().getClass().getSimpleName());
//			System.out.println("1 "+current_scope.peek());
			Stack<Vector> peek = (Stack<Vector>) current_scope.peek();
			ans = check(var,peek);
//			System.out.println("check1 "+ ans);
		}
//		System.out.println(current_scope.firstElement());
		return ans;
	}
	
	boolean check_repeat(String var,Stack<Vector> current_scope) {
		//true是不重复，没有迭代，只用检查当前的作用域即可
		boolean ans = true;
//		Vector<String> temp = new Vector<String>(2);
//		temp.add(var);
//		temp.add(null);
//		int a = current_scope.search(temp);
		
		int a = -1;
		for(Vector temp:current_scope) {
			if(temp.firstElement().getClass().getSimpleName().equals("String")&& temp.firstElement().equals(var)) {
				a = 1;
//				System.out.println(temp.get(1));
				if(temp.get(1)!=null && temp.get(1).equals("declear")) {
					a = -1;
					continue;
				}
				break;
			}
		}
		
		if(a>-1) {
//			System.out.println(var +" 重复！ ");
			error_table.add("ES02  "+var +" 重复！");
//			System.out.println();
			return false;
		}
//		System.out.println("当前符号： " + var);
//		if(current_scope.size()>0)
//			System.out.println(current_scope.firstElement()  );
		return ans;
	}
	
}
