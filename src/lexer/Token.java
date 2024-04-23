package lexer;

public class Token {
    public int tag; // 种别码
    public String value; // 字面值
    public String type; // 类型名

    public Token(int tag, String value, String type) {
        this.tag = tag;
        this.value = value;
        this.type = type;
    }

    public String toString() {
        return String.format("%10s, %2s, %s",type, tag, value);
    }
}
