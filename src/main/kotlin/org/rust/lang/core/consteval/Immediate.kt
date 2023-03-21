/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.mir.schemas.MirScalarValue

sealed class Immediate {
    data class ImScalar(val scalar: MirScalarValue) : Immediate()
    data class ImScalarPair(val left: MirScalarValue, val right: MirScalarValue) : Immediate()
    object Uninit : Immediate()

    companion object {
        fun fromScalarValue(scalar: MirScalarValue): Immediate {
            return ImScalar(scalar)
        }
    }
}
