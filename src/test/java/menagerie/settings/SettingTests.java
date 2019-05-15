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

import org.json.JSONObject;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static menagerie.settings.Setting.*;
import static org.junit.jupiter.api.Assertions.*;

public class SettingTests {

    @Test
    void createGet() {
        String id = "id";
        String label = "label";
        String type = "type";
        int version = 5;
        Setting s = new Setting(id, label) {
            @Override
            public String getType() {
                return type;
            }

            @Override
            public int getVersion() {
                return version;
            }
        };

        assertEquals(id, s.getID());
        assertEquals(label, s.getLabel());
        assertEquals(type, s.getType());
        assertEquals(version, s.getVersion());
    }

    @Test
    void testLabel() {
        Setting s = new Setting("id", "label") {
            @Override
            public String getType() {
                return null;
            }

            @Override
            public int getVersion() {
                return 0;
            }
        };

        String newLabel = "new label";
        s.setLabel(newLabel);
        assertEquals(newLabel, s.getLabel());
    }

    @Test
    void testJSONValidation() {
        String type = "type";

        JSONObject j1 = new JSONObject();
        j1.put("id", "id").put("type", type).put("version", 1).put("label", "label");
        assertTrue(Setting.isValidSettingJSON(j1, type));

        JSONObject j2 = new JSONObject();
        j2.put("id", "id").put("type", "wrong type").put("version", 1).put("label", "label");
        assertFalse(Setting.isValidSettingJSON(j2, type));

        JSONObject j3 = new JSONObject();
        j3.put("type", type).put("version", 1).put("label", "label");
        assertFalse(Setting.isValidSettingJSON(j3, type));

        JSONObject j4 = new JSONObject();
        j4.put("id", "id").put("version", 1).put("label", "label");
        assertFalse(Setting.isValidSettingJSON(j4, type));

        JSONObject j5 = new JSONObject();
        j5.put("id", "id").put("type", type).put("label", "label");
        assertFalse(Setting.isValidSettingJSON(j5, type));

        JSONObject j6 = new JSONObject();
        j6.put("id", "id").put("type", type).put("version", 1);
        assertFalse(Setting.isValidSettingJSON(j6, type));
    }

    @Test
    void testJSON() {
        String id = "id";
        String label = "label";
        String type = "type";
        int version = 1;
        Setting s = new Setting(id, label) {
            @Override
            public String getType() {
                return type;
            }

            @Override
            public int getVersion() {
                return version;
            }
        };

        JSONObject json = s.toJSON();

        assertTrue(json.has(ID_KEY));
        assertTrue(json.has(TYPE_KEY));
        assertTrue(json.has(LABEL_KEY));
        assertTrue(json.has(VERSION_KEY));

        assertEquals(id, json.getString(ID_KEY));
        assertEquals(type, json.getString(TYPE_KEY));
        assertEquals(label, json.getString(LABEL_KEY));
        assertEquals(version, json.getInt(VERSION_KEY));
    }

    @Test
    void testEquals() {
        String id = "id";
        String label = "label";
        String type = "type";
        int version = 1;
        Setting s = new Setting(id, label) {
            @Override
            public String getType() {
                return type;
            }

            @Override
            public int getVersion() {
                return version;
            }
        };

        assertEquals(s, s);
        assertNotEquals(s, new Object());

        Setting s1 = new Setting(id, label) {
            @Override
            public String getType() {
                return type;
            }

            @Override
            public int getVersion() {
                return 2;
            }
        };
        assertNotEquals(s, s1);

        Setting s2 = new Setting(id, label) {
            @Override
            public String getType() {
                return "wrong type";
            }

            @Override
            public int getVersion() {
                return version;
            }
        };
        assertNotEquals(s, s2);

        Setting s3 = new Setting(id, "wrong label") {
            @Override
            public String getType() {
                return type;
            }

            @Override
            public int getVersion() {
                return version;
            }
        };
        assertNotEquals(s, s3);

        Setting s4 = new Setting("wrong id", label) {
            @Override
            public String getType() {
                return type;
            }

            @Override
            public int getVersion() {
                return version;
            }
        };
        assertNotEquals(s, s4);
    }

    @Test
    void testToString() {
        Assertions.assertDoesNotThrow(() -> new Setting("id", "label") {
            @Override
            public String getType() {
                return null;
            }

            @Override
            public int getVersion() {
                return 0;
            }
        }).toString();
    }

}
