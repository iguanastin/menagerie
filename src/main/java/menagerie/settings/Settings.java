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

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Settings {

    private static final String VERSION_KEY = "version";

    protected static final List<Class<? extends Setting>> PARSABLE_SETTINGS = new ArrayList<>(Arrays.asList(GroupSetting.class, IntSetting.class, DoubleSetting.class, BooleanSetting.class, StringSetting.class, FileSetting.class, FolderSetting.class));

    private final List<Setting> settings = new ArrayList<>();


    public Setting getSetting(String identifier) {
        List<Setting> list = new ArrayList<>(settings);
        for (int i = 0; i < list.size(); i++) {
            Setting setting = list.get(i);
            if (setting.getID().equalsIgnoreCase(identifier)) {
                return setting;
            } else if (setting instanceof GroupSetting) {
                list.addAll(((GroupSetting) setting).getChildren());
            }
        }

        return null;
    }

    public List<Setting> getSettings() {
        return settings;
    }

    public int getVersion() {
        return 1;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Settings) {
            if (getVersion() != ((Settings) obj).getVersion()) return false;
            if (getSettings().size() != ((Settings) obj).getSettings().size()) return false;

            for (int i = 0; i < getSettings().size(); i++) {
                if (!getSettings().get(i).equals(((Settings) obj).getSettings().get(i))) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

    public static Settings load(File file) throws IOException {
        String fileText = String.join("\n", Files.readAllLines(file.toPath()));
        JSONObject json = new JSONObject(fileText);

        Settings settings = new Settings();

        if (!json.has(VERSION_KEY)) return settings; // No version defined, nothing to load.

        int version = json.getInt(VERSION_KEY); // Use this if there is ever a need for versioning in the future.

        if (json.has("settings")) {
            JSONArray settingsArray = json.getJSONArray("settings");

            settings.getSettings().addAll(parseArrayOfSettings(settingsArray));
        }

        return settings;
    }

    static List<Setting> parseArrayOfSettings(JSONArray settingsArray) {
        List<Setting> results = new ArrayList<>();

        for (int i = 0; i < settingsArray.length(); i++) {
            boolean parsed = false;
            for (Class<? extends Setting> parsableSetting : PARSABLE_SETTINGS) {
                try {
                    Method method = parsableSetting.getMethod("fromJSON", JSONObject.class);
                    Setting setting = (Setting) method.invoke(null, settingsArray.getJSONObject(i));
                    if (setting != null) {
                        results.add(setting);
                        parsed = true;
                        break;
                    }
                } catch (Exception ignore) {
                }
            }

            if (!parsed) {
                System.out.println("Unable to parse into setting: " + settingsArray.get(i));
            }
        }

        return results;
    }

    public void save(File file) throws IOException {
        JSONObject json = new JSONObject();
        json.put(VERSION_KEY, getVersion());

        for (Setting setting : settings) {
            json.append("settings", setting.toJSON());
        }

        try (FileWriter fw = new FileWriter(file)) {
            json.write(fw, 2, 0);
        }
    }

}
