package main;

import parser.Parser;
import semantic.Analyzer;

import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Parser parser = new Parser();
        parser.run();
        Analyzer.outputInstructions();
        Analyzer.outputSymbolTable();
    }
}