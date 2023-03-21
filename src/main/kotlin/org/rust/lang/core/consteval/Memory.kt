/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

class Memory {
    // In compiler, it maps AllocId to (Allocation, MemoryKind), but since I'm not using ids, I just store Kind here
    // TODO: do I even need kinds?
    // if we have Allocation here, than it is not global
    val allocationMap: MutableMap<Allocation, MemoryKind> = mutableMapOf()
}
