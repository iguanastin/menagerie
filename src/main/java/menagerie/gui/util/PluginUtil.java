package menagerie.gui.util;

import java.util.List;
import java.util.logging.Logger;
import menagerie.MenageriePlugin;

public class PluginUtil {

  private PluginUtil() {
  }

  private static final Logger LOGGER = Logger.getLogger(PluginUtil.class.getName());

  public static void closeAll(List<MenageriePlugin> plugins) {
    plugins.forEach(plugin -> {
      LOGGER.info("Attempting to close plugin: " + plugin.getPluginName());
      plugin.close();
    });
  }

}
