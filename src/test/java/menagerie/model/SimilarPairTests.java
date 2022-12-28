/*
 MIT License

 Copyright (c) 2019. Austin Thompson

 Permission is hereby granted, free of charge, to any person obtaining a copy
 of this software and associated documentation files (the "Software"), to deal
 in the Software without restriction, including without limitation the rights
 to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 copies of the Software, and to permit persons to whom the Software is
 furnished to do so, subject to the following conditions:

 The above copyright notice and this permission notice shall be included in all
 copies or substantial portions of the Software.

 THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 SOFTWARE.
 */

package menagerie.model;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class SimilarPairTests {

  @Test
  void createGet() {
    String s1 = "string1", s2 = "String 2";
    double similarity = 0.95;
    SimilarPair<String> s = new SimilarPair<>(s1, s2, similarity);

    assertEquals(s1, s.getObject1());
    assertEquals(s2, s.getObject2());
    assertEquals(similarity, s.getSimilarity());
  }

  @Test
  void createNull() {
    String s1 = "string1";
    String s2 = "String 2";
    double similarity = 0.5;

    assertThrows(NullPointerException.class, () -> new SimilarPair<>(null, s2, similarity));
    assertThrows(NullPointerException.class, () -> new SimilarPair<>(s1, null, similarity));
    assertThrows(NullPointerException.class, () -> new SimilarPair<>(null, null, similarity));
  }

  @Test
  void similarRange() {
    String s1 = "string1", s2 = "String 2";

    assertDoesNotThrow(() -> new SimilarPair<>(s1, s2, 0));
    assertDoesNotThrow(() -> new SimilarPair<>(s1, s2, 1));
    assertDoesNotThrow(() -> new SimilarPair<>(s1, s2, 0.25));

    assertThrows(IllegalArgumentException.class, () -> new SimilarPair<>(s1, s2, -1.2345));
    assertThrows(IllegalArgumentException.class, () -> new SimilarPair<>(s1, s2, -1000000));
    assertThrows(IllegalArgumentException.class, () -> new SimilarPair<>(s1, s2, 1.1));
    assertThrows(IllegalArgumentException.class, () -> new SimilarPair<>(s1, s2, 12312312.5));
  }

  @Test
  void equalObjects() {
    String str = "string1";

    assertThrows(IllegalArgumentException.class, () -> new SimilarPair<>(str, str, 0.5));
  }

}
