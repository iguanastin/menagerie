package menagerie.gui.errors;

import java.util.Date;

public class TrackedError {

    public enum Severity {
        NORMAL,
        HIGH
    }

    private final Exception exception;
    private final String title, whatHappened, likelyCause;
    private final Severity severity;
    private final Date date = new Date();

    private boolean showExpanded = false;


    public TrackedError(Exception exception, Severity severity, String title, String whatHappened, String likelyCause) {
        this.exception = exception;
        this.severity = severity;
        this.title = title;
        this.whatHappened = whatHappened;
        this.likelyCause = likelyCause;
    }

    Exception getException() {
        return exception;
    }

    Severity getSeverity() {
        return severity;
    }

    String getTitle() {
        return title;
    }

    String getWhatHappened() {
        return whatHappened;
    }

    String getLikelyCause() {
        return likelyCause;
    }

    Date getDate() {
        return date;
    }

    boolean isShowExpanded() {
        return showExpanded;
    }

    void setShowExpanded(boolean showExpanded) {
        this.showExpanded = showExpanded;
    }

}
