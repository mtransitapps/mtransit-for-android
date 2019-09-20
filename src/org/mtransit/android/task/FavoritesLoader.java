package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.Favorite;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.provider.FavoriteManager;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.SimpleArrayMap;
import androidx.collection.SparseArrayCompat;

public class FavoritesLoader extends MTAsyncTaskLoaderV4<ArrayList<POIManager>> {

	private static final String LOG_TAG = FavoritesLoader.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Nullable
	private ArrayList<POIManager> pois;

	public FavoritesLoader(@NonNull Context context) {
		super(context);
	}

	@NonNull
	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<>();
		ArrayList<Favorite> favorites = FavoriteManager.findFavorites(getContext());
		HashSet<Integer> favoriteFolderIds = new HashSet<>();
		HashMap<String, Integer> uuidToFavoriteFolderId = new HashMap<>();
		for (Favorite favorite : favorites) {
			uuidToFavoriteFolderId.put(favorite.getFkId(), favorite.getFolderId());
		}
		SimpleArrayMap<String, HashSet<String>> authorityToUUIDs = splitByAgency(favorites);
		if (authorityToUUIDs.size() > 0) {
			for (int a = 0; a < authorityToUUIDs.size(); a++) {
				String authority = authorityToUUIDs.keyAt(a);
				HashSet<String> authorityUUIDs = authorityToUUIDs.valueAt(a);
				if (authorityUUIDs != null && !authorityUUIDs.isEmpty()) {
					ArrayList<POIManager> agencyPOIs =
							DataSourceManager.findPOIs(getContext(), authority, POIProviderContract.Filter.getNewUUIDsFilter(authorityUUIDs));
					if (agencyPOIs != null) {
						CollectionUtils.sort(agencyPOIs, POIManager.POI_ALPHA_COMPARATOR);
						this.pois.addAll(agencyPOIs);
					}
				}
			}
		}
		CollectionUtils.sort(this.pois, new DataSourceType.POIManagerTypeShortNameComparator(getContext()));
		for (POIManager poim : this.pois) {
			Integer favoriteFolderId = uuidToFavoriteFolderId.get(poim.poi.getUUID());
			if (favoriteFolderId != null && favoriteFolderId > FavoriteManager.DEFAULT_FOLDER_ID) {
				poim.poi.setDataSourceTypeId(FavoriteManager.generateFavoriteFolderId(favoriteFolderId));
				favoriteFolderIds.add(favoriteFolderId);
			}
		}
		SparseArrayCompat<Favorite.Folder> favoriteFolders = FavoriteManager.findFolders(getContext());
		long textMessageId = TimeUtils.currentTimeMillis();
		for (int f = 0; f < favoriteFolders.size(); f++) {
			Favorite.Folder favoriteFolder = favoriteFolders.get(favoriteFolders.keyAt(f));
			if (favoriteFolder != null
					&& favoriteFolder.getId() > FavoriteManager.DEFAULT_FOLDER_ID
					&& !favoriteFolderIds.contains(favoriteFolder.getId())) {
				this.pois.add(FavoriteManager.getNewEmptyFolder(getContext(), textMessageId++, favoriteFolder.getId()));
			}
		}
		CollectionUtils.sort(this.pois, new Favorite.FavoriteFolderNameComparator(favoriteFolders));
		return this.pois;
	}

	@NonNull
	private static SimpleArrayMap<String, HashSet<String>> splitByAgency(@NonNull List<Favorite> favorites) {
		SimpleArrayMap<String, HashSet<String>> authorityToUUIDs = new SimpleArrayMap<>();
		for (Favorite favorite : favorites) {
			String uuid = favorite.getFkId();
			String authority = POI.POIUtils.extractAuthorityFromUUID(uuid);
			HashSet<String> uuids = authorityToUUIDs.get(authority);
			if (uuids == null) {
				uuids = new HashSet<>();
			}
			uuids.add(uuid);
			// TODO CRASH SimpleArrayMap ClassCastException: String cannot be cast to Object[]
			authorityToUUIDs.put(authority, uuids);
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
	public void deliverResult(@Nullable ArrayList<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}
}
