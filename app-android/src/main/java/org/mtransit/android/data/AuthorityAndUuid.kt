package org.mtransit.android.data

typealias AuthorityAndUuid = Pair<Authority, Uuid>

val AuthorityAndUuid.authority: Authority get() = this.first
val AuthorityAndUuid.uuid: Uuid get() = this.second
val AuthorityAndUuid.isAuthorityAndUuidValid get() = this.first.isAuthorityValid && this.second.isUUIDValid()
@Suppress("unused")
fun AuthorityAndUuid.isEqual(authority: Authority?, uuid: Uuid?) = this.first == authority && this.second == uuid
