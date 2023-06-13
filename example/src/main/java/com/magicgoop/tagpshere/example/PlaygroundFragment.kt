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
 */
class PlaygroundFragment : Fragment(), OnTagLongPressedListener, OnTagTapListener {

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
        tagView.setOnLongPressedListener(this)
        tagView.setOnTagTapListener(this)


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
        sbRadius.setOnSeekBarChangeListener(object : CustomOnSeekBarChangeListener() {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
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
            if (isChecked) {
                cbAutoRotate.isChecked = false
                tagView.stopAutoRotation()
            }
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

    override fun onLongPressed(tagItem: TagItem) {
        Snackbar
            .make(root, "onLongPressed: " + (tagItem as TextTagItem).text, Snackbar.LENGTH_LONG)
            .setAction("Delete tag") { tagView.removeTag(tagItem) }
            .show()
    }

    override fun onTap(tagItem: TagItem) {
        Snackbar
            .make(root, "onTap: " + (tagItem as TextTagItem).text, Snackbar.LENGTH_SHORT)
            .show()
    }
}