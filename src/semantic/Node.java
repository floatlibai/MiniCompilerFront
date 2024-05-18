package semantic;

import lexer.Tag;
import lexer.Token;

import java.util.HashSet;
import java.util.Set;

public class Node { // 带有综合属性的符号节点
    public int instr = -1; // 指令标号
    public Set<Integer> trueList = new HashSet<>();
    public Set<Integer> falseList = new HashSet<>(); // 存放指令数组中相应指令的标号
    public Set<Integer> nextList = new HashSet<>();
    public String symbol;
    public String lexeme;

    public Node(String symbol) {
        this.symbol = symbol;
    }
    public Node(Token token) {
        if(token.tag== Tag.ID) {
            this.symbol = "id";
            this.lexeme = token.value;
        } else if(token.tag == Tag.NUMBER) {
            this.symbol = "digits";
            this.lexeme = token.value;
        } else if (token.tag == Tag.INT) {
            this.symbol = "int";
            this.lexeme = "int";
        } else if (token.tag==Tag.FLOAT) {
            this.symbol = "float";
            this.lexeme = "float";
        } else {
            this.symbol = token.value;
        }
    }
}