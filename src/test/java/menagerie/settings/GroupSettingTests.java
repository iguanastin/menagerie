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

public class GroupSettingTests {

    @Test
    void createGet() {
        String id = "id";
        String label = "label";
        GroupSetting s = new GroupSetting(id, label);
        assertEquals(id, s.getID());
        assertEquals(label, s.getLabel());

        boolean toggle = true;
        boolean enabled = false;
        s = new GroupSetting(id, label, toggle, enabled);
        assertEquals(id, s.getID());
        assertEquals(label, s.getLabel());
        assertEquals(toggle, s.isToggleable());
        assertEquals(enabled, s.isEnabled());

        assertNotNull(s.getChildren());
        assertNotNull(s.toggleableProperty());
        assertNotNull(s.enabledProperty());

        toggle = false;
        enabled = true;
        s.setToggleable(toggle);
        s.setEnabled(enabled);
        assertEquals(toggle, s.isToggleable());
        assertEquals(enabled, s.isEnabled());

        assertEquals("group", s.getType());

        assertDoesNotThrow(s::getVersion);
    }

    @Test
    void testJSON() {
        GroupSetting s = new GroupSetting("id", "label", true, false);
        s.getChildren().add(new IntSetting("int", "label", 123));
        GroupSetting g = new GroupSetting("g2", "label");
        g.getChildren().add(new BooleanSetting("bool", "label", true));
        s.getChildren().add(g);
        GroupSetting s2 = GroupSetting.fromJSON(s.toJSON());

        assertEquals(s, s2);
    }

    @Test
    void testEquals() {
        GroupSetting s1 = new GroupSetting("id", "label", false, false);
        GroupSetting s2 = new GroupSetting("id", "label", true, false);

        assertNotEquals(s1, s2);

        s2.setToggleable(false);
        assertEquals(s1, s2);

        IntSetting is1 = new IntSetting("int", "label", 1234);
        IntSetting is2 = new IntSetting("int", "label", 1234);
        s1.getChildren().add(is1);
        s2.getChildren().add(is2);
        assertEquals(s1, s2);

        is2.setValue(-4321);
        assertNotEquals(s1, s2);
    }

    @Test
    void testToString() {
        assertDoesNotThrow(() -> new GroupSetting("id", "label").toString());
    }

}
