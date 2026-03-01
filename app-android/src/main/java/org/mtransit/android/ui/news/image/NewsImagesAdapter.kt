package org.mtransit.android.ui.news.image

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import org.mtransit.android.commons.MTLog
import org.mtransit.android.data.NewsImage
import org.mtransit.android.databinding.LayoutNewsImageItemBinding
import org.mtransit.android.ui.view.common.ImageManager

class NewsImagesAdapter(
    private val imageManager: ImageManager,
    private val onClick: (View, NewsImage) -> Unit,
    private val horizontal: Boolean = false,
) : ListAdapter<NewsImage, NewsImagesAdapter.NewsImageItemViewHolder>(NewsImageDiffCallback), MTLog.Loggable {

    companion object {
        private val LOG_TAG: String = NewsImagesAdapter::class.java.simpleName
    }

    override fun getLogTag() = LOG_TAG

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): NewsImageItemViewHolder {
        return NewsImageItemViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: NewsImageItemViewHolder, position: Int) {
        val newsImage = getItem(position)
        holder.bind(imageManager, position, itemCount, newsImage, horizontal, onClick)
    }

    class NewsImageItemViewHolder private constructor(
        private val binding: LayoutNewsImageItemBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        companion object {
            fun from(parent: ViewGroup): NewsImageItemViewHolder {
                val binding = LayoutNewsImageItemBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )
                return NewsImageItemViewHolder(binding)
            }
        }

        fun bind(
            imageManager: ImageManager,
            @Suppress("unused") position: Int,
            @Suppress("unused") itemCount: Int,
            newsImage: NewsImage,
            @Suppress("unused") horizontal: Boolean,
            onClick: (View, NewsImage) -> Unit,
        ) {
            val context = binding.root.context
            binding.apply {
                root.apply {
                    setOnClickListener { view ->
                        onClick(view, newsImage)
                    }
                }
                thumbnail.apply {
                    imageManager.loadInto(context, newsImage.imageUrl, this)
                }
            }
        }
    }

}