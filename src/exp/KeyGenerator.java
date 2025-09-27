package exp;

import java.util.*;

import entity.Key;

public class KeyGenerator {

    // 生成所有大小为 keySize 的属性组合
    public static List<Key> generateMinimalKeys(List<String> schema, int keySize, double ratio) {
        if (schema == null || schema.isEmpty() || keySize <= 0 || keySize > schema.size()) {
            throw new IllegalArgumentException("Invalid schema or key size");
        }

        List<Key> allKeys = new ArrayList<>();
        combine(schema, keySize, 0, new LinkedHashSet<>(), allKeys);

        // 打乱顺序
        Collections.shuffle(allKeys);

        // 根据 ratio 取子集
        int numKeysToReturn = (int) Math.ceil(allKeys.size() * ratio);
        return allKeys.subList(0, Math.min(numKeysToReturn, allKeys.size()));
    }

    // 递归生成组合
    private static void combine(List<String> schema, int keySize, int start, Set<String> current, List<Key> result) {
        if (current.size() == keySize) {
            result.add(new Key(new LinkedHashSet<>(current)));
            return;
        }
        for (int i = start; i < schema.size(); i++) {
            current.add(schema.get(i));
            combine(schema, keySize, i + 1, current, result);
            current.remove(schema.get(i));
        }
    }

    // 测试
    public static void main(String[] args) {
        List<String> schema = Arrays.asList("A", "B", "C", "D", "E");
        int keySize = 4;
        double ratio = 1;

        List<Key> minimalKeys = generateMinimalKeys(schema, keySize, ratio);

        for (int i = 0;i < minimalKeys.size();i ++) {
            System.out.println(i + " : " + minimalKeys.get(i).toString());
        }
    }
}

