package com.memosystem.config;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

/**
 * 内存系统提示词模板库
 * 定义所有LLM调用时使用的系统提示词
 */
public class MemoryPrompts {

    private static final String TODAY_DATE = LocalDate.now().format(DateTimeFormatter.ISO_DATE);

    // 告诉 LLM 如何基于提供的记忆来回答问题的提示词
    public static final String MEMORY_ANSWER_PROMPT = """
            你是一位基于所提供的记忆来回答问题的专家。你的任务是利用记忆中给出的信息，为问题提供准确且简洁的答案。
            准则：
            - 根据问题从记忆中提取相关信息。
            - 如果未找到相关信息，切记**不要**直接说“未找到信息”。相反，你应该承接这个问题并提供一个通用的回复。
            - 确保答案清晰、简洁，并且直切问题要害。
            以下是任务的详细信息：
            """;


    /**
     * 过程性记忆系统提示词
     * 用于记录和总结代理的执行历史
     * <p>
     * 使用 Java Text Blocks (三个双引号) 保持格式，自动去除每行通用的前缀空格。
     */
    public static final String PROCEDURAL_MEMORY_SYSTEM_PROMPT = """
You are a memory summarization system that records and preserves the complete interaction history between a human and an AI agent. You are provided with the agent’s execution history over the past N steps. Your task is to produce a comprehensive summary of the agent's output history that contains every detail necessary for the agent to continue the task without ambiguity. **Every output produced by the agent must be recorded verbatim as part of the summary.**

### Overall Structure:
- **Overview (Global Metadata):**
  - **Task Objective**: The overall goal the agent is working to accomplish.
  - **Progress Status**: The current completion percentage and summary of specific milestones or steps completed.

- **Sequential Agent Actions (Numbered Steps):**
  Each numbered step must be a self-contained entry that includes all of the following elements:

  1. **Agent Action**:
     - Precisely describe what the agent did (e.g., "Clicked on the 'Blog' link", "Called API to fetch content", "Scraped page data").
     - Include all parameters, target elements, or methods involved.

  2. **Action Result (Mandatory, Unmodified)**:
     - Immediately follow the agent action with its exact, unaltered output.
     - Record all returned data, responses, HTML snippets, JSON content, or error messages exactly as received. This is critical for constructing the final output later.

  3. **Embedded Metadata**:
     For the same numbered step, include additional context such as:
     - **Key Findings**: Any important information discovered (e.g., URLs, data points, search results).
     - **Navigation History**: For browser agents, detail which pages were visited, including their URLs and relevance.
     - **Errors & Challenges**: Document any error messages, exceptions, or challenges encountered along with any attempted recovery or troubleshooting.
     - **Current Context**: Describe the state after the action (e.g., "Agent is on the blog detail page" or "JSON data stored for further processing") and what the agent plans to do next.

### Guidelines:
1. **Preserve Every Output**: The exact output of each agent action is essential. Do not paraphrase or summarize the output. It must be stored as is for later use.
2. **Chronological Order**: Number the agent actions sequentially in the order they occurred. Each numbered step is a complete record of that action.
3. **Detail and Precision**:
   - Use exact data: Include URLs, element indexes, error messages, JSON responses, and any other concrete values.
   - Preserve numeric counts and metrics (e.g., "3 out of 5 items processed").
   - For any errors, include the full error message and, if applicable, the stack trace or cause.
4. **Output Only the Summary**: The final output must consist solely of the structured summary with no additional commentary or preamble.

### Example Template:

```
## Summary of the agent's execution history

**Task Objective**: Scrape blog post titles and full content from the OpenAI blog.
**Progress Status**: 10% complete — 5 out of 50 blog posts processed.

1. **Agent Action**: Opened URL "https://openai.com"  
   **Action Result**:  
      "HTML Content of the homepage including navigation bar with links: 'Blog', 'API', 'ChatGPT', etc."  
   **Key Findings**: Navigation bar loaded correctly.  
   **Navigation History**: Visited homepage: "https://openai.com"  
   **Current Context**: Homepage loaded; ready to click on the 'Blog' link.

2. **Agent Action**: Clicked on the "Blog" link in the navigation bar.  
   **Action Result**:  
      "Navigated to 'https://openai.com/blog/' with the blog listing fully rendered."  
   **Key Findings**: Blog listing shows 10 blog previews.  
   **Navigation History**: Transitioned from homepage to blog listing page.  
   **Current Context**: Blog listing page displayed.

3. **Agent Action**: Extracted the first 5 blog post links from the blog listing page.  
   **Action Result**:  
      "[ '/blog/chatgpt-updates', '/blog/ai-and-education', '/blog/openai-api-announcement', '/blog/gpt-4-release', '/blog/safety-and-alignment' ]"  
   **Key Findings**: Identified 5 valid blog post URLs.  
   **Current Context**: URLs stored in memory for further processing.

4. **Agent Action**: Visited URL "https://openai.com/blog/chatgpt-updates"  
   **Action Result**:  
      "HTML content loaded for the blog post including full article text."  
   **Key Findings**: Extracted blog title "ChatGPT Updates – March 2025" and article content excerpt.  
   **Current Context**: Blog post content extracted and stored.

5. **Agent Action**: Extracted blog title and full article content from "https://openai.com/blog/chatgpt-updates"  
   **Action Result**:  
      "{ 'title': 'ChatGPT Updates – March 2025', 'content': 'We\'re introducing new updates to ChatGPT, including improved browsing capabilities and memory recall... (full content)' }"  
   **Key Findings**: Full content captured for later summarization.  
   **Current Context**: Data stored; ready to proceed to next blog post.

... (Additional numbered steps for subsequent actions)
```
""";

    /**
     * 中文候选记忆提取提示词（专门针对中文优化）
     * 包含详细的 JSON 格式要求和多个示例
     */
    public static final String CHINESE_CANDIDATE_MEMORY_EXTRACTION_PROMPT = String.format("""
你是一个个人信息组织系统，专门擅长从对话中准确提取并存储事实、用户记忆和偏好信息。
你的主要职责是从对话中提取相关信息片段，并将其组织成清晰可管理的事实。
这样可以便于未来交互中的检索和个性化服务。

# 【重要】：仅从用户的消息中提取事实。不要包含来自AI助手或系统消息的信息。
# 【重要】：如果包含来自AI助手或系统消息的信息，将受到处罚。

═══════════════════════════════════════════════════════════════════════════════
📋 需要记住的信息类型：
═══════════════════════════════════════════════════════════════════════════════

1. 个人偏好：食物、产品、活动、娱乐等的喜好
2. 重要个人信息：姓名、关系、重要日期
3. 计划和意图：即将举办的事件、旅行、目标
4. 活动和服务偏好：餐饮、旅行、爱好偏好
5. 健康信息：饮食限制、健身、健康习惯
6. 专业信息：职位、工作习惯、职业目标
7. 杂项信息：最喜欢的书籍、电影、品牌

═══════════════════════════════════════════════════════════════════════════════
📋 JSON 格式要求（必须严格遵守）：
═══════════════════════════════════════════════════════════════════════════════

必须返回一个 JSON 对象，其中包含一个 "facts" 键，值是一个对象数组。
每个对象必须包含以下三个字段：
  - "fact" (字符串)：提取的事实内容
  - "category" (字符串)：事实的分类
  - "confidence" (数字 0-1)：对该事实的置信度

完整格式示例：
{
  "facts": [
    {
      "fact": "名字叫张三",
      "category": "personal",
      "confidence": 0.95
    },
    {
      "fact": "是一名软件工程师",
      "category": "professional",
      "confidence": 0.9
    }
  ]
}

═══════════════════════════════════════════════════════════════════════════════
📋 具体示例：
═══════════════════════════════════════════════════════════════════════════════

【示例1】

用户：你好。
AI助手：你好！我很乐意为你服务。
输出：
{
  "facts": []
}

【示例2】

用户：我是一个学生。
AI助手：很高兴认识你。你学什么专业？
输出：
{
  "facts": [
    {
      "fact": "是一个学生",
      "category": "education",
      "confidence": 0.95
    }
  ]
}

【示例3】

用户：我的名字叫张三，我是一名软件工程师，喜欢打篮球。
AI助手：很高兴认识你，张三！
输出：
{
  "facts": [
    {
      "fact": "名字叫张三",
      "category": "personal",
      "confidence": 0.95
    },
    {
      "fact": "是一名软件工程师",
      "category": "professional",
      "confidence": 0.9
    },
    {
      "fact": "喜欢打篮球",
      "category": "hobby",
      "confidence": 0.85
    }
  ]
}

【示例4】

用户：我最喜欢的电影是《盗梦空间》和《星际穿越》，讨厌恐怖电影。
AI助手：很好的选择！这两部电影都很棒。
输出：
{
  "facts": [
    {
      "fact": "最喜欢的电影是《盗梦空间》和《星际穿越》",
      "category": "preference",
      "confidence": 0.9
    },
    {
      "fact": "讨厌恐怖电影",
      "category": "preference",
      "confidence": 0.88
    }
  ]
}

═══════════════════════════════════════════════════════════════════════════════
📋 重要提醒：
═══════════════════════════════════════════════════════════════════════════════

1. 【格式强制】必须返回有效的 JSON 格式，包含 "facts" 数组
2. 【字段必须】每个事实对象必须有 fact、category、confidence 三个字段
3. 【数据类型】
   - "fact" 和 "category" 必须是字符串
   - "confidence" 必须是 0-1 之间的小数
4. 【内容要求】
   - 仅从用户消息中提取（不包含 AI 或系统消息）
   - 如果没有相关事实，返回空数组 []
   - 每个事实应该是原子化的、具体的、可验证的
5. 【语言】检测用户输入的语言，并用相同语言记录事实
6. 【置信度】根据用户陈述的确定性程度设置置信度
   - 明确陈述：0.9-1.0
   - 推理得出：0.7-0.89
   - 不太确定：0.5-0.69

今天的日期是 %s。

═══════════════════════════════════════════════════════════════════════════════
📝 开始提取：
═══════════════════════════════════════════════════════════════════════════════

以下是用户和AI助手之间的对话。
你需要从对话中提取关于用户的相关事实和偏好（如果有的话）。
必须按照上述 JSON 格式返回（包含 "facts" 数组，每个事实包含 fact、category、confidence 三个字段）。
""", TODAY_DATE);

    /**
     * 中文记忆决策提示词
     * 针对中文优化的内存更新决策逻辑
     */
    public static final String CHINESE_MEMORY_DECISION_PROMPT = """
You are a smart memory manager which controls the memory of a system.
You can perform four operations: (1) add into the memory, (2) update the memory, (3) delete from the memory, and (4) no change.

Based on the above four operations, the memory will change.

Compare newly retrieved facts with the existing memory. For each new fact, decide whether to:
- ADD: Add it to the memory as a new element
- UPDATE: Update an existing memory element
- DELETE: Delete an existing memory element
- NONE: Make no change (if the fact is already present or irrelevant)

There are specific guidelines to select which operation to perform:

1. **Add**: If the retrieved facts contain new information not present in the memory, then you have to add it by generating a new ID in the id field.
- **Example**:
    - Old Memory:
        [
            {
                "id" : "0",
                "text" : "User is a software engineer"
            }
        ]
    - Retrieved facts: ["Name is John"]
    - New Memory:
        {
            "memory" : [
                {
                    "id" : "0",
                    "text" : "User is a software engineer",
                    "event" : "NONE"
                },
                {
                    "id" : "1",
                    "text" : "Name is John",
                    "event" : "ADD"
                }
            ]

        }

2. **Update**: If the retrieved facts contain information that is already present in the memory but the information is totally different, then you have to update it. 
If the retrieved fact contains information that conveys the same thing as the elements present in the memory, then you have to keep the fact which has the most information. 
Example (a) -- if the memory contains "User likes to play cricket" and the retrieved fact is "Loves to play cricket with friends", then update the memory with the retrieved facts.
Example (b) -- if the memory contains "Likes cheese pizza" and the retrieved fact is "Loves cheese pizza", then you do not need to update it because they convey the same information.
If the direction is to update the memory, then you have to update it.
Please keep in mind while updating you have to keep the same ID.
Please note to return the IDs in the output from the input IDs only and do not generate any new ID.
- **Example**:
    - Old Memory:
        [
            {
                "id" : "0",
                "text" : "I really like cheese pizza"
            },
            {
                "id" : "1",
                "text" : "User is a software engineer"
            },
            {
                "id" : "2",
                "text" : "User likes to play cricket"
            }
        ]
    - Retrieved facts: ["Loves chicken pizza", "Loves to play cricket with friends"]
    - New Memory:
        {
        "memory" : [
                {
                    "id" : "0",
                    "text" : "Loves cheese and chicken pizza",
                    "event" : "UPDATE",
                    "old_memory" : "I really like cheese pizza"
                },
                {
                    "id" : "1",
                    "text" : "User is a software engineer",
                    "event" : "NONE"
                },
                {
                    "id" : "2",
                    "text" : "Loves to play cricket with friends",
                    "event" : "UPDATE",
                    "old_memory" : "User likes to play cricket"
                }
            ]
        }


3. **Delete**: If the retrieved facts contain information that contradicts the information present in the memory, then you have to delete it. Or if the direction is to delete the memory, then you have to delete it.
Please note to return the IDs in the output from the input IDs only and do not generate any new ID.
- **Example**:
    - Old Memory:
        [
            {
                "id" : "0",
                "text" : "Name is John"
            },
            {
                "id" : "1",
                "text" : "Loves cheese pizza"
            }
        ]
    - Retrieved facts: ["Dislikes cheese pizza"]
    - New Memory:
        {
        "memory" : [
                {
                    "id" : "0",
                    "text" : "Name is John",
                    "event" : "NONE"
                },
                {
                    "id" : "1",
                    "text" : "Loves cheese pizza",
                    "event" : "DELETE"
                }
        ]
        }

4. **No Change**: If the retrieved facts contain information that is already present in the memory, then you do not need to make any changes.
- **Example**:
    - Old Memory:
        [
            {
                "id" : "0",
                "text" : "Name is John"
            },
            {
                "id" : "1",
                "text" : "Loves cheese pizza"
            }
        ]
    - Retrieved facts: ["Name is John"]
    - New Memory:
        {
        "memory" : [
                {
                    "id" : "0",
                    "text" : "Name is John",
                    "event" : "NONE"
                },
                {
                    "id" : "1",
                    "text" : "Loves cheese pizza",
                    "event" : "NONE"
                }
            ]
        }
""";

    /**
     * 构建包含全局摘要和局部记忆的完整提示词
     */
    public static String buildCompleteExtractionPrompt(String globalSummary, String recentMemories,
                                                       String userMessage, String aiResponse) {
        return String.format("""
%s

系统摘要（全局上下文）：
%s

最近记忆（局部上下文）：
%s

新消息：
用户：%s
AI：%s

请根据以上全局上下文、局部记忆和新消息，提取用户的关键信息，形成候选记忆。
遵循以下原则：
- 仅从用户消息中提取信息
- 如果用户自相矛盾，仅提取最新的信息
- 不要重复现有的记忆内容
- 按照指定的JSON格式返回
""", CHINESE_CANDIDATE_MEMORY_EXTRACTION_PROMPT,
                globalSummary.isEmpty() ? "（暂无全局摘要）" : globalSummary,
                recentMemories.isEmpty() ? "（暂无最近记忆）" : recentMemories,
                userMessage, aiResponse);
    }
}