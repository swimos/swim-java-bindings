// Copyright 2015-2022 Swim Inc.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//      http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package ai.swim.structure.processor.models;

import ai.swim.structure.processor.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.ElementKind;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import java.nio.channels.SelectionKey;
import java.util.List;

public class ModelInstance extends Model {
  private final String init;

  public ModelInstance(TypeMirror type, String init) {
    super(type);
    this.init = init;
  }

  public static Resolver resolver(String root) {
    return new Resolver(root);
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor, boolean isAbstract) {
    return CodeBlock.of("$L", init);
  }

  public static class Resolver {
    private final String root;

    Resolver(String root) {
      this.root = root;
    }

    public ModelInstance resolve(ProcessingEnvironment environment, String elementName) {
      Elements elementUtils = environment.getElementUtils();
      String canonicalPath = String.format("%s.%s", root, elementName);
      TypeElement element = elementUtils.getTypeElement(root);

      if (element == null) {
        throw new IllegalArgumentException(String.format("Element %s does not exist", root));
      }

      ElementKind elementKind = element.getKind();
      if (!(elementKind.isClass() || elementKind.isInterface())) {
        throw new IllegalArgumentException(String.format("Element resolvers can only resolve members of interfaces or classes: %s", canonicalPath));
      }

      DeclaredType declaredType = (DeclaredType) element.asType();

      for (Element enclosedElement : declaredType.asElement().getEnclosedElements()) {
        if (enclosedElement.getKind().isField()) {
          VariableElement field = (VariableElement) enclosedElement;

          if (field.getSimpleName().contentEquals(elementName)) {
            if (!field.getModifiers().contains(Modifier.STATIC)) {
              throw new RuntimeException("Lookups must reference static variables. Attempted to reference: " + canonicalPath);
            }

            if (field.asType().getKind() == TypeKind.DECLARED) {
              DeclaredType fieldType = (DeclaredType) field.asType();
              List<? extends TypeMirror> typeArguments = fieldType.getTypeArguments();

              if (typeArguments.size() == 0) {
                return new ModelInstance(field.asType(), canonicalPath);
              } else if (typeArguments.size() == 1) {
                return new ModelInstance(typeArguments.get(0), canonicalPath);
              } else {
                throw new RuntimeException("Model has more than one type parameter: " + elementName);
              }
            } else {
              return new ModelInstance(field.asType(), canonicalPath);
            }
          }
        }
      }

      throw new IllegalStateException(String.format("%s could not be resolved", canonicalPath));
    }
  }
}
