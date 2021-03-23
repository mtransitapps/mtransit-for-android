package org.mtransit.android.ui.view;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.commons.MTLog;

public abstract class MTOnClickListener implements View.OnClickListener, MTLog.Loggable {

	private static final String LOG_TAG = MTOnClickListener.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@Override
	public void onClick(@NonNull View view) {
		onClickS(view, this);
	}

	public abstract void onClickMT(@NonNull View view);

	private static void onClickS(@Nullable final View view,
								 @Nullable final MTOnClickListener listener
	) {
		if (listener == null) {
			MTLog.d(LOG_TAG, "onClickS() > SKIP (no listener)");
			return;
		}
		if (view == null) {
			MTLog.d(LOG_TAG, "onClickS() > SKIP (no view)");
			return;
		}
		view.post(() ->
				listener.onClickMT(view)
		);
	}
}
