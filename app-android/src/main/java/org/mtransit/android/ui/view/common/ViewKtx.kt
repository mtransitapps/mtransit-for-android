package org.mtransit.android.ui.view.common

import android.content.Context
import android.content.res.Resources.NotFoundException
import android.graphics.drawable.Drawable
import android.view.View
import android.widget.TextView
import androidx.annotation.DrawableRes
import androidx.annotation.PluralsRes
import androidx.annotation.Px
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding

val ViewBinding.context: Context get() = root.context

var TextView.textAndVisibility: CharSequence?
    get() = text
    set(value) {
        isVisible = !value.isNullOrBlank()
        text = value
    }

fun TextView.setTextQuantityText(@PluralsRes resId: Int, quantity: Int) {
    this.text = this.resources.getQuantityText(resId, quantity)
}

fun TextView.setTextQuantityString(@PluralsRes resId: Int, quantity: Int) {
    this.text = this.resources.getQuantityString(resId, quantity)
}

fun TextView.setTextQuantityString(@PluralsRes resId: Int, quantity: Int, vararg formatArgs: Any) {
    this.text = this.resources.getQuantityString(resId, quantity, *formatArgs)
}

fun TextView.setDrawables(
    @DrawableRes start: Int? = null,
    @DrawableRes top: Int? = null,
    @DrawableRes end: Int? = null,
    @DrawableRes bottom: Int? = null,
    relative: Boolean,
    withIntrinsicBounds: Boolean,
) {
    setDrawables(
        left = start.takeIf { it != 0 }?.let { AppCompatResources.getDrawable(context, it) },
        top = top.takeIf { it != 0 }?.let { AppCompatResources.getDrawable(context, it) },
        right = end.takeIf { it != 0 }?.let { AppCompatResources.getDrawable(context, it) },
        bottom = bottom.takeIf { it != 0 }?.let { AppCompatResources.getDrawable(context, it) },
        relative = relative,
        withIntrinsicBounds = withIntrinsicBounds,
    )
}

fun TextView.setDrawables(
    left: Drawable? = null,
    top: Drawable? = null,
    right: Drawable? = null,
    bottom: Drawable? = null,
    relative: Boolean,
    withIntrinsicBounds: Boolean,
) {
    if (relative) {
        if (withIntrinsicBounds) {
            setCompoundDrawablesRelativeWithIntrinsicBounds(left, top, right, bottom)
        } else {
            setCompoundDrawablesRelative(left, top, right, bottom)
        }
    } else {
        if (withIntrinsicBounds) {
            setCompoundDrawablesWithIntrinsicBounds(left, top, right, bottom)
        } else {
            this.setCompoundDrawables(left, top, right, bottom)
        }
    }
}

fun View.setPadding(
    @Px horizontal: Int = 0,
    @Px vertical: Int = 0,
    @Px left: Int = horizontal,
    @Px top: Int = vertical,
    @Px right: Int = horizontal,
    @Px bottom: Int = vertical,
    relative: Boolean = false,
) {
    if (relative) {
        this.setPaddingRelative(left, top, right, bottom)
    } else {
        this.setPadding(left, top, right, bottom)
    }
}

inline var ViewBinding.isVisible: Boolean
    get() = root.visibility == View.VISIBLE
    set(value) {
        root.visibility = if (value) View.VISIBLE else View.GONE
    }

inline var View.isVisibleOnce: Boolean
    get() = isVisible
    set(value) {
        if (value != isVisible) {
            isVisible = value
        }
    }

/**
 * [android.view.View.toString]
 */
fun View.toStringSimple(): String {
    return javaClass.simpleName + "{" +
            "hex:${Integer.toHexString(System.identityHashCode(this))}, " +
            "id:${prettyId()}, " +
            "isVisible:$isVisible, " +
            "}"
}

/**
 * [android.view.View.toString]
 * [android.content.res.Resources] resourceHasPackage() HIDDEN
 */
fun View.prettyId(): String {
    val out = StringBuilder(128)
    val id = id
    if (id != View.NO_ID) {
        out.append(" #")
        out.append(Integer.toHexString(id))
        // val r: Resources? = this.resources
        val resourceHasPackage: Boolean = id ushr 24 != 0
        if (id > 0 && resourceHasPackage && resources != null) {
            try {
                val pkgName: String = when (id and -0x1000000) {
                    0x7f000000 -> "app"
                    0x01000000 -> "android"
                    else -> resources.getResourcePackageName(id)
                }
                val typeName = resources.getResourceTypeName(id)
                val entryName = resources.getResourceEntryName(id)
                out.append(" ")
                out.append(pkgName)
                out.append(":")
                out.append(typeName)
                out.append("/")
                out.append(entryName)
            } catch (_: NotFoundException) {
            }
        }
    }
    return out.toString()
}