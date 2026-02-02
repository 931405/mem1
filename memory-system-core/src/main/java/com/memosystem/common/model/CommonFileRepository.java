package com.memosystem.common.model;

import com.google.gson.*;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.Reader;
import java.io.UncheckedIOException;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Optional;

/**
 * 内容数据访问对象
 * 负责内容的序列化和持久化操作
 */
@Slf4j
public class CommonFileRepository {
    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    /**
     * 将内容追加保存到 JSON 文件（作为数组元素）
     * 修复：不使用泛型 TypeToken，直接操作 JsonArray
     */
    public static <T> void save(String fileName, T content) {
        Path path = Paths.get(fileName);
        JsonArray list;

        // 读取现有内容
        if (Files.exists(path)) {
            try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
                JsonElement root = JsonParser.parseReader(reader);
                if (root != null && root.isJsonArray()) {
                    list = root.getAsJsonArray();
                } else {
                    list = new JsonArray();
                }
            } catch (IOException | JsonSyntaxException e) {
                log.debug("文件不存在或格式错误，创建新列表");
                list = new JsonArray();
            }
        } else {
            list = new JsonArray();
        }

        // 添加新内容
        JsonElement newElement = GSON.toJsonTree(content);
        list.add(newElement);

        // 写回文件
        try (Writer writer = Files.newBufferedWriter(path, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            GSON.toJson(list, writer);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * 加载最后一条匹配 sessionId 的内容
     */
    public static <T> Optional<T> loadLastContent(String sessionId, String fileName, Class<T> clazz) {
        Path path = Paths.get(fileName);
        if (!Files.exists(path)) {
            return Optional.empty();
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);

            if (!root.isJsonArray()) {
                return Optional.empty();
            }

            JsonArray array = root.getAsJsonArray();

            // 倒序遍历
            for (int i = array.size() - 1; i >= 0; i--) {
                JsonElement element = array.get(i);

                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject jsonObject = element.getAsJsonObject();

                if (!isSessionMatch(jsonObject, sessionId)) {
                    continue;
                }

                return Optional.of(GSON.fromJson(jsonObject, clazz));
            }

        } catch (IOException | JsonSyntaxException e) {
            log.warn("读取消息对失败: {}", e.getMessage());
        }

        return Optional.empty();
    }

    private static boolean isSessionMatch(JsonObject jsonObject, String sessionId) {
        return jsonObject.has("sessionId") &&
                jsonObject.get("sessionId").getAsString().equals(sessionId);
    }

    /**
     * 加载指定 sessionId 的最近 N 条消息
     * 用于短期记忆的懒加载恢复
     * 
     * @param sessionId 会话ID
     * @param fileName  文件名
     * @param count     加载数量
     * @param clazz     目标类型
     * @return 最近的消息列表（按时间顺序，最早的在前）
     */
    public static <T> java.util.List<T> loadRecentMessages(String sessionId, String fileName, int count,
            Class<T> clazz) {
        java.util.List<T> result = new java.util.ArrayList<>();
        Path path = Paths.get(fileName);

        if (!Files.exists(path)) {
            return result;
        }

        try (Reader reader = Files.newBufferedReader(path, StandardCharsets.UTF_8)) {
            JsonElement root = JsonParser.parseReader(reader);

            if (!root.isJsonArray()) {
                return result;
            }

            JsonArray array = root.getAsJsonArray();
            java.util.List<T> matched = new java.util.ArrayList<>();

            // 倒序遍历收集匹配的消息
            for (int i = array.size() - 1; i >= 0 && matched.size() < count; i--) {
                JsonElement element = array.get(i);

                if (!element.isJsonObject()) {
                    continue;
                }

                JsonObject jsonObject = element.getAsJsonObject();

                if (isSessionMatch(jsonObject, sessionId)) {
                    matched.add(GSON.fromJson(jsonObject, clazz));
                }
            }

            // 反转，使最早的在前
            java.util.Collections.reverse(matched);
            return matched;

        } catch (IOException | JsonSyntaxException e) {
            log.warn("读取消息失败: {}", e.getMessage());
        }

        return result;
    }
}