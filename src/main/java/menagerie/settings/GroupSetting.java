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
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;
import javafx.scene.text.Font;
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

    public GroupSetting disable() {
        setEnabled(false);
        return this;
    }

    public GroupSetting toggleable() {
        setToggleable(true);
        return this;
    }

    public GroupSetting hide() {
        setHidden(true);
        return this;
    }

    public GroupSetting tip(String tip) {
        setTip(tip);
        return this;
    }

    public GroupSetting label(String label) {
        setLabel(label);
        return this;
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
    public SettingNode makeJFXNode() {
        VBox root = new VBox(5);
        root.setFillWidth(true);
        VBox v = new VBox(5);
        v.setPadding(new Insets(0, 0, 0, 20));
        v.setDisable(!isEnabled());

        CheckBox checkBox = null;
        if (isToggleable()) {
            checkBox = new CheckBox(getLabel());
            checkBox.selectedProperty().addListener((observable, oldValue, newValue) -> v.setDisable(!newValue));
            checkBox.setSelected(isEnabled());
            checkBox.setFont(new Font("System Bold", Font.getDefault().getSize()));
            root.getChildren().add(checkBox);
        } else {
            Label label = new Label(getLabel());
            label.setFont(new Font("System Bold Italic", Font.getDefault().getSize()));
            root.getChildren().add(label);
        }

        root.getChildren().add(v);

        List<SettingNode> childNodes = new ArrayList<>();
        for (Setting child : getChildren()) {
            if (child.isHidden()) continue;

            SettingNode node = child.makeJFXNode();
            childNodes.add(node);
            v.getChildren().add(node.getNode());
        }

        final CheckBox finalCheckBox = checkBox;
        return new SettingNode() {
            @Override
            public void applyToSetting() {
                if (finalCheckBox != null) setEnabled(finalCheckBox.isSelected());
                childNodes.forEach(SettingNode::applyToSetting);
            }

            @Override
            public Node getNode() {
                return root;
            }
        };
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

        String label = null, tip = null;
        boolean hidden = false, toggleable = false, enabled = true;

        if (json.has(LABEL_KEY)) label = json.getString(LABEL_KEY);
        if (json.has(TIP_KEY)) tip = json.getString(TIP_KEY);
        if (json.has(HIDDEN_KEY)) hidden = json.getBoolean(HIDDEN_KEY);
        if (json.has(TOGGLEABLE_KEY)) toggleable = json.getBoolean(TOGGLEABLE_KEY);
        if (json.has(ENABLED_KEY)) enabled = json.getBoolean(ENABLED_KEY);

        GroupSetting group = new GroupSetting(json.getString(ID_KEY), label, tip, hidden, toggleable, enabled);

        if (json.has(CHILDREN_KEY)) {
            group.getChildren().addAll(Settings.parseArrayOfSettings(json.getJSONArray(CHILDREN_KEY)));
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
