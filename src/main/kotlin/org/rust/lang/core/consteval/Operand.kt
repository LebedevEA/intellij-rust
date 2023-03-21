/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

sealed class Operand {
    data class OpImmediate(var value: Immediate) : Operand()
    // TODO: Indirect
}
