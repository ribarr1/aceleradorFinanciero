package com.greensqa.core;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FieldResolver {
    private static final Map<String,String> EVENT_DESC = Map.of(
            "45","cartera castigada",
            "47","dudoso recaudo",
            "46","cartera recuperada"
    );

    public static Object get(JsonNode item, String varName) {
        // Soporta variables “semánticas” frecuentes sin que el CSV use rutas
        return switch (varName) {
            case "economicSector" -> intOrNull(item.path("account").path("economicSector"));
            case "typeOfDebtor"   -> coalesce(
                    text(item.path("featuresLiabilities").path("typeOfDebtor")),
                    text(item.path("FeaturesCreditCard").path("typeOfDebtor")),
                    text(item.path("account").path("typeOfDebtor"))
            );
            case "businessBureauEvent" -> coalesce(
                    text(item.path("status").path("payment").path("businessBureauEvent")),
                    text(item.path("status").path("businessBureauEvent"))
            );
            case "businessBureauEventDesc" -> coalesce(
                    text(item.path("status").path("payment").path("businessBureauEventDesc")),
                    text(item.path("status").path("businessBureauEventDesc"))
            );
            case "personIdNumber" -> coalesce(
                    text(item.path("account").path("personId").path("personIdNumber")),
                    text(item.path("account").path("personIdNumber")),
                    text(item.path("personIdNumber"))
            );
            default -> // permitir dot-notation si quieres: account.primaryKey, etc.
                    text(resolveByPath(item, varName));
        };
    }

    public static boolean impliesDesc(JsonNode item, String eventCode) {
        String expectedDesc = EVENT_DESC.get(eventCode);
        if (expectedDesc == null) return true; // sin regla implícita
        String desc = (String) get(item, "businessBureauEventDesc");
        return desc != null && desc.toLowerCase(Locale.ROOT).contains(expectedDesc);
    }

    // Utiles fecha
    public static LocalDate getDate(JsonNode item, String var) {
        String s = (String) get(item, var);
        if (s == null || s.isBlank()) return null;
        // Intenta yyyy-MM-dd
        try { return LocalDate.parse(s, DateTimeFormatter.ISO_LOCAL_DATE); }
        catch (Exception ignore) {}
        // Agrega más formatos si tu JSON los trae distintos
        return null;
    }

    // helpers
    private static String text(JsonNode n){ return n!=null && !n.isMissingNode() && !n.isNull() ? n.asText(null) : null; }
    private static Integer intOrNull(JsonNode n){ return n!=null && n.isInt()? n.intValue():null; }
    private static String coalesce(String...v){ for(String s:v){ if(s!=null && !s.isBlank()) return s; } return null; }

    private static JsonNode resolveByPath(JsonNode root, String path) {
        JsonNode cur = root;
        for (String p : path.split("\\.")) {
            if (cur == null) return null;
            cur = cur.path(p);
        }
        return cur;
    }
}
