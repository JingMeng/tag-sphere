package com.magicgoop.tagsphere

/**
 * 自己写了一个收拾识处理的类
 */
interface OnGestureListener {
    fun cancel()
    fun onDeltaChanged(deltaX: Float, deltaY: Float)
    fun onFling(velocityX: Float, velocityY: Float)
    fun onSingleTap(posX: Float, posY: Float)
    fun onLongPressed(posX: Float, posY: Float)
}