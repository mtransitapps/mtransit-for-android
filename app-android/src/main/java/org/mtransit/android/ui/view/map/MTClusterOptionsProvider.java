package org.mtransit.android.ui.view.map;

import android.content.Context;
import android.graphics.Color;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.android.gms.maps.model.BitmapDescriptor;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.util.MapUtils;
import org.mtransit.commons.CollectionUtils;

import java.lang.ref.WeakReference;
import java.util.List;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
public class MTClusterOptionsProvider implements ClusterOptionsProvider, MTLog.Loggable {

	private static final String LOG_TAG = MTClusterOptionsProvider.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	private final ClusterOptions clusterOptions = new ClusterOptions();

	@NonNull
	private final WeakReference<Context> contextWR;

	public MTClusterOptionsProvider(@Nullable Context context) {
		this.contextWR = new WeakReference<>(context);
	}

	@NonNull
	@Override
	public ClusterOptions getClusterOptions(@NonNull List<IMarker> markers, float zoom) {
		final MTMapIconDef defaultCustomIconDef = MTMapIconsProvider.getDefaultClusterIconDef();
		this.clusterOptions.anchor(defaultCustomIconDef.getAnchorU(), defaultCustomIconDef.getAnchorV());
		this.clusterOptions.flat(defaultCustomIconDef.getFlat());
		this.clusterOptions.icon(getClusterIcon(markers, defaultCustomIconDef.getZoomResId(zoom, null)));
		return this.clusterOptions;
	}

	@Nullable
	private BitmapDescriptor getClusterIcon(@NonNull List<IMarker> markers, @DrawableRes int iconResId) {
		final Context context = this.contextWR.get();
		final int color = getColor(context, markers);
		return MapUtils.getIcon(context, iconResId, color, false);
	}

	@ColorInt
	private int getColor(@Nullable Context context, @NonNull List<IMarker> markers) {
		final int defaultMapColor = getDefaultColor(context);
		try {
			if (CollectionUtils.getSize(markers) == 0) {
				return defaultMapColor;
			}
			Integer color = null;
			Integer secondaryColor = null;
			Integer defaultColor = null;
			boolean first = true;
			for (IMarker marker : markers) {
				if (first) {
					color = marker.getColor();
					secondaryColor = marker.getSecondaryColor();
					defaultColor = marker.getDefaultColor();
					first = false;
					continue;
				}
				if (color == null) {
					if (marker.getColor() != null) {
						color = defaultMapColor;
					}
				} else if (!color.equals(marker.getColor())) {
					color = defaultMapColor;
				}
				if (secondaryColor == null) {
					if (marker.getSecondaryColor() != null) {
						secondaryColor = defaultMapColor;
					}
				} else if (!secondaryColor.equals(marker.getSecondaryColor())) {
					secondaryColor = defaultMapColor;
				}
				if (defaultColor == null) {
					if (marker.getDefaultColor() != null) {
						defaultColor = defaultMapColor;
					}
				} else if (!defaultColor.equals(marker.getDefaultColor())) {
					defaultColor = defaultMapColor;
				}
				if (color != null && color.equals(defaultMapColor)
						&& secondaryColor != null && secondaryColor.equals(defaultMapColor)
						&& defaultColor != null && defaultColor.equals(defaultMapColor)) {
					return defaultMapColor;
				}
			}
			if (color != null && !color.equals(defaultMapColor)) {
				return color;
			} else if (secondaryColor != null && !secondaryColor.equals(defaultMapColor)) {
				return secondaryColor;
			} else if (defaultColor != null && !defaultColor.equals(defaultMapColor)) {
				return defaultColor;
			} else {
				return defaultMapColor;
			}
		} catch (Exception e) {
			MTLog.w(this, e, "Error while finding color!");
			return defaultMapColor;
		}
	}

	@ColorInt
	private int getDefaultColor(@Nullable Context context) {
		if (context != null && ColorUtils.isDarkTheme(context)) {
			return Color.WHITE;
		} else {
			return Color.BLACK;
		}
	}
}
