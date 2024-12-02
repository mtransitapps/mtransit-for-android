package org.mtransit.android.data

typealias Authority = String

fun Authority.isAuthorityValid(): Boolean = this.isNotBlank()

const val AUTHORITY_INVALID = ""
