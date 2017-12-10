package org.mtransit.android.task;

import java.lang.ref.WeakReference;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.util.FragmentUtils;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;

public abstract class FragmentAsyncTaskV4<Params, Progress, Result, F extends Fragment> extends MTAsyncTask<Params, Progress, Result> implements
		MTLog.Loggable {

	private final WeakReference<F> fragmentWR;

	public FragmentAsyncTaskV4(@Nullable F fragment) {
		super();
		this.fragmentWR = new WeakReference<F>(fragment);
	}

	@Nullable
	public F getFragment() {
		return this.fragmentWR.get();
	}

	@SuppressWarnings("unchecked")
	@Nullable
	@Override
	protected Result doInBackgroundMT(Params... params) {
		F fragment = getFragment();
		if (fragment == null) {
			return null;
		}
		return doInBackgroundWithFragment(fragment, params);
	}

	@SuppressWarnings("unchecked")
	protected abstract Result doInBackgroundWithFragment(@NonNull F fragment, Params... params);

	@Override
	protected void onPostExecute(@Nullable Result result) {
		super.onPostExecute(result);
		F fragment = getFragment();
		if (!FragmentUtils.isFragmentReady(fragment)) {
			MTLog.d(this, "onPostExecute() > SKIP (fragment not ready)");
			return;
		}
		onPostExecuteFragmentReady(fragment, result);
	}

	protected abstract void onPostExecuteFragmentReady(@NonNull F fragment, @Nullable Result result);
}
