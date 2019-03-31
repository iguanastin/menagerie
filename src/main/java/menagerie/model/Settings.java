package menagerie.model;

import javafx.beans.property.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.util.*;

public class Settings {

    public enum Key {
        USE_FILENAME_FROM_URL,
        BACKUP_DATABASE,
        WINDOW_MAXIMIZED,
        COMBINE_TAGS,
        DO_AUTO_IMPORT,
        AUTO_IMPORT_MOVE_TO_DEFAULT,
        MUTE_VIDEO,
        REPEAT_VIDEO,
        COMPARE_GREYSCALE,
        GRID_WIDTH,
        WINDOW_WIDTH,
        WINDOW_HEIGHT,
        WINDOW_X,
        WINDOW_Y,
        DEFAULT_FOLDER,
        DATABASE_URL,
        DATABASE_USER,
        DATABASE_PASSWORD,
        AUTO_IMPORT_FOLDER,
        CONFIDENCE
    }


    private final Map<Key, Property> vars = new HashMap<>();
    private File file;


    public Settings(File file) {
        this();

        if (file != null) {
            this.file = file;
            try {
                Scanner scan = new Scanner(file);

                while (scan.hasNextLine()) {
                    String line = scan.nextLine();
                    if (line.startsWith("#") || line.isEmpty()) continue;

                    try {
                        final int firstColonIndex = line.indexOf(':');
                        final int secondColonIndex = line.indexOf(':', firstColonIndex + 1);
                        final Key key = keyFromName(line.substring(0, firstColonIndex));
                        final String typeName = line.substring(firstColonIndex + 1, secondColonIndex);
                        final String valueString = line.substring(secondColonIndex + 1);

                        if (key == null) {
                            System.err.println("Settings tried to load unknown key in line: " + line);
                            continue;
                        }

                        if (typeName.equalsIgnoreCase("BOOLEAN")) {
                            setBoolean(key, Boolean.parseBoolean(valueString));
                        } else if (typeName.equalsIgnoreCase("STRING")) {
                            setString(key, valueString);
                        } else if (typeName.equalsIgnoreCase("DOUBLE")) {
                            setDouble(key, Double.parseDouble(valueString));
                        } else if (typeName.equalsIgnoreCase("INTEGER")) {
                            setInt(key, Integer.parseInt(valueString));
                        } else {
                            System.err.println("Settings tried to load unknown type from line: " + line);
                        }
                    } catch (Exception e) {
                        System.err.println("Error trying to read line: " + line);
                        e.printStackTrace();
                    }
                }

                scan.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    public Settings() {
        setBoolean(Key.USE_FILENAME_FROM_URL, true);
        setBoolean(Key.BACKUP_DATABASE, true);
        setBoolean(Key.WINDOW_MAXIMIZED, true);
        setBoolean(Key.COMBINE_TAGS, true);
        setBoolean(Key.DO_AUTO_IMPORT, false);
        setBoolean(Key.AUTO_IMPORT_MOVE_TO_DEFAULT, true);
        setBoolean(Key.MUTE_VIDEO, true);
        setBoolean(Key.REPEAT_VIDEO, true);
        setBoolean(Key.COMPARE_GREYSCALE, false);
        setInt(Key.GRID_WIDTH, 2);
        setInt(Key.WINDOW_WIDTH, Integer.MIN_VALUE);
        setInt(Key.WINDOW_HEIGHT, Integer.MIN_VALUE);
        setInt(Key.WINDOW_X, Integer.MIN_VALUE);
        setInt(Key.WINDOW_Y, Integer.MIN_VALUE);
        setString(Key.DEFAULT_FOLDER, null);
        setString(Key.DATABASE_URL, "~/menagerie");
        setString(Key.DATABASE_USER, "sa");
        setString(Key.DATABASE_PASSWORD, "");
        setString(Key.AUTO_IMPORT_FOLDER, null);
        setDouble(Key.CONFIDENCE, 0.95);
    }

    public void save(File file) throws FileNotFoundException {
        this.file = file;

        save();
    }

    public void save() throws FileNotFoundException {
        PrintWriter writer = new PrintWriter(file);

        writer.println("# ------------- Menagerie Settings --------------");
        writer.println("# " + DateTimeFormatter.ofLocalizedDateTime(FormatStyle.MEDIUM).withLocale(Locale.getDefault()).withZone(ZoneId.systemDefault()).format(new Date().toInstant()));

        for (Key key : vars.keySet()) {
            Property get = vars.get(key);
            if (get == null) continue;

            writer.print(key + ":");

            if (get instanceof BooleanProperty) {
                writer.print("BOOLEAN");
            } else if (get instanceof StringProperty) {
                writer.print("STRING");
            } else if (get instanceof DoubleProperty) {
                writer.print("DOUBLE");
            } else if (get instanceof IntegerProperty) {
                writer.print("INTEGER");
            }

            writer.print(":");

            if (get.getValue() != null) {
                writer.print(get.getValue());
            }

            writer.println();
        }

        writer.close();
    }

    private static Key keyFromName(String name) {
        for (Key key : Key.values()) {
            if (key.toString().equals(name)) return key;
        }

        return null;
    }

    public void setBoolean(Key key, boolean value) {
        BooleanProperty get = (BooleanProperty) vars.get(key);
        if (get != null) {
            get.setValue(value);
        } else {
            vars.put(key, new SimpleBooleanProperty(value));
        }
    }

    public void setString(Key key, String value) {
        StringProperty get = (StringProperty) vars.get(key);
        if (get != null) {
            get.setValue(value);
        } else {
            vars.put(key, new SimpleStringProperty(value));
        }
    }

    public void setDouble(Key key, double value) {
        DoubleProperty get = (DoubleProperty) vars.get(key);
        if (get != null) {
            get.setValue(value);
        } else {
            vars.put(key, new SimpleDoubleProperty(value));
        }
    }

    public void setInt(Key key, int value) {
        IntegerProperty get = (IntegerProperty) vars.get(key);
        if (get != null) {
            get.setValue(value);
        } else {
            vars.put(key, new SimpleIntegerProperty(value));
        }
    }

    public Property getProperty(Key key) {
        return vars.get(key);
    }

    public boolean getBoolean(Key key) {
        return ((BooleanProperty) vars.get(key)).get();
    }

    public String getString(Key key) {
        return ((StringProperty) vars.get(key)).get();
    }

    public double getDouble(Key key) {
        return ((DoubleProperty) vars.get(key)).get();
    }

    public int getInt(Key key) {
        return ((IntegerProperty) vars.get(key)).get();
    }

}
