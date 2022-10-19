package com.github.shchurov.horizontalwheelview.utils

import android.content.res.Resources
import android.util.TypedValue

internal object ConversionUtils {

    @JvmStatic
    fun convertToPx(dp: Int, resources: Resources): Int {
        val dm = resources.displayMetrics
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp.toFloat(), dm).toInt()
    }
}