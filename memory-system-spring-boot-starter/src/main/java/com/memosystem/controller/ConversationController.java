package com.memosystem.controller;

import com.memosystem.common.model.Result;
import com.memosystem.config.TimingMonitor;
import com.memosystem.dto.ChatRequestDTO;
import com.memosystem.dto.UpdateSystemContextDTO;
import com.memosystem.vo.PromptResponseVO;
import com.memosystem.vo.VersionInfoVO;
import com.memosystem.service.ConversationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 对话控制器
 * 提供记忆系统的对话接口
 */
@RestController("mem0ConversationController")
@RequestMapping("${memory.api.prefix:/api/conversation}")
@Tag(name = "对话管理", description = "记忆管理系统对话相关接口")
@Slf4j
public class ConversationController {

    @Autowired
    private ConversationService conversationService;

    /**
     * 处理用户对话请求（支持自定义参数）
     * 
     * @param request 对话请求参数
     * @return 对话结果，包含 AI 回复和相关记忆信息
     */
    @PostMapping("/chat")
    @Operation(summary = "对话交互", description = "用户输入消息，系统返回 AI 回复及相关记忆信息。支持自定义模型和参数。")
    public Result<Map<String, Object>> chat(
            @io.swagger.v3.oas.annotations.parameters.RequestBody(description = "对话请求参数，支持自定义模型和参数", required = true) @jakarta.validation.Valid @RequestBody ChatRequestDTO request) {
        try {
            log.info("收到对话请求 - sessionId: {}, message长度: {}",
                    request.getSessionId(), request.getMessage().length());

            Map<String, Object> result = conversationService.processConversation(
                    request.getSessionId(),
                    request.getMessage());

            return Result.success("对话处理成功", result);
        } catch (Exception e) {
            log.error("对话处理异常 - sessionId: {}, error: {}", request.getSessionId(), e.getMessage(), e);
            return Result.error(5000, "对话处理失败: " + e.getMessage());
        }
    }

    /**
     * 获取带相似记忆的完整提示词
     * 
     * @param message 用户输入的消息
     * @return 构建完成的提示词
     */
    @GetMapping("/prompt")
    @Operation(summary = "获取完整提示词", description = "根据用户消息，返回构建完成的带有相似记忆的提示词")
    @TimingMonitor(value = "获取完整提示词")
    public Result<PromptResponseVO> buildPrompt(
            @Parameter(description = "用户输入的消息", required = true) @RequestParam("message") String message,
            @Parameter(description = "会话 ID", required = true) @RequestParam("sessionId") String sessionId) {
        try {
            log.info("收到提示词构建请求 - sessionId: {}, message长度: {}", sessionId, message.length());
            String prompt = conversationService.getPrompt(sessionId, message);

            PromptResponseVO response = new PromptResponseVO();
            response.setUserMessage(message);
            response.setBuiltPrompt(prompt);
            response.setTimestamp(System.currentTimeMillis());

            return Result.success("提示词构建成功", response);
        } catch (Exception e) {
            log.error("提示词构建异常 - sessionId: {}, error: {}", sessionId, e.getMessage(), e);
            return Result.error(5000, "提示词构建失败: " + e.getMessage());
        }
    }

    @PostMapping("/updateSystemContext")
    @Operation(summary = "更新系统上下文", description = "更新对话系统的全局上下文信息")
    @TimingMonitor(value = "更新系统上下文")
    public Result<String> updateSystemContext(@RequestBody @Validated UpdateSystemContextDTO dto) {

        return conversationService.updateSystemContext(
                dto.getSessionId(),
                dto.getUserMessage(),
                dto.getAiResponse());
    }

    /**
     * 健康检查接口
     * 
     * @return 系统状态
     */
    @GetMapping("/health")
    @Operation(summary = "健康检查", description = "检查系统是否正常运行")
    @ApiResponse(responseCode = "200", description = "系统运行正常")
    public Result<String> health() {
        return Result.success("Memory System is running");
    }

    /**
     * 获取系统版本信息
     * 
     * @return 版本信息
     */
    @GetMapping("/version")
    @Operation(summary = "获取版本信息", description = "获取系统版本和构建信息")
    public Result<VersionInfoVO> getVersion() {
        VersionInfoVO info = new VersionInfoVO(
                "1.0.0",
                "mem0-like Memory Management System",
                "2026-01-04");
        return Result.success("版本信息获取成功", info);
    }
}
