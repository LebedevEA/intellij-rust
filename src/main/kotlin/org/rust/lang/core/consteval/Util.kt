/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.mir.schemas.MirBinaryOperator
import org.rust.lang.core.psi.ext.ArithmeticOp
import org.rust.lang.core.types.ty.Ty
import org.rust.lang.core.types.ty.TyTuple

val MirBinaryOperator.isHomogeneous: Boolean get() = when (this.underlyingOp) {
    null, ArithmeticOp.SHL, ArithmeticOp.SHR -> false // null means Offset
    else -> true
}

fun Ty.field(fieldIndex: Int): Ty {
    return when (this) {
        is TyTuple -> this.types[fieldIndex]
        else -> TODO()
    }
}
