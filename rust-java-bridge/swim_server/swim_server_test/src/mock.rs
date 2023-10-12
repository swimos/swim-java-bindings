use std::time::Duration;

use bytes::BytesMut;
use jni::objects::JObject;
use jni::sys::{jbyteArray, jobject};
use rand::prelude::StdRng;
use rand::{Rng, SeedableRng};
use ratchet::deflate::DeflateExtProvider;
use ratchet::{Message, ProtocolRegistry, WebSocketConfig};
use swim_server_app::ServerHandle;
use tokio::join;
use tokio::net::TcpStream;

use jvm_sys::bridge::JniByteCodec;
use jvm_sys::env::JavaEnv;
use jvm_sys::null_pointer_check_abort;
use swim_server_core::agent::spec::PlaneSpec;
use swim_server_core::{run_server, server_fn};

server_fn! {
    TestSwimServer_runNative(
        env,
        _class,
        config: jbyteArray,
        plane_obj: jobject
    )  {
        null_pointer_check_abort!(env, config);

        let env = JavaEnv::new(env);

        let spec = match PlaneSpec::try_from_jbyte_array::<()>(&env, config) {
            Ok(spec) => spec,
            Err(e) => panic!("{:?}", e)
        };

        let task = async move {
            let ( handle, task) = run_server(env, unsafe { JObject::from_raw(plane_obj) }, spec).await;
            let server_task = tokio::spawn(task);

            // println!("{}",handle.bound_addr().await.unwrap());
            // server_task.await.unwrap();
            let gen_task = tokio::spawn(gen_task(handle));

            let (_a,_b) = join!(server_task, gen_task);
        };
        tokio::runtime::Builder::new_multi_thread()
            .enable_all()
            .build()
            .unwrap()
            .block_on(task);
    }
}

async fn gen_task(mut handle: ServerHandle) {
    println!("Spaned gen task");
    let addr = handle.bound_addr().await.unwrap();

    println!("{}", addr.ip());

    let url = format!("127.0.0.1:{}", addr.port());
    println!("{}", url);
    let stream = TcpStream::connect(url).await.unwrap();
    stream.set_nodelay(true).unwrap();

    let mut socket = ratchet::subscribe_with(
        WebSocketConfig::default(),
        stream,
        format!("warp://localhost:{}/", addr.port()),
        &DeflateExtProvider::default(),
        ProtocolRegistry::default(),
    )
    .await
    .unwrap()
    .websocket;

    let mut buf = BytesMut::new();

    socket
        .write_text("@link(node:nodeUri, lane:laneUri)")
        .await
        .unwrap();

    let msg = socket.read(&mut buf).await.unwrap();
    assert_eq!(msg, Message::Text);

    assert_eq!(
        std::str::from_utf8(buf.as_ref()).unwrap(),
        "@linked(node:nodeUri,lane:laneUri)"
    );

    let mut rng = StdRng::from_entropy();

    loop {
        tokio::time::sleep(Duration::from_millis(500)).await;

        let num: i32 = rng.gen();
        socket
            .write_text(format!("@command(node:nodeUri,lane:laneUri){}", num))
            .await
            .unwrap();
    }
}
