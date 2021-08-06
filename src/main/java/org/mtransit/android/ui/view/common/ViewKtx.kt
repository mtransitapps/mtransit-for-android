package org.mtransit.android.ui.view.common

import android.content.res.Resources.NotFoundException
import android.view.View
import androidx.core.view.isVisible
import androidx.viewbinding.ViewBinding


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
            } catch (e: NotFoundException) {
            }
        }
    }
    return out.toString()
}