package bit.minisys.minicc.ncgen;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

import com.fasterxml.jackson.databind.ObjectMapper;

import bit.minisys.minicc.MiniCCCfg;
import bit.minisys.minicc.icgen.internal.IRBuilder;
import bit.minisys.minicc.icgen.internal.MiniCCICGen;
import bit.minisys.minicc.internal.util.MiniCCUtil;
import bit.minisys.minicc.ncgen.IMiniCCCodeGen;
import bit.minisys.minicc.parser.ast.ASTCompilationUnit;


public class ExampleCodeGen implements IMiniCCCodeGen{


	public ExampleCodeGen() {
		
	}
	
	@Override
	public String run(String iFile, MiniCCCfg cfg) throws Exception {
		String oFile = MiniCCUtil.remove2Ext(iFile) + MiniCCCfg.MINICC_CODEGEN_OUTPUT_EXT;
		String tFile = MiniCCUtil.remove2Ext(iFile) + MiniCCCfg. MINICC_PARSER_OUTPUT_EXT;
		
		String ass_code="";

		if(cfg.target.equals("mips")) {
			//TODO:
		}else if (cfg.target.equals("riscv")) {
			//TODO:
		}else if (cfg.target.equals("x86")){
			//TODO:
			// 前头应用部分
			String data_seg = "\n.data\n__retval\tdword\t?\n";
			String code_seg = "\n";
			
			ass_code = ".386\n" + 
					".model flat, stdcall\n" + 
					"option casemap:none\n" + 
					"\n" + 
					"includelib msvcrt.lib\n" + 
					"includelib user32.lib\n" + 
					"includelib kernel32.lib\n";
			ass_code = func_c(ass_code);
			data_seg = data_seg_init(data_seg);
			code_seg = code_seg_init(code_seg);
			code_seg = asm_Mars_PrintInt(code_seg);
			code_seg = asm_Mars_GetInt(code_seg);
			code_seg = asm_Mars_PrintStr(code_seg);
			
			// 通过ast生成asm_code
			ObjectMapper mapper = new ObjectMapper();
			ASTCompilationUnit program = (ASTCompilationUnit)mapper.readValue(new File(tFile), ASTCompilationUnit.class);
			asmbuilder asmgen = new asmbuilder(ass_code,data_seg,code_seg);
			program.accept(asmgen);
			
			ass_code = asmgen.get_ass_code();
			
//			ass_code = (ass_code+data_seg+code_seg);
		}
		
		
		MiniCCUtil.createAndWriteFile(oFile, ass_code);
		System.out.println("7. Target code generation finished!");
		
		return oFile;
	}
	
	public String func_c(String asm_code) {
//		生成汇编中调用c的函数的部分
		
		String res = "\n";
		
		String printf_byte = "printf PROTO C : ptr sbyte, :VARARG\n";
		String scanf_byte = "scanf PROTO C : ptr sbyte, :VARARG\n";
		
		res += printf_byte;
		res += scanf_byte;
		return asm_code+ res;
	}
	public String code_seg_init(String asm_code) {
		// 乘胜代码段的开始部分 init和调用main
		String init = ".code\n" + 
				"__init:\n" + 
				"	call main\n" + 
				"	ret\n";
		return asm_code +init;
	}
	public String data_seg_init(String data_seg){
		// 预置的mars函数经常需要的一些有关输入输出的东西
		String data_init = "forIntNumber	db '%d',0\n" + 
				"forString	db '%s',0\n" + 
				"forEnter	db ' ',0\n" + 
				"IntNumberHolder dd 0\n";
		return data_seg + data_init;
	}
	public String asm_Mars_PrintInt(String asm_code) {
		// 预置函数都采用直接拼接的方式,值得注意的是，这里只是添加了函数部分，不是函数调用部分！
		// 函数调用部分需要先判断是否是第一次调用，第一次调用就要拼接，第二次就不用拼接，
		// 这个只针对预置函数，自定义的函数会在中间代码生成
		String asm_Mars_PrintInt = "Mars_PrintInt:\n"+
				"\tmov esi, [esp+4]\n" + 
				"	pushad\n" + 
				"	invoke printf, offset forIntNumber, esi\n" + 
				"	invoke printf, offset forEnter\n" + 
				"	popad\n" + 
				"	ret\n";
		return asm_code+asm_Mars_PrintInt;
	}
	public String asm_Mars_GetInt(String asm_code) {
		// 预置函数都采用直接拼接的方式
		String asm_Mars_GetInt = "Mars_GetInt:\n" + 
				"	pushad\n" + 
				"	invoke scanf, offset forIntNumber, offset IntNumberHolder\n" + 
				"	popad\n" + 
				"	lea eax, IntNumberHolder\n" + 
				"	mov eax, [eax]\n"+
				"	mov __retval, eax\n" + 	// 返回值送到特殊寄存器__retval中
				"	ret\n";
		return asm_code+asm_Mars_GetInt;
	}
	public String asm_Mars_PrintStr(String asm_code) {
		// 预置函数都采用直接拼接的方式
		String asm_Mars_PrintStr = "Mars_PrintStr:\n" + 
				"	mov esi, [esp+4]\n" + 
				"	pushad\n" + 
				"	invoke printf, offset forString, esi\n" + 
				"	popad\n" + 
				"	ret\n";
		return asm_code+asm_Mars_PrintStr;
	}
	
}
//bit.minisys.minicc.ncgen.ExampleCodeGen