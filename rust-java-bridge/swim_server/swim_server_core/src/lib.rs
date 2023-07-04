mod agent;
pub mod macros;

type DispatchContinuation = ();
type DispatchError = ();
type Message = ();

// fn dispatch(
//     env: &JNIEnv,
//     agent_obj: impl AsRef<JObject>,
//     message: Message,
// ) -> Result<DispatchContinuation, DispatchError> {
//     unimplemented!()
// }
