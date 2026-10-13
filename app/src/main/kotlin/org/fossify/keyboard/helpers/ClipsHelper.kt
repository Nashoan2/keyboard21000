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
        fun addDeletedText(text: String?) {
            // Text is cleared from system clipboard via clearSystemClipboardIfMatches
        }

        fun isDeletedText(text: String?): Boolean {
            // Never permanently block user from copying text again
            return false
        }

        fun clearDeletedText(text: String?) {
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
