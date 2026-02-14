package com.greensqa.core;

import com.greensqa.model.Condition;
import com.greensqa.model.Op;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.regex.Matcher;

/*public class DslParser {

    // Mapeo de eventos a descripciones implícitas
    private static final Map<String, String> EVENT_DESC_MAP = Map.of(
            "45", "Dudoso recaudo",
            "46", "Cartera recuperada",
            "47", "Cartera castigada"
            // Agregar más mapeos según necesidad
    );

    public static List<Condition> parseCell(String col, String val) {

        if (val == null || val.trim().isEmpty()) {
            return List.of();
        }

        String trimmedVal = val.trim();
        System.out.println("Parsing: '" + col + "' = '" + trimmedVal + "'");

        // Manejar condiciones compuestas con "y" (AND)
        if (trimmedVal.contains(" y ") && trimmedVal.contains("(")) {
            return parseCompoundCondition(trimmedVal);
        }

        // Manejar condiciones simples con "o" (OR)
        if (trimmedVal.contains(" o ")) {
            return parseOrCondition(trimmedVal);
        }

        // Condición simple
        return List.of(parseSimpleCondition(trimmedVal));
    }

    private static List<Condition> parseCompoundCondition(String expression) {
        List<Condition> conditions = new ArrayList<>();

        try {
            // Ejemplo: "personIdNumber <> 009004061505 y (personIdNumber = 00860034594 o 00860003020)"
            String[] parts = expression.split(" y ");

            for (String part : parts) {
                part = part.trim().replace("(", "").replace(")", "");

                if (part.contains(" o ")) {
                    // Es una condición OR
                    conditions.addAll(parseOrCondition(part));
                } else {
                    // Es una condición simple
                    conditions.add(parseSimpleCondition(part));
                }
            }
        } catch (Exception e) {
            System.err.println("Error parsing compound condition: " + expression);
            e.printStackTrace();
        }

        return conditions;
    }

    /*private static List<Condition> parseOrCondition(String expression) {
        List<Condition> conditions = new ArrayList<>();
        String[] orParts = expression.split(" o ");

        // Si es una condición con "variable = valor1 o valor2"
        if (orParts[0].contains("=")) {
            String[] leftRight = orParts[0].split("=");
            String variable = leftRight[0].trim();

            Condition cond = new Condition();
            cond.leftVar = variable;
            cond.op = Op.IN;  // Usar IN para múltiples valores
            cond.values = new ArrayList<>();

            // Agregar todos los valores (de todas las partes)
            for (String part : orParts) {
                String value = part.trim();
                // Si la parte contiene "=", extraer solo el valor derecho
                if (value.contains("=")) {
                    String[] lr = value.split("=");
                    value = lr[1].trim();
                }
                cond.values.add(value);
            }

            conditions.add(cond);
        } else {
            // Caso por defecto (mantener comportamiento actual)
            for (String part : orParts) {
                part = part.trim();
                conditions.add(parseSimpleCondition(part));
            }
        }

        return conditions;
    }*/


/*

    private static List<Condition> parseOrCondition(String expression) {
        List<Condition> conditions = new ArrayList<>();

        // Split tolerante: " o " con espacios variables y case-insensitive
        String[] orParts = expression.split("(?i)\\s+o\\s+");
        if (orParts.length == 0) return conditions;

        String first = orParts[0];

        boolean isEq  = first.contains("=");
        boolean isNeq = first.contains("<>");

        // ----- CASO 1: variable = valor1 o valor2  -> IN [v1,v2]
        if (isEq && !isNeq) {
            String[] leftRight = first.split("=", 2);
            String variable = leftRight[0].trim();

            Condition cond = new Condition();
            cond.leftVar = variable;
            cond.op = Op.IN;
            cond.values = new ArrayList<>();

            for (String part : orParts) {
                String value = part.trim();
                if (value.contains("=")) value = value.split("=", 2)[1].trim();
                cond.values.add(cleanValue(value));
            }

            conditions.add(cond);
            return conditions;
        }

        // ----- CASO 2: variable <> valor1 o valor2 -> NIN [v1,v2]
        if (isNeq) {
            String[] leftRight = first.split("<>", 2);
            String variable = leftRight[0].trim();

            Condition cond = new Condition();
            cond.leftVar = variable;
            cond.op = Op.NIN;
            cond.values = new ArrayList<>();

            for (String part : orParts) {
                String value = part.trim();
                if (value.contains("<>")) value = value.split("<>", 2)[1].trim();
                cond.values.add(cleanValue(value));
            }

            conditions.add(cond);
            return conditions;
        }

        // ----- Por defecto: cada parte se parsea individual
        for (String part : orParts) {
            part = part.trim();
            conditions.add(parseSimpleCondition(part));
        }
        return conditions;
    }

    private static String cleanValue(String v) {
        if (v == null) return "";
        v = v.trim();

        // Quitar paréntesis alrededor si vienen
        while (v.startsWith("(") && v.endsWith(")") && v.length() > 1) {
            v = v.substring(1, v.length() - 1).trim();
        }

        // Excel duplica comillas: ""texto"" -> "texto"
        v = v.replace("\"\"", "\"").trim();

        // Quitar comillas externas
        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v;
    }
    /****************************************************************************************/
/*
    private static Condition parseSimpleCondition(String expression) {
        Condition cond = new Condition();

        // Detectar operadores
        // Detectar condiciones de fecha primero
        if (expression.contains(" - ") && expression.contains(" meses")) {
            return parseDateCondition(expression);
        }
        if (expression.contains("<>")) {
            String[] parts = expression.split("<>");
            cond.leftVar = parts[0].trim();
            cond.op = Op.NEQ;
            cond.values = parseValues(parts[1].trim());
        } else if (expression.contains("=")) {
            String[] parts = expression.split("=");
            cond.leftVar = parts[0].trim();

            // Verificar si es una condición de fecha
            if (cond.leftVar.contains("|")) {
                cond.op = Op.DATE_DIFF_LT; // Por defecto, se puede ajustar después
            } else {
                cond.op = Op.EQ;
            }

            cond.values = parseValues(parts[1].trim());
        } else {
            // Valor por defecto para debugging
            cond.leftVar = expression;
            cond.op = Op.EQ;
            cond.values = List.of("true");
        }

        // Agregar descripción implícita si es un businessBureauEvent
        if ("businessBureauEvent".equals(cond.leftVar) && cond.op == Op.EQ) {
            addImplicitDescription(cond);
        }

        return cond;
    }

    private static Condition parseDateCondition(String expression) {
        Condition cond = new Condition();

        try {
            // Ejemplo: "consultDate - paymentDate < 12 meses"
            String[] parts = expression.split(" ");

            // Extraer las fechas (partes 0 y 2)
            String date1 = parts[0].trim();
            String date2 = parts[2].trim();
            cond.leftVar = date1 + "|" + date2;  // Formato: "consultDate|paymentDate"

            // Extraer operador y valor
            String operator = parts[3].trim(); // "<", ">", "="
            String valueWithMeses = parts[4].trim();
            String value = valueWithMeses.replace("meses", "").trim();

            // Asignar operador correcto
            switch (operator) {
                case "<": cond.op = Op.DATE_DIFF_LT; break;
                case ">": cond.op = Op.DATE_DIFF_GT; break;
                case "=": cond.op = Op.DATE_DIFF_EQ; break;
                default: cond.op = Op.DATE_DIFF_LT;
            }

            cond.values = List.of(value);

        } catch (Exception e) {
            System.err.println("Error parsing date condition: " + expression);
            // Fallback a condición básica
            cond.leftVar = expression;
            cond.op = Op.EQ;
            cond.values = List.of("true");
        }

        return cond;
    }

    private static List<String> parseValues(String valueStr) {
        // Limpiar y dividir valores múltiples
        return Arrays.stream(valueStr.split(" o "))
                .map(String::trim)
                .collect(Collectors.toList());
    }

    private static void addImplicitDescription(Condition eventCond) {
        // Para cada valor de evento, agregar la descripción implícita correspondiente
        List<String> implicitDescriptions = new ArrayList<>();

        for (String eventCode : eventCond.values) {
            String description = EVENT_DESC_MAP.get(eventCode.trim());
            if (description != null) {
                implicitDescriptions.add(description);
            }
        }

        // Si encontramos descripciones implícitas, las almacenamos para uso posterior
        if (!implicitDescriptions.isEmpty()) {
            System.out.println("Descripciones implícitas para " + eventCond.values + ": " + implicitDescriptions);
            // Esto se usará en Evaluators para validar automáticamente
        }
    }
}*/




public class DslParser {

    // Mapeo de eventos a descripciones implícitas
    private static final Map<String, String> EVENT_DESC_MAP = Map.of(
            "45", "Dudoso recaudo",
            "46", "Cartera recuperada",
            "47", "Cartera castigada"
    );

    // --- Regex fechas ---
    // Ej: consultDate - paymentDate <= 12
    private static final Pattern DATE_DIFF_LIMIT = Pattern.compile(
            "\\s*([\\w\\.]+)\\s*-\\s*([\\w\\.]+)\\s*(<=|>=|<|>|=)\\s*(\\d+)\\s*",
            Pattern.CASE_INSENSITIVE
    );

    // Ej: consultDate - accountOpeningDate = numero de meses
    private static final Pattern DATE_DIFF_MONTHS = Pattern.compile(
            "\\s*([\\w\\.]+)\\s*-\\s*([\\w\\.]+)\\s*=\\s*numero\\s+de\\s+meses\\s*",
            Pattern.CASE_INSENSITIVE
    );

    public static List<Condition> parseCell(String col, String val) {

        {
            String expr = (val == null) ? "" : val.trim();
            if (expr.contains(":")) {
                String[] p = expr.split(":", 2);
                String left = p[0].trim();    // campo fecha (ej consultDate, status.behaviourDate)
                String right = p[1].trim();   // MIN / MAX

                if (right.equalsIgnoreCase("MIN") || right.equalsIgnoreCase("MAX")) {
                    Condition c = new Condition();
                    c.leftVar = left; // el campo de fecha
                    c.op = right.equalsIgnoreCase("MIN") ? Op.PICK_MIN_DATE : Op.PICK_MAX_DATE;
                    c.values = List.of();
                    return List.of(c);
                }
            }
        }

        if (val == null || val.trim().isEmpty()) return List.of();

        String trimmedVal = val.trim();
        System.out.println("Parsing: '" + col + "' = '" + trimmedVal + "'");

        // 0) min / max (selector)
        if (trimmedVal.equalsIgnoreCase("min") || trimmedVal.equalsIgnoreCase("max")) {
            Condition c = new Condition();
            c.leftVar = col.trim(); // la columna debe ser el path de fecha, ej status.behaviourDate
            c.op = trimmedVal.equalsIgnoreCase("min") ? Op.PICK_MIN_DATE : Op.PICK_MAX_DATE;
            c.values = List.of();
            return List.of(c);
        }

        // 0.1) metadata tipo: consultDate = fecha (si lo usas así en CSV)
        // Si en el CSV pones: columna=consultDate, valor=fecha -> no es condición, es metadata.
        if (trimmedVal.equalsIgnoreCase("fecha")) {
            return List.of(); // NOOP
        }
        // También si alguien escribió literalmente: "consultDate = fecha"
        if (trimmedVal.matches("(?i)^consultDate\\s*=\\s*fecha$")) {
            return List.of(); // NOOP
        }

        // 1) Fechas: primero SIEMPRE
        Condition months = tryParseMonthsBetween(trimmedVal);
        if (months != null) return List.of(months);

        Condition limit = tryParseDateDiffLimit(trimmedVal);
        if (limit != null) return List.of(limit);

        // 2) Manejar AND compuesto: "a <> x y (a = y o z)"
        if (trimmedVal.contains(" y ") && trimmedVal.contains("(")) {
            return parseCompoundCondition(trimmedVal);
        }

        // 3) Manejar OR: "a = 1 o 2" / "a <> 1 o 2"
        if (trimmedVal.matches("(?i).*\\s+o\\s+.*")) {
            return parseOrCondition(trimmedVal);
        }

        // 4) simple
        return List.of(parseSimpleCondition(trimmedVal));
    }

    // ------------------- Fechas -------------------

    private static Condition tryParseMonthsBetween(String expr) {
        Matcher m = DATE_DIFF_MONTHS.matcher(expr);
        if (!m.matches()) return null;

        String d1 = m.group(1).trim(); // consultDate
        String d2 = m.group(2).trim(); // paymentDate / status.behaviourDate / accountOpeningDate

        Condition c = new Condition();
        c.leftVar = d1 + "|" + d2;     // consultDate|paymentDate
        c.op = Op.MONTHS_BETWEEN;      // devuelve número de meses en Evaluators
        c.values = List.of();
        return c;
    }

    private static Condition tryParseDateDiffLimit(String expr) {
        Matcher m = DATE_DIFF_LIMIT.matcher(expr);
        if (!m.matches()) return null;

        String d1 = m.group(1).trim();
        String d2 = m.group(2).trim();
        String op = m.group(3).trim();
        String n  = m.group(4).trim();

        Condition c = new Condition();
        c.leftVar = d1 + "|" + d2;
        c.values = List.of(n);

        c.op = switch (op) {
            case "<"  -> Op.DATE_DIFF_LT;
            case "<=" -> Op.DATE_DIFF_LTE;
            case ">"  -> Op.DATE_DIFF_GT;
            case ">=" -> Op.DATE_DIFF_GTE;
            default   -> Op.DATE_DIFF_EQ;
        };
        return c;
    }

    // ------------------- AND compuesto -------------------

    private static List<Condition> parseCompoundCondition(String expression) {
        List<Condition> conditions = new ArrayList<>();
        try {
            String[] parts = expression.split("(?i)\\s+y\\s+");
            for (String part : parts) {
                part = part.trim().replace("(", "").replace(")", "");
                if (part.matches("(?i).*\\s+o\\s+.*")) conditions.addAll(parseOrCondition(part));
                else conditions.add(parseSimpleCondition(part));
            }
        } catch (Exception e) {
            System.err.println("Error parsing compound condition: " + expression);
            e.printStackTrace();
        }
        return conditions;
    }

    // ------------------- OR -------------------

    private static List<Condition> parseOrCondition(String expression) {
        List<Condition> conditions = new ArrayList<>();

        String[] orParts = expression.split("(?i)\\s+o\\s+");
        if (orParts.length == 0) return conditions;

        String first = orParts[0];

        boolean isNeq = first.contains("<>");
        boolean isEq  = first.contains("=") && !isNeq;

        // variable = v1 o v2  -> IN
        if (isEq) {
            String[] leftRight = first.split("=", 2);
            String variable = leftRight[0].trim();

            Condition cond = new Condition();
            cond.leftVar = variable;
            cond.op = Op.IN;
            cond.values = new ArrayList<>();

            for (String part : orParts) {
                String value = part.trim();
                if (value.contains("=")) value = value.split("=", 2)[1].trim();
                cond.values.add(cleanValue(value));
            }
            conditions.add(cond);
            return conditions;
        }

        // variable <> v1 o v2 -> NIN
        if (isNeq) {
            String[] leftRight = first.split("<>", 2);
            String variable = leftRight[0].trim();

            Condition cond = new Condition();
            cond.leftVar = variable;
            cond.op = Op.NIN;
            cond.values = new ArrayList<>();

            for (String part : orParts) {
                String value = part.trim();
                if (value.contains("<>")) value = value.split("<>", 2)[1].trim();
                cond.values.add(cleanValue(value));
            }
            conditions.add(cond);
            return conditions;
        }

        // Por defecto: cada parte individual
        for (String part : orParts) conditions.add(parseSimpleCondition(part.trim()));
        return conditions;
    }

    // ------------------- simple (= / <>) -------------------

    private static Condition parseSimpleCondition(String expression) {
        Condition cond = new Condition();

        if (expression.contains("<>")) {
            String[] parts = expression.split("<>", 2);
            cond.leftVar = parts[0].trim();
            cond.op = Op.NEQ;
            cond.values = parseValues(parts[1].trim());
            return cond;
        }

        if (expression.contains("=")) {
            String[] parts = expression.split("=", 2);
            cond.leftVar = parts[0].trim();
            cond.op = Op.EQ;
            cond.values = parseValues(parts[1].trim());

            // Implícito businessBureauEvent
            if ("businessBureauEvent".equals(cond.leftVar) && cond.op == Op.EQ) {
                addImplicitDescription(cond);
            }
            return cond;
        }

        // fallback
        cond.leftVar = expression.trim();
        cond.op = Op.EQ;
        cond.values = List.of("true");
        return cond;
    }

    // ------------------- util -------------------

    private static List<String> parseValues(String valueStr) {
        if (valueStr == null) return List.of();
        return Arrays.stream(valueStr.split("(?i)\\s+o\\s+"))
                .map(String::trim)
                .map(DslParser::cleanValue)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
    }

    private static String cleanValue(String v) {
        if (v == null) return "";
        v = v.trim();

        while (v.startsWith("(") && v.endsWith(")") && v.length() > 1) {
            v = v.substring(1, v.length() - 1).trim();
        }

        v = v.replace("\"\"", "\"").trim();

        if ((v.startsWith("\"") && v.endsWith("\"")) || (v.startsWith("'") && v.endsWith("'"))) {
            v = v.substring(1, v.length() - 1).trim();
        }
        return v;
    }

    private static void addImplicitDescription(Condition eventCond) {
        List<String> implicitDescriptions = new ArrayList<>();
        for (String eventCode : eventCond.values) {
            String description = EVENT_DESC_MAP.get(eventCode.trim());
            if (description != null) implicitDescriptions.add(description);
        }
        if (!implicitDescriptions.isEmpty()) {
            System.out.println("Descripciones implícitas para " + eventCond.values + ": " + implicitDescriptions);
        }
    }
}