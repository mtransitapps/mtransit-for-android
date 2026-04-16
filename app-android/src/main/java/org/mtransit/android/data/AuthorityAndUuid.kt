package org.mtransit.android.data

typealias AuthorityAndUuid = Pair<Authority, Uuid>

fun AuthorityAndUuid.getAuthority(): Authority = this.first
fun AuthorityAndUuid.getUuid(): Uuid = this.second
fun AuthorityAndUuid.isAuthorityAndUuidValid() = this.first.isAuthorityValid() && this.second.isUUIDValid()
@Suppress("unused")
fun AuthorityAndUuid.isEqual(authority: Authority?, uuid: Uuid?) = this.first == authority && this.second == uuid
