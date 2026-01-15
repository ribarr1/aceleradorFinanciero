package com.greensqa.core;

import com.greensqa.model.RunResult;
import java.util.List;

public class ReportGenerator {

    public static void printSimpleReport(String personId, List<RunResult> results) {
        System.out.println("\n" + "=".repeat(60));
        System.out.println("          REPORTE PARA USUARIO - ID: " + personId);
        System.out.println("=".repeat(60));

        int total = results.size();
        int aprobados = 0;
        int rechazados = 0;

        for (RunResult r : results) {
            if ("PASS".equals(r.status)) aprobados++;
            if ("FAIL".equals(r.status)) rechazados++;
        }

        System.out.println("📊 RESUMEN:");
        System.out.println("   • Total casos: " + total);
        System.out.println("   • Aprobados: " + aprobados + " Pasados");
        System.out.println("   • Rechazados: " + rechazados + " Fallidos");
        System.out.println("   • Con error: " + (total - aprobados - rechazados) + " Errores");

        System.out.println("\n🔍 DETALLE:");
        for (RunResult r : results) {
            String icon = "PASS".equals(r.status) ? "Pasado" : "FAIL".equals(r.status) ? "fallido" : "Error";
            System.out.println("   " + icon + " Caso " + r.id + " - " + r.variable +
                    " (Json: " + r.expected + ", Robot: " + r.actual + ")");
        }

        System.out.println("\n🎯 CONCLUSIÓN:");
        if (rechazados == 0) {
            System.out.println("    CUMPLE con los criterios de aprobación");
        } else {
            System.out.println("    NO CUMPLE con algunos criterios");
        }
        System.out.println("=".repeat(60) + "\n");
    }
}