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

use jni::errors::Error as JError;
use jni::errors::Error::WrongJValueType;
use jni::objects::{JMethodID, JObject, JString, JValue};
use jni::signature::TypeSignature;
use jni::JNIEnv;

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
        &mut self,
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
            ..
        } = self;

        if signature.args.len() != args.len() {
            return Err(JError::InvalidArgList(signature.clone()));
        }

        let args = args.iter().map(|v| v.to_jni()).collect::<Vec<_>>();
        env.call_method_unchecked(
            object,
            JMethodID::from(*method_id),
            signature.ret.clone(),
            &args,
        )
    }
}

#[derive(Debug, Copy, Clone)]
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
    type Output: 'j;
    fn invoke<O>(
        &mut self,
        env: &'j JNIEnv,
        object: O,
        args: &[JValue<'j>],
    ) -> Result<Self::Output, JError>
    where
        O: Into<JObject<'j>>;
}

pub trait JavaMethodExt<'j>: JavaObjectMethod<'j> {
    fn string<'m>(&'m mut self) -> Mold<'m, 'j, Self, String>
    where
        Self: Sized,
    {
        fn mold<'j>(env: &JNIEnv<'j>, value: JValue<'j>) -> Result<String, JError> {
            let str = env.get_string(JString::from(value.l()?))?;
            Ok(str
                .to_str()
                .expect("Failed to convert Java String to Rust String")
                .to_string())
        }
        Mold {
            method: self,
            func: mold,
        }
    }

    fn void<'m>(&'m mut self) -> Mold<'m, 'j, Self, ()>
    where
        Self: Sized,
    {
        fn mold<'j>(_env: &JNIEnv<'j>, value: JValue<'j>) -> Result<(), JError> {
            value.v()
        }

        Mold {
            method: self,
            func: mold,
        }
    }

    fn null<'m>(&'m mut self) -> Mold<'m, 'j, Self, ()>
    where
        Self: Sized,
    {
        fn mold<'j>(_env: &JNIEnv<'j>, value: JValue<'j>) -> Result<(), JError> {
            match value.l() {
                Ok(ptr) if ptr.is_null() => Ok(()),
                Ok(ptr) => Err(WrongJValueType(
                    "JObject != null",
                    JValue::Object(ptr).type_name(),
                )),
                Err(e) => Err(e),
            }
        }

        Mold {
            method: self,
            func: mold,
        }
    }
}

pub struct Mold<'m, 'j: 'm, M, T> {
    method: &'m mut M,
    func: fn(&JNIEnv<'j>, JValue<'j>) -> Result<T, JError>,
}

impl<'m, 'j, M, T> JavaObjectMethod<'j> for Mold<'m, 'j, M, T>
where
    M: JavaObjectMethod<'j, Output = JValue<'j>>,
    T: 'j,
{
    type Output = T;

    fn invoke<O>(
        &mut self,
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

impl<'j, M> JavaMethodExt<'j> for M where M: JavaObjectMethod<'j> {}
