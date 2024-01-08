package com.example.myapplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class GraphicOverlay : View {
    private var boundingBox: RectF? = null
    private var recognizedName: String? = null
    private var confidence: String? = null

    constructor(context: Context?) : super(context)
    constructor(context: Context?, attrs: AttributeSet?) : super(context, attrs)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int) : super(context, attrs, defStyleAttr)
    constructor(context: Context?, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) : super(context, attrs, defStyleAttr, defStyleRes)

    fun updateOverlay(boundingBox: RectF?, recognizedName: String?, confidence: String?) {
        this.boundingBox = boundingBox
        this.recognizedName = recognizedName
        this.confidence = confidence
        postInvalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (boundingBox != null) {

            val paint = Paint()
            paint.color = Color.RED
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5.0f
            canvas.drawRect(boundingBox!!, paint)

            if (recognizedName != null && !recognizedName!!.isEmpty()) {
                paint.color = Color.WHITE
                paint.style = Paint.Style.FILL
                paint.textSize = 60.0f

                val textWidthName = paint.measureText(recognizedName)
                val xName = boundingBox!!.centerX() - textWidthName / 2
                val yName = boundingBox!!.top - 10 // Adjust the distance above the box
                canvas.drawText(recognizedName!!, xName, yName, paint)

                if (confidence != null && !confidence!!.isEmpty()) {
                    paint.color = Color.GREEN
                    paint.textSize = 50.0f
                    val textWidthConfidence = paint.measureText(confidence)
                    val xConfidence = boundingBox!!.centerX() - textWidthConfidence / 2
                    val yConfidence = yName + 40
                    canvas.drawText(confidence!!, xConfidence, yConfidence, paint)
                }
            }
        }
    }

    fun clearOverlay() {
        boundingBox = null
        recognizedName = null
        confidence = null
        postInvalidate()
    }
}