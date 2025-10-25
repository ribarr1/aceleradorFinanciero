package com.greensqa.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.greensqa.model.Condition;

import java.time.Period;
import java.util.List;
import java.util.Locale;

public class Evaluators {

    public static boolean test(JsonNode item, Condition c) {
        return switch (c.op) {
            case EQ   -> eq(item, c.leftVar, c.values.get(0));
            case NEQ  -> neq(item, c.leftVar, c.values.get(0));
            case IN   -> in(item, c.leftVar, c.values);
            case NIN  -> nin(item, c.leftVar, c.values);
            case DATE_DIFF_LT, DATE_DIFF_GT, DATE_DIFF_EQ -> dateDiff(item, c);
        };
    }

    private static boolean eq(JsonNode item, String var, String expected) {
        Object got = FieldResolver.get(item, var);
        if (got == null) return false;
        return normalize(got).equalsIgnoreCase(expected.trim());
    }

    private static boolean neq(JsonNode item, String var, String val) {
        Object got = FieldResolver.get(item, var);
        if (got == null) return true;
        return !normalize(got).equalsIgnoreCase(val.trim());
    }

    private static boolean in(JsonNode item, String var, List<String> vals) {
        Object got = FieldResolver.get(item, var);
        if (got == null) return false;
        String s = normalize(got);

        // Caso especial: businessBureauEventDesc implícito
        if ("businessBureauEventDesc".equals(var)) {
            // Verificar si coincide con alguna descripción implícita
            for (String val : vals) {
                if (s.contains(normalize(val))) {
                    return true;
                }
            }
        }

        return vals.stream().anyMatch(v -> s.equalsIgnoreCase(v.trim()));
    }

    private static boolean nin(JsonNode item, String var, List<String> vals) {
        Object got = FieldResolver.get(item, var);
        if (got == null) return true;
        String s = normalize(got);
        return vals.stream().noneMatch(v -> s.equalsIgnoreCase(v.trim()));
    }

    private static boolean dateDiff(JsonNode item, Condition c) {
        String[] lr = c.leftVar.split("\\|");
        var d1 = FieldResolver.getDate(item, lr[0]);
        var d2 = FieldResolver.getDate(item, lr[1]);
        if (d1 == null || d2 == null) return false;
        int months = Math.abs(Period.between(d2, d1).getYears()*12 + Period.between(d2, d1).getMonths());
        int threshold = Integer.parseInt(c.values.get(0));
        return switch (c.op) {
            case DATE_DIFF_LT -> months < threshold;
            case DATE_DIFF_GT -> months > threshold;
            default           -> months == threshold;
        };
    }

    private static String normalize(Object o) {
        String s = String.valueOf(o);
        return s.trim().toLowerCase(Locale.ROOT);
    }
}
