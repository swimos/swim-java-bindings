// Copyright 2015-2024 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

use crate::env::{JavaExceptionHandler, Scope};
use jni::errors::Error as JError;
use jni::errors::Error::WrongJValueType;
use jni::objects::{GlobalRef, JMethodID, JObject, JString, JValue};
use jni::signature::TypeSignature;
use std::any::type_name;
use std::fmt::Debug;
use std::marker::PhantomData;
use std::sync::Arc;

/// An initialised Java Object Method mirror that contains a parsed type signature and a method
/// ID.
///
/// It is expected that the method ID remains consistent for the lifetime of the application; as
/// in the case in HotSpot VM's.
#[derive(Debug, Clone)]
pub struct InitialisedJavaObjectMethod {
    def: JavaObjectMethodDef,
    signature: Arc<TypeSignature>,
    method_id: JMethodID,
}

impl<'j> JavaObjectMethod<'j> for InitialisedJavaObjectMethod {
    type Output = JValue<'j>;

    fn invoke<'s, H, O>(
        &self,
        handler: &H,
        scope: &'s Scope,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, H::Err>
    where
        H: JavaExceptionHandler,
        O: Into<JObject<'j>>,
        's: 'j,
    {
        let InitialisedJavaObjectMethod {
            signature,
            method_id,
            def,
        } = self;

        if signature.args.len() != args.len() {
            scope.fatal_error(JError::InvalidArgList(signature.as_ref().clone()));
        }

        let args = args.iter().map(|v| v.to_jni()).collect::<Vec<_>>();
        scope.call_method_unchecked(
            def,
            handler,
            object,
            *method_id,
            signature.ret.clone(),
            &args,
        )
    }
}

impl From<(&'static str, &'static str, &'static str)> for JavaObjectMethodDef {
    fn from(value: (&'static str, &'static str, &'static str)) -> Self {
        JavaObjectMethodDef {
            class: value.0,
            name: value.1,
            signature: value.2,
        }
    }
}

#[derive(Debug, Copy, Clone, Hash, PartialEq, Eq)]
pub struct JavaObjectMethodDef {
    class: &'static str,
    name: &'static str,
    signature: &'static str,
}

impl JavaObjectMethodDef {
    pub const fn new(
        class: &'static str,
        name: &'static str,
        signature: &'static str,
    ) -> JavaObjectMethodDef {
        JavaObjectMethodDef {
            class,
            name,
            signature,
        }
    }

    pub const fn class(&self) -> &str {
        self.class
    }

    pub const fn name(&self) -> &str {
        self.name
    }

    pub const fn signature(&self) -> &str {
        self.signature
    }

    pub fn initialise(&self, scope: &Scope) -> Result<InitialisedJavaObjectMethod, JError> {
        let JavaObjectMethodDef {
            class,
            name,
            signature,
        } = self;

        let method_id = scope.get_method_id(class, name, signature);
        let signature = TypeSignature::from_str(signature)?;

        Ok(InitialisedJavaObjectMethod {
            def: *self,
            signature: Arc::new(signature),
            method_id,
        })
    }
}

pub trait JavaObjectMethod<'j>: Debug {
    type Output;

    fn invoke<'s, H, O>(
        &self,
        handler: &H,
        scope: &'s Scope,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, H::Err>
    where
        H: JavaExceptionHandler,
        O: Into<JObject<'j>>,
        's: 'j;
}

pub trait JavaMethodExt<'j>: JavaObjectMethod<'j> {
    fn string(&self) -> MoldString<'_, Self>
    where
        Self: Sized,
    {
        MoldString { method: self }
    }

    fn null(&self) -> MoldNull<'_, Self>
    where
        Self: Sized,
    {
        MoldNull { method: self }
    }

    fn global_ref(&self) -> MoldGlobalRef<'_, Self>
    where
        Self: Sized,
    {
        MoldGlobalRef { method: self }
    }

    fn array<A>(&self) -> MoldArray<'_, Self, A>
    where
        Self: Sized,
        A: JavaArrayType,
    {
        MoldArray {
            method: self,
            _at: PhantomData::default(),
        }
    }

    fn l(&self) -> MoldObject<'_, Self>
    where
        Self: Sized,
    {
        MoldObject { method: self }
    }

    fn v(&self) -> MoldVoid<'_, Self>
    where
        Self: Sized,
    {
        MoldVoid { method: self }
    }

    fn z(&self) -> MoldBool<'_, Self>
    where
        Self: Sized,
    {
        MoldBool { method: self }
    }
}

impl<'j, M> JavaMethodExt<'j> for M where M: JavaObjectMethod<'j> {}

fn mold<M, F, O>(method: M, scope: &Scope, f: F) -> O
where
    F: FnOnce() -> Result<O, JError>,
    M: Debug,
{
    match f() {
        Ok(o) => o,
        Err(e) => scope.fatal_error(format!(
            "Unexpected return type: {}. Callee: {:?}",
            e, method
        )),
    }
}

#[derive(Debug)]
pub struct MoldString<'m, M> {
    method: &'m M,
}

impl<'j, 'm, M> JavaObjectMethod<'j> for MoldString<'m, M>
where
    M: JavaObjectMethod<'j, Output = JValue<'j>>,
{
    type Output = String;

    fn invoke<'s, H, O>(
        &self,
        handler: &H,
        scope: &'s Scope,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, H::Err>
    where
        H: JavaExceptionHandler,
        O: Into<JObject<'j>>,
        's: 'j,
    {
        let MoldString { method } = self;
        let value = method.l().invoke(handler, scope, object, args)?;
        Ok(scope.get_rust_string(JString::from(value)))
    }
}

#[derive(Debug)]
pub struct MoldVoid<'m, M> {
    method: &'m M,
}

impl<'j, 'm, M> JavaObjectMethod<'j> for MoldVoid<'m, M>
where
    M: JavaObjectMethod<'j, Output = JValue<'j>>,
{
    type Output = ();

    fn invoke<'s, H, O>(
        &self,
        handler: &H,
        scope: &'s Scope,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, H::Err>
    where
        H: JavaExceptionHandler,
        O: Into<JObject<'j>>,
        's: 'j,
    {
        let MoldVoid { method } = self;
        let val = method.invoke(handler, scope, object, args)?;
        mold(self, scope, || val.v());
        Ok(())
    }
}

#[derive(Debug)]
pub struct MoldNull<'m, M> {
    method: &'m M,
}

impl<'m, 'j, M> JavaObjectMethod<'j> for MoldNull<'m, M>
where
    M: JavaObjectMethod<'j, Output = JValue<'j>>,
{
    type Output = ();

    fn invoke<'s, H, O>(
        &self,
        handler: &H,
        scope: &'s Scope,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, H::Err>
    where
        H: JavaExceptionHandler,
        O: Into<JObject<'j>>,
        's: 'j,
    {
        let MoldNull { method } = self;
        let value = method.invoke(handler, scope, object, args)?;

        mold(self, scope, || match value.l() {
            Ok(ptr) if ptr.is_null() => Ok(()),
            Ok(ptr) => Err(WrongJValueType(
                "JObject != null",
                JValue::Object(ptr).type_name(),
            )),
            Err(e) => Err(e),
        });
        Ok(())
    }
}

#[derive(Debug)]
pub struct MoldObject<'m, M> {
    method: &'m M,
}

impl<'m, 'j, M> JavaObjectMethod<'j> for MoldObject<'m, M>
where
    M: JavaObjectMethod<'j, Output = JValue<'j>>,
{
    type Output = JObject<'j>;

    fn invoke<'s, H, O>(
        &self,
        handler: &H,
        scope: &'s Scope,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, H::Err>
    where
        H: JavaExceptionHandler,
        O: Into<JObject<'j>>,
        's: 'j,
    {
        let MoldObject { method } = self;
        let value = method.invoke(handler, scope, object, args)?;
        Ok(mold(self, scope, || value.l()))
    }
}

#[derive(Debug)]
pub struct MoldBool<'m, M> {
    method: &'m M,
}

impl<'m, 'j, M> JavaObjectMethod<'j> for MoldBool<'m, M>
where
    M: JavaObjectMethod<'j, Output = JValue<'j>>,
{
    type Output = bool;

    fn invoke<'s, H, O>(
        &self,
        handler: &H,
        scope: &'s Scope,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, H::Err>
    where
        H: JavaExceptionHandler,
        O: Into<JObject<'j>>,
        's: 'j,
    {
        let MoldBool { method } = self;
        let value = method.invoke(handler, scope, object, args)?;
        Ok(mold(self, scope, || value.z()))
    }
}

#[derive(Debug)]
pub struct MoldGlobalRef<'m, M> {
    method: &'m M,
}

impl<'m, 'j, M> JavaObjectMethod<'j> for MoldGlobalRef<'m, M>
where
    M: JavaObjectMethod<'j, Output = JObject<'j>>,
{
    type Output = GlobalRef;

    fn invoke<'s, H, O>(
        &self,
        handler: &H,
        scope: &'s Scope,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, H::Err>
    where
        H: JavaExceptionHandler,
        O: Into<JObject<'j>>,
        's: 'j,
    {
        let MoldGlobalRef { method } = self;
        let value = method.invoke(handler, scope, object, args)?;
        Ok(scope.new_global_ref(value))
    }
}

pub trait JavaArrayType: Debug {
    type ArrayType;

    fn mold(scope: &Scope, obj: JObject) -> Self::ArrayType;
}

#[derive(Debug)]
pub struct ByteArray;

impl JavaArrayType for ByteArray {
    type ArrayType = Vec<u8>;

    fn mold(scope: &Scope, obj: JObject) -> Self::ArrayType {
        scope.convert_byte_array(obj.into_raw())
    }
}

#[derive(Debug)]
pub struct MoldArray<'m, M, A> {
    method: &'m M,
    _at: PhantomData<A>,
}

impl<'m, 'j, M, A> JavaObjectMethod<'j> for MoldArray<'m, M, A>
where
    M: JavaObjectMethod<'j, Output = JObject<'j>>,
    A: JavaArrayType,
{
    type Output = A::ArrayType;

    fn invoke<'s, H, O>(
        &self,
        handler: &H,
        scope: &'s Scope,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, H::Err>
    where
        H: JavaExceptionHandler,
        O: Into<JObject<'j>>,
        's: 'j,
    {
        let MoldArray { method, .. } = self;
        let value = method.invoke(handler, scope, object, args)?;
        Ok(A::mold(scope, value))
    }
}
