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

        for (String part : orParts) {
            part = part.trim();
            conditions.add(parseSimpleCondition(part));
        }

        return conditions;
    }

    private static Condition parseSimpleCondition(String expression) {
        Condition cond = new Condition();

        // Detectar operadores
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