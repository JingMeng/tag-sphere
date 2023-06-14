package com.magicgoop.tagsphere

import com.magicgoop.tagsphere.item.TagItem
import kotlin.math.cos
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

internal class TagAdapter {

    private val tagList: MutableList<TagItem> = mutableListOf()

    private fun addPoint(i: Int, size: Int, tagItem: TagItem) {
        recalculate(i, size, tagItem)
        tagList.add(tagItem)
    }

    /**
     * recalculate 重新计算的意思
     *
     *  从这个地方那个来看，这应该是一个单位圆形成的球体
     *    val r = sqrt(1 - y.pow(2))
     *   时候这个文章的算法
     * https://blog.csdn.net/fanfan_hongyun/article/details/91415726
     *
     * https://zhuanlan.zhihu.com/p/19699319
     *
     * 本文采用的定义是：最大化任意两点间的最小距离。
     * 其他可能的定义还有：球面上的同性电荷能量最低时的分布／离散估计球面积分时最合适的采样点。
     *
     *
     * https://www.cnblogs.com/TappaT/p/15965985.html
     * x2+y2+z2=1 这个公式不能扔掉
     *
     *
     * https://zh.wikipedia.org/zh-hans/%E7%90%83%E5%BA%A7%E6%A8%99%E7%B3%BB
     *
     *
     *
     * 这个问题 todo：
     * https://cloud.tencent.com/developer/ask/sof/106495/answer/102384708
     *
     * 在上面的找到下面的这个链接-----上面的连接就是翻译的这个内容而来的
     *
     * https://stackoverflow.com/questions/9600801/evenly-distributing-n-points-on-a-sphere/26127012#26127012
     *
     *
     *  单位球体的公式
     *
     */
    private fun recalculate(i: Int, size: Int, tagItem: TagItem) {
        val offset: Float = 2f / size
        val increment = Math.PI * (3f - sqrt(5f))
        val y = ((i * offset) - 1) + (offset / 2);
        val r = sqrt(1 - y.pow(2))
        val phi = (i % size) * increment
        val x = cos(phi) * r
        val z = sin(phi) * r
        tagItem.setPointValues(x.toFloat(), y, z.toFloat())
    }

    fun addTag(tagItem: TagItem) {
        val size = tagList.size
        tagList.forEachIndexed { index, tag ->
            recalculate(index, size, tag)
        }
        addPoint(size, size + 1, tagItem)
    }

    fun removeTag(tagItem: TagItem) {
        if (tagList.remove(tagItem)) {
            val size = tagList.size
            tagList.forEachIndexed { index, tag ->
                recalculate(index, size, tag)
            }
        }
    }

    fun addTagList(list: List<TagItem>) {
        val size = list.size
        list.forEachIndexed { index, tagItem ->
            addPoint(index, size, tagItem)
        }
    }

    fun clearAll() {
        tagList.clear()
    }

    fun getTags(): MutableList<TagItem> = tagList

    fun getTagByProjectionPoint(x: Float, y: Float): TagItem? =
        /**
         * 这个是一个全局的过滤操作
         * 被点击的首先要从前面
         * 其次按照距离进行排序，获取哦最小的那一个
         *
         * 使用的投影来操作的，三维转二维比较计算
         */
        tagList.filter { it.isTagInFront() }.minBy { it.projectionDistanceTo(x, y) }

}