package org.mtransit.android.ui.view.map;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public interface TileOverlay {

	void clearTileCache();

	<T> T getData();

	boolean getFadeIn();

	@Deprecated
	String getId();

	float getZIndex();

	boolean isVisible();

	void remove();

	void setData(Object data);

	void setFadeIn(boolean fadeIn);

	void setVisible(boolean visible);

	void setZIndex(float zIndex);
}