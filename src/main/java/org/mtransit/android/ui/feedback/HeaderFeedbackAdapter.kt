package org.mtransit.android.ui.feedback

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.databinding.LayoutFeedbackItemHeaderBinding

class HeaderFeedbackAdapter(
    private val onClick: (View) -> Unit,
) : RecyclerView.Adapter<HeaderFeedbackAdapter.HeaderFeedbackViewHolder>() {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HeaderFeedbackViewHolder {
        return HeaderFeedbackViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: HeaderFeedbackViewHolder, position: Int) {
        holder.bind(onClick)
    }

    override fun getItemCount() = 1

    class HeaderFeedbackViewHolder private constructor(
        private val binding: LayoutFeedbackItemHeaderBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        companion object {
            fun from(parent: ViewGroup): HeaderFeedbackViewHolder {
                val binding = LayoutFeedbackItemHeaderBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return HeaderFeedbackViewHolder(binding)
            }
        }

        fun bind(onClick: (View) -> Unit) {
            binding.root.setOnClickListener {
                onClick(it)
            }
        }
    }
}