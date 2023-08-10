// Copyright 2015-2022 Swim Inc.
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
use std::marker::PhantomData;
use std::sync::Arc;

/// An initialised Java Object Method mirror that contains a parsed type signature and a method
/// ID.
///
/// It is expected that the method ID remains consistent for the lifetime of the application; as
/// in the case in HotSpot VM's.
#[derive(Debug, Clone)]
pub struct InitialisedJavaObjectMethod {
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
        } = self;

        if signature.args.len() != args.len() {
            scope.fatal_error(JError::InvalidArgList(signature.as_ref().clone()));
        }

        let args = args.iter().map(|v| v.to_jni()).collect::<Vec<_>>();
        scope.call_method_unchecked(handler, object, *method_id, signature.ret.clone(), &args)
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
            signature: Arc::new(signature),
            method_id,
        })
    }
}

pub trait JavaObjectMethod<'j> {
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
    fn string<'m>(&'m self) -> MoldString<'m, Self>
    where
        Self: Sized,
    {
        MoldString { method: self }
    }

    fn null<'m>(&'m self) -> MoldNull<'m, Self>
    where
        Self: Sized,
    {
        MoldNull { method: self }
    }

    fn global_ref<'m>(&'m self) -> MoldGlobalRef<'m, Self>
    where
        Self: Sized,
    {
        MoldGlobalRef { method: self }
    }

    fn array<'m, A>(&'m self) -> MoldArray<'m, Self, A>
    where
        Self: Sized,
        A: JavaArrayType,
    {
        MoldArray {
            method: self,
            _at: PhantomData::default(),
        }
    }

    fn l<'m>(&'m self) -> MoldObject<'m, Self>
    where
        Self: Sized,
    {
        MoldObject { method: self }
    }

    fn v<'m>(&'m self) -> MoldVoid<'m, Self>
    where
        Self: Sized,
    {
        MoldVoid { method: self }
    }

    fn z<'m>(&'m self) -> MoldBool<'m, Self>
    where
        Self: Sized,
    {
        MoldBool { method: self }
    }
}

impl<'j, M> JavaMethodExt<'j> for M where M: JavaObjectMethod<'j> {}

fn mold<F, O>(scope: &Scope, f: F) -> O
where
    F: FnOnce() -> Result<O, JError>,
{
    match f() {
        Ok(o) => o,
        Err(e) => scope.fatal_error(format!("Unexpected return type: {}", e)),
    }
}

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
        Ok(mold(scope, || val.v()))
    }
}

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

        Ok(mold(scope, || match value.l() {
            Ok(ptr) if ptr.is_null() => Ok(()),
            Ok(ptr) => Err(WrongJValueType(
                "JObject != null",
                JValue::Object(ptr).type_name(),
            )),
            Err(e) => Err(e),
        }))
    }
}

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

        Ok(mold(scope, || match value.l() {
            Ok(ptr) if ptr.is_null() => Err(WrongJValueType(
                "JObject == null",
                JValue::Object(ptr).type_name(),
            )),
            Ok(ptr) => Ok(ptr),
            Err(e) => Err(e),
        }))
    }
}

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

        Ok(mold(scope, || value.z()))
    }
}

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

pub trait JavaArrayType {
    type ArrayType;

    fn mold(scope: &Scope, obj: JObject) -> Self::ArrayType;
}

pub struct ByteArray;

impl JavaArrayType for ByteArray {
    type ArrayType = Vec<u8>;

    fn mold(scope: &Scope, obj: JObject) -> Self::ArrayType {
        scope.convert_byte_array(obj.into_raw())
    }
}

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
