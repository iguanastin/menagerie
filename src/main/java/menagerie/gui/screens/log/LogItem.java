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

package menagerie.gui.screens.log;

import java.util.logging.Level;

/**
 * A class representing a single log item.
 */
public class LogItem {

    private Level level;
    private String text;


    /**
     * Creates a log item with default CSS.
     *
     * @param text Log message.
     */
    public LogItem(String text) {
        this.text = text;
        level = Level.INFO;
    }

    /**
     * Creates a log item with specified CSS
     *
     * @param text  Log message.
     * @param level Log level.
     */
    public LogItem(String text, Level level) {
        this(text);
        this.level = level;
    }

    /**
     * @return Severity of log
     */
    public Level getLevel() {
        return level;
    }

    /**
     * @return The log message.
     */
    public String getText() {
        return text;
    }

}
