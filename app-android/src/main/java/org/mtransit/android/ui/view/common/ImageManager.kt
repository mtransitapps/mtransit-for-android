package org.mtransit.android.ui.view.common

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor() {

    fun loadInto(context: Context, imageUrl: String?, imageView: ImageView) {
        Glide.with(context)
            .load(imageUrl).into(imageView)
    }

    fun loadInto(fragment: Fragment, imageUrl: String?, imageView: ImageView) {
        Glide.with(fragment)
            .load(imageUrl).into(imageView)
    }

    fun clear(context: Context, view: View) {
        Glide.with(context)
            .clear(view)
    }

    fun clear(fragment: Fragment, view: View) {
        Glide.with(fragment)
            .clear(view)
    }
}

fun ImageView.loadImage(imageManager: ImageManager, imageUrl: String?) =
    imageManager.loadInto(this.context, imageUrl, this)

fun ImageView.loadImageAndShow(imageManager: ImageManager, imageUrl: String?) {
    loadImage(imageManager, imageUrl)
    isVisible = true
}

fun ImageView.clearImage(imageManager: ImageManager) =
    imageManager.clear(this.context, this)

fun ImageView.clearImageAndHide(imageManager: ImageManager) {
    clearImage(imageManager)
    isVisible = false
}
