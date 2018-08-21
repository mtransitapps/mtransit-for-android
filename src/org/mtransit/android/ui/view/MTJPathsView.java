package org.mtransit.android.ui.view;

import java.util.HashSet;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.StringUtils;
import org.mtransit.android.commons.ui.view.MTView;
import org.mtransit.android.data.JPaths;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.support.v4.util.Pair;
import android.util.AttributeSet;

public class MTJPathsView extends MTView {

	private static final String TAG = MTJPathsView.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private RectF bounds;

	private HashSet<Pair<Path, Paint>> pathsAndPaints = new HashSet<Pair<Path, Paint>>();

	private JPaths jPaths;

	private int color = Color.WHITE;

	private float width;

	public MTJPathsView(Context context) {
		super(context);
		init();
	}

	public MTJPathsView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MTJPathsView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
	}

	public void setColor(int color) {
		this.color = color;
		if (this.pathsAndPaints != null) {
			for (Pair<Path, Paint> pp : this.pathsAndPaints) {
				pp.second.setColor(color);
			}
		}
	}

	public void setJSON(JPaths jPaths) {
		boolean invalidate;
		if (this.jPaths == null && jPaths != null) {
			invalidate = true;
		} else {
			String thisId = this.jPaths == null ? StringUtils.EMPTY : this.jPaths.getId();
			String newId = jPaths == null ? StringUtils.EMPTY : this.jPaths.getId();
			invalidate = !thisId.equals(newId);
		}
		this.jPaths = jPaths;
		if (invalidate) {
			invalidate();
		}
	}

	public boolean hasPaths() {
		return this.jPaths != null;
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (this.pathsAndPaints != null) {
			for (Pair<Path, Paint> pp : this.pathsAndPaints) {
				canvas.drawPath(pp.first, pp.second);
			}
		}
	}

	@Override
	protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(TAG, "onMeasure(%s,%s)", widthMeasureSpec, heightMeasureSpec);
		}
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
		this.width = this.bounds.right - this.bounds.left;

		initPaths();
	}

	private void initPaths() {
		this.pathsAndPaints.clear();
		if (this.jPaths != null) {
			for (JPaths.JPath jPath : this.jPaths.getPaths()) {
				Path path = new Path();
				if (jPath.form instanceof JPaths.JCircle) {
					JPaths.JCircle jCircle = (JPaths.JCircle) jPath.form;
					path.addCircle(getX(jCircle.x), getY(jCircle.y), getSize(jCircle.radius), Path.Direction.CW);
				} else if (jPath.form instanceof JPaths.JRect) {
					JPaths.JRect jRect = (JPaths.JRect) jPath.form;
					path.addRect(getX(jRect.left), getY(jRect.top), getX(jRect.right), getY(jRect.bottom), Path.Direction.CW);
				} else {
					if (!isInEditMode()) {
						MTLog.w(this, "Unexpected path form '%s'!", jPath.form);
					}
					continue;
				}
				path.close();
				Paint paint = getNewDefaultPaint();
				paint.setStyle(jPath.paint.style);
				if (jPath.paint.strokeWidth >= 0f) {
					paint.setStrokeWidth(getSize(jPath.paint.strokeWidth));
				}
				if (jPath.rotation != null) {
					this.matrix.reset();
					this.matrix.postRotate(jPath.rotation.degrees, getX(jPath.rotation.px), getY(jPath.rotation.py));
					path.transform(this.matrix);
				}
				this.pathsAndPaints.add(new Pair<Path, Paint>(path, paint));
			}
		}
	}

	private float getSize(float percent) {
		return percent * width;
	}

	private float getX(float percent) {
		return this.bounds.left + percent * this.width;
	}

	private float getY(float percent) {
		return this.bounds.top + percent * this.width;
	}

	private final Matrix matrix = new Matrix();

	private Paint getNewDefaultPaint() {
		Paint newPaint = new Paint();
		newPaint.setColor(this.color);
		newPaint.setAntiAlias(true);
		return newPaint;
	}
}
