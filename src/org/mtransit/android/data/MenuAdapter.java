package org.mtransit.android.data;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Iterator;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ui.widget.MTBaseAdapter;
import org.mtransit.android.ui.PreferencesActivity;
import org.mtransit.android.ui.fragment.ABFragment;
import org.mtransit.android.ui.fragment.AgencyTypeFragment;
import org.mtransit.android.ui.fragment.FavoritesFragment;
import org.mtransit.android.ui.fragment.HomeFragment;
import org.mtransit.android.ui.fragment.MapFragment;
import org.mtransit.android.ui.fragment.NearbyFragment;
import org.mtransit.android.ui.fragment.NewsFragment;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class MenuAdapter extends MTBaseAdapter implements ListAdapter, DataSourceProvider.ModulesUpdateListener {

	private static final String TAG = MenuAdapter.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final int VIEW_TYPE_STATIC_MAIN_TEXT_WITH_ICON = 0; // Favorite, Nearby, Direction, Search, Map...
	private static final int VIEW_TYPE_DYNAMIC_AGENCY_TYPE = 2; // Bike, Bus, Subway, Train...
	private static final int VIEW_TYPE_SECONDARY = 3; // Settings, Help, Send Feedback...
	private static final int VIEW_TYPE_STATIC_SEPARATORS = 4; // -----

	private static final int VIEW_TYPE_COUNT = 5;

	private static final int ITEM_INDEX_HOME = 0;
	private static final int ITEM_INDEX_FAVORITE = 1;
	private static final int ITEM_INDEX_NEARBY = 2;
	private static final int ITEM_INDEX_MAP = 3;
	private static final int ITEM_INDEX_NEWS = 4;

	private static final int ITEM_INDEX_DYNAMIC_HEADER_SEPARATOR = 5;

	private static final int STATIC_ITEMS_BEFORE_DYNAMIC = ITEM_INDEX_DYNAMIC_HEADER_SEPARATOR + 1;

	private static final int ITEM_INDEX_AFTER_SEPARATOR = 0;
	private static final int ITEM_INDEX_SETTINGS = 1;
	private static final int STATIC_ITEMS_AFTER_DYNAMIC = 2;

	private static final String ITEM_ID_AGENCYTYPE_START_WITH = "agencytype-";
	private static final String ITEM_ID_STATIC_START_WITH = "static-";

	public static final int ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT = ITEM_INDEX_HOME;

	public static final String ITEM_ID_SELECTED_SCREEN_DEFAULT = ITEM_ID_STATIC_START_WITH + ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT;

	private WeakReference<Context> contextWR;

	private MenuUpdateListener listener;

	private LayoutInflater layoutInflater;

	private ArrayList<DataSourceType> allAgencyTypes = null;

	public MenuAdapter(Context context, MenuUpdateListener listener) {
		setContext(context);
		this.listener = listener;
		DataSourceProvider.addModulesUpdateListener(this);
	}

	public void setContext(Context context) {
		if (context == null) {
			return;
		}
		this.contextWR = new WeakReference<Context>(context);
		this.layoutInflater = LayoutInflater.from(context);
	}

	public void init() {
		getAllAgencyTypes(); // load all agency types
	}

	private ArrayList<DataSourceType> getAllAgencyTypes() {
		if (this.allAgencyTypes == null) {
			initAllAgencyTypes();
		}
		return allAgencyTypes;
	}

	private void initAllAgencyTypes() {
		Context context = this.contextWR == null ? null : this.contextWR.get();
		if (context != null) {
			this.allAgencyTypes = filterAgencyTypes(DataSourceProvider.get(context).getAvailableAgencyTypes());
		}
	}

	private ArrayList<DataSourceType> filterAgencyTypes(ArrayList<DataSourceType> availableAgencyTypes) {
		if (availableAgencyTypes != null) {
			Iterator<DataSourceType> it = availableAgencyTypes.iterator();
			while (it.hasNext()) {
				if (!it.next().isMenuList()) {
					it.remove();
				}
			}
		}
		return availableAgencyTypes;
	}

	private int getAllAgencyTypesCount() {
		return getAllAgencyTypes().size();
	}

	public void onDestroy() {
		DataSourceProvider.removeModulesUpdateListener(this);
	}

	private boolean resumed = false;

	public void onPause() {
		this.resumed = false;
	}

	public void onResume() {
		this.resumed = true;
		if (this.modulesUpdated) {
			new Handler().post(new Runnable() {
				@Override
				public void run() {
					if (MenuAdapter.this.modulesUpdated) {
						onModulesUpdated();
					}
				}
			});
		}
	}

	private boolean modulesUpdated = false;

	@Override
	public void onModulesUpdated() {
		this.modulesUpdated = true;
		if (!this.resumed) {
			return;
		}
		Context context = this.contextWR == null ? null : this.contextWR.get();
		ArrayList<DataSourceType> newAllAgencyTypes = filterAgencyTypes(DataSourceProvider.get(context).getAvailableAgencyTypes());
		if (CollectionUtils.getSize(this.allAgencyTypes) != CollectionUtils.getSize(newAllAgencyTypes)) {
			this.allAgencyTypes = newAllAgencyTypes; // force reset
			super.notifyDataSetChanged();
			if (this.listener != null) {
				this.listener.onMenuUpdated();
				this.modulesUpdated = false; // processed
			}
		} else {
			this.modulesUpdated = false; // nothing to do
		}
	}

	@Override
	public int getCountMT() {
		return STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypesCount() + STATIC_ITEMS_AFTER_DYNAMIC;
	}

	@Override
	public Object getItemMT(int position) {
		if (position < STATIC_ITEMS_BEFORE_DYNAMIC) {
			return null;
		}
		if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypesCount()) {
			return getAllAgencyTypes().get(position - STATIC_ITEMS_BEFORE_DYNAMIC);
		} else if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypesCount() + STATIC_ITEMS_AFTER_DYNAMIC) {
			return null;
		} else {
			MTLog.w(this, "No item expected at position '%s'!", position);
			return null;
		}
	}

	@Override
	public long getItemIdMT(int position) {
		if (position < STATIC_ITEMS_BEFORE_DYNAMIC) {
			return position;
		}
		if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypesCount()) {
			return 1000 + getAllAgencyTypes().get(position - STATIC_ITEMS_BEFORE_DYNAMIC).getId();
		} else if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypesCount() + STATIC_ITEMS_AFTER_DYNAMIC) {
			// return ITEM_INDEX_AFTER_START + (position - STATIC_ITEMS_BEFORE_DYNAMIC - getAllAgencyTypes().size());
			// return position - STATIC_ITEMS_BEFORE_DYNAMIC - getAllAgencyTypes().size();
			return position + STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypesCount();
		} else {
			MTLog.w(this, "No item ID expected at position '%s'!", position);
			return -1;
		}
	}

	public String getScreenItemId(int position) {
		switch (getItemViewType(position)) {
		case VIEW_TYPE_STATIC_MAIN_TEXT_WITH_ICON:
			return ITEM_ID_STATIC_START_WITH + position;
		case VIEW_TYPE_DYNAMIC_AGENCY_TYPE:
			return getAgencyTypeScreenItemId(position);
		case VIEW_TYPE_STATIC_SEPARATORS:
		case VIEW_TYPE_SECONDARY:
			return null;
		default:
			MTLog.w(this, "No screen item ID expected at position '%s'!", position);
			return null;
		}
	}

	public String getAgencyTypeScreenItemId(int position) {
		switch (getItemViewType(position)) {
		case VIEW_TYPE_DYNAMIC_AGENCY_TYPE:
			return ITEM_ID_AGENCYTYPE_START_WITH + getAgencyTypeAt(position).getId();
		default:
			MTLog.w(this, "No agency type screen item ID expected at position '%s'!", position);
			return null;
		}
	}

	public int getScreenItemPosition(String itemId) {
		if (TextUtils.isEmpty(itemId)) {
			return ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT;
		}
		if (itemId.startsWith(ITEM_ID_STATIC_START_WITH)) {
			try {
				return Integer.parseInt(itemId.substring(ITEM_ID_STATIC_START_WITH.length()));
			} catch (Exception e) {
				MTLog.w(this, e, "Error while finding static screen item ID '%s' position!", itemId);
				return ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT;
			}
		} else if (itemId.startsWith(ITEM_ID_AGENCYTYPE_START_WITH)) {
			try {
				int typeId = Integer.parseInt(itemId.substring(ITEM_ID_AGENCYTYPE_START_WITH.length()));
				return getDynamicAgencyTypePosition(typeId);
			} catch (Exception e) {
				MTLog.w(this, e, "Error while finding agency type screen item ID '%s' position!", itemId);
				return ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT;
			}
		}
		MTLog.w(this, "Unknown item ID'%s'!", itemId);
		return ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT;
	}

	private int getDynamicAgencyTypePosition(int typeId) {
		int i = 0;
		for (DataSourceType type : getAllAgencyTypes()) {
			if (type.getId() == typeId) {
				return i + STATIC_ITEMS_BEFORE_DYNAMIC;
			}
			i++;
		}
		MTLog.w(this, "Unknown agency type ID '%s'!", typeId);
		return ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT;
	}

	@Override
	public int getViewTypeCount() {
		return VIEW_TYPE_COUNT;
	}

	@Override
	public int getItemViewType(int position) {
		if (position < STATIC_ITEMS_BEFORE_DYNAMIC) {
			if (position == ITEM_INDEX_DYNAMIC_HEADER_SEPARATOR) {
				return VIEW_TYPE_STATIC_SEPARATORS;
			}
			return VIEW_TYPE_STATIC_MAIN_TEXT_WITH_ICON;
		}
		if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypesCount()) {
			return VIEW_TYPE_DYNAMIC_AGENCY_TYPE;
		} else if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypesCount() + STATIC_ITEMS_AFTER_DYNAMIC) {
			if (position - STATIC_ITEMS_BEFORE_DYNAMIC - getAllAgencyTypesCount() == ITEM_INDEX_AFTER_SEPARATOR) {
				return VIEW_TYPE_STATIC_SEPARATORS;
			}
			return VIEW_TYPE_SECONDARY;
		} else {
			MTLog.w(this, "No item view type expected at position '%s'!", position);
			return -1;
		}
	}

	public boolean isRootScreen(int position) {
		switch (getItemViewType(position)) {
		case VIEW_TYPE_STATIC_MAIN_TEXT_WITH_ICON:
		case VIEW_TYPE_DYNAMIC_AGENCY_TYPE:
			return true;
		case VIEW_TYPE_STATIC_SEPARATORS:
		case VIEW_TYPE_SECONDARY:
			return false;
		default:
			MTLog.w(this, "No root view expected at position '%s'!", position);
			return false;
		}
	}

	@Override
	public View getViewMT(int position, View convertView, ViewGroup parent) {
		switch (getItemViewType(position)) {
		case VIEW_TYPE_STATIC_MAIN_TEXT_WITH_ICON:
			return getStaticView(position, convertView, parent);
		case VIEW_TYPE_DYNAMIC_AGENCY_TYPE:
			return getDynamicAgencyTypeView(position, convertView, parent);
		case VIEW_TYPE_STATIC_SEPARATORS:
			return getStaticSeparator(convertView, parent);
		case VIEW_TYPE_SECONDARY:
			return getSecondarView(position, convertView, parent);
		default:
			MTLog.w(this, "No view expected at position '%s'!", position);
			return null; // CRASH
		}
	}

	public DataSourceType getAgencyTypeAt(int position) {
		return getAllAgencyTypes().get(position - STATIC_ITEMS_BEFORE_DYNAMIC);
	}

	private View getDynamicAgencyTypeView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.menu_item_agency_type, parent, false);
			MenuItemAgencyTypeViewHolder holder = new MenuItemAgencyTypeViewHolder();
			holder.nameTv = (TextView) convertView.findViewById(R.id.name);
			convertView.setTag(holder);
		}
		MenuItemAgencyTypeViewHolder holder = (MenuItemAgencyTypeViewHolder) convertView.getTag();
		DataSourceType type = getAgencyTypeAt(position);
		if (type != null) {
			holder.nameTv.setText(type.getAllStringResId());
			if (type.getMenuResId() != -1) {
				holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(type.getMenuResId(), 0, 0, 0);
			} else {
				holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			}
		} else {
			holder.nameTv.setText(null);
			MTLog.w(this, "No agency view view expected at position '%s'!", position);
		}
		return convertView;
	}

	private View getStaticSeparator(View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.menu_separator, parent, false);
		}
		return convertView;
	}


	@Override
	public boolean areAllItemsEnabled() {
		// return false; // to hide divider around disabled items (list view background visible behind hidden divider)
		return true; // to show divider around disabled items
	}

	@Override
	public boolean isEnabled(int position) {
		switch (getItemViewType(position)) {
		case VIEW_TYPE_STATIC_MAIN_TEXT_WITH_ICON:
		case VIEW_TYPE_DYNAMIC_AGENCY_TYPE:
		case VIEW_TYPE_SECONDARY:
			return true;
		case VIEW_TYPE_STATIC_SEPARATORS:
			return false;
		default:
			MTLog.w(this, "No root view expected at position '%s'!", position);
			return false;
		}
	}

	private int getSecondaryIndexItemAt(int position) {
		return position - STATIC_ITEMS_BEFORE_DYNAMIC - getAllAgencyTypes().size();
	}

	public View getSecondarView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.menu_item_secondary, parent, false);
			MenuItemSecondaryViewHolder holder = new MenuItemSecondaryViewHolder();
			holder.nameTv = (TextView) convertView.findViewById(R.id.name);
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			convertView.setTag(holder);
		}
		MenuItemSecondaryViewHolder holder = (MenuItemSecondaryViewHolder) convertView.getTag();
		final int secondaryPosition = position - STATIC_ITEMS_BEFORE_DYNAMIC - getAllAgencyTypes().size();
		if (secondaryPosition == ITEM_INDEX_SETTINGS) {
			holder.nameTv.setText(R.string.settings);
			holder.icon.setImageResource(R.drawable.ic_action_settings);
		} else {
			holder.nameTv.setText(null);
			holder.icon.setImageDrawable(null);
			MTLog.w(this, "No secondary view view expected at position '%s'!", position);
		}
		return convertView;
	}

	private View getStaticView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.menu_item, parent, false);
			MenuItemViewHolder holder = new MenuItemViewHolder();
			holder.nameTv = (TextView) convertView.findViewById(R.id.name);
			convertView.setTag(holder);
		}
		// update UI
		MenuItemViewHolder holder = (MenuItemViewHolder) convertView.getTag();
		if (position == ITEM_INDEX_HOME) {
			holder.nameTv.setText(R.string.home);
			holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_guide_holo_light, 0, 0, 0);
		} else if (position == ITEM_INDEX_FAVORITE) {
			holder.nameTv.setText(R.string.favorites);
			holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_favorites_holo_light, 0, 0, 0);
		} else if (position == ITEM_INDEX_NEARBY) {
			holder.nameTv.setText(R.string.nearby);
			holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_menu_nearby_holo_light, 0, 0, 0);
			// } else if (position == ITEM_INDEX_DIRECTIONS) {
			// holder.nameTv.setText(R.string.directions);
			// holder.icon.setImageResource(R.drawable.ic_menu_directions);
			// } else if (position == ITEM_INDEX_SEARCH) {
			// holder.nameTv.setText(R.string.search);
			// holder.icon.setImageResource(R.drawable.ic_menu_search);
		} else if (position == ITEM_INDEX_MAP) {
			holder.nameTv.setText(R.string.map);
			holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_action_map_holo_light, 0, 0, 0);
		} else if (position == ITEM_INDEX_NEWS) {
			holder.nameTv.setText(R.string.news);
			holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(R.drawable.ic_action_rss_holo_light, 0, 0, 0);
		} else {
			holder.nameTv.setText(null);
			holder.nameTv.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
			MTLog.w(this, "No static view view expected at position '%s'!", position);
		}
		return convertView;
	}

	public static class MenuItemSecondaryViewHolder {
		TextView nameTv;
		ImageView icon;
	}

	public static class MenuItemViewHolder {
		TextView nameTv;
	}

	public static class MenuItemAgencyTypeViewHolder {
		TextView nameTv;
	}

	public void startNewScreen(Activity activity, int position) {
		int secondaryPosition = getSecondaryIndexItemAt(position);
		if (secondaryPosition == ITEM_INDEX_SETTINGS) {
			activity.startActivity(PreferencesActivity.newInstance(activity));
			return;
		}
		MTLog.w(this, "No new screen for item at position '%s'!", position);
	}

	public ABFragment getNewStaticFragmentAt(int position) {
		if (position == ITEM_INDEX_HOME) {
			return HomeFragment.newInstance(null);
		} else if (position == ITEM_INDEX_FAVORITE) {
			return FavoritesFragment.newInstance();
		} else if (position == ITEM_INDEX_NEARBY) {
			return NearbyFragment.newNearbyInstance(null, null);
			// else if (position == ITEM_INDEX_SEARCH) {
			// return SearchFragment.newInstance();
			// } else if (position == MenuAdapter.ITEM_INDEX_DIRECTIONS) {
			// return DirectionsFragment.newInstance();
		} else if (position == MenuAdapter.ITEM_INDEX_MAP) {
			return MapFragment.newInstance(null, null, null);
		} else if (position == MenuAdapter.ITEM_INDEX_NEWS) {
			return NewsFragment.newInstance(null, null, null, null, null);
		}
		DataSourceType type = getAgencyTypeAt(position);
		if (type != null) {
			return AgencyTypeFragment.newInstance(type.getId(), type);
		}
		MTLog.w(this, "No fragment for item at position '%s'!", position);
		return null;
	}

	public static interface MenuUpdateListener {
		public void onMenuUpdated();
	}
}
