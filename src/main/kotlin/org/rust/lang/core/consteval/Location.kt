/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.mir.schemas.MirBasicBlock
import org.rust.lang.core.mir.schemas.MirStatement

// TODO: in compiler this is located in mir module, so consider moving it to mir package
data class Location(val block: MirBasicBlock, val statement: MirStatement? /* null means it's time for terminator */)
