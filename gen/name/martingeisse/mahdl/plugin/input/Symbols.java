package name.martingeisse.mahdl.plugin.input;

public class Symbols {

	//
	// terminals
	//

	public static final MahdlElementType BLOCK_COMMENT = new MahdlElementType("BLOCK_COMMENT");
	public static final MahdlElementType CLOSING_CURLY_BRACE = new MahdlElementType("CLOSING_CURLY_BRACE");
	public static final MahdlElementType CLOSING_PARENTHESIS = new MahdlElementType("CLOSING_PARENTHESIS");
	public static final MahdlElementType CLOSING_SQUARE_BRACKET = new MahdlElementType("CLOSING_SQUARE_BRACKET");
	public static final MahdlElementType COLON = new MahdlElementType("COLON");
	public static final MahdlElementType COMMA = new MahdlElementType("COMMA");
	public static final MahdlElementType DOT = new MahdlElementType("DOT");
	public static final MahdlElementType EQUALS = new MahdlElementType("EQUALS");
	public static final MahdlElementType IDENTIFIER = new MahdlElementType("IDENTIFIER");
	public static final MahdlElementType INTEGER_LITERAL = new MahdlElementType("INTEGER_LITERAL");
	public static final MahdlElementType KW_BIT = new MahdlElementType("KW_BIT");
	public static final MahdlElementType KW_BREAK = new MahdlElementType("KW_BREAK");
	public static final MahdlElementType KW_CASE = new MahdlElementType("KW_CASE");
	public static final MahdlElementType KW_CONSTANT = new MahdlElementType("KW_CONSTANT");
	public static final MahdlElementType KW_DEFAULT = new MahdlElementType("KW_DEFAULT");
	public static final MahdlElementType KW_DO = new MahdlElementType("KW_DO");
	public static final MahdlElementType KW_ELSE = new MahdlElementType("KW_ELSE");
	public static final MahdlElementType KW_IF = new MahdlElementType("KW_IF");
	public static final MahdlElementType KW_INOUT = new MahdlElementType("KW_INOUT");
	public static final MahdlElementType KW_INPUT = new MahdlElementType("KW_INPUT");
	public static final MahdlElementType KW_INTEGER = new MahdlElementType("KW_INTEGER");
	public static final MahdlElementType KW_INTERFACE = new MahdlElementType("KW_INTERFACE");
	public static final MahdlElementType KW_MEMORY = new MahdlElementType("KW_MEMORY");
	public static final MahdlElementType KW_MODULE = new MahdlElementType("KW_MODULE");
	public static final MahdlElementType KW_OUTPUT = new MahdlElementType("KW_OUTPUT");
	public static final MahdlElementType KW_REGISTER = new MahdlElementType("KW_REGISTER");
	public static final MahdlElementType KW_SIGNAL = new MahdlElementType("KW_SIGNAL");
	public static final MahdlElementType KW_SWITCH = new MahdlElementType("KW_SWITCH");
	public static final MahdlElementType KW_TEXT = new MahdlElementType("KW_TEXT");
	public static final MahdlElementType KW_VECTOR = new MahdlElementType("KW_VECTOR");
	public static final MahdlElementType LINE_COMMENT = new MahdlElementType("LINE_COMMENT");
	public static final MahdlElementType OPENING_CURLY_BRACE = new MahdlElementType("OPENING_CURLY_BRACE");
	public static final MahdlElementType OPENING_PARENTHESIS = new MahdlElementType("OPENING_PARENTHESIS");
	public static final MahdlElementType OPENING_SQUARE_BRACKET = new MahdlElementType("OPENING_SQUARE_BRACKET");
	public static final MahdlElementType OP_AND = new MahdlElementType("OP_AND");
	public static final MahdlElementType OP_CONCAT = new MahdlElementType("OP_CONCAT");
	public static final MahdlElementType OP_DIVIDED_BY = new MahdlElementType("OP_DIVIDED_BY");
	public static final MahdlElementType OP_EQUAL = new MahdlElementType("OP_EQUAL");
	public static final MahdlElementType OP_GREATER_THAN = new MahdlElementType("OP_GREATER_THAN");
	public static final MahdlElementType OP_GREATER_THAN_OR_EQUAL = new MahdlElementType("OP_GREATER_THAN_OR_EQUAL");
	public static final MahdlElementType OP_LESS_THAN = new MahdlElementType("OP_LESS_THAN");
	public static final MahdlElementType OP_LESS_THAN_OR_EQUAL = new MahdlElementType("OP_LESS_THAN_OR_EQUAL");
	public static final MahdlElementType OP_MINUS = new MahdlElementType("OP_MINUS");
	public static final MahdlElementType OP_MUX = new MahdlElementType("OP_MUX");
	public static final MahdlElementType OP_NOT = new MahdlElementType("OP_NOT");
	public static final MahdlElementType OP_NOT_EQUAL = new MahdlElementType("OP_NOT_EQUAL");
	public static final MahdlElementType OP_OR = new MahdlElementType("OP_OR");
	public static final MahdlElementType OP_PLUS = new MahdlElementType("OP_PLUS");
	public static final MahdlElementType OP_REMAINDER = new MahdlElementType("OP_REMAINDER");
	public static final MahdlElementType OP_SHIFT_LEFT = new MahdlElementType("OP_SHIFT_LEFT");
	public static final MahdlElementType OP_SHIFT_RIGHT = new MahdlElementType("OP_SHIFT_RIGHT");
	public static final MahdlElementType OP_TIMES = new MahdlElementType("OP_TIMES");
	public static final MahdlElementType OP_XOR = new MahdlElementType("OP_XOR");
	public static final MahdlElementType SEMICOLON = new MahdlElementType("SEMICOLON");
	public static final MahdlElementType TEXT_LITERAL = new MahdlElementType("TEXT_LITERAL");
	public static final MahdlElementType VECTOR_LITERAL = new MahdlElementType("VECTOR_LITERAL");


	//
	// nonterminals
	//

	public static final MahdlElementType caseItem_Default = new MahdlElementType("caseItem_Default");
	public static final MahdlElementType caseItem_Value = new MahdlElementType("caseItem_Value");
	public static final MahdlElementType dataType_Bit = new MahdlElementType("dataType_Bit");
	public static final MahdlElementType dataType_Integer = new MahdlElementType("dataType_Integer");
	public static final MahdlElementType dataType_Memory = new MahdlElementType("dataType_Memory");
	public static final MahdlElementType dataType_Text = new MahdlElementType("dataType_Text");
	public static final MahdlElementType dataType_Vector = new MahdlElementType("dataType_Vector");
	public static final MahdlElementType doBlockTrigger_Clocked = new MahdlElementType("doBlockTrigger_Clocked");
	public static final MahdlElementType doBlockTrigger_Combinatorial = new MahdlElementType("doBlockTrigger_Combinatorial");
	public static final MahdlElementType expression_BinaryAnd = new MahdlElementType("expression_BinaryAnd");
	public static final MahdlElementType expression_BinaryConcat = new MahdlElementType("expression_BinaryConcat");
	public static final MahdlElementType expression_BinaryDividedBy = new MahdlElementType("expression_BinaryDividedBy");
	public static final MahdlElementType expression_BinaryEqual = new MahdlElementType("expression_BinaryEqual");
	public static final MahdlElementType expression_BinaryGreaterThan = new MahdlElementType("expression_BinaryGreaterThan");
	public static final MahdlElementType expression_BinaryGreaterThanOrEqual = new MahdlElementType("expression_BinaryGreaterThanOrEqual");
	public static final MahdlElementType expression_BinaryLessThan = new MahdlElementType("expression_BinaryLessThan");
	public static final MahdlElementType expression_BinaryLessThanOrEqual = new MahdlElementType("expression_BinaryLessThanOrEqual");
	public static final MahdlElementType expression_BinaryMinus = new MahdlElementType("expression_BinaryMinus");
	public static final MahdlElementType expression_BinaryNotEqual = new MahdlElementType("expression_BinaryNotEqual");
	public static final MahdlElementType expression_BinaryOr = new MahdlElementType("expression_BinaryOr");
	public static final MahdlElementType expression_BinaryPlus = new MahdlElementType("expression_BinaryPlus");
	public static final MahdlElementType expression_BinaryRemainder = new MahdlElementType("expression_BinaryRemainder");
	public static final MahdlElementType expression_BinaryShiftLeft = new MahdlElementType("expression_BinaryShiftLeft");
	public static final MahdlElementType expression_BinaryShiftRight = new MahdlElementType("expression_BinaryShiftRight");
	public static final MahdlElementType expression_BinaryTimes = new MahdlElementType("expression_BinaryTimes");
	public static final MahdlElementType expression_BinaryXor = new MahdlElementType("expression_BinaryXor");
	public static final MahdlElementType expression_FunctionCall = new MahdlElementType("expression_FunctionCall");
	public static final MahdlElementType expression_Identifier = new MahdlElementType("expression_Identifier");
	public static final MahdlElementType expression_IndexSelection = new MahdlElementType("expression_IndexSelection");
	public static final MahdlElementType expression_InstancePort = new MahdlElementType("expression_InstancePort");
	public static final MahdlElementType expression_Literal = new MahdlElementType("expression_Literal");
	public static final MahdlElementType expression_Mux = new MahdlElementType("expression_Mux");
	public static final MahdlElementType expression_Parenthesized = new MahdlElementType("expression_Parenthesized");
	public static final MahdlElementType expression_RangeSelection = new MahdlElementType("expression_RangeSelection");
	public static final MahdlElementType expression_UnaryMinus = new MahdlElementType("expression_UnaryMinus");
	public static final MahdlElementType expression_UnaryNot = new MahdlElementType("expression_UnaryNot");
	public static final MahdlElementType expression_UnaryPlus = new MahdlElementType("expression_UnaryPlus");
	public static final MahdlElementType implementationItem_DoBlock = new MahdlElementType("implementationItem_DoBlock");
	public static final MahdlElementType implementationItem_ModuleInstance = new MahdlElementType("implementationItem_ModuleInstance");
	public static final MahdlElementType implementationItem_SignalLikeDefinitionGroup = new MahdlElementType("implementationItem_SignalLikeDefinitionGroup");
	public static final MahdlElementType instancePortName = new MahdlElementType("instancePortName");
	public static final MahdlElementType instanceReferenceName = new MahdlElementType("instanceReferenceName");
	public static final MahdlElementType literal_Integer = new MahdlElementType("literal_Integer");
	public static final MahdlElementType literal_Text = new MahdlElementType("literal_Text");
	public static final MahdlElementType literal_Vector = new MahdlElementType("literal_Vector");
	public static final MahdlElementType module = new MahdlElementType("module");
	public static final MahdlElementType portConnection = new MahdlElementType("portConnection");
	public static final MahdlElementType portDefinition = new MahdlElementType("portDefinition");
	public static final MahdlElementType portDefinitionGroup = new MahdlElementType("portDefinitionGroup");
	public static final MahdlElementType portDirection_Inout = new MahdlElementType("portDirection_Inout");
	public static final MahdlElementType portDirection_Input = new MahdlElementType("portDirection_Input");
	public static final MahdlElementType portDirection_Output = new MahdlElementType("portDirection_Output");
	public static final MahdlElementType qualifiedModuleName = new MahdlElementType("qualifiedModuleName");
	public static final MahdlElementType signalLikeDefinition_WithInitializer = new MahdlElementType("signalLikeDefinition_WithInitializer");
	public static final MahdlElementType signalLikeDefinition_WithoutInitializer = new MahdlElementType("signalLikeDefinition_WithoutInitializer");
	public static final MahdlElementType signalLikeKind_Constant = new MahdlElementType("signalLikeKind_Constant");
	public static final MahdlElementType signalLikeKind_Register = new MahdlElementType("signalLikeKind_Register");
	public static final MahdlElementType signalLikeKind_Signal = new MahdlElementType("signalLikeKind_Signal");
	public static final MahdlElementType statement_Assignment = new MahdlElementType("statement_Assignment");
	public static final MahdlElementType statement_Block = new MahdlElementType("statement_Block");
	public static final MahdlElementType statement_Break = new MahdlElementType("statement_Break");
	public static final MahdlElementType statement_IfThen = new MahdlElementType("statement_IfThen");
	public static final MahdlElementType statement_IfThenElse = new MahdlElementType("statement_IfThenElse");
	public static final MahdlElementType statement_Switch = new MahdlElementType("statement_Switch");
	public static final MahdlElementType synthetic_List_CaseItem = new MahdlElementType("synthetic_List_CaseItem");
	public static final MahdlElementType synthetic_List_ImplementationItem = new MahdlElementType("synthetic_List_ImplementationItem");
	public static final MahdlElementType synthetic_List_PortConnection = new MahdlElementType("synthetic_List_PortConnection");
	public static final MahdlElementType synthetic_List_PortDefinitionGroup = new MahdlElementType("synthetic_List_PortDefinitionGroup");
	public static final MahdlElementType synthetic_List_Statement = new MahdlElementType("synthetic_List_Statement");
	public static final MahdlElementType synthetic_SeparatedList_Expression_COMMA = new MahdlElementType("synthetic_SeparatedList_Expression_COMMA");
	public static final MahdlElementType synthetic_SeparatedList_IDENTIFIER_DOT = new MahdlElementType("synthetic_SeparatedList_IDENTIFIER_DOT");
	public static final MahdlElementType synthetic_SeparatedList_PortDefinition_COMMA = new MahdlElementType("synthetic_SeparatedList_PortDefinition_COMMA");
	public static final MahdlElementType synthetic_SeparatedList_SignalLikeDefinition_COMMA = new MahdlElementType("synthetic_SeparatedList_SignalLikeDefinition_COMMA");


	//
	// special
	//

    // partially parsed input in case of an error
	public static final MahdlElementType __PARSED_FRAGMENT = new MahdlElementType("__PARSED_FRAGMENT");

}
