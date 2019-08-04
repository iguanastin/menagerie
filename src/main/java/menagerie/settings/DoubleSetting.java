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

import javafx.beans.property.DoubleProperty;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import org.json.JSONObject;

public class DoubleSetting extends Setting {

    private static final String VALUE_KEY = "value";
    private static final String MIN_KEY = "min";
    private static final String MAX_KEY = "max";

    private static final String TYPE = "double";

    private final DoubleProperty value = new SimpleDoubleProperty();
    private double min = Double.MIN_VALUE, max = Double.MAX_VALUE;


    public DoubleSetting(String identifier, String label, String tip, boolean hidden, double value) {
        super(identifier, label, tip, hidden);
        this.value.set(value);
    }

    public DoubleSetting(String identifier, double value) {
        super(identifier);
        this.value.set(value);
    }

    public DoubleSetting(String identifier) {
        super(identifier);
    }

    public DoubleSetting hide() {
        setHidden(true);
        return this;
    }

    public DoubleSetting tip(String tip) {
        setTip(tip);
        return this;
    }

    public DoubleSetting label(String label) {
        setLabel(label);
        return this;
    }

    public DoubleSetting min(double min) {
        return range(min, getMax());
    }

    public DoubleSetting max(double max) {
        return range(getMin(), max);
    }

    public DoubleSetting range(double min, double max) {
        setRange(min, max);
        return this;
    }

    public double getMin() {
        return min;
    }

    public double getMax() {
        return max;
    }

    public void setMin(double min) {
        this.min = min;
        if (getValue() < min) setValue(min);
    }

    public void setMax(double max) {
        this.max = max;
        if (getValue() > max) setValue(max);
    }

    public void setRange(double min, double max) {
        if (min > max) {
            double temp = min;
            min = max;
            max = temp;
        }

        setMin(min);
        setMax(max);
    }

    public double getValue() {
        return value.get();
    }

    public void setValue(double value) {
        this.value.set(value);
    }

    public DoubleProperty valueProperty() {
        return value;
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public SettingNode makeJFXNode() {
        Label label = new Label(getLabel());
        TextField textfield = new TextField(getValue() + "");
        textfield.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                try {
                    double val = Double.parseDouble(textfield.getText());

                    if (val < getMin()) {
                        val = getMin();
                    } else if (val > getMax()) {
                        val = getMax();
                    }

                    setValue(val);
                    textfield.setText(val + "");
                } catch (NumberFormatException e) {
                    textfield.setText(getValue() + "");
                }
            }
        });
        if (getTip() != null && !getTip().isEmpty()) {
            textfield.setTooltip(new Tooltip(getTip()));
        }
        HBox h = new HBox(5, label, textfield);
        h.setAlignment(Pos.CENTER_LEFT);

        return new SettingNode() {
            @Override
            public void applyToSetting() {
                setValue(Double.parseDouble(textfield.getText()));
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

    public static DoubleSetting fromJSON(JSONObject json) {
        if (!isValidSettingJSON(json, TYPE)) return null;
        String label = null, tip = null;
        boolean hidden = false;
        double value = 0;

        if (json.has(LABEL_KEY)) label = json.getString(LABEL_KEY);
        if (json.has(TIP_KEY)) tip = json.getString(TIP_KEY);
        if (json.has(HIDDEN_KEY)) hidden = json.getBoolean(HIDDEN_KEY);
        if (json.has(VALUE_KEY)) value = json.getDouble(VALUE_KEY);

        DoubleSetting setting = new DoubleSetting(json.getString(ID_KEY), label, tip, hidden, value);
        if (json.has(MIN_KEY)) setting.setMin(json.getDouble(MIN_KEY));
        if (json.has(MAX_KEY)) setting.setMax(json.getDouble(MAX_KEY));

        return setting;
    }

    @Override
    public String toString() {
        return getType() + "(id:\"" + getID() + "\", label:\"" + getLabel() + "\", value:" + getValue() + ")";
    }

    @Override
    public boolean equals(Object obj) {
        return super.equals(obj) && obj instanceof DoubleSetting && ((DoubleSetting) obj).getValue() == getValue();
    }

}
