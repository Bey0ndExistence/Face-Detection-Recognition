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
        postInvalidate() // Trigger redraw
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (boundingBox != null) {
            // Draw a border around the detected face
            val paint = Paint()
            paint.color = Color.RED
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 5.0f
            canvas.drawRect(boundingBox!!, paint)

            // Draw the recognized face's name and confidence level above the box
            if (recognizedName != null && !recognizedName!!.isEmpty()) {
                paint.color = Color.WHITE
                paint.style = Paint.Style.FILL
                paint.textSize = 60.0f

                // Draw recognized name
                val textWidthName = paint.measureText(recognizedName)
                val xName = boundingBox!!.centerX() - textWidthName / 2
                val yName = boundingBox!!.top - 10 // Adjust the distance above the box
                canvas.drawText(recognizedName!!, xName, yName, paint)

                // Draw confidence level
                if (confidence != null && !confidence!!.isEmpty()) {
                    paint.color = Color.GREEN // Choose the color for confidence level
                    paint.textSize = 50.0f // Choose the text size for confidence level
                    val textWidthConfidence = paint.measureText(confidence)
                    val xConfidence = boundingBox!!.centerX() - textWidthConfidence / 2
                    val yConfidence = yName + 40 // Adjust the distance below the recognized name
                    canvas.drawText(confidence!!, xConfidence, yConfidence, paint)
                }
            }
        }
    }

    //help me do a clear overlay
    fun clearOverlay() {
        boundingBox = null
        recognizedName = null
        confidence = null
        postInvalidate() // Trigger redraw
    }
}