import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

/**
 * 这是一个单文件工具类，用于读取JSON配置并生成包含对话记录的HTML文件。
 * 要求 JDK 15+ (为了支持文本块功能)
 */
public class ChatLogGenerator {

    // ==========================================
    // 1. 数据模型 (内部类)
    // ==========================================

    public static class ChatData {
        public Meta meta;
        public String systemPrompt;
        public List<Message> dialogue;
    }

    public static class Meta {
        public String time;
        public String userName;
        public String userSignature;
        public String aiName;
        public String aiModel;
    }

    public static class Message {
        public String role; // "user" or "ai"
        public String content;
    }

    // ==========================================
    // 2. HTML 模板 (基于之前的宋体/衬线设计)
    // ==========================================

    private static final String HTML_TEMPLATE = """
<!DOCTYPE html>
<html lang="zh-CN">
<head>
    <meta charset="UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1.0">
    <title>Conversation Log</title>
    <link rel="preconnect" href="https://fonts.googleapis.com">
    <link rel="preconnect" href="https://fonts.gstatic.com" crossorigin>
    <link href="https://fonts.googleapis.com/css2?family=EB+Garamond:ital,wght@0,400;0,500;0,700;1,400&family=JetBrains+Mono:wght@400&family=Noto+Serif+SC:wght@300;400;600;700&display=swap" rel="stylesheet">
    <script src="https://cdn.jsdelivr.net/npm/marked/marked.min.js"></script>
    <style>
        :root {
            --bg-color: #Fdfbf7;
            --text-primary: #1F1F1F;
            --text-secondary: #5C5C59;
            --accent-color: #8C3B3B;
            --user-color: #243D55;
            --line-color: #D6D4CE;
            --code-bg: #EAE8E4;
            --font-body: 'EB Garamond', 'Noto Serif SC', 'Songti SC', serif;
            --font-mono: 'JetBrains Mono', monospace;
            --spacing-base: 24px;
            --container-width: 720px;
        }
        * { box-sizing: border-box; margin: 0; padding: 0; }
        body {
            background-color: var(--bg-color);
            color: var(--text-primary);
            font-family: var(--font-body);
            line-height: 1.8;
            font-size: 17px;
            padding: 60px 20px;
            -webkit-font-smoothing: antialiased;
        }
        body::before {
            content: ""; position: fixed; top: 0; left: 0; width: 100%; height: 100%;
            background-image: url("data:image/svg+xml,%3Csvg viewBox='0 0 200 200' xmlns='http://www.w3.org/2000/svg'%3E%3Cfilter id='noiseFilter'%3E%3CfeTurbulence type='fractalNoise' baseFrequency='0.8' numOctaves='3' stitchTiles='stitch'/%3E%3C/filter%3E%3Crect width='100%25' height='100%25' filter='url(%23noiseFilter)' opacity='0.04'/%3E%3C/svg%3E");
            pointer-events: none; z-index: -1;
        }
        .container { max-width: var(--container-width); margin: 0 auto; }
        .header {
            border-bottom: 1px solid var(--text-primary);
            padding-bottom: var(--spacing-base); margin-bottom: 60px;
            display: grid; grid-template-columns: 1fr 1fr; gap: 24px;
        }
        .meta-group { display: flex; flex-direction: column; gap: 2px; }
        .meta-label {
            font-family: var(--font-mono); font-size: 10px; text-transform: uppercase;
            letter-spacing: 1.5px; color: var(--text-secondary); margin-bottom: 4px;
        }
        .meta-value { font-family: var(--font-body); font-weight: 700; font-size: 19px; letter-spacing: -0.01em; }
        .meta-sub { font-size: 14px; font-style: italic; color: var(--text-secondary); margin-top: 2px; }
        .system-prompt-wrapper { margin-bottom: 60px; }
        details.system-prompt {
            border: 1px solid transparent; border-top: 1px solid var(--line-color); border-bottom: 1px solid var(--line-color);
            transition: all 0.3s ease;
        }
        details.system-prompt[open] { border-color: var(--line-color); background-color: rgba(255,255,255,0.4); }
        summary {
            padding: 16px 0; font-family: var(--font-mono); font-size: 11px; color: var(--text-secondary);
            cursor: pointer; list-style: none; display: flex; align-items: center; justify-content: space-between; letter-spacing: 1px;
        }
        summary::after { content: "▼"; font-size: 9px; opacity: 0.5; }
        details[open] summary::after { content: "▲"; }
        .prompt-content {
            padding: 0 0 20px 0; font-family: var(--font-mono); font-size: 13px; color: var(--text-secondary);
            white-space: pre-wrap; line-height: 1.6; border-top: 1px dashed var(--line-color); margin-top: -1px; padding-top: 16px;
        }
        .chat-stream { display: flex; flex-direction: column; gap: 56px; }
        .message-block {
            position: relative; padding-left: 28px; opacity: 0; transform: translateY(15px);
            animation: fadeIn 0.8s cubic-bezier(0.22, 1, 0.36, 1) forwards;
        }
        .message-block::before {
            content: ''; position: absolute; left: 6px; top: 6px; bottom: 6px; width: 1px; background-color: var(--line-color);
        }
        .message-block.role-user::after {
            content: ''; position: absolute; left: 5px; top: 0; width: 3px; height: 3px; background: var(--user-color); border-radius: 50%;
        }
        .message-block.role-ai::after {
            content: ''; position: absolute; left: 5px; top: 0; width: 3px; height: 3px; background: var(--accent-color); border-radius: 50%;
        }
        .role-name {
            font-family: var(--font-mono); font-size: 12px; text-transform: uppercase; letter-spacing: 1px;
            margin-bottom: 12px; display: block; color: var(--text-secondary);
        }
        .message-content { font-size: 17px; color: var(--text-primary); font-weight: 400; }
        .message-content p { margin-bottom: 1.2em; text-align: justify; }
        .message-content p:last-child { margin-bottom: 0; }
        .message-content strong { font-weight: 700; color: #000; }
        .message-content em { font-family: 'EB Garamond', serif; font-style: italic; font-size: 1.05em; color: var(--text-secondary); }
        .message-content blockquote {
            border-left: none; position: relative; padding: 10px 20px; margin: 24px 0;
            color: var(--text-secondary); font-style: italic; background: rgba(0,0,0,0.02);
        }
        .message-content blockquote::before {
            content: "“"; font-family: 'EB Garamond', serif; font-size: 40px; position: absolute; top: -10px; left: 4px; color: var(--line-color); opacity: 0.5;
        }
        .message-content code {
            font-family: var(--font-mono); background: rgba(0,0,0,0.04); padding: 2px 6px; border-radius: 2px; font-size: 0.85em; color: #C04646;
        }
        .message-content pre {
            background: var(--bg-color); padding: 16px; border: 1px solid var(--line-color); overflow-x: auto; margin: 24px 0; box-shadow: 2px 2px 0 rgba(0,0,0,0.03);
        }
        .footer {
            margin-top: 100px; text-align: center; font-family: var(--font-mono); font-size: 11px;
            color: var(--text-secondary); letter-spacing: 2px; opacity: 0.6;
        }
        .footer::before { content: "***"; display: block; margin-bottom: 20px; font-family: var(--font-body); font-size: 20px; letter-spacing: 10px; }
        @keyframes fadeIn { to { opacity: 1; transform: translateY(0); } }
        @media (max-width: 600px) {
            body { padding: 40px 20px; }
            .header { grid-template-columns: 1fr; gap: 24px; border-bottom: none;}
            .message-block { padding-left: 20px; }
        }
    </style>
</head>
<body>
    <div class="container">
        <header class="header" id="headerInfo"></header>
        <div class="system-prompt-wrapper">
            <details class="system-prompt" open>
                <summary>SYSTEM_PROTOCOL</summary>
                <div class="prompt-content" id="systemPrompt"></div>
            </details>
        </div>
        <div class="chat-stream" id="chatStream"></div>
        <footer class="footer">
            <div id="footerInfo">END OF RECORD</div>
        </footer>
    </div>
    <script>
        // ==========================================
        // DATA INJECTION
        // ==========================================
        const chatData = {{DATA_PLACEHOLDER}};

        // ==========================================
        // RENDER LOGIC
        // ==========================================
        function render() {
            const headerHTML = `
                <div class="meta-group">
                    <span class="meta-label">Date Recorded</span>
                    <span class="meta-value">${chatData.meta.time}</span>
                </div>
                <div class="meta-group" style="text-align:right; align-items: flex-end;">
                    <span class="meta-label">Model Architecture</span>
                    <span class="meta-value">${chatData.meta.aiModel}</span>
                </div>
                <div class="meta-group">
                    <span class="meta-label">User Identity</span>
                    <div class="meta-value">${chatData.meta.userName}</div>
                    <div class="meta-sub">${chatData.meta.userSignature}</div>
                </div>
                <div class="meta-group" style="text-align:right; align-items: flex-end;">
                    <span class="meta-label">AI Persona</span>
                    <div class="meta-value" style="color:var(--accent-color)">${chatData.meta.aiName}</div>
                </div>
            `;
            document.getElementById('headerInfo').innerHTML = headerHTML;
            document.getElementById('systemPrompt').innerText = chatData.systemPrompt;
            
            const chatContainer = document.getElementById('chatStream');
            chatData.dialogue.forEach((msg, index) => {
                const block = document.createElement('div');
                const isAi = msg.role === 'ai';
                const roleClass = isAi ? 'role-ai' : 'role-user';
                const displayName = isAi ? chatData.meta.aiName : chatData.meta.userName;
                
                block.style.animationDelay = (index * 0.15) + 's';
                block.className = `message-block ${roleClass}`;
                const renderedContent = marked.parse(msg.content);
                block.innerHTML = `
                    <div class="role-name">${displayName}</div>
                    <div class="message-content">${renderedContent}</div>
                `;
                chatContainer.appendChild(block);
            });
        }
        document.addEventListener('DOMContentLoaded', render);
    </script>
</body>
</html>
    """;

    // ==========================================
    // 3. 核心生成方法
    // ==========================================

    public static void generateHtmlFile(String inputJsonString, String outputPath) throws IOException {
        // 1. 验证 JSON 格式 (通过解析它)
        ObjectMapper mapper = new ObjectMapper();
        ChatData data = mapper.readValue(inputJsonString, ChatData.class);

        // 2. 再次序列化为 String 以确保格式统一且安全 (避免直接拼接字符串带来的转义问题)
        String cleanJsonData = mapper.writeValueAsString(data);

        // 3. 将 JSON 数据注入 HTML 模板
        String finalHtml = HTML_TEMPLATE.replace("{{DATA_PLACEHOLDER}}", cleanJsonData);

        // 4. 写入文件
        Path path = Paths.get(outputPath);
        Files.writeString(path, finalHtml, StandardCharsets.UTF_8);

        System.out.println("成功生成 HTML 文件: " + path.toAbsolutePath());
    }

    // ==========================================
    // 4. Main 方法 (测试用)
    // ==========================================

    public static void main(String[] args) {
        // 模拟输入的 JSON 字符串 (实际使用中你可以从文件读取)
        String mockInputJson = """
            {
              "meta": {
                "time": "2024年 5月 20日, 14:30",
                "userName": "Designer_007",
                "userSignature": "追求极致的极简主义者",
                "aiName": "Claude",
                "aiModel": "Claude 3.5 Sonnet"
              },
              "systemPrompt": "你是一个美学专家，擅长排版和配色。你的回答应该简洁、优雅。",
              "dialogue": [
                {
                  "role": "user",
                  "content": "如何评价这个 **Java 生成的 HTML** 设计？"
                },
                {
                  "role": "ai",
                  "content": "这是一个非常*出色*的实现。\\n\\n1.  **自动化**：Java 后端处理数据逻辑。\\n2.  **前端渲染**：保留了 CSS 的高级质感（衬线体、噪点背景）。\\n\\n> “代码与美学的完美结合。”"
                }
              ]
            }
        """;

        try {
            generateHtmlFile(mockInputJson, "output_chat.html");
        } catch (IOException e) {
            e.printStackTrace();
            System.err.println("生成失败: " + e.getMessage());
        }
    }
}