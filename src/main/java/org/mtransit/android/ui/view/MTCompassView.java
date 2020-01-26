package org.mtransit.android.ui.view;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.SensorUtils;
import org.mtransit.android.commons.ui.view.MTView;
import org.mtransit.android.data.POIManager;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.location.Location;
import android.util.AttributeSet;

public class MTCompassView extends MTView {

	private static final String TAG = MTCompassView.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private int headingInDegree = -1; // 0-360°

	private Paint compassPaint;

	private RectF bounds;

	private float boundsExactCenterX;

	private float boundsExactCenterY;

	private Path headingArrayPath;

	public MTCompassView(Context context) {
		super(context);
		init();
	}

	public MTCompassView(Context context, AttributeSet attrs) {
		super(context, attrs);
		init();
	}

	public MTCompassView(Context context, AttributeSet attrs, int defStyle) {
		super(context, attrs, defStyle);
		init();
	}

	private void init() {
		if (Constants.LOG_VIEW_LIFECYCLE) {
			MTLog.v(this, "init()");
		}
		this.compassPaint = new Paint();
		if (!isInEditMode()) {
			this.compassPaint.setColor(POIManager.getDefaultDistanceAndCompassColor(getContext()));
		}
		this.compassPaint.setAntiAlias(true);
	}

	public void setColor(int color) {
		this.compassPaint.setColor(color);
	}

	@Override
	protected void onDraw(Canvas canvas) {
		super.onDraw(canvas);
		if (this.headingInDegree >= 0) {
			canvas.rotate(this.headingInDegree, this.boundsExactCenterX, this.boundsExactCenterY);
			canvas.drawPath(this.headingArrayPath, this.compassPaint);
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
		this.boundsExactCenterX = left + (this.bounds.right - this.bounds.left) / 2;
		this.boundsExactCenterY = top + (this.bounds.bottom - this.bounds.top) / 2;
		this.headingArrayPath = getHeadingArrow();
	}

	private Path getHeadingArrow() {
		Path path = new Path();
		// we want to crossing between the inner circle and the quarter of the width or the circle
		float innerCircleWidth = this.bounds.right - this.bounds.left;
		float innerCircleRadius = innerCircleWidth / 2;
		float x = this.bounds.left + -(innerCircleWidth / 4);
		float y = (float) Math.sqrt(Math.pow(innerCircleRadius, 2) - Math.pow(x, 2));
		float innerCircleBottom = this.bounds.top + innerCircleRadius + y;
		float arrowWidth = innerCircleRadius / 2;
		path.moveTo(this.boundsExactCenterX, this.bounds.top); // center top
		path.lineTo(this.boundsExactCenterX - arrowWidth, innerCircleBottom); // "/"
		path.lineTo(this.boundsExactCenterX, this.boundsExactCenterY + arrowWidth / 2); // "/"
		path.lineTo(this.boundsExactCenterX + arrowWidth, innerCircleBottom); // "\"
		// path.lineTo(this.boundsExactCenterX, this.bounds.top); // "\" // not mandatory
		path.close();
		return path;
	}

	private Double lat;
	private Double lng;

	public void generateAndSetHeading(Location location, int lastCompassInDegree, float locationDeclination) {
		if (this.lat == null || this.lng == null || location == null) {
			return;
		}
		float compassRotation = SensorUtils.getCompassRotationInDegree(location.getLatitude(), location.getLongitude(), this.lat, this.lng,
				lastCompassInDegree, locationDeclination);
		setHeadingInDegree((int) compassRotation);
	}

	public void setLatLng(Double lat, Double lng) {
		this.lat = lat;
		this.lng = lng;
	}

	public void setHeadingInDegree(int headingInDegree) {
		headingInDegree = SensorUtils.convertToPosivite360Degree(headingInDegree); // should not be necessary anymore
		if (this.headingInDegree != headingInDegree) {
			this.headingInDegree = headingInDegree;
			invalidate();
		}
	}

	public void resetHeading() {
		if (this.headingInDegree < 0) {
			return; // skip
		}
		this.headingInDegree = -1; // no data
		invalidate();
	}

	public boolean isHeadingSet() {
		return this.headingInDegree >= 0;
	}
}
