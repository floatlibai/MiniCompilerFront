package main;

import lexer.Lexer;
import lexer.Token;

import java.io.FileWriter;
import java.io.IOException;

public class Main {
    public static void main(String[] args) throws IOException {
        Lexer lexer = new Lexer();
        lexer.readFile("input.txt");
        lexer.scan();
        String outputFilePath = "output.txt";
        FileWriter writer = new FileWriter(outputFilePath);
        for(Token t: lexer.tokens) {
            writer.write(t.toString()+"\n");
        }
        writer.close();
    }
}
