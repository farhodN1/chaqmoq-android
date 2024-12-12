package com.example.chaqmoq.ui.customViews

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

class WaveformView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private val paintPlayed = Paint().apply {
        color = Color.GREEN
        style = Paint.Style.FILL

    }
    private val paintUnplayed = Paint().apply {
        color = Color.GRAY
        style = Paint.Style.FILL
    }

    private var amplitudes: List<Int> = emptyList()
    private var currentPosition: Int = 0

    fun setAmplitudes(amps: List<Int>) {
        Log.d("amps", amps.toString())
        amplitudes = amps
        invalidate()
    }

    fun setCurrentPosition(position: Int) {
        currentPosition = position
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (amplitudes.isEmpty()) return

        val barSpacing = 5f // Space between bars
        val barWidth = 5f // Fixed width for all bars

        amplitudes.forEachIndexed { index, amp ->
            val paint = if (index <= currentPosition) paintPlayed else paintUnplayed

            val barHeight = amp / 255f * 100f
            val centerY = height / 2f
            val top = centerY - barHeight / 2
            val bottom = centerY + barHeight / 2

            val left = index * (barWidth + barSpacing)
            val right = left + barWidth

            canvas.drawRoundRect(
                left,
                top,
                right,
                bottom,
                5f,
                5f,
                paint
            )
        }
    }
}