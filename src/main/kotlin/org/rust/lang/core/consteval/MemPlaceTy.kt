/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.types.ty.Ty

class MemPlaceTy(
    val memPlace: MemPlace,
    val ty: Ty
) {
    constructor(pointer: Pointer, ty: Ty) : this(MemPlace(pointer), ty)

    fun toPlaceTy(): PlaceTy {
        return PlaceTy(Place.Ptr(memPlace), ty)
    }
}
