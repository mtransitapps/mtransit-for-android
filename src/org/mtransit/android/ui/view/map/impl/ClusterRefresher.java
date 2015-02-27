package org.mtransit.android.ui.view.map.impl;

import android.os.Handler;
import android.os.Handler.Callback;
import android.os.Message;

import java.util.HashSet;
import java.util.Set;

// based on Maciej Górski's Android Maps Extensions library (Apache License, Version 2.0)
class ClusterRefresher {

	private Set<ClusterMarker> refreshQueue = new HashSet<ClusterMarker>();
	private boolean refreshPending;
	private Handler refresher = new Handler(new Callback() {
		@Override
		public boolean handleMessage(Message msg) {
			refreshAll();
			return true;
		}
	});

	void refresh(ClusterMarker cluster) {
		refreshQueue.add(cluster);
		if (!refreshPending) {
			refresher.sendEmptyMessage(0);
			refreshPending = true;
		}
	}

	void cleanup() {
		refreshQueue.clear();
		refreshPending = false;
		refresher.removeMessages(0);
	}

	void refreshAll() {
		for (ClusterMarker cluster : refreshQueue) {
			cluster.refresh();
		}
		cleanup();
	}
}
