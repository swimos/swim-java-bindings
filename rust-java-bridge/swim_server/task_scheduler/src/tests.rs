use futures_util::future::MapErr;
use futures_util::{StreamExt, TryFutureExt};
use std::future::{ready, Ready};
use std::time::Duration;
use tokio::task::{JoinError, JoinHandle};

use crate::{Adapter, RunStrategyDef, TaskExecutor};

struct SpawnBlockingAdapter;

impl Adapter<()> for SpawnBlockingAdapter {
    type Output = MapErr<JoinHandle<()>, Box<dyn FnOnce(JoinError) -> ()>>;
    type Error = ();

    fn adapt(&mut self, _item: &()) -> Self::Output {
        let r = tokio::task::spawn_blocking(|| ());
        r.map_err(Box::new(|_| ()))
    }
}

struct UnitAdapter;

impl Adapter<()> for UnitAdapter {
    type Output = Ready<Result<(), ()>>;
    type Error = ();

    fn adapt(&mut self, _item: &()) -> Self::Output {
        ready(Ok(()))
    }
}

#[tokio::test]
async fn t() {
    let mut executor = TaskExecutor::new(UnitAdapter);
    let key = executor.push_task(
        RunStrategyDef::Schedule {
            run_count: 3,
            interval: Duration::from_millis(1),
        },
        (),
    );

    assert_eq!(Some(Ok(key)), executor.next().await);
    assert_eq!(Some(Ok(key)), executor.next().await);
    assert_eq!(Some(Ok(key)), executor.next().await);
    assert_eq!(None, executor.next().await);
}
