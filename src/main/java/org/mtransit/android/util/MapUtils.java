package org.mtransit.android.util;

import androidx.annotation.ColorInt;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.R;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ResourceUtils;

import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.util.LruCache;
import android.util.Pair;
import android.view.ViewGroup;

public final class MapUtils implements MTLog.Loggable {

	private static final String LOG_TAG = MapUtils.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	public static final int MAP_TYPE_NORMAL = GoogleMap.MAP_TYPE_NORMAL;
	public static final int MAP_TYPE_SATELLITE = GoogleMap.MAP_TYPE_HYBRID;

	public static final String PREFS_LCL_MAP_TYPE = "pMapType";
	public static final int PREFS_LCL_MAP_TYPE_DEFAULT = MAP_TYPE_NORMAL;

	public static final int DEFAULT_MARKET_COLOR = Color.WHITE;

	private static final String MAP_DIRECTION_URL_PART_1 = "https://maps.google.com/maps";
	private static final String MAP_DIRECTION_URL_SOURCE_ADDRESS_PARAM = "saddr";
	private static final String MAP_DIRECTION_URL_DESTINATION_ADDRESS_PARAM = "daddr";
	private static final String MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM = "dirflg";
	private static final String MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM_PUBLIC_TRANSIT_VALUE = "r";

	public static void showDirection(Activity activity, //
			@Nullable Double optDestLat, @Nullable Double optDestLng, //
			@Nullable Double optSrcLat, @Nullable Double optSrcLng, //
			@Nullable String optQuery) {
		Uri gmmIntentUri = Uri.parse(MAP_DIRECTION_URL_PART_1);
		if (optSrcLat != null && optSrcLng != null) {
			gmmIntentUri = gmmIntentUri //
					.buildUpon() //
					.appendQueryParameter(MAP_DIRECTION_URL_SOURCE_ADDRESS_PARAM, optSrcLat + "," + optSrcLng) //
					.build();
		}
		if (optDestLat != null && optDestLng != null) {
			gmmIntentUri = gmmIntentUri //
					.buildUpon() //
					.appendQueryParameter(MAP_DIRECTION_URL_DESTINATION_ADDRESS_PARAM, optDestLat + "," + optDestLng) //
					.build();
		}
		gmmIntentUri = gmmIntentUri //
				.buildUpon() //
				.appendQueryParameter(MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM, MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM_PUBLIC_TRANSIT_VALUE) //
				.build();
		startMapIntent(activity, gmmIntentUri);
	}

	private static final String GOOGLE_MAPS_PKG = "com.google.android.apps.maps";
	private static final String GOOGLE_MAPS_LITE_PKG = "com.google.android.apps.mapslite";

	private static void startMapIntent(@NonNull Activity activity, Uri gmmIntentUri) {
		Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
		mapIntent.setPackage(GOOGLE_MAPS_PKG);
		String label = activity.getString(R.string.google_maps);
		if (mapIntent.resolveActivity(activity.getPackageManager()) == null) {
			mapIntent.setPackage(GOOGLE_MAPS_LITE_PKG); // try with Maps Lite
		}
		if (mapIntent.resolveActivity(activity.getPackageManager()) == null) {
			mapIntent.setPackage(null); // clear Google Maps targeting
			label = activity.getString(R.string.map);
		}
		LinkUtils.open(activity, mapIntent, label, false);
	}

	private static final int MAP_WITH_BUTTONS_CAMERA_PADDING_IN_SP = 64;

	@Nullable
	private static Integer mapWithButtonsCameraPaddingInPx = null;

	public static int getMapWithButtonsCameraPaddingInPx(@Nullable Context context) {
		if (mapWithButtonsCameraPaddingInPx == null) {
			mapWithButtonsCameraPaddingInPx = (int) ResourceUtils.convertSPtoPX(context, MAP_WITH_BUTTONS_CAMERA_PADDING_IN_SP);
		}
		return mapWithButtonsCameraPaddingInPx;
	}

	private static final int MAP_WITHOUT_BUTTONS_CAMERA_PADDING_IN_SP = 32;

	@Nullable
	private static Integer mapWithoutButtonsCameraPaddingInPx = null;

	public static int getMapWithoutButtonsCameraPaddingInPx(@Nullable Context context) {
		if (mapWithoutButtonsCameraPaddingInPx == null) {
			mapWithoutButtonsCameraPaddingInPx = (int) ResourceUtils.convertSPtoPX(context, MAP_WITHOUT_BUTTONS_CAMERA_PADDING_IN_SP);
		}
		return mapWithoutButtonsCameraPaddingInPx;
	}

	public static void fixScreenFlickering(ViewGroup viewGroup) {
		// https://code.google.com/p/gmaps-api-issues/issues/detail?id=4639
		if (viewGroup == null) {
			return;
		}
		viewGroup.requestTransparentRegion(viewGroup);
	}

	@NonNull
	private static LruCache<Pair<Integer, Integer>, BitmapDescriptor> cache = new LruCache<>(128);

	@NonNull
	public static BitmapDescriptor getIcon(@Nullable Context context, @DrawableRes int iconResId, @ColorInt int color, boolean replaceColor) {
		Pair<Integer, Integer> key = new Pair<>(iconResId, color);
		if (color == Color.BLACK) {
			color = Color.DKGRAY; // black is too dark to colorize bitmap;
		}
		if (color == Color.WHITE) {
			color = Color.LTGRAY;
		}
		BitmapDescriptor cachedBitmap = cache.get(key);
		if (cachedBitmap != null) {
			return cachedBitmap;
		}
		Bitmap newBase = ColorUtils.colorizeBitmapResource(context, color, iconResId, replaceColor);
		BitmapDescriptor newBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(newBase);
		cache.put(key, newBitmapDescriptor);
		return newBitmapDescriptor;
	}
}
