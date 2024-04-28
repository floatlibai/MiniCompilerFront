package parser;

import java.util.Objects;

public class Item { // 单个项目
    char left; // 左侧非终结符，约定为大写字母
    String right; // 右侧文法符号串
    int pos; // 点的位置
    char ahead; // 展望符

    public Item(String prod) { // 约定产生式的格式为A->aBc且合法
        this.left = prod.charAt(0);
        this.right = prod.substring(3);
        this.pos = -1;
    }
    public Item(char left, String right) { // 约定产生式的格式为A->aBc且合法
        this.left = left;
        this.right = right;
        this.pos = -1;
    }

    public Item(String prod, int pos, char ahead) { // 约定产生式的格式为A->aBc且合法
        this.left = prod.charAt(0);
        this.right = prod.substring(3);
        this.pos = pos;
        this.ahead = ahead;
    }
    public Item(char left,String right, int pos, char ahead) { // 约定产生式的格式为A->aBc且合法
        this.left = left;
        this.right = right;
        this.pos = pos;
        this.ahead = ahead;
    }

    char getLeft() {
        return left;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Item item = (Item) obj;
        return left == item.left && Objects.equals(right, item.right) && pos == item.pos && ahead == item.ahead;
    }

}
