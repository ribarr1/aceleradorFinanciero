package com.greensqa.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.greensqa.model.Condition;
import com.greensqa.model.Op;
import java.time.Period;
import java.util.List;
import java.util.Locale;

public class Evaluators {

    public static boolean test(JsonNode item, JsonNode report, Condition c) {
        System.out.println("\n🔍 EVALUANDO CONDICIÓN:");
        System.out.println("   Variable: " + c.leftVar);
        System.out.println("   Operación: " + c.op);
        System.out.println("   Valores esperados: " + c.values);

        // Para condiciones de fecha, usar el report completo para buscar consultDate
        if (c.op == Op.DATE_DIFF_LT || c.op == Op.DATE_DIFF_GT || c.op == Op.DATE_DIFF_EQ) {
            return dateDiff(item, report, c);
        }
        // Obtener valor REAL del JSON
        Object actualValue = FieldResolver.get(item, c.leftVar);
        System.out.println("   Valor actual en JSON: " + actualValue);

        return switch (c.op) {
            case EQ   -> eq(item, c.leftVar, c.values.get(0));
            case NEQ  -> neq(item, c.leftVar, c.values.get(0));
            case IN   -> in(item, c.leftVar, c.values);
            case NIN  -> nin(item, c.leftVar, c.values);
            case DATE_DIFF_LT, DATE_DIFF_GT, DATE_DIFF_EQ -> dateDiff(item, report, c);
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

    private static boolean dateDiff(JsonNode item, JsonNode report, Condition c) {
        try {
            String[] dateFields = c.leftVar.split("\\|");
            var date1 = FieldResolver.getDate(report, dateFields[0]);
            var date2 = FieldResolver.getDate(item, dateFields[1]);

            if (date1 == null || date2 == null) {
                System.out.println("❌ No se pudieron obtener las fechas: " + dateFields[0] + ", " + dateFields[1]);
                return false;
            }

            int months = Math.abs(Period.between(date2, date1).getYears() * 12 +
                    Period.between(date2, date1).getMonths());
            int threshold = Integer.parseInt(c.values.get(0));

          //  System.out.println("📅 Diferencia de fechas:");
          //  System.out.println("   " + dateFields[0] + ": " + date1);
          //  System.out.println("   " + dateFields[1] + ": " + date2);
          //  System.out.println("   Diferencia en meses: " + months);
          //  System.out.println("   Umbral: " + threshold + " meses");

            boolean result = switch (c.op) {
                case DATE_DIFF_LT -> months < threshold;
                case DATE_DIFF_GT -> months > threshold;
                default -> months == threshold;
            };

            System.out.println("   Resultado: " + result);
            return result;

        } catch (Exception e) {
            System.err.println("❌ Error en dateDiff: " + e.getMessage());
            return false;
        }
    }

    private static String normalize(Object o) {
        String s = String.valueOf(o);
        return s.trim().toLowerCase(Locale.ROOT);
    }
}
