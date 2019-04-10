import javafx.beans.property.*;
import menagerie.model.Settings;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

public class SettingsTests {

    @ParameterizedTest(name = "DefaultIsSet {0}")
    @EnumSource(Settings.Key.class)
    void defaultIsSet(Settings.Key key) {
        Settings s = new Settings();

        assertNotNull(s.getProperty(key), String.format("\"%s\" has no default value", key));
    }

    @ParameterizedTest(name = "SetGetString {0}")
    @EnumSource(Settings.Key.class)
    void setGetString(Settings.Key key) {
        Settings s = new Settings();

        Property p = s.getProperty(key);
        if (p instanceof BooleanProperty) {
            s.setBoolean(key, true);
            assertTrue(s.getBoolean(key), String.format("'true' not stored or retrieved properly with key: '%s'", key));
            s.setBoolean(key, false);
            assertFalse(s.getBoolean(key), String.format("'false' not stored or retrieved properly with key: '%s'", key));
        } else if (p instanceof StringProperty) {
            final String expected = "string thing here 123 /:\"asd";
            assertTrue(s.setString(key, expected), String.format("String \"%s\" not accepted with key: '%s'", expected, key));
            assertEquals(expected, s.getString(key), String.format("String \"%s\" not stored/retrieved properly with key: '%s'", expected, key));

            final String newLine = "this string has a\nnewline";
            assertFalse(s.setString(key, newLine), String.format("String containing newline was accepted with key: '%s'", key));

            assertTrue(s.setString(key, null), String.format("Null string not accepted with key: '%s'", key));
            assertNull(s.getString(key), String.format("Null string not stored/retrieved properly with key: '%s'", key));
        } else if (p instanceof DoubleProperty) {
            final double expected = 54.321;
            s.setDouble(key, expected);
            assertEquals(expected, s.getDouble(key), String.format("Double %f not stored/retrieved properly with key: '%s'", expected, key));
        } else if (p instanceof IntegerProperty) {
            final int expected = 12345;
            s.setInt(key, expected);
            assertEquals(expected, s.getInt(key), String.format("Int %d not stored/retrieved properly with key: '%s'", expected, key));
        } else {
            fail(String.format("Key '%s' has unexpected property: %s", key, p));
        }
    }

    @DisplayName("SaveLoadTest")
    @Test
    void saveLoad() {
        final Settings s = new Settings();
        final File file;
        try {
            file = File.createTempFile("settings", ".txt");
            file.deleteOnExit();

            final int intVal = 12345;
            final double doubleVal = 54.321;
            final boolean boolVal = true;
            final String stringVal = "string abcd 1234";

            // Set all settings
            for (Settings.Key key : Settings.Key.values()) {
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

                Settings s2 = new Settings(file);
                for (Settings.Key key : Settings.Key.values()) {
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
