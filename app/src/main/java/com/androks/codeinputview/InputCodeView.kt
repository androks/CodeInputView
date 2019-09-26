package com.androks.codeinputview

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.RectF
import android.text.InputFilter
import android.text.InputType
import android.text.Selection
import android.text.Spannable
import android.text.TextPaint
import android.text.method.MovementMethod
import android.util.AttributeSet
import android.view.ActionMode
import android.view.KeyEvent
import android.view.Menu
import android.view.MenuItem
import android.view.MotionEvent
import android.view.View
import android.widget.EditText
import android.widget.TextView
import kotlin.math.min


class InputCodeView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : EditText(context, attrs) {

    companion object {
        private const val COUNT_OF_NUMBERS = 4
        private const val CURSOR_LEFT_PADDING = 5
        private const val CURSOR_SPEED = 500L
    }

    private val paint = Paint().apply {
        style = Paint.Style.STROKE
        strokeWidth = borderWidth
    }
    private val textPaint = TextPaint().apply {
        textSize = this@InputCodeView.textSize
        this.color = currentTextColor
    }

    private val cursorRunnable = CursorBlink()

    private var spaceBetweenItems = 0
    private var borderWidth = 0f
    private var boldBorderWidth = 0f
    private var itemHeight = 0
    private var itemWidth = 0
    private var cursorVisibility = true
    private var cursorHeight = 0
    private var cursorLeftPadding = dip(CURSOR_LEFT_PADDING)
    private var corderRadius = 0f
    private var borderColor = 0
    private var boldBorderColor = 0

    private var itemRectange = RectF()
    private var itemCenterPoint = PointF()
    private var itemTextRectangle = Rect()
    private var drawCursorFlag = true

    private var onCodeInputed: ((code: Int) -> Unit)? = null

    init {
        super.setCursorVisible(false)
        background = null
        isFocusableInTouchMode = true
        filters = arrayOf<InputFilter>(InputFilter.LengthFilter(COUNT_OF_NUMBERS))
        inputType = InputType.TYPE_CLASS_NUMBER
        disableSelectionMenu()
        context.useStyledAttributes(attrs, R.styleable.InputCodeView) {
            spaceBetweenItems = getDimensionPixelSize(
                R.styleable.InputCodeView_spaceSize,
                resources.getDimension(R.dimen.InputCodeView_spaceSize).toInt()
            )
            borderWidth = getDimensionPixelSize(
                R.styleable.InputCodeView_normalBorderWidth,
                resources.getDimension(R.dimen.InputCodeView_normalBorderWidth).toInt()
            ).toFloat()
            boldBorderWidth = getDimensionPixelSize(
                R.styleable.InputCodeView_selectedBorderWidth,
                resources.getDimension(R.dimen.InputCodeView_selectedBorderWidth).toInt()
            ).toFloat()
            itemHeight = getDimensionPixelSize(
                R.styleable.InputCodeView_itemHeight,
                resources.getDimension(R.dimen.InputCodeView_itemHeight).toInt()
            )
            itemWidth = getDimensionPixelSize(
                R.styleable.InputCodeView_itemWidth,
                resources.getDimension(R.dimen.InputCodeView_itemWidth).toInt()
            )
            cursorVisibility = getBoolean(
                R.styleable.InputCodeView_cursorVisible,
                resources.getBoolean(R.bool.InputCodeView_cursorVisible)
            )
            cursorHeight = getDimensionPixelSize(
                R.styleable.InputCodeView_cursorHeight,
                resources.getDimension(R.dimen.InputCodeView_cursorHeight).toInt()
            )
            corderRadius = getDimensionPixelSize(
                R.styleable.InputCodeView_cornerRadius,
                resources.getDimension(R.dimen.InputCodeView_cornerRadius).toInt()
            ).toFloat()
            borderColor = getColor(
                R.styleable.InputCodeView_borderColor,
                context.getColorRes(R.color.InputCodeView_borderColor)
            )
            boldBorderColor = getColor(
                R.styleable.InputCodeView_selectedBorderColor,
                context.getColorRes(R.color.InputCodeView_selectedBorderColor)
            )
        }
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        startCursor()
    }

    override fun onScreenStateChanged(screenState: Int) {
        super.onScreenStateChanged(screenState)
        when (screenState) {
            View.SCREEN_STATE_ON -> startCursor()
            View.SCREEN_STATE_OFF -> pauseCursor()
        }
    }

    override fun onDraw(canvas: Canvas) {
        val textSize = text.length
        val totalWidthOfCells =
            (COUNT_OF_NUMBERS * itemWidth) + spaceBetweenItems * (COUNT_OF_NUMBERS - 1)
        repeat(COUNT_OF_NUMBERS) { i ->
            val drawingCursor = shouldBlink() && textSize == i
            val drawingBoldBorder = isFocused && i <= textSize

            updateItemRectangle(i, totalWidthOfCells, itemHeight)
            updateCenterPoint()
            if (drawingBoldBorder) selectBoldBorder() else selectNormalBorder()

            canvas.drawItemBorder()
            if (textSize > i) {
                canvas.drawTextAtItem(i)
            }
            if (drawingCursor) {
                canvas.drawCursor()
            }
        }
    }

    override fun onTextChanged(
        text: CharSequence,
        start: Int,
        lengthBefore: Int,
        lengthAfter: Int
    ) {
        if (start != text.length) moveSelectionToEnd()
        if (text.length == COUNT_OF_NUMBERS) onCodeInputed?.invoke(text.toString().toInt())
        startCursor()
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        super.onSelectionChanged(selStart, selEnd)

        if (selEnd != text.length) {
            moveSelectionToEnd()
        }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)

        if (focused) {
            moveSelectionToEnd()
            startCursor()
        }
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val borderWidth = boldBorderWidth.toInt()
        val desiredWidth = COUNT_OF_NUMBERS * itemWidth +
                (COUNT_OF_NUMBERS - 1) * spaceBetweenItems +
                borderWidth + paddingEnd + paddingLeft
        val desiredHeight = itemHeight + paddingTop + paddingBottom + borderWidth
        setMeasuredDimension(
            measureDimension(desiredWidth, widthMeasureSpec),
            measureDimension(desiredHeight, heightMeasureSpec)
        )
    }

    private fun measureDimension(desiredSize: Int, measureSpec: Int): Int {
        val specSize = MeasureSpec.getSize(measureSpec)

        return when (MeasureSpec.getMode(measureSpec)) {
            MeasureSpec.EXACTLY -> specSize
            MeasureSpec.AT_MOST -> min(desiredSize, specSize)
            else -> desiredSize
        }
    }

    override fun getDefaultMovementMethod() = object : MovementMethod {
        override fun onTouchEvent(widget: TextView?, text: Spannable?, event: MotionEvent?) = false
        override fun canSelectArbitrarily() = false
        override fun onKeyDown(
            widget: TextView?,
            text: Spannable?,
            keyCode: Int,
            event: KeyEvent?
        ) = false

        override fun onKeyUp(widget: TextView?, text: Spannable?, keyCode: Int, event: KeyEvent?) =
            false

        override fun onGenericMotionEvent(
            widget: TextView?,
            text: Spannable?,
            event: MotionEvent?
        ) = false

        override fun onTakeFocus(widget: TextView?, text: Spannable?, direction: Int) = Unit
        override fun onKeyOther(view: TextView?, text: Spannable?, event: KeyEvent?): Boolean =
            false

        override fun onTrackballEvent(widget: TextView?, text: Spannable?, event: MotionEvent?) =
            false

        override fun initialize(widget: TextView?, text: Spannable?) {
            Selection.setSelection(text, 0)
        }
    }

    override fun onDetachedFromWindow() {
        super.onDetachedFromWindow()
        pauseCursor()
    }

    private fun updateItemRectangle(index: Int, allBoxesWidth: Int, allBoxesHeight: Int) {
        val verticalIndent = (height - allBoxesHeight) / 2f + scrollY
        val horizontalIdent = (width - allBoxesWidth) / 2f + scrollX

        val top = verticalIndent
        val bottom = top + itemHeight
        val left = horizontalIdent + index * (spaceBetweenItems + itemWidth)
        val right = left + itemWidth
        itemRectange.set(left, top, right, bottom)
    }

    private fun updateCenterPoint() {
        val cx = itemRectange.left + itemRectange.width() / 2f
        val cy = itemRectange.top + itemRectange.height() / 2f
        itemCenterPoint.set(cx, cy)
    }

    private fun selectNormalBorder() {
        paint.strokeWidth = borderWidth
        paint.color = borderColor
    }

    private fun selectBoldBorder() {
        paint.strokeWidth = boldBorderWidth
        paint.color = boldBorderColor
    }

    private fun Canvas.drawItemBorder() =
        drawRoundRect(itemRectange, corderRadius, corderRadius, paint)

    private fun Canvas.drawTextAtItem(index: Int) {
        textPaint.getTextBounds(text.toString(), index, index + 1, itemTextRectangle)
        val cx = itemCenterPoint.x
        val cy = itemCenterPoint.y
        val x = cx - itemTextRectangle.width() / 2 - itemTextRectangle.left
        val y = cy + itemTextRectangle.height() / 2 - itemTextRectangle.bottom
        drawText(text, index, index + 1, x, y, textPaint)
    }

    private fun Canvas.drawCursor() {
        if (!drawCursorFlag) return
        val cy = itemCenterPoint.y

        val x = itemRectange.left + cursorLeftPadding
        val y = cy - cursorHeight / 2

        drawLine(x, y, x, y + cursorHeight, paint)
    }

    fun setOnCodeInputedListener(listener: ((code: Int) -> Unit)) {
        onCodeInputed = listener
    }

    private fun moveSelectionToEnd() {
        setSelection(text.length)
    }

    private fun startCursor() {
        if (shouldBlink()) {
            removeCallbacks(cursorRunnable)
            drawCursorFlag = false
            post(cursorRunnable)
        } else {
            removeCallbacks(cursorRunnable)
        }
    }

    private fun pauseCursor() {
        removeCallbacks(cursorRunnable)
        invalidateCursor(false)
    }

    private fun shouldBlink(): Boolean {
        return cursorVisibility && isFocused
    }

    private fun invalidateCursor(showCursor: Boolean) {
        if (drawCursorFlag != showCursor) {
            drawCursorFlag = showCursor
            invalidate()
        }
    }

    private fun disableSelectionMenu() {
        customSelectionActionModeCallback = object : ActionMode.Callback {
            override fun onCreateActionMode(mode: ActionMode, menu: Menu) = false
            override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
            override fun onActionItemClicked(mode: ActionMode, item: MenuItem) = false
            override fun onDestroyActionMode(mode: ActionMode) = Unit
        }
        isLongClickable = false
    }

    override fun isSuggestionsEnabled() = false

    private inner class CursorBlink : Runnable {
        override fun run() {
            removeCallbacks(this)
            if (shouldBlink()) {
                invalidateCursor(!drawCursorFlag)
                postDelayed(this, CURSOR_SPEED)
            }
        }
    }
}