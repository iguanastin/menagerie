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

public class FileSettingTests {

    @Test
    void createGet() {
        String id = "id";
        FileSetting s = new FileSetting(id);
        assertEquals(id, s.getID());

        String value = "some/path", label = "label", tip = "tip";
        boolean hidden = true;
        s = new FileSetting(id, label, tip, hidden, value);
        assertEquals(id, s.getID());
        assertEquals(label, s.getLabel());
        assertEquals(value, s.getValue());
        assertEquals(hidden, s.isHidden());

        assertNotNull(s.valueProperty());

        value = "qwerty";
        s.setValue(value);
        assertEquals(value, s.getValue());

        assertEquals("file", s.getType());
    }

    @Test
    void testJSON() {
        FileSetting s = new FileSetting("id", "label", "tip", true, "A strinnnnnnnnnnnnnnnng");
        FileSetting s2 = FileSetting.fromJSON(s.toJSON());

        assertEquals(s, s2);
    }

    @Test
    void testEquals() {
        FileSetting s1 = new FileSetting("id", "label", "tip", false, "string 1");
        FileSetting s2 = new FileSetting("id", "label", "tip", false, "string 2");

        assertNotEquals(s1, s2);

        s2.setValue("string 1");
        assertEquals(s1, s2);
    }

    @Test
    void testToString() {
        assertDoesNotThrow(() -> new FileSetting("id").toString());
    }

}
