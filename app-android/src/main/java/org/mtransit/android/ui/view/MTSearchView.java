package org.mtransit.android.ui.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.View;
import android.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.R;
import org.mtransit.android.commons.KeyboardUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.ui.MainActivity;

import java.lang.ref.WeakReference;

@SuppressLint("ViewConstructor")
public class MTSearchView extends SearchView implements MTLog.Loggable, View.OnFocusChangeListener, SearchView.OnCloseListener, SearchView.OnQueryTextListener {

	private static final String TAG = MTSearchView.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	public MTSearchView(@Nullable MainActivity mainActivity, @NonNull Context context) {
		super(context);
		init(mainActivity);
	}

	@Nullable
	private WeakReference<MainActivity> mainActivityWR;

	private void init(@Nullable MainActivity mainActivity) {
		this.mainActivityWR = new WeakReference<>(mainActivity);
		setQueryHint(getContext().getString(R.string.search_hint));
		setIconifiedByDefault(true);
		setOnQueryTextListener(this);
		setOnQueryTextFocusChangeListener(this);
		setOnCloseListener(this);
		setIconified(false);
	}

	@Override
	public boolean onQueryTextChange(String newText) {
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity != null) {
			mainActivity.onSearchQueryRequested(newText);
		}
		return true; // handled
	}

	@Override
	public boolean onQueryTextSubmit(String query) {
		final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
		if (mainActivity != null) {
			mainActivity.onSearchQueryRequested(query);
			KeyboardUtils.hideKeyboard(mainActivity, this);
		}
		return true; // handled
	}

	@Override
	public void onFocusChange(View v, boolean hasFocus) {
		if (!hasFocus) {
			final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				KeyboardUtils.hideKeyboard(mainActivity, this);
			}
		} else {
			final MainActivity mainActivity = this.mainActivityWR == null ? null : this.mainActivityWR.get();
			if (mainActivity != null) {
				KeyboardUtils.showKeyboard(mainActivity, this);
			}
		}
	}

	@Override
	public boolean onClose() {
		return true; // do not close
	}
}
