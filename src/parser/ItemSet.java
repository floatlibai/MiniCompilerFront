package parser;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class ItemSet { // 项目集
    List<Item> items = new ArrayList<>();

    public void addItem(String prod) { // 添加项目，约定产生式合法
        int arrowIndex = prod.indexOf("->");
        for(int i=0;i<prod.length();i++) { // 分离提取终结符和非终结符
            char c = prod.charAt(i);
            if(Character.isUpperCase(c)) {
                Parser.nonTerminal.add(c);
            } else if (i>=arrowIndex+2 && c!='|' && c!='ε') {
                Parser.terminal.add(c);
            }
        }
        for(String s : prod.substring(arrowIndex+2).split("\\|")) {
            items.add(new Item(prod.charAt(0), s));
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
