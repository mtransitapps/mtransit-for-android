package org.mtransit.android.task;

import java.util.ArrayList;

import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.data.Route;
import org.mtransit.android.data.DataSourceManager;

import android.content.Context;

public class RTSAgencyRoutesLoader extends MTAsyncTaskLoaderV4<ArrayList<Route>> {

	private static final String TAG = RTSAgencyRoutesLoader.class.getSimpleName();

	@Override
	public String getLogTag() {
		if (this.authority != null) {
			return TAG + "-" + this.authority;
		}
		return TAG;
	}

	private String authority;
	private ArrayList<Route> routes;

	public RTSAgencyRoutesLoader(Context context, String authority) {
		super(context);
		this.authority = authority;
	}

	@Override
	public ArrayList<Route> loadInBackgroundMT() {
		if (this.routes != null) {
			return this.routes;
		}
		this.routes = new ArrayList<Route>();
		this.routes = DataSourceManager.findAllRTSAgencyRoutes(getContext(), this.authority);
		CollectionUtils.sort(this.routes, Route.SHORT_NAME_COMPARATOR);
		return routes;
	}

	@Override
	protected void onStartLoading() {
		super.onStartLoading();
		if (this.routes != null) {
			deliverResult(this.routes);
		}
		if (this.routes == null) {
			forceLoad();
		}
	}

	@Override
	protected void onStopLoading() {
		super.onStopLoading();
		cancelLoad();
	}

	@Override
	public void deliverResult(ArrayList<Route> data) {
		this.routes = data;
		if (isStarted()) {
			super.deliverResult(data);
		}
	}

}
