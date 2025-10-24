package com.greensqa.core;

import com.fasterxml.jackson.databind.JsonNode;
import com.greensqa.util.JsonUtils;

import java.util.ArrayList;
import java.util.List;

public class GroupSelector {
    public static List<JsonNode> select(JsonNode report, String groupName) {
        var out = new ArrayList<JsonNode>();
        JsonNode arr = JsonUtils.getSection(report, groupName);
        if (arr != null && arr.isArray()) arr.forEach(out::add);
        return out;
    }
}

