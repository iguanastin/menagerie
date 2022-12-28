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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.DoubleProperty;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.Property;
import javafx.beans.property.StringProperty;
import menagerie.settings.OldSettings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class OldSettingsTests {

  @ParameterizedTest(name = "DefaultIsSet {0}")
  @EnumSource(OldSettings.Key.class)
  void defaultIsSet(OldSettings.Key key) {
    OldSettings s = new OldSettings();

    assertNotNull(s.getProperty(key), String.format("\"%s\" has no default value", key));
  }

  @ParameterizedTest(name = "SetGetString {0}")
  @EnumSource(OldSettings.Key.class)
  void setGetString(OldSettings.Key key) {
    OldSettings s = new OldSettings();

    Property p = s.getProperty(key);
    if (p instanceof BooleanProperty) {
      s.setBoolean(key, true);
      assertTrue(s.getBoolean(key),
          String.format("'true' not stored or retrieved properly with key: '%s'", key));
      s.setBoolean(key, false);
      assertFalse(s.getBoolean(key),
          String.format("'false' not stored or retrieved properly with key: '%s'", key));
    } else if (p instanceof StringProperty) {
      final String expected = "string thing here 123 /:\"asd";
      assertTrue(s.setString(key, expected),
          String.format("String \"%s\" not accepted with key: '%s'", expected, key));
      assertEquals(expected, s.getString(key),
          String.format("String \"%s\" not stored/retrieved properly with key: '%s'", expected,
              key));

      final String newLine = "this string has a\nnewline";
      assertFalse(s.setString(key, newLine),
          String.format("String containing newline was accepted with key: '%s'", key));

      assertTrue(s.setString(key, null),
          String.format("Null string not accepted with key: '%s'", key));
      assertNull(s.getString(key),
          String.format("Null string not stored/retrieved properly with key: '%s'", key));
    } else if (p instanceof DoubleProperty) {
      final double expected = 54.321;
      s.setDouble(key, expected);
      assertEquals(expected, s.getDouble(key),
          String.format("Double %f not stored/retrieved properly with key: '%s'", expected, key));
    } else if (p instanceof IntegerProperty) {
      final int expected = 12345;
      s.setInt(key, expected);
      assertEquals(expected, s.getInt(key),
          String.format("Int %d not stored/retrieved properly with key: '%s'", expected, key));
    } else {
      fail(String.format("Key '%s' has unexpected property: %s", key, p));
    }
  }

  @DisplayName("SaveLoadTest")
  @Test
  void saveLoad() {
    final OldSettings s = new OldSettings();
    final File file;
    try {
      file = File.createTempFile("settings", ".txt");
      file.deleteOnExit();

      final int intVal = 12345;
      final double doubleVal = 54.321;
      final boolean boolVal = true;
      final String stringVal = "string abcd 1234";

      // Set all settings
      for (OldSettings.Key key : OldSettings.Key.values()) {
        Property p = s.getProperty(key);
        if (p instanceof StringProperty) {
          s.setString(key, stringVal);
        } else if (p instanceof IntegerProperty) {
          s.setInt(key, intVal);
        } else if (p instanceof DoubleProperty) {
          s.setDouble(key, doubleVal);
        } else if (p instanceof BooleanProperty) {
          s.setBoolean(key, boolVal);
        } else {
          fail(String.format("Key '%s' has unexpected property: %s", key, p));
        }
      }

      try {
        s.save(file);

        OldSettings s2 = new OldSettings(file);
        for (OldSettings.Key key : OldSettings.Key.values()) {
          Property p = s2.getProperty(key);
          if (p instanceof StringProperty) {
            assertEquals(stringVal, s2.getString(key));
          } else if (p instanceof IntegerProperty) {
            assertEquals(intVal, s2.getInt(key));
          } else if (p instanceof DoubleProperty) {
            assertEquals(doubleVal, s2.getDouble(key));
          } else if (p instanceof BooleanProperty) {
            assertEquals(boolVal, s2.getBoolean(key));
          } else {
            fail(String.format("Key '%s' has unexpected property: %s", key, p));
          }
        }
      } catch (FileNotFoundException e) {
        fail(e);
      }
    } catch (IOException e) {
      fail(e);
    }
  }

}
