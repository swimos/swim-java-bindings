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

use bytes::BytesMut;
use client_runtime::Transport;
use futures_util::future::{ready, BoxFuture};
use futures_util::stream::Empty;
use futures_util::FutureExt;
use ratchet::{NegotiatedExtension, NoExt, Role, WebSocket, WebSocketConfig};
use std::cell::RefCell;
use std::collections::HashMap;
use std::io;
use std::io::ErrorKind;
use std::net::SocketAddr;
use std::sync::{Arc, Mutex};
use swim_runtime::error::{ConnectionError, IoError};
use swim_runtime::remote::net::dns::{BoxDnsResolver, DnsResolver};
use swim_runtime::remote::table::SchemeHostPort;
use swim_runtime::remote::{ExternalConnections, Listener, Scheme, SchemeSocketAddr};
use swim_runtime::ws::{WsConnections, WsOpenFuture};
use swim_utilities::algebra::non_zero_usize;
use tokio::io::{duplex, DuplexStream};

#[derive(Debug)]
struct Inner {
    addrs: HashMap<SchemeHostPort, SchemeSocketAddr>,
    sockets: HashMap<SocketAddr, DuplexStream>,
}

impl Inner {
    fn new<R, S>(resolver: R, sockets: S) -> Inner
    where
        R: IntoIterator<Item = (SchemeHostPort, SchemeSocketAddr)>,
        S: IntoIterator<Item = (SocketAddr, DuplexStream)>,
    {
        Inner {
            addrs: HashMap::from_iter(resolver),
            sockets: HashMap::from_iter(sockets),
        }
    }
}

#[derive(Debug, Clone)]
pub struct MockExternalConnections {
    inner: Arc<Mutex<Inner>>,
}

impl MockExternalConnections {
    fn new<R, S>(resolver: R, sockets: S) -> MockExternalConnections
    where
        R: IntoIterator<Item = (SchemeHostPort, SchemeSocketAddr)>,
        S: IntoIterator<Item = (SocketAddr, DuplexStream)>,
    {
        MockExternalConnections {
            inner: Arc::new(Mutex::new(Inner::new(resolver, sockets))),
        }
    }
}

pub struct MockListener;
impl Listener for MockListener {
    type Socket = DuplexStream;
    type AcceptStream = Empty<io::Result<(DuplexStream, SchemeSocketAddr)>>;

    fn into_stream(self) -> Self::AcceptStream {
        panic!("Unexpected listener invocation")
    }
}

impl ExternalConnections for MockExternalConnections {
    type Socket = DuplexStream;
    type ListenerType = MockListener;

    fn bind(
        &self,
        _addr: SocketAddr,
    ) -> BoxFuture<'static, io::Result<(SocketAddr, Self::ListenerType)>> {
        panic!("Unexpected bind invocation")
    }

    fn try_open(&self, addr: SocketAddr) -> BoxFuture<'static, io::Result<Self::Socket>> {
        let result = self
            .inner
            .lock()
            .unwrap()
            .sockets
            .remove(&addr)
            .ok_or(ErrorKind::NotFound.into());
        ready(result).boxed()
    }

    fn dns_resolver(&self) -> BoxDnsResolver {
        Box::new(self.clone())
    }

    fn lookup(
        &self,
        host: SchemeHostPort,
    ) -> BoxFuture<'static, io::Result<Vec<SchemeSocketAddr>>> {
        self.resolve(host).boxed()
    }
}

impl DnsResolver for MockExternalConnections {
    type ResolveFuture = BoxFuture<'static, io::Result<Vec<SchemeSocketAddr>>>;

    fn resolve(&self, host: SchemeHostPort) -> Self::ResolveFuture {
        let result = match self.inner.lock().unwrap().addrs.get(&host) {
            Some(sock) => Ok(vec![sock.clone()]),
            None => Err(io::ErrorKind::NotFound.into()),
        };
        ready(result).boxed()
    }
}

enum WsAction {
    Open,
    Fail(ConnectionError),
}

pub struct MockWs {
    states: HashMap<String, WsAction>,
}

impl MockWs {
    fn new<S>(states: S) -> MockWs
    where
        S: IntoIterator<Item = (String, WsAction)>,
    {
        MockWs {
            states: HashMap::from_iter(states),
        }
    }
}

impl WsConnections<DuplexStream> for MockWs {
    type Ext = NoExt;
    type Error = ConnectionError;

    fn open_connection(
        &self,
        socket: DuplexStream,
        addr: String,
    ) -> WsOpenFuture<DuplexStream, Self::Ext, Self::Error> {
        let result = match self.states.get(&addr) {
            Some(WsAction::Open) => Ok(WebSocket::from_upgraded(
                WebSocketConfig::default(),
                socket,
                NegotiatedExtension::from(NoExt),
                BytesMut::default(),
                Role::Client,
            )),
            Some(WsAction::Fail(e)) => Err(e.clone()),
            None => Err(ConnectionError::Io(IoError::new(ErrorKind::NotFound, None))),
        };
        ready(result).boxed()
    }

    fn accept_connection(
        &self,
        _socket: DuplexStream,
    ) -> WsOpenFuture<DuplexStream, Self::Ext, Self::Error> {
        panic!("Unexpected accept connection invocation")
    }
}

pub fn create_io() -> (Transport<MockExternalConnections, MockWs>, Server) {
    let peer = SchemeHostPort::new(Scheme::Ws, "127.0.0.1".to_string(), 9001);
    let sock: SocketAddr = "127.0.0.1:9001".parse().unwrap();
    let (client_stream, server_stream) = duplex(128);
    let ext = MockExternalConnections::new(
        [(
            peer.clone(),
            SchemeSocketAddr::new(Scheme::Ws, sock.clone()),
        )],
        [("127.0.0.1:9001".parse().unwrap(), client_stream)],
    );
    let ws = MockWs::new([("127.0.0.1".to_string(), WsAction::Open)]);
    let transport = Transport::new(ext, ws, non_zero_usize!(128));
    let server = Server::new(server_stream);
    (transport, server)
}

struct Server {
    buf: BytesMut,
    transport: WebSocket<DuplexStream, NoExt>,
}

impl Server {
    fn new(transport: DuplexStream) -> Server {
        Server {
            buf: BytesMut::new(),
            transport: WebSocket::from_upgraded(
                WebSocketConfig::default(),
                transport,
                NegotiatedExtension::from(NoExt),
                BytesMut::default(),
                Role::Server,
            ),
        }
    }

    fn lane_for<N, L>(&mut self, node: N, lane: L) -> Lane<'_>
    where
        N: ToString,
        L: ToString,
    {
        Lane {
            node: node.to_string(),
            lane: lane.to_string(),
            server: RefCell::new(self),
        }
    }
}
