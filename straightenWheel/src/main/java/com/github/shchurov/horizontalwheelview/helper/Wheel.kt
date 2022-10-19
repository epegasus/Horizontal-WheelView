package com.github.shchurov.horizontalwheelview.helper

import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import com.github.shchurov.horizontalwheelview.StraightenWheelView
import com.github.shchurov.horizontalwheelview.utils.ConversionUtils
import java.util.Arrays
import kotlin.math.sin

internal class Wheel(private val view: StraightenWheelView) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var marksCount = 0
    private var normalColor = 0
    private var activeColor = 0
    private var showActiveRange = false
    private lateinit var gaps: FloatArray
    private lateinit var shades: FloatArray
    private lateinit var scales: FloatArray
    private val colorSwitches = intArrayOf(-1, -1, -1)
    private var viewportHeight = 0
    private var normalMarkWidth = 0
    private var normalMarkHeight = 0
    private var zeroMarkWidth = 0
    private var zeroMarkHeight = 0
    private var cursorCornersRadius = 0
    private var maxVisibleMarksCount = 0
    private val cursorRect = RectF()

    init {
        initDpSizes()
    }

    private fun initDpSizes() {
        normalMarkWidth = convertToPx(DP_NORMAL_MARK_WIDTH)
        zeroMarkWidth = convertToPx(DP_ZERO_MARK_WIDTH)
        cursorCornersRadius = convertToPx(DP_CURSOR_CORNERS_RADIUS)
    }

    private fun convertToPx(dp: Int): Int {
        return ConversionUtils.convertToPx(dp, view.resources)
    }

    fun setMarksCount(marksCount: Int) {
        this.marksCount = marksCount
        maxVisibleMarksCount = marksCount / 2 + 1
        gaps = FloatArray(maxVisibleMarksCount)
        shades = FloatArray(maxVisibleMarksCount)
        scales = FloatArray(maxVisibleMarksCount)
    }

    fun setNormalColor(color: Int) {
        normalColor = color
    }

    fun setActiveColor(color: Int) {
        activeColor = color
    }

    fun setShowActiveRange(show: Boolean) {
        showActiveRange = show
    }

    fun onSizeChanged() {
        viewportHeight = view.height - view.paddingTop - view.paddingBottom
        normalMarkHeight = (viewportHeight * NORMAL_MARK_RELATIVE_HEIGHT).toInt()
        zeroMarkHeight = (viewportHeight * ZERO_MARK_RELATIVE_HEIGHT).toInt()
        setupCursorRect()
    }

    private fun setupCursorRect() {
        val cursorHeight = (viewportHeight * CURSOR_RELATIVE_HEIGHT).toInt()
        cursorRect.top = (view.paddingTop + (viewportHeight - cursorHeight) / 2).toFloat()
        cursorRect.bottom = cursorRect.top + cursorHeight
        val cursorWidth = convertToPx(DP_CURSOR_WIDTH)
        cursorRect.left = ((view.width - cursorWidth) / 2).toFloat()
        cursorRect.right = cursorRect.left + cursorWidth
    }

    fun getMarksCount(): Int {
        return marksCount
    }

    fun onDraw(canvas: Canvas) {
        val step = 2 * Math.PI / marksCount
        var offset = (Math.PI / 2 - view.radiansAngle) % step
        if (offset < 0) {
            offset += step
        }
        setupGaps(step, offset)
        setupShadesAndScales(step, offset)
        val zeroIndex = calcZeroIndex(step)
        setupColorSwitches(step, offset, zeroIndex)
        drawMarks(canvas, zeroIndex)
        drawCursor(canvas)
    }

    private fun setupGaps(step: Double, offset: Double) {
        gaps[0] = sin(offset / 2).toFloat()
        var sum = gaps[0]
        var angle = offset
        var n = 1
        while (angle + step <= Math.PI) {
            gaps[n] = sin(angle + step / 2).toFloat()
            sum += gaps[n]
            angle += step
            n++
        }
        val lastGap = sin((Math.PI + angle) / 2).toFloat()
        sum += lastGap
        if (n != gaps.size) {
            gaps[gaps.size - 1] = -1f
        }
        val k = view.width / sum
        for (i in gaps.indices) {
            if (gaps[i] != -1f) {
                gaps[i] *= k
            }
        }
    }

    private fun setupShadesAndScales(step: Double, offset: Double) {
        var angle = offset
        for (i in 0 until maxVisibleMarksCount) {
            val sin = sin(angle)
            shades[i] = (1 - SHADE_RANGE * (1 - sin)).toFloat()
            scales[i] = (1 - SCALE_RANGE * (1 - sin)).toFloat()
            angle += step
        }
    }

    private fun calcZeroIndex(step: Double): Int {
        val twoPi = 2 * Math.PI
        val normalizedAngle = (view.radiansAngle + Math.PI / 2 + twoPi) % twoPi
        return if (normalizedAngle > Math.PI) {
            -1
        } else ((Math.PI - normalizedAngle) / step).toInt()
    }

    private fun setupColorSwitches(step: Double, offset: Double, zeroIndex: Int) {
        if (!showActiveRange) {
            Arrays.fill(colorSwitches, -1)
            return
        }
        val angle = view.radiansAngle
        var afterMiddleIndex = 0
        if (offset < Math.PI / 2) {
            afterMiddleIndex = ((Math.PI / 2 - offset) / step).toInt() + 1
        }
        if (angle > 3 * Math.PI / 2) {
            colorSwitches[0] = 0
            colorSwitches[1] = afterMiddleIndex
            colorSwitches[2] = zeroIndex
        } else if (angle >= 0) {
            colorSwitches[0] = 0.coerceAtLeast(zeroIndex)
            colorSwitches[1] = afterMiddleIndex
            colorSwitches[2] = -1
        } else if (angle < -3 * Math.PI / 2) {
            colorSwitches[0] = 0
            colorSwitches[1] = zeroIndex
            colorSwitches[2] = afterMiddleIndex
        } else if (angle < 0) {
            colorSwitches[0] = afterMiddleIndex
            colorSwitches[1] = zeroIndex
            colorSwitches[2] = -1
        }
    }

    private fun drawMarks(canvas: Canvas, zeroIndex: Int) {
        var x = view.paddingLeft.toFloat()
        var color = normalColor
        var colorPointer = 0
        for (i in gaps.indices) {
            if (gaps[i] == -1f) {
                break
            }
            x += gaps[i]
            while (colorPointer < 3 && i == colorSwitches[colorPointer]) {
                color = if (color == normalColor) activeColor else normalColor
                colorPointer++
            }
            if (i != zeroIndex) {
                drawNormalMark(canvas, x, scales[i], shades[i], color)
            } else {
                drawZeroMark(canvas, x, scales[i], shades[i])
            }
        }
    }

    private fun drawNormalMark(canvas: Canvas, x: Float, scale: Float, shade: Float, color: Int) {
        val height = normalMarkHeight * scale
        val top = view.paddingTop + (viewportHeight - height) / 2
        val bottom = top + height
        paint.strokeWidth = normalMarkWidth.toFloat()
        paint.color = applyShade(color, shade)
        canvas.drawLine(x, top, x, bottom, paint)
    }

    private fun applyShade(color: Int, shade: Float): Int {
        val r = (Color.red(color) * shade).toInt()
        val g = (Color.green(color) * shade).toInt()
        val b = (Color.blue(color) * shade).toInt()
        return Color.rgb(r, g, b)
    }

    private fun drawZeroMark(canvas: Canvas, x: Float, scale: Float, shade: Float) {
        val height = zeroMarkHeight * scale
        val top = view.paddingTop + (viewportHeight - height) / 2
        val bottom = top + height
        paint.strokeWidth = zeroMarkWidth.toFloat()
        paint.color = applyShade(activeColor, shade)
        canvas.drawLine(x, top, x, bottom, paint)
    }

    private fun drawCursor(canvas: Canvas) {
        paint.strokeWidth = 0f
        paint.color = activeColor
        canvas.drawRoundRect(cursorRect, cursorCornersRadius.toFloat(), cursorCornersRadius.toFloat(), paint)
    }

    companion object {
        private const val DP_CURSOR_CORNERS_RADIUS = 1
        private const val DP_NORMAL_MARK_WIDTH = 1
        private const val DP_ZERO_MARK_WIDTH = 2
        private const val DP_CURSOR_WIDTH = 3
        private const val NORMAL_MARK_RELATIVE_HEIGHT = 0.6f
        private const val ZERO_MARK_RELATIVE_HEIGHT = 0.8f
        private const val CURSOR_RELATIVE_HEIGHT = 1f
        private const val SHADE_RANGE = 0.7f
        private const val SCALE_RANGE = 0.1f
    }
}