/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.mir.schemas.MirBasicBlock
import org.rust.lang.core.mir.schemas.MirBody
import org.rust.lang.core.mir.schemas.MirLocal
import org.rust.lang.core.mir.schemas.MirSourceInfo

class Frame(
    val body: MirBody,
    val returnPlace: PlaceTy,
    /**
     *  LocalStates here MUST be different objects, because they are mutable
     */
    val locals: Map<MirLocal, LocalState>,
    location: Either<Location, MirSourceInfo>,
    // TODO: instance: do I need this? it's sort of complicated
    val returnTo: StackPopCleanup
) {
    var location: Either<Location, MirSourceInfo> = location
        private set

    private val nextStatement = body
        .basicBlocks
        .flatMap { it.statements.windowed(size = 2, step = 1, partialWindows = true) }
        .associate { statements -> statements[0] to statements.getOrNull(1) }

    fun nextStatement() {
        when (val loc = location) {
            is Either.Left -> location = Either.Left(Location(loc.value.block, nextStatement[loc.value.statement]))
            else -> error("Can't set next statement when location is not defined")
        }
    }

    fun setStartLocation() {
        location = Either.Left(body.startLocation())
    }

    fun setLocation(block: MirBasicBlock) {
        location = Either.Left(Location(block, block.statements.first()))
    }

    private fun MirBody.startLocation(): Location {
        val startBlock = this.basicBlocks.first()
        return Location(block = startBlock, statement = startBlock.statements.first())
    }
}
