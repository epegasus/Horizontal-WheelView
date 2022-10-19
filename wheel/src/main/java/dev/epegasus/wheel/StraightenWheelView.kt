package dev.epegasus.wheel

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import com.github.shchurov.horizontalwheelview.R
import dev.epegasus.wheel.helper.TouchHandler
import dev.epegasus.wheel.helper.Wheel
import dev.epegasus.wheel.utils.ConversionUtils.convertToPx
import kotlin.math.nextTowards

class StraightenWheelView(context: Context?, attrs: AttributeSet) : View(context, attrs) {

    private val wheel = Wheel(this)
    private val touchHandler = TouchHandler(this)
    private var listener: Listener? = null
    private var onlyPositiveValues = false
    private var endLock = false
    private var angle = 0.0

    init {
        readAttrs(attrs)
    }

    private fun readAttrs(attrs: AttributeSet) {
        val a = context.obtainStyledAttributes(attrs, R.styleable.StraightenWheelView)
        val marksCount = a.getInt(R.styleable.StraightenWheelView_sw_marksCount, DEFAULT_MARKS_COUNT)
        val normalColor = a.getColor(R.styleable.StraightenWheelView_sw_normalColor, DEFAULT_NORMAL_COLOR)
        val activeColor = a.getColor(R.styleable.StraightenWheelView_sw_activeColor, DEFAULT_ACTIVE_COLOR)
        val showActiveRange = a.getBoolean(R.styleable.StraightenWheelView_sw_showActiveRange, DEFAULT_SHOW_ACTIVE_RANGE)
        val snapToMarks = a.getBoolean(R.styleable.StraightenWheelView_sw_snapToMarks, DEFAULT_SNAP_TO_MARKS)
        wheel.setMarksCount(marksCount)
        wheel.setNormalColor(normalColor)
        wheel.setActiveColor(activeColor)
        wheel.setShowActiveRange(showActiveRange)
        touchHandler.setSnapToMarks(snapToMarks)
        endLock = a.getBoolean(R.styleable.StraightenWheelView_sw_endLock, DEFAULT_END_LOCK)
        onlyPositiveValues = a.getBoolean(R.styleable.StraightenWheelView_sw_onlyPositiveValues, DEFAULT_ONLY_POSITIVE_VALUES)
        a.recycle()
    }

    fun setListener(listener: Listener?) {
        this.listener = listener
        touchHandler.setListener(listener)
    }

    private fun checkEndLock(radians: Double): Boolean {
        if (!endLock) {
            return false
        }
        var hit = false
        if (radians >= 2 * Math.PI) {
            angle = (2 * Math.PI).nextTowards(Double.NEGATIVE_INFINITY)
            hit = true
        } else if (onlyPositiveValues && radians < 0) {
            angle = 0.0
            hit = true
        } else if (radians <= -2 * Math.PI) {
            angle = (-2 * Math.PI).nextTowards(Double.POSITIVE_INFINITY)
            hit = true
        }
        if (hit) {
            touchHandler.cancelFling()
        }
        return hit
    }

    var radiansAngle: Double
        get() = angle
        set(radians) {
            if (!checkEndLock(radians)) {
                angle = radians % (2 * Math.PI)
            }
            if (onlyPositiveValues && angle < 0) {
                angle += 2 * Math.PI
            }
            invalidate()
            if (listener != null) {
                listener!!.onRotationChanged(angle)
            }
        }
    var degreesAngle: Double
        get() = radiansAngle * 180 / Math.PI
        set(degrees) {
            val radians = degrees * Math.PI / 180
            radiansAngle = radians
        }
    var completeTurnFraction: Double
        get() = radiansAngle / (2 * Math.PI)
        set(fraction) {
            val radians = fraction * 2 * Math.PI
            radiansAngle = radians
        }

    fun setOnlyPositiveValues(onlyPositiveValues: Boolean) {
        this.onlyPositiveValues = onlyPositiveValues
    }

    fun setEndLock(lock: Boolean) {
        endLock = lock
    }

    fun setShowActiveRange(show: Boolean) {
        wheel.setShowActiveRange(show)
        invalidate()
    }

    fun setNormaColor(color: Int) {
        wheel.setNormalColor(color)
        invalidate()
    }

    fun setActiveColor(color: Int) {
        wheel.setActiveColor(color)
        invalidate()
    }

    fun setSnapToMarks(snapToMarks: Boolean) {
        touchHandler.setSnapToMarks(snapToMarks)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouchEvent(event: MotionEvent): Boolean {
        return touchHandler.onTouchEvent(event)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        wheel.onSizeChanged()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val resolvedWidthSpec = resolveMeasureSpec(widthMeasureSpec, DP_DEFAULT_WIDTH)
        val resolvedHeightSpec = resolveMeasureSpec(heightMeasureSpec, DP_DEFAULT_HEIGHT)
        super.onMeasure(resolvedWidthSpec, resolvedHeightSpec)
    }

    private fun resolveMeasureSpec(measureSpec: Int, dpDefault: Int): Int {
        val mode = MeasureSpec.getMode(measureSpec)
        if (mode == MeasureSpec.EXACTLY) {
            return measureSpec
        }
        var defaultSize = convertToPx(dpDefault, resources)
        if (mode == MeasureSpec.AT_MOST) {
            defaultSize = defaultSize.coerceAtMost(MeasureSpec.getSize(measureSpec))
        }
        return MeasureSpec.makeMeasureSpec(defaultSize, MeasureSpec.EXACTLY)
    }

    override fun onDraw(canvas: Canvas) {
        wheel.onDraw(canvas)
    }

    var marksCount: Int
        get() = wheel.getMarksCount()
        set(marksCount) {
            wheel.setMarksCount(marksCount)
            invalidate()
        }

    open class Listener {
        open fun onRotationChanged(radians: Double) {}
    }

    companion object {
        private const val DP_DEFAULT_WIDTH = 200
        private const val DP_DEFAULT_HEIGHT = 32
        private const val DEFAULT_MARKS_COUNT = 40
        private const val DEFAULT_NORMAL_COLOR = -0x1
        private const val DEFAULT_ACTIVE_COLOR = -0xab5310
        private const val DEFAULT_SHOW_ACTIVE_RANGE = true
        private const val DEFAULT_SNAP_TO_MARKS = false
        private const val DEFAULT_END_LOCK = false
        private const val DEFAULT_ONLY_POSITIVE_VALUES = false
        const val SCROLL_STATE_IDLE = 0
        const val SCROLL_STATE_DRAGGING = 1
        const val SCROLL_STATE_SETTLING = 2
    }
}