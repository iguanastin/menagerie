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

public class BooleanSettingTests {

    @Test
    void createGet() {
        String id = "id";
        BooleanSetting s = new BooleanSetting(id);
        assertEquals(id, s.getID());

        boolean value = true, hidden = true;
        String label = "label", tip = "tip";
        s = new BooleanSetting(id, label, tip, hidden, true);
        assertEquals(id, s.getID());
        assertEquals(label, s.getLabel());
        assertEquals(value, s.getValue());
        assertEquals(hidden, s.isHidden());

        assertNotNull(s.valueProperty());

        s.setValue(false);
        assertFalse(s.getValue());

        assertEquals("boolean", s.getType());
    }

    @Test
    void testJSON() {
        BooleanSetting s = new BooleanSetting("id", "label", "tip", true, true);
        BooleanSetting s2 = BooleanSetting.fromJSON(s.toJSON());

        assertEquals(s, s2);
    }

    @Test
    void testEquals() {
        BooleanSetting s1 = new BooleanSetting("id", "label", "tip", false, true);
        BooleanSetting s2 = new BooleanSetting("id", "label", "tip", false, false);

        assertNotEquals(s1, s2);

        s2.setValue(true);
        assertEquals(s1, s2);
    }

    @Test
    void testToString() {
        assertDoesNotThrow(() -> new BooleanSetting("id").toString());
    }

}
