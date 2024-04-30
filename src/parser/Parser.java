package parser;

import javafx.util.Pair;
import lexer.Lexer;
import lexer.Tag;
import lexer.Token;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

public class Parser {
    static Set<String> nonTerminal = new HashSet<>(); // 产生式的非终结符集合
    static Set<String> terminal = new HashSet<>(); // 产生式的终结符集合
    static Set<String> symbols = new HashSet<>();
    Set<String> emptyNonTerminal = new HashSet<>(); // 非终结符能否推出空串
    ItemSet prodSet = new ItemSet(); // 产生式的集合
    Map<String, Set<String>> FIRST = new HashMap<>(); // FIRST集
    List<ItemSet> C = new ArrayList<>(); // 项目集族
    Map<Pair<Integer, String>, Integer> GOTO = new HashMap<>(); // GOTO表
    Set<String> visited = new HashSet<>(); // 广搜闭包的visited数组
    Queue<Item> closureQueue = new LinkedList<>(); // 待搜索加入闭包的符号队列

    enum State {
        SHIFT, REDUCE, ACCEPT, ERROR
    }

    String[] stateStr = new String[]{"SHIFT", "REDUCE", "ACCEPT", "ERROR"};
    Map<Pair<Integer, String>, Pair<State, Integer>> ACTION = new HashMap<>(); // Action表

    Deque<Integer> statusStack = new ArrayDeque<>(); // 状态栈
    Deque<String> symbolStack = new ArrayDeque<>(); // 符号栈
    Lexer lexer = new Lexer();

    public void init() throws IOException {
        lexer.run();

        readFile();
        for (Item p : prodSet.items) {
            if (Objects.equals(p.right.get(0), "ε")) {
                emptyNonTerminal.add(p.left);
            }
        }
        setFirst();
        outputFIRST();
        outputSymbol();
        buildTable();
        outputTable();
        statusStack.push(0);
        symbolStack.push("$");

        parser();
    }

    public void readFile() throws IOException {
        String filePath = "grammar.txt";
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String tmp;
        while ((tmp = br.readLine()) != null) {
            prodSet.addProd(tmp);
        }
        prodSet.items.add(0, new Item("@", Collections.singletonList(prodSet.items.get(0).left))); // 增广文法
        nonTerminal.add("@");
        terminal.add("$");
        br.close();
    }

    public void setFirst() {
        for (Item p : prodSet.items) {
            if (!FIRST.containsKey(p.left)) getFirst(p.left);
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
            if (terminal.contains(c)) { // 终结符
                firstSet.add(c);
            } else { // 非终结符
                int pos = 0;
                boolean toEmpty;
                do {
                    c = p.right.get(pos++);
                    toEmpty = emptyNonTerminal.contains(c);
                    if (!Objects.equals(c, n)) { // 避免直接左递归
                        Set<String> set = new HashSet<>(getFirst(c));
                        if (pos < p.right.size()) set.remove("ε"); // 修改引用有隐患
                        firstSet.addAll(set);
                    }
                } while (pos < p.right.size() && toEmpty);

            }
        }
        FIRST.put(n, firstSet);
        return firstSet;
    }

    void outputFIRST() {
        for (Map.Entry<String, Set<String>> e : FIRST.entrySet()) {
            System.out.print(e.getKey() + ": ");
            for (String c : e.getValue()) {
                System.out.print(c + " ");
            }
            System.out.println();
        }
    }

    ItemSet Closure(ItemSet I) {
        for (int i = 0; i < I.items.size(); i++) {
            Item item = I.items.get(i); // 遍历每一个项目
            if (item.pos == -1 || item.pos == item.right.size()) continue; //找不到或归约态
            String c = item.right.get(item.pos);
            if (terminal.contains(c)) { // 移入态或空串
                if (Objects.equals(c, "ε")) item.pos++;
                continue;
            }
            // 待约态，找到c的所有产生式
            Set<Item> set = getRightSet(c);
            for (Item it : set) { // 遍历每个产生式，找展望符
                Set<String> aheads = new HashSet<>(); // 展望符集
                if (item.pos == item.right.size() - 1) // B是最后一个符号
                    aheads.add(item.ahead); // 继承
                else {
                    String c2 = item.right.get(item.pos + 1);
                    if (terminal.contains(c2)) // β为终结符
                        aheads.add(c2);
                    else {
                        aheads.addAll(FIRST.get(c2));
                        if (emptyNonTerminal.contains(c2)) {
                            aheads.add(item.ahead);
                        }
                        aheads.remove("ε"); // 尝试将空串加入展望符中
                    }
                }
//                if(emptyNonTerminal.contains(c)) aheads.add("$");
                for (String ahead : aheads) {
                    Item newItem = new Item(it.left, it.right, 0, ahead);
                    if (Objects.equals(newItem.right.get(0), "ε")) newItem.pos++;
                    if (I.items.contains(newItem)) // 去重
                        continue;
                    I.items.add(newItem);
                }
            }

        }
        return I;
    }

    ItemSet Goto(ItemSet I, String X) {
        ItemSet J = new ItemSet();
        if (I.items.isEmpty() || Objects.equals(X, "ε")) return J; // 空项目集或空串
        for (int i = 0; i < I.items.size(); i++) {
            Item item = I.items.get(i);
            if (item.pos < item.right.size()) // 非归约态，加入后继
                if (Objects.equals(item.right.get(item.pos), X)) {
                    J.items.add(new Item(item.left, item.right, item.pos + 1, item.ahead));
                }
        }
        return Closure(J);
    }

    void items() { // 为文法构造LR(1)项集族
        ItemSet I0 = new ItemSet();
        I0.items.add(new Item(prodSet.items.get(0).left, prodSet.items.get(0).right, 0, "$"));
        C.add(Closure(I0));
        boolean newAdded = true;
        while (newAdded) { // 重复直到不再有新的项集加入到C中
            newAdded = false;
            for (int i = 0; i < C.size(); i++) { // C的每一个项集
                ItemSet I = C.get(i);
                for (String x : nonTerminal) { // 每个文法符号x，非终结符
                    ItemSet J = Goto(I, x);
                    if (!J.items.isEmpty() && !C.contains(J)) {
                        C.add(J);
                        newAdded = true; // 设置标志，表示有新的项目集加入C中
                        GOTO.put(new Pair<>(i, x), C.size() - 1);
                    } else if (!J.items.isEmpty() && C.contains(J)) {
                        for (int k = 0; k < C.size(); k++) {
                            if (Objects.equals(C.get(k), J)) {
                                GOTO.put(new Pair<>(i, x), k); // 构造GOTO表
                                break;
                            }
                        }
                    }
                }
                for (String x : terminal) { // 每个文法符号x，终结符
                    if (x.equals("ε")) continue;
                    ItemSet J = Goto(I, x);
                    if (!J.items.isEmpty() && !C.contains(J)) {
                        C.add(J);
                        newAdded = true; // 设置标志，表示有新的项目集加入C中
                        GOTO.put(new Pair<>(i, x), C.size() - 1);
                    } else if (!J.items.isEmpty() && C.contains(J)) {
                        for (int k = 0; k < C.size(); k++) {
                            if (Objects.equals(C.get(k), J)) {
                                GOTO.put(new Pair<>(i, x), k);
                                break;
                            }
                        }
                    }
                }
            }
        }
    }

    void buildTable() { // LR分析表构造算法
        items(); // 得到规范项集族C
        for (int i = 0; i < C.size(); i++) { // 遍历每一个项目集
            ItemSet I = C.get(i);
            for (int j = 0; j < I.items.size(); j++) { // 遍历项目集中的每一个项目
                Item it = I.items.get(j);
                if (it.pos < it.right.size()) { //不是归约态，记住pos的定义
                    String next = it.right.get(it.pos);
                    if (terminal.contains(next)) { // 终结符
                        int jump = GOTO.get(new Pair<>(i, next)); // 从GOTO表中拿出要转移到的的状态
                        ACTION.put(new Pair<>(i, next), new Pair<>(State.SHIFT, jump));
                    } // 非终结符建GOTO表已经在items中实现了
                } else { // 归约态
                    if (!Objects.equals(it.left, "@")) {
                        ACTION.put(new Pair<>(i, it.ahead), new Pair<>(State.REDUCE, prodSet.findProd(it)));
                    } else {
                        ACTION.put(new Pair<>(i, "$"), new Pair<>(State.ACCEPT, -1));
                    }
                }
            }
        }
        // 将所有未定义的项目设置为error
        for (int i = 0; i < C.size(); i++) {
            for (String symbol : terminal) {
                if (!ACTION.containsKey(new Pair<>(i, symbol)))
                    ACTION.put(new Pair<>(i, symbol), new Pair<>(State.ERROR, -1));
            }
        }
    }

    void outputSymbol() {
        System.out.print("terminal: ");
        for (String X : terminal) {
            System.out.print(X + ", ");
        }
        System.out.println();
        System.out.print("nonterminal: ");
        for (String X : nonTerminal)
            System.out.print(X + ", ");
        System.out.println();
    }

    void outputTable() {
        for (int i = 0; i < C.size(); i++) {
            System.out.println("==========================");
            System.out.println("State " + i);
            outputClosure(C.get(i));
        }

        String filePath = "table.csv";
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.append("State,");
            for (String symbol : terminal) { // ACTION表表头
                writer.append(symbol).append(",");
            }
            writer.append("#,");
            for (String symbol : nonTerminal) { // GOTO表表头
                if (Objects.equals(symbol, "@")) continue;
                writer.append(symbol).append(",");
            }
            writer.append("\n");
            for (int i = 0; i < C.size(); i++) {
                writer.append(String.valueOf(i)).append(",");
                for (String symbol : terminal) { // ACTION表内容
                    writer.append(stateStr[ACTION.get(new Pair<>(i, symbol)).getKey().ordinal()]).append(" ").append(String.valueOf(ACTION.get(new Pair<>(i, symbol)).getValue())).append(",");
                }
                writer.append("#,");
                for (String symbol : nonTerminal) {
                    if (Objects.equals(symbol, "@")) continue;
                    writer.append(String.valueOf(GOTO.get(new Pair<>(i, symbol)))).append(",");
                }
                writer.append("\n");
            }
            writer.flush();
            writer.close();
            System.out.println("LR分析表已成功导出到 " + filePath);
        } catch (Exception e) {
            System.err.println("导出LR分析表时出错：" + e.getMessage());
        }
    }

    void outputClosure(ItemSet I) {
        for (Item i : I.items) {
            System.out.println(i);
        }
    }

    Set<Item> getRightSet(String left) {
        // 找到并返回所有左侧为left的产生式
        Set<Item> set = new HashSet<>();
        for (Item p : prodSet.items) {
            if (Objects.equals(p.left, left)) set.add(p);
        }
        return set;
    }

    public void outputStatusStack() {
        Deque<Integer> stack = new ArrayDeque<>(statusStack);
        System.out.print("statusStack: ");
        while (!stack.isEmpty()) {
            System.out.print(stack.pollFirst() + " ");
        }
        System.out.println();
    }

    public void outputSymbolStack() {
        Deque<String> stack = new ArrayDeque<>(symbolStack);
        System.out.print("symbolStack: ");
        while (!stack.isEmpty()) {
            System.out.print(stack.pollFirst() + " ");
        }
        System.out.println();
    }

    public void parser() {
        boolean accepted = false;
        while (!accepted) {
            outputStatusStack(); // 输出状态栈
            outputSymbolStack(); // 输出符号栈
            lexer.outputTokenQueue(); // 输出token流

            // 获得 栈顶状态 和 输入token 的配对
            Token token = lexer.tokenQueue.peek();
            Pair<Integer, String> p;

            if (token != null && token.tag == Tag.ID) { // 标识符
                p = new Pair<>(statusStack.peekLast(), "id");
            } else if (token != null && token.tag == Tag.NUMBER) {
                p = new Pair<>(statusStack.peekLast(), "digits");
            } else if (token != null) {
                p = new Pair<>(statusStack.peekLast(), token.value);
            } else {
                break;
            }

//            if (token != null) {
//                p = new Pair<>(statusStack.peekLast(), token.value);
//            } else break;

            System.out.println(p.getKey() + "," + p.getValue());
            Pair<State, Integer> action = ACTION.get(p);
            System.out.println(action.getKey() + "," + action.getValue());
            if (action.getKey() == State.ERROR)
                break;
            else if (action.getKey() == State.SHIFT) { // 移入
                statusStack.addLast(action.getValue());
                symbolStack.addLast(p.getValue());
                lexer.tokenQueue.poll();
            } else if (action.getKey() == State.REDUCE) { // 归约
                Item prod = prodSet.items.get(action.getValue()); // 获得用于归约的产生式
                System.out.println(prod);
                if (Objects.equals(prod.right.get(0), "ε")) { // 对于空串的归约
//                    symbolStack.addLast(prod.left);
//                    continue;
                }
                else {
                    for (int i = 0; i < prod.right.size(); i++) {
                        statusStack.pollLast();
                        symbolStack.pollLast();
                    }
                }
                symbolStack.addLast(prod.left);
                if (GOTO.get(new Pair<>(statusStack.peekLast(), symbolStack.peekLast())) != null) {
                    statusStack.addLast(GOTO.get(new Pair<>(statusStack.peekLast(), symbolStack.peekLast())));
                }
            } else if (action.getKey() == State.ACCEPT) {
                accepted = true;
            }
        }
        if (!accepted)
            System.out.println("Error!");
        else System.out.println("Accept!");
    }
}
