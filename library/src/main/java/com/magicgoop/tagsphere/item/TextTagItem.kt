package com.magicgoop.tagsphere.item

import android.graphics.Canvas
import android.graphics.Rect
import android.text.TextPaint
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

/**
 * 用绘制二维的方式绘制出来了三维立体的图形
 */
class TextTagItem(
    val text: String
) : TagItem() {
    private var firstInit: Boolean = false
    private var rect: Rect = Rect()

    override fun drawSelf(
        x: Float,
        y: Float,
        canvas: Canvas,
        paint: TextPaint,
        easingFunction: ((t: Float) -> Float)?
    ) {
        /**
         * 首次需要测量一下
         */
        if (!firstInit) {
            paint.getTextBounds(text, 0, text.length, rect)
            firstInit = true
        }
        //透明度处理计算
        easingFunction?.let { calc ->
            val ease = calc(getEasingValue())
            val alpha = if (!ease.isNaN()) max(0, min(255, (255 * ease).roundToInt())) else 0
            paint.alpha = alpha
        } ?: run { paint.alpha = 255 }

        if (paint.alpha > 0) {
            canvas.drawText(
                text,
                x,
                y - rect.bottom + rect.height() / 2f,
                paint
            )
        }
    }
}