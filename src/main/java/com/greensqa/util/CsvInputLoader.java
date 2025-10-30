package com.greensqa.util;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.*;

public class CsvInputLoader {


    public static List<Map<String, String>> loadCsv(String resourcePath) {
        List<Map<String, String>> rows = new ArrayList<>();
        try (var in = CsvInputLoader.class.getResourceAsStream(resourcePath);
             var reader = new BufferedReader(new InputStreamReader(in))) {
            String headerLine = reader.readLine();
            if (headerLine == null) throw new RuntimeException("CSV vacío: " + resourcePath);
            String[] headers = headerLine.split(";", -1);
            String line;
            while ((line = reader.readLine()) != null) {
                String[] values = line.split(";", -1);
                Map<String,String> map = new LinkedHashMap<>();
                for (int i=0;i<headers.length && i<values.length;i++) {
                    map.put(headers[i].trim(), values[i].trim());
                }
                rows.add(map);
            }
        } catch (Exception e) { throw new RuntimeException("Error leyendo CSV: " + resourcePath, e); }
        return rows;
    }
}
