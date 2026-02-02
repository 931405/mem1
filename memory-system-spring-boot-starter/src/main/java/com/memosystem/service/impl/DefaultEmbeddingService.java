package com.memosystem.service.impl;

import com.memosystem.service.EmbeddingService;
import lombok.extern.slf4j.Slf4j;

import java.util.ArrayList;
import java.util.List;

/**
 * 默认文本向量化服务实现
 * 使用简单的哈希算法生成确定性的向量
 * 
 * 注意：这是一个伪实现
 * 在生产环境中，应该提供自定义的 EmbeddingService 实现，
 * 调用真实的 embedding API（如 OpenAI、BGE、Jina 等）
 * 
 * 使用方可以通过定义自己的 EmbeddingService Bean 来替换此默认实现
 */
@Slf4j
public class DefaultEmbeddingService implements EmbeddingService {

    private static final int EMBEDDING_DIMENSION = 384; // 向量维度

    public DefaultEmbeddingService() {
        log.warn("⚠ 使用默认 EmbeddingService（伪实现）");
        log.warn("⚠ 生产环境请提供自定义的 EmbeddingService 实现");
    }

    /**
     * 将文本转换为向量表示
     * 这里使用一个简单的哈希算法生成确定性的向量
     * 在生产环境中应该调用真实的embedding API
     */
    @Override
    public List<Float> embed(String text) {
        if (text == null || text.isEmpty()) {
            return generateZeroVector();
        }

        // 使用文本哈希生成确定性向量
        List<Float> embedding = new ArrayList<>();
        byte[] bytes = text.getBytes();

        // 生成 EMBEDDING_DIMENSION 维的向量
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            // 使用不同的哈希函数组合生成向量分量
            long hash = 0;
            for (int j = 0; j < bytes.length; j++) {
                hash = ((hash << 5) - hash) + bytes[j] + i;
            }

            // 将哈希值归一化到[-1, 1]
            float value = ((float) ((hash % 1000) / 1000.0)) * 2 - 1;
            embedding.add(value);
        }

        // 归一化向量
        return normalizeVector(embedding);
    }

    /**
     * 归一化向量
     */
    private List<Float> normalizeVector(List<Float> vector) {
        float norm = 0f;
        for (float v : vector) {
            norm += v * v;
        }
        norm = (float) Math.sqrt(norm);

        if (norm == 0f) {
            return generateZeroVector();
        }

        List<Float> normalized = new ArrayList<>();
        for (float v : vector) {
            normalized.add(v / norm);
        }
        return normalized;
    }

    /**
     * 生成零向量
     */
    private List<Float> generateZeroVector() {
        List<Float> zero = new ArrayList<>();
        for (int i = 0; i < EMBEDDING_DIMENSION; i++) {
            zero.add(0f);
        }
        return zero;
    }

    /**
     * 获取向量维度
     */
    @Override
    public int getEmbeddingDimension() {
        return EMBEDDING_DIMENSION;
    }
}
