pub mod agent;
pub mod codec;

use futures_util::future::BoxFuture;
use futures_util::FutureExt;
use jni::objects::JObject;
use swim_server_app::{Server, ServerBuilder, ServerHandle};
use swim_utilities::routing::route_pattern::RoutePattern;

use crate::agent::foreign::JavaAgentFactory;
use crate::agent::spec::PlaneSpec;
pub use crate::agent::JavaGuestAgent;
use crate::agent::JavaGuestConfig;
use jvm_sys::env::JavaEnv;

pub mod macros;

/// Constructs and runs a new Swim Server.
///
/// # Arguments
/// - `env` - Java environment.
/// - `server_obj` - JObject referencing an ai/swim/server/AbstractSwimServerBuilder instance. Used
/// for instantiating new agents.
/// - `plane_spec` - Specification of the plane. Detailing the agents and their respective lanes.
/// All agents contained in the spec must be available in the `server_obj`'s factory.
///
/// # Returns
/// `0`: a handle to the server. Used for accessing its port and shutting down the server.
/// `1`: the server's task future.
pub async fn run_server(
    env: JavaEnv,
    server_obj: JObject<'_>,
    plane_spec: PlaneSpec,
) -> (ServerHandle, BoxFuture<'static, ()>) {
    let PlaneSpec { name, agent_specs } = plane_spec;
    let mut server = ServerBuilder::with_plane_name(name.as_str()).with_in_memory_store();

    let factory =
        env.with_env(|scope| JavaAgentFactory::new(&env, scope.new_global_ref(plane_obj)));

    for (uri, spec) in agent_specs {
        server = server.add_route(
            RoutePattern::parse_str(uri.as_str()).unwrap(),
            JavaGuestAgent::new(
                env.clone(),
                spec,
                factory.clone(),
                JavaGuestConfig::java_default(),
            ),
        );
    }

    let (task, server_handle) = server.build().await.unwrap().run();
    let jh = tokio::spawn(task);

    let handle = async move {
        jh.await.unwrap().unwrap();
    }
    .boxed();
    (server_handle, handle)
}
