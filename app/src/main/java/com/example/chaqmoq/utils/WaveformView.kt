package com.example.chaqmoq.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
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
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    private val progressPaint = Paint().apply {
        color = Color.RED
        strokeWidth = 4f
        strokeCap = Paint.Cap.ROUND
        style = Paint.Style.FILL
        isAntiAlias = true
    }

    fun setWaveform(data: List<Float>) {
        waveform = resampleTo50(data)
        invalidate()
    }

    fun setProgress(value: Float) {
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
            val barHeight = amplitude * height / 2f
            val startY = centerY - barHeight
            val endY = centerY + barHeight
            val paint = if (index < progress * waveform.size) progressPaint else paint

            canvas.drawLine(x, startY, x, endY, paint)
        }
    }

    fun resampleTo50(input: List<Float>): List<Float> {
        val targetSize = 50
        val inputSize = input.size

        if (inputSize == 0) return List(targetSize) { 0f }

        // If size is already 100, return as-is
        if (inputSize == targetSize) return input

        val output = MutableList(targetSize) { 0f }

        if (inputSize > targetSize) {
            // Downsample: average over segments
            val step = inputSize.toFloat() / targetSize
            for (i in 0 until targetSize) {
                val start = (i * step).toInt()
                val end = ((i + 1) * step).toInt().coerceAtMost(inputSize)
                output[i] = input.subList(start, end).average().toFloat()
            }
        } else {
            // Upsample: interpolate between values
            val scale = (inputSize - 1).toFloat() / (targetSize - 1)
            for (i in 0 until targetSize) {
                val pos = i * scale
                val idx = pos.toInt()
                val frac = pos - idx
                if (idx + 1 < inputSize) {
                    output[i] = input[idx] * (1 - frac) + input[idx + 1] * frac
                } else {
                    output[i] = input[idx]
                }
            }
        }

        return output
    }

}
