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

use std::collections::hash_map::Entry;
use std::collections::HashMap;
use std::marker::PhantomData;
use std::sync::Arc;

use jni::errors::Error::WrongJValueType;
use jni::errors::{Error as JError, Error};
use jni::objects::{GlobalRef, JMethodID, JObject, JString, JValue};
use jni::signature::TypeSignature;
use jni::sys::jobject;
use jni::JNIEnv;
use parking_lot::Mutex;

use crate::vm::utils::new_global_ref;
use crate::vm::with_local_frame_null;

/// An initialised Java Object Method mirror that contains a parsed type signature and a method
/// ID.
///
/// It is expected that the method ID remains consistent for the lifetime of the application; as
/// in the case in HotSpot VM's.
#[derive(Debug, Clone)]
pub struct InitialisedJavaObjectMethod {
    signature: TypeSignature,
    method_id: JMethodID,
}

impl InitialisedJavaObjectMethod {
    pub fn signature(&self) -> &TypeSignature {
        &self.signature
    }

    pub fn method_id(&self) -> JMethodID {
        self.method_id
    }
}

impl<'j> JavaObjectMethod<'j> for InitialisedJavaObjectMethod {
    type Output = JValue<'j>;

    fn invoke<O>(
        &self,
        env: &'j JNIEnv,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, JError>
    where
        O: Into<JObject<'j>>,
    {
        let InitialisedJavaObjectMethod {
            signature,
            method_id,
        } = self;

        if signature.args.len() != args.len() {
            return Err(JError::InvalidArgList(signature.clone()));
        }

        let args = args.iter().map(|v| v.to_jni()).collect::<Vec<_>>();
        env.call_method_unchecked(object, *method_id, signature.ret.clone(), &args)
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

    pub fn initialise(&self, env: &JNIEnv) -> Result<InitialisedJavaObjectMethod, JError> {
        let JavaObjectMethodDef {
            class,
            name,
            signature,
        } = self;

        let method_id = env.get_method_id(class, name, signature)?;
        let signature = TypeSignature::from_str(signature)?;

        Ok(InitialisedJavaObjectMethod {
            signature,
            method_id,
        })
    }
}

pub trait JavaObjectMethod<'j> {
    type Output;

    fn invoke<O>(
        &self,
        env: &'j JNIEnv,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, JError>
    where
        O: Into<JObject<'j>>;
}

pub trait JavaMethodExt<'j>: JavaObjectMethod<'j> {
    fn string<'m>(&'m self) -> MoldString<'m, Self>
    where
        Self: Sized,
    {
        MoldString { method: self }
    }

    fn void<'m>(&'m self) -> MoldVoid<'m, Self>
    where
        Self: Sized,
    {
        MoldVoid { method: self }
    }

    fn null<'m>(&'m self) -> MoldNull<'m, Self>
    where
        Self: Sized,
    {
        MoldNull { method: self }
    }

    fn object<'m>(&'m self) -> MoldObject<'m, Self>
    where
        Self: Sized,
    {
        MoldObject { method: self }
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
}

impl<'j, M> JavaMethodExt<'j> for M where M: JavaObjectMethod<'j> {}

pub struct MoldString<'m, M> {
    method: &'m M,
}

impl<'j, 'm, M> JavaObjectMethod<'j> for MoldString<'m, M>
where
    M: JavaObjectMethod<'j, Output = JValue<'j>>,
{
    type Output = String;

    fn invoke<O>(
        &self,
        env: &'j JNIEnv,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, JError>
    where
        O: Into<JObject<'j>>,
    {
        let MoldString { method } = self;
        let value = method.invoke(env, object, args)?;
        let str = env.get_string(JString::from(value.l()?))?;
        Ok(str
            .to_str()
            .expect("Failed to convert Java String to Rust String")
            .to_string())
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

    fn invoke<O>(
        &self,
        env: &'j JNIEnv,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, JError>
    where
        O: Into<JObject<'j>>,
    {
        let MoldVoid { method } = self;
        method.invoke(env, object, args)?.v()
    }
}

pub struct Mold<'m, 'j: 'm, M, T> {
    method: &'m M,
    func: fn(&JNIEnv<'j>, JValue<'j>) -> Result<T, JError>,
}

impl<'m, 'j, M, T> JavaObjectMethod<'j> for Mold<'m, 'j, M, T>
where
    M: JavaObjectMethod<'j, Output = JValue<'j>>,
    T: 'j,
{
    type Output = T;

    fn invoke<O>(
        &self,
        env: &'j JNIEnv,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, JError>
    where
        O: Into<JObject<'j>>,
    {
        let Mold { method, func } = self;
        let value = method.invoke(env, object, args)?;
        (func)(env, value)
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

    fn invoke<O>(
        &self,
        env: &'j JNIEnv,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, JError>
    where
        O: Into<JObject<'j>>,
    {
        let MoldNull { method } = self;
        let value = method.invoke(env, object, args)?;

        match value.l() {
            Ok(ptr) if ptr.is_null() => Ok(()),
            Ok(ptr) => Err(WrongJValueType(
                "JObject != null",
                JValue::Object(ptr).type_name(),
            )),
            Err(e) => Err(e),
        }
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

    fn invoke<O>(
        &self,
        env: &'j JNIEnv,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, JError>
    where
        O: Into<JObject<'j>>,
    {
        let MoldObject { method } = self;
        let value = method.invoke(env, object, args)?;

        match value.l() {
            Ok(ptr) if ptr.is_null() => Err(WrongJValueType(
                "JObject == null",
                JValue::Object(ptr).type_name(),
            )),
            Ok(ptr) => Ok(ptr),
            Err(e) => Err(e),
        }
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

    fn invoke<O>(
        &self,
        env: &'j JNIEnv,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, JError>
    where
        O: Into<JObject<'j>>,
    {
        let MoldGlobalRef { method } = self;
        let value = method.invoke(env, object, args)?;
        env.new_global_ref(value)
    }
}

pub trait JavaArrayType {
    type ArrayType;

    fn mold(env: &JNIEnv, obj: JObject) -> Result<Self::ArrayType, JError>;
}

pub struct ByteArray;

impl JavaArrayType for ByteArray {
    type ArrayType = Vec<u8>;

    fn mold(env: &JNIEnv, obj: JObject) -> Result<Self::ArrayType, Error> {
        env.convert_byte_array(obj.into_raw())
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

    fn invoke<O>(
        &self,
        env: &'j JNIEnv,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, JError>
    where
        O: Into<JObject<'j>>,
    {
        let MoldArray { method, .. } = self;
        let value = method.invoke(env, object, args)?;
        A::mold(env, value)
    }
}

/// A Java callback that invokes an optionally-null functional interface.
pub struct JavaCallback {
    /// A pointer to the functional interface.
    ptr: Option<GlobalRef>,
    /// The method definition/signature.
    def: MethodDefinition,
}

#[derive(Debug)]
pub enum MethodDefinition {
    Unit(JavaObjectMethodDef),
    Init(InitialisedJavaObjectMethod),
}

impl JavaCallback {
    pub fn for_method(
        env: &JNIEnv,
        ptr: jobject,
        method: JavaObjectMethodDef,
    ) -> Result<JavaCallback, Error> {
        Ok(JavaCallback {
            ptr: new_global_ref(env, ptr)?,
            def: MethodDefinition::Unit(method),
        })
    }

    pub fn execute<'j, F>(&mut self, env: &'j JNIEnv, f: F) -> Result<(), Error>
    where
        F: FnOnce(&mut InitialisedJavaObjectMethod, JObject) -> Result<(), Error>,
    {
        let JavaCallback { ptr, def } = self;
        match ptr {
            Some(ptr) => match def {
                MethodDefinition::Unit(inner) => {
                    let mut initialised = inner.initialise(env)?;
                    let result =
                        with_local_frame_null(env, None, || f(&mut initialised, ptr.as_obj()));
                    *def = MethodDefinition::Init(initialised);
                    result
                }
                MethodDefinition::Init(inner) => {
                    with_local_frame_null(env, None, || f(inner, ptr.as_obj()))
                }
            },
            None => Ok(()),
        }
    }
}

pub fn void_fn(env: &JNIEnv, ptr: &mut JavaCallback, args: &[JValue<'_>]) -> Result<(), Error> {
    ptr.execute(env, |init, obj| init.void().invoke(env, obj, args))
}

#[derive(Debug, Clone, Default)]
pub struct MethodResolver {
    inner: Arc<Mutex<ResolverInner>>,
}

impl MethodResolver {
    pub fn resolve(
        &self,
        env: &JNIEnv,
        def: impl Into<JavaObjectMethodDef>,
    ) -> InitialisedJavaObjectMethod {
        let guard = &mut *self.inner.lock();
        guard.resolve(env, def.into())
    }
}

#[derive(Debug, Default)]
struct ResolverInner {
    resolved: HashMap<JavaObjectMethodDef, InitialisedJavaObjectMethod>,
}

impl ResolverInner {
    pub fn resolve(
        &mut self,
        env: &JNIEnv,
        def: JavaObjectMethodDef,
    ) -> InitialisedJavaObjectMethod {
        match self.resolved.entry(def) {
            Entry::Occupied(entry) => entry.get().clone(),
            Entry::Vacant(entry) => {
                let initialised = def
                    .initialise(env)
                    .expect("Failed to initialise Java Object Method Definition");
                entry.insert(initialised).clone()
            }
        }
    }
}
