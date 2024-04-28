package parser;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

public class Parser {
    static HashSet<String> nonTerminal = new HashSet<>(); // 产生式的非终结符集合
    static HashSet<String> terminal = new HashSet<>(); // 产生式的终结符集合
    Set<String> emptyNonTerminal = new HashSet<>(); // 非终结符能否推出空串
    ItemSet prodSet = new ItemSet(); // 产生式的集合
    Map<String, Set<String>> FIRST = new HashMap<>(); // FIRST集

    public void init() throws IOException {
        readFile("input.txt");
        for (Item p : prodSet.items) {
            if (Objects.equals(p.right.get(0), "ε")) {
                emptyNonTerminal.add(p.left);
            }
        }
        setFirst();
        outputFIRST();
        ItemSet I0 = new ItemSet();
        I0.items.add(new Item(prodSet.items.get(0).left,prodSet.items.get(0).right, 0, "$"));
        outputClosure(closure(I0));
    }

    public void readFile(String filename) throws IOException {
        BufferedReader br = new BufferedReader(new FileReader(filename));
        String tmp;
        while ((tmp = br.readLine()) != null) {
            prodSet.addProd(tmp);
        }
        prodSet.items.add(0, new Item("X", Collections.singletonList(prodSet.items.get(0).left))); // 增广文法
        br.close();
    }

    public void setFirst() {
        for (Item p : prodSet.items) {
            if (!FIRST.containsKey(p.left))
                getFirst(p.left);
        }
    }

    Set<String> getFirst(String n) { // 递归获得非终结符n的FIRST集
        if (FIRST.get(n) != null) { // 计算过
            return FIRST.get(n);
        }
        // 获得左侧为n的所有产生式
        Set<Item> nProds = getRightSet(n);
        // 处理左侧为n的所有产生式
        Set<String> firstSet = new HashSet<>();
        for (Item p : nProds) {
            String c = p.right.get(0); // 获得该产生式右侧第一个符号
            if (terminal.contains(c) || Objects.equals(c, "ε")) { // 终结符
                firstSet.add(c);
            } else { // 非终结符
                int pos = 0;
                boolean check;
                do {
                    c = p.right.get(pos++);
                    check = emptyNonTerminal.contains(c);
                    if (!Objects.equals(c, n)) { // 避免左递归
                        Set<String> set = new HashSet<>(getFirst(c));
                        if (pos < p.right.size()) set.remove("ε"); // 修改引用有隐患
                        firstSet.addAll(set);
                    }
                } while (pos < p.right.size() && check);

            }
        }
        FIRST.put(n, firstSet);
        return firstSet;
    }

    void outputFIRST() {
        for (Map.Entry<String, Set<String>> e : FIRST.entrySet()) {
            System.out.print(e.getKey() + " ");
            for (String c : e.getValue()) {
                System.out.print(c + " ");
            }
            System.out.println();
        }
    }

    ItemSet closure(ItemSet I) {
        for (int i = 0; i < I.items.size(); i++) { // 遍历I中的每个项[A → α∙Bβ, a]
            Item item = I.items.get(i);
            if (item.pos == I.items.size())
                continue; // 归约态
            String c = item.right.get(item.pos);
            if (terminal.contains(c) || Objects.equals(c, "ε")) {
                // 终结符
                continue;
            }
            // 非终结符，找每个产生式B->γ
            Set<Item> set = getRightSet(c);
            for(Item it : set) {
                Set<String> aheads = new HashSet<>(); // 展望符集
                if(item.pos==item.right.size()-1) // B是最后一个符号
                    aheads.add(item.ahead); // 继承
                else{
                    String c2 = item.right.get(item.pos+1);
                    if(terminal.contains(c2)) // β为终结符
                        aheads.add(c2);
                    else {
                        aheads.addAll(FIRST.get(c2));
                        if(emptyNonTerminal.contains(c2)) {
                            aheads.add(item.ahead);
                            aheads.remove("ε");
                        }
                    }
                }
                for(String ahead : aheads) {
                    Item newItem =new Item(it.left, it.right, 0, ahead);
                    boolean check = false;
                    for(Item item1:I.items)
                    { // 去重
                        if(Objects.equals(item1, newItem)){
                            check = true;
                            break;
                        }
                    }
                    if(check) continue;
                    I.items.add(newItem);
                }
            }
        }
        return I;
    }

    void outputClosure(ItemSet I) {
        for(Item i : I.items) {
            System.out.println(i.left+"->"+i.right+" pos: "+i.pos+" ahead: "+i.ahead);
        }
    }

    Set<Item> getRightSet(String left) {
        // 找到并返回所有左侧为left的产生式
        Set<Item> set = new HashSet<>();
        for (Item p : prodSet.items) {
            if(Objects.equals(p.left, left)) set.add(p);
        }
        return set;
    }


}
