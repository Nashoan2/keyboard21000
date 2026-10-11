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
     * Play keypress sound if enabled.
     */
    fun playKeypressSoundIfNeeded(code: Int) {
        val soundMode = config.soundOnKeypress
        if (soundMode == SOUND_NONE) return

        val effect = when (code) {
            MyKeyboard.KEYCODE_DELETE -> AudioManager.FX_KEYPRESS_DELETE
            MyKeyboard.KEYCODE_ENTER -> AudioManager.FX_KEYPRESS_RETURN
            MyKeyboard.KEYCODE_SPACE -> AudioManager.FX_KEYPRESS_SPACEBAR
            else -> AudioManager.FX_KEYPRESS_STANDARD
        }

        when (soundMode) {
            SOUND_SYSTEM -> audioManager.playSoundEffect(effect)
            SOUND_ALWAYS -> audioManager.playSoundEffect(effect, 1.0f)
        }
    }

    /**
     * Perform audio feedback for a keypress (vibration disabled).
     */
    fun performKeypressFeedback(view: View, keyCode: Int) {
        playKeypressSoundIfNeeded(keyCode)
    }
}
