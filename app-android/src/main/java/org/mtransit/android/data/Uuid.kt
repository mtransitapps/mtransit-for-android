package org.mtransit.android.data

typealias Uuid = String

fun Uuid.isUUIDValid(): Boolean = this.isNotBlank()

const val UUID_INVALID = ""
