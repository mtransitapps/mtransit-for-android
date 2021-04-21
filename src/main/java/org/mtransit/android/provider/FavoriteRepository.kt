package org.mtransit.android.provider

import org.mtransit.android.common.IApplication

class FavoriteRepository(private val app: IApplication) {

    fun findFavoriteUUIDs(): Set<String> {
        return FavoriteManager.findFavoriteUUIDs(app.requireContext())
    }
}