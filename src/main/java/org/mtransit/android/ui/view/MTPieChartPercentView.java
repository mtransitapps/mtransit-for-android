package org.mtransit.android.ui.view;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.util.Pair;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ThemeUtils;
import org.mtransit.android.commons.ui.view.MTView;

import java.util.ArrayList;
import java.util.List;

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
	private final List<Piece> pieces = new ArrayList<>();

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
		init(
				ThemeUtils.obtainStyledInteger(context, attrs, R.styleable.MTPieChart, R.styleable.MTPieChart_mtPieCharPieces, 0)
		);
	}

	private void init(int count) {
		this.pieces.clear();
		for (int i = 0; i < count; i++) {
			pieces.add(new Piece());
		}
	}

	public void setPiecesColors(@NonNull List<Pair<Integer, Integer>> piecesColors) {
		if (piecesColors.size() != this.pieces.size()) {
			MTLog.w(this, "Trying to set wrong number of colors '%d'", piecesColors.size());
			return;
		}
		for (int i = 0; i < this.pieces.size(); i++) {
			Piece piece = this.pieces.get(i);
			Pair<Integer, Integer> pieceColor = piecesColors.get(i);
			if (pieceColor.first != null) {
				piece.setPaintColor(pieceColor.first);
			}
			if (pieceColor.second != null) {
				piece.setPaintBgColor(pieceColor.second);
			}
		}
	}

	public void setPieces(@NonNull List<Integer> newSizes) {
		if (newSizes.size() != this.pieces.size()) {
			MTLog.w(this, "Trying to set wrong number of color sizes '%d'", newSizes.size());
			return;
		}
		boolean sizesChanged = false;
		for (int i = 0; i < this.pieces.size(); i++) {
			Piece piece = this.pieces.get(i);
			Integer newSize = newSizes.get(i);
			int newSizeI = newSize == null ? 0 : newSize;
			if (piece.getSize() != newSizeI) {
				piece.setSize(newSizeI);
				sizesChanged = true;
			}
		}
		if (!sizesChanged) {
			return; // no change
		}
		resetSizeAngles();
	}

	private void resetSizeAngles() {
		int total = getTotal();
		float previousPieceEndingAngle = DEGREE_90;
		if (!this.pieces.isEmpty()) {
			for (int i = this.pieces.size() - 1; i >= 0; i--) { // last first
				Piece piece = pieces.get(i);
				final float pieceAngle = piece.getSize() * DEGREE_360 / total;
				final float pieceEndingAngle = previousPieceEndingAngle + pieceAngle;
				piece.setStartAngle(DEGREE_360 - pieceEndingAngle);
				piece.setSweepAngle(pieceEndingAngle - previousPieceEndingAngle);
				previousPieceEndingAngle = pieceEndingAngle;
			}
		}
		invalidate();
	}

	public int getTotal() {
		int total = 0;
		for (Piece piece : this.pieces) {
			if (piece.getSize() < 0) {
				return -1; // not ready
			}
			total += piece.getSize();
		}
		return total;
	}

	@Override
	protected void onDraw(@NonNull Canvas canvas) {
		super.onDraw(canvas);
		if (getTotal() < 0) {
			return; // pieces not ready
		}
		for (Piece piece : this.pieces) {
			canvas.drawArc(this.bounds, piece.getStartAngle(), piece.getSweepAngle(), true, piece.getPaint());
			canvas.drawArc(this.bounds, piece.getStartAngle(), piece.getSweepAngle(), false, piece.getPaintBg());
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
		left += getPaddingStart();
		top += getPaddingTop();
		right -= getPaddingEnd();
		bottom -= getPaddingBottom();
		this.bounds = new RectF(left, top, right, bottom);
		invalidate();
	}

	public static class Piece {

		@NonNull
		private final Paint paint;
		@NonNull
		private final Paint paintBg;

		private int size = -1;
		private float startAngle;
		private float sweepAngle;

		Piece() {
			this.paint = new Paint();
			this.paint.setStyle(Paint.Style.FILL);
			this.paint.setColor(Color.BLACK);
			this.paint.setAntiAlias(true);

			this.paintBg = new Paint();
			this.paintBg.setStyle(Paint.Style.STROKE);
			this.paintBg.setColor(Color.BLACK);
			this.paintBg.setAntiAlias(true);
		}

		int getSize() {
			return size;
		}

		void setSize(int size) {
			this.size = size;
		}

		@NonNull
		Paint getPaint() {
			return paint;
		}

		void setPaintColor(@ColorInt int color) {
			this.paint.setColor(color);
		}

		@NonNull
		Paint getPaintBg() {
			return paintBg;
		}

		void setPaintBgColor(@ColorInt int color) {
			this.paintBg.setColor(color);
		}

		float getStartAngle() {
			return startAngle;
		}

		void setStartAngle(float startAngle) {
			this.startAngle = startAngle;
		}

		float getSweepAngle() {
			return sweepAngle;
		}

		void setSweepAngle(float sweepAngle) {
			this.sweepAngle = sweepAngle;
		}
	}
}
