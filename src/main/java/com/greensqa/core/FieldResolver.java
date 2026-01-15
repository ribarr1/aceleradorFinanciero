package com.greensqa.core;

import com.fasterxml.jackson.core.JsonPointer;
import com.fasterxml.jackson.databind.JsonNode;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class FieldResolver {
    private static final Map<String,String> EVENT_DESC = Map.of(
            "45","cartera castigada",
            "47","dudoso recaudo",
            "46","cartera recuperada",
            "14", "mora 60 al dia"
    );

    public static Object get(JsonNode item, String varName) {
        // Soporta variables “semánticas” frecuentes sin que el CSV use rutas
        System.out.println("   📍 Buscando campo: '" + varName + "'");
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
            case "consultDate" -> text(item.path("ReportHDCplus").path("productResult").path("consultDate"));
            case "paymentDate" -> coalesce(
                    text(item.path("status").path("payment").path("paymentDate")),
                    text(item.path("paymentDate"))
            );
            case "originStatusOfAccount" -> coalesce(
                    text(item.path("status").path("origin").path("originStatusOfAccount")),
                    text(item.path("status").path("originStatusOfAccount"))
            );
            case "originStatusOfAccountDesc" -> coalesce(
                    text(item.path("status").path("origin").path("originStatusOfAccountDesc")),
                    text(item.path("status").path("originStatusOfAccountDesc"))
            );
            case "typeOfDebtorDesc" -> coalesce(
                    text(item.path("featuresLiabilities").path("typeOfDebtorDesc")),
                    text(item.path("FeaturesCreditCard").path("typeOfDebtorDesc"))
            );
            case "accountType" -> coalesce(
                    text(item.path("account").path("accountType")),
                    text(item.path("account").path("accountType"))
            );
            case "counterpartyIdNumber" -> coalesce(
                    text(item.path("account").path("counterpartyIdNumber")),
                    text(item.path("account").path("counterpartyIdNumber"))
            );
            case "periodicityOfPayments" -> coalesce(
                    text(item.withArray(JsonPointer.valueOf("/values")).path(0).path("periodicityOfPayments")),
                    text(item.withArray(JsonPointer.valueOf("/values")).path(0).path("periodicityOfPayments"))
            );
            case "subAccountType" -> coalesce(
                    text(item.path("account").path("subAccountType")),
                    text(item.path("account").path("subAccountType"))
            );
            case "typeOfCredit" -> coalesce(
                    text(item.path("featuresLiabilities").path("typeOfCredit")),
                    text(item.path("typeOfCredit"))
            );
            case "typeOfCreditDesc" -> coalesce(
                    text(item.path("featuresLiabilities").path("typeOfCreditDesc")),
                    text(item.path("typeOfCreditDesc"))
            );
            case "economicSectorName" -> coalesce(
                    text(item.path("account").path("economicSectorName")),
                    text(item.path("entity").path("economicSectorName")),
                    text(item.path("economicSectorName"))
            );
            case "inquiryReasonCode" -> coalesce(
                    text(item.path("inquiryReasonCode")),
                    text(item.path("inquiryReasonCode"))
            );
            case "initialValue" -> coalesce(
                    // A) primer valor no vacío en el array
                    textFromArrayFirst(item, "values", "initialValue"),
                    // fallback por si en algún payload viene plano
                    text(item.path("initialValue"))
            );
            case "debtBalance" -> coalesce(
                    // A) primer valor no vacío en el array
                    textFromArrayFirst(item, "values", "debtBalance"),
                    // fallback por si en algún payload viene plano
                    text(item.path("debtBalance"))
            );
            case "businessValueBalanceOverdue" -> coalesce(
                    // A) primer valor no vacío en el array
                    textFromArrayFirst(item, "values", "businessValueBalanceOverdue"),
                    // fallback por si en algún payload viene plano
                    text(item.path("businessValueBalanceOverdue"))
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

    private static String textFromArrayFirst(JsonNode root, String arrayName, String field) {
        JsonNode arr = root.path(arrayName);
        if (arr != null && arr.isArray()) {
            for (JsonNode elem : arr) {
                String v = text(elem.path(field));
                if (v != null && !v.isBlank()) return v;
            }
        }
        return null;
    }

    // Utiles fecha
    public static LocalDate getDate(JsonNode item, String field) {
        try {
            System.out.println("   📅 Buscando fecha: '" + field + "'");

            // Primero usar el mapeo de FieldResolver.get
            Object value = get(item, field);
            if (value instanceof String) {
                String dateStr = (String) value;
                // Extraer solo la parte de la fecha (antes de la T)
                if (dateStr.contains("T")) {
                    dateStr = dateStr.split("T")[0];
                }
                System.out.println("   📅 Fecha encontrada: " + dateStr);
                return LocalDate.parse(dateStr);
            }

            // Si no funciona, buscar directamente
            JsonNode node = resolveByPath(item, field);
            if (node != null && !node.isMissingNode() && !node.isNull()) {
                String dateStr = node.asText();
                // Extraer solo la parte de la fecha (antes de la T)
                if (dateStr.contains("T")) {
                    dateStr = dateStr.split("T")[0];
                }
  //              System.out.println("   📅 Fecha encontrada (directa): " + dateStr);
                return LocalDate.parse(dateStr);
            }

        } catch (Exception e) {
            System.err.println("❌ Error parsing date field '" + field + "': " + e.getMessage());
        }
        System.out.println("   ❌ Fecha no encontrada: " + field);
        return null;
    }

//    // helpers
//    private static String text(JsonNode n){ return n!=null && !n.isMissingNode() && !n.isNull() ? n.asText(null) : null; }
//    private static Integer intOrNull(JsonNode n){ return n!=null && n.isInt()? n.intValue():null; }
//    private static String coalesce(String...v){ for(String s:v){ if(s!=null && !s.isBlank()) return s; } return null; }

    // helpers
    private static String text(JsonNode n){
        return n != null && !n.isMissingNode() && !n.isNull() ? n.asText(null) : null;
    }

    private static Integer intOrNull(JsonNode n){
        return n != null && n.isInt() ? n.intValue() : null;
    }

    private static String coalesce(String...v){
        for(String s : v) {
            if(s != null && !s.isBlank())
                return s;
        }
        return null;
    }


    private static JsonNode resolveByPath(JsonNode root, String path) {
        JsonNode cur = root;
        for (String p : path.split("\\.")) {
            if (cur == null) return null;
            cur = cur.path(p);
        }
        return cur;
    }
}
