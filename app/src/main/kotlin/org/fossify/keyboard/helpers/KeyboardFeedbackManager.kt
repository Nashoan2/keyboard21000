package org.fossify.keyboard.helpers

import android.content.Context
import android.view.View

/**
 * Helper for keypress audio and vibration feedback.
 * All sound and vibration feedbacks are permanently removed per user request.
 */
class KeyboardFeedbackManager(private val context: Context) {

    fun vibrateIfNeeded(view: View? = null) {
        // Permanently disabled
    }

    fun performHapticHandleMove(view: View? = null) {
        // Permanently disabled
    }

    fun playKeypressSoundIfNeeded(code: Int) {
        // Permanently disabled
    }

    fun performKeypressFeedback(view: View? = null, keyCode: Int = 0) {
        // Permanently disabled
    }
}
