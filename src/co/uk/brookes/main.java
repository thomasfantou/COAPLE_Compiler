package co.uk.brookes;

import java.io.*;

/**
 * Author : Fantou Thomas
 * Date: 5/22/13
 */
public class main {

    /* Scanner Test Main
    public static void main(String args[]) {
        String testFile = "Z:\\dev\\IdeaProjects\\COAPLE_Compiler\\test files\\scanner_test1.cpl";
        Token t;
        Scanner scanner = new Scanner(testFile);

        do {
            t = scanner.NextToken();

                System.out.println("line " + t.line + ", col " + t.col + ": " + t.kind + "(val  " + t.val + ")");

        } while (t.kind != 0);

    }
    //*/

    //* Parser Test Main
    public static void main(String args[]) {
        String testFile = "Z:\\dev\\IdeaProjects\\COAPLE_Compiler\\test files\\parser_test1.cpl";
        Scanner scanner = new Scanner(testFile);
        Parser parser = new Parser(scanner);
        parser.Parse();

    }
    //*/



    /* Main
    public static void main(String args[]) {

    }
    //*/

}
