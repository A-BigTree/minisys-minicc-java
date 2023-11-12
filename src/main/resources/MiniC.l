D			[0-9]
L			[a-zA-Z_]
H     0x([a-zA-Z0-9])+

%{
  // nothing
  // 本词法定义文件基于“计算机系统综合课程设计”补充讲义121定义
%}

%%
"//"(.*)(\n)?     action:{ return(_COMMENT); }
"break"			      action:{ return(BREAK); }
"continue"	      action:{ return(CONTINUE); }
"else"			      action:{ return(ELSE); }
"for"			        action:{ return(FOR); }
"if"			        action:{ return(IF); }
"int"			        action:{ return(INT); }
"string"          action:{ return(STRING); }
"return"		      action:{ return(RETURN); }
"void"			      action:{ return(VOID); }
"while"			      action:{ return(WHILE); }
{H}               action:{ return(CONSTANT); }
{L}               action:{ return(IDENTIFIER); }
{L}({L}|{D})*		  action:{ return(IDENTIFIER); }
[1-9]{D}*					action:{ return(CONSTANT); }
{D}+							action:{ return(CONSTANT); }
\"[^"\n]*\"       action:{ return (STRING_LITERAL); }
">>"			        action:{ return(RIGHT_OP); }
"<<"			        action:{ return(LEFT_OP); }
"&&"			        action:{ return(AND_OP); }
"||"			        action:{ return(OR_OP); }
"<="		        	action:{ return(LE_OP); }
">="	        		action:{ return(GE_OP); }
"=="	        		action:{ return(EQ_OP); }
"!="	        		action:{ return(NE_OP); }
";"		        		action:{ return(SEMICOLON); }
"{"	            	action:{ return(LBRACE); }
"}"	          		action:{ return(RBRACE); }
","			        	action:{ return(COMMA); }
":"			        	action:{ return(COLON); }
"="			        	action:{ return(ASSIGN); }
"(" 		        	action:{ return(LPAREN); }
")"			        	action:{ return(RPAREN); }
"["			          action:{ return(LBRACKET); }
"]"	  	        	action:{ return(RBRACKET); }
"."			        	action:{ return(DOT); }
"&"			        	action:{ return(BITAND_OP); }
"!"			        	action:{ return(NOT_OP); }
"~"			        	action:{ return(BITINV_OP); }
"-"			        	action:{ return(MINUS); }
"+"			        	action:{ return(PLUS); }
"*"			        	action:{ return(MULTIPLY); }
"/"			        	action:{ return(SLASH); }
"%"               action:{ return(PERCENT); }
"<"			        	action:{ return(LT_OP); }
">"			        	action:{ return(GT_OP); }
"^"			        	action:{ return(BITXOR_OP); }
"|"			        	action:{ return(BITOR_OP); }
"$"               action:{ return(DOLLAR); }
[ \n\r\t]         action:{ return (_WHITESPACE); }
.			 	 	        action:{ return(_UNMATCH); }

%%

// nothing
