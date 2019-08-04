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

package menagerie.settings;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class DoubleSettingTests {

    @Test
    void createGet() {
        String id = "id";
        DoubleSetting s = new DoubleSetting(id);
        assertEquals(id, s.getID());

        double value = 0.05;
        String label = "label", tip = "tip";
        boolean hidden = true;
        s = new DoubleSetting(id, label, tip, hidden, value);
        assertEquals(id, s.getID());
        assertEquals(label, s.getLabel());
        assertEquals(value, s.getValue());
        assertEquals(hidden, s.isHidden());

        assertNotNull(s.valueProperty());

        value = -5.0123;
        s.setValue(value);
        assertEquals(value, s.getValue());

        assertEquals("double", s.getType());
    }

    @Test
    void testJSON() {
        DoubleSetting s = new DoubleSetting("id", "label", "tip", true, 1.234);
        DoubleSetting s2 = DoubleSetting.fromJSON(s.toJSON());

        assertEquals(s, s2);
    }

    @Test
    void testEquals() {
        DoubleSetting s1 = new DoubleSetting("id", "label", "tip", false, 1);
        DoubleSetting s2 = new DoubleSetting("id", "label", "tip", false, 2);

        assertNotEquals(s1, s2);

        s2.setValue(1);
        assertEquals(s1, s2);
    }

    @Test
    void testToString() {
        assertDoesNotThrow(() -> new DoubleSetting("id").toString());
    }

}
