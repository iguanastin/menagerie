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

public class FileSetting extends StringSetting {

    private static final String TYPE = "file";


    public FileSetting(String id, String label, String filepath) {
        super(id, label, filepath);
    }

    public FileSetting(String id, String label) {
        super(id, label);
    }

    @Override
    public String getType() {
        return TYPE;
    }

    @Override
    public int getVersion() {
        return 1;
    }

    public static FileSetting fromJSON(JSONObject json) {
        if (!isValidSettingJSON(json, TYPE)) return null;

        FileSetting setting = null;
        if (json.getInt(VERSION_KEY) == 1) {
            setting = new FileSetting(json.getString(ID_KEY), json.getString(LABEL_KEY), json.getString(VALUE_KEY));
        }

        return setting;
    }

}
