package org.mtransit.android.ui.view.listfooter

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.view.updateLayoutParams
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.data.POIListFooterManager
import org.mtransit.android.databinding.LayoutPoiListFooterBinding
import org.mtransit.android.ui.view.common.setOnClickListenerClickable

class FooterViewHolder private constructor(
    private val binding: LayoutPoiListFooterBinding,
) : RecyclerView.ViewHolder(binding.root) {

    companion object {

        @JvmStatic
        fun from(parent: ViewGroup): FooterViewHolder {
            val binding = LayoutPoiListFooterBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false
            )
            return FooterViewHolder(binding)
        }

        @JvmStatic
        fun LayoutPoiListFooterBinding.bind(footerManager: POIListFooterManager?) {
            if (footerManager == null) {
                root.isVisible = false
                return
            }
            if (footerManager.isShowLoading) {
                footerTextTv.isVisible = false
                progressBar.isVisible = true
                root.isVisible = true
            } else if (footerManager.isShowText) {
                progressBar.isVisible = false
                footerTextTv.apply {
                    text = footerManager.text
                    setCompoundDrawablesRelativeWithIntrinsicBounds(footerManager.textStartDrawableRes ?: 0, 0, 0, 0)
                    updateLayoutParams<ViewGroup.MarginLayoutParams> {
                        marginStart = footerManager.textHorizontalMargin
                        marginEnd = footerManager.textHorizontalMargin
                    }
                    isVisible = true
                }
                root.setOnClickListenerClickable(footerManager.onTextClickListener)
                root.isVisible = true
            } else {
                footerTextTv.isVisible = false
                progressBar.isVisible = false
                root.isVisible = false
            }
        }
    }

    fun bind(footerManager: POIListFooterManager?) = binding.apply { bind(footerManager) }
}
