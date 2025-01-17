package com.magicgoop.tagpshere.example

import android.os.Bundle
import android.text.TextPaint
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.magicgoop.tagpshere.example.util.CustomOnSeekBarChangeListener
import com.magicgoop.tagpshere.example.util.EmojiConstants
import com.magicgoop.tagsphere.OnTagLongPressedListener
import com.magicgoop.tagsphere.OnTagTapListener
import com.magicgoop.tagsphere.item.TagItem
import com.magicgoop.tagsphere.item.TextTagItem
import com.magicgoop.tagsphere.utils.EasingFunction
import kotlinx.android.synthetic.main.fragment_playground.*
import kotlin.random.Random


/**
 *
 * NestedScrollView 啥也没干，甚至连一个id都没有
 *
 * 但是作为一个CoordinatorLayout的子类
 *
 * app:behavior_peekHeight="@dimen/sheet_peek"
 * app:layout_behavior="@string/bottom_sheet_behavior"
 *
 * 实现了我们想要的那种效果，滴滴的第步滑动效果
 * 在一半的时候也有判断
 *
 * fixme  不在点击范围，因为view的宽高过大，也默认给你返回了一个最近的图标了，这个应该修改一下view的宽高
 *
 *  没有针对双指的操作，如果加上双指的操作，需要设置最大和最小的范围
 */
class PlaygroundFragment : Fragment() {

    companion object {
        fun newInstance(): PlaygroundFragment = PlaygroundFragment()

        private const val MIN_SENSITIVITY = 1
        private const val MIN_RADIUS = 10f
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_playground, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        initTagView()
        initSettings()
    }

    private fun initTagView() {
        val samples = EmojiConstants.emojiCodePoints.size - 1
        tagView.setTextPaint(
            TextPaint().apply {
                isAntiAlias = true
                textSize = resources.getDimension(R.dimen.tag_text_size)
            }
        )
        (0..100).map {
            //头像也仅仅是一个文字的展示
            TextTagItem(
                text = String(
                    Character.toChars(EmojiConstants.emojiCodePoints[Random.nextInt(samples)])
                )
            )
        }.toList().let {
            tagView.addTagList(it)
        }
        tagView.setOnLongPressedListener(object : OnTagLongPressedListener {
            override fun onLongPressed(tagItem: TagItem) {
                Snackbar
                    .make(
                        root,
                        "onLongPressed: " + (tagItem as TextTagItem).text,
                        Snackbar.LENGTH_LONG
                    )
                    .setAction("Delete tag") { tagView.removeTag(tagItem) }
                    .show()
            }
        })
        tagView.setOnTagTapListener(object : OnTagTapListener {
            override fun onTap(tagItem: TagItem) {
                Snackbar
                    .make(root, "onTap: " + (tagItem as TextTagItem).text, Snackbar.LENGTH_SHORT)
                    .show()
            }
        })


        /**
         * 虽然是写的一个view，但是本质还是一个 ViewGroup
         *
         * 时间都再次分发到每一个tag上面去了
         *
         * 里面的本质就式写了这个方法
         *   view.setOnTouchListener(this)
         *   这个是一个单一的
         *
         */
        if (false) {
            /**
             * 事件的分发是从这里的
             * [View.dispatchTouchEvent]
             *
             * 如果返回了true，就会导致onClick事件出现问题
             * 如果返回 false ，没有响应--处于一种完全不能使用的状态
             */
            tagView.setOnTouchListener { v, event ->
                if (event.action == MotionEvent.ACTION_UP) {
                    val multiplier = Random.nextInt(1, 5)
                    tagView.startAutoRotation(
                        Random.nextFloat() * multiplier,
                        -Random.nextFloat() * multiplier
                    )
                } else {
                    tagView.stopAutoRotation()
                }
                false
            }
        }
    }

    private fun initSettings() {
        /**
         * TagSphereView 没有重写 onMeasure方法，也就是意味着，他是全屏的，这个从xml里面也可以看的出来
         */
        sbRadius.setOnSeekBarChangeListener(object : CustomOnSeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                /**
                 * 球体的半径。值越大则半径越小。价值限制在 1f 到 10f
                 * 因为这个参数做了一个被除数
                 */
                tagView.setRadius((progress + MIN_RADIUS) / 10f)
            }
        })
        sbTouchSensitivity.setOnSeekBarChangeListener(object : CustomOnSeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                tagView.setTouchSensitivity(progress + MIN_SENSITIVITY)
            }
        })

        /**
         * 这个做了一个互斥的操作
         *
         * 这个互斥的目的是，当触摸发生的时候，就不要在滚动了
         *
         *
         * 允许在触摸时旋转球体
         * Allow rotation sphere on touch
         *
         * 如果不执行 stopAutoRotation，就会出现触摸的时候还会继续旋转
         */
        cbRotateOnTouch.setOnCheckedChangeListener { _, isChecked ->
            tagView.rotateOnTouch(isChecked)
            /**
             * 这部分注释掉，然后里面修改一下触摸事件的判断，以及添加触摸的处理就可以满足条件了
             */
//            if (isChecked) {
//                cbAutoRotate.isChecked = false
//                tagView.stopAutoRotation()
//            }
        }
        /**
         * 自动滚动了
         */
        cbAutoRotate.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                cbRotateOnTouch.isChecked = false
                val multiplier = Random.nextInt(1, 5)
                tagView.startAutoRotation(
                    Random.nextFloat() * multiplier,
                    -Random.nextFloat() * multiplier
                )
            } else {
                cbRotateOnTouch.isChecked = true
            }
        }


        rgEasingFunctions.setOnCheckedChangeListener { _, checkedId ->
            when (checkedId) {
                R.id.rbEaseInExpo -> tagView.setEasingFunction { EasingFunction.easeInExpo(it) }
                R.id.rbEaseOutExpo -> tagView.setEasingFunction { EasingFunction.easeOutExpo(it) }
                R.id.rbCustom -> tagView.setEasingFunction { t -> 1f - t * t * t * t * t }
                else -> tagView.setEasingFunction(null)

            }
        }
    }


}