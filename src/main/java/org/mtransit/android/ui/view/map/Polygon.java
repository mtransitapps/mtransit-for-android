package org.mtransit.android.ui.view.map;

import com.google.android.gms.maps.model.LatLng;

import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public interface Polygon {

	<T> T getData();

	int getFillColor();

	List<List<LatLng>> getHoles();

	@Deprecated
	String getId();

	List<LatLng> getPoints();

	int getStrokeColor();

	float getStrokeWidth();

	float getZIndex();

	boolean isGeodesic();

	boolean isVisible();

	void remove();

	void setData(Object data);

	void setFillColor(int fillColor);

	void setGeodesic(boolean geodesic);

	void setHoles(List<? extends List<LatLng>> holes);

	void setPoints(List<LatLng> points);

	void setStrokeColor(int strokeColor);

	void setStrokeWidth(float strokeWidth);

	void setVisible(boolean visible);

	void setZIndex(float zIndex);
}