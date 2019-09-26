package com.androks.codeinputview

import android.content.Context
import android.content.res.Resources
import android.content.res.TypedArray
import android.util.AttributeSet
import androidx.annotation.ColorRes
import androidx.annotation.StyleableRes
import androidx.core.content.ContextCompat

inline fun Context.useStyledAttributes(
    set: AttributeSet?,
    @StyleableRes attrs: IntArray,
    func: TypedArray.() -> Unit
) {
    val typedArray = obtainStyledAttributes(set, attrs)
    typedArray.func()
    typedArray.recycle()
}

fun Context.getColorRes(@ColorRes color: Int): Int {
    return ContextCompat.getColor(this, color)
}

fun dip(value: Int): Int {
    return (value * Resources.getSystem().displayMetrics.density).toInt()
}