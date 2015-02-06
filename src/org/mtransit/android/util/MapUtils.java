package org.mtransit.android.util;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ResourceUtils;
import org.mtransit.android.commons.task.MTAsyncTask;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.view.View;

import com.google.android.gms.maps.CameraUpdate;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.MapView;
import com.google.android.gms.maps.MapsInitializer;
import com.google.android.gms.maps.model.LatLngBounds;

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

	private static void startMapIntent(Activity activity, Uri gmmIntentUri) {
		Intent mapIntent = new Intent(Intent.ACTION_VIEW, gmmIntentUri);
		mapIntent.setPackage("com.google.android.apps.maps");
		if (mapIntent.resolveActivity(activity.getPackageManager()) == null) {
			mapIntent.setPackage(null); // clear Google Maps targeting
		}
		if (mapIntent.resolveActivity(activity.getPackageManager()) != null) {
			activity.startActivity(mapIntent);
		}
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

	public static void initMapAsync(final Context context) {
		new MTAsyncTask<Void, Void, Void>() {
			@Override
			public String getLogTag() {
				return TAG;
			}

			@Override
			protected Void doInBackgroundMT(Void... params) {
				MapsInitializer.initialize(context);
				return null;
			}
		}.execute();
	}

	public static boolean updateMapPosition(Context optContext, GoogleMap map, MapView optMapView, boolean anim, LatLngBounds llb, int cameraPaddingInPx) {
		if (map == null) {
			return false;
		}
		try {
			if (!anim) {
				if (optMapView != null) {
					if (optMapView.getVisibility() == View.VISIBLE) {
						optMapView.setVisibility(View.INVISIBLE);
					}
				}
			}
			CameraUpdate cameraUpdate = CameraUpdateFactory.newLatLngBounds(llb, cameraPaddingInPx);
			if (anim) {
				map.animateCamera(cameraUpdate);
			} else {
				map.moveCamera(cameraUpdate);
			}
			if (optMapView != null) {
				if (optMapView.getVisibility() != View.VISIBLE) {
					optMapView.setVisibility(View.VISIBLE);
				}
			}
			return true;
		} catch (IllegalStateException ise) {
			MTLog.w(TAG, "Error while initializing map position: %s", ise);
			return false;
		} catch (Exception e) {
			MTLog.w(TAG, e, "Error while initializing map position!");
			return false;
		}
	}

}
