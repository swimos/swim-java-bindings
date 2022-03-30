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

package ai.swim.codec.number;

import ai.swim.codec.Parser;
import ai.swim.codec.input.Input;
import ai.swim.codec.parsers.number.NumberParser;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class NumberParserTest {

  void numberTestOk(String input, Number expected) {
    Parser<Number> parser = NumberParser.numericLiteral().feed(Input.string(input));
    assertTrue(parser.isDone());
    assertEquals(parser.bind(), expected);
  }

  void numberTestErr(String input) {
    Parser<Number> parser = NumberParser.numericLiteral().feed(Input.string(input));
    assertTrue(parser.isError());
  }

  void numberTestCont(String firstInput, String secondInput, Number expected) {
    Parser<Number> parser = NumberParser.numericLiteral().feed(Input.string(firstInput).isPartial(true));
    assertTrue(parser.isCont());

    parser = parser.feed(Input.string(secondInput));
    assertTrue(parser.isDone());

    assertEquals(parser.bind(), expected);
  }

  @Test
  void parseIntegerDone() {
    numberTestOk("0x3039", 12345);
    numberTestOk("1230x3039", 1230);
    numberTestOk("0x3039", 12345);
//    numberTestOk("nan", Float.NaN);
//    numberTestOk("NaN", Float.NaN);
    numberTestOk("-inf", Float.NEGATIVE_INFINITY);
//    numberTestOk("inf", Float.POSITIVE_INFINITY);
//    numberTestOk("infinity", Float.POSITIVE_INFINITY);
    numberTestOk("12345.0", 12345.0f);
    numberTestOk("1", 1);
    numberTestOk("0012345", 12345);
    numberTestOk("0012345.12345", 12345.12345);
    numberTestOk("0x3039 ", 12345);
//    numberTestOk("nan ", Float.NaN);
    numberTestOk("-inf ", Float.NEGATIVE_INFINITY);
    numberTestOk("12345.0 ", 12345.0f);
    numberTestOk("1 ", 1);
    numberTestOk("0012345 ", 12345);
    numberTestOk("0012345.12345 ", 12345.12345);
    numberTestOk("12345asd", 12345);
    numberTestOk("0012345.:", 12345f);
    numberTestOk("00.X", 0f);
    numberTestOk("1.17549435e-38", 1.17549435e-38); // Float.MIN_VALUE
    numberTestOk("3.4028235e38", 3.4028235e38); // Float.MAX_VALUE
    numberTestOk("1.17549435e-38", 1.17549435e-38); // Float.MIN_NORMAL
    numberTestOk("4.9e-324", 4.9e-324); // Double.MIN_VALUE
    numberTestOk("1.7976931348623157e308", 1.7976931348623157e308); // Double.MAX_VALUE
    numberTestOk("2.2250738585072014e-308", 2.2250738585072014e-308); // Double.MIN_NORMAL
    numberTestOk("-4.0E02", -4.0E2f);
    numberTestOk("-4.0e+02", -4.0e+2f);
    numberTestOk("-4.0E+02", -4.0E+2f);
    numberTestOk("-4.0e-02", -4.0e-2);
    numberTestOk("-4.0E-02", -4.0E-2);
    numberTestOk("0x0", 0x0);
    numberTestOk("0x00000001", 0x00000001);
    numberTestOk("0x00000010", 0x00000010);
    numberTestOk("0x00000100", 0x00000100);
    numberTestOk("0x00001000", 0x00001000);
    numberTestOk("0x00010000", 0x00010000);
    numberTestOk("0x00100000", 0x00100000);
    numberTestOk("0x01000000", 0x01000000);
    numberTestOk("0x10000000", 0x10000000);
    numberTestOk("0xFFFFFFFF", 0xFFFFFFFF);
    numberTestOk("0xFEDCBA98", 0xFEDCBA98);
    numberTestOk("0x01234567", 0x01234567);
    numberTestOk("0x0000000000000001", 0x0000000000000001L);
    numberTestOk("0x0000000000000010", 0x0000000000000010L);
    numberTestOk("0x0000000000000100", 0x0000000000000100L);
    numberTestOk("0x0000000000001000", 0x0000000000001000L);
    numberTestOk("0x0000000000010000", 0x0000000000010000L);
    numberTestOk("0x0000000000100000", 0x0000000000100000L);
    numberTestOk("0x0000000001000000", 0x0000000001000000L);
    numberTestOk("0x0000000010000000", 0x0000000010000000L);
    numberTestOk("0x0000000100000000", 0x0000000100000000L);
    numberTestOk("0x0000001000000000", 0x0000001000000000L);
    numberTestOk("0x0000010000000000", 0x0000010000000000L);
    numberTestOk("0x0000100000000000", 0x0000100000000000L);
    numberTestOk("0x0001000000000000", 0x0001000000000000L);
    numberTestOk("0x0010000000000000", 0x0010000000000000L);
    numberTestOk("0x0100000000000000", 0x0100000000000000L);
    numberTestOk("0x1000000000000000", 0x1000000000000000L);
    numberTestOk("0xFFFFFFFFFFFFFFFF", 0xFFFFFFFFFFFFFFFFL);
    numberTestOk("0xFEDCBA9876543210", 0xFEDCBA9876543210L);
    numberTestOk("0x0123456789ABCDEF", 0x0123456789ABCDEFL);
    numberTestOk("9223372036854775808", new BigInteger("9223372036854775808"));
    numberTestOk("-9223372036854775809", new BigInteger("-9223372036854775809"));
    numberTestOk("259804429589205426119611", new BigInteger("259804429589205426119611"));
    numberTestOk("-259804429589205426119611", new BigInteger("-259804429589205426119611"));
  }

  @Test
  void parseIntegerCont() {
    numberTestCont("0x", "3039", 12345);
//    numberTestCont("na", "n", Float.NaN);
    numberTestCont("-", "inf", Float.NEGATIVE_INFINITY);
    numberTestCont("12345", ".0", 12345.0f);
    numberTestCont("", "1", 1);
    numberTestCont("00", "12345", 12345);
    numberTestCont("0012345", ".12345", 12345.12345);
    numberTestCont(".", "1", 0.1d);
    numberTestCont("", ".1", 0.1d);
  }

  @Test
  void parseIntegerError() {
    numberTestErr("0xZ");
    numberTestErr("naZ");
    numberTestErr("-nan");
    numberTestErr("infanity");
    numberTestErr("-Z");
    numberTestErr("a");
    numberTestErr("..1");
  }

}