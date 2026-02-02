package com.memosystem.adapter.llm;

import com.memosystem.common.exception.LLMClientException;
import com.memosystem.common.exception.JsonParseException;
import com.memosystem.core.memory.CandidateMemory;
import org.json.JSONArray;
import org.json.JSONObject;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.memosystem.config.MemoryPrompts;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.io.entity.StringEntity;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * LLM 客户端，用于调用 OpenAI 兼容 API
 * 实现 LLMClientInterface 接口
 * 
 * 所有配置通过构造函数传入，不依赖静态方法
 */
@Slf4j
public class LLMClient implements LLMClientInterface {
    private final String apiKey;
    private final String model;
    private final String chatCompletionEndpoint;
    private final int connectTimeout;
    private final int apiTimeout;
    private final double temperature;
    private final int maxTokens;
    private final Gson gson = new Gson();
    private final HttpClient httpClient;

    /**
     * 完整构造函数：接收所有必要的配置参数
     */
    public LLMClient(String apiKey, String model, String chatCompletionEndpoint,
            int connectTimeout, int apiTimeout, double temperature, int maxTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.chatCompletionEndpoint = chatCompletionEndpoint;
        this.connectTimeout = connectTimeout;
        this.apiTimeout = apiTimeout;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(connectTimeout))
                .build();

        if (this.apiKey == null || this.apiKey.isEmpty()) {
            log.warn("未配置 API 密钥，LLM 调用将失败");
        }
    }

    /**
     * 供记忆提取阶段调用，形成候选记忆列表
     */
    @Override
    public List<CandidateMemory> formCandidateMemories(String prompt) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new LLMClientException("未配置 API Key，无法提取候选记忆");
        }

        // 组装请求体
        JSONObject body = new JSONObject()
                .put("model", model)
                .put("temperature", temperature)
                .put("max_tokens", 4000)
                .put("messages", new JSONArray()
                        .put(new JSONObject()
                                .put("role", "system")
                                .put("content", MemoryPrompts.CHINESE_CANDIDATE_MEMORY_EXTRACTION_PROMPT))
                        .put(new JSONObject()
                                .put("role", "user")
                                .put("content", prompt)));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(chatCompletionEndpoint))
                .header("Authorization", "Bearer " + apiKey)
                .header("Content-Type", "application/json")
                .timeout(Duration.ofSeconds(apiTimeout))
                .POST(HttpRequest.BodyPublishers.ofString(body.toString()))
                .build();

        try {
            HttpResponse<String> resp = httpClient.send(req,
                    HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (resp.statusCode() != 200) {
                throw new LLMClientException("LLM API 调用失败，状态码：" + resp.statusCode() + "，响应：" + resp.body());
            }

            // 解析返回
            JSONObject jsonResp = new JSONObject(resp.body());
            String content = jsonResp.getJSONArray("choices")
                    .getJSONObject(0)
                    .getJSONObject("message")
                    .getString("content");

            log.debug("LLM 返回的原始内容：{}", content);

            // 尝试解析 JSON 数组
            return parseMemoriesFromContent(content);

        } catch (IOException | InterruptedException e) {
            throw new LLMClientException("调用 LLM API 异常：" + e.getMessage(), e);
        } catch (org.json.JSONException e) {
            throw new JsonParseException("解析 LLM 响应 JSON 失败：" + e.getMessage(), e);
        }
    }

    /**
     * 从 LLM 返回的内容中解析候选记忆
     */
    private List<CandidateMemory> parseMemoriesFromContent(String content) {
        if (content == null || content.trim().isEmpty()) {
            throw new JsonParseException("LLM 返回空内容，无法解析候选记忆");
        }

        content = content.trim();

        // 预处理：去除 Markdown 代码块标记
        content = stripMarkdownCodeBlock(content);

        log.debug("开始解析 LLM 返回内容，长度：{} 字符", content.length());

        // 尝试格式1：直接 JSON 数组 [...]
        if (content.startsWith("[")) {
            try {
                JSONArray arr = new JSONArray(content);
                return extractMemoriesFromArray(arr);
            } catch (Exception e) {
                log.debug("直接数组解析失败：{}", e.getMessage());
            }
        }

        // 尝试格式2：JSON 对象 { "facts": [...] }
        if (content.startsWith("{")) {
            try {
                JSONObject obj = new JSONObject(content);
                if (obj.has("facts")) {
                    JSONArray arr = obj.getJSONArray("facts");
                    return extractMemoriesFromArray(arr); // 空数组会返回空列表，不会抛异常
                }
            } catch (Exception e) {
                log.debug("JSON 对象解析失败：{}", e.getMessage());
            }
        }

        // 尝试格式3：文本中提取 JSON
        int startIdx = content.indexOf("[");
        int endIdx = content.lastIndexOf("]");

        if (startIdx >= 0 && endIdx > startIdx) {
            String jsonStr = content.substring(startIdx, endIdx + 1);
            try {
                JSONArray arr = new JSONArray(jsonStr);
                List<CandidateMemory> result = extractMemoriesFromArray(arr);
                if (!result.isEmpty()) {
                    return result;
                }
            } catch (Exception e) {
                log.debug("提取的 JSON 解析失败：{}", e.getMessage());
            }
        }

        // 尝试从 { } 中提取
        int objStartIdx = content.indexOf("{");
        int objEndIdx = content.lastIndexOf("}");

        if (objStartIdx >= 0 && objEndIdx > objStartIdx) {
            String jsonStr = content.substring(objStartIdx, objEndIdx + 1);
            try {
                JSONObject obj = new JSONObject(jsonStr);
                if (obj.has("facts")) {
                    JSONArray arr = obj.getJSONArray("facts");
                    List<CandidateMemory> result = extractMemoriesFromArray(arr);
                    if (!result.isEmpty()) {
                        return result;
                    }
                }
            } catch (Exception e) {
                log.debug("提取的 JSON 对象解析失败：{}", e.getMessage());
                // 尝试修复截断的 JSON
                try {
                    List<CandidateMemory> result = tryParseIncompleteJson(jsonStr);
                    if (!result.isEmpty()) {
                        log.warn("JSON 被截断，但成功解析了 {} 条候选记忆", result.size());
                        return result;
                    }
                } catch (Exception e2) {
                    log.debug("修复截断 JSON 失败：{}", e2.getMessage());
                }
            }
        }

        throw new IllegalArgumentException("无法从 LLM 返回内容中解析出有效的 JSON");
    }

    /**
     * 从 JSONArray 中提取候选记忆
     */
    private List<CandidateMemory> extractMemoriesFromArray(JSONArray arr) {
        List<CandidateMemory> list = new ArrayList<>();

        if (arr == null || arr.length() == 0) {
            return list;
        }

        for (int i = 0; i < arr.length(); i++) {
            try {
                Object elem = arr.get(i);

                if (elem instanceof org.json.JSONObject) {
                    JSONObject o = (JSONObject) elem;
                    CandidateMemory mem = parseMemoryObject(o, i);
                    if (mem != null) {
                        list.add(mem);
                    }
                } else if (elem instanceof String) {
                    String fact = (String) elem;
                    if (fact != null && !fact.trim().isEmpty()) {
                        list.add(new CandidateMemory(fact.trim(), "other", 0.7));
                    }
                } else if (elem instanceof org.json.JSONArray) {
                    JSONArray nested = (JSONArray) elem;
                    list.addAll(extractMemoriesFromArray(nested));
                }
            } catch (Exception e) {
                log.warn("解析第 {} 个候选记忆时出错：{}", i, e.getMessage());
            }
        }

        if (!list.isEmpty()) {
            log.debug("✓ 成功提取 {} 个候选记忆", list.size());
        }

        return list;
    }

    /**
     * 解析单个记忆对象
     */
    private CandidateMemory parseMemoryObject(JSONObject obj, int index) {
        if (!obj.has("fact")) {
            log.warn("第 {} 个元素缺少必需的 'fact' 字段，跳过", index);
            return null;
        }

        try {
            String fact = obj.getString("fact");
            if (fact == null || fact.trim().isEmpty()) {
                return null;
            }

            String category = obj.optString("category", "other");
            if (category == null || category.trim().isEmpty()) {
                category = "other";
            }

            double confidence = obj.optDouble("confidence", 0.8);
            if (confidence < 0)
                confidence = 0.0;
            if (confidence > 1)
                confidence = 1.0;

            return new CandidateMemory(fact.trim(), category.trim(), confidence);
        } catch (Exception e) {
            log.warn("解析对象异常：{}", e.getMessage());
            return null;
        }
    }

    /**
     * 去除 Markdown 代码块标记
     * 例如: ```json\n{...}\n``` → {...}
     */
    private String stripMarkdownCodeBlock(String content) {
        if (content == null) {
            return content;
        }

        String trimmed = content.trim();

        // 匹配 ```json 或 ``` 开头
        if (trimmed.startsWith("```")) {
            // 找到第一个换行符
            int firstNewline = trimmed.indexOf('\n');
            if (firstNewline > 0) {
                trimmed = trimmed.substring(firstNewline + 1);
            } else {
                // 没有换行符，去掉开头的 ```xxx
                trimmed = trimmed.substring(3);
            }
        }

        // 去掉结尾的 ```
        if (trimmed.endsWith("```")) {
            trimmed = trimmed.substring(0, trimmed.length() - 3);
        }

        return trimmed.trim();
    }

    /**
     * 通用的 API 调用方法
     */
    @Override
    public String chat(List<String> messages) {
        if (apiKey == null || apiKey.isEmpty()) {
            throw new LLMClientException("API 密钥未配置，无法调用 LLM");
        }

        try (CloseableHttpClient httpClient = HttpClients.createDefault()) {
            HttpPost request = new HttpPost(chatCompletionEndpoint);
            request.setHeader("Authorization", "Bearer " + apiKey);

            JsonObject requestBody = new JsonObject();
            requestBody.addProperty("model", model);
            requestBody.addProperty("temperature", temperature);
            requestBody.addProperty("max_tokens", maxTokens);

            JsonArray messagesArray = new JsonArray();
            for (String message : messages) {
                JsonObject msgObj = new JsonObject();
                msgObj.addProperty("role", "user");
                msgObj.addProperty("content", message);
                messagesArray.add(msgObj);
            }
            requestBody.add("messages", messagesArray);

            request.setEntity(new StringEntity(requestBody.toString(), ContentType.APPLICATION_JSON));

            return httpClient.execute(request, response -> {
                String result = new String(response.getEntity().getContent().readAllBytes());
                try {
                    JsonObject jsonResponse = gson.fromJson(result, JsonObject.class);
                    return jsonResponse.getAsJsonArray("choices")
                            .get(0).getAsJsonObject()
                            .getAsJsonObject("message")
                            .get("content").getAsString();
                } catch (com.google.gson.JsonSyntaxException e) {
                    throw new JsonParseException("解析 API 响应失败：" + e.getMessage(), e);
                }
            });
        } catch (IOException e) {
            throw new LLMClientException("LLM API 调用失败：" + e.getMessage(), e);
        }
    }

    /**
     * 根据用户消息获取 AI 响应
     */
    @Override
    public String generateResponse(String userMessage) {
        List<String> messages = new ArrayList<>();
        messages.add(userMessage);
        return chat(messages);
    }

    /**
     * 尝试解析被截断的 JSON
     */
    private List<CandidateMemory> tryParseIncompleteJson(String incompleteJson) {
        List<CandidateMemory> list = new ArrayList<>();

        try {
            int factsStart = incompleteJson.indexOf("\"facts\"");
            if (factsStart < 0) {
                return list;
            }

            int arrayStart = incompleteJson.indexOf("[", factsStart);
            if (arrayStart < 0) {
                return list;
            }

            int pos = arrayStart + 1;
            int braceCount = 0;
            StringBuilder currentObj = new StringBuilder();

            while (pos < incompleteJson.length()) {
                char ch = incompleteJson.charAt(pos);

                if (ch == '{') {
                    if (braceCount == 0) {
                        currentObj = new StringBuilder();
                    }
                    braceCount++;
                    currentObj.append(ch);
                } else if (ch == '}') {
                    currentObj.append(ch);
                    braceCount--;

                    if (braceCount == 0) {
                        try {
                            JSONObject obj = new JSONObject(currentObj.toString());
                            CandidateMemory mem = parseMemoryObject(obj, list.size());
                            if (mem != null) {
                                list.add(mem);
                            }
                        } catch (Exception e) {
                            log.debug("解析对象失败: {}", e.getMessage());
                        }
                    }
                } else if (braceCount > 0) {
                    currentObj.append(ch);
                }

                pos++;
            }

            log.info("从截断的 JSON 中提取了 {} 个完整的候选记忆", list.size());
        } catch (Exception e) {
            log.debug("尝试解析截断 JSON 失败: {}", e.getMessage());
        }

        return list;
    }
}
