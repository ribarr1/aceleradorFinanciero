package com.greensqa.arnes;
import com.greensqa.core.ReportGenerator;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.greensqa.core.DslParser;
import com.greensqa.core.Engine;
import com.greensqa.model.CaseDef;
import com.greensqa.model.Condition;
import com.greensqa.model.RunResult;
import com.greensqa.services.ServiceClient;
import com.greensqa.util.CsvInputLoader;
import com.greensqa.util.TemplateUtil;
import java.nio.file.Path;
import java.util.*;
import java.util.stream.Collectors;

public class Arnes {
    public static void main(String[] args) {
      try {
          runApplication(args);
      }  catch (Throwable t) {
          System.err.println("ERROR CRÍTICO: " + t.getClass().getSimpleName() + " - " + t.getMessage());
          t.printStackTrace();
          // Mantener la consola abierta
          System.out.println("Presiona Enter para salir...");
          try { System.in.read(); } catch (Exception e) {}
          try { Thread.sleep(5000); } catch (InterruptedException e) {}
      }
    }

    private static void runApplication(String[] args) {
        String cookie = "AWSALB=...; AWSALBCORS=...";   // si aplica
        String reportUrl = "https://webappsdev.devbancoomeva.co/credit-history/v1/hdcplus";
        String variablesUrl = "https://rulesdev.devbancoomeva.co/hdc-plus/api/v1/interpreter";

        ServiceClient client = new ServiceClient(cookie);
        ObjectMapper mapper = client.mapper();

        // 1) Cargar entradas (múltiples filas)
        List<Map<String, String>> inputs = CsvInputLoader.loadCsv("/request.csv");

       // System.out.println("=== DEBUG CSV INPUT ===");
       // System.out.println("Número de registros cargados: " + inputs.size());
        for (int i = 0; i < inputs.size(); i++) {
     //       System.out.println("Registro " + i + ": " + inputs.get(i));
        }
     //   System.out.println("=======================");


        // 2) Cargar modelo de casos (filas → CaseDef)
        List<Map<String, String>> casosCsv = CsvInputLoader.loadCsv("/modeloGruposCasos.csv");

        List<CaseDef> caseDefs = casosCsv.stream().map(Arnes::toCaseDef).toList();

    //    System.out.println("=== INICIANDO LOOP PRINCIPAL ===");
    //    System.out.println("Número de registros a procesar: " + inputs.size());
    //    System.out.println("IDs de los registros: " + inputs.stream().map(m -> m.get("personIdNumber")).collect(Collectors.toList()));

        for (Map<String, String> vars : inputs) {
            String personId = vars.getOrDefault("personIdNumber", "(s/d)");
            System.out.println("=== Ejecutando personIdNumber = " + personId + " ===");
            System.out.flush();


            // 3) templating de bodies
            JsonNode bodyR = TemplateUtil.loadTemplatedJson(mapper, "/bodyReporte.json", vars);
            JsonNode bodyV = TemplateUtil.loadTemplatedJson(mapper, "/bodyVariables.json", vars);
        //    System.out.println("Templating completado para registro " + personId);


            // 4) consumir una vez
          //  System.out.println("Enviando requests HTTP para " + personId);
            JsonNode report = client.postJson(reportUrl, bodyR, null);
            JsonNode variables = client.postJson(variablesUrl, bodyV, null);
            //System.out.println("Requests HTTP completados para " + personId);

            // 5) ejecutar casos

            //System.out.println("Ejecutando casos para " + personId);
            List<RunResult> results = new ArrayList<>();
            for (CaseDef def : caseDefs) {
                try {
                    results.add(Engine.run(report, variables, def));
                } catch (Exception e) {
                    results.add(new RunResult(def.id, def.expectedVar, "ERROR", null, null, null, e.getMessage()));
                }
            }
           // System.out.println("Todos los casos ejecutados para " + personId);

            //System.out.println("Generando reporte y resumen para " + personId);
            ReportGenerator.printSimpleReport(personId, results);
            //System.out.println("ReportGenerator completado para " + personId); // ← Agregar esto

            // 6) guardar artefactos y resumen
            // client.saveJson(Path.of("outputs/report.json"), report);
            // client.saveJson(Path.of("outputs/variables.json"), variables);
            var summary = new LinkedHashMap<String, Object>();
            summary.put("personIdNumber", vars.get("personIdNumber"));
            summary.put("total", results.size());
            summary.put("pass", results.stream().filter(r -> "PASS".equals(r.status)).count());
            summary.put("fail", results.stream().filter(r -> "FAIL".equals(r.status)).count());
            summary.put("error", results.stream().filter(r -> "ERROR".equals(r.status)).count());
            summary.put("results", results.stream().map(r -> {
                var m = new LinkedHashMap<String, Object>();
                m.put("id", r.id);
                m.put("variable", r.variable);
                m.put("status", r.status);
                m.put("resultadoRobot", r.actual);
                m.put("resultadoJson", r.expected);
                m.put("resultadoCaso", r.resultadocaso);
                if (r.reason != null) m.put("reason", r.reason);
                return m;
            }).collect(Collectors.toList()));

           // System.out.println("Imprimiendo summary para " + personId);
            try {
                //mapper.writerWithDefaultPrettyPrinter().writeValue(System.out, summary);
                String jsonOutput = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(summary);
            //    System.out.println("Summary impreso correctamente para " + personId); // ← Agregar esto
                System.out.println(jsonOutput);
            } catch (Exception e) {
                System.err.println("ERROR CRÍTICO imprimiendo summary: " + e.getClass().getName() + " - " + e.getMessage());
                e.printStackTrace();

                // Imprimir el summary de forma manual como fallback
                System.out.println("=== FALLBACK - SUMMARY MANUAL ===");
                System.out.println("personIdNumber: " + summary.get("personIdNumber"));
                System.out.println("total: " + summary.get("total"));
                System.out.println("pass: " + summary.get("pass"));
                System.out.println("fail: " + summary.get("fail"));
                System.out.println("error: " + summary.get("error"));
                System.out.println("results: " + summary.get("results"));
            }
            System.out.println("=== REGISTRO " + personId + " COMPLETADO ===");
            System.out.println();
        }
    }

    private static CaseDef toCaseDef(Map<String, String> row) {
        CaseDef def = new CaseDef();

        // Asignar valores básicos
        def.id = row.get("﻿id") != null ? row.get("﻿id").trim() : row.getOrDefault("id", "").trim();
       // def.id = row.containsKey("id") ? row.get("id").trim() : row.getOrDefault("id", "").trim();
        def.expectedVar = row.getOrDefault("expectedVar", "").trim();
        def.groupsRaw = row.getOrDefault("groups", "").trim();
        def.expectedExpr = row.getOrDefault("expected", "").trim();

        // Procesar todas las columnas que no son las reservadas
        for (var e : row.entrySet()) {
            String col = e.getKey().trim();
            String val = e.getValue() != null ? e.getValue().trim() : "";

            if (!col.startsWith("variable ")) continue;
            if (val.isBlank()) continue;

            List<Condition> conditions = DslParser.parseCell(col, val);

            def.conditions.addAll(conditions);
        }

        // Parsear expectedVar si contiene =
        if (def.expectedVar.contains("=") ) {
            String[] parts = def.expectedVar.split("=");
            def.expectedVar = parts[0].trim();
            try {
                def.expectedOperator="=";
                def.expectedConst = Integer.parseInt(parts[1].trim());
            } catch (NumberFormatException e) {
                def.expectedConst = null;
                def.expectedOperator=null;
            }
        }else{
            String[] parts = def.expectedVar.split(">");
            def.expectedVar = parts[0].trim();
            try {
                def.expectedConst = Integer.parseInt(parts[1].trim());
                def.expectedOperator=">";
            } catch (NumberFormatException e) {
                def.expectedConst = null;
                def.expectedOperator=null;
            }
        }


        return def;
    }
}
