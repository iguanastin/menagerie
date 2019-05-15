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

import org.json.JSONObject;

public abstract class Setting {

    static final String TYPE_KEY = "type";
    static final String ID_KEY = "id";
    static final String LABEL_KEY = "label";
    static final String VERSION_KEY = "version";

    private final String id;
    private String label;


    public Setting(String id, String label) {
        this.id = id;
        this.label = label;
    }

    public String getID() {
        return id;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public abstract String getType();

    public abstract int getVersion();

    @Override
    public boolean equals(Object obj) {
        if (obj instanceof Setting) {
            Setting s = (Setting) obj;
            String id1 = getID(), id2 = s.getID();
            String l1 = getLabel(), l2 = s.getLabel();
            String t1 = getType(), t2 = s.getType();
            int v1 = getVersion(), v2 = s.getVersion();
            return id1.equals(id2) && l1.equals(l2) && t1.equals(t2) && v1 == v2;
        }

        return false;
    }

    static boolean isValidSettingJSON(JSONObject json, String expectedType) {
        return json.has(TYPE_KEY) && json.has(VERSION_KEY) && json.has(LABEL_KEY) && json.has(ID_KEY) && json.getString(TYPE_KEY).equals(expectedType);
    }

    JSONObject toJSON() {
        JSONObject json = new JSONObject();
        json.put(ID_KEY, getID());
        json.put(TYPE_KEY, getType());
        json.put(LABEL_KEY, getLabel());
        json.put(VERSION_KEY, getVersion());
        return json;
    }

}
