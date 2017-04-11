/* *** This file is given as part of the programming assignment. *** */
import java.util.ArrayList;

public class Parser {


    // tok is global to all these parsing methods;
    // scan just calls the scanner's scan method and saves the result in tok.
    private Token tok; // the current token
    private ArrayList<ArrayList<String>> symbolTable;
    
    private void scan() {
	    tok = scanner.scan();
    }

    private Scan scanner;
    
    Parser(Scan scanner) {
    	this.scanner = scanner;
    	this.symbolTable = new ArrayList<ArrayList<String>>();
    	scan();
    	program();
    	if( tok.kind != TK.EOF )
    	    parse_error("junk after logical end of program");
    }
    
    // program() parsing method
    private void program() {
        System.out.println("#include <stdio.h>");
	    System.out.println("main()"); 
	    block();
	}

    // block() parsing method
    private void block(){
        this.symbolTable.add(new ArrayList<String>()); // Adds new list to symbol table to hold legal variables in this new block
        System.out.println("{"); 
    	declaration_list();
    	statement_list();
    	System.out.println("}"); 
	    this.symbolTable.remove(this.symbolTable.size() - 1); // List for this block is popped from the stack since block has ended
    }

    // declaration_list() parsing method
    private void declaration_list() {
	// below checks whether tok is in first set of declaration.
	// here, that's easy since there's only one token kind in the set.
	// in other places, though, there might be more.
	// so, you might want to write a general function to handle that.
    	while( is(TK.DECLARE) ) {
    	    declaration();
    	}
    }

    // declaration() parsing method
    private void declaration() {
        System.out.println("int");
    	mustbe(TK.DECLARE);
    	
    	int varCount = 0; // Used to determine whether or not commas need to be printed (for multiple variable declarations on the same line)
    	// Prints redeclaration error if most recent list in symbol table already contains the variable.
    	// Else, adds the variable to the most recent list in symbol table
    	if (this.symbolTable.get(symbolTable.size() - 1).contains(tok.string)) {
    	    System.err.println("redeclaration of variable " + tok.string);
    	} else {
    	    varCount++;
    	    this.symbolTable.get(symbolTable.size() - 1).add(tok.string);
    	    int scopeLvl = symbolTable.size() - 1; // Used in print statement to munge identifiers for implementing levels for the E scoping operator in C.
    	    System.out.println("x_" + scopeLvl + tok.string);
    	}
    	mustbe(TK.ID);
    	
    	while( is(TK.COMMA) ) {
    	    mustbe(TK.COMMA);
    	    // Prints redeclaration error if most recent list in symbol table already contains the variable.
    	    // Else, adds the variable to the most recent list in symbol table    	    
    	    if (this.symbolTable.get(symbolTable.size() - 1).contains(tok.string)) {
    	        System.err.println("redeclaration of variable " + tok.string);
    	    } else {
    	        if (varCount >= 1) {
    	             System.out.println(",");
    	        }
    	        varCount++;
    	        this.symbolTable.get(symbolTable.size() - 1).add(tok.string);
    	        int scopeLvl = symbolTable.size() - 1; // Used in print statement to munge identifiers for implementing levels for the E scoping operator in C.
    	        System.out.println("x_" + scopeLvl + tok.string);
    	    }
    	    mustbe(TK.ID);
    	}
    	System.out.println(";");
    }

    // statement_list() parsing method
    private void statement_list() {
       while ( first(TK.TILDE, TK.ID, TK.PRINT, TK.DO, TK.IF, TK.FOR) ) {
           statement();
       } // checks for "FOR" token as well to account for it being added to the statement_list definition
    }
    
    // statement() parsing method    
    private void statement() {
         if (first(TK.TILDE, TK.ID)) {
            assignment();
        } else if (is(TK.PRINT)) {
            print();
        } else if (is(TK.DO)) {
            Do();
        } else if (is(TK.IF)) {
            If();
        } else if (is(TK.FOR)) { // checks for "FOR" token as well to account for it being added to the statement definition 
            For();
        } else {
            System.err.println( "Error: Should not get here.");
        }
    }
    
    // For() parsing method
    private void For() {
        System.out.println("for(;");
        mustbe(TK.FOR);
        ref_id();
        System.out.println(" < ");
        System.out.println(tok.string + ";");
        mustbe(TK.NUM);
        assignment();
        System.out.println(")");
        System.out.println("{");
        mustbe(TK.STARTFOR);
        block();
        System.out.println("}");
        mustbe(TK.ENDFOR);
    }
    
    // assignment() parsing method
    private void assignment() {
        ref_id();
        System.out.println("=");
        mustbe(TK.ASSIGN);
        expr();
        if (!is(TK.STARTFOR)) { // Added to check if assignment is within for loop condition. If so, do not print semicolon, or else it will disturb C syntax.
            System.out.println(";");
        }
    }
    
    // ref_id() parsing method
    private void ref_id() {
        if ( is(TK.TILDE) ) {
            mustbe(TK.TILDE);
            
            boolean isGlobal = true; // Used to determine whether or not variable is global (without scoping number). Needed to check its existence separately.
            if (is(TK.NUM)) {
                isGlobal = false;
                int blockLevel = Integer.parseInt(tok.string); // Translates string scope number into integer scope number so that it can be used to index symbol table.
                mustbe(TK.NUM);
                // If block level is greater than current depth of nesting, variable does not exist.
                if (blockLevel > symbolTable.size() - 1) {
                    System.err.println("no such variable ~" + blockLevel + tok.string + " on line " + tok.lineNumber);
                    System.exit(1);
                }
                // If correct list (scope) does not contain variable, then variable does not exist.
                if (!this.symbolTable.get(symbolTable.size() - blockLevel - 1).contains(tok.string)) {
                    System.err.println("no such variable ~" + blockLevel + tok.string + " on line " + tok.lineNumber);
                    System.exit(1);
                }
                int scopeLvl = symbolTable.size() - blockLevel - 1; // Used in print statement to munge identifiers for implementing levels for the E scoping operator in C.
                System.out.println("x_" + scopeLvl + tok.string);
            } 
            // If global list (scope) does not contain global variable, variable does not exist
            if (isGlobal && !this.symbolTable.get(0).contains(tok.string)) {
                System.err.println("no such variable ~" + tok.string + " on line " + tok.lineNumber);
                System.exit(1);
            }
            if (isGlobal) {
                System.out.println("x_0" + tok.string);
            }
            mustbe(TK.ID);
        } else if (is(TK.ID)) {
            int scopeLvl = isDeclared(tok.string); // Used in print statement to munge identifiers for implementing levels for the E scoping operator in C.
            // If symbol table does not contain variable, then variable does not exist.
            if (scopeLvl == symbolTable.size()) {
    	        System.err.println(tok.string + " is an undeclared variable on line " + tok.lineNumber);
    	        System.exit(1);
            }
            
            System.out.println("x_" + scopeLvl + tok.string);
            mustbe(TK.ID);
        } else {
            System.err.println( "Error: Should not get here.");
        }
    }
    
    // expr() parsing method    
    private void expr() {
        term();
        while (first(TK.PLUS, TK.MINUS)) {
            addop();
            term();
        }
    }
    
    // term() parsing method
    private void term() {
        factor();
        while (first(TK.TIMES, TK.DIVIDE)) {
            multop();
            factor();
        }
    }
    
    // addop() parsing method    
    private void addop() {
        if (is(TK.PLUS)) {
            System.out.println("+");
            scan();
        } else if (is(TK.MINUS)) {
            System.out.println("-");
            scan();
        } else {
            System.err.println("Error: Should not get here.");
        }
    }
    
    // factor() parsing method    
    private void factor() {
        if (is(TK.LPAREN)) {
            System.out.println("(");
            mustbe(TK.LPAREN);
            expr();
            System.out.println(")");
            mustbe(TK.RPAREN);
        } else if (first(TK.TILDE, TK.ID) ) {
            ref_id();
        } else if (is(TK.NUM)) {
            System.out.println(tok.string);
            mustbe(TK.NUM);
        } else {
            System.err.println( "Error: Should not get here.");
        }
    }
    
    // multop() parsing method    
    private void multop() {
        if (is(TK.TIMES)) {
            System.out.println("*");
            scan();
        } else if (is(TK.DIVIDE)) {
            System.out.println("/");
            scan();
        } else {
            System.err.println("Error: Should not get here.");
        }
    }

    // print() parsing method    
    private void print() {
        System.out.print("printf(\"%d\\n\",");
        mustbe(TK.PRINT);
        expr();
        System.out.println(");");
    }
    
    // Do() parsing method    
    private void Do() {
        System.out.println("while ("); 
        mustbe(TK.DO);
        guarded_command();
        System.out.println("}");
        mustbe(TK.ENDDO);
    }

    // guarded_command() parsing method    
    private void guarded_command() {
        expr();
        System.out.println(" <= 0)");
        System.out.println("{");
        mustbe(TK.THEN);
        block();
    }
    
    // If() parsing method    
    private void If() {
        System.out.println("if (");
        mustbe(TK.IF);
        guarded_command();
        System.out.println("}");
        while (is(TK.ELSEIF)) {
            System.out.println("else if (");
            mustbe(TK.ELSEIF);
            guarded_command();
            System.out.println("}");
        }
        if (is(TK.ELSE)) {
            System.out.println("else {");
            mustbe(TK.ELSE);
            block();
            System.out.println("}");
        }
        mustbe(TK.ENDIF);
    }
    
    // Returns true if character being scanned is the first of a nonterminal with 6 terminals in its first set.
    private boolean first(TK tk1, TK tk2, TK tk3, TK tk4, TK tk5, TK tk6) {
        return (tk1 == tok.kind || tk2 == tok.kind || tk3 == tok.kind
            || tk4 == tok.kind || tk5 == tok.kind || tk6 == tok.kind);
    }
    
    // Returns true if character being scanned is the first of a nonterminal with 2 terminals in its first set.
    private boolean first(TK tk1, TK tk2) {
        return (tk1 == tok.kind || tk2 == tok.kind);
    }
    
    // Used to determine if a variable without the scoping operator has been declared.
    // Does this by searching from most recent list of symbol table to oldest list of symbol table.
    // Returns position of symbol table if variable is found, otherwise returns size of symbol table to indicate that variable is not found.    
    private int isDeclared(String tk) {
        for (int i = symbolTable.size() - 1; i >= 0; i--) {
            if (symbolTable.get(i).contains(tk)) {
                return i;
            }
        }
        
        return symbolTable.size();
    }
    
    // is current token what we want?
    private boolean is(TK tk) {
        return tk == tok.kind;
    }

    // ensure current token is tk and skip over it.
    private void mustbe(TK tk) {
	if( tok.kind != tk ) {
	    System.err.println( "mustbe: want " + tk + ", got " +
				    tok);
	    parse_error( "missing token (mustbe)" );
	}
	scan();
    }

    private void parse_error(String msg) {
	System.err.println( "can't parse: line "
			    + tok.lineNumber + " " + msg );
	System.exit(1);
    }
}
