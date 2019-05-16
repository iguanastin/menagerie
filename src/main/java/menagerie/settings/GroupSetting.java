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

import java.util.ArrayList;
import java.util.List;

public class GroupSetting extends Setting {

    private static final String TOGGLEABLE_KEY = "toggleable";
    private static final String ENABLED_KEY = "enabled";
    private static final String CHILDREN_KEY = "children";

    private static final String TYPE = "group";

    private final List<Setting> children = new ArrayList<>();
    private final BooleanProperty enabled = new SimpleBooleanProperty(true);
    private final BooleanProperty toggleable = new SimpleBooleanProperty(false);


    public GroupSetting(String identifier, String label, String tip, boolean hidden, boolean toggleable, boolean enabled) {
        super(identifier, label, tip, hidden);
        this.toggleable.set(toggleable);
        this.enabled.set(enabled);
    }

    public GroupSetting(String identifier) {
        super(identifier);
    }

    public List<Setting> getChildren() {
        return children;
    }

    public boolean isToggleable() {
        return toggleable.get();
    }

    public void setToggleable(boolean toggleable) {
        this.toggleable.set(toggleable);
    }

    public BooleanProperty toggleableProperty() {
        return toggleable;
    }

    public boolean isEnabled() {
        return enabled.get();
    }

    public void setEnabled(boolean b) {
        enabled.set(b);
    }

    public BooleanProperty enabledProperty() {
        return enabled;
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
        json.put(TOGGLEABLE_KEY, isToggleable());
        json.put(ENABLED_KEY, isEnabled());
        for (Setting setting : getChildren()) {
            json.append(CHILDREN_KEY, setting.toJSON());
        }
        return json;
    }

    public static GroupSetting fromJSON(JSONObject json) {
        if (!isValidSettingJSON(json, TYPE)) return null;

        GroupSetting group = null;
        if (json.getInt(VERSION_KEY) == 1) {
            String label = null, tip = null;
            boolean hidden = false, toggleable = false, enabled = true;

            if (json.has(LABEL_KEY)) label = json.getString(LABEL_KEY);
            if (json.has(TIP_KEY)) tip = json.getString(TIP_KEY);
            if (json.has(HIDDEN_KEY)) hidden = json.getBoolean(HIDDEN_KEY);
            if (json.has(TOGGLEABLE_KEY)) toggleable = json.getBoolean(TOGGLEABLE_KEY);
            if (json.has(ENABLED_KEY)) enabled = json.getBoolean(ENABLED_KEY);

            group = new GroupSetting(json.getString(ID_KEY), label, tip, hidden, toggleable, enabled);

            if (json.has(CHILDREN_KEY)) {
                group.getChildren().addAll(Settings.parseArrayOfSettings(json.getJSONArray(CHILDREN_KEY)));
            }
        }

        return group;
    }

    @Override
    public String toString() {
        return getType() + "(id:\"" + getID() + "\", label:\"" + getLabel() + "\", toggleable:" + isToggleable() + ", enabled:" + isEnabled() + ", children:" + getChildren().size() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        if (super.equals(obj) && obj instanceof GroupSetting && ((GroupSetting) obj).isToggleable() == isToggleable() && ((GroupSetting) obj).isEnabled() == isEnabled()) {
            if (((GroupSetting) obj).getChildren().size() != getChildren().size()) return false;
            for (int i = 0; i < getChildren().size(); i++) {
                if (!getChildren().get(i).equals(((GroupSetting) obj).getChildren().get(i))) {
                    return false;
                }
            }

            return true;
        }

        return false;
    }

}
