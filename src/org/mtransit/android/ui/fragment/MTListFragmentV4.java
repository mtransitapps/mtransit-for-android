package org.mtransit.android.ui.fragment;

import org.mtransit.android.commons.Constants;
import org.mtransit.android.commons.MTLog;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.support.v4.app.ListFragment;
import android.util.AttributeSet;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ContextMenu.ContextMenuInfo;
import android.widget.ListAdapter;
import android.widget.ListView;

/**
 * NO LOGIC HERE, just logs.
 */
public abstract class MTListFragmentV4 extends ListFragment implements MTLog.Loggable {

	public MTListFragmentV4() {
		super();
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "%s()", getLogTag());
		}
	}

	@Override
	public ListAdapter getListAdapter() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "getListAdapter()");
		}
		return super.getListAdapter();
	}

	@Override
	public ListView getListView() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "getListView()");
		}
		return super.getListView();
	}

	@Override
	public long getSelectedItemId() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "getSelectedItemId()");
		}
		return super.getSelectedItemId();
	}

	@Override
	public int getSelectedItemPosition() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "getSelectedItemPosition()");
		}
		return super.getSelectedItemPosition();
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onListItemClick(%s,%s,%s,%s)", l, v, position, id);
		}
		super.onListItemClick(l, v, position, id);
	}

	@Override
	public void setEmptyText(CharSequence text) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "setEmptyText(%s)", text);
		}
		super.setEmptyText(text);
	}

	@Override
	public void setListAdapter(ListAdapter adapter) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "setListAdapter(%s)", adapter);
		}
		super.setListAdapter(adapter);
	}

	@Override
	public void setListShown(boolean shown) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "setListShown(%s)", shown);
		}
		super.setListShown(shown);
	}

	@Override
	public void setListShownNoAnimation(boolean shown) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "setListShownNoAnimation(%s)", shown);
		}
		super.setListShownNoAnimation(shown);
	}

	@Override
	public void setSelection(int position) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "setSelection(%s)", position);
		}
		super.setSelection(position);
	}

	// INHERITED FROM FRAGMENT

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onActivityCreated(%s)", savedInstanceState);
		}
		super.onActivityCreated(savedInstanceState);
	}

	@Override
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onActivityResult(%s,%s,%s)", requestCode, resultCode, data);
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onAttach(Activity activity) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onAttach(%s)", activity);
		}
		super.onAttach(activity);
	}

	@Override
	public void onAttach(Context context) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onAttach(%s)", context);
		}
		super.onAttach(context);
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onConfigurationChanged(%s)", newConfig);
		}
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreate(%s)", savedInstanceState);
		}
		super.onCreate(savedInstanceState);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreateView(%s,%s,%s)", inflater, container, savedInstanceState);
		}
		return super.onCreateView(inflater, container, savedInstanceState);
	}

	@Override
	public void onDestroy() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDestroy()");
		}
		super.onDestroy();
	}

	@Override
	public void onDestroyView() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDestroyView()");
		}
		super.onDestroyView();
	}

	@Override
	public void onDetach() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onDetach()");
		}
		super.onDetach();
	}

	@SuppressWarnings("deprecation")
	@Override
	public void onInflate(Activity activity, AttributeSet attrs, Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onInflate(%s,%s,%s)", activity, attrs, savedInstanceState);
		}
		super.onInflate(activity, attrs, savedInstanceState);
	}

	@Override
	public void onInflate(Context context, AttributeSet attrs, Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onInflate(%s,%s,%s)", context, attrs, savedInstanceState);
		}
		super.onInflate(context, attrs, savedInstanceState);
	}

	@Override
	public void onLowMemory() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onLowMemory()");
		}
		super.onLowMemory();
	}

	@Override
	public void onPause() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPause()");
		}
		super.onPause();
	}

	@Override
	public void onResume() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onResume()");
		}
		super.onResume();
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onSaveInstanceState(%s)", outState);
		}
		super.onSaveInstanceState(outState);
	}

	@Override
	public void onStart() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onStart()");
		}
		super.onStart();
	}

	@Override
	public void onStop() {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onStop()");
		}
		super.onStop();
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onViewCreated(%s, %s)", view, savedInstanceState);
		}
		super.onViewCreated(view, savedInstanceState);
	}

	@Override
	public void onViewStateRestored(Bundle savedInstanceState) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onViewStateRestored(%s)", savedInstanceState);
		}
		super.onViewStateRestored(savedInstanceState);
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreateContextMenu(%s,%s,%s)", menu, v, menuInfo);
		}
		super.onCreateContextMenu(menu, v, menuInfo);
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onContextItemSelected(%s)", item);
		}
		return super.onContextItemSelected(item);
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onCreateOptionsMenu(%s,%s)", menu, inflater);
		}
		super.onCreateOptionsMenu(menu, inflater);
	}

	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onPrepareOptionsMenu(%s)", menu);
		}
		super.onPrepareOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		if (Constants.LOG_LIFECYCLE) {
			MTLog.v(this, "onOptionsItemSelected(%s)", item);
		}
		return super.onOptionsItemSelected(item);
	}

}
