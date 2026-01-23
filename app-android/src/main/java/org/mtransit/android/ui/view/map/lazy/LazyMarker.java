package org.mtransit.android.ui.view.map.lazy;

import android.content.Context;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.util.MapUtils;

import java.lang.ref.WeakReference;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
@SuppressWarnings({"unused", "WeakerAccess"})
public class LazyMarker implements MTLog.Loggable {

	private static final String LOG_TAG = LazyMarker.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public interface OnMarkerCreateListener {
		void onMarkerCreate(@NonNull LazyMarker marker);
	}

	@Nullable
	private Marker marker; // or AdvancedMarker
	// @Nullable
	private GoogleMap map;
	// @Nullable
	private MarkerOptions markerOptions; // or AdvancedMarkerOptions
	@Nullable
	private OnMarkerCreateListener listener;

	public LazyMarker(@NonNull GoogleMap map, @NonNull MarkerOptions options) {
		this(map, options, null);
	}

	public LazyMarker(@NonNull GoogleMap map, @NonNull MarkerOptions options, @Nullable OnMarkerCreateListener listener) {
		this(map, options, null, null, null, null, null, null, listener);
	}

	public LazyMarker(@NonNull GoogleMap map,
					  @NonNull MarkerOptions options,
					  @Nullable Integer optionsColor,
					  @Nullable Integer optionsSecondaryColor,
					  @Nullable Integer optionsDefaultColor,
					  @Nullable Integer optionsIconResId,
					  @Nullable Boolean optionsIconReplaceColor,
					  @Nullable Context optionsContext,
					  @Nullable OnMarkerCreateListener listener) {
		if (options.isVisible()) {
			createMarker(map, options, optionsColor, optionsSecondaryColor, optionsDefaultColor, optionsIconResId, optionsIconReplaceColor, optionsContext, listener);
		} else {
			this.map = map;
			this.markerOptions = copy(options);
			this.markerOptionsColor = optionsColor;
			this.markerOptionsSecondaryColor = optionsSecondaryColor;
			this.markerOptionsDefaultColor = optionsDefaultColor;
			this.markerOptionsIconResId = optionsIconResId;
			this.markerOptionsIconReplaceColor = optionsIconReplaceColor;
			this.markerOptionsContextWR = new WeakReference<>(optionsContext);
			this.listener = listener;
		}
	}

	public float getAlpha() {
		if (marker != null) {
			return marker.getAlpha();
		} else {
			return markerOptions.getAlpha();
		}
	}

	@NonNull
	@Deprecated
	public String getId() {
		final Marker newMarker = createMarkerIfNecessary();
		return newMarker.getId();
	}

	@Nullable
	public Marker getMarker() {
		return marker;
	}

	@NonNull
	public LatLng getPosition() {
		if (marker != null) {
			return marker.getPosition();
		} else {
			return markerOptions.getPosition();
		}
	}

	public float getRotation() {
		if (marker != null) {
			return marker.getRotation();
		} else {
			return markerOptions.getRotation();
		}
	}

	public float getZIndex() {
		if (marker != null) {
			return marker.getZIndex();
		} else {
			return markerOptions.getZIndex();
		}
	}

	@Nullable
	public String getSnippet() {
		if (marker != null) {
			return marker.getSnippet();
		} else {
			return markerOptions.getSnippet();
		}
	}

	@Nullable
	public String getTitle() {
		if (marker != null) {
			return marker.getTitle();
		} else {
			return markerOptions.getTitle();
		}
	}

	public void hideInfoWindow() {
		if (marker != null) {
			marker.hideInfoWindow();
		}
	}

	public boolean isDraggable() {
		if (marker != null) {
			return marker.isDraggable();
		} else {
			return markerOptions.isDraggable();
		}
	}

	public boolean isFlat() {
		if (marker != null) {
			return marker.isFlat();
		} else {
			return markerOptions.isFlat();
		}
	}

	public boolean isInfoWindowShown() {
		if (marker != null) {
			return marker.isInfoWindowShown();
		} else {
			return false;
		}
	}

	public boolean isVisible() {
		if (marker != null) {
			return marker.isVisible();
		} else {
			return false;
		}
	}

	public void remove() {
		if (marker != null) {
			marker.remove();
			marker = null;
		} else {
			map = null;
			markerOptions = null;
			markerOptionsColor = null;
			markerOptionsSecondaryColor = null;
			markerOptionsDefaultColor = null;
			markerOptionsIconResId = null;
			markerOptionsIconReplaceColor = null;
			if (markerOptionsContextWR != null) {
				markerOptionsContextWR.clear();
				markerOptionsContextWR = null;
			}
			listener = null;
		}
	}

	public void setZIndex(float zIndex) {
		if (marker != null) {
			marker.setZIndex(zIndex);
		} else {
			markerOptions.zIndex(zIndex);
		}
	}

	public void setAlpha(float alpha) {
		if (marker != null) {
			marker.setAlpha(alpha);
		} else {
			markerOptions.alpha(alpha);
		}
	}

	public void setAnchor(float anchorU, float anchorV) {
		if (marker != null) {
			marker.setAnchor(anchorU, anchorV);
		} else {
			markerOptions.anchor(anchorU, anchorV);
		}
	}

	public void setDraggable(boolean draggable) {
		if (marker != null) {
			marker.setDraggable(draggable);
		} else {
			markerOptions.draggable(draggable);
		}
	}

	public void setFlat(boolean flat) {
		if (marker != null) {
			marker.setFlat(flat);
		} else {
			markerOptions.flat(flat);
		}
	}

	@Deprecated
	public void setIcon(@Nullable BitmapDescriptor icon) {
		if (marker != null) {
			marker.setIcon(icon);
		} else {
			markerOptions.icon(icon);
		}
		markerOptionsColor = null;
		markerOptionsSecondaryColor = null;
		markerOptionsDefaultColor = null;
		markerOptionsIconResId = null;
		markerOptionsIconReplaceColor = null;
		if (markerOptionsContextWR != null) {
			markerOptionsContextWR.clear();
			markerOptionsContextWR = null;
		}
	}

	@DrawableRes
	@Nullable
	private Integer markerOptionsIconResId = null;
	@Nullable
	private Boolean markerOptionsIconReplaceColor = null;
	@ColorInt
	@Nullable
	private Integer markerOptionsColor = null;
	@ColorInt
	@Nullable
	private Integer markerOptionsSecondaryColor = null;
	@ColorInt
	@Nullable
	private Integer markerOptionsDefaultColor = null;
	@Nullable
	private WeakReference<Context> markerOptionsContextWR = null;

	public void setIcon(@Nullable Context context,
						@DrawableRes @Nullable Integer iconResId,
						@Nullable Boolean replaceColor,
						@ColorInt @Nullable Integer color,
						@ColorInt @Nullable Integer secondaryColor,
						@ColorInt @Nullable Integer defaultColor) {
		if (marker != null) {
			if (iconResId != null && replaceColor != null && color != null) {
				marker.setIcon(MapUtils.getIcon(context, iconResId, color, replaceColor));
			}
		} else {
			markerOptionsIconResId = iconResId;
			markerOptionsIconReplaceColor = replaceColor;
			markerOptionsColor = color;
			markerOptionsSecondaryColor = secondaryColor;
			markerOptionsDefaultColor = defaultColor;
			markerOptionsContextWR = new WeakReference<>(context);
			markerOptions.icon(null);
		}
	}

	@Nullable
	@ColorInt
	public Integer getColor() {
		return this.markerOptionsColor;
	}

	@Nullable
	@ColorInt
	public Integer getSecondaryColor() {
		return this.markerOptionsSecondaryColor;
	}

	@Nullable
	@ColorInt
	public Integer getDefaultColor() {
		return this.markerOptionsDefaultColor;
	}

	public void setInfoWindowAnchor(float anchorU, float anchorV) {
		if (marker != null) {
			marker.setInfoWindowAnchor(anchorU, anchorV);
		} else {
			markerOptions.infoWindowAnchor(anchorU, anchorV);
		}
	}

	public void setPosition(@NonNull LatLng position) {
		if (marker != null) {
			marker.setPosition(position);
		} else {
			markerOptions.position(position);
		}
	}

	public void setRotation(float rotation) {
		if (marker != null) {
			marker.setRotation(rotation);
		} else {
			markerOptions.rotation(rotation);
		}
	}

	public void setSnippet(@Nullable String snippet) {
		if (marker != null) {
			marker.setSnippet(snippet);
		} else {
			markerOptions.snippet(snippet);
		}
	}

	public void setTitle(@Nullable String title) {
		if (marker != null) {
			marker.setTitle(title);
		} else {
			markerOptions.title(title);
		}
	}

	public void setVisible(boolean visible) {
		if (marker != null) {
			marker.setVisible(visible);
		} else if (visible) {
			markerOptions.visible(true);
			createMarkerIfNecessary();
		}
	}

	public void showInfoWindow() {
		if (marker != null) {
			marker.showInfoWindow();
		}
	}

	@NonNull
	private Marker createMarkerIfNecessary() {
		if (marker == null) {
			final Context markerOptionsContext = markerOptionsContextWR == null ? null : markerOptionsContextWR.get();
			createMarker(map,
					markerOptions,
					markerOptionsColor,
					markerOptionsSecondaryColor,
					markerOptionsDefaultColor,
					markerOptionsIconResId,
					markerOptionsIconReplaceColor,
					markerOptionsContext,
					listener
			);
			map = null;
			markerOptions = null;
			markerOptionsIconResId = null;
			markerOptionsIconReplaceColor = null;
			if (markerOptionsContextWR != null) {
				markerOptionsContextWR.clear();
				markerOptionsContextWR = null;
			}
			listener = null;
		}
		return marker;
	}

	private void createMarker(@NonNull GoogleMap map,
							  @NonNull MarkerOptions options,
							  @ColorInt Integer markerOptionsColor,
							  @ColorInt Integer markerOptionsSecondaryColor,
							  @ColorInt Integer markerOptionsDefaultColor,
							  @DrawableRes Integer markerOptionsIconResId,
							  @Nullable Boolean markerOptionsIconReplaceColor,
							  @Nullable Context markerOptionsContext,
							  @Nullable OnMarkerCreateListener listener) {
		if (markerOptionsDefaultColor != null && markerOptionsIconResId != null && markerOptionsContext != null && markerOptionsIconReplaceColor != null) {
			final int color = markerOptionsColor == null ? markerOptionsSecondaryColor == null ? markerOptionsDefaultColor : markerOptionsSecondaryColor
					: markerOptionsColor;
			options.icon(MapUtils.getIcon(markerOptionsContext, markerOptionsIconResId, color, markerOptionsIconReplaceColor));
		}
		marker = map.addMarker(options);
		if (listener != null) {
			listener.onMarkerCreate(this);
		}
	}

	@NonNull
	private static MarkerOptions copy(@NonNull MarkerOptions options) {
		MarkerOptions copy = new MarkerOptions();
		copy.alpha(options.getAlpha());
		copy.anchor(options.getAnchorU(), options.getAnchorV());
		copy.draggable(options.isDraggable());
		copy.flat(options.isFlat());
		copy.icon(options.getIcon());
		copy.infoWindowAnchor(options.getInfoWindowAnchorU(), options.getInfoWindowAnchorV());
		copy.position(options.getPosition());
		copy.rotation(options.getRotation());
		copy.snippet(options.getSnippet());
		copy.title(options.getTitle());
		copy.visible(options.isVisible());
		return copy;
	}
}
