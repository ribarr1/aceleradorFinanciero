package com.greensqa.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.greensqa.model.Condition;
import com.greensqa.model.Op;
import java.time.Period;
import java.util.List;
import java.util.Locale;

public class Evaluators {

    public static Object[] test(JsonNode item, JsonNode report, Condition c) {
        boolean ok;
        double valorForzado;
        String sumar = "";
        System.out.println("\n🔍 EVALUANDO CONDICIÓN:");
        System.out.println("   Variable: " + c.leftVar);
        System.out.println("   Operación: " + c.op);
        System.out.println("   Valores esperados: " + c.values);

        // Para condiciones de fecha, usar el report completo para buscar consultDate
        Object actualValue = null;double valor = 0.0; String compare = "";


        if (c.op == Op.DATE_DIFF_LT || c.op == Op.DATE_DIFF_LTE || c.op == Op.DATE_DIFF_GT || c.op == Op.DATE_DIFF_EQ) {
            ok = dateDiff(item, report, c);
            return new Object[]{ ok, valor, sumar };
        }

        if (!(c.leftVar != null && c.leftVar.contains("-"))){

            actualValue = FieldResolver.get(item, c.leftVar);
            System.out.println("   Valor actual en JSON: " + actualValue);



            compare = String.valueOf(actualValue);
            if (String.valueOf(c.op).equalsIgnoreCase("EQ")) {
                System.out.println("   C VALUE: " + c.values.get(0));
                if (c.values.get(0).equals("SUM") || c.values.get(0).equals("MIN") || c.values.get(0).equals("MAX")) {
                    sumar = c.values.get(0);
                    if (actualValue != null) {
                        try {
                            valor = Double.parseDouble(String.valueOf(actualValue)
                                    .replace("$", "")
                                    .replace(",", "")
                                    .trim());
                        } catch (NumberFormatException e) {
                            System.err.println("⚠️ No se pudo convertir a número: " + actualValue);
                        }
                    } else {
                        valor = 0.0;
                    }
                } else {
                    compare = c.values.get(0);
                }
            }
        }else {


            if (c.leftVar.contains("-")
                    && (c.op == Op.EQ || c.op == Op.IN || c.op == Op.NEQ || c.op == Op.NIN)) {

                if (c.op == Op.EQ || c.op == Op.IN) {
                    ok = pairIn(item, c.leftVar, c.values);
                } else {
                    ok = pairNin(item, c.leftVar, c.values);
                }

                System.out.println("Resultado (PAIR): " + ok);
                return new Object[]{ok, valor, sumar};
            }
        }
        /*ok = switch (c.op) {
            case EQ   -> eq(item, c.leftVar, compare);
            case NEQ  -> neq(item, c.leftVar, c.values.get(0));
            case IN   -> in(item, c.leftVar, c.values);
            case NIN  -> nin(item, c.leftVar, c.values);
            case DATE_DIFF_LT, DATE_DIFF_GT, DATE_DIFF_EQ -> dateDiff(item, report, c);
        };*/

        ok = switch (c.op) {
            case EQ   -> eq(item, c.leftVar, compare);
            case NEQ  -> (c.values.size() > 1)
                    ? nin(item, c.leftVar, c.values)     // <-- si hay varios, trátalo como NIN
                    : neq(item, c.leftVar, c.values.get(0));
            case IN   -> in(item, c.leftVar, c.values);
            case NIN  -> nin(item, c.leftVar, c.values);
            case DATE_DIFF_LT, DATE_DIFF_LTE, DATE_DIFF_GT, DATE_DIFF_GTE, DATE_DIFF_EQ
                    -> dateDiff(item, report, c);
            case PICK_MIN_DATE, PICK_MAX_DATE
                    -> true; // no se evalúa aquí (Engine)

            case MONTHS_BETWEEN
                    -> true; // tampoco aquí (Engine toma el valor en r[1] si lo calculas antes del switch)
        };
        System.out.println("   SUMAR: " + sumar);
        return new Object[]{ ok, valor, sumar };
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

    private static boolean pairIn(JsonNode item, String varPair, List<String> pairValues) {
        // varPair ejemplo: "accountType-subAccountType"
        String[] vars = varPair.split("-", 2);
        String left = vars[0].trim();   // accountType
        String right = vars[1].trim();  // subAccountType

        Object leftObj = FieldResolver.get(item, left);
        Object rightObj = FieldResolver.get(item, right);
        System.out.println("   Valor actual en JSON accountType: " + leftObj);
        System.out.println("   Valor actual en JSON subAccountType: " + rightObj);

        if (leftObj == null) return false; // si no hay accountType, no puede hacer match

        String actualLeft = normalizeKeepZeros(leftObj);     // "05"
        String actualRight = rightObj == null ? "" : normalizeKeepZeros(rightObj);

        for (String token : pairValues) {
            if (token == null) continue;
            String t = token.trim().replace("\"\"", "\"");
            // token ejemplo: "05-00" o "03-todos"
            String[] parts = t.split("-", 2);
            String expLeft = parts[0].trim();
            String expRight = (parts.length > 1 ? parts[1].trim() : "");

            expLeft = normalizeKeepZeros(expLeft);
            expRight = normalizeKeepZeros(expRight);

            if (!actualLeft.equals(expLeft)) continue;

            // wildcard "todos" => cualquier subAccountType es válido
            if ("todos".equalsIgnoreCase(expRight) || "*".equals(expRight)) {
                return true;
            }

            // match exacto del subAccountType
            if (!actualRight.isEmpty() && actualRight.equals(expRight)) {
                return true;
            }
        }
        return false;
    }

    private static boolean pairNin(JsonNode item, String varPair, List<String> forbiddenPairs) {
        // NOT IN de pares: pasa si NO hace match con ninguno
        return !pairIn(item, varPair, forbiddenPairs);
    }

    /**
     * Normaliza sin romper ceros a la izquierda.
     * - Si viene "05" lo deja "05"
     * - Si viene 5 lo convierte a "5" (si necesitas forzar a 2 dígitos me dices y lo ajusto)
     */
    private static String normalizeKeepZeros(Object o) {
        String s = String.valueOf(o).trim();
        // quitar comillas externas si vienen
        if ((s.startsWith("\"") && s.endsWith("\"")) || (s.startsWith("'") && s.endsWith("'"))) {
            s = s.substring(1, s.length() - 1).trim();
        }
        return s.toLowerCase(Locale.ROOT);
    }
}
