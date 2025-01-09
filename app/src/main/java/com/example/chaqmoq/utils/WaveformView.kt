package com.example.chaqmoq.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.util.Log
import android.view.View

class WaveformView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var waveform: List<Float> = emptyList()
    private var progress: Float = 0f
    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
    }
    private val progressPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 2f
    }

    fun setWaveform(data: List<Float>) {
        waveform = data
        invalidate()
    }

    fun setProgress(value: Float) {
//        Log.d("progress", value.toString())
        progress = value
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (waveform.isEmpty()) return

        val widthPerSample = width / waveform.size.toFloat()
        val centerY = height / 2f

        waveform.forEachIndexed { index, amplitude ->
            val x = index * widthPerSample
            val y = centerY - amplitude * height / 2
            val paint = if (index < progress * waveform.size) progressPaint else paint
            canvas.drawLine(x, centerY, x, y, paint)
        }
    }
}
