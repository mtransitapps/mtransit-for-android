package org.mtransit.android.data

@JvmInline
value class AuthorityAndUuid(private val authorityAndUuid: Pair<Authority, Uuid>) {

    constructor (authority: Authority, uuid: Uuid) : this(Pair(authority, uuid))

    fun getAuthority(): Authority = authorityAndUuid.first

    fun getUuid(): Uuid = authorityAndUuid.second

    fun isValid() = authorityAndUuid.first.isValid() && authorityAndUuid.second.isValid()

    override fun toString() = "${AuthorityAndUuid::class.java.simpleName}(${authorityAndUuid.first.authority}, ${authorityAndUuid.second.uuid})"
}