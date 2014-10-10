package org.mtransit.android.data;

import java.util.List;

import org.mtransit.android.R;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.ui.widget.MTBaseAdapter;

import android.content.Context;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ListAdapter;
import android.widget.TextView;

public class MenuAdapter extends MTBaseAdapter implements ListAdapter {

	private static final String TAG = MenuAdapter.class.getSimpleName();

	@Override
	public String getLogTag() {
		return TAG;
	}

	private static final int VIEW_TYPE_STATIC_MAIN_TEXT_WITH_ICON = 0; // Favorite, Nearby, Direction, Search, Maps...
	private static final int VIEW_TYPE_STATIC_HEADERS = 1; // Browse...
	private static final int VIEW_TYPE_DYNAMIC_AGENCY_TYPE = 2; // Bike, Bus, Subway, Train...
	private static final int VIEW_TYPE_SECONDARY = 3; // Settings, Help, Send Feedback...

	private static final int VIEW_TYPE_COUNT = 4;

	public static final int ITEM_INDEX_SEARCH = 0;
	public static final int ITEM_INDEX_FAVORITE = 1;
	public static final int ITEM_INDEX_NEARBY = 2;
	public static final int ITEM_INDEX_DIRECTIONS = 3;
	public static final int ITEM_INDEX_MAPS = 4;

	private static final int ITEM_INDEX_DYNAMIC_HEADER = 5;

	private static final int STATIC_ITEMS_BEFORE_DYNAMIC = 6;

	private static final int ITEM_INDEX_AFTER_START = 0; // 100;
	private static final int ITEM_INDEX_SETTINGS = ITEM_INDEX_AFTER_START + 0;
	private static final int ITEM_INDEX_HELP = ITEM_INDEX_AFTER_START + 1;
	public static final int ITEM_INDEX_SEND_FEEDBACK = ITEM_INDEX_AFTER_START + 2;

	private static final int STATIC_ITEMS_AFTER_DYNAMIC = 3;

	private static final String ITEM_ID_AGENCYTYPE_START_WITH = "agencytype-";
	private static final String ITEM_ID_STATIC_START_WITH = "static-";

	private static final int ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT = ITEM_INDEX_NEARBY;

	public static final String ITEM_ID_SELECTED_SCREEN_DEFAULT = ITEM_ID_STATIC_START_WITH + ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT;

	private Context context;

	private LayoutInflater layoutInflater;

	private List<DataSourceType> allAgencyTypes = null;

	public MenuAdapter(Context context) {
		this.context = context;
		this.layoutInflater = (LayoutInflater) this.context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	}

	public List<DataSourceType> getAllAgencyTypes() {
		if (this.allAgencyTypes == null) {
			this.allAgencyTypes = DataSourceProvider.get().getAvailableAgencyTypes(this.context);
		}
		return allAgencyTypes;
	}

	@Override
	public int getCountMT() {
		return STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypes().size() + STATIC_ITEMS_AFTER_DYNAMIC;
	}

	@Override
	public Object getItemMT(int position) {
		if (position < STATIC_ITEMS_BEFORE_DYNAMIC) {
			return null;
		} else if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypes().size()) {
			return getAllAgencyTypes().get(position - STATIC_ITEMS_BEFORE_DYNAMIC);
		} else if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypes().size() + STATIC_ITEMS_AFTER_DYNAMIC) {
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
		} else if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypes().size()) {
			return 1000 + getAllAgencyTypes().get(position - STATIC_ITEMS_BEFORE_DYNAMIC).getId();
		} else if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypes().size() + STATIC_ITEMS_AFTER_DYNAMIC) {
			return ITEM_INDEX_AFTER_START + (position - STATIC_ITEMS_BEFORE_DYNAMIC - getAllAgencyTypes().size());
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
			return ITEM_ID_AGENCYTYPE_START_WITH + getAgencyTypeAt(position).getId();
		case VIEW_TYPE_STATIC_HEADERS:
		case VIEW_TYPE_SECONDARY:
			return null;
		default:
			MTLog.w(this, "No screen item ID expected at position '%s'!", position);
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
				return Integer.parseInt(itemId.substring(ITEM_ID_AGENCYTYPE_START_WITH.length())) + STATIC_ITEMS_BEFORE_DYNAMIC;
			} catch (Exception e) {
				MTLog.w(this, e, "Error while finding agency type screen item ID '%s' position!", itemId);
				return ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT;
			}
		}
		return ITEM_ID_SELECTED_SCREEN_POSITION_DEFAULT;
	}

	@Override
	public int getViewTypeCount() {
		return VIEW_TYPE_COUNT;
	}

	@Override
	public int getItemViewType(int position) {
		if (position < STATIC_ITEMS_BEFORE_DYNAMIC) {
			if (position == ITEM_INDEX_DYNAMIC_HEADER) {
				return VIEW_TYPE_STATIC_HEADERS;
			}
			return VIEW_TYPE_STATIC_MAIN_TEXT_WITH_ICON;
		} else if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypes().size()) {
			return VIEW_TYPE_DYNAMIC_AGENCY_TYPE;
		} else if (position < STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypes().size() + STATIC_ITEMS_AFTER_DYNAMIC) {
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
		case VIEW_TYPE_STATIC_HEADERS:
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
		case VIEW_TYPE_STATIC_HEADERS:
			return getStaticHeaderView(position, convertView, parent);
		case VIEW_TYPE_DYNAMIC_AGENCY_TYPE:
			return getDynamicAgencyTypeView(position, convertView, parent);
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

	public View getDynamicAgencyTypeView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.menu_item_agency_type, parent, false);
			MenuItemAgencyTypeViewHolder holder = new MenuItemAgencyTypeViewHolder();
			holder.nameTv = (TextView) convertView.findViewById(R.id.name);
			convertView.setTag(holder);
		}
		MenuItemAgencyTypeViewHolder holder = (MenuItemAgencyTypeViewHolder) convertView.getTag();
		final DataSourceType type = getAllAgencyTypes().get(position - STATIC_ITEMS_BEFORE_DYNAMIC);
		if (type != null) {
			holder.nameTv.setText(type.getShortNameResId());
		} else {
			holder.nameTv.setText(null);
			MTLog.w(this, "No agency view view expected at position '%s'!", position);
		}
		return convertView;
	}

	public View getStaticHeaderView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.menu_list_header, parent, false);
			MenuListHeaderViewHolder holder = new MenuListHeaderViewHolder();
			holder.nameTv = (TextView) convertView;
			convertView.setTag(holder);
		}
		MenuListHeaderViewHolder holder = (MenuListHeaderViewHolder) convertView.getTag();
		holder.nameTv.setText(R.string.browse);
		return convertView;
	}

	@Override
	public boolean areAllItemsEnabled() {
		return false;
	}

	@Override
	public boolean isEnabled(int position) {
		if (position == ITEM_INDEX_DYNAMIC_HEADER) {
			return false;
		}
		return true;
	}

	public int getSecondaryIndexItemAt(int position) {
		final int secondaryPosition = position - (STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypes().size()) + ITEM_INDEX_AFTER_START;
		return secondaryPosition;
	}

	public View getSecondarView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.menu_item_secondary, parent, false);
			MenuItemViewHolder holder = new MenuItemViewHolder();
			holder.nameTv = (TextView) convertView.findViewById(R.id.name);
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			convertView.setTag(holder);
		}
		MenuItemViewHolder holder = (MenuItemViewHolder) convertView.getTag();
		int secondaryPosition = position - (STATIC_ITEMS_BEFORE_DYNAMIC + getAllAgencyTypes().size()) + ITEM_INDEX_AFTER_START;
		if (secondaryPosition == ITEM_INDEX_SETTINGS) {
			holder.nameTv.setText(R.string.settings);
			holder.icon.setImageResource(R.drawable.ic_menu_settings);
		} else if (secondaryPosition == ITEM_INDEX_HELP) {
			holder.nameTv.setText(R.string.help);
			holder.icon.setImageResource(R.drawable.ic_menu_help);
		} else if (secondaryPosition == ITEM_INDEX_SEND_FEEDBACK) {
			holder.nameTv.setText(R.string.send_feedback);
			holder.icon.setImageResource(R.drawable.ic_menu_feedback);
		} else {
			holder.nameTv.setText(null);
			holder.icon.setImageDrawable(null);
			MTLog.w(this, "No secondary view view expected at position '%s'!", position);
		}
		return convertView;
	}

	public View getStaticView(int position, View convertView, ViewGroup parent) {
		if (convertView == null) {
			convertView = this.layoutInflater.inflate(R.layout.menu_item, parent, false);
			MenuItemViewHolder holder = new MenuItemViewHolder();
			holder.nameTv = (TextView) convertView.findViewById(R.id.name);
			holder.icon = (ImageView) convertView.findViewById(R.id.icon);
			convertView.setTag(holder);
		}
		// update UI
		MenuItemViewHolder holder = (MenuItemViewHolder) convertView.getTag();
		if (position == ITEM_INDEX_FAVORITE) {
			holder.nameTv.setText("Favorites");
			holder.icon.setImageResource(R.drawable.ic_menu_favorites);
		} else if (position == ITEM_INDEX_NEARBY) {
			holder.nameTv.setText(R.string.nearby);
			holder.icon.setImageResource(R.drawable.ic_menu_nearby);
		} else if (position == ITEM_INDEX_DIRECTIONS) {
			holder.nameTv.setText(R.string.directions);
			holder.icon.setImageResource(R.drawable.ic_menu_directions);
		} else if (position == ITEM_INDEX_SEARCH) {
			holder.nameTv.setText(R.string.search);
			holder.icon.setImageResource(R.drawable.ic_menu_search);
		} else if (position == ITEM_INDEX_MAPS) {
			holder.nameTv.setText(R.string.maps);
			holder.icon.setImageResource(R.drawable.ic_menu_maps);
		} else {
			holder.nameTv.setText(null);
			holder.icon.setImageDrawable(null);
			MTLog.w(this, "No static view view expected at position '%s'!", position);
		}
		return convertView;
	}

	public static class MenuItemViewHolder {
		TextView nameTv;
		ImageView icon;
	}

	public static class MenuItemAgencyTypeViewHolder {
		TextView nameTv;
	}

	public static class MenuListHeaderViewHolder {
		TextView nameTv;
	}

}
