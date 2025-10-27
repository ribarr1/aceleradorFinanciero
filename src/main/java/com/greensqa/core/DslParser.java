package com.greensqa.core;

import com.greensqa.model.Condition;
import com.greensqa.model.Op;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class DslParser {

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

    private static List<Condition> parseOrCondition(String expression) {
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
    }

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
}