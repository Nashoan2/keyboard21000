package org.fossify.keyboard.helpers

import android.content.Context
import org.fossify.keyboard.R

val ALLOWED_EMOJIS = listOf(
    "😂", "⬆️", "👇", "☑️", "🫡", "👋", "👆", "🤍", "😔", "🤩", "🤲", "🙏", "❤️", "🌹", "👍"
)

/**
 * Returns only the 15 user-requested emojis.
 */
fun parseRawEmojiSpecsFile(context: Context, path: String): MutableList<EmojiData> {
    return ALLOWED_EMOJIS.map { emoji ->
        EmojiData(
            category = "smileys_emotion",
            emoji = emoji,
            variants = emptyList()
        )
    }.toMutableList()
}

fun parseRawJsonSpecsFile(context: Context, path: String): HashMap<String, String> {
    return HashMap()
}

data class EmojiData(
    val category: String,
    val emoji: String,
    val variants: List<String>
)

fun getCategoryIconRes(category: String): Int = R.drawable.ic_emoji_category_smileys

fun getCategoryTitleRes(category: String) = R.string.emojis
