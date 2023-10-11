use std::collections::HashMap;

use swim_api::meta::lane::LaneKind;

use bytebridge::ByteCodec;

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
    pub lane_idx: i32,
    pub lane_kind_repr: LaneKindRepr,
}

impl LaneSpec {
    pub fn new(is_transient: bool, lane_idx: i32, lane_kind_repr: LaneKindRepr) -> LaneSpec {
        LaneSpec {
            is_transient,
            lane_idx,
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
