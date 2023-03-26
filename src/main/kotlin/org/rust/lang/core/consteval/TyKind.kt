/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.types.ty.Ty
import org.rust.lang.core.types.ty.TyBool
import org.rust.lang.core.types.ty.TyInteger

sealed interface TyKind {
    sealed interface IntTy : TyKind {
        object I32 : IntTy
    }
    object Bool : TyKind
}

val Ty.kind: TyKind get() = when (this) {
    is TyInteger.I32 -> TyKind.IntTy.I32
    TyBool.INSTANCE -> TyKind.Bool
    else -> TODO("Unimplemented for $this")
}
