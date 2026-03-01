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
import org.mtransit.android.ui.view.common.setDrawables

class SearchTypeFilterAdapter @JvmOverloads constructor(
    context: Context,
    @LayoutRes resource: Int = -1
) : MTArrayAdapter<DataSourceType?>(context, resource) {

    companion object {
        private val LOG_TAG: String = SearchTypeFilterAdapter::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

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
            binding.name.text = dst?.getPoiShortName(binding.root.context)
                ?: binding.root.context?.getText(R.string.all) // ALL
            binding.name.setDrawables(
                start = dst?.iconResId.takeIf { it != -1 },
                relative = false,
                withIntrinsicBounds = true,
            )
        }
    }
}