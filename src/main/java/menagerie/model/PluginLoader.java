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
import java.util.logging.Logger;

public abstract class PluginLoader {

    private static final Logger LOGGER = Logger.getLogger(PluginLoader.class.getName());


    public static List<MenageriePlugin> loadPlugins(File folder) {
        if (!folder.exists() || !folder.isDirectory()) {
            if (!folder.mkdirs()) {
                LOGGER.severe("Unable to find/create plugin directory: " + folder.getAbsolutePath());
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
                    LOGGER.info("Found plugin JAR: " + file);
                    classes.add(mainClass);
                    urls.add(new URL("jar:file:" + folder + "/" + file.getName() + "!/"));
                }
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Error reading plugin jarfile", e);
            }
        }

        List<MenageriePlugin> plugins = new ArrayList<>();

        URLClassLoader classLoader = new URLClassLoader(urls.toArray(new URL[0]));
        classes.forEach(className -> {
            try {
                Class c = classLoader.loadClass(className);
                for (Class anInterface : c.getInterfaces()) {
                    if (anInterface == MenageriePlugin.class) {
                        MenageriePlugin plugin = (MenageriePlugin) c.newInstance();
                        plugins.add(plugin);
                        LOGGER.info("Loaded plugin: " + plugin.getPluginName());
                        break;
                    }
                }
            } catch (IllegalAccessException | InstantiationException | ClassNotFoundException | NoClassDefFoundError e) {
                LOGGER.log(Level.SEVERE, "Failed to load plugin class: " + className, e);
            }
        });

        return plugins;
    }

}
