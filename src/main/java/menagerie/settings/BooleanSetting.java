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

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import org.json.JSONObject;

public class BooleanSetting extends Setting {

    private static final String VALUE_KEY = "value";

    private static final String TYPE = "boolean";

    private final BooleanProperty value = new SimpleBooleanProperty();


    public BooleanSetting(String identifier, String label, boolean value) {
        this(identifier, label);
        this.value.set(value);
    }

    public BooleanSetting(String identifier, String label) {
        super(identifier, label);
    }

    public boolean getValue() {
        return value.get();
    }

    public void setValue(boolean value) {
        this.value.set(value);
    }

    public BooleanProperty valueProperty() {
        return value;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    @Override
    JSONObject toJSON() {
        JSONObject json = super.toJSON();

        json.put(VALUE_KEY, getValue());

        return json;
    }

    public static BooleanSetting fromJSON(JSONObject json) {
        if (!isValidSettingJSON(json, TYPE)) return null;

        BooleanSetting setting = null;
        if (json.getInt(VERSION_KEY) == 1) {
            setting = new BooleanSetting(json.getString(ID_KEY), json.getString(LABEL_KEY), json.getBoolean(VALUE_KEY));
        }

        return setting;
    }

    @Override
    public String toString() {
        return getType() + "(id:\"" + getID() + "\", label:\"" + getLabel() + "\", value:" + getValue() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof BooleanSetting && ((BooleanSetting) obj).getValue() == getValue();
    }

}
