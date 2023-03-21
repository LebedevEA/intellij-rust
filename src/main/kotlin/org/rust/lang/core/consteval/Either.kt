/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

// TODO: maybe something is implemented already
sealed class Either<in L, in R> {
    data class Left<L>(val value: L) : Either<L, Any>()
    data class Right<R>(val value: R) : Either<Any, R>()
}
