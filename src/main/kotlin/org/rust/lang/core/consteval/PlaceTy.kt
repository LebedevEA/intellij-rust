/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.types.ty.Ty

data class PlaceTy(val place: Place, val local: Ty)
