/*
 * Use of this source code is governed by the MIT license that can be
 * found in the LICENSE file.
 */

package org.rust.ide.annotator

import org.rust.MockRustcVersion

class RsSyntaxErrorsAnnotatorTest : RsAnnotatorTestBase(RsSyntaxErrorsAnnotator::class) {
    fun `test E0379 const trait function`() = checkErrors("""
        trait Foo {
            fn foo();
            <error descr="Trait functions cannot be declared const [E0379]">const</error> fn bar();
        }
    """)

    fun `test impl assoc function`() = checkErrors("""
        struct Person<D> { data: D }
        impl<D> Person<D> {
            #[inline]
            pub const unsafe fn new<'a>(id: u32, name: &'a str, data: D, _: bool) -> Person<D> where D: Sized {
                Person { data: data }
            }
            default fn def() {}
            extern "C" fn ext_fn() {}

            <error descr="Default associated function `def_pub` cannot have the `pub` qualifier">pub</error> default fn def_pub() {}
            fn no_body()<error descr="Associated function `no_body` must have a body">;</error>
            fn anon_param(<error descr="Associated function `anon_param` cannot have anonymous parameters">u8</error>, a: i16) {}
            fn var_foo(a: bool, <error descr="Associated function `var_foo` cannot be variadic">...</error>) {}
        }
    """)

    fun `test impl method`() = checkErrors("""
        struct Person<D> { data: D }
        impl<D> Person<D> {
            #[inline]
            pub const unsafe fn check<'a>(&self, s: &'a str) -> bool where D: Sized {
                false
            }
            default fn def(&self) {}
            extern "C" fn ext_m(&self) {}

            <error descr="Default method `def_pub` cannot have the `pub` qualifier">pub</error> default fn def_pub(&self) {}
            fn no_body(&self)<error descr="Method `no_body` must have a body">;</error>
            fn anon_param(&self, <error descr="Method `anon_param` cannot have anonymous parameters">u8</error>, a: i16) {}
            fn var_foo(&self, a: bool, <error descr="Method `var_foo` cannot be variadic">...</error>) {}
        }
    """)

    fun `test trait assoc function`() = checkErrors("""
        trait Animal<T> {
            #[inline]
            unsafe fn feed<'a>(food: T, d: &'a str, _: bool, f32) -> Option<f64> where T: Sized {
                None
            }
            fn no_body();
            extern "C" fn ext_fn();

            <error descr="Associated function `default_foo` cannot have the `default` qualifier">default</error> fn default_foo();
            <error descr="Associated function `pub_foo` cannot have the `pub` qualifier">pub</error> fn pub_foo();
            fn tup_param(<error descr="Associated function `tup_param` cannot have tuple parameters">(x, y): (u8, u8)</error>, a: bool);
            fn var_foo(a: bool, <error descr="Associated function `var_foo` cannot be variadic">...</error>);
        }
    """)

    fun `test trait method`() = checkErrors("""
        trait Animal<T> {
            #[inline]
            fn feed<'a>(&mut self, food: T, d: &'a str, _: bool, f32) -> Option<f64> where T: Sized {
                None
            }
            fn no_body(self);
            extern "C" fn ext_m();

            <error descr="Method `default_foo` cannot have the `default` qualifier">default</error> fn default_foo(&self);
            <error descr="Method `pub_foo` cannot have the `pub` qualifier">pub</error> fn pub_foo(&mut self);
            fn tup_param(&self, <error descr="Method `tup_param` cannot have tuple parameters">(x, y): (u8, u8)</error>, a: bool);
            fn var_foo(&self, a: bool, <error descr="Method `var_foo` cannot be variadic">...</error>);
        }
    """)

    fun `test foreign function`() = checkErrors("""
        extern {
            #[cold]
            pub fn full(len: size_t, ...) -> size_t;

            <error descr="Foreign function `default_foo` cannot have the `default` qualifier">default</error> fn default_foo();
            <error descr="Foreign function `with_const` cannot have the `const` qualifier">const</error> fn with_const();
            <error descr="Foreign function `with_unsafe` cannot have the `unsafe` qualifier">unsafe</error> fn with_unsafe();
            <error descr="Foreign function `with_ext_abi` cannot have an extern ABI">extern "C"</error> fn with_ext_abi();
            fn with_self(<error descr="Foreign function `with_self` cannot have `self` parameter">&self</error>, s: size_t);
            fn anon_param(<error descr="Foreign function `anon_param` cannot have anonymous parameters">u8</error>, a: i8);
            fn with_body() <error descr="Foreign function `with_body` cannot have a body">{ let _ = 1; }</error>
            fn var_coma(a: size_t, ...<error descr="`...` must be last in argument list for variadic function">,</error>);
        }
    """)

    fun `test union tuple`() = checkErrors("""
        union U<error descr="Union cannot be tuple-like">(i32, f32)</error>;
    """)

    fun `test type alias free`() = checkErrors("""
        type Int = i32;
        pub type UInt = u32;
        type Maybe<T> = Option<T>;
        type SizedMaybe<T> where T: Sized = Option<T>;

        <error descr="Type `DefBool` cannot have the `default` qualifier">default</error> type DefBool = bool;
        <error descr="Aliased type must be provided for type `Unknown`">type Unknown;</error>
        type Show<error descr="Type `Show` cannot have type parameter bounds">: Display</error> = u32;
    """)

    fun `test type alias in trait`() = checkErrors("""
        trait Computer {
            type Int;
            type Long = i64;
            type Show: Display;

            <error descr="Type `DefSize` cannot have the `default` qualifier">default</error> type DefSize = isize;
            <error descr="Type `PubType` cannot have the `pub` qualifier">pub</error> type PubType;
            type GenType<error descr="Type `GenType` cannot have generic parameters"><T></error> = Option<T>;
            type WhereType <error descr="Type `WhereType` cannot have `where` clause">where T: Sized</error> = f64;
        }
    """)

    fun `test type alias in trait impl`() = checkErrors("""
            trait Vehicle {
                type Engine;
                type Control;
                type Lock;
                type Cage;
                type Insurance;
                type Driver;
            }
            struct NumericVehicle<T> { foo: T }
            impl<T> Vehicle for NumericVehicle<T> {
                type Engine = u32;
                default type Control = isize;
                type Lock<error descr="Type `Lock` cannot have generic parameters"><T></error> = Option<T>;
                type Cage<error descr="Type `Cage` cannot have type parameter bounds">: Sized</error> = f64;
                type Insurance <error descr="Type `Insurance` cannot have `where` clause">where T: Sized</error> = i8;
                <error descr="Aliased type must be provided for type `Driver`">type Driver;</error>
            }
    """)

    fun `test const free`() = checkErrors("""
        const FOO: u32 = 42;
        pub const PUB_FOO: u32 = 41;
        static S_FOO: bool = true;
        static mut S_MUT_FOO: bool = false;
        pub static S_PUB_BAR: u8 = 0;
        pub static mut S_PUB_MUT_BAR: f16 = 1.12;

        <error descr="Constant `BAR` must have a value">const BAR: u8;</error>
        <error descr="Static constant `DEF_BAR` cannot have the `default` qualifier">default</error> static DEF_BAR: u16 = 9;
    """)

    fun `test const in trait`() = checkErrors("""
        trait Foo {
            const FOO_1: u16 = 10;
            const FOO_2: f64;

            <error descr="Constant `PUB_BAZ` cannot have the `pub` qualifier">pub</error> const PUB_BAZ: bool;
            <error descr="Constant `DEF_BAR` cannot have the `default` qualifier">default</error> const DEF_BAR: u16 = 9;
            <error descr="Static constants are not allowed in traits">static</error> ST_FOO: u32 = 18;
        }
    """)

    fun `test const in impl`() = checkErrors("""
        struct Foo;
        impl Foo {
            const FOO: u32 = 109;
            pub const PUB_FOO: u32 = 81;
            default const DEF_FOO: u8 = 1;

            <error descr="Constant `BAR` must have a value">const BAR: u8;</error>
            <error descr="Static constants are not allowed in impl blocks">static</error> ST_FOO: u32 = 18;
        }
    """)

    fun `test const in extern`() = checkErrors("""
        extern "C" {
            static mut FOO: u32;
            pub static mut PUB_FOO: u8;
            static NON_MUT_FOO: u32;

            <error descr="Static constant `DEF_FOO` cannot have the `default` qualifier">default</error> static mut DEF_FOO: bool;
            <error descr="Only static constants are allowed in extern blocks">const</error> CONST_FOO: u32;
            static mut VAL_FOO: u32 <error descr="Static constants in extern blocks cannot have values">= 10</error>;
        }
    """)

    fun `test function`() = checkErrors("""
        #[inline]
        pub const unsafe fn full<'a, T>(id: u32, name: &'a str, data: &T, _: &mut FnMut(Display)) -> Option<u32> where T: Sized {
            None
        }
        fn trailing_comma(a: u32,) {}
        extern "C" fn ext_fn() {}

        <error descr="Function `foo_default` cannot have the `default` qualifier">default</error> fn foo_default(f: u32) {}
        fn ref_self(<error descr="Function `ref_self` cannot have `self` parameter">&mut self</error>, f: u32) {}
        fn no_body()<error descr="Function `no_body` must have a body">;</error>
        fn anon_param(<error descr="Function `anon_param` cannot have anonymous parameters">u8</error>, a: i16) {}
        fn var_foo(a: bool, <error descr="Function `var_foo` cannot be variadic">...</error>) {}
        <error>default</error> fn two_errors(<error>u8</error>, a: i16) {}
    """)

    fun `test E0202 type alias in inherent impl`() = checkErrors("""
        struct Foo;
        impl Foo {
            <error descr="Associated types are not allowed in inherent impls [E0202]">type Long = i64;</error>
        }
    """)

    fun `test add parameter_name fix`() = checkFixByText("Add dummy parameter name", """
        trait Display {
            fn fmt(&self, <warning>F/*caret*/</warning>);
        }
    """, """
        trait Display {
            fn fmt(&self, _: F);
        }
    """)

    fun `test no warning for fn type`() = checkWarnings("""
        fn foo<F: Fn(i32, &mut String)>(f: F) {}
        pub trait T {
            fn foo<F: FnMut(i32)>() {}
        }
    """)

    fun `test no warning for fn trait object`() = checkWarnings("""
        pub trait Registry {
            fn query(&mut self,
                     dep: &Dependency,
                     f: &mut FnMut(Summary)) -> CargoResult<()>;
        }
    """)

    fun `test lifetime params after type params`() = checkErrors("""
        fn foo<T, <error descr="Lifetime parameters must be declared prior to type parameters">'a</error>>(bar: &'a T) {}
    """)

    fun `test type arguments order`() = checkErrors("""
        type A1 = B<C, <error descr="Lifetime arguments must be declared prior to type arguments">'d</error>>;

        type A2 = B<C, <error descr="Lifetime arguments must be declared prior to type arguments">'d</error>,
                      <error descr="Lifetime arguments must be declared prior to type arguments">'e</error>>;

        type A3 = B<C=D, <error descr="Type arguments must be declared prior to associated type bindings">E</error>>;

        type A4 = B<C=D, <error descr="Type arguments must be declared prior to associated type bindings">E</error>,
                         <error descr="Type arguments must be declared prior to associated type bindings">F</error>>;

        type A3 = B<C=D, <error descr="Lifetime arguments must be declared prior to associated type bindings">'e</error>>;

        type A4 = B<C=D, <error descr="Lifetime arguments must be declared prior to associated type bindings">'e</error>,
                         <error descr="Lifetime arguments must be declared prior to associated type bindings">'f</error>>;

        type A5 = B<1, <error descr="Type arguments must be declared prior to const arguments">C</error>,
                         <error descr="Lifetime arguments must be declared prior to const arguments">'d</error>>;
    """)

    fun `test default type parameters in impl`() = checkErrors("""
        struct S<T=String>{ f: T }
        impl<T=<error descr="Defaults for type parameters are only allowed in `struct`, `enum`, `type`, or `trait` definitions">String</error>> S<T> {}
    """)

    fun `test default type parameters order`() = checkErrors("""
        struct S1<<error descr="Type parameters with a default must be trailing">T1=Debug</error>,T2>{ f1: T1, f2: T2 }
        struct S2<T2,T1=String>{ f1: T1, f2: T2 }
    """)

    @MockRustcVersion("1.34.0-nightly")
    fun `test c-variadic function E0658 1`() = checkErrors("""
        unsafe extern "C" fn ext_fn1(a: bool, <error descr="C-variadic functions is experimental [E0658]">...</error>) {}
        unsafe extern "C" fn ext_fn2(a: bool, args: <error descr="C-variadic functions is experimental [E0658]">...</error>) {}
    """)

    @MockRustcVersion("1.34.0-nightly")
    fun `test c-variadic function E0658 2`() = checkErrors("""
        #![feature(c_variadic)]
        unsafe extern "C" fn ext_fn1(a: bool, ...) {}
        unsafe extern "C" fn ext_fn2(a: bool, args: ...) {}
    """)

    fun `test illegal items inside trait, impl or foreign mod`() = checkErrors("""
        trait T {
            <error descr="Structs are not allowed inside a trait">struct</error> A;
            <error descr="Unions are not allowed inside a trait">union</error> B {}
            <error descr="Modules are not allowed inside a trait">mod</error> c {}
            <error descr="Modules are not allowed inside a trait">mod</error> d;
            <error descr="Enums are not allowed inside a trait">enum</error> E {}
            <error descr="Traits are not allowed inside a trait">trait</error> F {}
            <error descr="Trait aliases are not allowed inside a trait">trait</error> G = F;
            <error descr="Impls are not allowed inside a trait">impl</error> A {}
            <error descr="Use items are not allowed inside a trait">use</error> A;
            <error descr="Foreign modules are not allowed inside a trait">extern</error> "C" {}
            <error descr="Extern crates are not allowed inside a trait">extern</error> crate H;
            <error descr="Macros are not allowed inside a trait">macro_rules</error>! i {}
            <error descr="Macros are not allowed inside a trait">macro</error> j() {}
        }
        impl S {
            <error descr="Structs are not allowed inside an impl">struct</error> A;
            <error descr="Unions are not allowed inside an impl">union</error> B {}
            <error descr="Modules are not allowed inside an impl">mod</error> c {}
            <error descr="Modules are not allowed inside an impl">mod</error> d;
            <error descr="Enums are not allowed inside an impl">enum</error> E {}
            <error descr="Traits are not allowed inside an impl">trait</error> F {}
            <error descr="Trait aliases are not allowed inside an impl">trait</error> G = F;
            <error descr="Impls are not allowed inside an impl">impl</error> A {}
            <error descr="Use items are not allowed inside an impl">use</error> A;
            <error descr="Foreign modules are not allowed inside an impl">extern</error> "C" {}
            <error descr="Extern crates are not allowed inside an impl">extern</error> crate H;
            <error descr="Macros are not allowed inside an impl">macro_rules</error>! i {}
            <error descr="Macros are not allowed inside an impl">macro</error> j() {}
        }
        extern "C" {
            <error descr="Structs are not allowed inside a foreign module">struct</error> A;
            <error descr="Unions are not allowed inside a foreign module">union</error> B {}
            <error descr="Modules are not allowed inside a foreign module">mod</error> c {}
            <error descr="Modules are not allowed inside a foreign module">mod</error> d;
            <error descr="Enums are not allowed inside a foreign module">enum</error> E {}
            <error descr="Traits are not allowed inside a foreign module">trait</error> F {}
            <error descr="Trait aliases are not allowed inside a foreign module">trait</error> G = F;
            <error descr="Impls are not allowed inside a foreign module">impl</error> A {}
            <error descr="Use items are not allowed inside a foreign module">use</error> A;
            <error descr="Foreign modules are not allowed inside a foreign module">extern</error> "C" {}
            <error descr="Extern crates are not allowed inside a foreign module">extern</error> crate H;
            <error descr="Macros are not allowed inside a foreign module">macro_rules</error>! i {}
            <error descr="Macros are not allowed inside a foreign module">macro</error> j() {}
        }
    """)

    fun `test 'default' keyword`() = checkErrors("""
        <error descr="Structs cannot have the `default` qualifier">default</error> struct A;
        <error descr="Unions cannot have the `default` qualifier">default</error> union B {}
        <error descr="Modules cannot have the `default` qualifier">default</error> mod c {}
        <error descr="Modules cannot have the `default` qualifier">default</error> mod d;
        <error descr="Enums cannot have the `default` qualifier">default</error> enum E {}
        <error descr="Use items cannot have the `default` qualifier">default</error> use A;
        <error descr="Foreign modules cannot have the `default` qualifier">default</error> extern "C" {}
        <error descr="Extern crates cannot have the `default` qualifier">default</error> extern crate H;
        <error descr="Macros cannot have the `default` qualifier">default</error> macro_rules! i {}
        <error descr="Macros cannot have the `default` qualifier">default</error> macro j() {}
        <error descr="Macro invocations cannot have the `default` qualifier">default</error> foo!();
    """)

    fun `test constants without a type`() = checkErrors("""
        <error descr="Missing type for `const` item">const MY_CONST = 1;</error>
        <error descr="Missing type for `static` item">static MY_STATIC = 1;</error>
        const PARTIAL_TYPE:<error descr="<type> expected, got '='"> </error> = 1;
    """)
}
