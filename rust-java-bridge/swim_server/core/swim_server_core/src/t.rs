use futures_util::StreamExt;
use jni::objects::GlobalRef;
use std::mem::size_of;
use std::time::Duration;

use tokio_util::time::DelayQueue;

struct JavaTask {}

struct JavaTaskScheduler {
    schedule: DelayQueue<JavaTask>,
}

#[derive(Debug)]
enum Step {
    Oneshot,
    Schedule(usize, Duration),
    Infinite(Duration),
}

const DELAY: Duration = Duration::from_secs(1);

#[tokio::test]
async fn t() {
    // let mut queue = DelayQueue::new();

    println!("{}", size_of::<DelayQueue<u128>>());
    //
    // // run_after; execute F after period P
    // let key = queue.insert(Step::Oneshot, DELAY);
    //
    // println!("{:?}", key);
    //
    // // schedule; same as suspend but the task is rerun after it is executed
    // let key = queue.insert(Step::Infinite(DELAY), DELAY);
    //
    // println!("{:?}", key);
    //
    // println!("{:?}", queue.next().await);
    // println!("{:?}", queue.next().await);
    //
    // let key = queue.insert(Step::Oneshot, DELAY);
    // println!("{:?}", key);
    //
    // let key = queue.insert(Step::Oneshot, DELAY);
    //
    // println!("{:?}", key);
    //
    // println!("{:?}", queue.next().await);
}
