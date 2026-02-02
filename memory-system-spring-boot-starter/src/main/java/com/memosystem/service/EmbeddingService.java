package com.memosystem.service;

import java.util.List;

/**
 * Embedding 服务接口
 * 用于将文本转换为向量表示
 * 
 * 使用方可以实现此接口来提供自定义的 embedding 实现，
 * 例如调用 OpenAI、BGE、Jina 等 embedding API
 */
public interface EmbeddingService {

    /**
     * 将文本转换为向量表示
     * 
     * @param text 要转换的文本
     * @return 向量表示（浮点数列表）
     */
    List<Float> embed(String text);

    /**
     * 获取向量维度
     * 
     * @return 向量维度
     */
    int getEmbeddingDimension();
}
