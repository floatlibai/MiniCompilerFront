package semantic;

import java.util.*;

public class Analyzer {
    public static List<String> instructions = new ArrayList<>(); // 生成的指令集合
    public static Map<String,String> symTable = new HashMap<>(); // 符号表 <id_name, int or float>
    public static Set<Integer> makeList(int i) { // 创建一个只包含i的列表
        Set<Integer> set = new HashSet<>();
        set.add(i);
        return set;
    }
    public static Set<Integer> merge(Set<Integer> p1, Set<Integer> p2) { // 将p1和p2指向的列表进行合并
        Set<Integer> set = new HashSet<>();
        set.addAll(p1);
        set.addAll(p2);
        return set;
    }
    public static void backPatch(Set<Integer> p, int i) { // 将i作为目标标号插入到p所指列表中的各指令中
        for(int j:p) {
            instructions.set(j, instructions.get(j) + i);
        }
    }

    public static void gen(String str) {
        instructions.add(str);
    }

    public static int nextInstr() {
        return instructions.size();
    }

    public static void outputInstructions() {
        System.out.println("================三地址代码=================");
        for(int i=0;i<instructions.size();i++) {
            System.out.println(i+": "+instructions.get(i));
        }
    }

    public static void outputSymbolTable() {
        System.out.println("================符号表=====================");
        System.out.println(symTable);
    }
}