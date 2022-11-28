package org.mtransit.android.data

@JvmInline
value class Authority(val authority: String) {

    companion object {
        const val INVALID = ""
    }

    fun isValid(): Boolean = this.authority.isNotBlank()

    override fun toString() = "${Authority::class.java.simpleName}($authority)"
}