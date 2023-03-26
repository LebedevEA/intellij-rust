/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.lang.core.consteval

import junit.framework.TestCase
import org.intellij.lang.annotations.Language
import org.rust.ProjectDescriptor
import org.rust.RsTestBase
import org.rust.WithStdlibRustProjectDescriptor
import org.rust.lang.core.mir.schemas.MirScalar
import org.rust.lang.core.mir.schemas.MirScalarInt
import org.rust.lang.core.psi.RsConstant
import org.rust.lang.core.psi.RsFile

@ProjectDescriptor(WithStdlibRustProjectDescriptor::class)
class ConstEvaluationTest : RsTestBase() {
    fun `test constant`() = doTest(
        code = "const FOO: i32 = 43;",
        expected = int(43),
    )

    fun `test add`() = doTest(
        code = "const FOO: i32 = 30 + 13;",
        expected = int(43),
    )

    fun `test sub`() = doTest(
        code = "const FOO: i32 = 43 - 1;",
        expected = int(42),
    )

    fun `test mul`() = doTest(
        code = "const FOO: i32 = 21 * 2;",
        expected = int(42),
    )

    fun `test div`() = doTest(
        code = "const FOO: i32 = 43 / 2;",
        expected = int(21),
    )

    fun `test rem`() = doTest(
        code = "const FOO: i32 = 43 % 13;",
        expected = int(4),
    )

    fun `test bit and`() = doTest(
        code = "const FOO: i32 = 5 & 3;",
        expected = int(1),
    )

    fun `test bit or`() = doTest(
        code = "const FOO: i32 = 5 | 2;",
        expected = int(7),
    )

    fun `test bit xor`() = doTest(
        code = "const FOO: i32 = 7 ^ 5;",
        expected = int(2),
    )

    fun `test shl`() = doTest(
        code = "const FOO: i32 = 3 << 3;",
        expected = int(24),
    )

    fun `test shr`() = doTest(
        code = "const FOO: i32 = 3 >> 1;",
        expected = int(1),
    )

    private fun doTest(
        @Language("Rust") code: String,
        expected: Allocation,
        fileName: String = "main.rs"
    ) {
        InlineFile(code, fileName)
        val constant = (myFixture.file as RsFile).children.find { it is RsConstant } as RsConstant
        val actual = Interpreter.interpret(constant)
        compareRecursively(expected, actual)
    }

    private fun int(value: Long): Allocation {
        return Allocation.scalar(MirScalar.Int(MirScalarInt(value, 0)))
    }

    // TODO: maybe use something already implemented
    private fun compareRecursively(expected: Any?, actual: Any?) {
        if (expected === actual) return
        if (expected == null) TestCase.assertNotNull(actual)
        if (actual == null) TestCase.assertNotNull(expected)
        TestCase.assertEquals(expected!!.javaClass, actual!!.javaClass)
        if (expected.javaClass.isPrimitive) {
            return TestCase.assertEquals(expected, actual)
        }
        // TODO: do I need arrays and something similar
        expected.javaClass.declaredFields.forEach {
            it.isAccessible = true
            compareRecursively(it.get(expected), it.get(actual))
        }
    }
}
