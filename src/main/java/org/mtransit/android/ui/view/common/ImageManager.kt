package org.mtransit.android.ui.view.common

import android.content.Context
import android.view.View
import android.widget.ImageView
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImageManager @Inject constructor(
) {

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