package com.huajuan.aispace.data

import android.content.Context
import androidx.annotation.StringRes
import com.huajuan.aispace.R
import com.huajuan.aispace.i18n.AppLocaleManager
import com.huajuan.aispace.i18n.LocalizedResources

enum class AssistantCategory(
    val id: String,
    @StringRes val labelRes: Int
) {
    All("all", R.string.assistant_category_all),
    Efficiency("efficiency", R.string.assistant_category_efficiency),
    Writing("writing", R.string.assistant_category_writing),
    Coding("coding", R.string.assistant_category_coding),
    Learning("learning", R.string.assistant_category_learning),
    Life("life", R.string.assistant_category_life),
    Fun("fun", R.string.assistant_category_fun),
    User("user", R.string.assistant_category_user)
}

data class BuiltInAssistantSpec(
    val id: String,
    @StringRes val nameRes: Int,
    @StringRes val descriptionRes: Int,
    @StringRes val promptRes: Int,
    val category: AssistantCategory,
    val emoji: String
)

data class BuiltInAssistantDefaults(
    val assistantId: String,
    val name: String,
    val systemPrompt: String
)

object BuiltInAssistants {
    private val specs: List<BuiltInAssistantSpec> = listOf(
        BuiltInAssistantSpec("eff_1", R.string.assistant_eff_1_name, R.string.assistant_eff_1_description, R.string.assistant_eff_1_prompt, AssistantCategory.Efficiency, "✨"),
        BuiltInAssistantSpec("eff_2", R.string.assistant_eff_2_name, R.string.assistant_eff_2_description, R.string.assistant_eff_2_prompt, AssistantCategory.Efficiency, "📝"),
        BuiltInAssistantSpec("eff_3", R.string.assistant_eff_3_name, R.string.assistant_eff_3_description, R.string.assistant_eff_3_prompt, AssistantCategory.Efficiency, "✉️"),
        BuiltInAssistantSpec("eff_4", R.string.assistant_eff_4_name, R.string.assistant_eff_4_description, R.string.assistant_eff_4_prompt, AssistantCategory.Efficiency, "🧠"),
        BuiltInAssistantSpec("write_1", R.string.assistant_write_1_name, R.string.assistant_write_1_description, R.string.assistant_write_1_prompt, AssistantCategory.Writing, "💖"),
        BuiltInAssistantSpec("write_2", R.string.assistant_write_2_name, R.string.assistant_write_2_description, R.string.assistant_write_2_prompt, AssistantCategory.Writing, "🎬"),
        BuiltInAssistantSpec("write_3", R.string.assistant_write_3_name, R.string.assistant_write_3_description, R.string.assistant_write_3_prompt, AssistantCategory.Writing, "🔤"),
        BuiltInAssistantSpec("dev_1", R.string.assistant_dev_1_name, R.string.assistant_dev_1_description, R.string.assistant_dev_1_prompt, AssistantCategory.Coding, "🛠️"),
        BuiltInAssistantSpec("dev_2", R.string.assistant_dev_2_name, R.string.assistant_dev_2_description, R.string.assistant_dev_2_prompt, AssistantCategory.Coding, "#️⃣"),
        BuiltInAssistantSpec("dev_3", R.string.assistant_dev_3_name, R.string.assistant_dev_3_description, R.string.assistant_dev_3_prompt, AssistantCategory.Coding, "🌳"),
        BuiltInAssistantSpec("learn_1", R.string.assistant_learn_1_name, R.string.assistant_learn_1_description, R.string.assistant_learn_1_prompt, AssistantCategory.Learning, "📚"),
        BuiltInAssistantSpec("learn_2", R.string.assistant_learn_2_name, R.string.assistant_learn_2_description, R.string.assistant_learn_2_prompt, AssistantCategory.Learning, "🗣️"),
        BuiltInAssistantSpec("learn_3", R.string.assistant_learn_3_name, R.string.assistant_learn_3_description, R.string.assistant_learn_3_prompt, AssistantCategory.Learning, "🧪"),
        BuiltInAssistantSpec("life_1", R.string.assistant_life_1_name, R.string.assistant_life_1_description, R.string.assistant_life_1_prompt, AssistantCategory.Life, "🍳"),
        BuiltInAssistantSpec("life_2", R.string.assistant_life_2_name, R.string.assistant_life_2_description, R.string.assistant_life_2_prompt, AssistantCategory.Life, "💬"),
        BuiltInAssistantSpec("life_3", R.string.assistant_life_3_name, R.string.assistant_life_3_description, R.string.assistant_life_3_prompt, AssistantCategory.Life, "🎁"),
        BuiltInAssistantSpec("life_4", R.string.assistant_life_4_name, R.string.assistant_life_4_description, R.string.assistant_life_4_prompt, AssistantCategory.Life, "🧭"),
        BuiltInAssistantSpec("fun_1", R.string.assistant_fun_1_name, R.string.assistant_fun_1_description, R.string.assistant_fun_1_prompt, AssistantCategory.Fun, "🎮"),
        BuiltInAssistantSpec("fun_2", R.string.assistant_fun_2_name, R.string.assistant_fun_2_description, R.string.assistant_fun_2_prompt, AssistantCategory.Fun, "🔮"),
        BuiltInAssistantSpec("fun_3", R.string.assistant_fun_3_name, R.string.assistant_fun_3_description, R.string.assistant_fun_3_prompt, AssistantCategory.Fun, "🫶")
    )

    fun resolveAgents(context: Context, localeOverride: String? = null): List<Agent> =
        specs.map { spec ->
            Agent(
                id = spec.id,
                name = LocalizedResources.getString(context, spec.nameRes, localeOverride),
                description = LocalizedResources.getString(context, spec.descriptionRes, localeOverride),
                systemPrompt = LocalizedResources.getString(context, spec.promptRes, localeOverride),
                category = spec.category.id,
                iconResId = 0,
                emoji = spec.emoji
            )
        }

    fun categoryIds(): List<String> = listOf(
        AssistantCategory.All.id,
        AssistantCategory.Efficiency.id,
        AssistantCategory.Writing.id,
        AssistantCategory.Coding.id,
        AssistantCategory.Learning.id,
        AssistantCategory.Life.id,
        AssistantCategory.Fun.id
    )

    fun resolveCategoryLabel(context: Context, categoryId: String, localeOverride: String? = null): String {
        val category = AssistantCategory.entries.firstOrNull { it.id == categoryId } ?: AssistantCategory.User
        return LocalizedResources.getString(context, category.labelRes, localeOverride)
    }

    fun defaultValuesFor(context: Context, assistantId: String, localeOverride: String? = null): BuiltInAssistantDefaults? {
        val spec = specs.firstOrNull { it.id == assistantId } ?: return null
        return BuiltInAssistantDefaults(
            assistantId = assistantId,
            name = LocalizedResources.getString(context, spec.nameRes, localeOverride),
            systemPrompt = LocalizedResources.getString(context, spec.promptRes, localeOverride)
        )
    }

    fun allLocalizedDefaultValues(context: Context, assistantId: String): List<BuiltInAssistantDefaults> =
        AppLocaleManager.supportedContentLanguages().mapNotNull { locale ->
            defaultValuesFor(context, assistantId, locale)
        }
}
