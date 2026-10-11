package org.fossify.keyboard.extensions

import android.content.Context
import android.graphics.Color

fun Context.getKeyboardBackgroundColor(): Int {
    return Color.parseColor("#060B14")
}

fun Context.getStrokeColor(): Int {
    return Color.parseColor("#00D2FF")
}
