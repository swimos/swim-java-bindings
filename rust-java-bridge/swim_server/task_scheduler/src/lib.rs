#[cfg(test)]
mod tests;

use futures::Stream;
use std::fmt::{Debug, Formatter};
use std::future::Future;
use std::pin::Pin;
use std::task::{ready, Context, Poll};
use std::time::Duration;

use pin_project::pin_project;
use tokio_util::time::delay_queue::{Expired, Key};
use tokio_util::time::DelayQueue;

pub trait Adapter<T> {
    type Output: Future<Output = Result<(), Self::Error>> + Unpin;
    type Error;

    fn adapt(&mut self, item: &T) -> Self::Output;
}

pub struct TaskDef<T> {
    def: T,
    strategy: RunStrategy,
}

impl<T> TaskDef<T> {
    fn of(def: T, strategy: RunStrategy) -> TaskDef<T> {
        TaskDef { def, strategy }
    }
}

pub struct TaskScheduler<T> {
    queue: DelayQueue<TaskDef<T>>,
}

impl<T> Default for TaskScheduler<T> {
    fn default() -> Self {
        TaskScheduler {
            queue: DelayQueue::default(),
        }
    }
}

impl<T> TaskScheduler<T> {
    fn is_empty(&self) -> bool {
        self.queue.is_empty()
    }
}

impl<T> Future for TaskScheduler<T> {
    type Output = Option<Expired<TaskDef<T>>>;

    fn poll(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        self.queue.poll_expired(cx)
    }
}

pub enum RunStrategyDef {
    Once {
        after: Duration,
    },
    Infinite {
        interval: Duration,
    },
    Schedule {
        run_count: usize,
        interval: Duration,
    },
}

#[derive(Debug, Copy, Clone)]
enum RunStrategy {
    Infinite {
        interval: Duration,
    },
    Schedule {
        remaining: usize,
        interval: Duration,
    },
}

impl RunStrategy {
    fn interval(&self) -> Duration {
        match self {
            RunStrategy::Infinite { interval } => *interval,
            RunStrategy::Schedule { interval, .. } => *interval,
        }
    }

    fn decrement(self) -> Option<RunStrategy> {
        match self {
            s @ RunStrategy::Infinite { .. } => Some(s),
            RunStrategy::Schedule {
                remaining,
                interval,
            } => remaining
                .checked_sub(1)
                .map(|remaining| RunStrategy::Schedule {
                    remaining,
                    interval,
                }),
        }
    }
}

impl From<RunStrategyDef> for RunStrategy {
    fn from(value: RunStrategyDef) -> RunStrategy {
        match value {
            RunStrategyDef::Once { after } => RunStrategy::Schedule {
                remaining: 1,
                interval: after,
            },
            RunStrategyDef::Infinite { interval } => RunStrategy::Infinite { interval },
            RunStrategyDef::Schedule {
                run_count,
                interval,
            } => RunStrategy::Schedule {
                remaining: run_count,
                interval,
            },
        }
    }
}

struct TaskHeader<T> {
    task: T,
    key: Key,
    strategy: RunStrategy,
}

impl<T> Debug for TaskHeader<T> {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        f.debug_struct("TaskHeader")
            .field("task", &"..")
            .field("key", &self.key)
            .field("strategy", &self.key)
            .finish()
    }
}

#[pin_project]
struct ExecutingTask<D, T> {
    task_def: Option<D>,
    key: Key,
    #[pin]
    future: T,
    strategy: RunStrategy,
}

impl<D, T> ExecutingTask<D, T> {
    pub fn new(task_def: D, key: Key, future: T, strategy: RunStrategy) -> ExecutingTask<D, T> {
        ExecutingTask {
            task_def: Some(task_def),
            key,
            future,
            strategy,
        }
    }
}

impl<D, T> Debug for ExecutingTask<D, T> {
    fn fmt(&self, f: &mut Formatter<'_>) -> std::fmt::Result {
        let ExecutingTask {
            task_def,
            key,
            strategy,
            ..
        } = self;
        let def_fmt = match task_def {
            Some(_) => "Some(..)",
            None => "None",
        };
        f.debug_struct("ExecutingTask")
            .field("task_def", &def_fmt)
            .field("key", key)
            .field("future", &"..")
            .field("strategy", strategy)
            .finish()
    }
}

impl<D, T, E> Future for ExecutingTask<D, T>
where
    T: Future<Output = Result<(), E>>,
{
    type Output = Result<TaskHeader<D>, E>;

    fn poll(self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Self::Output> {
        let projected = self.project();
        match ready!(projected.future.poll(cx)) {
            Ok(()) => Poll::Ready(Ok(TaskHeader {
                task: projected
                    .task_def
                    .take()
                    .expect("Task polled after completion"),
                key: *projected.key,
                strategy: *projected.strategy,
            })),
            Err(e) => Poll::Ready(Err(e)),
        }
    }
}

#[pin_project]
pub struct TaskExecutor<T, A>
where
    A: Adapter<T>,
{
    #[pin]
    scheduler: TaskScheduler<T>,
    #[pin]
    current: Option<ExecutingTask<T, A::Output>>,
    adapter: A,
}

impl<T, A> TaskExecutor<T, A>
where
    A: Adapter<T>,
{
    pub fn new(adapter: A) -> TaskExecutor<T, A> {
        TaskExecutor {
            scheduler: TaskScheduler::default(),
            current: None,
            adapter,
        }
    }

    fn requeue(&mut self, task: TaskHeader<T>) {
        let TaskHeader { task, strategy, .. } = task;
        if let Some(strategy) = strategy.decrement() {
            let interval = strategy.interval();
            self.scheduler
                .queue
                .insert(TaskDef::of(task, strategy), interval);
        }
    }

    pub fn push_task(&mut self, def: RunStrategyDef, task: T) -> Key {
        let strategy: RunStrategy = def.into();
        let interval = strategy.interval();
        self.scheduler
            .queue
            .insert(TaskDef::of(task, strategy), interval)
    }

    pub fn is_empty(&self) -> bool {
        self.scheduler.is_empty()
    }
}

impl<T, A> Stream for TaskExecutor<T, A>
where
    A: Adapter<T>,
{
    type Item = Result<Key, A::Error>;

    fn poll_next(mut self: Pin<&mut Self>, cx: &mut Context<'_>) -> Poll<Option<Self::Item>> {
        loop {
            let projected = self.as_mut().project();
            let mut current: Pin<&mut Option<ExecutingTask<T, A::Output>>> = projected.current;

            match current.as_mut().as_pin_mut() {
                Some(pin) => {
                    return match ready!(pin.poll(cx)) {
                        Ok(header) => {
                            let key = header.key;
                            current.set(None);
                            self.requeue(header);
                            Poll::Ready(Some(Ok(key)))
                        }
                        Err(e) => Poll::Ready(Some(Err(e))),
                    }
                }
                None => match ready!(projected.scheduler.poll(cx)) {
                    Some(expired) => {
                        let key = expired.key();
                        let task = expired.into_inner();
                        let future = projected.adapter.adapt(&task.def);
                        current.set(Some(ExecutingTask::new(
                            task.def,
                            key,
                            future,
                            task.strategy,
                        )));
                    }
                    None => {
                        return Poll::Pending;
                    }
                },
            }
        }
    }
}
