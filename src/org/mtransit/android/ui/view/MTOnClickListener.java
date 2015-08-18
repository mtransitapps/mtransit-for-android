package org.mtransit.android.ui.view;

import android.view.View;

public abstract class MTOnClickListener implements View.OnClickListener {

	@Override
	public void onClick(View view) {
		onClickS(view, this);
	}

	public abstract void onClickMT(View view);

	public static void onClickS(final View view, final MTOnClickListener listener) {
		if (listener == null) {
			return;
		}
		if (view == null) {
			listener.onClickMT(null);
			return;
		}
		view.post(new Runnable() {
			@Override
			public void run() {
				listener.onClickMT(view);
			}
		});
	}
}
