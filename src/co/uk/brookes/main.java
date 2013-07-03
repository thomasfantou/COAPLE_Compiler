package co.uk.brookes;

import co.uk.brookes.codegeneration.builder.Builder;
import co.uk.brookes.symboltable.STab;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Author : Fantou Thomas
 * Date: 5/22/13
 */
public class main {
    public static boolean LOGGING_SYMTAB_ENABLED = false;
    static final int
        error_free_scanner  = 0,
        error_free_parser   = 1,
        error_free_symbol_table = 2,
        code_generation = 3;

    static final int
        current_test = code_generation;



    //* Main
    public static void main(String args[]) {
        String testFile;
        Scanner scanner;
        Parser parser;
        STab.init();
        Builder.init();

        switch(current_test){
            case error_free_scanner:
                testFile = "Z:\\dev\\IdeaProjects\\COAPLE_Compiler\\test files\\scanner_test1.cpl";
                Token t;
                scanner = new Scanner(testFile);
                do {
                    t = scanner.NextToken();

                    System.out.println("line " + t.line + ", col " + t.col + ": " + t.kind + "(val  " + t.val + ")");

                } while (t.kind != 0);
                break;
            case error_free_parser:
                testFile = "Z:\\dev\\IdeaProjects\\COAPLE_Compiler\\test files\\parser_test1.cpl";
                scanner = new Scanner(testFile);
                parser = new Parser(scanner);
                parser.Parse();
                break;
            case error_free_symbol_table:
                testFile = "Z:\\dev\\IdeaProjects\\COAPLE_Compiler\\test files\\parser_symtab1.cpl";
                LOGGING_SYMTAB_ENABLED = true;
                scanner = new Scanner(testFile);
                parser = new Parser(scanner);
                parser.Parse();
                break;
            case code_generation:
                testFile = "Z:\\dev\\IdeaProjects\\COAPLE_Compiler\\test files\\code_gen1.cpl";
                scanner = new Scanner(testFile);
                parser = new Parser(scanner);
                parser.Parse();

                Builder.write();
                break;
        }
    }
    //*/

}
