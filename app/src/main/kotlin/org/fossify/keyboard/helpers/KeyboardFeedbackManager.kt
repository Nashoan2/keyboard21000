package org.fossify.keyboard.helpers

import android.content.Context
import android.media.AudioManager
import android.view.View
import org.fossify.keyboard.extensions.config
import org.fossify.keyboard.extensions.safeStorageContext

/**
 * Helper for keypress audio (vibration disabled per user request).
 */
class KeyboardFeedbackManager(private val context: Context) {

    private val config: Config
        get() = context.safeStorageContext.config

    private val audioManager by lazy {
        context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    }

    /**
     * Vibrate disabled completely per user request.
     */
    fun vibrateIfNeeded(view: View) {
        // No-op: Vibration completely removed
    }

    /**
     * Cursor haptic disabled completely per user request.
     */
    fun performHapticHandleMove(view: View) {
        // No-op: Vibration completely removed
    }

    /**
     * Play keypress sound disabled completely per user request.
     */
    fun playKeypressSoundIfNeeded(code: Int) {
        // No-op: Sound completely removed per user request
    }

    /**
     * Perform feedback for a keypress (sound and vibration completely removed).
     */
    fun performKeypressFeedback(view: View, keyCode: Int) {
        // No-op: Feedback completely removed per user request
    }
}
