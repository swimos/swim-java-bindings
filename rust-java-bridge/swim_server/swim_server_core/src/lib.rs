pub mod agent;
pub mod codec;

use futures_util::future::BoxFuture;
use futures_util::FutureExt;
use jni::objects::JObject;
use swim_server_app::{Server, ServerBuilder, ServerHandle};
use swim_utilities::routing::route_pattern::RoutePattern;

use crate::agent::foreign::JavaAgentFactory;
use crate::agent::spec::PlaneSpec;
pub use crate::agent::FfiAgentDef;
use jvm_sys::env::JavaEnv;

pub mod macros;

pub async fn run_server(
    env: JavaEnv,
    plane_obj: JObject<'_>,
    plane_spec: PlaneSpec,
) -> (ServerHandle, BoxFuture<'static, ()>) {
    let PlaneSpec { name, agent_specs } = plane_spec;
    let mut server = ServerBuilder::with_plane_name(name.as_str()).with_in_memory_store();

    let factory =
        env.with_env(|scope| JavaAgentFactory::new(&env, scope.new_global_ref(plane_obj)));

    for (uri, spec) in agent_specs {
        server = server.add_route(
            RoutePattern::parse_str(uri.as_str()).unwrap(),
            FfiAgentDef::new(env.clone(), spec, factory.clone()),
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
