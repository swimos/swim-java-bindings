use std::collections::HashMap;
use std::str::FromStr;

use futures_util::future::BoxFuture;
use swim_api::agent::{Agent, AgentConfig, AgentContext, AgentInitResult};
use swim_utilities::routing::route_uri::RouteUri;

use bytebridge::ByteCodec;

#[derive(ByteCodec, Debug, Clone)]
pub struct AgentSpec {
    agent_uri: String,
    lane_specs: HashMap<String, LaneSpec>,
}

#[derive(ByteCodec, Debug, Clone)]
pub struct LaneSpec {
    is_transient: bool,
    lane_kind: LaneKind,
}

#[derive(Debug, Copy, Clone)]
enum LaneKind {
    Action,
    Command,
    Demand,
    DemandMap,
    Map,
    JoinMap,
    JoinValue,
    Supply,
    Spatial,
    Value,
}

#[derive(Debug)]
pub struct UnknownLaneKind(String);

impl FromStr for LaneKind {
    type Err = UnknownLaneKind;

    fn from_str(s: &str) -> Result<Self, Self::Err> {
        match s {
            "Action" => Ok(LaneKind::Action),
            "Command" => Ok(LaneKind::Command),
            "Demand" => Ok(LaneKind::Demand),
            "DemandMap" => Ok(LaneKind::DemandMap),
            "Map" => Ok(LaneKind::Map),
            "JoinMap" => Ok(LaneKind::JoinMap),
            "JoinValue" => Ok(LaneKind::JoinValue),
            "Supply" => Ok(LaneKind::Supply),
            "Spatial" => Ok(LaneKind::Spatial),
            "Value" => Ok(LaneKind::Value),
            s => Err(UnknownLaneKind(s.to_string())),
        }
    }
}

#[derive(Debug, Clone)]
pub struct FfiAgent {
    spec: AgentSpec,
}

impl Agent for FfiAgent {
    fn run(
        &self,
        route: RouteUri,
        route_params: HashMap<String, String>,
        config: AgentConfig,
        context: Box<dyn AgentContext + Send>,
    ) -> BoxFuture<'static, AgentInitResult> {
        todo!()
    }
}
