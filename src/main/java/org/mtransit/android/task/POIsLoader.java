package org.mtransit.android.task;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.collection.ArrayMap;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

public class POIsLoader extends MTAsyncTaskLoaderX<ArrayList<POIManager>> {

	private static final String TAG = POIsLoader.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	@NonNull
	private final ArrayList<String> uuids;
	@NonNull
	private final ArrayList<String> authorities;
	@Nullable
	private ArrayList<POIManager> pois;

	public POIsLoader(@NonNull Context context, @NonNull ArrayList<String> uuids, @NonNull ArrayList<String> authorities) {
		super(context);
		this.uuids = uuids;
		this.authorities = authorities;
	}

	@Nullable
	@Override
	public ArrayList<POIManager> loadInBackgroundMT() {
		if (this.pois != null) {
			return this.pois;
		}
		this.pois = new ArrayList<>();
		ArrayMap<String, HashSet<String>> authorityToUUIDs = splitByAgency(this.uuids, this.authorities);
		if (authorityToUUIDs != null && authorityToUUIDs.size() > 0) {
			for (String authority : authorityToUUIDs.keySet()) {
				HashSet<String> authorityUUIDs = authorityToUUIDs.get(authority);
				if (authorityUUIDs != null && authorityUUIDs.size() > 0) {
					ArrayList<POIManager> agencyPOIs = DataSourceManager.findPOIs(getContext(), authority,
							POIProviderContract.Filter.getNewUUIDsFilter(authorityUUIDs));
					if (agencyPOIs != null) {
						this.pois.addAll(agencyPOIs);
					}
				}
			}
		}
		CollectionUtils.sort(this.pois, POIManager.POI_ALPHA_COMPARATOR);
		return this.pois;
	}

	@NonNull
	private ArrayMap<String, HashSet<String>> splitByAgency(@NonNull List<String> uuids, @NonNull List<String> authorities) {
		ArrayMap<String, HashSet<String>> authorityToUUIDs = new ArrayMap<>();
		for (int i = 0; i < uuids.size(); i++) {
			String uuid = uuids.get(i);
			String authority = authorities.get(i);
			if (!authorityToUUIDs.containsKey(authority)) {
				authorityToUUIDs.put(authority, new HashSet<>());
			}
			authorityToUUIDs.get(authority).add(uuid);
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
