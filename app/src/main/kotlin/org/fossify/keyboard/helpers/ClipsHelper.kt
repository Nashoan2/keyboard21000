package org.fossify.keyboard.helpers

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import org.fossify.keyboard.extensions.clipsDB
import org.fossify.keyboard.models.Clip
import java.util.Collections

class ClipsHelper(val context: Context) {

    companion object {
        // Keeps track of deleted texts so they never resurrect or re-appear
        val deletedTexts: MutableSet<String> = Collections.synchronizedSet(HashSet<String>())

        fun addDeletedText(text: String?) {
            val trimmed = text?.trim() ?: return
            if (trimmed.isNotEmpty()) {
                deletedTexts.add(trimmed)
            }
        }

        fun isDeletedText(text: String?): Boolean {
            val trimmed = text?.trim() ?: return true
            return deletedTexts.contains(trimmed)
        }

        fun clearDeletedText(text: String?) {
            val trimmed = text?.trim() ?: return
            deletedTexts.remove(trimmed)
        }

        fun clearSystemClipboardIfMatches(context: Context, text: String?) {
            val trimmed = text?.trim() ?: return
            try {
                val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val currentClipText = clipboardManager?.primaryClip?.getItemAt(0)?.text?.toString()?.trim()
                if (currentClipText == trimmed) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        clipboardManager?.clearPrimaryClip()
                    } else {
                        clipboardManager?.setPrimaryClip(ClipData.newPlainText("", ""))
                    }
                }
            } catch (ignored: Exception) {
            }
        }
    }

    // make sure clips have unique values
    fun insertClip(clip: Clip): Long {
        val trimmed = clip.value.trim()
        if (trimmed.isEmpty()) return -1L
        clearDeletedText(trimmed)
        return if (context.clipsDB.getClipWithValue(trimmed) == null) {
            context.clipsDB.insertOrUpdate(clip.copy(value = trimmed))
        } else {
            -1L
        }
    }
}
