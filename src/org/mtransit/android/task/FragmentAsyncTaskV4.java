package org.mtransit.android.task;

import java.lang.ref.WeakReference;

import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.util.FragmentUtils;

import android.support.v4.app.Fragment;

public abstract class FragmentAsyncTaskV4<Params, Progress, Result> extends MTAsyncTask<Params, Progress, Result> implements MTLog.Loggable {

	private final WeakReference<Fragment> fragmentWR;

	public FragmentAsyncTaskV4(Fragment fragment) {
		super();
		this.fragmentWR = new WeakReference<Fragment>(fragment);
	}

	@Override
	protected void onPostExecute(Result result) {
		super.onPostExecute(result);
		Fragment fragment = this.fragmentWR == null ? null : this.fragmentWR.get();
		if (!FragmentUtils.isFragmentReady(fragment)) {
			MTLog.d(this, "onPostExecute() > SKIP (fragment not ready)");
			return;
		}
		onPostExecuteFragmentReady(result);
	}

	protected abstract void onPostExecuteFragmentReady(Result result);
}
