package com.dev.statflex.json;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

public class JSONObject {
    private final Map<String, Object> map;

    public JSONObject() {
        map = new HashMap<>();
    }

    public JSONObject put(String key, Object value) {
        map.put(key, value);
        return this;
    }

    public Object get(String key) {
        return map.get(key);
    }

    public String getString(String key) {
        Object val = get(key);
        return val != null ? val.toString() : null;
    }

    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("{");
        Set<String> keys = map.keySet();
        int i = 0;
        for (String key : keys) {
            sb.append("\"").append(key).append("\":");
            Object val = map.get(key);
            if (val instanceof String) {
                sb.append("\"").append(val).append("\"");
            } else {
                sb.append(val);
            }
            if (i < keys.size() - 1) sb.append(",");
            i++;
        }
        sb.append("}");
        return sb.toString();
    }
}
