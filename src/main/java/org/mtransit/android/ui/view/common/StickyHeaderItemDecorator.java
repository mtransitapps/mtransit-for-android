package org.mtransit.android.ui.view.common;

import android.graphics.Canvas;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class StickyHeaderItemDecorator<VH extends RecyclerView.ViewHolder> extends RecyclerView.ItemDecoration {

	@NonNull
	private final StickyAdapter<VH> adapter;
	private int currentStickyPosition = RecyclerView.NO_POSITION;
	@NonNull
	private final RecyclerView recyclerView;
	@NonNull
	private final VH currentStickyHolder;
	private View lastViewOverlappedByHeader = null;

	public StickyHeaderItemDecorator(@NonNull StickyAdapter<VH> adapter, @NonNull RecyclerView recyclerView) {
		this.adapter = adapter;
		this.recyclerView = recyclerView;
		currentStickyHolder = adapter.onCreateHeaderViewHolder(recyclerView);
		fixLayoutSize();
		setupCallbacks();

	}

	private void setupCallbacks() {
		recyclerView.addItemDecoration(this);
	}

	@Override
	public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
		super.onDrawOver(c, parent, state);

		RecyclerView.LayoutManager layoutManager = parent.getLayoutManager();
		if (layoutManager == null) {
			return;
		}

		int topChildPosition = RecyclerView.NO_POSITION;
		if (layoutManager instanceof LinearLayoutManager) {
			topChildPosition = ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition();
		} else {
			View topChild = parent.getChildAt(0);
			if (topChild != null) {
				topChildPosition = parent.getChildAdapterPosition(topChild);
			}
		}

		if (topChildPosition == RecyclerView.NO_POSITION) {
			return;
		}

		View viewOverlappedByHeader = getChildInContact(parent, currentStickyHolder.itemView.getBottom());
		if (viewOverlappedByHeader == null) {
			if (lastViewOverlappedByHeader != null) {
				viewOverlappedByHeader = lastViewOverlappedByHeader;
			} else {
				viewOverlappedByHeader = parent.getChildAt(topChildPosition);
			}
		}
		lastViewOverlappedByHeader = viewOverlappedByHeader;

		int overlappedByHeaderPosition = parent.getChildAdapterPosition(viewOverlappedByHeader);
		int overlappedHeaderPosition;
		int preOverlappedPosition;
		if (overlappedByHeaderPosition > 0) {
			preOverlappedPosition = adapter.getHeaderPositionForItem(overlappedByHeaderPosition - 1);
			overlappedHeaderPosition = adapter.getHeaderPositionForItem(overlappedByHeaderPosition);
		} else {
			preOverlappedPosition = adapter.getHeaderPositionForItem(topChildPosition);
			overlappedHeaderPosition = preOverlappedPosition;
		}

		if (preOverlappedPosition == RecyclerView.NO_POSITION) {
			return;
		}

		if (preOverlappedPosition != overlappedHeaderPosition && shouldMoveHeader(viewOverlappedByHeader)) {
			updateStickyHeader(topChildPosition);
			moveHeader(c, viewOverlappedByHeader);
		} else {
			updateStickyHeader(topChildPosition);
			drawHeader(c);
		}
	}

	private boolean shouldMoveHeader(View viewOverlappedByHeader) {
		int dy = (viewOverlappedByHeader.getTop() - viewOverlappedByHeader.getHeight());
		return (viewOverlappedByHeader.getTop() >= 0 && dy <= 0);
	}

	private void updateStickyHeader(int topChildPosition) {
		int headerPositionForItem = adapter.getHeaderPositionForItem(topChildPosition);
		if (headerPositionForItem != currentStickyPosition && headerPositionForItem != RecyclerView.NO_POSITION) {
			adapter.onBindHeaderViewHolder(currentStickyHolder, headerPositionForItem);
			currentStickyPosition = headerPositionForItem;
		} else if (headerPositionForItem != RecyclerView.NO_POSITION) {
			adapter.onBindHeaderViewHolder(currentStickyHolder, headerPositionForItem);
		}
	}

	private void drawHeader(Canvas c) {
		c.save();
		c.translate(0, 0);
		currentStickyHolder.itemView.draw(c);
		c.restore();
	}

	private void moveHeader(Canvas c, View nextHeader) {
		c.save();
		c.translate(0, nextHeader.getTop() - nextHeader.getHeight());
		currentStickyHolder.itemView.draw(c);
		c.restore();
	}

	private View getChildInContact(RecyclerView parent, int contactPoint) {
		View childInContact = null;
		for (int i = 0; i < parent.getChildCount(); i++) {
			View child = parent.getChildAt(i);
			if (child.getBottom() > contactPoint) {
				if (child.getTop() <= contactPoint) {
					childInContact = child;
					break;
				}
			}
		}
		return childInContact;
	}

	private void fixLayoutSize() {
		recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
			@Override
			public void onGlobalLayout() {
				recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
				int widthSpec = View.MeasureSpec.makeMeasureSpec(recyclerView.getWidth(), View.MeasureSpec.EXACTLY);
				int heightSpec = View.MeasureSpec.makeMeasureSpec(recyclerView.getHeight(), View.MeasureSpec.UNSPECIFIED);

				int childWidthSpec = ViewGroup.getChildMeasureSpec(
						widthSpec,
						recyclerView.getPaddingLeft() + recyclerView.getPaddingRight(),
						currentStickyHolder.itemView.getLayoutParams().width);
				int childHeightSpec = ViewGroup.getChildMeasureSpec(
						heightSpec,
						recyclerView.getPaddingTop() + recyclerView.getPaddingBottom(),
						currentStickyHolder.itemView.getLayoutParams().height);

				currentStickyHolder.itemView.measure(childWidthSpec, childHeightSpec);

				currentStickyHolder.itemView.layout(0, 0,
						currentStickyHolder.itemView.getMeasuredWidth(),
						currentStickyHolder.itemView.getMeasuredHeight());
			}
		});
	}

	public interface StickyAdapter<VH extends RecyclerView.ViewHolder> {

		int getHeaderPositionForItem(int itemPosition);

		void onBindHeaderViewHolder(VH holder, int headerPosition);

		VH onCreateHeaderViewHolder(@NonNull ViewGroup parent);
	}
}