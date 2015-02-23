package org.mtransit.android.ui.view.map;

import com.google.android.gms.maps.model.LatLng;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public interface Circle {

	boolean contains(LatLng position);

	LatLng getCenter();

	<T> T getData();

	int getFillColor();

	@Deprecated
	String getId();

	double getRadius();

	int getStrokeColor();

	float getStrokeWidth();

	float getZIndex();

	boolean isVisible();

	void remove();

	void setCenter(LatLng center);

	void setData(Object data);

	void setFillColor(int fillColor);

	void setRadius(double radius);

	void setStrokeColor(int strokeColor);

	void setStrokeWidth(float strokeWidth);

	void setVisible(boolean visible);

	void setZIndex(float zIndex);
}