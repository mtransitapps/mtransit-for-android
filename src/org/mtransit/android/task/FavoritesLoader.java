package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.TimeUtils;
import org.mtransit.android.commons.data.POI;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.data.Favorite;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.data.TextMessage;
import org.mtransit.android.provider.FavoriteManager;

import android.content.Context;
import android.support.v4.util.ArrayMap;

public class FavoritesLoader extends MTAsyncTaskLoaderV4<ArrayList<POIManager>> {

	private static final String TAG = FavoritesLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ArrayList<POIManager> pois;

	public FavoritesLoader(Context context) {
		super(context);
	}

	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<POIManager>();
		ArrayList<Favorite> favorites = FavoriteManager.findFavorites(getContext());
		HashSet<Integer> favoriteFolderIds = new HashSet<Integer>();
		HashMap<String, Integer> uuidToFavoriteFolderId = new HashMap<String, Integer>();
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				uuidToFavoriteFolderId.put(favorite.getFkId(), favorite.getFolderId());
			}
		}
		ArrayMap<String, HashSet<String>> authorityToUUIDs = splitByAgency(favorites);
		if (authorityToUUIDs != null && authorityToUUIDs.size() > 0) {
			for (String authority : authorityToUUIDs.keySet()) {
				HashSet<String> authorityUUIDs = authorityToUUIDs.get(authority);
				if (CollectionUtils.getSize(authorityUUIDs) > 0) {
					ArrayList<POIManager> agencyPOIs = DataSourceManager.findPOIs(getContext(), authority,
							POIProviderContract.Filter.getNewUUIDsFilter(authorityUUIDs));
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
		HashMap<Integer, Favorite.Folder> favoriteFoders = FavoriteManager.findFolders(getContext());
		if (favoriteFoders != null) {
			long textMessageId = TimeUtils.currentTimeMillis();
			for (Favorite.Folder favoriteFolder : favoriteFoders.values()) {
				if (favoriteFolder.getId() > FavoriteManager.DEFAULT_FOLDER_ID && !favoriteFolderIds.contains(favoriteFolder.getId())) {
					TextMessage textMessage = new TextMessage(textMessageId++, getContext().getString(R.string.favorite_folder_empty));
					textMessage.setDataSourceTypeId(FavoriteManager.generateFavoriteFolderId(favoriteFolder.getId()));
					this.pois.add(new POIManager(textMessage));
				}
			}
		}
		CollectionUtils.sort(this.pois, new Favorite.FavoriteFolderNameComparator(favoriteFoders));
		return this.pois;
	}

	public static ArrayMap<String, HashSet<String>> splitByAgency(ArrayList<Favorite> favorites) {
		ArrayMap<String, HashSet<String>> authorityToUUIDs = new ArrayMap<String, HashSet<String>>();
		if (favorites != null) {
			for (Favorite favorite : favorites) {
				String uuid = favorite.getFkId();
				String authority = POI.POIUtils.extractAuthorityFromUUID(uuid);
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
	public void deliverResult(ArrayList<POIManager> data) {
		this.pois = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

}
