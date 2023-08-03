use bytes::BytesMut;
use std::collections::HashMap;
use std::str::FromStr;
use std::sync::Arc;

use futures_util::future::BoxFuture;
use futures_util::FutureExt;
use swim_api::agent::{Agent, AgentConfig, AgentContext, AgentInitResult, AgentTask};
use swim_api::error::AgentInitError;
use swim_api::meta::lane::LaneKind;
use swim_utilities::routing::route_uri::RouteUri;

use bytebridge::{ByteCodec, FromBytesError};
use jvm_sys::vm::method::JavaObjectMethodDef;
use jvm_sys::vm::SharedVm;

struct AgentVTable {
    on_start: JavaObjectMethodDef,
}

#[derive(ByteCodec, Debug, Clone)]
pub struct PlaneSpec {
    pub name: String,
    // nodeUri -> spec
    pub agent_specs: HashMap<String, AgentSpec>,
}

impl PlaneSpec {
    pub fn new(name: String, agent_specs: HashMap<String, AgentSpec>) -> PlaneSpec {
        PlaneSpec { name, agent_specs }
    }
}

#[derive(ByteCodec, Debug, Clone)]
pub struct AgentSpec {
    pub name: String,
    // laneUri -> spec
    pub lane_specs: HashMap<String, LaneSpec>,
}

impl AgentSpec {
    pub fn new(name: String, lane_specs: HashMap<String, LaneSpec>) -> AgentSpec {
        AgentSpec { name, lane_specs }
    }
}

#[derive(ByteCodec, Debug, Clone)]
pub struct LaneSpec {
    pub is_transient: bool,
    pub lane_kind_repr: LaneKindRepr,
}

impl LaneSpec {
    pub fn new(is_transient: bool, lane_kind_repr: LaneKindRepr) -> LaneSpec {
        LaneSpec {
            is_transient,
            lane_kind_repr,
        }
    }
}

#[derive(ByteCodec, Debug, Copy, Clone)]
pub enum LaneKindRepr {
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

impl From<LaneKindRepr> for LaneKind {
    fn from(value: LaneKindRepr) -> Self {
        match value {
            LaneKindRepr::Action => LaneKind::Action,
            LaneKindRepr::Command => LaneKind::Command,
            LaneKindRepr::Demand => LaneKind::Demand,
            LaneKindRepr::DemandMap => LaneKind::DemandMap,
            LaneKindRepr::Map => LaneKind::Map,
            LaneKindRepr::JoinMap => LaneKind::JoinMap,
            LaneKindRepr::JoinValue => LaneKind::JoinValue,
            LaneKindRepr::Supply => LaneKind::Supply,
            LaneKindRepr::Spatial => LaneKind::Spatial,
            LaneKindRepr::Value => LaneKind::Value,
        }
    }
}

impl LaneKindRepr {
    pub fn map_like(&self) -> bool {
        matches!(
            self,
            LaneKindRepr::Map
                | LaneKindRepr::DemandMap
                | LaneKindRepr::JoinMap
                | LaneKindRepr::JoinValue
        )
    }
}
