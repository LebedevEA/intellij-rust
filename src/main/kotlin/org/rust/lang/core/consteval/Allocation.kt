/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.mir.schemas.MirScalar

/**
 * In compiler, it usually refers as generic Provenance, but in const eval it's always `AllocId`
 * Allocation stores some binary array in compiler, but I don't want to operate with raw bytes, so
 * I am using Kinds. It's not sealed class now, because I think I'll need to dynamically change kind
 * from uninitialized to something more specific
 */
class Allocation private constructor(var kind: Kind) {
    sealed class Kind {
        object Uninitialized : Kind() {
            override fun clone() = this
        }

        class Scalar(val value: MirScalar) : Kind() {
            override fun clone(): Kind {
                return Scalar(value)
            }
        }

        abstract fun clone(): Kind
    }

    companion object {
        fun uninit() = Allocation(Kind.Uninitialized)
        fun scalar(value: MirScalar) = Allocation(Kind.Scalar(value))
    }
}
