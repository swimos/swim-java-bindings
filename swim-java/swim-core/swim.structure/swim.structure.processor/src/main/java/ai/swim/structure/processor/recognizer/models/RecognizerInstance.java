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

package ai.swim.structure.processor.recognizer.models;

import ai.swim.structure.processor.recognizer.context.ScopedContext;
import com.squareup.javapoet.CodeBlock;

import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.type.DeclaredType;
import javax.lang.model.type.TypeKind;
import javax.lang.model.type.TypeMirror;
import javax.lang.model.util.Elements;
import javax.lang.model.util.Types;
import java.util.List;

public class RecognizerInstance extends RecognizerModel {
  private final String init;

  public RecognizerInstance(TypeMirror type, String init) {
    super(type);
    this.init = init;
  }

  @Override
  public CodeBlock initializer(ScopedContext context, boolean inConstructor) {
    return CodeBlock.of("$L", init);
  }

  public static Resolver resolver(String root) {
    return new Resolver(root);
  }

  public static class Resolver {
    private final String root;

    Resolver(String root) {
      this.root = root;
    }

    public RecognizerInstance resolve(ProcessingEnvironment environment, String elementName) {
      Elements elementUtils = environment.getElementUtils();
      Types typeUtils = environment.getTypeUtils();
      String canonicalPath = String.format("%s.%s", root, elementName);
      TypeElement element = elementUtils.getTypeElement(canonicalPath);

      if (element == null) {
        element = elementUtils.getTypeElement(root);
        DeclaredType declaredType = (DeclaredType) element.asType();

        // we couldn't find the element itself but perhaps the class contains static instances of a reusable recognizer.
        // iterate over those and try and find a matching type
        for (Element enclosedElement : declaredType.asElement().getEnclosedElements()) {
          if (enclosedElement.getKind().isField()) {
            VariableElement field = (VariableElement) enclosedElement;

            if (field.getSimpleName().contentEquals(elementName)) {
              if (!field.getModifiers().contains(Modifier.STATIC)){
                throw new RuntimeException("Recognizer lookups must reference static variables. Attempted to reference: " + canonicalPath);
              }

              if (field.asType().getKind() == TypeKind.DECLARED) {
                DeclaredType fieldType = (DeclaredType) field.asType();
                List<? extends TypeMirror> typeArguments = fieldType.getTypeArguments();

                if (typeArguments.size() == 0) {
                  return new RecognizerInstance(field.asType(), canonicalPath);
                } else if (typeArguments.size() == 1) {
                  return new RecognizerInstance(typeArguments.get(0), canonicalPath);
                } else {
                  throw new RuntimeException("Recognizer has more than one type parameter: " + elementName);
                }
              } else {
                return new RecognizerInstance(field.asType(), canonicalPath);
              }
            }
          }
        }

        throw new IllegalStateException(String.format("%s.%s could not be resolved", root, elementName));
      } else {
        return new RecognizerInstance(typeUtils.erasure(element.asType()), canonicalPath);
      }
    }
  }
}
