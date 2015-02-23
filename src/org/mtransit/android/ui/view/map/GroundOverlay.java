package org.mtransit.android.ui.view.map;

import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.LatLngBounds;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public interface GroundOverlay {

	float getBearing();

	LatLngBounds getBounds();

	<T> T getData();

	float getHeight();

	@Deprecated
	String getId();

	LatLng getPosition();

	float getTransparency();

	float getWidth();

	float getZIndex();

	boolean isVisible();

	void remove();

	void setBearing(float bearing);

	void setData(Object data);

	void setDimensions(float width, float height);

	void setDimensions(float width);

	void setImage(BitmapDescriptor image);

	void setPosition(LatLng position);

	void setPositionFromBounds(LatLngBounds bounds);

	void setTransparency(float transparency);

	void setVisible(boolean visible);

	void setZIndex(float zIndex);
}