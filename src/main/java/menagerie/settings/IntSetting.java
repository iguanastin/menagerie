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

import javafx.beans.property.IntegerProperty;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.Spinner;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.json.JSONObject;

public class IntSetting extends Setting {

    private static final String VALUE_KEY = "value";
    private static final String MIN_KEY = "min";
    private static final String MAX_KEY = "max";

    private static final String TYPE = "int";

    private final IntegerProperty value = new SimpleIntegerProperty();
    private int min = Integer.MIN_VALUE, max = Integer.MAX_VALUE;


    public IntSetting(String identifier, String label, String tip, boolean hidden, int value) {
        super(identifier, label, tip, hidden);
        this.value.set(value);
    }

    public IntSetting(String identifier, int value) {
        super(identifier);
        this.value.set(value);
    }

    public IntSetting(String identifier) {
        super(identifier);
        this.value.set(0);
    }

    public IntSetting hide() {
        setHidden(true);
        return this;
    }

    public IntSetting tip(String tip) {
        setTip(tip);
        return this;
    }

    public IntSetting label(String label) {
        setLabel(label);
        return this;
    }

    public IntSetting min(int min) {
        return range(min, getMax());
    }

    public IntSetting max(int max) {
        return range(getMin(), max);
    }

    public IntSetting range(int min, int max) {
        setRange(min, max);
        return this;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    public void setMin(int min) {
        this.min = min;
        if (getValue() < min) setValue(min);
    }

    public void setMax(int max) {
        this.max = max;
        if (getValue() > max) setValue(max);
    }

    public void setRange(int min, int max) {
        if (min > max) {
            int temp = min;
            min = max;
            max = temp;
        }

        setMin(min);
        setMax(max);
    }

    public int getValue() {
        return value.get();
    }

    public void setValue(int value) {
        this.value.set(value);
    }

    public IntegerProperty valueProperty() {
        return value;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public SettingNode makeJFXNode() {
        Label label = new Label(getLabel());
        Spinner<Integer> spinner = new Spinner<>(getMin(), getMax(), getValue());
        spinner.setEditable(true);
        spinner.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    int v = Integer.parseInt(spinner.getEditor().getText());
                    if (v < getMin() || v > getMax()) {
                        v = Math.min(Math.max(getMin(), v), getMax());
                    }
                    setValue(v);
                } catch (NumberFormatException ignore) {
                }

                spinner.getValueFactory().setValue(getValue());
                spinner.getEditor().setText(spinner.getValue() + "");
            }
        });
        if (getTip() != null && !getTip().isEmpty()) {
            spinner.setTooltip(new Tooltip(getTip()));
        }
        HBox h = new HBox(5, label, spinner);
        h.setAlignment(Pos.CENTER_LEFT);

        return new SettingNode() {
            @Override
            public void applyToSetting() {
                setValue(spinner.getValue());
            }

            @Override
            public Node getNode() {
                return h;
            }
        };
    }

    @Override
    JSONObject toJSON() {
        JSONObject json = super.toJSON();

        json.put(VALUE_KEY, getValue());
        json.put(MIN_KEY, getMin());
        json.put(MAX_KEY, getMax());

        return json;
    }

    public static IntSetting fromJSON(JSONObject json) {
        if (!isValidSettingJSON(json, TYPE)) return null;

        String label = null, tip = null;
        boolean hidden = false;
        int value = 0;

        if (json.has(LABEL_KEY)) label = json.getString(LABEL_KEY);
        if (json.has(TIP_KEY)) tip = json.getString(TIP_KEY);
        if (json.has(HIDDEN_KEY)) hidden = json.getBoolean(HIDDEN_KEY);
        if (json.has(VALUE_KEY)) value = json.getInt(VALUE_KEY);

        IntSetting setting = new IntSetting(json.getString(ID_KEY), label, tip, hidden, value);

        if (json.has(MIN_KEY)) setting.setMin(json.getInt(MIN_KEY));
        if (json.has(MAX_KEY)) setting.setMax(json.getInt(MAX_KEY));

        return setting;
    }

    @Override
    public String toString() {
        return getType() + "(id:\"" + getID() + "\", label:\"" + getLabel() + "\", value:" + getValue() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof IntSetting && ((IntSetting) obj).getValue() == getValue();
    }

}
