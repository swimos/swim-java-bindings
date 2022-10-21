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

package ai.swim.structure.processor.recognizer.writer;

public class Lookups {

  public static final String RECOGNIZING_BUILDER_CLASS = "ai.swim.structure.RecognizingBuilder";
  public static final String FIELD_RECOGNIZING_BUILDER_CLASS = "ai.swim.structure.FieldRecognizingBuilder";
  public static final String RECOGNIZING_BUILDER_FEED_INDEX = "feedIndexed";
  public static final String RECOGNIZING_BUILDER_BIND = "bind";
  public static final String RECOGNIZING_BUILDER_RESET = "reset";
  public static final String TYPE_READ_EVENT = "ai.swim.recon.event.ReadEvent";
  public static final String RECOGNIZER_CLASS = "ai.swim.structure.recognizer.Recognizer";
  public static final String LIST_RECOGNIZER_CLASS = "ai.swim.structure.recognizer.std.collections.ListRecognizer";
  public static final String MAP_RECOGNIZER_CLASS = "ai.swim.structure.recognizer.std.MapRecognizer";
  public static final String STRUCTURAL_RECOGNIZER_CLASS = "ai.swim.structure.recognizer.structural.StructuralRecognizer";
  public static final String LABELLED_CLASS_RECOGNIZER = "ai.swim.structure.recognizer.structural.labelled.LabelledClassRecognizer";
  public static final String DELEGATE_CLASS_RECOGNIZER = "ai.swim.structure.recognizer.structural.delegate.DelegateClassRecognizer";
  public static final String FIXED_TAG_SPEC = "ai.swim.structure.recognizer.structural.tag.FixedTagSpec";
  public static final String ENUM_TAG_SPEC = "ai.swim.structure.recognizer.structural.tag.EnumerationTagSpec";
  public static final String FIELD_TAG_SPEC = "ai.swim.structure.recognizer.structural.tag.FieldTagSpec";
  public static final String LABELLED_ITEM_FIELD_KEY = "ai.swim.structure.recognizer.structural.labelled.LabelledFieldKey.ItemFieldKey";
  public static final String LABELLED_ATTR_FIELD_KEY = "ai.swim.structure.recognizer.structural.labelled.LabelledFieldKey.AttrFieldKey";
  public static final String DELEGATE_HEADER_SLOT_KEY = "ai.swim.structure.recognizer.structural.delegate.HeaderFieldKey.HeaderSlotKey";
  public static final String DELEGATE_ORDINAL_ATTR_KEY = "ai.swim.structure.recognizer.structural.delegate.OrdinalFieldKey.OrdinalFieldKeyAttr";
  public static final String TYPE_PARAMETER = "ai.swim.structure.recognizer.proxy.RecognizerTypeParameter";
  public static final String RECOGNIZER_PROXY = "ai.swim.structure.recognizer.proxy.RecognizerProxy";
  public static final String UNTYPED_RECOGNIZER = "ai.swim.structure.recognizer.untyped.UntypedRecognizer";
  private Lookups() {
    throw new AssertionError();
  }

}
