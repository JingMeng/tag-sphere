package com.magicgoop.tagpshere.example.showcase2

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.magicgoop.tagpshere.example.R
import kotlinx.android.synthetic.main.fragment_showcase2.*


class Showcase2Fragment : Fragment() {
    companion object {
        fun newInstance(): Showcase2Fragment =
            Showcase2Fragment()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_showcase2, container, false);
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        /**
         * 使用起来很简单，自定义就可以满足了
         */
        (0..250).map {
            DotTagItem(resources.getDimension(R.dimen.dot_radius_2))
        }.toList().let {
            tagSphere1.addTagList(it)
        }
    }

    override fun onResume() {
        super.onResume()
        /**
         * 自动滚动的操作
         */
        tagSphere1.startAutoRotation(-1f, 1f)
    }


    override fun onPause() {
        super.onPause()
        /**
         * 停止自动滚动，这个地方有必要调用吗？
         * 因为 代码里面已经判断力，这个应该是有必要的，因为使用vp的时候，是不会调用view里面的那个的
         */
        tagSphere1.stopAutoRotation()
    }
}