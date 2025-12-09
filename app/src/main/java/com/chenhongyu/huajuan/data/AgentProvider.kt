package com.chenhongyu.huajuan.data

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*

/**
 * 智能体提供者，提供预设的智能体列表
 */
class AgentProvider {
    fun getAgents(): List<Agent> {
        return listOf(
            // 推荐类别
            Agent(
                id = "recommended_1",
                name = "全能助手",  // 角色名称
                description = "一个通用的AI助手，可以回答各种问题",
                systemPrompt = "你是一个全能的AI助手，能够回答各种问题，提供帮助和建议。",  // 系统提示词
                category = "推荐",
                iconResId = 0 // 这里暂时用0表示，实际应用中应该使用图标资源
            ),
            Agent(
                id = "recommended_2",
                name = "翻译专家",
                description = "专业翻译员，支持多语言互译",
                systemPrompt = "你是一个专业的翻译员，精通多种语言，能够准确地进行语言互译。",
                category = "推荐",
                iconResId = 0
            ),
            
            // 学习类别
            Agent(
                id = "study_1",
                name = "数学导师",
                description = "擅长数学领域的解题和讲解",
                systemPrompt = "你是一位数学导师，擅长各个数学领域，能够详细解释数学概念和解题过程。",
                category = "学习",
                iconResId = 0
            ),
            Agent(
                id = "study_2",
                name = "英语老师",
                description = "专业的英语学习指导老师",
                systemPrompt = "你是一位专业的英语老师，可以帮助用户学习英语语法、词汇和口语表达。",
                category = "学习",
                iconResId = 0
            ),
            Agent(
                id = "study_3",
                name = "编程专家",
                description = "多种编程语言的资深开发者",
                systemPrompt = "你是一位资深的软件开发工程师，精通多种编程语言和技术栈，能够帮助用户解决编程问题。",
                category = "学习",
                iconResId = 0
            ),
            
            // 工作类别
            Agent(
                id = "work_1",
                name = "产品经理",
                description = "协助产品规划和需求分析",
                systemPrompt = "你是一位经验丰富的产品经理，擅长市场分析、用户研究和产品规划。",
                category = "工作",
                iconResId = 0
            ),
            Agent(
                id = "work_2",
                name = "文案策划",
                description = "创意文案和营销方案撰写",
                systemPrompt = "你是一位专业的文案策划师，擅长创作吸引人的广告文案和营销内容。",
                category = "工作",
                iconResId = 0
            ),
            Agent(
                id = "work_3",
                name = "数据分析",
                description = "专业的数据分析师",
                systemPrompt = "你是一位专业的数据分析师，擅长数据处理、统计分析和可视化呈现。",
                category = "工作",
                iconResId = 0
            ),
            
            // 生活类别
            Agent(
                id = "life_1",
                name = "营养师",
                description = "专业的饮食和营养建议",
                systemPrompt = "你是一位专业的营养师，了解各类食物的营养价值，能够提供科学的饮食建议。",
                category = "生活",
                iconResId = 0
            ),
            Agent(
                id = "life_2",
                name = "旅行规划师",
                description = "定制个性化旅游路线",
                systemPrompt = "你是一位专业的旅行规划师，熟悉各地旅游资源，能够制定个性化的旅行计划。",
                category = "生活",
                iconResId = 0
            ),
            Agent(
                id = "life_3",
                name = "心理咨询师",
                description = "提供心理健康支持和建议",
                systemPrompt = "你是一位专业的心理咨询师，能够倾听用户的困扰并提供心理支持和建议。",
                category = "生活",
                iconResId = 0
            )
        )
    }
    
    fun getCategories(): List<String> {
        return listOf("推荐", "学习", "工作", "生活")
    }
}