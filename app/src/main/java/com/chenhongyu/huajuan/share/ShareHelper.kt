package com.chenhongyu.huajuan.share

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import com.chenhongyu.huajuan.data.Conversation
import com.chenhongyu.huajuan.data.Message
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import com.chenhongyu.huajuan.utils.ThinkTagProcessor

object ShareHelper {

    // Internal DTOs matching ChatLogGenerator structure
    data class ChatData(val meta: Meta, val systemPrompt: String, val dialogue: List<DialogueMessage>)
    data class Meta(val time: String, val userName: String, val userSignature: String, val aiName: String, val aiModel: String)
    data class DialogueMessage(val role: String, val content: String)

    // HTML template taken from ChatLogGenerator.java (JS ${...} expressions must remain literal in output)
    private val HTML_TEMPLATE = """<!DOCTYPE html>
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
                    <span class="meta-value">${'$'}{chatData.meta.time}</span>
                </div>
                <div class="meta-group" style="text-align:right; align-items: flex-end;">
                    <span class="meta-label">Model Architecture</span>
                    <span class="meta-value">${'$'}{chatData.meta.aiModel}</span>
                </div>
                <div class="meta-group">
                    <span class="meta-label">User Identity</span>
                    <div class="meta-value">${'$'}{chatData.meta.userName}</div>
                    <div class="meta-sub">${'$'}{chatData.meta.userSignature}</div>
                </div>
                <div class="meta-group" style="text-align:right; align-items: flex-end;">
                    <span class="meta-label">AI Persona</span>
                    <div class="meta-value" style="color:var(--accent-color)">${'$'}{chatData.meta.aiName}</div>
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
                block.className = `message-block ${'$'}{roleClass}`;
                const renderedContent = marked.parse(msg.content);
                block.innerHTML = `
                    <div class="role-name">${'$'}{displayName}</div>
                    <div class="message-content">${'$'}{renderedContent}</div>
                `;
                chatContainer.appendChild(block);
            });
        }
        document.addEventListener('DOMContentLoaded', render);
    </script>
</body>
</html>
"""

    // New helper: generate HTML file and return the File (does not start any intent)
    suspend fun generateConversationHtmlFile(context: Context, conversation: Conversation, messages: List<Message>, username: String?, signature: String?, modelName: String?): File {
        return withContext(Dispatchers.IO) {
            // Build meta time string similar to Java generator
            val sdf = SimpleDateFormat("MMM dd, yyyy · HH:mm", Locale.ENGLISH)
            val timeStr = sdf.format(Date()).uppercase(Locale.ENGLISH)

            val meta = Meta(time = timeStr,
                userName = username ?: "",
                userSignature = signature ?: "",
                aiName = conversation.roleName,
                aiModel = modelName ?: ""
            )

            val dialogue = messages.map { m ->
                DialogueMessage(role = if (m.isUser) "user" else "ai", content = ThinkTagProcessor.removeThinkTags(m.text))
            }

            val chatData = ChatData(meta = meta, systemPrompt = conversation.systemPrompt ?: "", dialogue = dialogue)

            val gson = Gson()
            val cleanJsonData = gson.toJson(chatData)

            val finalHtml = HTML_TEMPLATE.replace("{{DATA_PLACEHOLDER}}", cleanJsonData)

            // Save to cache
            val fileName = "huajuan_conversation_${conversation.id}.html"
            val cacheFile = File(context.cacheDir, fileName)
            cacheFile.writeText(finalHtml, Charsets.UTF_8)
            cacheFile
        }
    }

    /**
     * Export conversation to HTML using the template above. This builds a JSON object matching the template's
     * `chatData` shape and injects it into HTML.
     */
    suspend fun shareConversationAsHtml(context: Context, conversation: Conversation, messages: List<Message>, username: String?, signature: String?, modelName: String?) {
        withContext(Dispatchers.IO) {
            try {
                val cacheFile = generateConversationHtmlFile(context, conversation, messages, username, signature, modelName)

                // Share via FileProvider
                val authority = "com.chenhongyu.huajuan.fileprovider"
                val uri: Uri = FileProvider.getUriForFile(context, authority, cacheFile)

                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/html"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                val chooser = Intent.createChooser(shareIntent, "分享对话")
                chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(chooser)

            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

}
