package main;

import lexer.Lexer;
import lexer.Token;
import parser.Parser;

import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Parser parser = new Parser();
        parser.init();
    }
}
