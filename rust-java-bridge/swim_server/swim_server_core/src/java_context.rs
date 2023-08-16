use tokio::sync::mpsc;

pub struct JavaAgentContext {
    tx: mpsc::Sender<()>,
}

impl JavaAgentContext {
    pub fn new(tx: mpsc::Sender<()>) -> Self {
        Self { tx }
    }
}
