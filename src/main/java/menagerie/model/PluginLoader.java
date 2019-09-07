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

package menagerie.model;

import menagerie.MenageriePlugin;
import menagerie.gui.Main;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.logging.Level;

public abstract class PluginLoader {

    public static List<MenageriePlugin> loadPlugins(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            if (!folder.mkdirs()) {
                Main.log.severe("Unable to find/create plugin directory: " + folder.getAbsolutePath());
                return new ArrayList<>();
            }
        }

        List<URL> urls = new ArrayList<>();
        List<String> classes = new ArrayList<>();

        for (File file : Objects.requireNonNull(folder.listFiles((dir, name) -> name.endsWith(".jar")))) {
            try {
                JarFile jar = new JarFile(file);
                String mainClass = jar.getManifest().getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);
                if (mainClass != null) {
                    classes.add(mainClass);
                    urls.add(new URL("jar:file:" + folder + "/" + file.getName() + "!/"));
                }
                //                jar.stream().filter(jarEntry -> jarEntry.getName().endsWith(".class")).forEach(jarEntry -> classes.add(jarEntry.getName().replaceAll("/", ".").replace(".class", "")));
            } catch (IOException e) {
                Main.log.log(Level.SEVERE, "Error reading plugin jarfile", e);
            }
        }

        List<MenageriePlugin> plugins = new ArrayList<>();

        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]));
        classes.forEach(className -> {
            try {
                Class c = classLoader.loadClass(className);
                for (Class anInterface : c.getInterfaces()) {
                    if (anInterface == MenageriePlugin.class) {
                        plugins.add((MenageriePlugin) c.newInstance());
                        break;
                    }
                }
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | NoClassDefFoundError e) {
                Main.log.log(Level.SEVERE, "Failed to load plugin class: " + className, e);
            }
        });

        return plugins;
    }

}