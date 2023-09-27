#![allow(warnings)]

use futures_util::future::BoxFuture;
use futures_util::FutureExt;
use jni::objects::JObject;
use swim_server_app::{AgentExt, Server, ServerBuilder, ServerHandle};
use swim_utilities::routing::route_pattern::RoutePattern;

use jvm_sys::env::JavaEnv;

use crate::spec::PlaneSpec;

mod agent;
mod codec;
mod java_context;
pub mod macros;
pub mod spec;

pub use agent::{AgentFactory, FfiAgentDef};
pub use java_context::JavaAgentContext;

#[derive(Debug, Clone)]
pub struct FfiContext {
    env: JavaEnv,
}

impl FfiContext {
    pub fn new(env: JavaEnv) -> FfiContext {
        FfiContext { env }
    }
}

pub async fn run_server(
    env: JavaEnv,
    plane_obj: JObject<'_>,
    plane_spec: PlaneSpec,
) -> (ServerHandle, BoxFuture<'static, ()>) {
    let PlaneSpec { name, agent_specs } = plane_spec;

    let mut server = ServerBuilder::with_plane_name(name.as_str()).with_in_memory_store();

    let factory = env.with_env(|scope| AgentFactory::new(&env, scope.new_global_ref(plane_obj)));
    let ffi_context = FfiContext { env };

    for (uri, spec) in agent_specs {
        server = server.add_route(
            RoutePattern::parse_str(uri.as_str()).unwrap(),
            FfiAgentDef::new(ffi_context.clone(), spec, factory.clone()),
        );
    }

    println!("Spawning server");

    let (task, server_handle) = server.build().await.unwrap().run();
    let jh = tokio::spawn(task);
    println!("Spawned server");

    let handle = async move {
        jh.await.unwrap().unwrap();
    }
    .boxed();
    (server_handle, handle)
}
