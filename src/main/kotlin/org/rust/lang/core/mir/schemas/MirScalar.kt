/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.mir.schemas

sealed class MirScalar {
    data class Int(val scalarInt: MirScalarInt) : MirScalar()

    fun tryToInt(): MirScalarInt? = when (this) {
        is Int -> scalarInt
    }

    // TODO: there is error handling done here
    fun toBits(): Long = tryToInt()?.toBits() ?: error("Could not get bits from scalar")

    companion object {
        fun from(bool: Boolean) = Int(MirScalarInt(if (bool) 1 else 0, 0)) // TODO: size is not used anywhere
    }
}
