package com.greensqa.util;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class JsonUtils {
    public static JsonNode loadResourceJson(ObjectMapper mapper, String resourcePath) {
        try (InputStream in = JsonUtils.class.getResourceAsStream(resourcePath)) {
            if (in == null) throw new IllegalArgumentException("No existe recurso: " + resourcePath);
            return mapper.readTree(in);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static JsonNode getSection(JsonNode root, String name) {
        if (root.has(name) && root.get(name).isArray()) return root.get(name);
        if (root.has("ReportHDCplus") && root.get("ReportHDCplus").has(name)
                && root.get("ReportHDCplus").get(name).isArray()) {
            return root.get("ReportHDCplus").get(name);
        }
        return null;
    }

    public static List<JsonNode> findKeyRecursive(JsonNode root, String key) {
        List<JsonNode> hits = new ArrayList<>();
        walk(root, key, hits);
        return hits;
    }
    private static void walk(JsonNode n, String key, List<JsonNode> hits) {
        if (n.isObject()) {
            n.fieldNames().forEachRemaining(fn -> {
                JsonNode c = n.get(fn);
                if (fn.equals(key)) hits.add(c);
                walk(c, key, hits);
            });
        } else if (n.isArray()) n.forEach(c -> walk(c, key, hits));
    }

    public static Integer findFirstInt(JsonNode root, String key) {
        var hits = findKeyRecursive(root, key);
        if (hits.isEmpty()) return null;
        JsonNode v = hits.get(0);
        try {
            return v.isNumber() ? v.intValue() : Integer.parseInt(v.asText().trim());
        } catch (Exception e) {
            return null;
        }
    }
}
