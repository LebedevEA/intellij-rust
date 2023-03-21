/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.mir.schemas.MirLocal

sealed class Place {
    data class Local(val frame: Frame, val local: MirLocal) : Place() {
        fun access(): Operand {
            return when (val value = frame.locals[local]?.value) {
                LocalValue.Dead -> TODO()
                is LocalValue.Live -> value.operand
                null -> TODO()
            }
        }
    }

    data class Ptr(val pointer: MemPlace) : Place()
}
