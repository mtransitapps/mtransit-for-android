package org.mtransit.android.ui.view.common

import android.content.Context
import android.graphics.drawable.PictureDrawable
import com.bumptech.glide.Glide
import com.bumptech.glide.Registry
import com.bumptech.glide.annotation.GlideModule
import com.bumptech.glide.integration.okhttp3.OkHttpUrlLoader
import com.bumptech.glide.load.model.GlideUrl
import com.bumptech.glide.module.AppGlideModule
import com.caverock.androidsvg.SVG
import org.mtransit.android.commons.di.NetworkComponents
import java.io.InputStream

// https://github.com/bumptech/glide/blob/master/samples/svg/
@GlideModule
class SVGModule : AppGlideModule() {

    override fun registerComponents(context: Context, glide: Glide, registry: Registry) {
        registry.apply {
            register(SVG::class.java, PictureDrawable::class.java, SVGDrawableTranscoder())
            append(InputStream::class.java, SVG::class.java, SVGDecoder())
            replace(GlideUrl::class.java, InputStream::class.java, OkHttpUrlLoader.Factory(NetworkComponents.getOkHttpClient(context)))
        }
    }

    // Disable manifest parsing to avoid adding similar modules twice.
    override fun isManifestParsingEnabled(): Boolean {
        return false
    }
}