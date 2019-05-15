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

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

class SettingsTests {

    private Settings s = new Settings();

    private IntSetting int1 = new IntSetting("int1", "int setting", 12345);
    private IntSetting int2 = new IntSetting("int2", "int setting", -54321);
    private DoubleSetting double1 = new DoubleSetting("double1", "double setting", -321.45);
    private DoubleSetting double2 = new DoubleSetting("double2", "double setting", 123.54);
    private BooleanSetting boolean1 = new BooleanSetting("boolean1", "boolean setting", true);
    private BooleanSetting boolean2 = new BooleanSetting("boolean2", "boolean setting", false);
    private StringSetting string1 = new StringSetting("string1", "string setting", "a string");

    private GroupSetting group1 = new GroupSetting("group1", "first group", true, false);

    private GroupSetting group2 = new GroupSetting("group2", "second group");

    @BeforeEach
    void resetVariables() {
        s = new Settings();

        int1 = new IntSetting("int1", "int setting", 12345);
        int2 = new IntSetting("int2", "int setting", -54321);
        double1 = new DoubleSetting("double1", "double setting", -321.45);
        double2 = new DoubleSetting("double2", "double setting", 123.54);
        boolean1 = new BooleanSetting("boolean1", "boolean setting", true);
        boolean2 = new BooleanSetting("boolean2", "boolean setting", false);
        string1 = new StringSetting("string1", "string setting", "a string");

        group1 = new GroupSetting("group1", "first group", true, false);

        group2 = new GroupSetting("group2", "second group");

        s.getSettings().add(int1);
        s.getSettings().add(double1);
        s.getSettings().add(boolean1);
        s.getSettings().add(string1);

        s.getSettings().add(group1);
        group1.getChildren().add(int2);
        group1.getChildren().add(double2);

        group1.getChildren().add(group2);
        group2.getChildren().add(boolean2);
    }

    @Test
    void getSetting() {
        assertEquals(int1, s.getSetting("int1"));
        assertEquals(int2, s.getSetting("int2"));
        assertEquals(double1, s.getSetting("double1"));
        assertEquals(double2, s.getSetting("double2"));
        assertEquals(boolean1, s.getSetting("boolean1"));
        assertEquals(boolean2, s.getSetting("boolean2"));
        assertEquals(string1, s.getSetting("string1"));
        assertEquals(group1, s.getSetting("group1"));
        assertEquals(group2, s.getSetting("group2"));

        assertNull(s.getSetting("nonexistent"));
    }

    @Test
    void saveLoad() {
        File file = null;
        try {
            file = File.createTempFile("test", ".txt");
            file.deleteOnExit();
        } catch (IOException e) {
            fail(e);
        }

        final File finalFile = file;
        assertDoesNotThrow(() -> s.save(finalFile));

        Settings s2 = null;
        try {
            s2 = Settings.load(file);
        } catch (IOException e) {
            fail(e);
        }

        assertEquals(s, s2);
    }

    @Test
    void loadBadSettings() {
        File file = null;
        try {
            file = File.createTempFile("test", ".txt");
            file.deleteOnExit();
        } catch (IOException e) {
            fail(e);
        }

        Setting anonSetting = new Setting("bad-setting", "a label") {
            @Override
            public String getType() {
                return "anonymous";
            }

            @Override
            public int getVersion() {
                return 1;
            }
        };
        s.getSettings().add(anonSetting);

        try {
            s.save(file);
        } catch (IOException e) {
            fail(e);
        }

        try {
            Settings.PARSABLE_SETTINGS.add(anonSetting.getClass());
            Settings s2 = Settings.load(file);

            s.getSettings().remove(anonSetting);
            assertEquals(s, s2);
        } catch (IOException e) {
            fail(e);
        } finally {
            Settings.PARSABLE_SETTINGS.remove(anonSetting.getClass());
        }
    }

    @Test
    void testEquals() {
        assertNotEquals(s, new Object());

        Settings s2 = s;
        resetVariables();

        assertEquals(s, s2);

        String id = "bad";
        s2.getSettings().add(new Setting(id, "bad-label") {
            @Override
            public String getType() {
                return "bad-type";
            }

            @Override
            public int getVersion() {
                return 0;
            }
        });
        assertNotEquals(s, s2);
        s2.getSettings().remove(s2.getSetting(id));
        assertEquals(s, s2);

        IntSetting i1 = (IntSetting) s2.getSetting(int1.getID());
        i1.setValue(i1.getValue() + 1337);
        assertNotEquals(s, s2);
    }

}
