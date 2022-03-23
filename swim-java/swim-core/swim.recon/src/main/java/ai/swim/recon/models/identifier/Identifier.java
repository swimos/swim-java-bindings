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

package ai.swim.recon.models.identifier;

public abstract class Identifier {
  public abstract boolean isText();

  public abstract boolean isBoolean();

  public static Identifier string(String value){
    return new StringIdentifier(value);
  }

  public static Identifier bool(boolean value){
    return new BooleanIdentifier(value);
  }

  public static Identifier decimal(float value){
    return new DecimalLiteralIdentifier(value);
  }

}
