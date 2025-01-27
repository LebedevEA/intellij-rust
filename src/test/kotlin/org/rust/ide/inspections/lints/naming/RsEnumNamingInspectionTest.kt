/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.inspections.lints.naming

import org.rust.ide.inspections.RsInspectionsTestBase
import org.rust.ide.inspections.lints.RsEnumNamingInspection

class RsEnumNamingInspectionTest : RsInspectionsTestBase(RsEnumNamingInspection::class) {
    fun `test enums`() = checkByText("""
        enum EnumOk {}
        enum <warning descr="Type `enum_foo` should have a camel case name such as `EnumFoo`">enum_foo</warning> {}
    """)

    fun `test enums suppression`() = checkByText("""
        #[allow(non_camel_case_types)]
        enum enum_foo {}
    """)

    fun `test enums fix`() = checkFixByText("Rename to `EnumFoo`", """
        enum <warning descr="Type `enum_foo` should have a camel case name such as `EnumFoo`">enum_f<caret>oo</warning> { Var }
        fn enum_use() {
            let a = enum_foo::Var;
        }
    """, """
        enum EnumFoo { Var }
        fn enum_use() {
            let a = EnumFoo::Var;
        }
    """, preview = null)

    fun `test enum not support case`() = checkByText("""
        enum 枚举 {}
    """)
}
