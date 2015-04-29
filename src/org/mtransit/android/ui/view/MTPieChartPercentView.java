package org.mtransit.android.ui.view;

import org.mtransit.android.commons.ui.view.MTView;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

public class MTPieChartPercentView extends MTView {

	private static final String TAG = MTPieChartPercentView.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final float DEGREE_90 = 90.0f;
	private static final float DEGREE_360 = 360.0f;

	private RectF bounds;

	private int value1 = -1;
	private int value2 = -1;
	private Paint value1Paint;
	private Paint value1PaintBg;
	private Paint value2Paint;
	private Paint value2PaintBg;
	private float value1StartAngle;
	private float value1SweepAngle;
	private float value2StartAngle;
	private float value2SweepAngle;

	public MTPieChartPercentView(Context context) {
		super(context);
		init();
	}

	public MTPieChartPercentView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MTPieChartPercentView(Context context, AttributeSet attrs, int defStyle) {
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
	}

	public void setValueColors(int value1Color, int value1ColorBg, int value2Color, int value2ColorBg) {
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
	}

	public void setValues(int value1, int value2) {
		if (this.value1 == value1 && this.value2 == value2) {
			return; // no change
		}
		this.value1 = value1;
		this.value2 = value2;
		resetValuesAngles();
	}

	private void resetValuesAngles() {
		int total = getTotal();
		float value1EndingAngle = DEGREE_90 + (this.value1 * DEGREE_360 / total);
		float value2EndingAngle = value1EndingAngle + (this.value2 * DEGREE_360 / total);
		this.value1StartAngle = DEGREE_360 - value1EndingAngle;
		this.value1SweepAngle = value1EndingAngle - DEGREE_90;
		this.value2StartAngle = DEGREE_360 - value2EndingAngle;
		this.value2SweepAngle = value2EndingAngle - value1EndingAngle;
		invalidate();
	}

	public int getTotal() {
		if (this.value1 < 0 || this.value2 < 0) {
			return -1;
		}
		return this.value1 + this.value2;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (getTotal() >= 0) {
			canvas.drawArc(this.bounds, this.value1StartAngle, this.value1SweepAngle, true, this.value1Paint);
			canvas.drawArc(this.bounds, this.value1StartAngle, this.value1SweepAngle, false, this.value1PaintBg);
			canvas.drawArc(this.bounds, this.value2StartAngle, this.value2SweepAngle, true, this.value2Paint);
			canvas.drawArc(this.bounds, this.value2StartAngle, this.value2SweepAngle, false, this.value2PaintBg);
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
		this.bounds = new RectF(left, top, right, bottom);
		invalidate();
	}
}
