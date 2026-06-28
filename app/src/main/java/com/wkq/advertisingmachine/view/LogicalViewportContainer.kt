package com.wkq.advertisingmachine.view

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.view.ViewGroup
import com.wkq.advertisingmachine.model.ScreenWindow

class LogicalViewportContainer @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ViewGroup(context, attrs, defStyleAttr) {

    class LogicalLayoutParams : ViewGroup.LayoutParams {
        var x: Int = 0
        var y: Int = 0
        var w: Int = 0
        var h: Int = 0
        var layer: Int = 0

        constructor(x: Int, y: Int, w: Int, h: Int, layer: Int = 0) : super(w, h) {
            this.x = x
            this.y = y
            this.w = w
            this.h = h
            this.layer = layer
        }

        constructor(source: ViewGroup.LayoutParams) : super(source) {
            if (source is LogicalLayoutParams) {
                this.x = source.x
                this.y = source.y
                this.w = source.w
                this.h = source.h
                this.layer = source.layer
            }
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        // 测量自身大小
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)

        val scaleX = width.toFloat() / 1920f
        val scaleY = height.toFloat() / 1920f

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val lp = child.layoutParams as? LogicalLayoutParams ?: continue
                val childWidth = (lp.w * scaleX).toInt()
                val childHeight = (lp.h * scaleY).toInt()
                child.measure(
                    MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY),
                    MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY)
                )
            }
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val width = r - l
        val height = b - t
        val scaleX = width.toFloat() / 1920f
        val scaleY = height.toFloat() / 1920f

        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child.visibility != View.GONE) {
                val lp = child.layoutParams as? LogicalLayoutParams ?: continue
                val childLeft = (lp.x * scaleX).toInt()
                val childTop = (lp.y * scaleY).toInt()
                child.layout(
                    childLeft,
                    childTop,
                    childLeft + child.measuredWidth,
                    childTop + child.measuredHeight
                )
            }
        }
    }

    override fun generateLayoutParams(attrs: AttributeSet?): ViewGroup.LayoutParams {
        return LogicalLayoutParams(0, 0, 1920, 1920)
    }

    override fun generateLayoutParams(p: ViewGroup.LayoutParams?): ViewGroup.LayoutParams {
        return LogicalLayoutParams(p ?: ViewGroup.LayoutParams(1920, 1920))
    }

    override fun checkLayoutParams(p: ViewGroup.LayoutParams?): Boolean {
        return p is LogicalLayoutParams
    }

    // --- 窗口管理操作 API ---

    fun addOrUpdateWindow(
        screenId: String,
        x: Int,
        y: Int,
        w: Int,
        h: Int,
        layer: Int,
        bgColorStr: String
    ): WindowItemView {
        val existingView = findWindowView(screenId)
        val color = try {
            Color.parseColor(bgColorStr)
        } catch (e: Exception) {
            Color.TRANSPARENT
        }

        if (existingView != null) {
            // 更新坐标和层级
            val lp = LogicalLayoutParams(x, y, w, h, layer)
            existingView.layoutParams = lp
            existingView.setBackgroundColor(color)
            existingView.z = layer.toFloat()
            requestLayout()
            return existingView
        } else {
            // 创建新窗口
            val windowView = WindowItemView(context).apply {
                this.screenId = screenId
                this.setBackgroundColor(color)
                this.z = layer.toFloat()
            }
            val lp = LogicalLayoutParams(x, y, w, h, layer)
            addView(windowView, lp)
            return windowView
        }
    }

    fun removeWindow(screenId: String) {
        val view = findWindowView(screenId)
        if (view != null) {
            view.release()
            removeView(view)
        }
    }

    fun clearAllWindows() {
        for (i in 0 until childCount) {
            val view = getChildAt(i) as? WindowItemView
            view?.release()
        }
        removeAllViews()
    }

    fun findWindowView(screenId: String): WindowItemView? {
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is WindowItemView && child.screenId == screenId) {
                return child
            }
        }
        return null
    }

    fun getAllWindowsData(): List<ScreenWindow> {
        val list = mutableListOf<ScreenWindow>()
        for (i in 0 until childCount) {
            val child = getChildAt(i) as? WindowItemView ?: continue
            val lp = child.layoutParams as? LogicalLayoutParams ?: continue
            list.add(
                ScreenWindow(
                    screenId = child.screenId,
                    x = lp.x,
                    y = lp.y,
                    w = lp.w,
                    h = lp.h,
                    layer = lp.layer,
                    bgColor = child.getBackgroundColorString(),
                    currentMedia = child.currentMediaState,
                    overlayTextList = child.getOverlayTexts()
                )
            )
        }
        return list
    }
}
