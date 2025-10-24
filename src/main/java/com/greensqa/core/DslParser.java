package com.greensqa.core;

import com.greensqa.model.Condition;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class DslParser {

    public static List<Condition> parseCell(String header, String cell) {
        List<Condition> out = new ArrayList<>();
        if (cell == null || cell.isBlank()) return out;

        String s = cell.trim();

        // Expresiones fecha:  fecha1 - fecha2 < 12 meses
        if (s.matches("(?i).+\\s-\\s.+\\s*[<>=]\\s*\\d+\\s+meses?")) {
            out.add(parseDateDiff(header, s));
            return out;
        }

        // Combinaciones con "y" (AND) en una misma celda:
        // ej: "<> 009... y = 0086... o 0086.."
        String[] andParts = s.split("(?i)\\s+y\\s+");
        for (String part : andParts) {
            out.addAll(parseSimple(header, part.trim()));
        }
        return out;
    }

    private static List<Condition> parseSimple(String var, String s) {
        List<Condition> out = new ArrayList<>();

        // "= v1 o v2 ..."
        if (s.startsWith("=")) {
            String rhs = s.substring(1).trim();
            List<String> vals = splitOr(rhs);
            Condition c = new Condition();
            c.leftVar = var;
            c.op = vals.size() == 1 ? Condition.Op.EQ : Condition.Op.IN;
            c.values = vals;
            out.add(c);

            // regla implícita: si businessBureauEvent tiene valor, validaremos desc
            if ("businessBureauEvent".equals(var) && vals.size()==1) {
                Condition c2 = new Condition();
                c2.leftVar = "businessBureauEventDesc";
                c2.op = Condition.Op.IN; // se validará por imply en Evaluators
                c2.values = vals; // pasamos code y Evaluators verificará imply(desc)
                out.add(c2);
            }
            return out;
        }

        // "<> v1 o v2 ..."
        if (s.startsWith("<>")) {
            String rhs = s.substring(2).trim();
            List<String> vals = splitOr(rhs);
            Condition c = new Condition();
            c.leftVar = var;
            c.op = vals.size() == 1 ? Condition.Op.NEQ : Condition.Op.NIN;
            c.values = vals;
            out.add(c);
            return out;
        }

        // Soportar formato "var = valor" cuando header es "expected" (aunque aquí no usamos)
        // Para condiciones normales ya cubrimos.

        return out;
    }

    private static Condition parseDateDiff(String header, String s) {
        // header puede ser "regla_fecha" (no usamos); el nombre de variables está en la expresión
        // "fecha1 - fecha2 < 12 meses"
        String expr = s.replaceAll("(?i)meses?", "meses").trim();
        String[] sides = expr.split("[<>=]");
        String lhs = sides[0].trim(); // "fecha1 - fecha2"
        String rhs = expr.substring(lhs.length()).trim(); // "< 12 meses"

        String[] parts = lhs.split("\\s-\\s");
        String f1 = parts[0].trim();
        String f2 = parts[1].trim();

        String opStr = expr.substring(lhs.length(), expr.indexOf(" ", lhs.length())).trim(); // < ó > ó =
        String numPart = rhs.replaceAll("[^0-9]", "");
        int months = Integer.parseInt(numPart);

        Condition c = new Condition();
        c.leftVar = f1 + "|" + f2; // las dos fechas separadas por |
        c.values = List.of(String.valueOf(months), "meses");
        switch (opStr) {
            case "<" -> c.op = Condition.Op.DATE_DIFF_LT;
            case ">" -> c.op = Condition.Op.DATE_DIFF_GT;
            default  -> c.op = Condition.Op.DATE_DIFF_EQ;
        }
        return c;
    }

    private static List<String> splitOr(String rhs) {
        return Arrays.stream(rhs.split("(?i)\\s+o\\s+"))
                .map(String::trim).filter(s -> !s.isBlank()).toList();
    }
}
