package org.mtransit.android.data

import org.mtransit.android.commons.data.News

val News.authorityT: Authority
    get() = Authority(this.authority)

val News.uuidT: Uuid
    get() = Uuid(this.uuid)

val News.authorityAndUuidT: AuthorityAndUuid
    get() = AuthorityAndUuid(this.authorityT, this.uuidT)

val News.imageUrls: List<NewsImage>
    get() = (0 until this.imageURLsCount).map {
        NewsImage(this.getImageUrl(it))
    }
