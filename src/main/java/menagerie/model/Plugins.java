package menagerie.model;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import menagerie.ErrorListener;
import menagerie.MenageriePlugin;
import menagerie.duplicates.DuplicateFinder;

public class Plugins {

  /**
   * Folder containing plugins to be loaded on startup
   */
  private static final File pluginsFolder = new File("./plugins");

  private static final Logger LOGGER = Logger.getLogger(Plugins.class.getName());

  /**
   * List of loaded plugins. Populated during initialization
   */
  private List<MenageriePlugin> allPlugins = null;

  /**
   * Attempts to load plugins in the plugins folder and initialize them
   */
  public void initPlugins() {
    LOGGER.info("Loading plugins from: " + pluginsFolder.getAbsolutePath());
    allPlugins = PluginLoader.loadPlugins(pluginsFolder);
    allPlugins.forEach(plugin -> plugin.addErrorListener(new ErrorListener() {
      @Override
      public void postMessage(String s) {
        LOGGER.info(s);
      }

      @Override
      public void postException(String s, Exception e) {
        LOGGER.log(Level.SEVERE, s, e);
      }
    }));
    LOGGER.info(() -> "Loaded " + allPlugins.size() + " plugins");
  }

  public void closeAll() {
    allPlugins.forEach(plugin -> {
      LOGGER.info("Attempting to close plugin: " + plugin.getPluginName());
      plugin.close();
    });
  }

  public List<DuplicateFinder> getAllDuplicateFinders() {
    List<DuplicateFinder> finders = new ArrayList<>();
    for (MenageriePlugin plugin : allPlugins) {
      if (plugin instanceof DuplicateFinder) {
        finders.add((DuplicateFinder) plugin);
      }
    }
    return finders;
  }

}
