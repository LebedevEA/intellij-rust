/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.mir.schemas.MirScalar

sealed class Immediate {
    data class ImScalar(val scalar: MirScalar) : Immediate()
    data class ImScalarPair(val left: MirScalar, val right: MirScalar) : Immediate()
    object Uninit : Immediate()

    companion object {
        fun fromScalarValue(scalar: MirScalar): Immediate {
            return ImScalar(scalar)
        }
    }
}
