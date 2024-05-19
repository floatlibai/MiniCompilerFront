package lexer;

import semantic.Analyzer;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Lexer {
    enum State {
        STATE_START, STATE_LETTER, STATE_DIGIT, STATE_OPERATOR, STATE_DELIMITER, STATE_ERROR
    }

    HashMap<String, Tag> keywords = new HashMap<>();
    HashMap<String, Tag> operators = new HashMap<>(); // 算术运算符
    HashMap<String, Tag> delimiters = new HashMap<>();
    HashMap<String, Tag> identifiers = new HashMap<>(); // 暂时充当符号表
    String[] type = {"ERROR", "KEYWORD", "OPERATOR", "DELIMITER", "IDENTIFIER", "NUMBER", "END"};
    List<String> lines = new ArrayList<>();
    public List<Token> tokens = new ArrayList<>(); // token流
    public Queue<Token> tokenQueue = new LinkedList<>();

    public Lexer() {
        keywords.put("if", Tag.IF);
        keywords.put("else", Tag.ELSE);
        keywords.put("while", Tag.WHILE);
        keywords.put("int", Tag.INT);
        keywords.put("float", Tag.FLOAT);
        operators.put("+", Tag.PLUS);
        operators.put("-", Tag.MINUS);
        operators.put("*", Tag.TIMES);
        operators.put("/", Tag.OVER);
        operators.put("=", Tag.ASSIGN);
        operators.put("<", Tag.LT);
        operators.put(">", Tag.GT);
        operators.put("<=", Tag.LE);
        operators.put(">=", Tag.GE);
        operators.put("==", Tag.EQ);
        operators.put("!=", Tag.NE);
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

    public void readFromConsole() throws IOException {
        Scanner scanner = new Scanner(System.in);
        String tmp;
        while (!(tmp = scanner.nextLine()).equals("exit")) {
            lines.add(tmp);
        }
        scanner.close();
    }

    boolean isKeyword(String word) {
        return keywords.containsKey(word);
    }

    boolean isOperator(String word) {
        return operators.containsKey(word);
    }

    boolean isDelimiter(String word) {
        return delimiters.containsKey(word);
    }

    public void scanAll() {
        for (String line : lines) {
            scanLine(line);
        }
    }

    void scanLine(String line) {
        int pos = 0, len = line.length();
        State currentState = State.STATE_START;
        StringBuilder sb = new StringBuilder();
        while (pos < len) {
            char peek = line.charAt(pos);
            switch (currentState) {
                case STATE_START:
                    if (peek == ' ' || peek == '\t') {
                        pos++;
                    } else if (Character.isLetter(peek) || peek == '_') {
                        currentState = State.STATE_LETTER;
                        sb.append(peek);
                        pos++;
                    } else if (Character.isDigit(peek)) {
                        currentState = State.STATE_DIGIT;
                        sb.append(peek);
                        pos++;
                    } else if (isOperator(String.valueOf(peek)) || peek == '!') {
                        currentState = State.STATE_OPERATOR;
                        sb.append(peek);
                        pos++;
                    } else if (isDelimiter(String.valueOf(peek))) {
                        currentState = State.STATE_DELIMITER;
                        sb.append(peek);
                        pos++;
                    } else {
                        processToken(String.valueOf(peek), State.STATE_ERROR);
                        pos++;
                    }
                    break;
                case STATE_LETTER:
                    if (Character.isLetter(peek) || peek == '_') {
                        sb.append(peek);
                        pos++;
                    } else { // 该词法中标识符中不能有数字
                        processToken(sb.toString(), currentState);
                        sb.setLength(0);
                        currentState = State.STATE_START;
                    }
                    break;
                case STATE_DIGIT:
                    if (Character.isDigit(peek)) {
                        sb.append(peek);
                        pos++;
                    } else {
                        processToken(sb.toString(), currentState);
                        sb.setLength(0);
                        currentState = State.STATE_START;
                    }
                    break;
                case STATE_OPERATOR:
                    if (isOperator(sb.toString() + peek)) {
                        processToken(sb.toString() + peek, currentState);
                        sb.setLength(0);
                        currentState = State.STATE_START;
                        pos++;
                    } else {
                        if (sb.charAt(0) == '!') {
                            processToken("!", State.STATE_ERROR);
                        } else {
                            processToken(sb.toString(), currentState);
                        }
                        sb.setLength(0);
                        currentState = State.STATE_START;
                    }
                    break;
                case STATE_DELIMITER:
                    processToken(sb.toString(), currentState);
                    sb.setLength(0);
                    currentState = State.STATE_START;
                    break;
            }
        }
        if (currentState != State.STATE_START) {
            processToken(sb.toString(), currentState);
        }
    }

    void processToken(String tokenValue, State state) {
        switch (state) {
            case STATE_LETTER:
                if (isKeyword(tokenValue)) { // 是关键字吗
                    tokens.add(new Token(keywords.get(tokenValue), tokenValue, type[1]));
                } else {
                    if (identifiers.containsKey(tokenValue)) { // 是已经存在的标识符吗
                        tokens.add(new Token(Tag.ID, tokenValue, type[4]));
                    } else {
                        identifiers.put(tokenValue, Tag.ID);
                        Analyzer.symTable.put(tokenValue,"");
                        tokens.add(new Token(Tag.ID, tokenValue, type[4]));
                    }
                }
                break;
            case STATE_DIGIT:
                tokens.add(new Token(Tag.NUMBER, tokenValue, type[5]));
                break;
            case STATE_OPERATOR:
                tokens.add(new Token(operators.get(tokenValue), tokenValue, type[2]));
                break;
            case STATE_DELIMITER:
                tokens.add(new Token(delimiters.get(tokenValue), tokenValue, type[3]));
                break;
            case STATE_ERROR:
                tokens.add(new Token(Tag.ERROR, tokenValue, type[0]));
                break;
        }
    }

    public void outputTokenQueue() {
        Queue<Token> queue = new LinkedList<>(tokenQueue);
        System.out.print("tokenQueue: ");
        while (!queue.isEmpty()) {
            System.out.print(queue.poll().value+" ");
        }
        System.out.println();
    }

    public void run() throws IOException {
//        readFile("lexical.txt");
        System.out.println("Please enter lines of text (type 'exit' to finish):");
        readFromConsole();

        scanAll();
        String outputFilePath = "token.txt";
        FileWriter writer = new FileWriter(outputFilePath);
        System.out.println("================Token序列=================");
        for (Token t : tokens) {
            writer.write(t.toString() + "\n");
            if(Objects.equals(t.type, "IDENTIFIER")) {
                System.out.println("token: "+"( identifier, "+t.value+" )");
            } else if(Objects.equals(t.type, "KEYWORD")) {
                System.out.println("token: "+"( keyword, "+t.value+" )");
            } else if(Objects.equals(t.type, "OPERATOR")) {
                System.out.println("token: "+"( operator, "+t.value+" )");
            } else if (Objects.equals(t.type, "DELIMITER")) {
                System.out.println("token: "+"( delimiter, "+t.value+" )");
            } else if(Objects.equals(t.type, "NUMBER")){
                System.out.println("token: "+"( digits, "+t.value+" )");
            }
        }
        writer.close();

        for(Token t : tokens) { // 创建Token队列供语法分析使用
            tokenQueue.offer(t);
        }
        tokenQueue.offer(new Token(Tag.END, "$", type[6]));
    }
}
