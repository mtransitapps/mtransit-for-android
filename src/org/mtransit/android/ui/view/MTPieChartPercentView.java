package org.mtransit.android.ui.view;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.ui.view.MTView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

public class MTPieChartPercentView extends MTView {

	private static final String LOG_TAG = MTPieChartPercentView.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	private static final float DEGREE_90 = 90.0f;
	private static final float DEGREE_360 = 360.0f;

	private RectF bounds;

	private int value1 = -1;
	private int value2 = -1;
	private int value3 = -1;
	private Paint value1Paint;
	private Paint value1PaintBg;
	private Paint value2Paint;
	private Paint value2PaintBg;
	private Paint value3Paint;
	private Paint value3PaintBg;
	private float value1StartAngle;
	private float value1SweepAngle;
	private float value2StartAngle;
	private float value2SweepAngle;
	private float value3StartAngle;
	private float value3SweepAngle;

	public MTPieChartPercentView(@NonNull Context context) {
		super(context);
		init();
	}

	public MTPieChartPercentView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MTPieChartPercentView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		this.value1Paint = new Paint();
		this.value1Paint.setStyle(Paint.Style.FILL);
		this.value1Paint.setColor(Color.BLACK);
		this.value1Paint.setAntiAlias(true);

		this.value1PaintBg = new Paint();
		this.value1PaintBg.setStyle(Paint.Style.STROKE);
		this.value1PaintBg.setColor(Color.BLACK);
		this.value1PaintBg.setAntiAlias(true);

		this.value2Paint = new Paint();
		this.value2Paint.setColor(Color.WHITE);
		this.value2Paint.setStyle(Paint.Style.FILL);
		this.value2Paint.setAntiAlias(true);

		this.value2PaintBg = new Paint();
		this.value2PaintBg.setColor(Color.WHITE);
		this.value2PaintBg.setStyle(Paint.Style.STROKE);
		this.value2PaintBg.setAntiAlias(true);

		this.value3Paint = new Paint();
		this.value3Paint.setColor(Color.WHITE);
		this.value3Paint.setStyle(Paint.Style.FILL);
		this.value3Paint.setAntiAlias(true);

		this.value3PaintBg = new Paint();
		this.value3PaintBg.setColor(Color.WHITE);
		this.value3PaintBg.setStyle(Paint.Style.STROKE);
		this.value3PaintBg.setAntiAlias(true);
	}

	public void setValueColors(@ColorInt int value1Color,
			@ColorInt int value1ColorBg,
			@ColorInt int value2Color,
			@ColorInt int value2ColorBg,
			@ColorInt int value3Color,
			@ColorInt int value3ColorBg) {
		if (this.value1Paint != null) {
			this.value1Paint.setColor(value1Color);
		}
		if (this.value1PaintBg != null) {
			this.value1PaintBg.setColor(value1ColorBg);
		}
		if (this.value2Paint != null) {
			this.value2Paint.setColor(value2Color);
		}
		if (this.value2PaintBg != null) {
			this.value2PaintBg.setColor(value2ColorBg);
		}
		if (this.value3Paint != null) {
			this.value3Paint.setColor(value3Color);
		}
		if (this.value3PaintBg != null) {
			this.value3PaintBg.setColor(value3ColorBg);
		}
	}

	public void setValues(int value1, int value2, int value3) {
		if (this.value1 == value1
				&& this.value2 == value2
				&& this.value3 == value3) {
			return; // no change
		}
		this.value1 = value1;
		this.value2 = value2;
		this.value3 = value3;
		resetValuesAngles();
	}

	private void resetValuesAngles() {
		int total = getTotal();

		final float value1Angle = this.value1 * DEGREE_360 / total;
		final float value2Angle = this.value2 * DEGREE_360 / total;
		final float value3Angle = this.value3 * DEGREE_360 / total;

		float value0EndingAngle = DEGREE_90;

		float value1EndingAngle = value0EndingAngle + value1Angle;
		float value2EndingAngle = value1EndingAngle + value2Angle;
		float value3EndingAngle = value2EndingAngle + value3Angle;

		this.value1StartAngle = DEGREE_360 - value1EndingAngle;
		this.value1SweepAngle = value1EndingAngle - value0EndingAngle;

		this.value2StartAngle = DEGREE_360 - value2EndingAngle;
		this.value2SweepAngle = value2EndingAngle - value1EndingAngle;

		this.value3StartAngle = DEGREE_360 - value3EndingAngle;
		this.value3SweepAngle = value3EndingAngle - value2EndingAngle;
		invalidate();
	}

	public int getTotal() {
		if (this.value1 < 0
				|| this.value2 < 0
				|| this.value3 < 0) {
			return -1;
		}
		return this.value1 + this.value2 + this.value3;
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
		if (getTotal() >= 0) {
			canvas.drawArc(this.bounds, this.value1StartAngle, this.value1SweepAngle, true, this.value1Paint);
			canvas.drawArc(this.bounds, this.value1StartAngle, this.value1SweepAngle, false, this.value1PaintBg);
			canvas.drawArc(this.bounds, this.value2StartAngle, this.value2SweepAngle, true, this.value2Paint);
			canvas.drawArc(this.bounds, this.value2StartAngle, this.value2SweepAngle, false, this.value2PaintBg);
			canvas.drawArc(this.bounds, this.value3StartAngle, this.value3SweepAngle, true, this.value3Paint);
			canvas.drawArc(this.bounds, this.value3StartAngle, this.value3SweepAngle, false, this.value3PaintBg);
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		int width = MeasureSpec.getSize(widthMeasureSpec);
		int height = MeasureSpec.getSize(heightMeasureSpec);
		setMeasuredDimension(width, height);
	}

	@Override
	protected void onSizeChanged(int w, int h, int oldw, int oldh) {
		super.onSizeChanged(w, h, oldw, oldh);
		float innerCircleDiameter = Math.min(w, h);
		float left = w > innerCircleDiameter ? (w - innerCircleDiameter) / 2 : 0;
		float top = h > innerCircleDiameter ? (h - innerCircleDiameter) / 2 : 0;
		float right = left + innerCircleDiameter;
		float bottom = top + innerCircleDiameter;
		left += getPaddingLeft();
		top += getPaddingTop();
		right -= getPaddingRight();
		bottom -= getPaddingBottom();
		this.bounds = new RectF(left, top, right, bottom);
		invalidate();
	}
}
