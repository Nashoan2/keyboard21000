package org.fossify.keyboard.extensions

import android.content.Context
import org.fossify.commons.models.RadioItem
import org.fossify.keyboard.R
import org.fossify.keyboard.helpers.LANGUAGE_ARABIC
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_ASSET
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_COLEMAK
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_COLEMAKDH
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_DVORAK
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_NIRO
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_QWERTZ
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_SOUL
import org.fossify.keyboard.helpers.LANGUAGE_ENGLISH_WORKMAN

fun Context.getSelectedLanguagesSorted(): List<Int> {
    return config.selectedLanguages
        .map { it to getKeyboardLanguageText(it) }
        .sortedBy { it.second }
        .map { it.first }
}

fun Context.getKeyboardLanguagesRadioItems(): ArrayList<RadioItem> {
    return getSelectedLanguagesSorted()
        .map { RadioItem(it, getKeyboardLanguageText(it)) }
        .toMutableList() as ArrayList<RadioItem>
}

fun Context.getKeyboardLanguageText(language: Int): String {
    return when (language) {
        LANGUAGE_ARABIC -> getString(R.string.translation_arabic)
        LANGUAGE_ENGLISH_ASSET -> "${getString(R.string.translation_english)} (Asset)"
        LANGUAGE_ENGLISH_COLEMAK -> "${getString(R.string.translation_english)} (Colemak)"
        LANGUAGE_ENGLISH_COLEMAKDH -> "${getString(R.string.translation_english)} (Colemak-DH)"
        LANGUAGE_ENGLISH_DVORAK -> "${getString(R.string.translation_english)} (DVORAK)"
        LANGUAGE_ENGLISH_NIRO -> "${getString(R.string.translation_english)} (Niro)"
        LANGUAGE_ENGLISH_QWERTZ -> "${getString(R.string.translation_english)} (QWERTZ)"
        LANGUAGE_ENGLISH_SOUL -> "${getString(R.string.translation_english)} (Soul)"
        LANGUAGE_ENGLISH_WORKMAN -> "${getString(R.string.translation_english)} (Workman)"
        else -> "${getString(R.string.translation_english)} (QWERTY)"
    }
}
