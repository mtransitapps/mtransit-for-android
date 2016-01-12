package org.mtransit.android.ui.fragment;

import java.util.ArrayList;
import java.util.HashMap;

import org.mtransit.android.R;
import org.mtransit.android.commons.LocationUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.TaskUtils;
import org.mtransit.android.commons.ToastUtils;
import org.mtransit.android.commons.task.MTAsyncTask;
import org.mtransit.android.data.Favorite;
import org.mtransit.android.data.POIArrayAdapter;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.provider.FavoriteManager;
import org.mtransit.android.task.FavoritesLoader;
import org.mtransit.android.ui.MTActivityWithLocation;
import org.mtransit.android.util.LoaderUtils;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.location.Location;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewStub;
import android.widget.AbsListView;
import android.widget.EditText;
import android.widget.TextView;

public class FavoritesFragment extends ABFragment implements LoaderManager.LoaderCallbacks<ArrayList<POIManager>>, MTActivityWithLocation.UserLocationListener,
		FavoriteManager.FavoriteUpdateListener {

	private static final String TAG = FavoritesFragment.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final String TRACKING_SCREEN_NAME = "Favorites";

	@Override
	public String getScreenName() {
		return TRACKING_SCREEN_NAME;
	}

	public static FavoritesFragment newInstance() {
		return new FavoritesFragment();
	}

	private POIArrayAdapter adapter;
	private CharSequence emptyText = null;
	private Location userLocation;

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);
		initAdapters(activity);
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
		super.onCreateView(inflater, container, savedInstanceState);
		View view = inflater.inflate(R.layout.fragment_favorites, container, false);
		setupView(view);
		return view;
	}

	@Override
	public void onResume() {
		super.onResume();
		View view = getView();
		if (this.modulesUpdated) {
			if (view != null) {
				view.post(new Runnable() {
					@Override
					public void run() {
						if (FavoritesFragment.this.modulesUpdated) {
							onModulesUpdated();
						}
					}
				});
			}
		}
		switchView(view);
		if (this.adapter != null && this.adapter.isInitialized()) {
			this.adapter.onResume(getActivity(), this.userLocation);
		} else {
			resetFavoriteFolders();
			LoaderUtils.restartLoader(this, FAVORITES_LOADER, null, this);
		}
		onUserLocationChanged(((MTActivityWithLocation) getActivity()).getUserLocation());
	}

	private static final int FAVORITES_LOADER = 0;

	@Override
	public Loader<ArrayList<POIManager>> onCreateLoader(int id, Bundle args) {
		switch (id) {
		case FAVORITES_LOADER:
			getFavoriteFoldersOrNull(); // load favorite folder if not loaded yet
			return new FavoritesLoader(getActivity());
		default:
			MTLog.w(this, "Loader id '%s' unknown!", id);
			return null;
		}
	}

	@Override
	public void onLoaderReset(Loader<ArrayList<POIManager>> loader) {
		if (this.adapter != null) {
			this.adapter.clear();
		}
	}

	@Override
	public void onLoadFinished(Loader<ArrayList<POIManager>> loader, ArrayList<POIManager> data) {
		this.emptyText = getString(R.string.no_favorites);
		this.adapter.setPois(data);
		this.adapter.updateDistanceNowAsync(this.userLocation);
		switchView(getView());
	}

	private HashMap<Integer, Favorite.Folder> favoriteFolders = null;

	private boolean hasFavoriteFolders() {
		if (this.favoriteFolders == null) {
			initFavoriteFoldersAsync();
			return false;
		}
		return true;
	}

	private void initFavoriteFoldersAsync() {
		if (this.loadFavoriteFoldersTask != null && this.loadFavoriteFoldersTask.getStatus() == MTAsyncTask.Status.RUNNING) {
			return;
		}
		this.loadFavoriteFoldersTask = new LoadFavoriteFoldersTask();
		TaskUtils.execute(this.loadFavoriteFoldersTask);
	}

	private LoadFavoriteFoldersTask loadFavoriteFoldersTask = null;

	private class LoadFavoriteFoldersTask extends MTAsyncTask<Void, Void, Boolean> {
		@Override
		public String getLogTag() {
			return FavoritesFragment.this.getLogTag() + ">" + LoadFavoriteFoldersTask.class.getSimpleName();
		}

		@Override
		protected Boolean doInBackgroundMT(Void... params) {
			return initFavoriteFoldersSync();
		}

		@Override
		protected void onPostExecute(Boolean result) {
			super.onPostExecute(result);
			if (result) {
				applyNewFavoriteFolders();
			}
		}
	}

	private void resetFavoriteFolders() {
		this.favoriteFolders = null;
	}

	private HashMap<Integer, Favorite.Folder> getFavoriteFoldersOrNull() {
		if (!hasFavoriteFolders()) {
			return null;
		}
		return this.favoriteFolders;
	}

	private boolean initFavoriteFoldersSync() {
		if (this.favoriteFolders != null) {
			return false;
		}
		this.favoriteFolders = FavoriteManager.findFolders(getContext());
		return this.favoriteFolders != null;
	}

	private void applyNewFavoriteFolders() {
		if (this.favoriteFolders == null) {
			return;
		}
		if (this.adapter != null) {
			this.adapter.setFavoriteFolders(this.favoriteFolders);
		}
	}

	@Override
	public void onUserLocationChanged(Location newLocation) {
		if (newLocation != null) {
			if (this.userLocation == null || LocationUtils.isMoreRelevant(getLogTag(), this.userLocation, newLocation)) {
				this.userLocation = newLocation;
				if (this.adapter != null) {
					this.adapter.setLocation(newLocation);
				}
			}
		}
	}

	@Override
	public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.menu_favorites, menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case R.id.menu_add_favorite_folder:
			LayoutInflater inflater = getActivity().getLayoutInflater();
			View view = inflater.inflate(R.layout.layout_favorites_folder_edit, null);
			final EditText newFolderNameTv = (EditText) view.findViewById(R.id.folder_name);
			new AlertDialog.Builder(getActivity()).setView(view).setPositiveButton(R.string.favorite_folder_new_create, new DialogInterface.OnClickListener() {
				@Override
				public void onClick(DialogInterface dialog, int id) {
					String newFolderName = newFolderNameTv.getText().toString();
					if (TextUtils.isEmpty(newFolderName)) {
						ToastUtils.makeTextAndShowCentered(getContext(), R.string.favorite_folder_new_invalid_name);
						return;
					}
					Favorite.Folder createdFolder = FavoriteManager.addFolder(getContext(), new Favorite.Folder(newFolderName));
					if (createdFolder == null) {
						ToastUtils.makeTextAndShowCentered(getContext(),
								getContext().getString(R.string.favorite_folder_new_creation_error_and_folder_name, newFolderName));
					} else {
						onFavoriteUpdated();
						ToastUtils.makeTextAndShowCentered(getContext(),
								getContext().getString(R.string.favorite_folder_new_created_and_folder_name, newFolderName));
					}
				}
			}).setNegativeButton(R.string.favorite_folder_new_cancel, new DialogInterface.OnClickListener() {
				public void onClick(DialogInterface dialog, int id) {
					if (dialog != null) {
						dialog.cancel();
					}
				}
			}).show();
			return true;
		}
		return super.onOptionsItemSelected(item);
	}

	@Override
	public void onPause() {
		super.onPause();
		if (this.adapter != null) {
			this.adapter.onPause();
		}
	}

	@Override
	public void onDestroy() {
		super.onDestroy();
		if (this.adapter != null) {
			this.adapter.onDestroy();
			this.adapter = null;
		}
	}

	private void initAdapters(Activity activity) {
		this.adapter = new POIArrayAdapter(activity);
		this.adapter.setTag(getLogTag());
		this.adapter.setShowFavorite(false); // all items in this screen are favorites
		this.adapter.setFavoriteUpdateListener(this);
		this.adapter.setShowTypeHeader(POIArrayAdapter.TYPE_HEADER_ALL_NEARBY);
	}

	private void setupView(View view) {
		if (view == null) {
			return;
		}
		inflateList(view);
		switchView(view);
		linkAdapterWithListView(view);
	}

	private void linkAdapterWithListView(View view) {
		if (view == null || this.adapter == null) {
			return;
		}
		View listView = view.findViewById(R.id.list);
		if (listView != null) {
			this.adapter.setListView((AbsListView) listView);
		}
	}

	@Override
	public void onFavoriteUpdated() {
		resetFavoriteFolders();
		LoaderUtils.restartLoader(this, FAVORITES_LOADER, null, this);
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!isResumed()) {
			return;
		}
		LoaderUtils.restartLoader(this, FAVORITES_LOADER, null, this);
		this.modulesUpdated = false; // processed
	}

	private void switchView(View view) {
		if (view == null) {
			return;
		}
		if (this.adapter == null || !this.adapter.isInitialized()) {
			showLoading(view);
		} else if (this.adapter.getPoisCount() == 0) {
			showEmpty(view);
		} else {
			showList(view);
		}
	}

	private void showList(View view) {
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		inflateList(view);
		view.findViewById(R.id.list).setVisibility(View.VISIBLE); // show
	}

	private void inflateList(View view) {
		if (view.findViewById(R.id.list) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.list_stub)).inflate(); // inflate
		}
	}

	private void showLoading(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) != null) { // IF inflated/present DO
			view.findViewById(R.id.empty).setVisibility(View.GONE); // hide
		}
		view.findViewById(R.id.loading).setVisibility(View.VISIBLE); // show
	}

	private void showEmpty(View view) {
		if (view.findViewById(R.id.list) != null) { // IF inflated/present DO
			view.findViewById(R.id.list).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.loading) != null) { // IF inflated/present DO
			view.findViewById(R.id.loading).setVisibility(View.GONE); // hide
		}
		if (view.findViewById(R.id.empty) == null) { // IF NOT present/inflated DO
			((ViewStub) view.findViewById(R.id.empty_stub)).inflate(); // inflate
		}
		if (!TextUtils.isEmpty(this.emptyText)) {
			((TextView) view.findViewById(R.id.empty_text)).setText(this.emptyText);
		}
		view.findViewById(R.id.empty).setVisibility(View.VISIBLE); // show
	}

	@Override
	public CharSequence getABTitle(Context context) {
		return context.getString(R.string.favorites);
	}
}
