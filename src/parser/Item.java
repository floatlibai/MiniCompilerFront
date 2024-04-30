package parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

public class Item { // 单个项目
    String left; // 左侧非终结符，约定为大写字母
    List<String> right = new ArrayList<>(); // 右侧文法符号集合
    int pos = -1; // 点的位置
    String ahead; // 展望符

    public Item(String prod) { // 约定产生式的格式为 "A -> a B c"
        String[] s = prod.split(" ");
        this.left = s[0];
        this.right.addAll(Arrays.asList(s).subList(2, s.length));
    }

    public Item(String left, List<String> right) {
        this.left = left;
        this.right = right;
    }

    public Item(String left, List<String> right, int pos, String ahead) {
        this.left = left;
        this.right = right;
        this.pos = pos;
        this.ahead = ahead;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(left).append(" -> ");
        for(int i=0;i<right.size();i++) {
            if(i==pos) sb.append(".");
            sb.append(right.get(i));
        }
        sb.append(", ").append(ahead);
        return sb.toString();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        Item item = (Item) obj;
        return Objects.equals(left, item.left) && Objects.equals(right, item.right) && pos == item.pos && Objects.equals(ahead, item.ahead);
    }

}
