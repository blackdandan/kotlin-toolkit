package org.readium.adapter.pdfium.navigator

import android.content.Context
import android.graphics.PointF
import android.util.AttributeSet
import android.view.MotionEvent
import com.github.barteksc.pdfviewer.PDFView
import kotlin.math.sqrt

public class  DragEventPdfView @JvmOverloads constructor(context: Context, set: AttributeSet? = null) : PDFView(context, set) {
    private var customTouchListener: CustomTouchListener? = null
    private var isDragging = false // 用来标记是否开始拖动
    private var startPoint = PointF() // 记录触摸开始位置
    private val dragThreshold = 50f // 设置拖动阈值，单位像素
    public interface CustomTouchListener {
        public fun onTouch(event: MotionEvent?)
    }

    public fun setCustomTouchListener(listener: CustomTouchListener) {
        this.customTouchListener = listener
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        processTouch(ev)
        return super.dispatchTouchEvent(ev)
    }

    private fun processTouch(event: MotionEvent?) {
        event?.let {
            when (it.action) {
                MotionEvent.ACTION_DOWN -> {
                    // 用户开始触摸，记录开始位置
                    startPoint.set(event.x, event.y)
                    isDragging = false // 初始时不认为是拖动
                }

                MotionEvent.ACTION_MOVE -> {
                    // 计算当前触摸点与起始点的距离
                    val dx = event.x - startPoint.x
                    val dy = event.y - startPoint.y
                    val distance = sqrt((dx * dx + dy * dy).toDouble()).toFloat()

                    // 如果距离超过设定的阈值，就认为是拖动
                    if (!isDragging && distance > dragThreshold) {
                        isDragging = true
                    }

                    // 如果已判断为拖动，则触发 onDrag 事件
                    if (isDragging) {
                        customTouchListener?.onTouch(event)
                    }
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                    // 用户结束触摸，重置拖动状态
                    isDragging = false
                }
            }
        }
    }


}