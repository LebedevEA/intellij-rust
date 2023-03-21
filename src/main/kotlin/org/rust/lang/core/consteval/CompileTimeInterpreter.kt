/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

class CompileTimeInterpreter(
    private var stepsRemaining: Int,
    private val recursionLimit: Int,
    private val canAccessStatics: Boolean,
) {
    // in compiler it's init_frame_extra
    fun checkRecursionLimit() {
        if (recursionLimit <= stack.size + 1) {
            TODO() // throw something
        }
    }

    fun push(frame: Frame) {
        stack.add(frame)
    }

    fun curFrame(): Frame {
        return stack.last()
    }

    fun stack(): List<Frame> = stack
    fun pop(): Frame {
        return stack.removeLast()
    }

    fun beforeTerminator() {
        stepsRemaining -= 1
        if (stepsRemaining == 0) {
            TODO() // throw something
        }
    }

    private val stack: MutableList<Frame> = mutableListOf()
}
