package bit.minisys.minicc.parser;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.antlr.v4.gui.TreeViewer;

import com.fasterxml.jackson.databind.ObjectMapper;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.parser.ast.*;

/*
 * PROGRAM     --> FUNC_LIST
 * FUNC_LIST   --> FUNC FUNC_LIST | e
 * FUNC        --> TYPE ID '(' ARGUMENTS ')' CODE_BLOCK
 * TYPE        --> INT
 * ARGS   	   --> e | ARG_LIST
 * ARG_LIST    --> ARG ',' ARGLIST | ARG
 * ARG    	   --> TYPE ID
 * CODE_BLOCK  --> '{' STMTS '}'
 * STMTS       --> STMT STMTS | e
 * STMT        --> ASSIGN_STMT | RETURN_STMT | DECL_STMT | EX
 *
 * RETURN STMT --> RETURN EXPR ';' ｜ RETURN ';'
 *
 * EXPR        --> TERM EXPR'
 * EXPR'       --> '+' TERM EXPR' | '-' TERM EXPR' | '<' TERM EXPR' | e
 *
 * TERM        --> FACTOR TERM'
 * TERM'       --> '*' FACTOR TERM' | e
 *
 * FACTOR      --> ID | IntCon
 * 
 * DECL_STMT   --> TYPE InitList ';'
 * InitList    --> Init_expr ',' InitList | Init_expr 
 * Init_expr   --> ID '=' EXPR | ID
 * 
 * fun_ARGS   	   --> e | fun_ARG_LIST
 * func_ARG_LIST    --> EXPR ',' ARGLIST | EXPR
 * 
 * ID          --> arry or va
 */

class ScannerToken{
	public String lexme;
	public String type;
	public int	  line;
	public int    column;
}

public class ExampleParser implements IMiniCCParser {

	private ArrayList<ScannerToken> tknList;
	private int tokenIndex;
	private ScannerToken nextToken;
	
	@Override
	public String run(String iFile) throws Exception {
		System.out.println("Parsing...");

		String oFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_PARSER_OUTPUT_EXT;
		String tFile = MiniCCUtil.removeAllExt(iFile) + MiniCCCfg.MINICC_SCANNER_OUTPUT_EXT;
		
		tknList = loadTokens(tFile);
		tokenIndex = 0;

		ASTNode root = program();
		
		
		String[] dummyStrs = new String[16];
		TreeViewer viewr = new TreeViewer(Arrays.asList(dummyStrs), root);
	    viewr.open();
	    
	    System.out.println();
		ObjectMapper mapper = new ObjectMapper();
		mapper.writeValue(new File(oFile), root);

		//TODO: write to file
		
		
		return oFile;
	}
	

	private ArrayList<ScannerToken> loadTokens(String tFile) {
		tknList = new ArrayList<ScannerToken>();
		
		ArrayList<String> tknStr = MiniCCUtil.readFile(tFile);
		
		for(String str: tknStr) {
			if(str.trim().length() <= 0) {
				continue;
			}
			
			ScannerToken st = new ScannerToken();
			//[@0,0:2='int',<'int'>,1:0]
			String[] segs;
			if(str.indexOf("<','>") > 0) {
				str = str.replace("','", "'DOT'");
				
				segs = str.split(",");
				segs[1] = "=','";
				segs[2] = "<','>";
				
			}else {
				segs = str.split(",");
			}
//			st.lexme = segs[1].substring(segs[1].indexOf("=") + 1);
			st.lexme = segs[1].split("'")[1];
			st.type  = segs[2].substring(segs[2].indexOf("<") + 1, segs[2].length() - 1);
			String[] lc = segs[3].split(":");
			st.line = Integer.parseInt(lc[0]);
			st.column = Integer.parseInt(lc[1].replace("]", ""));
			
			tknList.add(st);
		}
		
		return tknList;
	}

	private ScannerToken getToken(int index){
		if (index < tknList.size()){
			return tknList.get(index);
		}
		return null;
	}

	public void matchToken(String type) {
		if(tokenIndex < tknList.size()) {
			ScannerToken next = tknList.get(tokenIndex);
			if(!next.type.equals(type)) {
				System.out.println("[ERROR]Parser: unmatched token, expected = " + type + ", " 
						+ "input = " + next.type);
			}
			else {
				tokenIndex++;
			}
		}
	}

	//PROGRAM --> FUNC_LIST
	public ASTNode program() {
		ASTCompilationUnit p = new ASTCompilationUnit();
		ArrayList<ASTNode> fl = funcList();
		if(fl != null) {
			//p.getSubNodes().add(fl);
			p.items.addAll(fl);
		}
		p.children.addAll(p.items);
		return p;
	}

	//FUNC_LIST --> FUNC FUNC_LIST | e
	public ArrayList<ASTNode> funcList() {
		ArrayList<ASTNode> fl = new ArrayList<ASTNode>();
		
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("EOF")) {
			return null;
		}
		else {
			ASTNode f = func();
			fl.add(f);
			ArrayList<ASTNode> fl2 = funcList();
			if(fl2 != null) {
				fl.addAll(fl2);
			}
			return fl;
		}
	}

	//FUNC --> TYPE ID '(' ARGUMENTS ')' CODE_BLOCK
	public ASTNode func() {
		ASTFunctionDefine fdef = new ASTFunctionDefine();
		
		ASTToken s = type();
		
		fdef.specifiers.add(s);
		
		fdef.children.add(s);
		
		ASTFunctionDeclarator fdec = new ASTFunctionDeclarator();

		ASTIdentifier id = new ASTIdentifier();
		id.tokenId = tokenIndex;
		nextToken = tknList.get(tokenIndex);
		id.value = nextToken.lexme;
		matchToken("Identifier");
		
		ASTVariableDeclarator vd =  new ASTVariableDeclarator();
		vd.identifier = id;
		fdec.declarator = vd;

		fdef.children.add(id);
		
		matchToken("'('");
		ArrayList<ASTParamsDeclarator> pl = arguments();
		matchToken("')'");
		
		//fdec.identifiers.add(id);
		if(pl != null) {
			fdec.params.addAll(pl);
			fdec.children.addAll(pl);
		}
		else {
			fdec.params = null;
		}
			
		
		
		ASTCompoundStatement cs = codeBlock();
		
		
		
		fdef.declarator = fdec;
		fdef.children.add(fdec);
		fdef.body = cs;
		fdef.children.add(cs);

		
		return fdef;
	}
	
	//DECL_STMT --> TYPE InitList ';'
	public ASTStatement DECL_STMT() {
		// fixme 这里注意到 ASTDeclaration是astnode的拓展，但返回要是一个ASTStatement的拓展
		ASTCompoundStatement ans = new ASTCompoundStatement();
		ASTDeclaration fdec = new ASTDeclaration();
		ASTToken s = type();
		System.out.println(s.value);
		fdec.specifiers.add(s);
		fdec.children.add(s);

		
		fdec.initLists = InitList();

		ans.blockItems.add(fdec);
		
		matchToken("';'");
		return ans;
//		return fdec;
		
	}
	public ASTDeclaration DECL_STMT1() {
		// fixme 这里注意到 ASTDeclaration是astnode的拓展，但返回要是一个ASTStatement的拓展
		ASTDeclaration fdec = new ASTDeclaration();
		ASTToken s = type();
		System.out.println(s.value);
		fdec.specifiers.add(s);
		fdec.children.add(s);
		fdec.initLists = InitList();

//		ans.blockItems.add(fdec);
		
		matchToken("';'");
//		return ans;
		return fdec;
		
	}
	

	//InitList --> Init_expr ',' InitList | Init_expr 
	public ArrayList<ASTInitList> InitList() {
		ArrayList<ASTInitList> il_list = new ArrayList<ASTInitList>();
		ASTInitList il = Init_expr();
		il_list.add(il);
		
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("','")) {
			matchToken("','");
			ArrayList<ASTInitList> il_list2 = InitList();
			il_list.addAll(il_list2);
		}
		return il_list;
	}
	
	
	//InitList --> ID '=' EXPR | ID
	public ASTInitList Init_expr() {
		ASTInitList il = new ASTInitList();
		
		ASTIdentifier id = new ASTIdentifier();
		id.tokenId = tokenIndex;
		nextToken = tknList.get(tokenIndex);
		id.value = nextToken.lexme;
		matchToken("Identifier");
		
		nextToken = tknList.get(tokenIndex);
		
		if(nextToken.type.equals("'['")) {
			matchToken("'['");
			ASTExpression e1 = expr();
			matchToken("']'");
			ASTArrayDeclarator ad = new ASTArrayDeclarator();
			ASTVariableDeclarator vd =  new ASTVariableDeclarator();
			vd.identifier = id;
			ad.declarator = vd;
			ad.expr = e1;
			il.declarator = ad;
		}
		else
		{
			ASTVariableDeclarator vd =  new ASTVariableDeclarator();
			vd.identifier = id;
			il.declarator = vd;
		}
		
		
		if(nextToken.type.equals("'='")) {
			matchToken("'='");
			ASTExpression e = expr();
			List<ASTExpression> temp = new ArrayList<ASTExpression>();
			temp.add(e);
			il.exprs = temp;
		}
		
//		System.out.println(id.value);
		return il;
	}
	
	
	
	//TYPE --> INT |FLOAT | CHART
	public ASTToken type() {
		ScannerToken st = tknList.get(tokenIndex);
		ASTToken t = new ASTToken();
//		System.out.println(st.type);
		if(st.type.equals("'int'")) {
			t.tokenId = tokenIndex;
			
			t.value = st.lexme;
			tokenIndex++;
//			System.out.println("6 step");
		}
		return t;
	}

	//ARGUMENTS --> e | ARG_LIST
	public ArrayList<ASTParamsDeclarator> arguments() {
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("')'")) { //ending
			return null;
		}
		else {
			ArrayList<ASTParamsDeclarator> al = argList();
			return al;
		}
	}

	//ARG_LIST --> ARGUMENT ',' ARGLIST | ARGUMENT
	public ArrayList<ASTParamsDeclarator> argList() {
		ArrayList<ASTParamsDeclarator> pdl = new ArrayList<ASTParamsDeclarator>();
		ASTParamsDeclarator pd = argument();
		pdl.add(pd);
		
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("','")) {
			matchToken("','");
			ArrayList<ASTParamsDeclarator> pdl2 = argList();
			pdl.addAll(pdl2);
		}
		
		return pdl;
	}
		
	//ARGUMENT --> TYPE ID
	public ASTParamsDeclarator argument() {
		ASTParamsDeclarator pd = new ASTParamsDeclarator();
		ASTToken t = type();
		pd.specfiers.add(t);
		
		ASTIdentifier id = new ASTIdentifier();
		id.tokenId = tokenIndex;
		nextToken = tknList.get(tokenIndex);
		id.value = nextToken.lexme;
		matchToken("Identifier");
		
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("'['")) {
			matchToken("'['");
			ASTExpression e1 = expr();
			matchToken("']'");
			ASTArrayDeclarator ad = new ASTArrayDeclarator();
			ASTVariableDeclarator vd =  new ASTVariableDeclarator();
			vd.identifier = id;
			ad.declarator = vd;
			ad.expr = e1;
			pd.declarator = ad;
		}
		else
		{
			ASTVariableDeclarator vd =  new ASTVariableDeclarator();
			vd.identifier = id;
			pd.declarator = vd;
		}
		
//		ASTVariableDeclarator vd =  new ASTVariableDeclarator();
//		vd.identifier = id;
//		pd.declarator = vd;
		
		return pd;
	}

	

	//CODE_BLOCK --> '{' STMTS '}'
	public ASTCompoundStatement codeBlock() {
		matchToken("'{'");
		ASTCompoundStatement cs = stmts();
		matchToken("'}'");

		return cs;
	}

	//STMTS --> STMT STMTS | e
	public ASTCompoundStatement stmts() {
		nextToken = tknList.get(tokenIndex);
//		System.out.print("test " + nextToken.lexme);
		if (nextToken.type.equals("'}'"))
			return null;
		else {
			ASTCompoundStatement cs = new ASTCompoundStatement();
			ASTNode s = stmt();
			cs.blockItems.add(s);
			
			ASTCompoundStatement cs2 = stmts();
			if(cs2 != null)
				for(ASTNode temp :cs2.blockItems) {
					System.out.println("yes");
					cs.blockItems.add(temp);
				}
				
			return cs;
		}
	}

	//STMT --> ASSIGN_STMT | RETURN_STMT | DECL_STMT | FUNC_CALL 选择 循环
	public ASTNode stmt() {
		nextToken = tknList.get(tokenIndex);

		if(nextToken.type.equals("'return'")) {
			return returnStmt();
		}
		else if(nextToken.type.equals("'int'")) {
			// 这里要注意
			return DECL_STMT1();
		}
		else if(nextToken.type.equals("Identifier")) {
			return EXPR_STMT();
		}
		else if(nextToken.type.equals("'if'")){
			return IF_STMT();
		}
		else if(nextToken.type.equals("'for'")){
			return FOR_STMT();
		}
		else{
//			System.out.println("[ERROR]Parser: unreachable stmt!");
			return null;
		}
	}
	
	// FOR_STMT --> 'for' '(' for_arg1 expr ';' for_arg3 ')' {' STMTS '}'
	// for_arg1 --> DECL_STMT | expr ';'
	
	public ASTStatement FOR_STMT() {
		ASTIterationDeclaredStatement for_stmt1 = new ASTIterationDeclaredStatement();
		ASTIterationStatement for_stmt2 = new ASTIterationStatement ();
		matchToken("'for'");
		matchToken("'('");
		int flag = 1;
		nextToken = tknList.get(tokenIndex);
		System.out.println("youkon "+ nextToken.type);
		if(nextToken.type.equals("'int'")){
			ASTDeclaration for_arg1 = DECL_STMT1();
			for_stmt1.init = for_arg1;
			flag = 1;
		}
		else {
			for_stmt2 = new ASTIterationStatement ();
			ASTExpression e = expr();
			LinkedList<ASTExpression> temp1 = new LinkedList<ASTExpression>();
			temp1.add(e);
			for_stmt2.init = temp1;
			matchToken("';'");
			flag = 2;
		}
		
		
		
		ASTExpression e = expr();
		LinkedList<ASTExpression> temp = new LinkedList<ASTExpression>();
		temp.add(e);
		for_stmt1.cond = temp;
		for_stmt2.cond = temp;
		matchToken("';'");
//		ASTExpression e1 = expr();
		
		// 这里应该单独写一个函数，甚至要修改一下expr，但这里为了方便就算了 也只考虑了++
		ASTPostfixExpression pe = new ASTPostfixExpression();
		ASTIdentifier id = new ASTIdentifier();
		id.tokenId = tokenIndex;
		nextToken = tknList.get(tokenIndex);
		id.value = nextToken.lexme;
		matchToken("Identifier");
		pe.expr = id;
		
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("'++'")) {
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			tkn.value = "++";
			// 如果复杂考虑的化在函数里就要用if判别一下，但大同小异
			matchToken("'++'");
			pe.op = tkn;
		}
		
		
		LinkedList<ASTExpression> temp1 = new LinkedList<ASTExpression>();
		temp1.add(pe);
		for_stmt1.step = temp1;
		for_stmt2.step = temp1;
		matchToken("')'");
		
		matchToken("'{'");
		ASTStatement for_stat = stmts();
		for_stmt1.stat = for_stat;
		for_stmt2.stat = for_stat;
		matchToken("'}'");
				
		
		if(flag == 1) {
			System.out.println("flag "+ flag);
			return for_stmt1;
		}
//		else if(flag==2)
		return for_stmt2;
	}
	
	
	
	// IF_STMT --> 'if' '(' EXPR ')' IF_then
	// IF_then --> '{' STMTS '}' | ASSIGN_STMT | RETURN_STMT | FUNC_CALL
	
	public ASTSelectionStatement IF_STMT() {
		ASTSelectionStatement ans = new  ASTSelectionStatement();
		matchToken("'if'");
		matchToken("'('");
		ASTExpression e = expr();
		LinkedList<ASTExpression> temp = new LinkedList<ASTExpression>();
		temp.add(e);
		ans.cond = temp;
		matchToken("')'");
		
		nextToken = tknList.get(tokenIndex);
//		System.out.println("nihao "+nextToken.type);
		if(nextToken.type.equals("'{'")) {
			matchToken("'{'");
			ASTStatement if_then = stmts();
			ans.then =  if_then;
			matchToken("'}'");
			return ans;
		}
		ASTStatement if_then = (ASTStatement) stmt();
		ans.then = if_then;
		ans.otherwise = null;
		return ans;
	}
	
//	public ASTStatement IF_then() {
//		ASTStatement if_then = stmt();
//		
//		return if_then;
//	}
	
	public ASTExpression ID(){
		ASTIdentifier id = new ASTIdentifier();
		id.tokenId = tokenIndex;
		nextToken = tknList.get(tokenIndex);
		id.value = nextToken.lexme;
		matchToken("Identifier");
		
		ASTArrayAccess aa = new ASTArrayAccess();
		// 考虑 a[5]这种可能
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("'['")) {
			matchToken("'['");
			ASTExpression e1 = expr();
			matchToken("']'");
			aa.arrayName = id;
			List<ASTExpression> aa_ele = new ArrayList<ASTExpression>();
			aa_ele.add(e1);
			aa.elements = aa_ele;
			return aa;
		}
		return id;
	}
	
	// 没有考虑前缀表达式
	// 
	public ASTExpressionStatement EXPR_STMT() {
		ASTExpressionStatement ans = new  ASTExpressionStatement();
		
		ASTExpression id = ID();
		
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("'('")) {
			matchToken("'('");
			ASTFunctionCall fc = new ASTFunctionCall();
			fc.argList = func_args();
			matchToken("')'");
			matchToken("';'");
			fc.funcname =id;
			List<ASTExpression> temp = new ArrayList<ASTExpression>();
			temp.add(fc);
			ans.exprs = temp;	
		}
		else if(nextToken.type.equals("'*='")) {
			ASTBinaryExpression be = expr2();
			if(be != null) {
				be.expr1 = id;
				List<ASTExpression> temp = new ArrayList<ASTExpression>();
				temp.add(be);
				ans.exprs = temp;
				matchToken("';'");
			}else {
				System.out.println("error here");
			}
		}
		else if(nextToken.type.equals("'='")) {
			ASTBinaryExpression be = expr2();
			System.out.println(be.op.value); 
			if(be != null) {
				be.expr1 = id;
				List<ASTExpression> temp = new ArrayList<ASTExpression>();
				temp.add(be);
				ans.exprs = temp;
				matchToken("';'");
			}else {
				System.out.println("error here");
			}
		}
		else if(nextToken.type.equals("'++'")) {
			ASTPostfixExpression pe = new ASTPostfixExpression();
			pe.expr = id;
			
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			tkn.value = "++";
			matchToken("'++'");
			
			pe.op = tkn;
			matchToken("';'");	
			List<ASTExpression> temp = new ArrayList<ASTExpression>();
			temp.add(pe);
			ans.exprs = temp;
		}
		
//		ans.exprs.add(expr_stmt);
		return ans;
	}
	
	//fun_ARGS --> e | fun_ARG_LIST
	public List<ASTExpression> func_args() {
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("')'")) { //ending
			return null;
		}
		else {
			List<ASTExpression> fun_args = func_ARG_LIST();
			return fun_args;
		}
		
		
	}
	
	//func_ARG_LIST    --> EXPR ',' ARGLIST | EXPR
	public List<ASTExpression> func_ARG_LIST(){
		List<ASTExpression> fun_args = new ArrayList<ASTExpression>();
		
		ASTExpression pd = expr();
		fun_args.add(pd);
		
		
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("','")) {
			matchToken("','");
			List<ASTExpression>pdl2 = func_ARG_LIST();
			fun_args.addAll(pdl2);
		}
		
		return fun_args;
	}
	
	//RETURN_STMT --> RETURN EXPR ';' ｜ RETURN ';'
	public ASTReturnStatement returnStmt() {
		matchToken("'return'");
		ASTReturnStatement rs = new ASTReturnStatement();
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("';'")) {
			matchToken("';'");
			rs.expr = null;
			return rs;
		}
		ASTExpression e = expr();
		matchToken("';'");
		rs.expr.add(e);
		return rs;
	}

	//EXPR --> TERM EXPR'
	public ASTExpression expr() {
		ASTExpression term = term();
		ASTBinaryExpression be = expr2();
		
		if(be != null) {
			be.expr1 = term;
			return be;
		}else {
			return term;
		}
	}

	//EXPR' --> '+' TERM EXPR' | '-' TERM EXPR' | '<' TERM EXPR' | '*=' TERM EXPR' | '=' TERM EXPR' | e
	public ASTBinaryExpression expr2() {
		nextToken = tknList.get(tokenIndex);
		System.out.println("here " + nextToken.type);
		if (nextToken.type.equals("';'"))
			return null;
		
		if(nextToken.type.equals("'+'")){
			ASTBinaryExpression be = new ASTBinaryExpression();
			
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			tkn.value = "+";
			matchToken("'+'");
			
			be.op = tkn;
//			be.expr2 = term();
//			
//			ASTBinaryExpression expr = expr2();
//			if(expr != null) {
//				expr.expr1 = be;
//				return expr;
//			}
//			System.out.println("there " + be.op.value);
//			return be;
			ASTExpression temp1 = term();
			
			
			ASTBinaryExpression expr = expr2();
			if(expr != null) {
				expr.expr1 = temp1;
				be.expr2 = expr;
//				System.out.println("there2 " + be.op.value);
				return be;
			}
//			System.out.println("there1 " + be.op.value);
			be.expr2 = temp1;
			return be;
		}
		else if(nextToken.type.equals("'-'")){
			ASTBinaryExpression be = new ASTBinaryExpression();
			
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			tkn.value = "-";
			matchToken("'-'");
			
			be.op = tkn;
//			be.expr2 = term();
//			
//			ASTBinaryExpression expr = expr2();
//			if(expr != null) {
//				expr.expr1 = be;
//				return expr;
//			}
//			return be;
			ASTExpression temp1 = term();
			
			ASTBinaryExpression expr = expr2();
			if(expr != null) {
				expr.expr1 = temp1;
				be.expr2 = expr;
//				System.out.println("there2 " + be.op.value);
				return be;
			}
//			System.out.println("there1 " + be.op.value);
			be.expr2 = temp1;
			return be;
		}
		else if(nextToken.type.equals("'*='")){
			ASTBinaryExpression be = new ASTBinaryExpression();
			
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			tkn.value = "*=";
			matchToken("'*='");
			
			be.op = tkn;
			be.expr2 = term();
			
			ASTBinaryExpression expr = expr2();
			if(expr != null) {
				expr.expr1 = be;
				return expr;
			}
			return be;
		}
		else if(nextToken.type.equals("'<'")){
			ASTBinaryExpression be = new ASTBinaryExpression();
			
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			tkn.value = "<";
			matchToken("'<'");
			
			be.op = tkn;
			ASTExpression temp1 = term();
			
			
			ASTBinaryExpression expr = expr2();
			if(expr != null) {
				expr.expr1 = temp1;
				be.expr2 = expr;
//				System.out.println("there2 " + be.op.value);
				return be;
			}
//			System.out.println("there1 " + be.op.value);
			be.expr2 = temp1;
			return be;
		}
		else if(nextToken.type.equals("'>'")){
			ASTBinaryExpression be = new ASTBinaryExpression();
			
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			tkn.value = ">";
			matchToken("'>'");
			
			be.op = tkn;
			ASTExpression temp1 = term();
			
			
			ASTBinaryExpression expr = expr2();
			if(expr != null) {
				expr.expr1 = temp1;
				be.expr2 = expr;
//				System.out.println("there2 " + be.op.value);
				return be;
			}
//			System.out.println("there1 " + be.op.value);
			be.expr2 = temp1;
			return be;
		}
		else if(nextToken.type.equals("'='")){
			ASTBinaryExpression be = new ASTBinaryExpression();
			
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			tkn.value = "=";
			matchToken("'='");
			
			be.op = tkn;
			ASTExpression temp1 = term();
			
			
			ASTBinaryExpression expr = expr2();
			if(expr != null) {
				expr.expr1 = temp1;
				be.expr2 = expr;
//				System.out.println("there2 " + be.op.value);
				return be;
			}
			System.out.println("there1 " + be.op.value);
			be.expr2 = temp1;
			return be;
		}
		else {
			return null;
		}
	}

	//TERM --> FACTOR TERM'
	public ASTExpression term() {
		ASTExpression f = factor();
		ASTBinaryExpression be = term2();
		
		if(be != null) {
			be.expr1 = f;
			return be;
		}else {
//			System.out.println("factor");
			return f;
		}
	}

	//TERM'--> '*' FACTOR TERM' | '/' FACTOR TERM' | e
	public ASTBinaryExpression term2() {
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("'*'")){
			ASTBinaryExpression be = new ASTBinaryExpression();
			
			ASTToken tkn = new ASTToken();
			tkn.tokenId = tokenIndex;
			matchToken("'*'");
			
			be.op = tkn;
			be.expr2 = factor();
			
			ASTBinaryExpression term = term2();
			if(term != null) {
				term.expr1 = be;
				return term;
			}
			return be;
		}else {
			return null;
		}
	}

	//FACTOR --> '(' EXPR ')' | ID | CONST | FUNC_CALL
	public ASTExpression factor() {
		nextToken = tknList.get(tokenIndex);
		if(nextToken.type.equals("Identifier")) {
			ASTIdentifier id = new ASTIdentifier();
			ASTArrayAccess aa = new ASTArrayAccess();
			id.tokenId = tokenIndex;
			id.value = nextToken.lexme;
			matchToken("Identifier");
			nextToken = tknList.get(tokenIndex);
			if(nextToken.type.equals("'('")){
				matchToken("'('");
				ASTFunctionCall fc = new ASTFunctionCall();
				fc.argList = func_args();
				matchToken("')'");
				fc.funcname = id;
				return fc;
			}
			
			// 考虑 a[5]这种可能
			
			else if(nextToken.type.equals("'['")) {
				matchToken("'['");
				ASTExpression e1 = expr();
				matchToken("']'");
				aa.arrayName = id;
				List<ASTExpression> aa_ele = new ArrayList<ASTExpression>();
				aa_ele.add(e1);
				aa.elements = aa_ele;
				return aa;
			}
//			System.out.println("fac "+ id.value);
			return id;		
		}
		else if(nextToken.type.equals("IntegerConstant")) {
			ASTIntegerConstant intcon = new ASTIntegerConstant();
			intcon.tokenId = tokenIndex;
			intcon.value = Integer.parseInt(nextToken.lexme);
			matchToken("IntegerConstant");
			System.out.println("fac "+ intcon.value);
			return intcon;	
		}
		else if(nextToken.type.equals("StringLiteral")) {
			ASTStringConstant intcon = new ASTStringConstant();
			intcon.tokenId = tokenIndex;
			intcon.value = nextToken.lexme;
			matchToken("StringLiteral");
			return intcon;	
		}
		else {
			return null;
		}
	}
	
	
}
