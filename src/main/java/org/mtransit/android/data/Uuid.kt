package org.mtransit.android.data

@JvmInline
value class Uuid(val uuid: String) {

    companion object {
        const val INVALID = ""
    }

    fun isValid(): Boolean = this.uuid.isNotBlank()

    override fun toString() = "${Uuid::class.java.simpleName}($uuid)"
}