package com.greensqa.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.greensqa.model.CaseDef;
import com.greensqa.model.Condition;
import com.greensqa.model.Op;
import com.greensqa.model.RunResult;
import com.greensqa.util.JsonUtils;

import java.time.LocalDate;
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

    public static int countOnesFirstmeses(String input, int meses, char targetChar) {

        if (input == null) return 0;

        // Tomar solo los primeros meses caracteres
        String firstmeses = input.length() > meses
                ? input.substring(0, meses)
                : input;

        int count = 0;

        for (int i = 0; i < firstmeses.length(); i++) {
            if (firstmeses.charAt(i) == targetChar) {
                count++;
            }
        }

        return count;
    }

    public static int countSpecialFirstMeses(String input, int meses) {

        if (input == null) return 0;

        String firstmeses = input.length() > meses
                ? input.substring(0, meses)
                : input;

        int count = 0;

        for (int i = 0; i < firstmeses.length(); i++) {

            char ch = Character.toUpperCase(firstmeses.charAt(i));

            // Dígitos mayores a 4
            if (Character.isDigit(ch) && ch > '4') {
                count++;
            }
            // Letras D o C
            else if (ch == 'D' || ch == 'C') {
                count++;
            }
        }

        return count;
    }

    static double applyAgg(AggMode mode, double current, double v, int countOk) {
        return switch (mode) {
            case SUM -> current + v;
            case MIN -> (countOk == 0) ? v : Math.min(current, v);
            case MAX -> (countOk == 0) ? v : Math.max(current, v);
            default  -> current;
        };
    }

    private static double monthsBetween(JsonNode report, JsonNode item, Condition c) {
        try {
            String[] f = c.leftVar.split("\\|");
            String f1 = f[0].trim(); // consultDate
            String f2 = f[1].trim(); // accountOpeningDate

            LocalDate d1 = FieldResolver.getDate(report, item, f1);
            LocalDate d2 = FieldResolver.getDate(report, item, f2);

            if (d1 == null || d2 == null) return 0.0;

            long months = Math.abs(java.time.Period.between(d2, d1).toTotalMonths());
            return (double) months;
        } catch (Exception e) {
            System.err.println("❌ monthsBetween error: " + e.getMessage());
            return 0.0;
        }
    }

    private static double monthsOnlyDifference(JsonNode report, JsonNode item, Condition c) {
        try {
            String[] f = c.leftVar.split("\\|");
            String f1 = f[0].trim();
            String f2 = f[1].trim();

            LocalDate d1 = FieldResolver.getDate(report, item, f1);
            LocalDate d2 = FieldResolver.getDate(report, item, f2);

            if (d1 == null || d2 == null) return 0.0;

            int month1 = d1.getMonthValue(); // 1–12
            int month2 = d2.getMonthValue(); // 1–12

            int diff = Math.abs(month1 - month2);

            return (double) diff;

        } catch (Exception e) {
            System.err.println("❌ monthsOnlyDifference error: " + e.getMessage());
            return 0.0;
        }
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
        int contOk = 0; LocalDate bestDate = null;
        double acumulador= 0; double maxmeses= -1;
        boolean arreglo = false; boolean existeSumExt = false;
        String vector = null; boolean existeMinExt = false; boolean existeMaxExt = false;
        int totalVector = 0; int maxVector = 0;
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


        java.time.LocalDate d;
        for (JsonNode it : items) {
            cantidad++;
            System.out.println("\nEVALUANDO ITEM " + cantidad);
            System.out.println("Item JSON: " + it);

            List<Object[]> resultados = new ArrayList<>();

            for (var c : def.conditions) {
                if (c.op == Op.PICK_MIN_DATE || c.op == Op.PICK_MAX_DATE) {
                    continue;
                }

                Object[] r = Evaluators.test(it, report, c);
                resultados.add(r);

                // si falla una condición, cortas temprano
                if (!((boolean) r[0])) break;
            }

            boolean ok = resultados.stream().allMatch(r -> (boolean) r[0]);

            Condition pick = def.conditions.stream()
                    .filter(c -> c.op == Op.PICK_MIN_DATE || c.op == Op.PICK_MAX_DATE)
                    .findFirst()
                    .orElse(null);

            Condition pickMonths = def.conditions.stream()
                    .filter(c -> c.op == Op.MONTHS_BETWEEN)
                    .findFirst()
                    .orElse(null);


            if (ok && pick != null) {
                d = FieldResolver.getDate(it, pick.leftVar);

                if (d != null) {
                    if (bestDate == null) bestDate = d;
                    else if (pick.op == Op.PICK_MIN_DATE && d.isBefore(bestDate)) bestDate = d;
                    else if (pick.op == Op.PICK_MAX_DATE && d.isAfter(bestDate)) bestDate = d;
                }
            }


            if (ok && pickMonths != null) {
                double meses = 0; int factor12 = 12;int desc = 0; char caracter = 'z';int bandera=0;
                if (def.expectedVar.toUpperCase().contains("CONTMORA")) {
                    String expectedUpper = def.expectedVar.toUpperCase();
                    if (expectedUpper.contains("CONTMORA30")) {
                        caracter = '1';
                    }
                    else if (expectedUpper.contains("CONTMORA60") ) {
                        caracter = '2';
                    }
                    else if (expectedUpper.contains("CONTMORA90")) {
                        caracter = '3';
                    }
                    else if (expectedUpper.contains("CONTMORA120") && (expectedUpper.contains("ULT12"))) {
                       bandera=1;
                    }
                   else if (expectedUpper.contains("CONTMORA120") && (!expectedUpper.contains("ULT12"))) {
                        caracter = '4';
                    }
                    else if (expectedUpper.contains("ULT6")) {
                        factor12 = 6;
                    }

                    meses = monthsOnlyDifference(report, it, pickMonths); // report + item
                    desc = (int) (factor12 - meses);
                    vector = JsonUtils.findFirstText(it, "businessBehaviourVectorProduct");
                    if(bandera == 1)
                        totalVector = countSpecialFirstMeses(vector, desc);
                    else
                        totalVector = countOnesFirstmeses(vector, desc, caracter);

                    if (totalVector > maxVector)
                        maxVector = totalVector;
                    arreglo = true;
                }else{
                     meses = monthsBetween(report, it, pickMonths); // report + item
                    if (meses > maxmeses){
                        maxmeses = meses;
                    }
                }
                anyMatched = true;

            }


            /* if (ok && pickMonths != null) {
                double meses = monthsBetween(report, it, pickMonths); // report + item
                if (meses > maxmeses){
                    maxmeses = meses;
                }
                if (def.expectedVar.toUpperCase().contains("CONTMORA")) {
                    vector = JsonUtils.findFirstText(it, "businessBehaviourVectorProduct");
                    totalVector=countOnesFirstmeses(vector, (int) meses, '1');
                    if (totalVector > maxVector)
                        maxVector=totalVector;
                    arreglo = true;
                }
                //acumulador = meses;     // para que luego hagas actual = (int)acumulador
                anyMatched = true;      // si tu regla cuenta como “match”
                // break;  // si solo necesitas el primer item válido

            }*/

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







        //   int resultadocaso = def.expectedConst;
        int resultadocaso = 0;
        // 3) actual: por defecto 1 si existió al menos una obligación que cumple, sino 0
        String actual="9";


       /* if (def.expectedOperator.equals("=")) {
            if(!existeSumExt)
                actual = anyMatched ? 1 : 0;
            else
                actual = (int) acumulador;
        }*/
        String out = JsonUtils.findFirstText(variables, def.expectedVar);
        LocalDate outDate = JsonUtils.parseDate(out);

        if (def.expectedOperator.equals("=")) {


            if (!existeSumExt) {




                // 👉 Caso FECHA
                if (arreglo) {
                    actual = String.valueOf(maxVector);
                    System.out.println("VECTOR:  "+vector);
                    System.out.println("MAXMESES:  "+maxmeses);
                    System.out.println("TOTALVECTOR:  "+totalVector);
                    System.out.println("MAXVECTOR:  "+maxVector);

                }
                else if (maxmeses > -1)
                    actual = String.valueOf((int) maxmeses);
                else if (bestDate != null || outDate != null) {
                    System.out.println("FECHA ROBOT:  "+bestDate);
                    System.out.println("FECHA JSON:  "+ outDate);
                    //actual = (bestDate != null && outDate != null && bestDate.equals(outDate)) ? 1 : 0;
                    actual = bestDate.toString();

                }
                // 👉 Caso NORMAL (match)
                else {
                    actual = anyMatched ? "1" : "0";
                }

            } else {
                // 👉 Caso SUMA
                System.out.print("ACUMULADOR:  ");
                System.out.printf(java.util.Locale.US, "%.0f%n", acumulador);
                //actual = (int) acumulador;
                actual = String.valueOf((int) acumulador);
            }
        }



        if (def.expectedOperator.equals(">")){
            actual=String.valueOf(contOk);
        }
        if (def.expectedOperator.equals(">=")){
            System.out.print("ACUMULADOR:  ");
            System.out.printf(java.util.Locale.US, "%.0f%n", acumulador);
            //actual= (int) acumulador;
            actual = String.valueOf((int) acumulador);
        }

        // 4) esperado
        //int expected = (def.expectedVar == null || def.expectedVar.isBlank())
          //      ? null
            //    : JsonUtils.findFirstInt(variables, def.expectedVar);
        String expected = (def.expectedVar == null || def.expectedVar.isBlank())
                ? null
                : String.valueOf(JsonUtils.findFirstInt(variables, def.expectedVar));

        if (expected == "null" && def.expectedOperator.equals("=") && (bestDate != null || out != null)) {
            // regla: si falta alguna fecha => 0, si ambas existen => 1
            //expected = (bestDate != null && out != null) ? 1 : 0;
            expected =  out;

        }

        String status = (expected == null) ? "ERROR" :
                (Objects.equals(actual, expected)) ? "PASS" : "FAIL";
              //  (Objects.equals(actual, expected) && Objects.equals(actual, resultadocaso)) ? "PASS" : "FAIL";
        return new RunResult(def.id, def.expectedVar, status, actual, expected, resultadocaso, null);
    }


}
