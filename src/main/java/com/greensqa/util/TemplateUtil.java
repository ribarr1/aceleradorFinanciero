package com.greensqa.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Iterator;
import java.util.Map;

public class TemplateUtil {
    // Reemplaza {{clave}} en un JSON cargado como String
    public static String replacePlaceholders(String json, Map<String,String> vars) {
        String out = json;
        for (Map.Entry<String,String> e : vars.entrySet()) {
            out = out.replace("{{" + e.getKey() + "}}", e.getValue());
        }
        return out;
    }

    // Carga un recurso y devuelve JsonNode tras reemplazos
    public static JsonNode loadTemplatedJson(ObjectMapper mapper, String resourcePath, Map<String,String> vars) {
        try (var in = TemplateUtil.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IllegalArgumentException("No existe recurso: " + resourcePath);
            String raw = new String(in.readAllBytes());
            String templated = replacePlaceholders(raw, vars);
            return mapper.readTree(templated);
        } catch (Exception e) {
            throw new RuntimeException("Error cargando plantilla: " + resourcePath, e);
        }
    }
}
