package org.mtransit.android.ui.fragment;

import java.util.Locale;

import org.mtransit.android.R;
import org.mtransit.android.commons.BundleUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.data.DataSourceType;
import org.mtransit.android.ui.MainActivity;

import android.content.Context;
import android.os.Bundle;

public class AgencyTypeFragment extends ABFragment {

	private static final String TAG = AgencyTypeFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG + "-" + this.type;
	}

	private static final String EXTRA_TYPE_ID = "extra_type_id";

	public static AgencyTypeFragment newInstance(DataSourceType type) {
		AgencyTypeFragment f = new AgencyTypeFragment();
		Bundle args = new Bundle();
		args.putInt(EXTRA_TYPE_ID, type.getId());
		f.setArguments(args);
		return f;
	}

	private DataSourceType type;

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);
		restoreInstanceState(savedInstanceState);
	}

	private void restoreInstanceState(Bundle savedInstanceState) {
		Integer typeId = BundleUtils.getInt(EXTRA_TYPE_ID, savedInstanceState, getArguments());
		if (typeId != null) {
			this.type = DataSourceType.parseId(typeId);
			((MainActivity) getActivity()).notifyABChange();
		}
	}

	@Override
	public CharSequence getTitle(Context context) {
		if (this.type == null) {
			return context.getString(R.string.ellipsis);
		}
		return context.getString(this.type.getShortNameResId()).toUpperCase(Locale.ENGLISH);
	}

	@Override
	public CharSequence getSubtitle(Context context) {
		return null;
	}

	@Override
	public int getIconDrawableResId() {
		return -1;
	}

}
