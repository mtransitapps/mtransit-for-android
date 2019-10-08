package org.mtransit.android.task;

import java.util.ArrayList;
import java.util.HashSet;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.provider.POIProviderContract;
import org.mtransit.android.data.DataSourceManager;
import org.mtransit.android.data.POIManager;

import android.content.Context;
import androidx.collection.ArrayMap;

public class POIsLoader extends MTAsyncTaskLoaderV4<ArrayList<POIManager>> {

	private static final String TAG = POIsLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private ArrayList<String> uuids;
	private ArrayList<String> authorities;
	private ArrayList<POIManager> pois;

	public POIsLoader(Context context, ArrayList<String> uuids, ArrayList<String> authorities) {
		super(context);
		this.uuids = uuids;
		this.authorities = authorities;
	}

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

	private ArrayMap<String, HashSet<String>> splitByAgency(ArrayList<String> uuids, ArrayList<String> authorities) {
		ArrayMap<String, HashSet<String>> authorityToUUIDs = new ArrayMap<>();
		if (uuids != null) {
			for (int i = 0; i < uuids.size(); i++) {
				String uuid = uuids.get(i);
				String authority = authorities.get(i);
				if (!authorityToUUIDs.containsKey(authority)) {
					authorityToUUIDs.put(authority, new HashSet<>());
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
