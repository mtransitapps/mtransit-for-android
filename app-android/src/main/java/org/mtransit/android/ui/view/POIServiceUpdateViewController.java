package org.mtransit.android.ui.view;

import android.content.Context;
import android.view.View;
import android.view.ViewStub;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.viewbinding.ViewBinding;

import org.mtransit.android.R;
import org.mtransit.android.commons.ColorUtils;
import org.mtransit.android.commons.HtmlUtils;
import org.mtransit.android.commons.MTLog;
import org.mtransit.android.commons.data.ServiceUpdate;
import org.mtransit.android.data.POIManager;
import org.mtransit.android.databinding.LayoutPoiServiceUpdateBinding;
import org.mtransit.android.ui.common.UISourceLabelUtils;
import org.mtransit.android.util.LinkUtils;
import org.mtransit.commons.CollectionUtils;

import java.util.List;

@SuppressWarnings({"unused", "WeakerAccess"})
public class POIServiceUpdateViewController implements MTLog.Loggable {

	private static final String LOG_TAG = POIServiceUpdateViewController.class.getSimpleName();

	@NonNull
	@Override
	public String getLogTag() {
		return LOG_TAG;
	}

	@NonNull
	public static ViewBinding getLayoutViewBinding(@NonNull ViewStub viewStub) {
		viewStub.setLayoutResource(getLayoutResId());
		return LayoutPoiServiceUpdateBinding.bind(viewStub.inflate());
	}

	@LayoutRes
	public static int getLayoutResId() {
		return R.layout.layout_poi_service_update;
	}

	public static void initViewHolder(@NonNull View view) {
		ServiceUpdatesListViewHolder serviceUpdatesListViewHolder = new ServiceUpdatesListViewHolder();
		serviceUpdatesListViewHolder.layout = view;
		serviceUpdatesListViewHolder.messagesTv = view.findViewById(R.id.service_update_text);
		serviceUpdatesListViewHolder.sourceLabelTv = view.findViewById(R.id.source_label);
		view.setTag(serviceUpdatesListViewHolder);
	}

	public static void updateView(@Nullable View view,
								  @Nullable List<ServiceUpdate> serviceUpdates,
								  @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updateView() > SKIP (no view)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof ServiceUpdatesListViewHolder)) {
			initViewHolder(view);
		}
		ServiceUpdatesListViewHolder holder = (ServiceUpdatesListViewHolder) view.getTag();
		updateView(view.getContext(), holder, serviceUpdates, dataProvider);
	}

	public static void updateView(@Nullable View view,
								  @NonNull POIManager poim,
								  @NonNull POIDataProvider dataProvider) {
		if (view == null) {
			MTLog.d(LOG_TAG, "updateView() > SKIP (no view)");
			return;
		}
		if (view.getTag() == null || !(view.getTag() instanceof ServiceUpdatesListViewHolder)) {
			initViewHolder(view);
		}
		ServiceUpdatesListViewHolder holder = (ServiceUpdatesListViewHolder) view.getTag();
		updateView(view.getContext(), holder, poim, dataProvider);
	}

	private static void updateView(@NonNull Context context,
								   @Nullable ServiceUpdatesListViewHolder serviceUpdatesListViewHolder,
								   @Nullable List<ServiceUpdate> serviceUpdates,
								   @NonNull POIDataProvider dataProvider) {
		if (!dataProvider.isShowingStatus() || serviceUpdatesListViewHolder == null) {
			if (serviceUpdatesListViewHolder != null) {
				serviceUpdatesListViewHolder.layout.setVisibility(View.GONE);
			}
			return;
		}
		updateServiceUpdatesView2(context, serviceUpdatesListViewHolder, serviceUpdates, dataProvider);
	}

	private static void updateView(@NonNull Context context,
								   @Nullable ServiceUpdatesListViewHolder serviceUpdatesListViewHolder,
								   @NonNull POIManager poim,
								   @NonNull POIDataProvider dataProvider) {
		if (!dataProvider.isShowingStatus() || serviceUpdatesListViewHolder == null) {
			if (serviceUpdatesListViewHolder != null) {
				serviceUpdatesListViewHolder.layout.setVisibility(View.GONE);
			}
			return;
		}
		updateServiceUpdatesView(context, serviceUpdatesListViewHolder, poim, dataProvider);
	}

	public static void updateServiceUpdate(@Nullable View view,
										   @Nullable List<ServiceUpdate> serviceUpdates,
										   @NonNull POIDataProvider dataProvider) {
		if (view == null || view.getTag() == null || !(view.getTag() instanceof ServiceUpdatesListViewHolder)) {
			MTLog.d(LOG_TAG, "updateView() > SKIP (no view holder)");
			return;
		}
		ServiceUpdatesListViewHolder holder = (ServiceUpdatesListViewHolder) view.getTag();
		updateServiceUpdatesView(view.getContext(), holder, serviceUpdates, dataProvider);
	}

	private static void updateServiceUpdatesView2(@NonNull Context context,
												  @Nullable ServiceUpdatesListViewHolder serviceUpdatesListViewHolder,
												  @Nullable List<ServiceUpdate> serviceUpdates,
												  @NonNull POIDataProvider dataProvider) {
		if (serviceUpdatesListViewHolder != null) {
			if (dataProvider.isShowingServiceUpdates()) {
				updateServiceUpdatesView(
						context,
						serviceUpdatesListViewHolder,
						serviceUpdates,
						dataProvider
				);
			} else {
				serviceUpdatesListViewHolder.layout.setVisibility(View.GONE);
			}
		}
	}

	private static void updateServiceUpdatesView(@NonNull Context context,
												 @Nullable ServiceUpdatesListViewHolder serviceUpdatesListViewHolder,
												 @NonNull POIManager poim,
												 @NonNull POIDataProvider dataProvider) {
		if (serviceUpdatesListViewHolder != null) {
			if (dataProvider.isShowingServiceUpdates()) {
				poim.setServiceUpdateLoaderListener(dataProvider);
				updateServiceUpdatesView(
						context,
						serviceUpdatesListViewHolder,
						poim.getServiceUpdates(dataProvider.providesServiceUpdateLoader()),
						dataProvider
				);
			} else {
				serviceUpdatesListViewHolder.layout.setVisibility(View.GONE);
			}
		}
	}

	private static void updateServiceUpdatesView(@NonNull Context context,
												 @NonNull ServiceUpdatesListViewHolder serviceUpdatesListViewHolder,
												 @Nullable List<ServiceUpdate> serviceUpdates,
												 @NonNull POIDataProvider dataProvider) {
		int serviceMessageDisplayed = 0;
		boolean isWarning = false;
		if (CollectionUtils.getSize(serviceUpdates) != 0) {
			StringBuilder ssb = new StringBuilder();
			if (serviceUpdates != null) {
				for (ServiceUpdate serviceUpdate : serviceUpdates) {
					if (!serviceUpdate.shouldDisplay()) {
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
			serviceUpdatesListViewHolder.messagesTv.setText(LinkUtils.linkifyHtml(HtmlUtils.fromHtml(ssb.toString()), false), TextView.BufferType.SPANNABLE);
			serviceUpdatesListViewHolder.messagesTv.setMovementMethod(LinkUtils.LinkMovementMethodInterceptor.getInstance(dataProvider));
			serviceUpdatesListViewHolder.messagesTv.setBackgroundResource(
					isWarning ? R.drawable.service_update_warning
							: R.drawable.service_update_info
			);
		}
		if (serviceMessageDisplayed == 0) {
			serviceUpdatesListViewHolder.layout.setVisibility(View.GONE);
		} else {
			serviceUpdatesListViewHolder.layout.setVisibility(View.VISIBLE);
		}
		UISourceLabelUtils.setSourceLabelTextView(serviceUpdatesListViewHolder.sourceLabelTv, serviceUpdates);
	}

	private static class ServiceUpdatesListViewHolder {
		View layout;
		TextView messagesTv;
		TextView sourceLabelTv;
	}
}
