package com.azad.libs.waveformequalizer


import android.animation.ValueAnimator
import android.content.Context
import android.graphics.*
import android.graphics.drawable.Drawable
import android.util.AttributeSet
import android.util.Log
import android.view.MotionEvent
import android.view.View
import androidx.core.content.ContextCompat
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt


class EQView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {
    private val TAG = EQView::class.java.simpleName
    private var listener: EQEventListener? = null
    private val path = Path()

    private val DEFAULT_BAND_SIZE = 0
    private var maxBand: List<Int?> = listOf()
    private var bandLevels = mutableListOf<Float?>()
    private var hertz = mutableListOf<Int>()
    private var bandSize: Int = hertz.size
    private val defaultProgress = 25f
    private val defaultMaxProgress = 50

    private var activeSliderIndex: Int? = null

    private val seekbarMargin = 70f
    private val bottomMargin = 50f
    private val trackStrokeWidth = 4f
    private val progressStrokeWidth = 4f

    private var backgroundColor = Color.WHITE
    private var waveFormStartColor = Color.parseColor("#B80005")
    private var waveFormEndColor = Color.TRANSPARENT
    private var waveFormLineColor = Color.parseColor("#7C060A")
    private var activePathStartColor = Color.parseColor("#7C060A")
    private var activePathEndColor = Color.TRANSPARENT
    private var inactivePathStartColor = Color.parseColor("#838383")
    private var inactivePathEndColor = Color.parseColor("#505C5C5C")
    private var isSliderGradient = false
    private var inActivePathColor = Color.parseColor("#838383")
    private var activePathColor = Color.parseColor("#7C060A")


    private var activeBorderColor = Color.parseColor("#7C060A")
    private var defaultSeekbarPathColor = Color.BLACK
    private val defaultWavePathColor = Color.parseColor("#B80005")

    private val waveLinePathColor = Color.parseColor("#840A0A")
    private var bandNameColor = Color.parseColor("#CCCCCC")
    private var activeThumb: Drawable? = null
    private var inActiveThumb: Drawable? = null

    private var isSeekbarEnabled = true
    private var topPadding = 60f

    private var bandTextSize = 14

    init {
        attrs?.let {
            val typedArray = context.theme.obtainStyledAttributes(
                attrs,
                R.styleable.EqualizerView,
                0,
                0
            )
            bandSize = typedArray.getInteger(R.styleable.EqualizerView_eqBands, DEFAULT_BAND_SIZE)
            backgroundColor =
                typedArray.getColor(R.styleable.EqualizerView_eqBackgroundColor, backgroundColor)
            waveFormStartColor =
                typedArray.getColor(R.styleable.EqualizerView_eqWaveStartColor, waveFormStartColor)
            waveFormEndColor =
                typedArray.getColor(R.styleable.EqualizerView_eqWaveEndColor, waveFormEndColor)
            waveFormLineColor =
                typedArray.getColor(R.styleable.EqualizerView_eqWaveLineColor, waveFormLineColor)
            isSliderGradient =
                typedArray.getBoolean(R.styleable.EqualizerView_eqIsSliderGradient, false)
            activePathStartColor = typedArray.getColor(
                R.styleable.EqualizerView_eqActiveProgressStartColor,
                activePathStartColor
            )
            activePathEndColor = typedArray.getColor(
                R.styleable.EqualizerView_eqActiveProgressEndColor,
                activePathEndColor
            )
            inactivePathStartColor = typedArray.getColor(
                R.styleable.EqualizerView_eqInactiveProgressStartColor,
                inactivePathStartColor
            )
            inactivePathEndColor = typedArray.getColor(
                R.styleable.EqualizerView_eqInactiveProgressEndColor,
                inactivePathEndColor
            )
            inActivePathColor = typedArray.getColor(
                R.styleable.EqualizerView_eqInActiveProgressColor,
                inActivePathColor
            )
            activePathColor = typedArray.getColor(
                R.styleable.EqualizerView_eqActiveProgressColor,
                activePathColor
            )
            defaultSeekbarPathColor = typedArray.getColor(
                R.styleable.EqualizerView_eqSeekBarPathColor,
                defaultSeekbarPathColor
            )
            bandNameColor =
                typedArray.getColor(R.styleable.EqualizerView_eqBandsTextColor, bandNameColor)
            maxBand = List<Int?>(bandSize) { 0 }
            bandLevels = List<Float?>(bandSize) { 0f }.toMutableList()
            activeThumb = typedArray.getDrawable(
                R.styleable.EqualizerView_eqActiveThumb
            )
            inActiveThumb = typedArray.getDrawable(R.styleable.EqualizerView_eqInActiveThumb)
            inActiveThumb = ContextCompat.getDrawable(context, R.drawable.inactive_thumb)!!
            activeThumb = ContextCompat.getDrawable(context, R.drawable.active_thumb)!!
            bandTextSize = typedArray.getDimensionPixelSize(R.styleable.EqualizerView_eqBandTextSize,bandTextSize)
        }

        setBackgroundColor(backgroundColor)
    }

    /**
     * onDraw:
     * The method is called to render the view. It handles the drawing of the waveform,
     * sliders, and frequency labels based on the current settings.
     *
     * @param canvas Canvas object used to draw the components of the equalizer view
     */
    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val width = width.toFloat()
        val height = height.toFloat()
        val availableHeight = height - PixelUtil.dpToPx(context, bottomMargin)

        val segmentWidth = (width - 2 * seekbarMargin) / (bandSize - 1)

        // Draw the waveform
        drawWaveform(canvas, availableHeight, segmentWidth)

        // Draw the sliders
        drawSliders(canvas, availableHeight, segmentWidth)

        //Draw Frequency names
        drawText(canvas, height, segmentWidth)
    }

    /**
     * drawText:
     * Draws the frequency labels at the bottom of the view. These labels correspond to the frequency bands and
     * are displayed in kHz or Hz depending on the frequency value.
     *
     * @param canvas Canvas object used for rendering text
     * @param height The total height of the view
     * @param segmentWidth The width between each frequency band slider
     */
    private fun drawText(canvas: Canvas, height: Float, segmentWidth: Float) {
//        val customTypeface = ResourcesCompat.getFont(context, R.font.montserratlight)
        val paint = Paint()
        paint.color = bandNameColor
        paint.textSize = bandTextSize.toFloat()
        paint.textAlign = Paint.Align.CENTER
//        paint.typeface = customTypeface
        hertz.forEachIndexed { index, hert ->
            val x = seekbarMargin + index * segmentWidth

            val name: String
            if (hert >= 1000) {
                val kHz = hert.toFloat() / 1000
                val kHzStringFormat = kHz.toString().removeSuffix("0").removeSuffix(".")
                name = String.format("%sK", kHzStringFormat)
            } else {
                name = String.format("%s", hert)
            }
            canvas.drawText(name, x, height, paint)
        }
    }

    /**
     * drawWaveform:
     * This method is responsible for drawing the waveform that represents the frequency bands. It creates a
     * cubic Bezier curve for each band and smoothly connects the curves to form the waveform.
     *
     * @param canvas Canvas object used to render the waveform
     * @param availableHeight The available height for drawing the waveform
     * @param segmentWidth The width for each frequency band's segment
     */
    private fun drawWaveform(
        canvas: Canvas,
        availableHeight: Float,
        segmentWidth: Float
    ) {
        path.reset()
        val startX = 20f
        val firstSliderX = 20f + seekbarMargin
        val firstSliderY =
            availableHeight * (1 - (bandLevels[0] ?: defaultProgress) / (maxBand[0]
                ?: defaultMaxProgress)) + topPadding

        path.moveTo(startX, availableHeight + topPadding)
        path.cubicTo(startX, firstSliderY, startX, firstSliderY, firstSliderX, firstSliderY)

        bandLevels.forEachIndexed { index, value ->
            if (index < bandLevels.size - 1) {
                val innerStartX = seekbarMargin + index * segmentWidth
                val startY =
                    availableHeight * (1 - (value ?: defaultProgress) / (maxBand[index]
                        ?: defaultMaxProgress)) + topPadding
                val endX = seekbarMargin + (index + 1) * segmentWidth
                val endY =
                    availableHeight * (1 - (bandLevels[index + 1]
                        ?: defaultProgress) / (maxBand[index + 1]
                        ?: defaultMaxProgress)) + topPadding

                val innerControlX1 = innerStartX + segmentWidth / 2

                val controlX2 = endX - segmentWidth / 2

                path.cubicTo(innerControlX1, startY, controlX2, endY, endX, endY)
            }
        }

        val lastSliderX = (bandLevels.size - 1) * segmentWidth
        val lastSliderY =
            availableHeight * (1 - (bandLevels.last()
                ?: defaultProgress) / (maxBand[bandLevels.size - 1]
                ?: defaultMaxProgress)) + topPadding

        val controlExitX1 = lastSliderX + PixelUtil.dpToPx(context, 45f)
        val controlX2 = lastSliderX + PixelUtil.dpToPx(context, 45f)

        path.cubicTo(
            controlExitX1,
            lastSliderY,
            controlX2,
            lastSliderY,
            controlX2,
            availableHeight + topPadding
        )

        val fillGradient = LinearGradient(
            0f, 0f, 0f, availableHeight + topPadding + 2f,
            waveFormStartColor,
            waveFormEndColor,
            Shader.TileMode.CLAMP
        )
        val dropGradient = LinearGradient(
            0f, 0f, 0f, availableHeight + topPadding,
            waveFormStartColor,
            waveFormEndColor,
            Shader.TileMode.CLAMP
        )

        val fillPaint = Paint()
        fillPaint.shader = fillGradient
        fillPaint.isAntiAlias = true
        fillPaint.style = Paint.Style.FILL
        fillPaint.pathEffect = CornerPathEffect(10f)
        fillPaint.strokeJoin = Paint.Join.ROUND
        fillPaint.strokeCap = Paint.Cap.ROUND
        canvas.drawPath(path, fillPaint)

        val linePaint = Paint()
        linePaint.isAntiAlias = true
        linePaint.isDither = true
        linePaint.shader = dropGradient
        linePaint.color = waveFormLineColor
        linePaint.style = Paint.Style.STROKE
        linePaint.strokeWidth = 5f
        canvas.drawPath(path, linePaint)
    }

    /**
     * drawSliders:
     * Draws the sliders for each frequency band, including the active and inactive states.
     * The sliders are drawn as vertical lines with a corresponding thumb at the top.
     *
     * @param canvas Canvas object used to render the sliders
     * @param availableHeight The available height for drawing the sliders
     * @param segmentWidth The width for each frequency band's segment
     */
    private fun drawSliders(
        canvas: Canvas,
        availableHeight: Float,
        segmentWidth: Float
    ) {

        bandLevels.forEachIndexed { index, _ ->
            val paint = Paint()
            paint.isAntiAlias = true
            val x = seekbarMargin + index * segmentWidth

            val adjustedY =
                (availableHeight * (1 - (bandLevels[index] ?: defaultProgress) / (maxBand[index]
                    ?: defaultMaxProgress))) + topPadding

            paint.color = defaultSeekbarPathColor
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = PixelUtil.dpToPx(context, trackStrokeWidth)
            canvas.drawLine(
                x,
                availableHeight + topPadding,
                x,
                topPadding,
                paint
            ) // Apply topPadding here

            if (activeSliderIndex == index) {
                if (isSliderGradient) {
                    val progressGradient = LinearGradient(
                        0f, 0f, 0f, availableHeight + topPadding,
                        activePathStartColor,
                        activePathEndColor,
                        Shader.TileMode.REPEAT
                    )
                    paint.shader = progressGradient
                } else {
                    paint.shader = null
                    paint.color = activePathColor
                }
                paint.strokeWidth = PixelUtil.dpToPx(context, progressStrokeWidth)
                canvas.drawLine(
                    x,
                    availableHeight + topPadding,
                    x,
                    adjustedY,
                    paint
                )
            } else {

                if (isSliderGradient) {
                    val progressGradient = LinearGradient(
                        0f, 0f, 0f, availableHeight + topPadding,
                        inactivePathStartColor,
                        inactivePathEndColor,
                        Shader.TileMode.CLAMP
                    )
                    paint.shader = progressGradient
                } else {
                    paint.shader = null
                    paint.color = inActivePathColor
                }
                paint.strokeWidth = PixelUtil.dpToPx(context, progressStrokeWidth)
                canvas.drawLine(
                    x,
                    availableHeight + topPadding,
                    x,
                    adjustedY,
                    paint
                )
            }
//            val shadow = ContextCompat.getDrawable(context, R.drawable.thumb_shadow)!!
//            val thumbShadowX = x - PixelUtil.dpToPx(context, shadow.intrinsicWidth) / 2
//            val thumbShadowY =
//                adjustedY - PixelUtil.dpToPx(context, shadow.intrinsicHeight) / 2
//            shadow.setBounds(
//                thumbShadowX.toInt(), thumbShadowY.toInt(),
//                (thumbShadowX + PixelUtil.dpToPx(context, shadow.intrinsicWidth)).toInt(),
//                (thumbShadowY + PixelUtil.dpToPx(context, shadow.intrinsicWidth)).toInt()
//            )
//            canvas.translate(0f, 0f)
//            shadow.draw(canvas)
            if (activeSliderIndex == index) {

                val thumbX = x - PixelUtil.dpToPx(context, activeThumb?.intrinsicWidth!!) / 2
                val thumbY =
                    adjustedY - PixelUtil.dpToPx(context, activeThumb?.intrinsicHeight!!) / 2
                activeThumb?.setBounds(
                    thumbX.toInt(), thumbY.toInt(),
                    (thumbX + PixelUtil.dpToPx(context, activeThumb?.intrinsicWidth!!)).toInt(),
                    (thumbY + PixelUtil.dpToPx(context, activeThumb?.intrinsicWidth!!)).toInt()
                )
                canvas.translate(0f, 0f)
                activeThumb?.draw(canvas)
            } else {

                val thumbX = x - PixelUtil.dpToPx(context, inActiveThumb?.intrinsicWidth!!) / 2
                val thumbY =
                    adjustedY - PixelUtil.dpToPx(context, inActiveThumb?.intrinsicHeight!!) / 2
                inActiveThumb?.setBounds(
                    thumbX.toInt(), thumbY.toInt(),
                    (thumbX + PixelUtil.dpToPx(context, inActiveThumb?.intrinsicWidth!!)).toInt(),
                    (thumbY + PixelUtil.dpToPx(context, inActiveThumb?.intrinsicWidth!!)).toInt()
                )
                canvas.translate(0f, 0f)
                inActiveThumb?.draw(canvas)


            }

        }
    }


    /**
     * onTouchEvent:
     * Handles touch events on the EQ view. This allows users to interact with the frequency sliders
     * by dragging them to adjust the frequency levels.
     *
     * @param event MotionEvent object that contains information about the touch event
     * @return Boolean indicating whether the event was handled
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val width = width.toFloat()
        val heightLeft = height.toFloat() - PixelUtil.dpToPx(context, bottomMargin) + topPadding
        val segmentWidth = (width - 2 * seekbarMargin) / (bandSize - 1)
        val touchX = event.x
        val touchY = event.y - topPadding

        when (event.action) {
            MotionEvent.ACTION_DOWN -> {
                val closestIndex = bandLevels.indices.minByOrNull { index ->
                    val thumbX = seekbarMargin + index * segmentWidth
                    val thumbY = heightLeft * (1 - (bandLevels[index]
                        ?: defaultProgress) / (maxBand[index] ?: defaultMaxProgress))

                    val distance = sqrt(
                        (touchX - thumbX).toDouble().pow(2.0) + (touchY - thumbY).toDouble()
                            .pow(2.0)
                    )
                    distance
                }

                closestIndex?.let {
                    val sliderX = seekbarMargin + it * segmentWidth
                    val sliderY =
                        heightLeft * (1 - (bandLevels[it] ?: defaultProgress) / (maxBand[it]
                            ?: defaultMaxProgress))

                    if (abs(touchX - sliderX) <= PixelUtil.dpToPx(
                            context,
                            inActiveThumb?.intrinsicWidth!!
                        ) * 2
                        && abs(touchY - sliderY) <= PixelUtil.dpToPx(
                            context,
                            inActiveThumb?.intrinsicWidth!!
                        ) * 2
                    ) {
                        activeSliderIndex = it
                        bandLevels[it] =
                            ((heightLeft - touchY) / heightLeft) * (maxBand[it]
                                ?: defaultMaxProgress).toFloat()
                        bandLevels[it] = bandLevels[it]?.coerceIn(
                            0f,
                            maxBand[it]?.toFloat() ?: defaultMaxProgress.toFloat()
                        )
                        Log.d(
                            "NewSeekbarValue",
                            "Seekbar : $it, new value : ${
                                bandLevels[it]?.coerceIn(
                                    0f,
                                    maxBand[it]?.toFloat() ?: defaultMaxProgress.toFloat()
                                )
                            }"
                        )
                        parent?.requestDisallowInterceptTouchEvent(true)
                        postInvalidateDelayed(1L)
                    } else {
                        parent?.requestDisallowInterceptTouchEvent(false)
                        activeSliderIndex = null
                    }
                }

            }

            MotionEvent.ACTION_MOVE -> {
                activeSliderIndex?.let { index ->
                    bandLevels[index] =
                        ((heightLeft - touchY) / heightLeft) * (maxBand[index]
                            ?: defaultMaxProgress).toFloat()
                    bandLevels[index] = bandLevels[index]?.coerceIn(
                        0f,
                        maxBand[index]?.toFloat() ?: defaultMaxProgress.toFloat()
                    )
                    Log.d(
                        "NewSeekbarValue",
                        "Seekbar : $index, new value : ${
                            bandLevels[index]?.coerceIn(
                                0f,
                                maxBand[index]?.toFloat() ?: defaultMaxProgress.toFloat()
                            )
                        }"
                    )
                    postInvalidateDelayed(1L)
                }

            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                activeSliderIndex?.let {
                    listener?.onBandLevelChanged(
                        it,
                        bandLevels[it]?.toInt() ?: 0,
                        true
                    )
                }
                parent?.requestDisallowInterceptTouchEvent(false)
            }
        }
        return true
    }

    fun removeBandView() {
    }

    /**
     * setBands:
     * This method allows external code to update the frequency bands.
     *
     * @param bands A list of frequency bands name.
     */
    fun setBands(bands: ArrayList<Int>?) {
        Log.d(TAG, "bandNAme: ${bands.toString()}")
        if (bands?.size!! > DEFAULT_BAND_SIZE) {
            bandSize = bands.size
            hertz = bands
            maxBand = List<Int?>(bandSize) { 0 }
            bandLevels = List<Float?>(bandSize) { 0f }.toMutableList()
        }
    }

    /**
     * setBandLevel:
     * This method allows external code to update the frequency band levels. It takes a index with it's value
     * and apply this to the equalizer view.
     *
     * @param band A Band/Slider index of new frequency level for the equalizer
     * @param level A Band/Slider value/progress of new frequency level for the equalizer
     */
    fun setBandLevel(band: Int, level: Int) {
        Log.d(TAG, "setBandLevel: band $band level $level")
        if (band < bandSize) {
            animateBandLevelChange(band, level.toFloat())
        }
    }

    /**
     * setEnableSeekbar:
     * This method allows external code to handle to make Equalizer Interactive to User or not.
     *
     * @param isEnable A boolean to set Equalizer Enable/Disable
     */
    fun setEnableSeekbar(isEnable: Boolean) {
        isSeekbarEnabled = isEnable
    }

    /**
     * unSelectTheBar:
     * This method allows external code to remove a Frequency slider selection state
     */
    fun unSelectTheBar() {
        activeSliderIndex = null
        invalidate()
    }

    /**
     * updateMaxBands:
     * This method allows external code to set the maximum levels for frequencies band.
     *
     * @param max A list of maximum levels for the equalizer bands
     */
    fun setMax(max: List<Int>) {
        Log.d(TAG, "MaxList: $max")
        maxBand = max
    }

    /**
     * resetAllBands:
     * This method resets the frequency bands to their original state by clearing any changes made to the band levels
     * and restoring them to the initial values stored in the `tempBands` list. After resetting the band levels,
     * it triggers a redraw of the view to reflect the reset state.
     */
    fun resetAllBands() {
        activeSliderIndex = null
        invalidate()
    }

    fun setBandListener(bandListener: EQEventListener) {
        listener = bandListener
    }

    fun drawData() {
        if (bandLevels[bandSize - 1] != null) {
            invalidate()
        }
    }

    /**
     * animateBandLevelChange:
     * This method animates the change in the frequency band's level. When an external
     * trigger updates the level of a frequency band, this method creates a smooth transition
     * from the current level to the new level over a specified duration. It uses a ValueAnimator to smoothly
     * update the band's level and refresh the UI.
     *
     * @param index The index of the frequency band whose level needs to be changed
     * @param newLevel The new level to which the band's value should animate
     */
    private fun animateBandLevelChange(index: Int, newLevel: Float) {
        val currentLevel = bandLevels[index] ?: 0f
        if (currentLevel == newLevel) return

        val animator = ValueAnimator.ofFloat(currentLevel, newLevel)
        animator.duration = 300
        animator.addUpdateListener { animation ->
            if (bandLevels.size > 0) {
                bandLevels[index] = animation.animatedValue as Float
                invalidate()
            } else {
                return@addUpdateListener
            }
        }
        animator.start()
    }

    interface EQEventListener {

        fun onBandLevelChanged(bandId: Int, level: Int, fromUser: Boolean) = Unit

    }

}