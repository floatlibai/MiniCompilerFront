package main;

import lexer.Lexer;
import lexer.Token;

import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Lexer lexer = new Lexer();
        lexer.run("input.txt");
    }
}
