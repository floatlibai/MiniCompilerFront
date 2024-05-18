package main;

import lexer.Lexer;
import lexer.Token;
import parser.Item;
import parser.ItemSet;
import parser.Parser;
import semantic.Analyzer;

import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Main {
    public static void main(String[] args) throws IOException {
        Parser parser = new Parser();
        parser.init();
        Analyzer.outputInstructions();
        Analyzer.outputSymbolTable();
    }
}
