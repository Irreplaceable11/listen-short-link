package io.listen.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.runtime.Startup;
import jakarta.enterprise.inject.spi.CDI;
import jakarta.inject.Singleton;

@Singleton
public class JsonUtils {


    static ObjectMapper objectMapper = CDI.current().select(ObjectMapper.class).get();

    /**
     * 将对象序列化为 JSON 字符串
     */
    public static String toJson(Object obj) {
        if (obj == null) {
            return "null";
        }

        try {
            return objectMapper.writeValueAsString(obj);
        } catch (Exception e) {
            return String.format("{\"error\":\"序列化失败\",\"className\":\"%s\",\"message\":\"%s\"}",
                    obj.getClass().getSimpleName(), e.getMessage());
        }
    }

    /**
     * 将 JSON 字符串反序列化为对象
     */
    public static <T> T fromJson(String json, Class<T> clazz) {
        if (json == null || json.trim().isEmpty()) {
            return null;
        }

        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    public static String safeToJson(Object obj) {
        return toJson(obj);
    }

    public static boolean isValidJson(String json) {
        try {
            objectMapper.readTree(json);
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
