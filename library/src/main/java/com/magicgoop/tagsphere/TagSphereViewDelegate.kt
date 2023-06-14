package com.magicgoop.tagsphere

import android.animation.PointFEvaluator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.graphics.*
import android.os.Handler
import android.text.TextPaint
import android.util.Log
import android.view.MotionEvent
import android.view.VelocityTracker
import android.view.View
import android.view.animation.DecelerateInterpolator
import com.magicgoop.tagsphere.item.TagItem
import com.magicgoop.tagsphere.utils.EasingFunction
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt


const val TAG = "TagSphereViewDelegate"

@SuppressLint("ClickableViewAccessibility")
internal class TagSphereViewDelegate constructor(
    private val view: View
) : View.OnTouchListener {

    companion object {
        const val DEFAULT_RADIUS = 2.5f
        const val DEFAULT_SENSITIVITY = 10
    }

    var easingFunction: ((t: Float) -> Float)? = { EasingFunction.easeOutExpo(it) }
        set(value) {
            field = value
            view.postInvalidateOnAnimation()
        }
    var rotateOnTouch: Boolean = true
    var radius: Float = DEFAULT_RADIUS
        set(value) {
            if (value in 1f..10f) {
                field = value
                //这个是之前的java一半不会这么写，java 如果写成set方法一般也是可以的
                updateSphere()
            }
        }
    var sensitivity: Int = DEFAULT_SENSITIVITY
        set(value) {
            if (value in 1..100) {
                radians = (Math.PI / 90f / sensitivity).toFloat()
                field = value
            }
        }

    var textPaint = TextPaint().apply {
        isAntiAlias = true
        style = Paint.Style.FILL
        color = Color.BLACK
        textSize = paintTextSize
    }
        set(value) {
            value.textAlign = Paint.Align.CENTER
            paintTextSize = value.textSize
            field = value
            view.invalidate()
        }

    private val adapter: TagAdapter = TagAdapter()
    private var onLongPressedListener: OnTagLongPressedListener? = null
    private var onTapListener: OnTagTapListener? = null

    //弧度
    private var radians: Float = (Math.PI / 90f / sensitivity).toFloat()
    private var flingAnimator: ValueAnimator? = null

    private var paintTextSize = 30f
    private var projectionDistance: Float = 0f
    private var sphereRadius = Float.MIN_VALUE
    private val viewCenter = PointF()

    private val handler = Handler()

    private val gestureListener: OnGestureListener = object : OnGestureListener {
        override fun cancel() {
            cancelFlingAnim()
        }

        override fun onDeltaChanged(deltaX: Float, deltaY: Float) {
            adapter.getTags().forEach { tag ->
                tag.run {
                    //默认这两个就是0
                    rotateY(deltaX * radians)
                    rotateX(deltaY * radians)
                    /**
                     * Projection 是投影的意思
                     * updateProjectionX x轴方向的投影
                     * updateProjectionY Y轴方向的投影
                     */
                    updateProjectionX(width / 2f, radius, projectionDistance, padding.left)
                    updateProjectionY(height / 2f, radius, projectionDistance, padding.top)
                    maybeUpdateSphereRadius(getProjectionX(), getProjectionY())
                }
            }
            adapter.getTags().sort()
            view.postInvalidateOnAnimation()
        }

        override fun onFling(velocityX: Float, velocityY: Float) {
            startFling(velocityX, velocityY)
        }

        override fun onSingleTap(posX: Float, posY: Float) {
            if (isInsideProjection(posX, posY)) {
                adapter.getTagByProjectionPoint(posX - padding.left, posY - padding.top)?.let {
                    onTapListener?.onTap(it)
                }
            }
        }

        override fun onLongPressed(posX: Float, posY: Float) {
            adapter.getTagByProjectionPoint(posX - padding.left, posY - padding.top)?.let {
                onLongPressedListener?.onLongPressed(it)
            }
        }
    }
    private val gestureDelegate: GestureDelegate

    private var width: Float = 0f
    private var height: Float = 0f
    private var padding: Rect = Rect()

    init {
        //回调的就是 onTouch  这个方法
        view.setOnTouchListener(this)
        gestureDelegate = GestureDelegate(view.context, gestureListener)
    }

    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        if (event != null && event.action and MotionEvent.ACTION_MASK == MotionEvent.ACTION_DOWN) {
            v?.parent?.requestDisallowInterceptTouchEvent(true)
        }
        event?.let {
            processTouchEvent(event)
        }
        return if (rotateOnTouch) {
            //可以修改一下，如果这个不消耗的话，执行那个滑动
            gestureDelegate.onTouchEvent(event)
        } else {
            //回调了view的默认的操作
            view.onTouchEvent(event)
            /**
             * 无论如何都需要消耗一下再他这个范围的之内的点击事件的
             *
             * event?.let {
             *       processTouchEvent(event)
             *  }
             *
             *  类似于上面的代码，如果按照默认的代码业务逻辑处理 view.onTouchEvent(event)不接收事件
             *  只能处理一个 ACTION_DOWN 事件，后续的事件就无法继续执行了
             */
            true
        }
    }

    @Volatile
    var isTouch = false
    private fun processTouchEvent(ev: MotionEvent) {
        val action = ev.action
        //and MotionEvent.ACTION_MASK
        Log.i(TAG, "---processTouchEvent---------$action-------")
        when (action) {
            MotionEvent.ACTION_DOWN -> {
                isTouch = true
                Log.i(TAG, "---processTouchEvent---------ACTION_DOWN-------")
            }

            MotionEvent.ACTION_MOVE -> {
                isTouch = true
                Log.i(TAG, "---processTouchEvent---------ACTION_MOVE-------")
            }

            MotionEvent.ACTION_CANCEL -> {
                isTouch = false
                Log.i(TAG, "---processTouchEvent---------ACTION_CANCEL-------")
            }

            MotionEvent.ACTION_UP -> {
                isTouch = false
                Log.i(TAG, "---processTouchEvent---------ACTION_UP-------")
            }
        }
    }

    fun updateViewSize() {
        padding.left = view.paddingLeft
        padding.right = view.paddingRight
        padding.top = view.paddingTop
        padding.bottom = view.paddingBottom
        //这几个可以不再去计算，因为我们一般不会设置padding

        this.width = view.width.toFloat() - padding.left - padding.right
        this.height = view.height.toFloat() - padding.top - padding.bottom
        //这个计算也没有什么问题
        viewCenter.x = width / 2f + padding.left
        viewCenter.y = height / 2f + padding.top

        //投影的距离选择一个小的
        projectionDistance = min(width, height)

        updateSphere()
    }

    /**
     * 执行了view的分发方法，并没有太多的子view
     *
     * 也就是没有执行addView的这个操作
     *
     * 直接开始绘制的
     * 也就是前面看到的没有onMeasure这个操作
     */
    fun onDraw(canvas: Canvas) {
        adapter.getTags().forEach { tag ->
            val x = tag.getProjectionX()
            val y = tag.getProjectionY()
            if (x >= 0 && x < view.width + paintTextSize && y >= 0 && y < view.height + paintTextSize) {
                tag.drawSelf(x, y, canvas, textPaint, easingFunction)
            }
        }
    }

    fun addTagItem(tag: TagItem) {
        adapter.addTag(tag)
        updateSphere()
    }

    fun addTagItems(list: List<TagItem>) {
        adapter.addTagList(list)
        updateSphere()
    }

    fun removeTagItem(tag: TagItem) {
        adapter.removeTag(tag)
        updateSphere()
    }

    fun clearAllTags() {
        adapter.clearAll()
        updateSphere()
    }

    fun startFling(vX: Float, vY: Float) {
        cancelFlingAnim()
        flingAnimator =
            ValueAnimator.ofObject(PointFEvaluator(), PointF(vX / 100, vY / 100), PointF(0f, 0f))
                .apply {
                    duration = 1000
                    addUpdateListener {
                        (it.animatedValue as PointF).run {
                            gestureListener.onDeltaChanged(x, y)
                        }
                    }
                    interpolator = DecelerateInterpolator()
                }
        flingAnimator?.start()
    }

    fun setLongPressedListener(listener: OnTagLongPressedListener?) {
        onLongPressedListener = listener
    }

    fun setOnTapListener(listener: OnTagTapListener?) {
        onTapListener = listener
    }

    private fun cancelFlingAnim() {
        flingAnimator?.cancel()
    }

    /**
     * 这个就是自动的默认的旋转动画
     */
    fun startAnimation(deltaX: Float, deltaY: Float) {
        val delay = 1000 / 60L
        handler.postDelayed(object : Runnable {
            override fun run() {
                //现在在触摸状态的话就直接忽略吧
//                Log.i(TAG, "----startAnimation-----------${isTouch}---")
                if (!isTouch) {
                    gestureListener.onDeltaChanged(deltaX, -deltaY)
                }
                handler.postDelayed(this, delay)
            }

        }, delay)
    }

    fun stopAnimation() {
        handler.removeCallbacksAndMessages(null)
    }

    /**
     * 通过数据观察，得到的
     * ${height / 2f}和${width / 2f} 一定是不一样的，因为我们的手机屏幕从感官上也是不一样的
     *
     * 下面的数据是从一部手机上得到的 高度，宽度 和球形的半径
     * 847.0==========540.0==================464.6786
     * 从上面的数据可以发现并没有超过我们的最小宽度的一半，也就是宽度的一半
     *
     *
     *   x 是投影到 x轴上的距离
     *   y是投影到 y轴上的距离
     *
     *    x - viewCenter.x 是得到一个半径的距离
     *    y - viewCenter.y 是得到一个半径的距离
     *
     */
    private fun maybeUpdateSphereRadius(x: Float, y: Float) {
        /**
         * sphereRadius = Float.MIN_VALUE 初始值没有意义
         *
         * 这个就是一个球体的半径，这样整个就圆起来了
         */
        val temp = sphereRadius
        sphereRadius = max(sphereRadius, x - viewCenter.x)
        sphereRadius = max(sphereRadius, y - viewCenter.y)
//        Log.i(
//            TAG,
//            "=====${height / 2f}==========${width / 2f}==================$temp=========$sphereRadius==========(x - viewCenter.x):${x - viewCenter.x}=======(y - viewCenter.y):${y - viewCenter.y}========="
//        )
    }

    private fun isInsideProjection(posX: Float, posY: Float): Boolean {
        val dx = viewCenter.x - posX
        val dy = viewCenter.y - posY
        return sqrt(dx * dx + dy * dy) <= sphereRadius + paintTextSize
    }

    private fun updateSphere() {
        gestureListener.onDeltaChanged(0f, 0f)
    }
}