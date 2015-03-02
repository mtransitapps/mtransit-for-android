package org.mtransit.android.util;

import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.LinkUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ResourceUtils;

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

	private static final String TAG = MapUtils.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	public static final int DEFAULT_MARKET_COLOR = Color.WHITE;

	private static final String MAP_DIRECTION_URL_PART_1 = "http://maps.google.com/maps";
	private static final String MAP_DIRECTION_URL_SOURCE_ADDRESS_PARAM = "saddr";
	private static final String MAP_DIRECTION_URL_DESTINATION_ADDRESS_PARAM = "daddr";
	private static final String MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM = "dirflg";
	private static final String MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM_PUBLIC_TRANSIT_VALUE = "r";

	public static void showDirection(Activity activity, double lat, double lng, Double optStartLat, Double optStartLng, String optQuery) {
		Uri gmmIntentUri = Uri.parse(MAP_DIRECTION_URL_PART_1);
		if (optStartLat != null && optStartLng != null) {
			gmmIntentUri = gmmIntentUri.buildUpon().appendQueryParameter(MAP_DIRECTION_URL_SOURCE_ADDRESS_PARAM, optStartLat + "," + optStartLng).build();
		}
		gmmIntentUri = gmmIntentUri.buildUpon().appendQueryParameter(MAP_DIRECTION_URL_DESTINATION_ADDRESS_PARAM, lat + "," + lng).build();
		gmmIntentUri = gmmIntentUri.buildUpon()
				.appendQueryParameter(MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM, MAP_DIRECTION_URL_DIRECTION_FLAG_PARAM_PUBLIC_TRANSIT_VALUE).build();
		startMapIntent(activity, gmmIntentUri);
	}

	private static final String GOOGLE_MAPS_PKG = "com.google.android.apps.maps";

	private static void startMapIntent(Activity activity, Uri gmmIntentUri) {
		Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
		mapIntent.setPackage(GOOGLE_MAPS_PKG);
		String label = activity.getString(R.string.google_maps);
		if (mapIntent.resolveActivity(activity.getPackageManager()) == null) {
			mapIntent.setPackage(null); // clear Google Maps targeting
			label = activity.getString(R.string.map);
		}
		LinkUtils.open(activity, mapIntent, label, false);
	}

	private static final int MAP_WITH_BUTTONS_CAMERA_PADDING_IN_SP = 64;

	private static Integer mapWithButtonsCameraPaddingInPx = null;

	public static int getMapWithButtonsCameraPaddingInPx(Context context) {
		if (mapWithButtonsCameraPaddingInPx == null) {
			mapWithButtonsCameraPaddingInPx = (int) ResourceUtils.convertSPtoPX(context, MAP_WITH_BUTTONS_CAMERA_PADDING_IN_SP);
		}
		return mapWithButtonsCameraPaddingInPx;
	}

	private static final int MAP_WITHOUT_BUTTONS_CAMERA_PADDING_IN_SP = 32;

	private static Integer mapWithoutButtonsCameraPaddingInPx = null;

	public static int getMapWithoutButtonsCameraPaddingInPx(Context context) {
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

	private static LruCache<Pair<Integer, Integer>, BitmapDescriptor> cache = new LruCache<Pair<Integer, Integer>, BitmapDescriptor>(128);

	public static BitmapDescriptor getIcon(Context context, int iconResId, int color) {
		Pair<Integer, Integer> key = new Pair<Integer, Integer>(iconResId, color);
		if (color == Color.BLACK) {
			color = Color.DKGRAY; // black is too dark to colorize bitmap;
		}
		BitmapDescriptor cachedBitmap = cache.get(key);
		if (cachedBitmap != null) {
			return cachedBitmap;
		}
		Bitmap newBase = ColorUtils.colorizeBitmapResource(context, color, iconResId);
		BitmapDescriptor newBitmapDescriptor = BitmapDescriptorFactory.fromBitmap(newBase);
		cache.put(key, newBitmapDescriptor);
		return newBitmapDescriptor;
	}

}
