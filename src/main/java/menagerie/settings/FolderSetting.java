package menagerie.settings;

import org.json.JSONObject;

public class FolderSetting extends StringSetting {

    private static final String TYPE = "folder";


    public FolderSetting(String identifier, String label, String value) {
        super(identifier, label, value);
    }

    public FolderSetting(String identifier, String label) {
        super(identifier, label);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    public static FolderSetting fromJSON(JSONObject json) {
        if (!isValidSettingJSON(json, TYPE)) return null;

        FolderSetting setting = null;
        if (json.getInt(VERSION_KEY) == 1) {
            setting = new FolderSetting(json.getString(ID_KEY), json.getString(LABEL_KEY), json.getString(VALUE_KEY));
        }

        return setting;
    }

}
