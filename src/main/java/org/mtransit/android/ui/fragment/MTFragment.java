package org.mtransit.android.ui.fragment;

import android.content.Context;

import androidx.annotation.CallSuper;
import androidx.annotation.ContentView;
import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.fragment.app.FragmentOnAttachListener;

import org.mtransit.android.common.IContext;
import org.mtransit.android.commons.MTLog;

import java.util.WeakHashMap;

public abstract class MTFragment extends MTFragmentX implements IContext, MTLog.Loggable, FragmentOnAttachListener {

	@NonNull
	private final WeakHashMap<Fragment, Object> childFragmentsWR = new WeakHashMap<>();

	public MTFragment() {
		super();
	}

	@SuppressWarnings("WeakerAccess")
	@ContentView
	public MTFragment(@LayoutRes int contentLayoutId) {
		super(contentLayoutId);
	}

	@CallSuper
	@Override
	public void onAttach(@NonNull Context context) {
		super.onAttach(context);
		getChildFragmentManager().addFragmentOnAttachListener(this);
	}

	@Override
	public void onAttachFragment(@NonNull FragmentManager fragmentManager, @NonNull Fragment fragment) {
		childFragmentsWR.put(fragment, null);
	}

	@Override
	public void onDetach() {
		super.onDetach();
		getChildFragmentManager().removeFragmentOnAttachListener(this);
	}

	@Nullable
	public java.util.Set<Fragment> getChildFragments() {
		return childFragmentsWR.keySet();
	}
}
