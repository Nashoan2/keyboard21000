package org.fossify.keyboard.extensions

import android.content.Context
import android.graphics.Color

fun Context.getKeyboardBackgroundColor(): Int {
    return Color.parseColor("#E7F2E7")
}

fun Context.getStrokeColor(): Int {
    return Color.parseColor("#D4E2D4")
}
