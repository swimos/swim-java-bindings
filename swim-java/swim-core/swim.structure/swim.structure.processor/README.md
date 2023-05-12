# Form annotation processor

This project contains an annotation processor for deriving both a Recognizers and Writable implementation for a class
that has been annotated with `@AutoForm`.

Example usage:

```java

@AutoForm(subTypes = @AutoForm.Type(CommandMessage.class))
public static abstract class LaneAddressed extends Envelope {
  @AutoForm.Name("node")
  public String nodeUri;
  @AutoForm.Name("lane")
  public String laneUri;
  @AutoForm.Kind(FieldKind.Body)
  private Object body;

  public LaneAddressed() {

  }
}

@AutoForm
@AutoForm.Tag("command")
public static class CommandMessage extends LaneAddressed {
  public CommandMessage() {

  }
}
```

## Design

The `@AutoForm` annotation processor works by inspecting a type using
a [Model Inspector](./src/main/java/ai/swim/structure/processor/model/ModelInspector.java) and deriving a Model
representation of the type. During this model resolution, the type is validated against a set of criteria that ensures
that the type is of a correct layout;

- The type, if a class, contains a zero-arg, public constructor.
- Its tag is a valid Recon identifier.
- Each field has a suitable accessor; either public visibility, or a valid, public, getter and setter exists.
- Each field's type is resolvable.
- For types that contain subclasses, they must also be processed by the annotation processor and actually implement the
  superclass and be resolvable.
- For types that implement superclasses they must be resolvable.

If the Model Inspector does not reject the type element, then the derived model is stored in a map against its
respective [TypeMirror](https://docs.oracle.com/javase/8/docs/api/javax/lang/model/type/TypeMirror.html) and a
recognizer and/or writable is derived and written to
the [ProcessingEnvironment](https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/ProcessingEnvironment.html)'
s [Filer](https://docs.oracle.com/javase/8/docs/api/javax/annotation/processing/Filer.html)
using [JavaPoet](https://github.com/square/javapoet).

## Model Resolution

During the model resolution phase, classes have their fields inspected and the type of the field is resolved to a model
of one of the following types;

- A core type; boxed or unboxed primitive, String, Number, BigInteger, or a BigDecimal.
- An array type where the component type is resolvable.
- A java.util.List where the element type is resolvable.
- A java.util.Map where the key and value types are resolvable.
- Another type that is being processed by the annotation processor.
- Unresolved. If field's type references a type that is not annotated with `@AutoForm` then the field's model is set to
  unresolved and its corresponding type will be resolved at runtime, if it is possible.
- Untyped. A field that has an unbounded generic type parameter is set as untyped and type resolution is attempted at
  runtime. For a writer, this is performed using an object's class and resolution is attempted using the `WriterProxy`.
  For a recognizer, an attempt is made to infer a type from the contents of the read events.

If the field's type is another type to be processed by the annotation processor, then this model is inspected, stored
and set as the field's corresponding model. When this type is later encountered by the annotation processor it is
readily available to be written as a recognizer and/or writer if its `TypeMirror` is the same as the field's type; two
fields may reference the same type, but they reference different `TypeMirror`s due to their usage of type parameters and
in this case will result in two separate model resolution calls.

### Bounded types

Generic types that contain an upper or lower bound, the types are unraveled if each bound is a type that can be
resolved. For example:

```java

@AutoForm
public class UnerasedTypes {
  public List<? extends Map<? extends List<Integer>, ? super Number>> list;
}
```

The processor will unravel the list type to:

```java

@AutoForm
public class UnerasedTypes {
  public List<Map<List<Integer>, Number>> list;
}
```

### Inheritance Resolution

The form processor supports inheritance resolution for both classes and interface.

For example:

```java

@AutoForm(subtypes = {@AutoForm.Type(HostAddressed.class)})
public interface Addressed {

}

@AutoForm(subtypes = {@AutoForm.Type(NodeAddressed.class)})
public abstract class HostAddressed implements Addressed {
  public String host;
}

@AutoForm(subtypes = {@AutoForm.Type(LaneAddressed.class)})
public abstract class NodeAddressed extends HostAddressed {
  public String node;
}

@AutoForm
public class LaneAddressed extends NodeAddressed {
  public String lane;
}
```

During this resolution phase, a class is inspected to ensure that it is properly defined before its inheritance tree is
traversed; both its supertypes and subtypes if they have been declared.

#### Supertype resolution

Imagining a fresh run in the annotation processor, let's assume that we're processing the `LaneAddressed` class and it
is valid. A model is built up that contains the fields of:

| Name | Type   | Model               |
|------|--------|---------------------|
| lane | String | CoreTypeKind.String |

In order to inspect `NodeAddressed`'s supertypes we must inspect `HostAddressed`, which will trigger  `Addressed` to be
inspected and stored. As a class has both its subtypes and supertypes inspected, we must also signal to the inspector
the class that triggered the resolution and provide it with a fully resolved model in its place to prevent the callee
from being resolved; triggering an infinite resolution cycle. So, when we inspect `NodeAddressed` the `LaneAddressed`
model is provided and its subtype model is stored.

Following `NodeAddressed` being inspected, a model is returned that contains the following fields:

| Name | Type   | Model               |
|------|--------|---------------------|
| node | String | CoreTypeKind.String |
| host | String | CoreTypeKind.String |

Which are then merged into the `LaneAddressed` model:

| Name | Type   | Model               |
|------|--------|---------------------|
| lane | String | CoreTypeKind.String |
| node | String | CoreTypeKind.String |
| host | String | CoreTypeKind.String |

For concrete classes that contain subtypes, a writer and recognizer is written that supports both its concrete class and its subtypes.

## Model transformations
Once a model has been derived, a recognizer and/or writable is derived and written out to the classpath. This is done using a [TypeInitializer](./src/main/java/ai/swim/structure/processor/model/TypeInitializer.java) interface that visits models and their fields. These type initializers transpose a model to an initialized type (I.e, an Integer to an IntegerRecognizer) and a code block for instantiating it (ScalarRecognizers.INTEGER).

Both the derived recognizers and structural writable implementations work similarly to the Rust implementation except for runtime model resolution for types that may not have been available at compile time (a type with a manual form implementation) and for writables that contain type parameters (these are resolved at runtime and stored in the class so they are available for the next invocation).
