package io.github.minehollow.minecraft.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public record MenuLayout(Map<Character, int[]> layout, int rows) {
    public static MenuLayout of(String... layout) {
        if (layout.length < 1 || layout.length > 6) {
            throw new IllegalArgumentException("Layout must have between 1 and 6 rows.");
        }
        for (String row : layout) {
            if (row.length() != 9) {
                throw new IllegalArgumentException("Each row must have exactly 9 characters.");
            }
        }
        
        Map<Character, List<Integer>> parsed = new HashMap<>();
        for (int row = 0; row < layout.length; row++) {
            for (int col = 0; col < 9; col++) {
                char c = layout[row].charAt(col);
                parsed.computeIfAbsent(c, k -> new ArrayList<>()).add(row * 9 + col);
            }
        }
        
        Map<Character, int[]> layoutMap = new HashMap<>();
        parsed.forEach((key, value) -> layoutMap.put(key, value.stream().mapToInt(Integer::intValue).toArray()));
        
        return new MenuLayout(layoutMap, layout.length);
    }
    
    public int getSingle(char c) {
        int[] positions = this.layout.get(c);
        if (positions == null || positions.length != 1) {
            throw new IllegalArgumentException("Character '" + c + "' does not map to exactly one position.");
        }
        return positions[0];
    }
    public int[] get(char c) {
        return this.layout.getOrDefault(c, new int[0]);
    }
}
