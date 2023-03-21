/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.types.ty.Ty

class OperandTy(
    val operand: Operand,
    val ty: Ty,
) {
    fun asMemPlaceOrImm(): Either<MemPlaceTy, ImmediateTy> {
        return when (operand) {
            is Operand.OpImmediate -> Either.Right(ImmediateTy(operand.value, ty))
        }
    }
}
