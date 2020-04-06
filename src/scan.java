import java.io.*;
import java.util.HashSet;

enum DFA_STATE{
    DFA_STATE_INITIAL,
    DFA_STATE_identify,
    DFA_STATE_constant,
    DFA_STATE_constant_start,

    DFA_STATE_single_c,
    DFA_STATE_double_c,
    DFA_STATE_ADD_0,
    DFA_STATE_sub_0,
    DFA_STATE_c1_0,// &
    DFA_STATE_c2_0,// !
    DFA_STATE_c3_0,// /
    DFA_STATE_c4_0,// %
    DFA_STATE_c4_1,// %
    DFA_STATE_c4_2,// %:%:
    DFA_STATE_c5_0,// < /
    DFA_STATE_c5_1,// < /
    DFA_STATE_c6_0,// > /
    DFA_STATE_c6_1,// > /
    DFA_STATE_c7_0,// ^
    DFA_STATE_c8_0,// =
    DFA_STATE_c9_0,// *
    DFA_STATE_c10_0,// |
    DFA_STATE_c12_0,// :
    DFA_STATE_c11_0,// . /
    DFA_STATE_c11_1, // ...
    DFA_STATE_c13_0,// #
    DFA_STATE_string,
    DFA_STATE_s1_start, // 只包含双引号

    DFA_STATE_UNKNW
}

public class scan {
    // done 读取文件内容
    // done DFA
    // done 运算符和界限符
    // done 字面量
    // done 常量
    // done 标识符
    // done 关键字

    // done -2
    // done 判断行列号

    // notice ++之间是不能有空格的



    private int lIndex = 1; //当前行号
    private int cIndex = -1; //当前列号
    private int word_cnt = 0;
    private int total_index = -1;
    private HashSet<String> keywordSet;

    public void setKeywordSet(){
        this.keywordSet = new HashSet<String>();
        this.keywordSet.add("auto");
        this.keywordSet.add("break");
        this.keywordSet.add("case");
        this.keywordSet.add("char");
        this.keywordSet.add("const");
        this.keywordSet.add("continue");
        this.keywordSet.add("default");
        this.keywordSet.add("do");
        this.keywordSet.add("double");
        this.keywordSet.add("else");
        this.keywordSet.add("enum");
        this.keywordSet.add("extern");
        this.keywordSet.add("float");
        this.keywordSet.add("for");
        this.keywordSet.add("goto");
        this.keywordSet.add("if");
        this.keywordSet.add("inline");
        this.keywordSet.add("int");
        this.keywordSet.add("long");
        this.keywordSet.add("register");
        this.keywordSet.add("restrict");
        this.keywordSet.add("return");
        this.keywordSet.add("short");
        this.keywordSet.add("signed");
        this.keywordSet.add("sizeof");
        this.keywordSet.add("static");
        this.keywordSet.add("struct");
        this.keywordSet.add("switch");
        this.keywordSet.add("typedef");
        this.keywordSet.add("union");
        this.keywordSet.add("unsigned");
        this.keywordSet.add("void");
        this.keywordSet.add("volatile");
        this.keywordSet.add("while");
    }

    private boolean isAlpha(char c) {
        return Character.isAlphabetic(c)||c=='_';
    }

    private boolean isDigit(char c) {
        return Character.isDigit(c);
    }

    private boolean isAlphaOrDigit(char c) {
        return Character.isLetterOrDigit(c)||c=='_';
    }
    private boolean isAlphaOrDigit2(char c) {
        return Character.isLetterOrDigit(c)||c=='.';
    }

    private String gotToken(String local,String curtype,int temp,int total,int line){
        String ans = null;
        ans = ("[@"+String.valueOf(word_cnt)+",");
        word_cnt ++;
        int cnt = total-local.length()+3;
        ans += (String.valueOf(cnt)+":"+String.valueOf(total));
        ans += ("="+local+",");
        ans += "<";
        ans += curtype;
        ans += ">,";
        ans += String.valueOf(line);
        ans += ":";
        ans += String.valueOf(temp);
        ans += "]\n";
        return ans;
    }
    private String gotToken(String local,String curtype,int temp,int total){
        String ans = null;
        ans = ("[@"+String.valueOf(word_cnt)+",");
        word_cnt ++;
        int cnt = total-local.length()+3;
        ans += (String.valueOf(cnt)+":"+String.valueOf(total));
        ans += ("="+local+",");
        ans += "<";
        ans += curtype;
        ans += ">,";
        ans += String.valueOf(lIndex);
        ans += ":";
        ans += String.valueOf(temp);
        ans += "]\n";
        return ans;
    }

    public void readfile(String filename) throws Exception {
        String fileName = filename;

        // 在同一文件夹下输出
        String [] temp_str = filename.split("\\.");
        String fileName1 = "";
        for (int i = 0; i < temp_str.length - 1; i++) {
            fileName1 = fileName1.concat(temp_str[i]);

        }
        fileName1 = fileName1.concat(".token");

        File file = new File(fileName);
        Reader char_read=new FileReader(file);
        FileReader reader = new FileReader(file);
        char[] deposit = new char[1];
        int count = 0;
        String lexme 	= "";		//token lexme
        char c 			= ' ';		//next char
        boolean keep 	= false;	//keep current char
        boolean end 	= false;
        String strTokens = "";
        int iTknNum = 0;
        DFA_STATE state = DFA_STATE.DFA_STATE_INITIAL;
        setKeywordSet();
        int temp = 0;
        System.out.println("Scanning...");
//        System.out.println(this.keywordSet);
        int line =0;
        while(!end ){
            if(!keep){
                if(reader.read(deposit)!=-1){
                    c = deposit[0];
                    cIndex ++;
                    total_index++;
                }
                else{
                    // notice 输入结束 加上一个eof 起始也可以不加
                    end = true;
                    break;
                }
                if(deposit[0]=='\n'  ){
                    lIndex ++;
                    cIndex = -1;
                    total_index+=1;
//                    continue;
                }
            }
            keep = false;

            // 正式求解
            // 按照字符输入

            switch (state){
                case DFA_STATE_INITIAL:
                    temp = cIndex;
                    lexme = "";
                    if(isAlpha(c)) {
                        state = DFA_STATE.DFA_STATE_identify;
                        lexme = lexme + c;
                    }
                    // 常量
                    else if(isDigit(c)) {
                        state = DFA_STATE.DFA_STATE_constant;
                        lexme = lexme + c;
                    }
                    else if(c =='\'') {
                        state = DFA_STATE.DFA_STATE_constant_start;
                        lexme = lexme + c;
                    }


                    else if(c == ' '){
                    }
                    else if(c == '\n'){
                    }
                    else if(c == '\t'){
                    }

                    //DFA_STATE_string
                    else if(c == '"'){
                        state = DFA_STATE.DFA_STATE_s1_start;
                        lexme = lexme + c;
                    }

                    //DFA_STATE_double_c
                    else if(c == '+'){
                        state = DFA_STATE.DFA_STATE_ADD_0;
                        lexme = lexme + c;
                    }
                    else if(c == '-'){
                        state = DFA_STATE.DFA_STATE_sub_0;
                        lexme = lexme + c;
                    }
                    else if(c == '&'){
                        state = DFA_STATE.DFA_STATE_c1_0;
                        lexme = lexme + c;
                    }
                    else if(c == '!'){
                        state = DFA_STATE.DFA_STATE_c2_0;
                        lexme = lexme + c;
                    }
                    else if(c == '/'){
                        state = DFA_STATE.DFA_STATE_c3_0;
                        lexme = lexme + c;
                    }
                    else if(c == '%'){
                        state = DFA_STATE.DFA_STATE_c4_0;
                        lexme = lexme + c;
                    }
                    else if(c == '^'){
                        state = DFA_STATE.DFA_STATE_c7_0;
                        lexme = lexme + c;
                    }
                    else if(c == '='){
                        state = DFA_STATE.DFA_STATE_c8_0;
                        lexme = lexme + c;
                    }
                    else if(c == '*'){
                        state = DFA_STATE.DFA_STATE_c9_0;
                        lexme = lexme + c;
                    }
                    else if(c == '#'){
                        state = DFA_STATE.DFA_STATE_c13_0;
                        lexme = lexme + c;
                    }
                    else if(c == '|'){
                        state = DFA_STATE.DFA_STATE_c10_0;
                        lexme = lexme + c;
                    }
                    else if(c == ':'){
                        state = DFA_STATE.DFA_STATE_c12_0;
                        lexme = lexme + c;
                    }
                    else if(c == '.'){
                        state = DFA_STATE.DFA_STATE_c11_0;
                        lexme = lexme + c;
                    }
                    else if(c == '<'){
                        state = DFA_STATE.DFA_STATE_c5_0;
                        lexme = lexme + c;
                    }else if(c == '>') {
                        state = DFA_STATE.DFA_STATE_c6_0;
                        lexme = lexme + c;
                    }



                     // DFA_STATE_single_c
                    else if(c == '{') {
                        String ans = gotToken("'{'","'{'",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }else if(c == '~') {
                        String ans = gotToken("'~'","'~'",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    else if(c == '}') {
                        String ans = gotToken("'}'","'}'",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }else if(c == '(') {
                        String ans = gotToken("'('","'('",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }else if(c == ')') {
                        String ans = gotToken("')'","')'",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }else if(c == ';') {
                        // done
                        String ans = gotToken("';'","';'",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    else if(c == ',') {
                        String ans = gotToken("','","','",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }else if(c == ']') {
                        String ans = gotToken("']'","']'",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }else if(c == '[') {
                        String ans = gotToken("'['","'['",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }else if(c == '?') {
                        String ans = gotToken("'?'","'?'",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    // end
                    break;

                case DFA_STATE_constant:
                    if(isAlphaOrDigit2(c)) {
                        lexme += c;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,"Constant",temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    break;
                case DFA_STATE_constant_start:
                    if(c != '\'') {
                        lexme += c;
                    }
                    else {
                        lexme ='\''+ lexme+c+'\'';
                        String ans = gotToken(lexme,"Constant",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    break;


                case DFA_STATE_s1_start:
                    // done 字符串中是可以有等号的，所以在输入的时候判断是错误的
                    if(c != '"') {
                        lexme += c;
                    }
                    else {
                        lexme ='\''+ lexme +c+'\'';
                        String ans = gotToken(lexme,"StringLiteral",temp,total_index);
                        strTokens += ans;
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    break;

                case DFA_STATE_identify:
                    if(lexme.equals("L")||lexme.equals("U")|| lexme.equals("u")||lexme.equals("U8")||lexme.equals("u8")){
                        if(c=='"'){
                            state = DFA_STATE.DFA_STATE_s1_start;
                            lexme = lexme + c;
                            break;
                        }
                        else if(c=='\''){
                            state = DFA_STATE.DFA_STATE_constant_start;
                            lexme = lexme + c;
                            break;
                        }
                    }
                    if(isAlphaOrDigit(c)) {
                        lexme = lexme + c;
                    }
                    else {
                        if(this.keywordSet.contains(lexme)) {
                            lexme ='\''+ lexme+'\'';
                            // done
                            line = lIndex;

                            if (lexme.equals("'case'")){
                                total_index++;
                            }
                            int tt = total_index-1;
                            if(c == '\n'){
                                line--;
                                tt--;
//                                System.out.println("test"+total_index);
                                if(lexme.equals("'struct'")){
                                    total_index++;
                                }
                            }
//                            System.out.println("test"+c);
                            String ans = gotToken(lexme,lexme,temp,tt,line);
                            strTokens += ans;
                        }else {
                            lexme ='\''+ lexme+'\'';
                            String ans = gotToken(lexme,"Identifier",temp,total_index-1);
                            if(c == ':'){
//                                System.out.println("test"+total_index);
                                total_index++;
                            }
                            strTokens += ans;
                        }
                        iTknNum++;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                        keep = true;
                    }
                    break;

                case DFA_STATE_ADD_0:
                    if(c == '+'||c == '=') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_sub_0:
                    if(c == '-'||c == '='||c == '>') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c1_0:
                    if(c == '&'||c == '=') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c2_0:
                    if(c == '=') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c3_0:
                    if(c == '=') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c7_0:
                    if(c == '=') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c8_0:
                    if(c == '=') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c9_0:
                    if(c == '=') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c10_0:
                    if(c == '='||c == '|') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c12_0:
                    if(c == '>') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        line = lIndex;
                        int tt = total_index-1;
                        if(c == '\n'){
                            line--;
                            tt-=2; // done 减去的是\n加上的量
                        }
//                        System.out.println(": "+String.valueOf(total_index));
                        String ans = gotToken(lexme,lexme,temp,tt,line);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c13_0:
                    if(c == '#') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c11_0: // . ...
                    if(c == '.') {
                        lexme += c;
                        state = DFA_STATE.DFA_STATE_c11_1;
                    }
                    else if(isDigit(c)) {
                        lexme += c;
                        state = DFA_STATE.DFA_STATE_constant;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    break;
                case DFA_STATE_c11_1: // ...
                    if(c == '.') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c5_0:
                    if(c == '<') {
                        lexme += c;
                        state = DFA_STATE.DFA_STATE_c5_1;
                    }
                    else if(c=='='||c==':'||c=='%'){
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    break;
                case DFA_STATE_c5_1:
                    if(c == '=') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c6_0:
                    if(c == '>') {
                        lexme += c;
                        state = DFA_STATE.DFA_STATE_c6_1;
                    }
                    else if(c=='='){
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    break;
                case DFA_STATE_c6_1:
                    if(c == '=') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;
                case DFA_STATE_c4_0:
                    if(c == ':') {
                        lexme += c;
                        state = DFA_STATE.DFA_STATE_c4_1;
                    }
                    else if(c=='>'||c=='='){
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    break;
                case DFA_STATE_c4_1:
                    if(c == '%') {
                        lexme += c;
                        state = DFA_STATE.DFA_STATE_c4_2;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                        state = DFA_STATE.DFA_STATE_INITIAL;
                    }
                    break;
                case DFA_STATE_c4_2:
                    if(c == ':') {
                        lexme += c;
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index);
                        strTokens += ans;
                    }
                    else {
                        lexme ='\''+ lexme+'\'';
                        String ans = gotToken(lexme,lexme,temp,total_index-1);
                        strTokens += ans;
                        iTknNum++;
                        keep = true;
                    }
                    state = DFA_STATE.DFA_STATE_INITIAL;
                    break;

            }
        }
        reader.close();

        FileWriter writer=new FileWriter(fileName1);
        // notice 没有输出eof占位
        writer.write(strTokens);
        writer.close();
        System.out.println("scan finish");

    }

//    public static void main(String[] args) throws Exception {
//        scan myscan = new scan();
//        String filename  = "test/scanner_example.c";
//
//        myscan.readfile(filename);
//        System.out.println(args[0]);
//    }


}
