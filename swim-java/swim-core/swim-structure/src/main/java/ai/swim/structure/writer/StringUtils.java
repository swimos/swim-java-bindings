/*
 * Copyright 2015-2024 Swim Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ai.swim.structure.writer;

public class StringUtils {

  /**
   * Returns whether the string requires escaping.
   */
  public static boolean needsEscape(String c) {
    return c.chars().anyMatch(StringUtils::isEscapeChar);
  }

  /**
   * Returns whether the code point is an escape character.
   */
  public static boolean isEscapeChar(int c) {
    return c == 0x09 || c == 0x20
        || c >= 0x21 && c <= 0x7e
        || c >= 0x80 && c <= 0xff;
  }

  /**
   * Returns whether the string is a valid Recon identifier.
   */
  public static boolean isIdentifier(String value) {
    final int n = value.length();
    if (n == 0 || !StringUtils.isIdentStartChar(value.codePointAt(0))) {
      return false;
    }
    for (int i = value.offsetByCodePoints(0, 1); i < n; i = value.offsetByCodePoints(i, 1)) {
      if (!StringUtils.isIdentChar(value.codePointAt(i))) {
        return false;
      }
    }
    return true;
  }

  /**
   * Returns whether the code point is valid a a Recon identifier.
   */
  static boolean isIdentChar(int c) {
    return isIdentStartChar(c) || c == '-' || c >= '0' && c <= '9';
  }

  /**
   * Returns whether the code point is valid after the start of an identifier.
   */
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

  /**
   * Escapes all control characters.
   */
  public static String escape(String value) {
    StringBuilder output = new StringBuilder(value.length());
    final int n = value.length();

    for (int i = 0; i < n; i = value.offsetByCodePoints(i, 1)) {
      final int c = value.codePointAt(i);
      switch (c) {
        case '\b':
          output.append("\\b");
          break;
        case '\t':
          output.append("\\t");
          break;
        case '\n':
          output.append("\\n");
          break;
        case '\f':
          output.append("\\f");
          break;
        case '\r':
          output.append("\\r");
          break;
        case '\"':
          output.append("\\\"");
          break;
        case '\\':
          output.append("\\\\");
          break;
        default:
          if (Character.isISOControl(c)) {
            output
                .appendCodePoint('\\')
                .append('u')
                .append(StringUtils.encodeHex((c >>> 12) & 0xf))
                .append(StringUtils.encodeHex((c >>> 8) & 0xf))
                .append(StringUtils.encodeHex((c >>> 4) & 0xf))
                .append(StringUtils.encodeHex(c & 0xf));
          } else {
            output.appendCodePoint(c);
          }
      }
    }

    return output.toString();
  }

  private static char encodeHex(int x) {
    if (x < 10) {
      return (char) ('0' + x);
    } else {
      return (char) ('A' + (x - 10));
    }
  }

}
