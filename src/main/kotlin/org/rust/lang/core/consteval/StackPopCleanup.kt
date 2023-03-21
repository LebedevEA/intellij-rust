/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

sealed class StackPopCleanup {
    object Root : StackPopCleanup() // TODO there is something about cleanup, but I don't think it's used in const eval
    // TODO: goto
}
