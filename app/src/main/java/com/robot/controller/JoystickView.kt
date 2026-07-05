package com.robot.controller

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

/**
 * Simple analog joystick. Reports normalized x,y (-1..1) via listener.
 * x: -1 = full left, 1 = full right
 * y: -1 = full back, 1 = full forward
 */
class JoystickView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    interface Listener {
        fun onMove(x: Float, y: Float)
        fun onRelease()
    }

    var listener: Listener? = null

    private val basePaint = Paint().apply {
        color = Color.parseColor("#1E2126")
        isAntiAlias = true
    }
    private val ringPaint = Paint().apply {
        color = Color.parseColor("#00C2A8")
        style = Paint.Style.STROKE
        strokeWidth = 6f
        isAntiAlias = true
    }
    private val knobPaint = Paint().apply {
        color = Color.parseColor("#00C2A8")
        isAntiAlias = true
    }

    private var centerX = 0f
    private var centerY = 0f
    private var baseRadius = 0f
    private var knobRadius = 0f
    private var knobX = 0f
    private var knobY = 0f

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        centerX = w / 2f
        centerY = h / 2f
        baseRadius = min(w, h) / 2f - 20f
        knobRadius = baseRadius / 3f
        knobX = centerX
        knobY = centerY
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        canvas.drawCircle(centerX, centerY, baseRadius, basePaint)
        canvas.drawCircle(centerX, centerY, baseRadius, ringPaint)
        canvas.drawCircle(knobX, knobY, knobRadius, knobPaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_MOVE -> {
                val dx = event.x - centerX
                val dy = event.y - centerY
                val dist = sqrt(dx * dx + dy * dy)
                if (dist <= baseRadius) {
                    knobX = event.x
                    knobY = event.y
                } else {
                    val angle = atan2(dy, dx)
                    knobX = centerX + baseRadius * kotlin.math.cos(angle)
                    knobY = centerY + baseRadius * kotlin.math.sin(angle)
                }
                invalidate()
                val normX = ((knobX - centerX) / baseRadius).coerceIn(-1f, 1f)
                val normY = (-(knobY - centerY) / baseRadius).coerceIn(-1f, 1f)
                listener?.onMove(normX, normY)
            }
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                knobX = centerX
                knobY = centerY
                invalidate()
                listener?.onRelease()
            }
        }
        return true
    }
}
