package parser;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ItemSet { // 项目集
    List<Item> items = new ArrayList<>();

    public void addProd(String prod) { // 约定产生式的格式为 "A -> a B c | b"
        String[] part1 = prod.split(" -> ");
        String[] part2 = part1[1].split(" \\| ");
        Parser.nonTerminal.add(part1[0]);
        for (String a : part2) {
            String[] part3 = a.split(" ");
            List<String> list = new ArrayList<>();
            for (String b : part3) {
                if (b.length() == 1 && Character.isUpperCase(b.charAt(0)))
                    Parser.nonTerminal.add(b);
                else Parser.terminal.add(b);
                list.add(b);
            }
            items.add(new Item(part1[0].trim(), list));
        }
    }

    public int findProd(Item I) {
        for (int i = 0; i < items.size(); i++) {
            if (Objects.equals(items.get(i).left, I.left) && Objects.equals(items.get(i).right, I.right)) {
                return i;
            }
        }
        return -1;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ItemSet itemSet = (ItemSet) obj;
        return items.equals(itemSet.items);
    }

}
