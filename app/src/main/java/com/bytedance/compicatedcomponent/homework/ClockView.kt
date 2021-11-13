package com.bytedance.compicatedcomponent.homework

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View
import kotlin.math.cos
import kotlin.math.sin

/**
 *  author : neo
 *  time   : 2021/10/25
 *  desc   :
 */
class ClockView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    companion object {
        private const val FULL_ANGLE = 360

        private const val CUSTOM_ALPHA = 140
        private const val FULL_ALPHA = 255

        private const val POINTER_TYPE_SECOND : Byte = 3
        private const val POINTER_TYPE_MINUTES : Byte = 2
        private const val POINTER_TYPE_HOURS : Byte = 1

        private const val DEFAULT_PRIMARY_COLOR: Int = Color.WHITE
        private const val DEFAULT_SECONDARY_COLOR: Int = Color.LTGRAY

        private const val DEFAULT_DEGREE_STROKE_WIDTH = 0.010f

        private const val RIGHT_ANGLE = 90

        private const val UNIT_DEGREE = (6 * Math.PI / 180).toFloat() // 一个小格的度数
    }

    private var panelRadius = 200.0f // 表盘半径

    private var hourPointerLength = 0f // 指针长度

    private var minutePointerLength = 0f
    private var secondPointerLength = 0f

    private var resultWidth = 0
    private  var centerX: Int = 0
    private  var centerY: Int = 0
    private  var radius: Int = 0

    private var degreesColor = 0

    private var currentTime: Int = 0
    private var nowHours: Byte = 0
    private var nowMinutes: Byte = 0
    private var nowSeconds: Byte = 0
    private var touchPointerType: Byte = -1

    private val needlePaint: Paint

    init {
        degreesColor = DEFAULT_PRIMARY_COLOR
        needlePaint = Paint(Paint.ANTI_ALIAS_FLAG)
        needlePaint.style = Paint.Style.FILL_AND_STROKE
        needlePaint.strokeCap = Paint.Cap.ROUND
        needlePaint.textAlign = Paint.Align.CENTER
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
        val size: Int
        val width = measuredWidth
        val height = measuredHeight
        val widthWithoutPadding = width - paddingLeft - paddingRight
        val heightWithoutPadding = (height - paddingTop - paddingBottom) / 4 * 3
        size = if (widthWithoutPadding > heightWithoutPadding) {
            heightWithoutPadding
        } else {
            widthWithoutPadding
        }
        setMeasuredDimension(size + paddingLeft + paddingRight, size / 3 * 4 + paddingTop + paddingBottom)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        handler.postDelayed({ invalidate() },
            if(touchPointerType == (-1).toByte()) System.currentTimeMillis() % 500 + 1 else 1000)
        resultWidth = if (height / 4 * 3 > width) width else height / 4 * 3
        centerX = width / 2
        centerY = height / 2
        radius = resultWidth / 2
        panelRadius = radius.toFloat()
        hourPointerLength = panelRadius * 0.4f
        minutePointerLength = panelRadius * 0.55f
        secondPointerLength = panelRadius * 0.7f
        drawDegrees(canvas)
        drawHoursValues(canvas)
        currentTime = if (touchPointerType == (-1).toByte())
            (System.currentTimeMillis() / 1000 % 43200).toInt()
            else (currentTime + 1) % 43200
        drawNeedles(canvas)

        // todo 1: 每一秒刷新一次，让指针动起来
    }

    private fun drawDegrees(canvas: Canvas) {
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            style = Paint.Style.FILL_AND_STROKE
            strokeCap = Paint.Cap.ROUND
            strokeWidth = resultWidth * DEFAULT_DEGREE_STROKE_WIDTH
            color = degreesColor
        }
        val rPadded: Int = (resultWidth * 0.49f).toInt()
        val rEnd: Int = (resultWidth * 0.46f).toInt()
        var i = 0
        while (i < FULL_ANGLE) {
            if (i % RIGHT_ANGLE != 0 && i % 15 != 0) {
                paint.alpha = CUSTOM_ALPHA
            } else {
                paint.alpha = FULL_ALPHA
            }
            val startX = (centerX + rPadded * cos(Math.toRadians(i.toDouble())))
            val startY = (centerX - rPadded * sin(Math.toRadians(i.toDouble())))
            val stopX = (centerX + rEnd * cos(Math.toRadians(i.toDouble())))
            val stopY = (centerX - rEnd * sin(Math.toRadians(i.toDouble())))
            canvas.drawLine(
                startX.toFloat(),
                startY.toFloat(),
                stopX.toFloat(),
                stopY.toFloat(),
                paint
            )
            i += 6
        }
    }

    /**
     * Draw Hour Text Values, such as 1 2 3 ...
     *
     * @param canvas
     */
    private fun drawHoursValues(canvas: Canvas) {
        // Default Color:
        // - hoursValuesColor
        needlePaint.color = DEFAULT_SECONDARY_COLOR
        needlePaint.strokeWidth = resultWidth * DEFAULT_DEGREE_STROKE_WIDTH * 0.5f
        needlePaint.textSize = 48f
        canvas.drawText("12", centerX.toFloat(), centerY - resultWidth * 0.4f, needlePaint)
        canvas.drawText("3", centerX + resultWidth * 0.42f, centerY + resultWidth * 0.02f, needlePaint)
        canvas.drawText("6", centerX.toFloat(), centerY + resultWidth * 0.44f, needlePaint)
        canvas.drawText("9", centerX - resultWidth * 0.42f, centerY + resultWidth * 0.02f, needlePaint)
        needlePaint.textSize = 72f
        canvas.drawText("reset", centerX.toFloat(), centerY + resultWidth * 0.58f, needlePaint)
    }

    /**
     * Draw hours, minutes needles
     * Draw progress that indicates hours needle disposition.
     *
     * @param canvas
     */
    private fun drawNeedles(canvas: Canvas) {
        nowHours = (currentTime / 3600).toByte()
        nowMinutes = (currentTime / 60 % 60).toByte()
        nowSeconds = (currentTime % 60).toByte()
        canvas.drawText(String.format("%02d : %02d : %02d", nowHours, nowMinutes, nowSeconds),
            centerX.toFloat(), centerY - resultWidth * 0.52f, needlePaint)
        needlePaint.strokeWidth = resultWidth * DEFAULT_DEGREE_STROKE_WIDTH
        // 画秒针
        drawPointer(canvas, POINTER_TYPE_SECOND, nowSeconds)
        // 画分针
        // todo 2: 画分针
        drawPointer(canvas, POINTER_TYPE_MINUTES, nowMinutes)

        // 画时针
        drawPointer(canvas, POINTER_TYPE_HOURS, (5 * nowHours + nowMinutes / 12).toByte())
    }


    private fun drawPointer(canvas: Canvas, pointerType: Byte, value: Byte) {
        val degree: Float
        var pointerHeadXY = FloatArray(2)
        when (pointerType) {
            POINTER_TYPE_HOURS -> {
                degree = value * UNIT_DEGREE
                needlePaint.color = Color.WHITE
                pointerHeadXY = getPointerHeadXY(hourPointerLength, degree)
            }
            POINTER_TYPE_MINUTES -> {
                degree = value * UNIT_DEGREE
                needlePaint.color = Color.BLUE
                pointerHeadXY = getPointerHeadXY(minutePointerLength, degree)
            }
            POINTER_TYPE_SECOND -> {
                degree = value * UNIT_DEGREE
                needlePaint.color = Color.GREEN
                pointerHeadXY = getPointerHeadXY(secondPointerLength, degree)
            }
        }
        canvas.drawLine(
            centerX.toFloat(), centerY.toFloat(),
            pointerHeadXY[0], pointerHeadXY[1], needlePaint
        )
    }

    private fun getPointerHeadXY(pointerLength: Float, degree: Float): FloatArray {
        val xy = FloatArray(2)
        xy[0] = centerX + pointerLength * sin(degree)
        xy[1] = centerY - pointerLength * cos(degree)
        return xy
    }
}