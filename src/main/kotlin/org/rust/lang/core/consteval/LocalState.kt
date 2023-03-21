/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

data class LocalState(val value: LocalValue) {
    fun access(): Operand {
        return when (value) {
            LocalValue.Dead -> TODO()
            is LocalValue.Live -> value.operand
        }
    }
}
