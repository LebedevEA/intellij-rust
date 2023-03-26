/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import org.rust.lang.core.mir.MirBuilder
import org.rust.lang.core.mir.asSource
import org.rust.lang.core.mir.isSigned
import org.rust.lang.core.mir.schemas.*
import org.rust.lang.core.mir.schemas.MirPlace
import org.rust.lang.core.psi.RsConstant
import org.rust.lang.core.psi.ext.ArithmeticOp
import org.rust.lang.core.types.ty.Ty
import org.rust.lang.core.types.ty.isIntegral


/**
 * Something similar to `InterpCx` from compiler
 */
class Interpreter private constructor(
    private val machine: CompileTimeInterpreter,
    private val source: MirSourceInfo,
) {
    private val memory = Memory()

    private fun eval(body: MirBody): MemPlaceTy {
        val returnPlace = allocate(body.returnPlace().ty, MemoryKind.STACK)
        pushStackFrame(body, returnPlace.toPlaceTy(), StackPopCleanup.Root)
        do {
            val hasNextStep = step()
        } while (hasNextStep)
        return returnPlace
    }

    private fun step(): Boolean {
        if (machine.stack().isEmpty()) return false

        val location = when (val location = machine.curFrame().location) {
            is Either.Left -> location.value
            is Either.Right -> {
                popStackFrame(unwinding = true)
                return true
            }
        }

        location.statement?.let { statement ->
            statement(statement)
            machine.curFrame().nextStatement()
        } ?: run {
            machine.beforeTerminator()
            terminator(location.block.terminator)
        }
        return true
    }

    private fun statement(statement: MirStatement) {
        when (statement) {
            is MirStatement.Assign -> evalRvalueIntoPlace(statement.rvalue, statement.place)
            is MirStatement.StorageDead -> TODO()
            is MirStatement.StorageLive -> TODO()
        }
    }

    private fun evalRvalueIntoPlace(rvalue: MirRvalue, place: MirPlace) {
        val dest = evalPlace(place)
        when (rvalue) {
            is MirRvalue.Aggregate.Tuple -> TODO()
            is MirRvalue.BinaryOpUse -> TODO()
            is MirRvalue.CheckedBinaryOpUse -> {
                val left = readImmediate(evalOperand(rvalue.left, null))
                val layoutTy = if (rvalue.op.isHomogeneous) left.ty else null
                val right = readImmediate(evalOperand(rvalue.right, layoutTy))
                binopWithOverflow(rvalue.op, left, right, dest)
            }
            is MirRvalue.UnaryOpUse -> TODO()
            is MirRvalue.Use -> copyOp(evalOperand(rvalue.operand, dest.ty), dest, false)
        }
    }

    private fun binopWithOverflow(
        operator: MirBinaryOperator,
        left: ImmediateTy,
        right: ImmediateTy,
        dest: PlaceTy,
    ) {
        val (value, overflowed, ty) = overflowingBinaryOp(operator, left, right)
        val pair = Immediate.ImScalarPair(value, MirScalar.from(overflowed))
        // randomize layout is unstable option so not handling it
        writeImmediate(pair, dest)
    }

    private fun overflowingBinaryOp(
        op: MirBinaryOperator,
        left: ImmediateTy,
        right: ImmediateTy,
    ): Triple<MirScalar, Boolean, Ty> {
        val tyKind = left.ty.kind
        return when {
            left.ty.isIntegral -> {
                val leftBits = left.immediate.asScalar().toBits()
                val rightBits = right.immediate.asScalar().toBits()
                binaryIntOp(op, leftBits, left.ty, rightBits, right.ty)
            }
            else -> TODO()
        }
    }

    private fun binaryIntOp(
        operator: MirBinaryOperator,
        left: Long,
        leftTy: Ty,
        right: Long,
        rightTy: Ty,
    ): Triple<MirScalar, Boolean, Ty> {
        if (operator.underlyingOp is ArithmeticOp.SHR || operator.underlyingOp is ArithmeticOp.SHL) {
            TODO()
        }
        if (leftTy.isSigned) {
            when (operator.underlyingOp) {
                // TODO: overflow and truncating
                is ArithmeticOp.ADD -> return Triple(MirScalar.from(left + right), false, leftTy)
                else -> TODO()
            }
        }
        TODO()
    }

    private fun readImmediate(operand: OperandTy): ImmediateTy {
        return readImmediateRaw(operand)
            .let { it as Either.Right<ImmediateTy> }
            .value
    }

    private fun copyOp(src: OperandTy, dest: PlaceTy, allowTransmutate: Boolean) {
        // validation is unstable option, so not doing it :)
        // TODO: there is something about whether "mir_assign_valid_types", not sure if I ned this
        val memorySrc = when (val either = readImmediateRaw(src)) {
            is Either.Left -> either.value
            is Either.Right -> {
                // TODO: check that src's and dest's layouts are sized (I don't think I need this)
                val scrValue = either.value
                // TODO: there is handling if layout is not compatible, I think it should always be in my case
                return writeImmediate(scrValue.immediate, dest)
            }
        }
        TODO()
        val memoryDest = forceAllocation(dest)
        memoryCopy(memorySrc.memPlace.pointer, memoryDest.memPlace.pointer)
    }

    private fun memoryCopy(src: Pointer?, dest: Pointer?) {
        // I think this means that it's going to be zst case, unused at the moment
        if (src == null) return
        requireNotNull(dest)
        // TODO: I'm just doing something easy here, but maybe I have to make it more difficult
        dest.provenance.kind = src.provenance.kind.clone()
    }

    private fun forceAllocation(dest: PlaceTy): MemPlaceTy {
        TODO("Not yet implemented")
    }

    private fun writeImmediate(src: Immediate, dest: PlaceTy) {
        val memPlace = when (dest.place) {
            is Place.Local -> when (val state = dest.place.access()) {
                is Operand.OpImmediate -> {
                    state.value = src
                    return
                }
            }
            is Place.Ptr -> dest.place.pointer
        }
        writeImmediateToMemPlace(src, memPlace)
    }

    private fun writeImmediateToMemPlace(value: Immediate, dest: MemPlace) {
        // In compiler, it's a bit more complex, I don't think it's needed
        val allocation = dest.pointer?.provenance ?: return // ZST
        when (value) {
            is Immediate.ImScalar -> allocation.kind = Allocation.Kind.Scalar(value.scalar)
            is Immediate.ImScalarPair -> TODO()
            Immediate.Uninit -> TODO()
        }
    }

    private fun readImmediateRaw(operand: OperandTy): Either<MemPlaceTy, ImmediateTy> {
        return when (val either = operand.asMemPlaceOrImm()) {
            is Either.Left -> TODO()
            is Either.Right -> either
        }
    }

    private fun evalPlace(mirPlace: MirPlace): PlaceTy {
        var place = PlaceTy(Place.Local(machine.curFrame(), mirPlace.local), mirPlace.local.ty)
        mirPlace.projections.forEach {
            place = placeProjection(place, it)
        }
        return place
    }

    private fun placeProjection(place: PlaceTy, projectionElem: MirProjectionElem<Ty>): PlaceTy {
        return when (projectionElem) {
            is MirProjectionElem.Field -> placeField(place, projectionElem.fieldIndex)
        }
    }

    private fun placeField(place: PlaceTy, fieldIndex: Int): PlaceTy {
        TODO("Not yet implemented")
    }

    private fun evalOperand(operand: MirOperand, layoutTy: Ty?): OperandTy {
        return when (operand) {
            is MirOperand.Constant -> {
                // TODO: there is something complicated called subst_from_current_frame_and_normalize_erasing_regions
                evalMirConstant(operand.constant, layoutTy)
            }
            is MirOperand.Copy -> evalPlaceToOp(operand.place, layoutTy)
            is MirOperand.Move -> evalPlaceToOp(operand.place, layoutTy)
        }
    }

    private fun evalPlaceToOp(place: MirPlace, layoutTy: Ty?): OperandTy {
        val usedLayoutTy = if (place.projections.isEmpty()) layoutTy else null
        return place.projections.fold(localToOperand(place.local, usedLayoutTy)) { operand, proj ->
            operandProjection(operand, proj)
        }
    }

    private fun operandProjection(base: OperandTy, projectionElem: MirProjectionElem<Ty>): OperandTy {
        return when (projectionElem) {
            is MirProjectionElem.Field -> operandField(base, projectionElem.fieldIndex)
        }
    }

    private fun operandField(base: OperandTy, fieldIndex: Int): OperandTy {
        val actualBase = when (val either = base.asMemPlaceOrImm()) {
            is Either.Left -> {
                TODO()
            }
            is Either.Right -> either.value
        }
        val fieldTy = actualBase.ty.field(fieldIndex)
        val fieldValue = when (actualBase.immediate) {
            is Immediate.ImScalarPair -> when (fieldIndex) {
                0 -> Immediate.fromScalarValue(actualBase.immediate.left)
                1 -> Immediate.fromScalarValue(actualBase.immediate.right)
                else -> error("Field of pair can only be 0 or 1 but it is $fieldIndex")
            }
            else -> TODO()
        }
        return OperandTy(
            operand = Operand.OpImmediate(fieldValue),
            ty = fieldTy,
        )
    }

    private fun MemPlaceTy.field(fieldIndex: Int): MemPlaceTy {
        TODO()
    }

    private fun localToOperand(local: MirLocal, layoutTy: Ty?): OperandTy {
        val operand = machine.curFrame().locals[local]?.access() ?: error("Local has no state")
        return OperandTy(operand, layoutTy ?: local.ty)
    }

    private fun evalMirConstant(constant: MirConstant, layoutTy: Ty?): OperandTy {
        return when (constant) {
            is MirConstant.Value -> constValToOp(constant.constValue, constant.ty, layoutTy)
        }
    }

    private fun constValToOp(constValue: MirConstValue, ty: Ty, layoutTy: Ty?): OperandTy {
        // TODO: there is an adjustment for scalar, but it's not idempotent only in case of pointers
        val operand = when (constValue) {
            is MirConstValue.Scalar -> Operand.OpImmediate(Immediate.fromScalarValue(constValue.value))
        }
        return OperandTy(operand, layoutTy ?: ty)
    }

    private fun terminator(terminator: MirTerminator<MirBasicBlock>) {
        when (terminator) {
            is MirTerminator.Assert -> {
                val conditionValue = readScalar(evalOperand(terminator.cond, null)).toBool()
                if (conditionValue == terminator.expected) {
                    machine.curFrame().setLocation(terminator.target)
                } else {
                    TODO()
                }
            }
            is MirTerminator.Goto -> TODO()
            is MirTerminator.Resume -> TODO()
            is MirTerminator.Return -> popStackFrame(false)
            is MirTerminator.SwitchInt -> TODO()
        }
    }

    private fun goto(block: MirBasicBlock) {

    }

    private fun readScalar(operand: OperandTy): MirScalar {
        return readImmediate(operand).immediate.asScalar()
    }

    private fun pushStackFrame(body: MirBody, returnPlace: PlaceTy, returnTo: StackPopCleanup) {
        val frame = Frame(
            body = body,
            location = Either.Right(body.source),
            returnPlace = returnPlace,
            locals = LinkedHashMap<MirLocal, LocalState>(body.localDecls.size).apply {
                val alwaysStorageLiveLocals = body.alwaysStorageLiveLocals()
                body.localDecls.forEach { local ->
                    this[local] = if (local in alwaysStorageLiveLocals) {
                        LocalState(LocalValue.Live(Operand.OpImmediate(Immediate.Uninit)))
                    } else {
                        LocalState(LocalValue.Dead)
                    }
                }
            },
            returnTo = returnTo,
        )
        machine.checkRecursionLimit()
        machine.push(frame)
        // TODO: make sure all the constants required by this frame evaluate successfully (c) compiler
        machine.curFrame().setStartLocation()
        // TODO: do I need tracing?
    }

    private fun popStackFrame(unwinding: Boolean) {
        if (unwinding && machine.stack().size == 1) {
            TODO() // throw something
        }
        if (!unwinding) {
            copyOp(
                src = localToPlace(machine.curFrame(), machine.curFrame().body.returnPlace()),
                dest = machine.curFrame().returnPlace,
                allowTransmutate = true,
            )
        }
        val returnToBlock = machine.curFrame().returnTo
        val cleanup = when (returnToBlock) {
            StackPopCleanup.Root -> false
        }
        if (cleanup) {
            TODO() // not used yet
        }
        val frame = machine.pop()
        if (!cleanup) return
        TODO() // not yet used
    }

    private fun localToPlace(frame: Frame, local: MirLocal): OperandTy {
        val operand = frame.locals[local]?.access() ?: TODO()
        return OperandTy(operand, local.ty)
    }

    private fun allocate(ty: Ty, kind: MemoryKind): MemPlaceTy {
        val ptr = allocatePtr(kind)
        return MemPlaceTy(ptr, ty)
    }

    private fun allocatePtr(kind: MemoryKind): Pointer {
        val allocation = Allocation.uninit()
        return allocateRawPtr(allocation, kind)
    }

    private fun allocateRawPtr(allocation: Allocation, kind: MemoryKind): Pointer {
        memory.allocationMap[allocation] = kind
        return Pointer(allocation)
    }

    companion object {
        fun interpret(constant: RsConstant): Allocation {
            val interpreter = Interpreter(
                machine = CompileTimeInterpreter(
                    stepsRemaining = 2468, // TODO this numbers are made up
                    recursionLimit = 1357, // TODO ^^^^^^^^^^^^^^^^^^^^^^^^
                    canAccessStatics = constant.static != null,
                ),
                source = constant.asSource,
            )
            val mir = MirBuilder.build(constant)
            val res = interpreter.eval(mir)
            return res.memPlace.pointer?.provenance ?: TODO()
        }
    }
}
