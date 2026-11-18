package org.mtransit.android.data

typealias Authority = String

val Authority.isAuthorityValid: Boolean get() = this.isNotBlank()

const val AUTHORITY_INVALID = ""
