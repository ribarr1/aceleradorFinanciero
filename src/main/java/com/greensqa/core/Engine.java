package com.greensqa.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.greensqa.model.CaseDef;
import com.greensqa.model.RunResult;
import com.greensqa.util.JsonUtils;

import java.util.*;

public class Engine {
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
        System.out.println("Número de condiciones: " + def.conditions.size());
        System.out.println("Condiciones: " + def.conditions);

        for (JsonNode it : items) {

            System.out.println("\nEVALUANDO ITEM ");
            System.out.println("Item JSON: " + it);

            boolean ok = def.conditions.stream().allMatch(c -> Evaluators.test(it, report, c));
            if (def.expectedOperator.equals("=")){
                if (ok) { anyMatched = true; break; }
            }
            else {if (ok) {contOk++;}}
        }
        System.out.println("Ok:  "+contOk);

        int resultadocaso = def.expectedConst;
        // 3) actual: por defecto 1 si existió al menos una obligación que cumple, sino 0
        int actual=9;
        if (def.expectedOperator.equals("=")) {
            actual = anyMatched ? 1 : 0;
        }else{
            actual=contOk;
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
