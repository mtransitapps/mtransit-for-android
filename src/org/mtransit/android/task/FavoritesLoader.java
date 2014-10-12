package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.UriUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.task.MTAsyncTaskLoaderV4;
import org.mtransit.android.data.DataSourceProvider;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.Favorite;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.provider.FavoriteManager;

import android.content.Context;
import android.net.Uri;

public class FavoritesLoader extends MTAsyncTaskLoaderV4<List<POIManager>> {

	private static final String TAG = FavoritesLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private List<POIManager> pois;

	public FavoritesLoader(Context context) {
		super(context);
	}

	@Override
	public List<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<POIManager>();
		final List<Favorite> favorites = FavoriteManager.findFavorites(getContext());
		final HashMap<String, HashSet<String>> authorityToUUIDs = splitByAgency(favorites);
		if (authorityToUUIDs != null && authorityToUUIDs.size() > 0) {
			for (String authority : authorityToUUIDs.keySet()) {
				final HashSet<String> authorityUUIDs = authorityToUUIDs.get(authority);
				if (authorityUUIDs != null && authorityUUIDs.size() > 0) {
					final Uri contentUri = UriUtils.newContentUri(authority);
					final List<POIManager> agencyPOIs = DataSourceProvider.findPOIsWithUUIDs(getContext(), contentUri, authorityUUIDs);
					if (agencyPOIs != null) {
						Collections.sort(agencyPOIs, POIManager.POI_ALPHA_COMPATOR);
						pois.addAll(agencyPOIs);
					}
				}
			}
		}
		CollectionUtils.sort(this.pois, new DataSourceType.POIManagerTypeShortNameComparator(getContext()));
		return pois;
	}

	private HashMap<String, HashSet<String>> splitByAgency(List<Favorite> favorites) {
		HashMap<String, HashSet<String>> authorityToUUIDs = new HashMap<String, HashSet<String>>();
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				final String uuid = favorite.getFkId();
				final String authority = POI.POIUtils.extractAuthorityFromUUID(uuid);
				if (!authorityToUUIDs.containsKey(authority)) {
					authorityToUUIDs.put(authority, new HashSet<String>());
				}
				authorityToUUIDs.get(authority).add(uuid);
			}
		}
		return authorityToUUIDs;
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		if (this.pois != null) {
			deliverResult(this.pois);
		}
		if (this.pois == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	public void deliverResult(List<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

}
