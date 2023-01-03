package menagerie.gui.handler;

import java.util.Date;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import menagerie.gui.screens.log.LogItem;
import menagerie.gui.screens.log.LogScreen;

public class LoggerHandler extends Handler {

  private final BooleanProperty errorLog;
  private final BooleanProperty warningLog;
  private final LogScreen logScreen;

  public LoggerHandler(BooleanProperty errorLog, BooleanProperty warningLog,
                       LogScreen logScreen) {
    this.errorLog = errorLog;
    this.warningLog = warningLog;
    this.logScreen = logScreen;
  }

  @Override
  public void publish(LogRecord logRecord) {
    StringBuilder work = new StringBuilder(new Date(logRecord.getMillis()).toString());
    work.append(" [").append(logRecord.getLevel()).append("]: ").append(logRecord.getMessage());
    if (logRecord.getThrown() != null) {
      work.append("\n").append(logRecord.getThrown().toString());
      for (StackTraceElement e : logRecord.getThrown().getStackTrace()) {
        work.append("\n    at ").append(e);
      }
    }
    Platform.runLater(() -> {
      LogItem item = new LogItem(work.toString(), logRecord.getLevel());
      if (item.getLevel() == Level.SEVERE) {
        errorLog.set(true);
      } else if (item.getLevel() == Level.WARNING) {
        warningLog.set(true);
      }
      logScreen.getListView().getItems().add(item);
      if (this.logScreen.getListView().getItems().size() > 1000) {
        this.logScreen.getListView().getItems().remove(0);
      }
    });
  }

  @Override
  public void flush() {
  }

  @Override
  public void close() throws SecurityException {
  }
}
