package com.huajuan.aispace.screens.assistant.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.PrimaryScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.huajuan.aispace.R
import com.huajuan.aispace.ui.theme.AppDimens

data class EmojiCategory(
    val id: String,
    val emojis: List<String>
)

object EmojiCatalog {
    val categories: List<EmojiCategory> = listOf(
        EmojiCategory(
            id = "smileys",
            emojis = listOf(
                "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
                "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
                "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
                "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
                "😖", "😫", "😩", "😢", "😭", "😤", "😠", "😡", "🤬", "🤯",
                "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗", "🤔",
                "🤭", "🤫", "🤥", "😶", "😐", "😑"
            )
        ),
        EmojiCategory(
            id = "people",
            emojis = listOf(
                "👍", "👎", "👌", "✌️", "🤞", "🤟", "🤘", "🤙", "👋", "🤚",
                "🖐️", "✋", "🖖", "👈", "👉", "👆", "👇", "☝️", "🙌", "👏",
                "🤝", "🙏", "💪", "🦾", "🧠", "👀", "👁️", "🧒", "👦", "👧",
                "🧑", "👨", "👩", "🧑‍🦱", "🧑‍🦰", "🧑‍🦳", "🧑‍🦲", "👶", "👴", "👵"
            )
        ),
        EmojiCategory(
            id = "nature",
            emojis = listOf(
                "🌱", "🌿", "☘️", "🍀", "🌵", "🌴", "🌳", "🌲", "🌼", "🌸",
                "🌺", "🌻", "🌹", "🌷", "🌞", "🌝", "🌙", "⭐", "🌟", "✨",
                "🌈", "☁️", "⛅", "🌤️", "🌧️", "⛈️", "🌩️", "❄️", "🌊", "🔥"
            )
        ),
        EmojiCategory(
            id = "animals",
            emojis = listOf(
                "🐶", "🐱", "🐭", "🐹", "🐰", "🦊", "🐻", "🐼", "🐨", "🐯",
                "🦁", "🐮", "🐷", "🐸", "🐵", "🐔", "🐧", "🐦", "🐤", "🐣",
                "🦆", "🦅", "🦉", "🐺", "🐗", "🐴", "🦄", "🐝", "🐛", "🦋",
                "🐌", "🐞", "🐜", "🐢", "🐍", "🦎", "🐙", "🦑", "🐠", "🐟",
                "🐬", "🐳", "🐋", "🦈"
            )
        ),
        EmojiCategory(
            id = "food",
            emojis = listOf(
                "🍎", "🍊", "🍋", "🍌", "🍉", "🍇", "🍓", "🫐", "🍒", "🍑",
                "🍍", "🥭", "🥥", "🥝", "🍅", "🥑", "🥕", "🌽", "🥔", "🍠",
                "🥐", "🍞", "🥖", "🧀", "🥚", "🍳", "🥞", "🧇", "🥓", "🍖",
                "🍗", "🍔", "🍟", "🍕", "🌭", "🌮", "🌯", "🥗", "🍝", "🍜",
                "🍣", "🍤", "🥟", "🍚", "🍙", "🍢", "🍡", "🍦", "🍧", "🍨",
                "🍩", "🍪", "🎂", "🍰", "🧁", "🍫", "🍬", "🍭", "🍮", "🥤",
                "🧋", "☕", "🍵", "🧃", "🍺", "🍻", "🍷", "🥂", "🍸", "🍹"
            )
        ),
        EmojiCategory(
            id = "activity",
            emojis = listOf(
                "⚽", "🏀", "🏈", "⚾", "🎾", "🏐", "🎱", "🏓", "🏸", "🥅",
                "🏒", "🏑", "🥍", "🥊", "🥋", "🎽", "🛹", "🛼", "🚲", "🏍️",
                "🏎️", "🏆", "🥇", "🥈", "🥉", "🎯", "🎳", "🎮", "🧩", "🎲",
                "🎸", "🎹", "🥁", "🎷", "🎺", "🎻", "🎨", "🧵", "🧶"
            )
        ),
        EmojiCategory(
            id = "travel",
            emojis = listOf(
                "🚗", "🚕", "🚙", "🚌", "🚎", "🚓", "🚑", "🚒", "🚐", "🚚",
                "🚛", "🚜", "🚲", "🛴", "🚃", "🚄", "🚅", "🚇", "🚉", "✈️",
                "🛫", "🛬", "🛩️", "🚀", "🛸", "🚁", "🛶", "⛵", "🚤", "🛳️",
                "⛴️", "🚢", "🗺️", "🧭", "🏝️", "🏜️", "🏔️", "⛰️", "🏕️", "🏖️",
                "🏟️", "🏛️", "🏗️", "🏠", "🏡", "🏢", "🏭"
            )
        ),
        EmojiCategory(
            id = "objects",
            emojis = listOf(
                "💡", "🔦", "🕯️", "📱", "💻", "🖥️", "⌨️", "🖱️", "🖨️", "🗜️",
                "📷", "📸", "🎥", "📺", "📻", "🎙️", "🎧", "⏰", "📚", "📖",
                "📝", "✏️", "🖊️", "📌", "📎", "🧷", "🧲", "🔧", "🔨", "🛠️",
                "🔩", "⚙️", "🧰", "🧪", "🧫", "🧬", "🩺", "💊", "💉", "🩹",
                "🪄", "🎁", "📦", "🧳", "🔑", "🔒", "🔓"
            )
        ),
        EmojiCategory(
            id = "symbols",
            emojis = listOf(
                "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "🤎", "💔",
                "❣️", "💕", "💞", "💓", "💗", "💖", "💘", "💝", "✅", "☑️",
                "✔️", "❌", "✖️", "⛔", "⚠️", "🛑", "🚫", "❓", "❔", "❗",
                "‼️", "💯", "🔥", "✨", "⭐", "🌟", "♻️", "🔔", "🔕", "📣"
            )
        ),
        EmojiCategory(
            id = "flags",
            emojis = listOf(
                "🇨🇳", "🇺🇸", "🇯🇵", "🇰🇷", "🇬🇧", "🇫🇷", "🇩🇪", "🇪🇸", "🇮🇹", "🇨🇦",
                "🇦🇺", "🇸🇬", "🇮🇳", "🇧🇷", "🇷🇺", "🇿🇦"
            )
        )
    )

    fun findCategoryIndex(emoji: String): Int {
        val index = categories.indexOfFirst { category -> emoji in category.emojis }
        return if (index >= 0) index else 0
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EmojiPickerBottomSheet(
    currentEmoji: String,
    onSelect: (String) -> Unit,
    onDismiss: () -> Unit
) {
    val categories = remember { EmojiCatalog.categories }
    var selectedIndex by remember(currentEmoji) {
        mutableIntStateOf(EmojiCatalog.findCategoryIndex(currentEmoji))
    }
    val selectedCategory = categories.getOrNull(selectedIndex) ?: categories.first()

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppDimens.screenPadding)
        ) {
            Text(
                text = stringResource(R.string.emoji_picker_title),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Spacer(modifier = Modifier.height(AppDimens.spacingS))
            PrimaryScrollableTabRow(
                selectedTabIndex = selectedIndex,
                modifier = Modifier.fillMaxWidth(),
                edgePadding = 0.dp,
                divider = {},
                containerColor = MaterialTheme.colorScheme.surface,
                contentColor = MaterialTheme.colorScheme.onSurface
            ) {
                categories.forEachIndexed { index, category ->
                    Tab(
                        selected = index == selectedIndex,
                        onClick = { selectedIndex = index },
                        text = {
                            Text(
                                text = localizedEmojiCategoryName(category.id),
                                fontWeight = if (index == selectedIndex) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selectedContentColor = MaterialTheme.colorScheme.primary,
                        unselectedContentColor = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Spacer(modifier = Modifier.height(AppDimens.spacingS))
            LazyVerticalGrid(
                columns = GridCells.Fixed(8),
                contentPadding = PaddingValues(bottom = AppDimens.spacingL),
                verticalArrangement = Arrangement.spacedBy(8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(selectedCategory.emojis) { emoji ->
                    val selected = emoji == currentEmoji
                    Box(
                        modifier = Modifier
                            .clip(MaterialTheme.shapes.small)
                            .background(
                                if (selected) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surfaceVariant
                            )
                            .clickable { onSelect(emoji) }
                            .padding(vertical = 6.dp, horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = emoji,
                            fontSize = 22.sp,
                            color = if (selected) MaterialTheme.colorScheme.onPrimaryContainer
                            else MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun localizedEmojiCategoryName(categoryId: String): String {
    return when (categoryId) {
        "smileys" -> stringResource(R.string.emoji_category_smileys)
        "people" -> stringResource(R.string.emoji_category_people)
        "nature" -> stringResource(R.string.emoji_category_nature)
        "animals" -> stringResource(R.string.emoji_category_animals)
        "food" -> stringResource(R.string.emoji_category_food)
        "activity" -> stringResource(R.string.emoji_category_activity)
        "travel" -> stringResource(R.string.emoji_category_travel)
        "objects" -> stringResource(R.string.emoji_category_objects)
        "symbols" -> stringResource(R.string.emoji_category_symbols)
        "flags" -> stringResource(R.string.emoji_category_flags)
        else -> categoryId
    }
}
