package org.mtransit.android.ui.view;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import java.util.ArrayList;
import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ThemeUtils;
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

	@NonNull
	private final List<Piece> values = new ArrayList<>();

	public MTPieChartPercentView(@NonNull Context context) {
		super(context);
	}

	public MTPieChartPercentView(@NonNull Context context, @Nullable AttributeSet attrs) {
		super(context, attrs);
		applyCustomFont(context, attrs);
	}

	public MTPieChartPercentView(@NonNull Context context, @Nullable AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		applyCustomFont(context, attrs);
	}

	private void applyCustomFont(@NonNull Context context, @Nullable AttributeSet attrs) {
		if (isInEditMode()) {
			return;
		}
		init(ThemeUtils.obtainStyledInteger(context, attrs, R.styleable.MTPieChart, R.styleable.MTPieChart_mtPieCharPieces, 0));
	}

	private void init(int count) {
		this.values.clear();
		for (int i = 0; i < count; i++) {
			values.add(new Piece());
		}
	}

	public void setValueColors(@NonNull List<Pair<Integer, Integer>> valueColors) {
		if (valueColors.size() != this.values.size()) {
			MTLog.w(this, "Trying to set wrong number of colors '%d'", valueColors.size());
			return;
		}
		for (int i = 0; i < this.values.size(); i++) {
			Piece piece = this.values.get(i);
			Pair<Integer, Integer> valueColor = valueColors.get(i);
			if (valueColor.first != null) {
				piece.setValuePaintColor(valueColor.first);
			}
			if (valueColor.second != null) {
				piece.setValuePaintBgColor(valueColor.second);
			}
		}
	}

	public void setValues(@NonNull List<Integer> newValues) {
		if (newValues.size() != this.values.size()) {
			MTLog.w(this, "Trying to set wrong number of colors '%d'", newValues.size());
			return;
		}
		boolean valueChanged = false;
		for (int i = 0; i < this.values.size(); i++) {
			Piece piece = this.values.get(i);
			int newValue = newValues.get(i);
			if (piece.getValue() != newValue) {
				piece.setValue(newValue);
				valueChanged = true;
			}
		}
		if (!valueChanged) {
			return; // no change
		}
		resetValuesAngles();
	}

	private void resetValuesAngles() {
		int total = getTotal();
		float previousValueEndingAngle = DEGREE_90;
		for (Piece value : this.values) {
			final float valueAngle = value.value * DEGREE_360 / total;
			float valueEndingAngle = previousValueEndingAngle + valueAngle;
			value.valueStartAngle = DEGREE_360 - valueEndingAngle;
			value.valueSweepAngle = valueEndingAngle - previousValueEndingAngle;
			previousValueEndingAngle = valueEndingAngle;
		}
		invalidate();
	}

	public int getTotal() {
		int total = 0;
		for (Piece value : this.values) {
			if (value.value < 0) {
				return -1; // not ready
			}
			total += value.value;
		}
		return total;
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
		if (getTotal() < 0) {
			return; // values not ready
		}
		for (Piece value : this.values) {
			canvas.drawArc(this.bounds, value.getValueStartAngle(), value.getValueSweepAngle(), true, value.getValuePaint());
			canvas.drawArc(this.bounds, value.getValueStartAngle(), value.getValueSweepAngle(), false, value.getValuePaintBg());
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

	public static class Piece {

		@NonNull
		private final Paint valuePaint;
		@NonNull
		private final Paint valuePaintBg;

		private int value = -1;
		private float valueStartAngle;
		private float valueSweepAngle;

		Piece() {
			this.valuePaint = new Paint();
			this.valuePaint.setStyle(Paint.Style.FILL);
			this.valuePaint.setColor(Color.BLACK);
			this.valuePaint.setAntiAlias(true);

			this.valuePaintBg = new Paint();
			this.valuePaintBg.setStyle(Paint.Style.STROKE);
			this.valuePaintBg.setColor(Color.BLACK);
			this.valuePaintBg.setAntiAlias(true);
		}

		int getValue() {
			return value;
		}

		void setValue(int value) {
			this.value = value;
		}

		@NonNull
		Paint getValuePaint() {
			return valuePaint;
		}

		void setValuePaintColor(@ColorInt int color) {
			this.valuePaint.setColor(color);
		}

		@NonNull
		Paint getValuePaintBg() {
			return valuePaintBg;
		}

		void setValuePaintBgColor(@ColorInt int color) {
			this.valuePaintBg.setColor(color);
		}

		float getValueStartAngle() {
			return valueStartAngle;
		}

		float getValueSweepAngle() {
			return valueSweepAngle;
		}
	}
}
