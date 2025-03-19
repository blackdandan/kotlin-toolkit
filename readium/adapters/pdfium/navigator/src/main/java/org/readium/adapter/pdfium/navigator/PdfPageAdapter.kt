import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.PointF
import android.view.MotionEvent
import android.view.ViewGroup
import android.widget.ImageView
import androidx.annotation.ColorInt
import androidx.recyclerview.widget.RecyclerView
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore

public class PdfPageAdapter(
    private val context: Context,
    private val pdfiumCore: PdfiumCore,
    private val pdfDocument: PdfDocument,
    private val pageCount: Int,
    private val isHorizontal: Boolean
) : RecyclerView.Adapter<PdfPageAdapter.PdfPageViewHolder>() {
    private var mBgColorInt: Int? = null
    public var onTapAction: ((PointF) -> Unit)? = null
    public var onDragAction: ((MotionEvent, PointF) -> Unit)? = null
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PdfPageViewHolder {
        val view = ImageView(context)
        mBgColorInt?.let {
            view.setBackgroundColor(it)
        }
        view.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            if (isHorizontal) ViewGroup.LayoutParams.MATCH_PARENT else ViewGroup.LayoutParams.WRAP_CONTENT
        )
        return PdfPageViewHolder(view)
    }

    override fun onBindViewHolder(holder: PdfPageViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = pageCount
    public fun setonTapListener(onTapAction: (PointF) -> Unit ) {
        this.onTapAction = onTapAction
    }

    public fun setOnDragListener(onDragAction: (MotionEvent, PointF) -> Unit ) {
        this.onDragAction = onDragAction
    }

    public fun setBackgroundColor(@ColorInt int: Int) {
        mBgColorInt = int
    }

    public inner class PdfPageViewHolder(itemView: ImageView) : RecyclerView.ViewHolder(itemView) {
        private var downX: Float = 0f
        private var downY: Float = 0f
        private var isClick: Boolean = false
        @SuppressLint("ClickableViewAccessibility")
        public fun bind(pageIndex: Int) {
            val pageWidth = 1080  // 设定渲染宽度
            val pageHeight = 1440 // 设定渲染高度

            val bitmap = Bitmap.createBitmap(pageWidth, pageHeight, Bitmap.Config.ARGB_8888)
            pdfiumCore.openPage(pdfDocument, pageIndex)
            pdfiumCore.renderPageBitmap(pdfDocument, bitmap, pageIndex, 0, 0, pageWidth, pageHeight)
            (itemView as ImageView).setImageBitmap(bitmap)
            itemView.setOnTouchListener { v, event ->
                var shouldConsume = false
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        // 记录按下时的坐标
                        downX = event.x
                        downY = event.y
                        isClick = true
                        onDragAction?.invoke(event, PointF(event.x, event.y))
                        shouldConsume = true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        // 判断是否移动超过阈值
                        if (Math.abs(event.x - downX) > 10 || Math.abs(event.y - downY) > 10) {
                            isClick = false
                            onDragAction?.invoke(event, PointF(downX, downY))
                        }
                        shouldConsume = false
                    }
                    MotionEvent.ACTION_UP -> {
                        // 如果是点击事件则触发onTap
                        if (isClick) {
                            onTapAction?.invoke(PointF(event.x, event.y))
                            shouldConsume = true
                        } else {
                            shouldConsume = false
                            onDragAction?.invoke(event, PointF(downX, downY))
                        }
                    }
                }
                shouldConsume
            }
        }
    }
}
