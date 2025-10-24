package com.greensqa.model;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CaseDef {
    public String id;
    public String expectedVar;
    public String groupsRaw;         // ej. "liabilities o creditCard"
    public String expectedExpr;      // opcional: "VAR=valor" si quieres forzar
    public Integer expectedConst;
    public final List<Condition> conditions = new ArrayList<>();

    public List<String> groups() {
        if (groupsRaw == null) return List.of();
        String[] parts = groupsRaw.split("\\s*(?:o|y|,)\\s+"); // admite "o", "y" o ","
        return Arrays.stream(parts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }



}
