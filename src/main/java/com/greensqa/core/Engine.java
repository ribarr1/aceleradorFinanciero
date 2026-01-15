package com.greensqa.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.greensqa.model.CaseDef;
import com.greensqa.model.RunResult;
import com.greensqa.util.JsonUtils;

import java.util.*;

public class Engine {

    enum AggMode { NONE, SUM, MIN, MAX }

    static AggMode parseMode(String s) {
        if (s == null) return AggMode.NONE;
        return switch (s.trim().toUpperCase()) {
            case "SUM" -> AggMode.SUM;
            case "MIN" -> AggMode.MIN;
            case "MAX" -> AggMode.MAX;
            default -> AggMode.NONE;
        };
    }

    static double applyAgg(AggMode mode, double current, double v, int countOk) {
        return switch (mode) {
            case SUM -> current + v;
            case MIN -> (countOk == 0) ? v : Math.min(current, v);
            case MAX -> (countOk == 0) ? v : Math.max(current, v);
            default  -> current;
        };
    }
    public static RunResult run(JsonNode report, JsonNode variables, CaseDef def) {

        System.out.println("🚀 INICIANDO EJECUCIÓN CASO: " + def.id);
        System.out.println("Grupos a evaluar: " + def.groupsRaw);
        System.out.println("Total condiciones: " + def.conditions.size());


        // 1) Seleccionar items de grupos (uno o “A o B”)
        List<JsonNode> items = new ArrayList<>();
        for (String g : def.groups()) items.addAll(GroupSelector.select(report, g));

        System.out.println("Total items a evaluar: " + items.size());


        // 2) Evaluar condiciones (AND de todas las conditions de la fila)
        boolean anyMatched = false;
        int contOk = 0;
        double acumulador= 0;
        boolean existeSum = false; boolean existeSumExt = false;
        boolean existeMin = false; boolean existeMinExt = false;
        boolean existeMax = false; boolean existeMaxExt = false;
        System.out.println("Número de condiciones: " + def.conditions.size());
        System.out.println("Condiciones: " + def.conditions);
        int cantidad = 0; String cond = "";
        /*for (JsonNode it : items) {
            cantidad = cantidad + 1;
            System.out.println("\nEVALUANDO ITEM "  + cantidad);
            System.out.println("Item JSON: " + it);



            List<Object[]> resultados = new ArrayList<>();
            String operando="";
            for (var c : def.conditions) {
                System.out.println("VALOR DE C: " + c);
                Object[] r = Evaluators.test(it, report, c);
                resultados.add(r);              // 👈 Sigues guardando cada resultado
                operando = (String) r[2];
                System.out.println("VALOR DE OPERANDO: " + operando);
                // si la condición no es OK, rompemos el ciclo
                if (!((boolean) r[0])) {
                    break;
                }

            }

            existeSum = resultados.stream()
                    .anyMatch(r -> "SUM".equals((String) r[2]));
            existeMin = resultados.stream()
                    .anyMatch(r -> "MIN".equals((String) r[2]));

            // Verificar si todas las condiciones devolvieron ok=true
            boolean ok = resultados.stream()
                    .allMatch(r -> (boolean) r[0]);

            // Obtener el primer valor distinto de 0 o null
            Double valor = resultados.stream()
                    .map(r -> (Double) r[1])
                    .filter(v -> v != null && v != 0)
                    .findFirst()
                    .orElse(0.0);


            if (def.expectedOperator.equals("=")){

                if (ok) {
                    if (!existeSum) {
                        anyMatched = true;
                        break;
                    } else {
                        existeSumExt = true;
                        System.out.print("ACUMULADOR:  ");
                        System.out.printf(java.util.Locale.US, "%.0f%n", acumulador);
                        acumulador = acumulador + valor;
                        contOk++;

                    }
                }

            }
            if (def.expectedOperator.equals(">")){
                if (ok) {contOk++;}
            }
            if (def.expectedOperator.equals(">=")){
                if (ok) {
                           System.out.print("ACUMULADOR:  ");
                           System.out.printf(java.util.Locale.US, "%.0f%n", acumulador);
                           acumulador = acumulador + valor;
                           contOk++;
                        }
            }
        }*/



        for (JsonNode it : items) {
            cantidad++;
            System.out.println("\nEVALUANDO ITEM " + cantidad);
            System.out.println("Item JSON: " + it);

            List<Object[]> resultados = new ArrayList<>();

            for (var c : def.conditions) {
                Object[] r = Evaluators.test(it, report, c);
                resultados.add(r);

                // si falla una condición, cortas temprano
                if (!((boolean) r[0])) break;
            }

            boolean ok = resultados.stream().allMatch(r -> (boolean) r[0]);

            double valor = resultados.stream()
                    .map(r -> (Double) r[1])
                    .filter(v -> v != null && v != 0)
                    .findFirst()
                    .orElse(0.0);

            // modo (SUM/MIN/MAX) si existe en cualquier condición
            String modoStr = resultados.stream()
                    .map(r -> String.valueOf(r[2]))
                    .filter(s -> !"".equals(s) && !"null".equalsIgnoreCase(s))
                    .findFirst()
                    .orElse("");

            AggMode mode = parseMode(modoStr);

            // ---- AQUÍ está tu lógica de negocio, bien separada ----
            if (!ok) continue; // si no cumple, ignora el item

            switch (def.expectedOperator) {

                case ">" -> {
                    // solo contador
                    contOk++;
                }

                case "=", ">=" -> {
                    // agregación solo para "=" y ">="
                    if (mode == AggMode.NONE) {
                        // tu regla actual: si es "=" y no hay SUM/MIN/MAX, consideras match y sales
                        if ("=".equals(def.expectedOperator)) {
                            anyMatched = true;
                            break; // rompe el for de items (igual que tu código)
                        }
                        // si es ">=" y no hay modo, no agregas nada (según tu regla)
                    } else {
                        // aplica SUM/MIN/MAX
                        acumulador = applyAgg(mode, acumulador, valor, contOk);
                        contOk++;

                        // opcional: flags
                        existeSumExt |= (mode == AggMode.SUM);
                        existeMinExt |= (mode == AggMode.MIN);
                        existeMaxExt |= (mode == AggMode.MAX);

                        System.out.print("ACUMULADOR:  ");
                        System.out.printf(java.util.Locale.US, "%.0f%n", acumulador);
                    }
                }

                default -> {
                    // operador no soportado
                }
            }
        }



        System.out.println("Ok:  "+contOk);
        System.out.print("ACUMULADOR:  ");
        System.out.printf(java.util.Locale.US, "%.0f%n", acumulador);


     //   int resultadocaso = def.expectedConst;
        int resultadocaso = 0;
        // 3) actual: por defecto 1 si existió al menos una obligación que cumple, sino 0
        int actual=9;
        if (def.expectedOperator.equals("=")) {
            if(!existeSumExt)
                actual = anyMatched ? 1 : 0;
            else
                actual = (int) acumulador;
        }
        if (def.expectedOperator.equals(">")){
            actual=contOk;
        }
        if (def.expectedOperator.equals(">=")){
            actual= (int) acumulador;
        }

        // 4) esperado
        Integer expected = (def.expectedVar == null || def.expectedVar.isBlank())
                ? null
                : JsonUtils.findFirstInt(variables, def.expectedVar);

        String status = (expected == null) ? "ERROR" :
                (Objects.equals(actual, expected)) ? "PASS" : "FAIL";
              //  (Objects.equals(actual, expected) && Objects.equals(actual, resultadocaso)) ? "PASS" : "FAIL";
        return new RunResult(def.id, def.expectedVar, status, actual, expected, resultadocaso, null);
    }
}
