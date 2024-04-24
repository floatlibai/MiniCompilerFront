package lexer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

public class Lexer {
    HashMap<String, Integer> keywords = new HashMap<>();
    HashMap<String, Integer> ariOperators = new HashMap<>(); // 算术运算符
    HashMap<String, Integer> relOperators = new HashMap<>(); // 关系运算符
    HashMap<String, Integer> delimiters = new HashMap<>();
    HashMap<String, Integer> identifiers = new HashMap<>();
    String[] type = {"ERROR", "KEYWORD", "OPERATOR", "DELIMITER", "IDENTIFIER", "NUMBER"};
    List<String> lines = new ArrayList<>();
    public List<Token> tokens = new ArrayList<>(); // token流

    public Lexer() {
        keywords.put("if", Tag.IF);
        keywords.put("else", Tag.ELSE);
        keywords.put("while", Tag.WHILE);
        keywords.put("int", Tag.INT);
        keywords.put("float", Tag.FLOAT);
        ariOperators.put("+", Tag.PLUS);
        ariOperators.put("-", Tag.MINUS);
        ariOperators.put("*", Tag.TIMES);
        ariOperators.put("/", Tag.OVER);
        ariOperators.put("=", Tag.ASSIGN);
        relOperators.put("<", Tag.LT);
        relOperators.put(">", Tag.GT);
        relOperators.put("<=", Tag.LE);
        relOperators.put(">=", Tag.GE);
        relOperators.put("==", Tag.EQ);
        relOperators.put("!=", Tag.NE);
        delimiters.put("(", Tag.LBracket);
        delimiters.put(")", Tag.RBracket);
        delimiters.put(";", Tag.SEMICOLON);
        delimiters.put("'", Tag.Quotation);
    }

    public void readFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String tmp;
        while ((tmp = br.readLine()) != null) {
            lines.add(tmp);
        }
        br.close();
    }

    boolean isKeyword(String word) {
        return keywords.containsKey(word);
    }

    boolean isAriOperator(char ch) {
        return ariOperators.containsKey(String.valueOf(ch));
    }

    boolean isRelOperator(char ch) {
        return relOperators.containsKey(String.valueOf(ch));
    }

    boolean isRelOperator(String word) {
        return relOperators.containsKey(word);
    }

    boolean isDelimiter(String word) {
        return delimiters.containsKey(word);
    }

    public void scan() {
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i);
            int pos = 0, len = line.length();
            char peek;
            while (pos < len) {
                peek = line.charAt(pos);
                if (peek == ' ' || peek == '\t') { // 跳过空格或多个空格，行末没有\n，不用处理
                    pos++;
                    continue;
                }
                switch (peek) { // 处理关系运算符
                    case '=':
                        if (pos + 1 < len && line.charAt(pos + 1) == '=') {
                            tokens.add(new Token(Tag.EQ, "==", type[2]));
                            pos = pos + 2;
                            continue;
                        } else {
                            tokens.add(new Token(Tag.ASSIGN, "=", type[2]));
                            pos++;
                            continue;
                        }
                    case '<':
                        if (pos + 1 < len && line.charAt(pos + 1) == '=') {
                            tokens.add(new Token(Tag.LE, "<=", type[2]));
                            pos = pos + 2;
                            continue;
                        } else {
                            tokens.add(new Token(Tag.LT, "<", type[2]));
                            pos++;
                            continue;
                        }
                    case '>':
                        if (pos + 1 < len && line.charAt(pos + 1) == '=') {
                            tokens.add(new Token(Tag.EQ, ">=", type[2]));
                            pos = pos + 2;
                            continue;
                        } else {
                            tokens.add(new Token(Tag.ASSIGN, ">", type[2]));
                            pos++;
                            continue;
                        }
                }
                if (Character.isDigit(peek)) { // 是整数吗
                    int num = 0;
                    while (true) {
                        num = 10 * num + Character.digit(peek, 10);
                        if (pos + 1 < len && Character.isDigit(line.charAt(pos + 1))) {
                            pos++;
                            peek = line.charAt(pos);
                        } else {
                            pos++;
                            break;
                        }
                    }
                    tokens.add(new Token(Tag.INT, String.valueOf(num), type[5]));
                    continue;
                }
                if (Character.isLetter(peek) || peek == '_') {
                    StringBuilder sb = new StringBuilder();
                    while (true) {
                        sb.append(peek);
                        if (pos + 1 < len && (Character.isLetter(line.charAt(pos + 1)) || line.charAt(pos + 1) == '_')) {
                            pos++;
                            peek = line.charAt(pos);
                        } else {
                            pos++;
                            break;
                        }
                    }
                    if (isKeyword(sb.toString())) { // 是关键字吗
                        tokens.add(new Token(keywords.get(sb.toString()), sb.toString(), type[1]));
                    } else {
                        if (identifiers.containsKey(sb.toString())) { // 是已经存在的标识符吗
                            tokens.add(new Token(Tag.ID, sb.toString(), type[4]));
                        } else {
                            identifiers.put(sb.toString(), Tag.ID);
                            tokens.add(new Token(Tag.ID, sb.toString(), type[4]));
                        }
                    }
                    continue;
                }
                if (isAriOperator(line.charAt(pos))) {
                    tokens.add(new Token(ariOperators.get(String.valueOf(line.charAt(pos))), String.valueOf(line.charAt(pos)), type[2]));
                    pos++;
                    continue;
                }
                if (isDelimiter(String.valueOf(line.charAt(pos)))) {
                    tokens.add(new Token(delimiters.get(String.valueOf(line.charAt(pos))), String.valueOf(line.charAt(pos)), type[3]));
                    pos++;
                    continue;
                }
                tokens.add(new Token(Tag.ERROR, String.valueOf(i + 1), type[0]));
                pos++;
            }
        }
    }
}
