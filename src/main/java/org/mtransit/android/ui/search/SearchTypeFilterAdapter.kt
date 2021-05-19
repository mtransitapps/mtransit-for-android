package org.mtransit.android.ui.search

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.LayoutRes
import org.mtransit.android.R
import org.mtransit.android.commons.ui.widget.MTArrayAdapter
import org.mtransit.android.data.DataSourceType
import org.mtransit.android.databinding.LayoutPoiTypeItemBinding

class SearchTypeFilterAdapter @JvmOverloads constructor(
    context: Context,
    @LayoutRes resource: Int = -1
) : MTArrayAdapter<DataSourceType?>(context, resource) {

    companion object {
        private val LOG_TAG = SearchTypeFilterAdapter::class.java.simpleName
    }

    override fun getLogTag(): String = LOG_TAG

    private fun clearSilently() {
        setNotifyOnChange(false)
        clear() // skip 1st notifyDataSetChanged()
        setNotifyOnChange(true)
    }

    fun setData(typeFilters: List<DataSourceType>) {
        clearSilently()
        add(null) // ALL
        addAll(typeFilters)
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getTheView(position, convertView, parent)
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        return getTheView(position, convertView, parent)
    }

    private fun getTheView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view: View
        val holder: TypeViewHolder
        if (convertView == null) {
            holder = TypeViewHolder.from(parent)
            view = holder.binding.root
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as TypeViewHolder
        }
        holder.bind(getItem(position))
        return view
    }

    class TypeViewHolder private constructor(
        val binding: LayoutPoiTypeItemBinding
    ) {

        companion object {
            fun from(parent: ViewGroup): TypeViewHolder {
                val binding = LayoutPoiTypeItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return TypeViewHolder(binding)
            }
        }

        fun bind(dst: DataSourceType?) {
            if (dst != null) {
                binding.name.setText(dst.poiShortNameResId)
            } else {
                binding.name.setText(R.string.all) // ALL
            }
            if (dst != null && dst.iconResId != -1) {
                binding.name.setCompoundDrawablesWithIntrinsicBounds(dst.iconResId, 0, 0, 0)
            } else { // ALL
                binding.name.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0)
            }
        }
    }
}