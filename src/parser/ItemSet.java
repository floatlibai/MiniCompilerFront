package parser;

import java.util.*;

public class ItemSet { // 项目集
    List<Item> items = new ArrayList<>();

    public void addProd(String prod) { // 约定产生式的格式为 "A -> a B c | b"
        String[] part1 = prod.split(" -> ");
        String[] part2 = part1[1].split("\\|");
        Parser.nonTerminal.add(part1[0].trim());
        for(String a : part2) {
            String[] part3 = a.split(" ");
            List<String> list = new ArrayList<>();
            for(String b:part3){
                String c = b.trim();
                if(c.length()==1 && Character.isUpperCase(c.charAt(0)))
                    Parser.nonTerminal.add(c);
                else Parser.terminal.add(c);
                list.add(c);
            }
            items.add(new Item(part1[0].trim(),list));
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null || getClass() != obj.getClass()) return false;
        ItemSet itemSet = (ItemSet) obj;
        return items.equals(itemSet.items);
    }

}
