package parser;

import javafx.util.Pair;
import lexer.Lexer;
import lexer.Tag;
import lexer.Token;
import semantic.Analyzer;
import semantic.Node;

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
    List<ItemSet> C = new ArrayList<>(); // 项目集族
    Map<Pair<Integer, String>, Integer> GOTO = new HashMap<>(); // GOTO表

    enum State {
        SHIFT, REDUCE, ACCEPT, ERROR
    }

    String[] stateStr = new String[]{"SHIFT", "REDUCE", "ACCEPT", "ERROR"};
    Map<Pair<Integer, String>, Pair<State, Integer>> ACTION = new HashMap<>(); // Action表

    Deque<Integer> statusStack = new ArrayDeque<>(); // 状态栈
    Deque<Node> symbolStack = new ArrayDeque<>(); // 符号栈
    Lexer lexer = new Lexer();

    public void init() throws IOException {
        lexer.run();
        readFile();
        for (Item p : prodSet.items) {
            if (Objects.equals(p.right.get(0), "ε")) {
                emptyNonTerminal.add(p.left);
            }
        }
        buildTable();
        outputTable();
        statusStack.push(0);
        symbolStack.push(new Node("$"));
        parser();
//        outputC();
    }

    public void readFile() throws IOException {
        String filePath = "grammar.txt";
        BufferedReader br = new BufferedReader(new FileReader(filePath));
        String tmp;
        while ((tmp = br.readLine()) != null) {
            prodSet.addProd(tmp);
        }
//        prodSet.items.add(0, new Item("@", Collections.singletonList(prodSet.items.get(0).left))); // 增广文法
//        nonTerminal.add("@");
        terminal.add("$");
        br.close();
    }


    Set<String> first(List<String> strList) { // 动态获取first集
        Set<String> firstSet = new HashSet<>();
        if (strList.isEmpty()) {
            firstSet.add("ε"); // @ = ε
            return firstSet;
        } else if (strList.size() == 1) {
            if (terminal.contains(strList.get(0)) || Objects.equals(strList.get(0), "$")) { // # = $
                // 终结符
                firstSet.add(strList.get(0));
                return firstSet;
            } else { // 非终结符
                // 获得左侧为第一个非终结符的所有产生式 X ->
                Set<Item> Prods = getRightSet(strList.get(0));
                for (Item p : Prods) {
                    int pos = p.findLeft();
                    if (pos != -1) { // X -> aXb
                        if (pos == 0) continue; // X -> Xb
                        else { // X -> aXb
                            List<String> a = p.right.subList(0, pos);
                            Set<String> set = first(a);
                            if (set.size() == 1 && set.contains("ε")) continue;
                        }
                    }
                    Set<String> set = new HashSet<>(first(p.right));
                    firstSet.addAll(set);
                }
                return firstSet;
            }
        } else { // first(X1X2X3)，逐个求first(Xi)
            for (int i = 0; i < strList.size(); i++) {
                List<String> temp = new ArrayList<>();
                temp.add(strList.get(i)); // 获得单个元素
                Set<String> set = first(temp);
                if (set.contains("ε") && i != strList.size() - 1) {
                    set.remove("ε");
                    firstSet.addAll(set);
                } else { // 无"ε"或最后一个了
                    firstSet.addAll(set);
                    break;
                }
            }
            return firstSet;
        }
    }

    public ItemSet Closure(ItemSet I) {
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
            List<String> b = new ArrayList<>(item.right.subList(item.pos + 1, item.right.size()));
            b.add(item.ahead);
            Set<String> aheads = new HashSet<>(first(b)); // 展望符集
            for (Item it : set) { // 遍历每个产生式和展望符的组合
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
        I0.seg = 0;
        I0.items.add(new Item(prodSet.items.get(0).left, prodSet.items.get(0).right, 0, "$"));
        C.add(Closure(I0));
        boolean newAdded = true;
        while (newAdded) { // 重复直到不再有新的项集加入到C中
            newAdded = false;
            for (int i = 0; i < C.size(); i++) { // C的每一个项集
                ItemSet I = C.get(i);
                for (String x : symbols) { // 每个文法符号x，非终结符
                    ItemSet J = Goto(I, x);
                    if (!J.items.isEmpty()) {
                        int k = 0;
                        for (; k < C.size(); k++) {
                            if (Objects.equals(C.get(k), J)) {
                                break;
                            }
                        }
                        if (k != C.size()) { // 找到
                            GOTO.put(new Pair<>(i, x), k);
                        } else { // 没找到
                            J.seg = C.size();
                            J.from = I.seg;
                            C.add(J);
                            newAdded = true; // 设置标志，表示有新的项目集加入C中
                            GOTO.put(new Pair<>(i, x), C.size() - 1);
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
                    if (terminal.contains(next) && GOTO.containsKey(new Pair<>(i, next))) { // 终结符
                        int jump = GOTO.get(new Pair<>(i, next)); // 从GOTO表中拿出要转移到的的状态
                        ACTION.put(new Pair<>(i, next), new Pair<>(State.SHIFT, jump));
                    } // 非终结符建GOTO表已经在items中实现了
                } else { // 归约态
                    if (!Objects.equals(it.left, "P")) {
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

    void outputC() {
        for (int i = 0; i < C.size(); i++) {
            System.out.println("==========================");
            System.out.println("State " + i);
            outputClosure(C.get(i));
        }
    }

    void outputTable() {
        String filePath = "table.csv";
        try {
            FileWriter writer = new FileWriter(filePath);
            writer.append("State,");
            for (String symbol : terminal) { // ACTION表表头
                writer.append(symbol).append(",");
            }
            writer.append("#,");
            for (String symbol : nonTerminal) { // GOTO表表头
                if (Objects.equals(symbol, "P")) continue;
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
                    if (Objects.equals(symbol, "P")) continue;
                    writer.append(String.valueOf(GOTO.get(new Pair<>(i, symbol)))).append(",");
                }
                writer.append("\n");
            }
            writer.flush();
            writer.close();
//            System.out.println("LR分析表已成功导出到 " + filePath);
        } catch (Exception e) {
            System.err.println("导出LR分析表时出错：" + e.getMessage());
        }
    }

    void outputClosure(ItemSet I) {
        System.out.println("seg: " + I.seg);
        System.out.println("from: " + I.from);
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
        Deque<Node> stack = new ArrayDeque<>(symbolStack);
        System.out.print("symbolStack: ");
        while (!stack.isEmpty()) {
            System.out.print(stack.pollFirst().symbol + " ");
        }
        System.out.println();
    }

    public void parser() {
        boolean accepted = false;
        while (!accepted) {
//            outputStatusStack(); // 输出状态栈
//            outputSymbolStack(); // 输出符号栈
//            lexer.outputTokenQueue(); // 输出token流

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

//            System.out.println(p.getKey() + "," + p.getValue());
            Pair<State, Integer> action = ACTION.get(p);
//            System.out.println(action.getKey() + "," + action.getValue());
            if (action.getKey() == State.ERROR) // 出错
                break;
            else if (action.getKey() == State.SHIFT) { // 移入
                statusStack.addLast(action.getValue());
                symbolStack.addLast(new Node(token));
                lexer.tokenQueue.poll();
            } else if (action.getKey() == State.REDUCE) { // 归约
                Item prod = prodSet.items.get(action.getValue()); // 获得用于归约的产生式
                List<Node> nodes = new ArrayList<>();
                if (!Objects.equals(prod.right.get(0), "ε")) {
                    for (int i = 0; i < prod.right.size(); i++) {
                        statusStack.pollLast();
                        nodes.add(0, symbolStack.pollLast());
                    }
                }
                symbolStack.addLast(new Node(prod.left)); // 空集直接进栈，不出栈
                if (GOTO.get(new Pair<>(statusStack.peekLast(), symbolStack.peekLast().symbol)) != null) {
                    statusStack.addLast(GOTO.get(new Pair<>(statusStack.peekLast(), symbolStack.peekLast().symbol)));
                }

                // 规约完成，根据用于规约的产生式触发语义动作
                switch (action.getValue()) {
                    case 6: // S -> if ( C ) M S
                        Analyzer.backPatch(nodes.get(2).trueList, nodes.get(4).instr);
                        symbolStack.peekLast().nextList = Analyzer.merge(nodes.get(2).falseList, nodes.get(5).nextList);
                        break;
                    case 7: // S -> if ( C ) M S N else M S
                        Analyzer.backPatch(nodes.get(2).trueList, nodes.get(4).instr);
                        Analyzer.backPatch(nodes.get(2).falseList, nodes.get(8).instr);
                        Set<Integer> temp = Analyzer.merge(nodes.get(5).nextList, nodes.get(6).nextList);
                        symbolStack.peekLast().nextList = Analyzer.merge(temp, nodes.get(9).nextList);
                        break;
                    case 8: // S -> while M ( C ) M S
                        Analyzer.backPatch(nodes.get(6).nextList, nodes.get(1).instr);
                        Analyzer.backPatch(nodes.get(3).trueList, nodes.get(5).instr);
                        symbolStack.peekLast().nextList = nodes.get(3).falseList;
                        Analyzer.gen("goto " + nodes.get(1).instr);
                        break;
                    case 9: // S -> S M S
                        Analyzer.backPatch(nodes.get(0).nextList, nodes.get(1).instr);
                        symbolStack.peekLast().nextList = nodes.get(2).nextList;
                        break;
                    case 22: // M -> ε
                        symbolStack.peekLast().instr = Analyzer.nextInstr();
                        break;
                    case 23: // N -> ε
                        symbolStack.peekLast().nextList = Analyzer.makeList(Analyzer.nextInstr());
                        Analyzer.gen("goto ");
                        break;

                    case 10: // C -> E > E
                        symbolStack.peekLast().trueList = Analyzer.makeList(Analyzer.nextInstr());
                        symbolStack.peekLast().falseList = Analyzer.makeList(Analyzer.nextInstr() + 1);
                        Analyzer.gen("if " + nodes.get(0).lexeme + ">" + nodes.get(2).lexeme + " goto ");
                        Analyzer.gen("goto ");
                        break;
                    case 11: // C -> E < E
                        symbolStack.peekLast().trueList = Analyzer.makeList(Analyzer.nextInstr());
                        symbolStack.peekLast().falseList = Analyzer.makeList(Analyzer.nextInstr() + 1);
                        Analyzer.gen("if " + nodes.get(0).lexeme + "<" + nodes.get(2).lexeme + " goto ");
                        Analyzer.gen("goto ");
                        break;
                    case 12: // C -> E == E
                        symbolStack.peekLast().trueList = Analyzer.makeList(Analyzer.nextInstr());
                        symbolStack.peekLast().falseList = Analyzer.makeList(Analyzer.nextInstr() + 1);
                        Analyzer.gen("if " + nodes.get(0).lexeme + "==" + nodes.get(2).lexeme + " goto ");
                        Analyzer.gen("goto ");
                        break;

                    case 20: // F -> id 字面量综合属性丢上去
                        symbolStack.peekLast().lexeme = nodes.get(0).lexeme;
                        break;
                    case 21: // F -> digits
                        symbolStack.peekLast().lexeme = nodes.get(0).lexeme;
                        break;
                    case 16: // T -> F
                        symbolStack.peekLast().lexeme = nodes.get(0).lexeme;
                        break;
                    case 15: // E -> T
                        symbolStack.peekLast().lexeme = nodes.get(0).lexeme;
                        break;
                    case 3: // L -> int
                        symbolStack.peekLast().lexeme = nodes.get(0).lexeme;
                        break;
                    case 4: // L -> float
                        symbolStack.peekLast().lexeme = nodes.get(0).lexeme;
                        break;

                    case 14: // E -> E - T
                        symbolStack.peekLast().lexeme = nodes.get(0).lexeme + "-" + nodes.get(2).lexeme;
                        break;
                    case 13: // E -> E + T
                        symbolStack.peekLast().lexeme = nodes.get(0).lexeme + "+" + nodes.get(2).lexeme;
                        break;

                    case 5: // S -> id = E ;
                        Analyzer.gen(nodes.get(0).lexeme + "=" + nodes.get(2).lexeme);
                        break;
                    case 1: // D -> L id ; D
//                        Analyzer.gen(nodes.get(0).lexeme+" "+nodes.get(1).lexeme);
                        Analyzer.symTable.put(nodes.get(1).lexeme, nodes.get(0).lexeme);

                    default:
                        break;
                }

            } else if (action.getKey() == State.ACCEPT) {
                accepted = true;
            }
        }
        System.out.println("================语法分析结果===============");
        if (!accepted)
            System.out.println("Error! 语法分析失败!");
        else System.out.println("Accept! 语法分析成功!");
    }
}
