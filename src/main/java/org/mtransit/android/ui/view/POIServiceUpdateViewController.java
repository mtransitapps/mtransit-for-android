package org.mtransit.android.ui.view;

import android.content.Context;
import android.text.TextUtils;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.mtransit.android.R;
import org.mtransit.android.commons.CollectionUtils;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.util.LinkUtils;

import java.util.ArrayList;

public class POIServiceUpdateViewController implements MTLog.Loggable {

	private static final String TAG = POIServiceUpdateViewController.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return TAG;
	}

	@LayoutRes
	public static int getLayoutResId() {
		return R.layout.layout_poi_service_update;
	}

	public static void initViewHolder(POIManager poim, @NonNull View view) {
		ServiceUpdatesListViewHolder serviceUpdatesListViewHolder = new ServiceUpdatesListViewHolder();
		serviceUpdatesListViewHolder.layout = view;
		serviceUpdatesListViewHolder.messagesTv = view.findViewById(R.id.service_udapte_text);
		view.setTag(serviceUpdatesListViewHolder);
	}

	public static void updateView(@NonNull Context context, View view, POIManager poim, POIViewController.POIDataProvider dataProvider) {
		if (view == null) {
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof ServiceUpdatesListViewHolder)) {
			initViewHolder(poim, view);
		}
		ServiceUpdatesListViewHolder holder = (ServiceUpdatesListViewHolder) view.getTag();
		updateView(context, holder, poim, dataProvider);
	}

	private static void updateView(@NonNull Context context, ServiceUpdatesListViewHolder serviceUpdatesListViewHolder, POIManager poim,
								   POIViewController.POIDataProvider dataProvider) {
		if (dataProvider == null || !dataProvider.isShowingStatus() || poim == null || serviceUpdatesListViewHolder == null) {
			if (serviceUpdatesListViewHolder != null) {
				serviceUpdatesListViewHolder.layout.setVisibility(View.GONE);
			}
			return;
		}
		updateServiceUpdatesView(context, serviceUpdatesListViewHolder, poim, dataProvider);
	}

	public static void updateServiceUpdate(@NonNull Context context, View view, ArrayList<ServiceUpdate> serviceUpdates,
										   POIViewController.POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof ServiceUpdatesListViewHolder)) {
			return;
		}
		ServiceUpdatesListViewHolder holder = (ServiceUpdatesListViewHolder) view.getTag();
		updateServiceUpdatesView(context, holder, serviceUpdates, dataProvider);
	}

	private static void updateServiceUpdatesView(@NonNull Context context, ServiceUpdatesListViewHolder serviceUpdatesListViewHolder, POIManager poim,
												 POIViewController.POIDataProvider dataProvider) {
		if (serviceUpdatesListViewHolder != null) {
			if (dataProvider != null && dataProvider.isShowingServiceUpdates() && poim != null) {
				poim.setServiceUpdateLoaderListener(dataProvider);
				updateServiceUpdatesView(context, serviceUpdatesListViewHolder, poim.getServiceUpdates(context), dataProvider);
			} else {
				serviceUpdatesListViewHolder.layout.setVisibility(View.GONE);
			}
		}
	}

	private static void updateServiceUpdatesView(@NonNull Context context,
												 ServiceUpdatesListViewHolder serviceUpdatesListViewHolder,
												 @Nullable ArrayList<ServiceUpdate> serviceUpdates,
												 POIViewController.POIDataProvider dataProvider) {
		int serviceMessageDisplayed = 0;
		boolean isWarning = false;
		if (dataProvider != null && CollectionUtils.getSize(serviceUpdates) != 0) {
			StringBuilder ssb = new StringBuilder();
			if (serviceUpdates != null) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					if (serviceUpdate.getSeverity() == ServiceUpdate.SEVERITY_NONE) {
						continue;
					}
					if (TextUtils.isEmpty(serviceUpdate.getText()) && TextUtils.isEmpty(serviceUpdate.getTextHTML())) {
						continue;
					}
					if (ssb.length() > 0) {
						ssb.append(HtmlUtils.BR).append(HtmlUtils.BR);
					}
					String thisMsgFromHtml = serviceUpdate.getTextHTML();
					if (serviceUpdate.isSeverityWarning()) {
						thisMsgFromHtml = HtmlUtils.applyFontColor(thisMsgFromHtml, ColorUtils.toRGBColor(ColorUtils.getTextColorSecondary(context)));
					} else {
						thisMsgFromHtml = HtmlUtils.applyFontColor(thisMsgFromHtml, ColorUtils.toRGBColor(ColorUtils.getTextColorTertiary(context)));
					}
					ssb.append(thisMsgFromHtml);
					if (!isWarning && serviceUpdate.isSeverityWarning()) {
						isWarning = true;
					}
					serviceMessageDisplayed++;
				}
			}
			serviceUpdatesListViewHolder.messagesTv.setText(LinkUtils.linkifyHtml(ssb.toString(), true), TextView.BufferType.SPANNABLE);
			serviceUpdatesListViewHolder.messagesTv.setMovementMethod(LinkUtils.LinkMovementMethodInterceptor.getInstance(dataProvider));
		}
		if (serviceMessageDisplayed == 0) {
			serviceUpdatesListViewHolder.layout.setVisibility(View.GONE);
		} else {
			serviceUpdatesListViewHolder.layout.setVisibility(View.VISIBLE);
		}
	}

	private static class ServiceUpdatesListViewHolder {
		View layout;
		TextView messagesTv;
	}
}
