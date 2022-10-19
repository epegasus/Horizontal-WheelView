package dev.epegasus.wheel.helper

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.animation.ValueAnimator.AnimatorUpdateListener
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.animation.DecelerateInterpolator
import android.view.animation.Interpolator
import dev.epegasus.wheel.StraightenWheelView
import kotlin.math.abs
import kotlin.math.roundToInt

internal class TouchHandler(private val view: StraightenWheelView) : SimpleOnGestureListener() {

    private lateinit var settlingAnimator: ValueAnimator

    private val gestureDetector: GestureDetector = GestureDetector(view.context, this)
    private var scrollState = StraightenWheelView.SCROLL_STATE_IDLE
    private var listener: StraightenWheelView.Listener? = null
    private var snapToMarks = false

    fun setListener(listener: StraightenWheelView.Listener?) {
        this.listener = listener
    }

    fun setSnapToMarks(snapToMarks: Boolean) {
        this.snapToMarks = snapToMarks
    }

    fun onTouchEvent(event: MotionEvent): Boolean {
        gestureDetector.onTouchEvent(event)
        val action = event.actionMasked
        if (scrollState != StraightenWheelView.SCROLL_STATE_SETTLING && (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_CANCEL)) {
            if (snapToMarks) {
                playSettlingAnimation(findNearestMarkAngle(view.radiansAngle))
            } else {
                updateScrollStateIfRequired(StraightenWheelView.SCROLL_STATE_IDLE)
            }
        }
        return true
    }

    override fun onDown(e: MotionEvent): Boolean {
        cancelFling()
        return true
    }

    fun cancelFling() {
        if (scrollState == StraightenWheelView.SCROLL_STATE_SETTLING) {
            settlingAnimator.cancel()
        }
    }

    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
        val newAngle = view.radiansAngle + distanceX * SCROLL_ANGLE_MULTIPLIER
        view.radiansAngle = newAngle
        updateScrollStateIfRequired(StraightenWheelView.SCROLL_STATE_DRAGGING)
        return true
    }

    private fun updateScrollStateIfRequired(newState: Int) {
        if (listener != null && scrollState != newState) {
            scrollState = newState
            //listener!!.onScrollStateChanged(newState)
        }
    }

    override fun onFling(e1: MotionEvent, e2: MotionEvent, velocityX: Float, velocityY: Float): Boolean {
        var endAngle = view.radiansAngle - velocityX * FLING_ANGLE_MULTIPLIER
        if (snapToMarks) {
            endAngle = findNearestMarkAngle(endAngle).toFloat().toDouble()
        }
        playSettlingAnimation(endAngle)
        return true
    }

    private fun findNearestMarkAngle(angle: Double): Double {
        val step = 2 * Math.PI / view.marksCount
        return (angle / step).roundToInt() * step
    }

    private fun playSettlingAnimation(endAngle: Double) {
        updateScrollStateIfRequired(StraightenWheelView.SCROLL_STATE_SETTLING)
        val startAngle = view.radiansAngle
        val duration = (abs(startAngle - endAngle) * SETTLING_DURATION_MULTIPLIER).toInt()
        settlingAnimator = ValueAnimator.ofFloat(startAngle.toFloat(), endAngle.toFloat()).setDuration(duration.toLong())
        settlingAnimator.interpolator = INTERPOLATOR
        settlingAnimator.addUpdateListener(flingAnimatorListener)
        settlingAnimator.addListener(animatorListener)
        settlingAnimator.start()
    }

    private val flingAnimatorListener = AnimatorUpdateListener { animation -> view.radiansAngle = (animation.animatedValue as Float).toDouble() }
    private val animatorListener: Animator.AnimatorListener = object : AnimatorListenerAdapter() {
        override fun onAnimationEnd(animation: Animator) {
            updateScrollStateIfRequired(StraightenWheelView.SCROLL_STATE_IDLE)
        }
    }

    companion object {
        private const val SCROLL_ANGLE_MULTIPLIER = 0.002f
        private const val FLING_ANGLE_MULTIPLIER = 0.0002f
        private const val SETTLING_DURATION_MULTIPLIER = 1000
        private val INTERPOLATOR: Interpolator = DecelerateInterpolator(2.5f)
    }
}