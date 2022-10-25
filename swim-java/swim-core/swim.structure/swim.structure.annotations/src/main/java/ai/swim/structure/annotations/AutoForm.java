// Copyright 2015-2021 Swim Inc.
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

package ai.swim.structure.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a class, enum or interface to be processed by the Form annotation processor to generate an implementation of
 * {@code Recognizer<T>} and {@code Writable<T>} where {@code T} is the object that this annotation has been placed on.
 * <p>
 * A form provides a conversion mechanism between two types; such as a Java class and a Recon String, or a {@code Value}.
 * All types that wish to interface with a Swim application must provide both a {@code Recognizer<T>} and
 * {@code Writable<T>} definition and have registered it with their respective proxy.
 * <p>
 * For non-specialised types, marking a Java object with {@code @AutoForm} should suffice in generating recognizers and
 * writable objects that will then automatically be registered with their respective proxy upon the class being loaded.
 * These generated definitions the minimal amount of reflection possible and only require it to resolve types when
 * writing an object; if a type is known when deserializing an object, it is possible to provide a type parameter to
 * the recognizer through its {@code @TypedConstructor} constructor.
 *
 * <h1>Classes</h1>
 * The form annotation processor functions similarly to other data serialization libraries with the exception that the
 * entire definition is generated at compile time instead of using runtime reflection. This annotation may be placed on
 * both concrete and abstract classes to generate {@code Recognizer<T>} and {@code Writable<T>} definitions
 * automatically but a field may appear only once in the inheritance tree unless it has a mirror that is ignored; any
 * subtypes of a class must be marked with {@code @AutoForm(subtypes = {...}}.
 * <p>
 * The form annotation processor supports generics, wildcard types with and without inheritance bounds, and the use of
 * polymorphic types as fields. When the processor encounters these types, it will attempt to unroll the type if it has
 * any inheritance bounds so that it can align either a {@code Recognizer} or {@code Writable} for the field. For
 * example, if a class has a generic of {@code L extends List<? extends Number>} then fields that use this type
 * parameter will have a matching recognizer of {@code List<Number>}. If a field uses a type of {@code Object},
 * {@code ?} or any unbounded generic type parameter, then the matching {@code Recognizer} will be untyped and a
 * best-effort approach is taken to deserialize the type from known scalar recognizers. For the {@code Writable}
 * definition, runtime reflection is used to find a matching {@code Writable} for the field.
 *
 * <h1>Interfaces</h1>
 * All interfaces annotated with this parameter must have the {@code subtype} property populated with the acceptable
 * subtypes of this object.
 *
 * <h1>Enums</h1>
 * Compared to other data serialization libraries, form enumerations in Java work differently in that their entire
 * definition is both serialized and deserialized as a node may be communicating with another node that is not written
 * in the same language. As such, the structure of the read events fed to an enum recognizer will be compared against
 * the current definition of the enumeration. No thread safety is provided by automatically derived enumerations and
 * the most recent version of the enumeration is used when it is checked for equality after it has deserialized the
 * definition. Aside from this, derived form definitions for enumerations function the same as class definitions with
 * the exception that getters are not required for variables as the enumeration is only read to, not mutated.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface AutoForm {

  /**
   * Notifies the annotation processor to generate a {@code Writable} definition for this object.
   *
   * @return whether to generate a {@code Writable} definition for this object.
   */
  boolean writer() default true;

  /**
   * Notifies the annotation processor to generate a {@code Recognizer} definition for this object.
   *
   * @return whether to generate a {@code Recognizer} definition for this object.
   */
  boolean recognizer() default true;

  /**
   * Polymorphic subtypes of this object that may be used for deriving {@code Recognizer} and {@code Writable}
   * definitions.
   */
  Type[] subTypes() default {};

  /**
   * An annotation to specify the name of this class or enumeration constant
   */
  @Target({ElementType.TYPE, ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Tag {
    /**
     * The tag of this object.
     */
    String value() default "";
  }

  /**
   * A marker annotation used to denote the placement of this field in the transmuted structure.
   * <p>
   * Generally, this property should not be required.
   */
  @Target({ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Kind {
    FieldKind value();
  }

  /**
   * Renames this field. This property is not applicable for enumeration constants, use {@code @Tag} instead.
   */
  @Target({ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Name {
    /**
     * Sets the name of this field.
     */
    String value();
  }

  /**
   * A marker annotation that can be used to specify a method that provides mutable access to set the state of a field
   * in this class. The annotated method must be a local method that accepts a variable of the same type that this
   * annotation references. Every member  variable in the parent class must either have public visibility, provide a
   * method named {@code setVar} (where 'var' is the name of the reference variable) or have a method annotated with
   * this annotation.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Setter {
    /**
     * The variable's name that this getter provides access to.
     */
    String value();
  }

  /**
   * A marker annotation that can be used to specify a method that is used to access a member variable of a class. This
   * method must be a no-argument, member method that returns the same type as the field specified. Every member
   * variable in the parent class must either have public visibility, provide a method named {@code getVar} (where 'var'
   * is the name of the reference variable) or have a method annotated with this annotation.
   */
  @Target(ElementType.METHOD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Getter {
    /**
     * The variable's name that this getter provides access to.
     */
    String value();
  }

  /**
   * Marks this field to be written as optional in the generated {@code Writable} and {@code Recognizer} definitions. If
   * the field is missing, then its default value will be used in the {@code Recognizer} and it will be {@code Extant}
   * in its {@code Writable} definition.
   */
  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Optional {

  }

  /**
   * Marks this field to be ignored by the annotation processor.
   */
  @Target(ElementType.FIELD)
  @Retention(RetentionPolicy.RUNTIME)
  @interface Ignore {

  }

  /**
   * Annotation used with {@code AutoForm.subTypes} to mark polymorphic subtypes that may be abstract classes, concrete
   * classes or interfaces.
   */
  @Target({ElementType.FIELD})
  @Retention(RetentionPolicy.RUNTIME)
  @interface Type {
    /**
     * The subtype's class.
     */
    Class<?> value();
  }

  /**
   * A marker annotation that is used to define a constructor that accepts {@code TypeParameter}s that will initialize
   * either a {@code Recognizer}'s generic field recognizers or a {@code Writable}'s generic field writables. This is
   * more important for {@code Recognizer}'s, as if the annotation processor visits a generic field then it will set
   * the recognizer for that field to be one that attempts to deserialize the field from known scalar types, a
   * collection or map of scalar types. For {@code Writable} definitions, the field is looked up using reflection and
   * a matched {@code Writable} will be set for that field
   */
  @Target({ElementType.CONSTRUCTOR})
  @Retention(RetentionPolicy.RUNTIME)
  @interface TypedConstructor {

  }

}
