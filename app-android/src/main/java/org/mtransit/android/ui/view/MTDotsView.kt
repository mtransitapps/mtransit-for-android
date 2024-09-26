package org.mtransit.android.ui.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import androidx.annotation.ColorInt
import org.mtransit.android.R
import org.mtransit.android.commons.ColorUtils
import org.mtransit.android.commons.ThemeUtils
import org.mtransit.android.commons.ui.view.MTView
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

class MTDotsView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : MTView(context, attrs, defStyle) {

    companion object {
        private val LOG_TAG: String = MTDotsView::class.java.simpleName

        private const val MARGIN = 2

        private const val NOTHING_COLOR = Color.TRANSPARENT

        private const val EMPTY_DOCK_COLOR_DARK = Color.LTGRAY
        private const val EMPTY_DOCK_COLOR_LIGHT = Color.LTGRAY
    }

    override fun getLogTag() = LOG_TAG

    private var rowCount: Int = 0
    private var linesCount: Int = 0
    private var bounds = RectF()

    private var dotSizePx = 0
    private var dotMarginPx = 0

    init {
        if (isInEditMode) {
            setColorDots(
                20,
                (List(Random.nextInt(1, 15)) { Color.RED }
                        + List(Random.nextInt(1, 5)) { Color.BLUE }
                        ).reversed()
            )
        }
        if (!isInEditMode) {
            this.dotSizePx = ThemeUtils.obtainStyledDimensionPx(context, attrs, R.styleable.MTDots, R.styleable.MTDots_mtDotsSize, 0)
            this.dotMarginPx = ThemeUtils.obtainStyledDimensionPx(context, attrs, R.styleable.MTDots, R.styleable.MTDots_mtDotsMargin, 0)
        }
    }

    private var allDotsCount = 0

    private var colorIntDots: List<Int> = emptyList()

    private fun getEmptyDocksColor() = if (ColorUtils.isDarkTheme(context)) EMPTY_DOCK_COLOR_DARK else EMPTY_DOCK_COLOR_LIGHT

    fun setColorDots(allDotsCount: Int, colorIntDots: List<Int?>) {
        this.allDotsCount = allDotsCount
        this.rowCount = ceil(sqrt(allDotsCount.toFloat())).toInt()
        this.linesCount = ceil(this.allDotsCount.toFloat() / this.rowCount).toInt()
        this.colorIntDots = colorIntDots.filterNotNull()
        this.colorIntDots += List(allDotsCount - this.colorIntDots.size) { getEmptyDocksColor() }
        this.colorIntDots += List(this.rowCount * this.linesCount - allDotsCount) { NOTHING_COLOR }
        this.colorIntDots = this.colorIntDots.reversed()

        invalidate()
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(width, height)
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val innerSquareSize = min(w, h)
        var left = if (w > innerSquareSize) (w - innerSquareSize) / 2f else 0f
        var top = if (h > innerSquareSize) (h - innerSquareSize) / 2f else 0f
        var right = left + innerSquareSize
        var bottom = top + innerSquareSize
        left += paddingStart.toFloat()
        top += paddingTop.toFloat()
        right -= paddingEnd.toFloat()
        bottom -= paddingBottom.toFloat()
        this.bounds = RectF(left, top, right, bottom)
    }

    private val colorPaints = mutableMapOf<Int, Paint>()

    private fun makePaint(@ColorInt colorInt: Int): Paint {
        return Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = colorInt
        }
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val margin = this.dotMarginPx.toFloat().takeIf { it > 0f } ?: MARGIN.toFloat()

        val computedSize = (bounds.width() / this.rowCount) - margin
        val dotWidth = this.dotSizePx.toFloat().takeIf { it > 0f && it < computedSize } ?: computedSize
        val dotHeight = dotWidth

        val innerTopPadding = (bounds.height() - (linesCount * (dotHeight + margin))) / 2f
        val innerLeftPadding = (bounds.width() - (this.rowCount * (dotWidth + margin))) / 2f

        (0 until linesCount).forEach { lineIndex ->
            (0 until this.rowCount).forEach { columnIndex ->
                val dotIndex = lineIndex * this.rowCount + columnIndex
                val colorInt = this.colorIntDots.getOrNull(dotIndex) ?: NOTHING_COLOR // should not happen
                val colorPaint = this.colorPaints.getOrPut(colorInt) { makePaint(colorInt) }
                val left = innerLeftPadding + this.bounds.left + columnIndex * (dotWidth + margin)
                val right = left + dotWidth
                val top = innerTopPadding + this.bounds.top + lineIndex * (dotHeight + margin)
                val bottom = top + dotHeight
                canvas.drawRect(left, top, right, bottom, colorPaint)
            }
        }
    }
}