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

package ai.swim.recon;

public class ReconUtils {

  static boolean isIdentStartChar(int c) {
    return c >= 'A' && c <= 'Z'
      || c == '_'
      || c >= 'a' && c <= 'z'
      || c >= 0xc0 && c <= 0xd6
      || c >= 0xd8 && c <= 0xf6
      || c >= 0xf8 && c <= 0x2ff
      || c >= 0x370 && c <= 0x37d
      || c >= 0x37f && c <= 0x1fff
      || c >= 0x200c && c <= 0x200d
      || c >= 0x2070 && c <= 0x218f
      || c >= 0x2c00 && c <= 0x2fef
      || c >= 0x3001 && c <= 0xd7ff
      || c >= 0xf900 && c <= 0xfdcf
      || c >= 0xfdf0 && c <= 0xfffd
      || c >= 0x10000 && c <= 0xeffff;
  }

  static boolean isIdentChar(int c) {
    return c == '-'
      || c >= '0' && c <= '9'
      || c >= 'A' && c <= 'Z'
      || c == '_'
      || c >= 'a' && c <= 'z'
      || c == 0xb7
      || c >= 0xc0 && c <= 0xd6
      || c >= 0xd8 && c <= 0xf6
      || c >= 0xf8 && c <= 0x37d
      || c >= 0x37f && c <= 0x1fff
      || c >= 0x200c && c <= 0x200d
      || c >= 0x203f && c <= 0x2040
      || c >= 0x2070 && c <= 0x218f
      || c >= 0x2c00 && c <= 0x2fef
      || c >= 0x3001 && c <= 0xd7ff
      || c >= 0xf900 && c <= 0xfdcf
      || c >= 0xfdf0 && c <= 0xfffd
      || c >= 0x10000 && c <= 0xeffff;
  }

}
