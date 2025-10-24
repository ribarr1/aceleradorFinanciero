package com.greensqa.arnes;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greensqa.core.DslParser;
import com.greensqa.core.Engine;
import com.greensqa.model.CaseDef;
import com.greensqa.model.Condition;
import com.greensqa.model.RunResult;
import com.greensqa.services.ServiceClient;
import com.greensqa.util.CsvInputLoader;
import com.greensqa.util.JsonUtils;
import com.greensqa.util.TemplateUtil;

import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Arnes {
    public static void main(String[] args) {
        String cookie = "AWSALB=...; AWSALBCORS=...";   // si aplica
        String reportUrl = "https://webappsdev.devbancoomeva.co/credit-history/v1/hdcplus";
        String variablesUrl = "https://rulesdev.devbancoomeva.co/hdc-plus/api/v1/interpreter";

        ServiceClient client = new ServiceClient(cookie);
        ObjectMapper mapper = client.mapper();

        // 1) Cargar entradas (múltiples filas)
        List<Map<String,String>> inputs = CsvInputLoader.loadCsv("/request.csv");

        // 2) Cargar modelo de casos (filas → CaseDef)
        List<Map<String,String>> casosCsv = CsvInputLoader.loadCsv("/modeloGruposCasos.csv");
        List<CaseDef> caseDefs = casosCsv.stream().map(Arnes::toCaseDef).toList();

        for (Map<String,String> vars : inputs) {
            System.out.println("=== Ejecutando personIdNumber = " + vars.getOrDefault("personIdNumber","(s/d)") + " ===");

            // 3) templating de bodies
            JsonNode bodyR = TemplateUtil.loadTemplatedJson(mapper, "/bodyReporte.json", vars);
            JsonNode bodyV = TemplateUtil.loadTemplatedJson(mapper, "/bodyVariables.json", vars);

            // 4) consumir una vez
            JsonNode report = client.postJson(reportUrl, bodyR, null);
            JsonNode variables = client.postJson(variablesUrl, bodyV, null);

            // 5) ejecutar casos
            List<RunResult> results = new ArrayList<>();
            for (CaseDef def : caseDefs) {
                try {
                    results.add(Engine.run(report, variables, def));
                } catch (Exception e) {
                    results.add(new RunResult(def.id, def.expectedVar, "ERROR", null, null, e.getMessage()));
                }
            }

            // 6) guardar artefactos y resumen
            client.saveJson(Path.of("outputs/report.json"), report);
            client.saveJson(Path.of("outputs/variables.json"), variables);
            var summary = new LinkedHashMap<String,Object>();
            summary.put("personIdNumber", vars.get("personIdNumber"));
            summary.put("total", results.size());
            summary.put("pass", results.stream().filter(r->"PASS".equals(r.status)).count());
            summary.put("fail", results.stream().filter(r->"FAIL".equals(r.status)).count());
            summary.put("error", results.stream().filter(r->"ERROR".equals(r.status)).count());
            summary.put("results", results.stream().map(r -> {
                var m = new LinkedHashMap<String,Object>();
                m.put("id", r.id); m.put("variable", r.variable);
                m.put("status", r.status); m.put("actual", r.actual); m.put("expected", r.expected);
                if (r.reason != null) m.put("reason", r.reason);
                return m;
            }).collect(Collectors.toList()));

            try { mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, summary); }
            catch (Exception ignore) {}
            System.out.println();
        }
    }

    // Construye 1 CaseDef desde una fila del modelo CSV
    private static CaseDef toCaseDef(Map<String,String> row) {
        CaseDef def = new CaseDef();
        def.id          = row.getOrDefault("id", "").trim();
        def.expectedVar = row.getOrDefault("expectedVar", "").trim();
        def.groupsRaw   = row.getOrDefault("groups", "").trim();
        def.expectedExpr= row.getOrDefault("expected", "").trim();

        // Por cada columna “variable”, parseamos su contenido si no está vacío
        for (var e : row.entrySet()) {
            String col = e.getKey().trim();
            String val = e.getValue().trim();
            if (List.of("id","expectedVar","groups","expected").contains(col)) continue;
            if (val.isBlank()) continue;

            // Celdas pueden traer “y” (AND) y “o” (OR), =, <>
            List<Condition> cs = DslParser.parseCell(col, val);
            def.conditions.addAll(cs);
        }
        if (def.expectedVar != null && !def.expectedVar.isBlank() && def.expectedVar.contains("=")) {
            String expr = def.expectedVar.trim();
            int eq = expr.indexOf('=');
            String name = expr.substring(0, eq).trim();
            String val  = expr.substring(eq + 1).trim();
            def.expectedVar = name;
            try {
                def.expectedConst = Integer.parseInt(val);
            } catch (Exception ignore) {
                def.expectedConst = null; // si no es número, se leerá del JSON de variables
            }
        }
        return def;
    }
}
