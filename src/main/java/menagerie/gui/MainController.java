/*
 MIT License

 Copyright (c) 2020. Austin Thompson

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

package menagerie.gui;

import com.mortennobel.imagescaling.AdvancedResizeOp;
import com.mortennobel.imagescaling.ResampleFilters;
import com.mortennobel.imagescaling.ResampleOp;
import javafx.application.Platform;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.BooleanPropertyBase;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.ListChangeListener;
import javafx.css.PseudoClass;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.input.*;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import menagerie.ErrorListener;
import menagerie.MenageriePlugin;
import menagerie.duplicates.DuplicateFinder;
import menagerie.gui.grid.ItemGridCell;
import menagerie.gui.grid.ItemGridView;
import menagerie.gui.media.DynamicMediaView;
import menagerie.gui.media.DynamicVideoView;
import menagerie.gui.predictive.PredictiveTextField;
import menagerie.gui.screens.HelpScreen;
import menagerie.gui.screens.ScreenPane;
import menagerie.gui.screens.SlideshowScreen;
import menagerie.gui.screens.TagListScreen;
import menagerie.gui.screens.dialogs.*;
import menagerie.gui.screens.duplicates.DuplicateOptionsScreen;
import menagerie.gui.screens.findonline.FindOnlineScreen;
import menagerie.gui.screens.importer.ImporterScreen;
import menagerie.gui.screens.log.LogItem;
import menagerie.gui.screens.log.LogListCell;
import menagerie.gui.screens.log.LogScreen;
import menagerie.gui.screens.move.MoveFilesScreen;
import menagerie.gui.screens.settings.SettingsScreen;
import menagerie.gui.taglist.SimpleCSSColorPicker;
import menagerie.gui.taglist.TagListCell;
import menagerie.model.PluginLoader;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.*;
import menagerie.model.menagerie.db.DatabaseManager;
import menagerie.model.menagerie.importer.ImportJob;
import menagerie.model.menagerie.importer.ImporterThread;
import menagerie.model.menagerie.server.APIServer;
import menagerie.model.search.GroupSearch;
import menagerie.model.search.Search;
import menagerie.model.search.SearchHistory;
import menagerie.settings.MenagerieSettings;
import menagerie.util.CancellableThread;
import menagerie.util.Filters;
import menagerie.util.folderwatcher.FolderWatcherThread;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.sql.SQLException;
import java.util.List;
import java.util.*;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

public class MainController {

    /**
     * Logger for this class
     */
    private static final Logger LOGGER = Logger.getLogger(MainController.class.getName());

    // ------------------------------- JFX -------------------------------------------
    public StackPane rootPane;
    public BorderPane explorerRootPane;
    public ScreenPane screenPane;
    public MenuBar menuBar;

    // Right side
    public BorderPane gridPane;
    public PredictiveTextField searchTextField;
    public ToggleButton listDescendingToggleButton;
    public ToggleButton showGroupedToggleButton;
    public ToggleButton shuffledSearchButton;
    public ItemGridView itemGridView;
    public Label resultCountLabel;
    public Label scopeLabel;
    public HBox scopeHBox;
    public Button importsButton;
    public Label dbUpdatesLabel;
    public Button logButton;
    public Button backButton;

    // Left side
    public PredictiveTextField editTagsTextField;
    public DynamicMediaView previewMediaView;
    public ItemInfoBox itemInfoBox;
    public ListView<Tag> tagListView;
    public Label explorerZoomLabel;

    // ----------------------------------- Screens -----------------------------------
    private TagListScreen tagListScreen;
    private HelpScreen helpScreen;
    private SlideshowScreen slideshowScreen;
    private SettingsScreen settingsScreen;
    private ImporterScreen importerScreen;
    private LogScreen logScreen;
    private DuplicateOptionsScreen duplicateOptionsScreen;
    private ImportDialogScreen importDialogScreen;
    private GroupDialogScreen groupDialogScreen;
    private MoveFilesScreen moveFilesScreen;
    private FindOnlineScreen findOnlineScreen;

    // --------------------------------- Menagerie vars ------------------------------
    /**
     * The Menagerie environment this application is using.
     */
    private Menagerie menagerie;
    /**
     * History of tag edit events.
     */
    private final Stack<TagEditEvent> tagEditHistory = new Stack<>();
    /**
     * List of loaded plugins. Populated during initialization
     */
    private List<MenageriePlugin> plugins = null;

    // ------------------------------- Explorer screen vars --------------------------
    /**
     * Clipboard content object used by this application.
     */
    private final ClipboardContent clipboard = new ClipboardContent();
    /**
     * Current search that is active and being shown in the item grid.
     */
    private Search currentSearch = null;
    /**
     * Item that is currently being displayed in the preview viewport.
     */
    private Item currentlyPreviewing = null;
    /**
     * The last string that was used to edit tags.
     */
    private String lastEditTagString = null;
    /**
     * Variable used to track drag status of items from the item grid.
     */
    private boolean itemGridViewDragging = false;
    /**
     * Search history stack
     */
    private final Stack<SearchHistory> searchHistory = new Stack<>();
    /**
     * Tag listener used to update tag list for currently previewed item
     */
    private final ListChangeListener<Tag> previewTagListener = c -> {
        while (c.next()) {
            tagListView.getItems().addAll(c.getAddedSubList());
            tagListView.getItems().removeAll(c.getRemoved());
            tagListView.getItems().sort(Comparator.comparing(Tag::getName));
        }
    };
    /**
     * Search listener listening for items being added or removed from the current search
     */
    private final ListChangeListener<Item> searchChangeListener = c -> {
        while (c.next()) {
            // Added
            if (c.wasAdded()) {
                itemGridView.getItems().addAll(0, c.getAddedSubList()); // TODO: Insert these in the right position or sort it or something.
            }

            // Removed
            if (c.wasRemoved()) {
                if (c.getRemoved().contains(currentlyPreviewing)) previewItem(null);

                final int oldLastIndex = itemGridView.getItems().indexOf(itemGridView.getLastSelected());
                int newIndex = oldLastIndex;
                for (Item item : c.getRemoved()) {
                    final int i = itemGridView.getItems().indexOf(item);
                    if (i < 0) continue;

                    if (i < oldLastIndex) {
                        newIndex--;
                    }
                }

                itemGridView.getItems().removeAll(c.getRemoved());

                if (!itemGridView.getItems().isEmpty() && itemGridView.getSelected().isEmpty()) {
                    if (newIndex >= itemGridView.getItems().size()) newIndex = itemGridView.getItems().size() - 1;
                    if (newIndex >= 0) itemGridView.select(itemGridView.getItems().get(newIndex), false, false);
                }
            }
        }
    };

    /**
     * Log button pseudoclass for unread error state
     */
    private final PseudoClass logErrorPseudoClass = PseudoClass.getPseudoClass("error");
    /**
     * Log button pseudoclass for unread warning state
     */
    private final PseudoClass logWarningPseudoClass = PseudoClass.getPseudoClass("warning");
    /**
     * Flag set when an unread error log is present
     */
    private final BooleanProperty logError = new BooleanPropertyBase() {
        @Override
        protected void invalidated() {
            logButton.pseudoClassStateChanged(logErrorPseudoClass, get());
        }

        @Override
        public Object getBean() {
            return logButton.getClass();
        }

        @Override
        public String getName() {
            return "error";
        }
    };
    /**
     * Flag set when an unread warning log is present
     */
    private final BooleanProperty logWarning = new BooleanPropertyBase() {
        @Override
        protected void invalidated() {
            logButton.pseudoClassStateChanged(logWarningPseudoClass, get());
        }

        @Override
        public Object getBean() {
            return logButton.getClass();
        }

        @Override
        public String getName() {
            return "warning";
        }
    };

    // --------------------------------- Threads -------------------------------------
    /**
     * Importer thread for the menagerie. Main pipeline for adding any new items.
     */
    private ImporterThread importer;
    /**
     * Current folder watcher thread, may be null. Thread monitors a folder for new files and sends them to the importer.
     */
    private FolderWatcherThread folderWatcherThread = null;
    /**
     * The API server
     */
    private APIServer apiServer = null;

    // ---------------------------------- Settings var -------------------------------
    /**
     * Settings object used by this application.
     */
    private final MenagerieSettings settings;
    /**
     * Folder containing plugins to be loaded on startup
     */
    private static final File pluginsFolder = new File("./plugins");

    // ------------------------------ Video preview status ---------------------------
    /**
     * Variable used to track if a video should be played after the player regains focus.
     */
    private boolean playVideoAfterFocusGain = false;
    /**
     * Variable used to track if a video should be played after the explorer regains focus.
     */
    private boolean playVideoAfterExplorerEnabled = false;


    private long lastDBLabelUpdates = 0;


    // --------------------------------- Constructor ---------------------------------

    /**
     * Constructs this controller with a given environment
     *
     * @param menagerie Menagerie of the environment
     * @param settings  Settings for the controller and environment
     */
    MainController(Menagerie menagerie, MenagerieSettings settings) {
        this.menagerie = menagerie;
        this.settings = settings;
    }

    // ---------------------------------- Initializers -------------------------------

    /**
     * Initializes this controller and elements
     */
    @FXML
    public void initialize() {

        // Initialize the api server
        initAPIServer();

        // Initialize the importer
        initImporterThread();

        // Init screens
        initScreens();

        // Init plugins
        initPlugins();

        initDatabaseUpdateCounter();

        // Things to run on first "tick"
        Platform.runLater(() -> {
            //Apply window props and listeners
            initWindowPropertiesAndListeners();

            initAltTabbingFix();

            //Init closeRequest handling on window
            rootPane.getScene().getWindow().setOnCloseRequest(event -> cleanExit(false));

            // Apply a default search
            applySearch(null, null, listDescendingToggleButton.isSelected(), showGroupedToggleButton.isSelected(), shuffledSearchButton.isSelected());

            // Init folder watcher
            if (settings.autoImportGroup.isEnabled())
                startWatchingFolderForImages(settings.autoImportFolder.getValue(), settings.autoImportMove.getValue());

            // Show help screen if setting is set
            if (settings.helpOnStart.getValue()) {
                screenPane.open(helpScreen);
                settings.helpOnStart.setValue(false);
            }

            // Init user-accepted filetypes in filters
            if (settings.userFileTypes.getValue() != null && !settings.userFileTypes.getValue().trim().isEmpty()) {
                Filters.USER_EXTS.addAll(Arrays.asList(settings.userFileTypes.getValue().trim().split(" ")));
            }
            settings.userFileTypes.valueProperty().addListener((observable, oldValue, newValue) -> {
                Filters.USER_EXTS.clear();

                if (newValue != null && !newValue.isEmpty()) {
                    Filters.USER_EXTS.addAll(Arrays.asList(newValue.trim().split(" ")));
                }
            });
        });
    }

    private void initAPIServer() {
        apiServer = new APIServer(menagerie, settings.apiPageSize.getValue());
        try {
            apiServer.start(settings.apiPort.getValue());
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "Error while starting API server", e);
        }

        settings.apiPageSize.valueProperty().addListener((observable, oldValue, newValue) -> apiServer.setPageSize(newValue.intValue()));
        settings.apiPort.valueProperty().addListener((observable, oldValue, newValue) -> {
            apiServer.stop();
            try {
                apiServer.start(newValue.intValue());
            } catch (IOException e) {
                LOGGER.log(Level.SEVERE, "Failed to restart API server with new port: " + newValue.intValue(), e);
            }
        });
    }

    private void initDatabaseUpdateCounter() {
        menagerie.getDatabaseManager().setQueueSizeListener(count -> {
            if (count == 0 || System.currentTimeMillis() - lastDBLabelUpdates > 17) {
                lastDBLabelUpdates = System.currentTimeMillis();
                Platform.runLater(() -> dbUpdatesLabel.setText("Queued DB updates: " + count));
            }
        });
    }

    /**
     * Initializes a workaround for the garbage implementation of alt tabbing with toolbar menus in JavaFX
     */
    private void initAltTabbingFix() {
        LOGGER.info("Initializing alt-tab fix");
        rootPane.getScene().addEventFilter(KeyEvent.KEY_RELEASED, event -> {
            // Workaround for alt-tabbing correctly
            if (event.getCode() == KeyCode.ALT) {
                if (menuBar.isFocused()) {
                    itemGridView.requestFocus();
                } else {
                    menuBar.requestFocus();
                }
                event.consume();
            }
        });
        rootPane.getScene().addEventFilter(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ALT) event.consume();
        });
    }

    /**
     * Attempts to load plugins in the plugins folder and initialize them
     */
    private void initPlugins() {
        LOGGER.info("Loading plugins from: " + pluginsFolder.getAbsolutePath());
        plugins = PluginLoader.loadPlugins(pluginsFolder);
        plugins.forEach(plugin -> {
            plugin.addErrorListener(new ErrorListener() {
                @Override
                public void postMessage(String s) {
                    LOGGER.info(s);
                }

                @Override
                public void postException(String s, Exception e) {
                    LOGGER.log(Level.SEVERE, s, e);
                }
            });
        });
        LOGGER.info("Loaded " + plugins.size() + " plugins");
    }

    /**
     * Initializes screen objects for the ScreenPane
     */
    private void initScreens() {
        LOGGER.info("Initializing screens");

        // Explorer. Base screen.
        initExplorer();

        // SettingsScreen
        settingsScreen = new SettingsScreen();

        // TagListScreen
        tagListScreen = new TagListScreen();
        tagListScreen.setCellFactory(param -> {
            TagListCell c = new TagListCell(thing -> {
                String text = searchTextField.getText();
                if (text == null) text = "";
                text = text.trim();
                if (Arrays.asList(text.toLowerCase().split(" ")).contains("-" + thing.getName().toLowerCase())) {
                    text = text.replaceAll("(^|\\s)-" + thing.getName(), " ");
                } else if (!Arrays.asList(text.toLowerCase().split(" ")).contains(thing.getName().toLowerCase())) {
                    if (!text.isEmpty() && !text.endsWith(" ")) text += " ";
                    text += thing.getName() + " ";
                }
                searchTextField.setText(text.trim());
                searchTextField.requestFocus();
                searchTextField.positionCaret(text.length());
            }, thing -> {
                String text = searchTextField.getText();
                if (text == null) text = "";
                text = text.trim();
                if (Arrays.asList(text.toLowerCase().split(" ")).contains(thing.getName().toLowerCase())) {
                    text = text.replaceAll("(^|\\s)" + thing.getName(), " ");
                } else if (!Arrays.asList(text.toLowerCase().split(" ")).contains("-" + thing.getName().toLowerCase())) {
                    if (!text.isEmpty() && !text.endsWith(" ")) text += " ";
                    text += "-" + thing.getName() + " ";
                }
                searchTextField.setText(text.trim());
                searchTextField.requestFocus();
                searchTextField.positionCaret(text.length());
            });
            c.setOnContextMenuRequested(event -> {
                MenuItem i0 = new MenuItem("Add note");
                i0.setOnAction(event1 -> new TextDialogScreen().open(screenPane, "Add a note", String.format("Add a note to tag '%s'", c.getItem().getName()), null, note -> c.getItem().addNote(note), null));
                ContextMenu m = new ContextMenu(i0, new SeparatorMenuItem());
                m.show(c.getScene().getWindow(), event.getScreenX(), event.getScreenY());
            });
            c.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && c.getItem() != null) {
                    searchTextField.setText(c.getItem().getName() + " ");
                    searchTextField.positionCaret(searchTextField.getText().length());
                }
            });
            return c;
        });

        // SlideshowScreen
        initSlideshowScreen();

        // HelpScreen
        helpScreen = new HelpScreen();

        // DuplicatesScreen
        initDuplicatesScreen();

        // ImportScreen
        initImportScreen();

        // LogScreen
        initLogScreen();

        // ImportDialogScreen
        importDialogScreen = new ImportDialogScreen(settings, menagerie, importer);

        // GroupDialogScreen
        groupDialogScreen = new GroupDialogScreen();
        groupDialogScreen.tagTagmeProperty().bind(settings.tagTagme.valueProperty());

        // MoveFilesScreen
        moveFilesScreen = new MoveFilesScreen();

        // FindOnlineScreen
        findOnlineScreen = new FindOnlineScreen();
        findOnlineScreen.loadAheadProperty().bind(settings.onlineLoadAhead.valueProperty());

        // Init disabling explorer when screen is open
        screenPane.currentProperty().addListener((observable, oldValue, newValue) -> explorerRootPane.setDisable(newValue != null));
    }

    /**
     * Initializes the import screen, which displays queued/in-progress/failed imports
     */
    private void initImportScreen() {
        importerScreen = new ImporterScreen(importer, pairs -> duplicateOptionsScreen.getDuplicatesScreen().open(screenPane, menagerie, pairs), this::selectItemInGridView);

        final PseudoClass hasImportsPseudoClass = PseudoClass.getPseudoClass("has-imports");
        final BooleanProperty hasImports = new SimpleBooleanProperty();
        hasImports.addListener(observable -> importsButton.pseudoClassStateChanged(hasImportsPseudoClass, hasImports.get()));
        importsButton.getStyleClass().addAll("imports-button");
        importerScreen.getListView().getItems().addListener((ListChangeListener<? super ImportJob>) c -> Platform.runLater(() -> {
            int count = c.getList().size() + importerScreen.getSimilar().size();
            importsButton.setText("Imports: " + count);

            hasImports.set(count > 0);
        }));
        importerScreen.getSimilar().addListener((ListChangeListener<? super SimilarPair<MediaItem>>) c -> Platform.runLater(() -> {
            int count = c.getList().size() + importerScreen.getListView().getItems().size();
            importsButton.setText("Imports: " + count);

            hasImports.set(count > 0);
        }));
    }

    /**
     * Initializes the general purpose duplicates screen
     */
    private void initDuplicatesScreen() {
        duplicateOptionsScreen = new DuplicateOptionsScreen(settings);
        duplicateOptionsScreen.getDuplicatesScreen().setSelectListener(this::selectItemInGridView);
        duplicateOptionsScreen.getDuplicatesScreen().preloadProperty().bind(settings.duplicatePreload.valueProperty());
        duplicateOptionsScreen.getDuplicatesScreen().getLeftInfoBox().extendedProperty().addListener((observable, oldValue, newValue) -> settings.expandItemInfo.setValue(newValue));
        duplicateOptionsScreen.getDuplicatesScreen().getRightInfoBox().extendedProperty().addListener((observable, oldValue, newValue) -> settings.expandItemInfo.setValue(newValue));
    }

    /**
     * Initializes the slideshow screen
     */
    private void initSlideshowScreen() {
        slideshowScreen = new SlideshowScreen(item -> {
            slideshowScreen.close();
            selectItemInGridView(item);
        });
        slideshowScreen.getInfoBox().extendedProperty().addListener((observable, oldValue, newValue) -> settings.expandItemInfo.setValue(newValue));
        slideshowScreen.intervalProperty().bind(settings.slideshowInterval.valueProperty());
        slideshowScreen.preloadProperty().bind(settings.slideshowPreload.valueProperty());
    }

    /**
     * Initializes menagerie importer thread.
     */
    private void initImporterThread() {
        LOGGER.info("Starting importer thread");
        importer = new ImporterThread(menagerie, settings);
        importer.setDaemon(true);
        importer.start();

        settings.autoImportGroup.enabledProperty().addListener((observable, oldValue, newValue) -> Platform.runLater(() -> {
            // Defer to later to ensure other settings get updated before any action is taken, since this operation relies on other settings
            if (folderWatcherThread != null) {
                folderWatcherThread.stopWatching();
            }

            if (newValue)
                startWatchingFolderForImages(settings.autoImportFolder.getValue(), settings.autoImportMove.getValue());
        }));
    }

    /**
     * Initializes the log screen
     */
    private void initLogScreen() {
        logScreen = new LogScreen();
        logScreen.getListView().setCellFactory(param -> new LogListCell());
        logButton.getStyleClass().addAll("log-button");

        Main.MENAGERIE_LOGGER.addHandler(new Handler() {
            @Override
            public void publish(LogRecord record) {
                StringBuilder work = new StringBuilder(new Date(record.getMillis()).toString());
                work.append(" [").append(record.getLevel()).append("]: ").append(record.getMessage());
                if (record.getThrown() != null) {
                    work.append("\n").append(record.getThrown().toString());
                    for (StackTraceElement e : record.getThrown().getStackTrace()) {
                        work.append("\n    at ").append(e);
                    }
                }
                Platform.runLater(() -> {
                    LogItem item = new LogItem(work.toString(), record.getLevel());
                    if (item.getLevel() == Level.SEVERE) {
                        logError.set(true);
                    } else if (item.getLevel() == Level.WARNING) {
                        logWarning.set(true);
                    }
                    logScreen.getListView().getItems().add(item);
                    if (logScreen.getListView().getItems().size() > 1000) logScreen.getListView().getItems().remove(0);
                });
            }

            @Override
            public void flush() {

            }

            @Override
            public void close() throws SecurityException {

            }
        });
    }

    /**
     * Initializes the core explorer interface
     */
    private void initExplorer() {
        // Set image grid width from settings
        setGridWidth(settings.gridWidth.getValue());

        // Init image grid
        initExplorerItemGridView();

        // Init drag/drop handlers
        explorerRootPane.disabledProperty().addListener((observable1, oldValue1, newValue1) -> explorerRootPaneDisabledChanged(newValue1));
        explorerRootPane.setOnDragOver(this::explorerRootPaneOnDragOver);
        explorerRootPane.setOnDragDropped(this::explorerRootPaneOnDragDropped);

        // Init tag list cell factory
        initExplorerTagListCellFactory();

        // Init predictive textfields
        initPredictiveTextFields();

        // Init preview
        previewMediaView.getImageView().getScale().addListener((observable, oldValue, newValue) -> {
            if (newValue.doubleValue() == 1) {
                explorerZoomLabel.setText(null);
            } else {
                explorerZoomLabel.setText(String.format("%d%%", (int) (100 * (1 / newValue.doubleValue()))));
            }
        });
        previewMediaView.setMute(settings.muteVideo.getValue());
        previewMediaView.setRepeat(settings.repeatVideo.getValue());

        // Init item info box
        itemInfoBox.extendedProperty().addListener((observable, oldValue, newValue) -> settings.expandItemInfo.setValue(newValue));
        settings.expandItemInfo.valueProperty().addListener((observable, oldValue, newValue) -> {
            itemInfoBox.setExtended(newValue);
            slideshowScreen.getInfoBox().setExtended(newValue);
            duplicateOptionsScreen.getDuplicatesScreen().getLeftInfoBox().setExtended(newValue);
            duplicateOptionsScreen.getDuplicatesScreen().getRightInfoBox().setExtended(newValue);
        });

        // Init scope label binding
        scopeLabel.maxWidthProperty().bind(scopeHBox.widthProperty().subtract(backButton.widthProperty()).subtract(scopeHBox.getSpacing()));

        // Init search button icons/tooltips
        listDescendingToggleButton.setGraphic(new ImageView(new Image(getClass().getResource("/misc/descending.png").toString())));
        listDescendingToggleButton.setTooltip(new Tooltip("Descending order"));
        showGroupedToggleButton.setGraphic(new ImageView(new Image(getClass().getResource("/misc/opengroups.png").toString())));
        showGroupedToggleButton.setTooltip(new Tooltip("Show group elements"));
        shuffledSearchButton.setGraphic(new ImageView(new Image(getClass().getResource("/misc/shuffle.png").toString())));
        shuffledSearchButton.setTooltip(new Tooltip("Shuffle results"));

        // Init settings listeners
        settings.gridWidth.valueProperty().addListener((observable, oldValue, newValue) -> setGridWidth(newValue.intValue()));
        settings.muteVideo.valueProperty().addListener((observable, oldValue, newValue) -> previewMediaView.setMute(newValue));
        settings.repeatVideo.valueProperty().addListener((observable, oldValue, newValue) -> previewMediaView.setRepeat(newValue));
    }

    /**
     * Initializes the predictive text fields with option listeners
     */
    private void initPredictiveTextFields() {
        editTagsTextField.setOptionsListener(prefix -> {
            prefix = prefix.toLowerCase();
            boolean negative = prefix.startsWith("-");
            if (negative) prefix = prefix.substring(1);

            List<String> results = new ArrayList<>();

            List<Tag> tags;
            if (negative) {
                tags = new ArrayList<>();
                for (Item item : itemGridView.getSelected()) {
                    item.getTags().forEach(tag -> {
                        if (!tags.contains(tag)) tags.add(tag);
                    });
                }
            } else {
                tags = new ArrayList<>(menagerie.getTags());
            }

            tags.sort((o1, o2) -> o2.getFrequency() - o1.getFrequency());
            for (Tag tag : tags) {
                if (tag.getName().toLowerCase().startsWith(prefix)) {
                    if (negative) {
                        results.add("-" + tag.getName());
                    } else {
                        results.add(tag.getName());
                    }
                }

                if (results.size() >= 8) break;
            }

            return results;
        });
        searchTextField.setTop(false);
        searchTextField.setOptionsListener(prefix -> {
            prefix = prefix.toLowerCase();
            boolean negative = prefix.startsWith("-");
            if (negative) prefix = prefix.substring(1);

            List<String> results = new ArrayList<>();

            List<Tag> tags = new ArrayList<>(menagerie.getTags());

            tags.sort((o1, o2) -> o2.getFrequency() - o1.getFrequency());
            for (Tag tag : tags) {
                if (tag.getName().toLowerCase().startsWith(prefix)) {
                    if (negative) {
                        results.add("-" + tag.getName());
                    } else {
                        results.add(tag.getName());
                    }
                }

                if (results.size() >= 8) break;
            }

            return results;
        });
    }

    /**
     * Initializes the cell factory for the explorer tag list
     */
    private void initExplorerTagListCellFactory() {
        tagListView.setCellFactory(param -> {
            TagListCell c = new TagListCell(thing -> {
                String text = searchTextField.getText();
                if (text == null) text = "";
                text = text.trim();
                if (Arrays.asList(text.toLowerCase().split(" ")).contains("-" + thing.getName().toLowerCase())) {
                    text = text.replaceAll("(^|\\s)-" + thing.getName(), " ");
                } else if (!Arrays.asList(text.toLowerCase().split(" ")).contains(thing.getName().toLowerCase())) {
                    if (!text.isEmpty() && !text.endsWith(" ")) text += " ";
                    text += thing.getName() + " ";
                }
                searchTextField.setText(text.trim());
                searchTextField.requestFocus();
                searchTextField.positionCaret(text.length());
            }, thing -> {
                String text = searchTextField.getText();
                if (text == null) text = "";
                text = text.trim();
                if (Arrays.asList(text.toLowerCase().split(" ")).contains(thing.getName().toLowerCase())) {
                    text = text.replaceAll("(^|\\s)" + thing.getName(), " ");
                } else if (!Arrays.asList(text.toLowerCase().split(" ")).contains("-" + thing.getName().toLowerCase())) {
                    if (!text.isEmpty() && !text.endsWith(" ")) text += " ";
                    text += "-" + thing.getName() + " ";
                }
                searchTextField.setText(text.trim());
                searchTextField.requestFocus();
                searchTextField.positionCaret(text.length());
            });
            // TODO: Make a custom popup that encompasses all the tag controls?
            c.setOnContextMenuRequested(event -> {
                if (c.getItem() != null) {
                    // TODO Transfer all of this to TagListCell class and use listeners/callbacks
                    MenuItem addNote = new MenuItem("Add note");
                    addNote.setOnAction(event1 -> new TextDialogScreen().open(screenPane, "Add a note", String.format("Add a note to tag '%s'", c.getItem().getName()), null, note -> c.getItem().addNote(note), null));
                    MenuItem tagSelected = new MenuItem("Tag selected");
                    tagSelected.setOnAction(event1 -> {
                        Map<Item, List<Tag>> added = new HashMap<>();
                        itemGridView.getSelected().forEach(item -> {
                            if (item.addTag(c.getItem())) {
                                added.computeIfAbsent(item, k -> new ArrayList<>()).add(c.getItem());
                            }
                        });

                        tagEditHistory.push(new TagEditEvent(added, null));
                    });
                    MenuItem untagSelected = new MenuItem("Untag selected");
                    untagSelected.setOnAction(event1 -> {
                        Map<Item, List<Tag>> removed = new HashMap<>();
                        itemGridView.getSelected().forEach(item -> {
                            if (item.removeTag(c.getItem())) {
                                removed.computeIfAbsent(item, k -> new ArrayList<>()).add(c.getItem());
                            }
                        });

                        tagEditHistory.push(new TagEditEvent(null, removed));
                    });
                    SimpleCSSColorPicker colorPicker = new SimpleCSSColorPicker(color -> c.getItem().setColor(color));
                    colorPicker.getTextfield().setText(c.getItem().getColor());
                    CustomMenuItem tagColorPicker = new CustomMenuItem(colorPicker, false);

                    ContextMenu m = new ContextMenu(addNote, new SeparatorMenuItem(), tagSelected, untagSelected, new SeparatorMenuItem(), tagColorPicker, new SeparatorMenuItem());

                    // TODO: Do this better, jfc
                    for (String note : c.getItem().getNotes()) {
                        MenuItem item = new MenuItem(note);
                        item.setMnemonicParsing(false);
                        item.setOnAction(event1 -> {
                            try {
                                Desktop.getDesktop().browse(new URI(note));
                            } catch (IOException | URISyntaxException e) {
                                try {
                                    Desktop.getDesktop().browse(new URI("https://" + c.getItem()));
                                } catch (IOException | URISyntaxException ignore) {
                                }
                            }
                        });
                        m.getItems().add(item);
                    }

                    m.show(c.getScene().getWindow(), event.getScreenX(), event.getScreenY());
                }
            });

            c.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && c.getItem() != null) {
                    searchTextField.setText(c.getItem().getName() + " ");
                    searchTextField.requestFocus();
                    searchTextField.positionCaret(searchTextField.getText().length());
                    event.consume();
                }
            });

            return c;
        });
    }

    /**
     * Initializes listeners and etc. of the itemGridView
     */
    private void initExplorerItemGridView() {
        itemGridView.addSelectionListener(image -> Platform.runLater(() -> previewItem(image)));
        itemGridView.setCellFactory(param -> {
            ItemGridCell c = new ItemGridCell();
            c.setOnDragDetected(event -> {
                if (!itemGridView.getSelected().isEmpty() && event.isPrimaryButtonDown()) {
                    if (c.getItem() instanceof MediaItem && !itemGridView.isSelected(c.getItem()))
                        itemGridView.select(c.getItem(), event.isControlDown(), event.isShiftDown());

                    Dragboard db = c.startDragAndDrop(TransferMode.ANY);

                    for (Item item : itemGridView.getSelected()) {
                        if (item instanceof MediaItem) {
                            String filename = ((MediaItem) item).getFile().getName().toLowerCase();
                            if (filename.endsWith(".png") || filename.endsWith(".jpg") || filename.endsWith(".jpeg") || filename.endsWith(".bmp")) {
                                if (item.getThumbnail().isLoaded()) {
                                    db.setDragView(item.getThumbnail().getImage());
                                    break;
                                }
                            }
                        }
                    }

                    List<File> files = new ArrayList<>();
                    itemGridView.getSelected().forEach(item -> {
                        if (item instanceof MediaItem) files.add(((MediaItem) item).getFile());
                        else if (item instanceof GroupItem)
                            ((GroupItem) item).getElements().forEach(mediaItem -> files.add(mediaItem.getFile()));
                    });
                    clipboard.putFiles(files);
                    db.setContent(clipboard);

                    itemGridViewDragging = true;
                    event.consume();
                }
            });
            c.setOnDragDone(event -> {
                itemGridViewDragging = false;
                event.consume();
            });
            c.setOnDragOver(event -> {
                if (event.getGestureSource() instanceof ItemGridCell && currentSearch instanceof GroupSearch && !event.getGestureSource().equals(c)) {
                    event.acceptTransferModes(TransferMode.ANY);
                    event.consume();
                }
            });
            c.setOnDragDropped(event -> {
                if (event.getGestureSource() instanceof ItemGridCell && !itemGridView.getSelected().isEmpty()) {
                    List<MediaItem> list = new ArrayList<>();
                    itemGridView.getSelected().forEach(item -> list.add((MediaItem) item));
                    list.sort(Comparator.comparingInt(MediaItem::getPageIndex));

                    boolean before = false;
                    if (c.sceneToLocal(event.getSceneX(), event.getSceneY()).getX() < (double) Thumbnail.THUMBNAIL_SIZE / 2)
                        before = true;
                    if (currentSearch.isDescending()) before = !before;
                    if (((MediaItem) c.getItem()).getGroup().moveElements(list, (MediaItem) c.getItem(), before)) {
                        currentSearch.sort();
                        itemGridView.getItems().sort(currentSearch.getComparator());
                        event.consume();
                    }
                }
            });
            c.setOnMouseReleased(event -> {
                if (!itemGridViewDragging && event.getButton() == MouseButton.PRIMARY) {
                    itemGridView.select(c.getItem(), event.isControlDown(), event.isShiftDown());
                    event.consume();
                }
            });
            c.setOnContextMenuRequested(event -> {
                if (!itemGridView.isSelected(c.getItem())) {
                    itemGridView.select(c.getItem(), false, false);
                }
                constructGridCellContextMenu(itemGridView.getSelected()).show(c.getScene().getWindow(), event.getScreenX(), event.getScreenY());
                event.consume();
            });
            c.setOnMouseClicked(event -> {
                if (event.getButton() == MouseButton.PRIMARY && event.getClickCount() > 1) {
                    if (c.getItem() instanceof GroupItem) {
                        explorerOpenGroup((GroupItem) c.getItem());
                    } else if (c.getItem() instanceof MediaItem) {
                        try {
                            Desktop.getDesktop().open(((MediaItem) c.getItem()).getFile());
                        } catch (IOException e) {
                            LOGGER.log(Level.SEVERE, "Failed to open file with system default: " + ((MediaItem) c.getItem()).getFile(), e);
                        }
                    }
                }
            });
            return c;
        });
        itemGridView.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.DELETE) {
                if (event.isControlDown()) {
                    forgetFilesDialog(itemGridView.getSelected());
                } else {
                    deleteFilesDialog(itemGridView.getSelected());
                }
                event.consume();
            } else if (event.getCode() == KeyCode.G && event.isControlDown()) {
                groupDialog(itemGridView.getSelected());
            } else if (event.getCode() == KeyCode.U && event.isControlDown()) {
                ungroupDialog(itemGridView.getSelected());
            }
        });
        itemGridView.getSelected().addListener((ListChangeListener<? super Item>) c -> resultCountLabel.setText(itemGridView.getSelected().size() + " / " + currentSearch.getResults().size()));
        itemGridView.getItems().addListener((ListChangeListener<? super Item>) c -> {
            while (c.next()) {
                if (c.wasAdded()) {
                    itemGridView.getItems().sort(currentSearch.getComparator());
                    break;
                }
            }
        });
    }

    /**
     * Applies window properties to the window and starts listeners for changes to update the settings objects
     */
    private void initWindowPropertiesAndListeners() {
        LOGGER.info("Initializing window properties and listeners");

        Stage stage = ((Stage) explorerRootPane.getScene().getWindow());
        stage.setMaximized(settings.windowMaximized.getValue());
        if (settings.windowWidth.getValue() > 0) {
            stage.setWidth(settings.windowWidth.getValue());
        } else {
            settings.windowWidth.setValue((int) stage.getWidth());
        }
        if (settings.windowHeight.getValue() > 0) {
            stage.setHeight(settings.windowHeight.getValue());
        } else {
            settings.windowHeight.setValue((int) stage.getHeight());
        }
        if (settings.windowX.getValue() >= 0) {
            stage.setX(settings.windowX.getValue());
        } else {
            settings.windowX.setValue((int) stage.getX());
        }
        if (settings.windowY.getValue() >= 0) {
            stage.setY(settings.windowY.getValue());
        } else {
            settings.windowY.setValue((int) stage.getY());
        }

        //Bind window properties to settings
        //        stage.maximizedProperty().addListener((observable, oldValue, newValue) -> settings.windowMaximized.setValue(newValue));
        //        stage.widthProperty().addListener((observable, oldValue, newValue) -> settings.windowWidth.setValue(newValue.intValue()));
        //        stage.heightProperty().addListener((observable, oldValue, newValue) -> settings.windowHeight.setValue(newValue.intValue()));
        //        stage.xProperty().addListener((observable, oldValue, newValue) -> settings.windowX.setValue(newValue.intValue()));
        //        stage.yProperty().addListener((observable, oldValue, newValue) -> settings.windowY.setValue(newValue.intValue()));
        // TODO test simplified binding
        settings.windowMaximized.valueProperty().bind(stage.maximizedProperty());
        settings.windowWidth.valueProperty().bind(stage.widthProperty());
        settings.windowHeight.valueProperty().bind(stage.heightProperty());
        settings.windowX.valueProperty().bind(stage.xProperty());
        settings.windowY.valueProperty().bind(stage.yProperty());

        stage.focusedProperty().addListener((observable, oldValue, newValue) -> {
            if (!newValue) {
                if (previewMediaView.isPlaying()) {
                    previewMediaView.pause();
                    playVideoAfterFocusGain = true;
                }
            } else if (playVideoAfterFocusGain) {
                previewMediaView.play();
                playVideoAfterFocusGain = false;
            }
        });
    }

    // ---------------------------------- GUI Action Methods ---------------------------

    /**
     * Constructs a context menu for a given set of items. Different combinations of GroupItem and MediaItem will give different context menu items.
     *
     * @param selected Set of items that this context menu with operate on.
     * @return A context menu ready to be shown for the given set of items.
     */
    private ContextMenu constructGridCellContextMenu(List<Item> selected) {
        if (selected == null || selected.isEmpty()) {
            MenuItem mi = new MenuItem("Nothing selected");
            mi.setDisable(true);
            return new ContextMenu(mi);
        }

        ContextMenu cm = new ContextMenu();

        int groupCount = 0, mediaCount = 0, itemsInGroupCount = 0;
        for (Item item : selected) {
            if (item instanceof GroupItem) {
                groupCount++;
            } else if (item instanceof MediaItem) {
                mediaCount++;
                if (((MediaItem) item).isInGroup()) itemsInGroupCount++;
            }
        }

        // More than one group or at least one media item
        if (groupCount == 1 && selected.size() == 1) {
            MenuItem elementTags = new MenuItem("Sync element tags to group");
            elementTags.setOnAction(event -> {
                GroupItem group = (GroupItem) selected.get(0);
                group.getElements().forEach(item -> item.getTags().forEach(group::addTag));
            });
            cm.getItems().add(elementTags);
            MenuItem reverse = new MenuItem("Reverse element order");
            reverse.setOnAction(event -> ((GroupItem) selected.get(0)).reverseElements());
            cm.getItems().add(reverse);
            MenuItem rename = new MenuItem("Rename group");
            rename.setOnAction(event -> openGroupRenameDialog((GroupItem) selected.get(0)));
            cm.getItems().add(rename);
            cm.getItems().add(new SeparatorMenuItem());
        }
        if (groupCount > 1 || mediaCount > 0) {
            MenuItem combineGroups = new MenuItem("Combine into Group");
            combineGroups.setOnAction(event -> groupDialog(selected));
            cm.getItems().add(combineGroups);
        }
        if (itemsInGroupCount > 0) {
            MenuItem removeFromGroup = new MenuItem("Remove from group");
            removeFromGroup.setOnAction(event -> selected.forEach(item -> {
                if (item instanceof MediaItem && ((MediaItem) item).isInGroup()) {
                    ((MediaItem) item).getGroup().removeItem((MediaItem) item);
                }
            }));
            cm.getItems().add(removeFromGroup);
        }

        if (groupCount > 0 || mediaCount > 0) {

            MenuItem grabbed = new MenuItem("Selected");
            grabbed.setOnAction(event -> openSlideShow(selected, false));
            MenuItem searched = new MenuItem("Searched");
            searched.setOnAction(event -> openSlideShow(currentSearch.getResults(), true));
            MenuItem all = new MenuItem("All");
            all.setOnAction(event -> openSlideShow(menagerie.getItems(), true));
            Menu slideshow = new Menu("Slideshow...", null, grabbed, searched, all);

            MenuItem moveFiles = new MenuItem("Move Files");
            moveFiles.setOnAction(event -> moveFilesScreen.open(screenPane, selected));

            MenuItem findOnline = new MenuItem("Online");
            findOnline.setOnAction(event -> findOnlineDialog(selected));
            MenuItem findDupes = new MenuItem("Duplicates");
            findDupes.setOnAction(event -> duplicateOptionsScreen.open(screenPane, menagerie, selected, currentSearch.getResults(), menagerie.getItems()));
            Menu find = new Menu("Find...", null, findOnline, findDupes);

            cm.getItems().addAll(find, slideshow, moveFiles);
        }

        if (mediaCount > 0) {
            MenuItem openDefault = new MenuItem("Open");
            openDefault.setOnAction(event -> {
                Item last = selected.get(selected.size() - 1);
                if (last instanceof MediaItem) {
                    try {
                        Desktop.getDesktop().open(((MediaItem) last).getFile());
                    } catch (IOException e) {
                        LOGGER.log(Level.WARNING, "Error opening file with system default program", e);
                    }
                }
            });
            MenuItem explorer = new MenuItem("Open in Explorer");
            explorer.setOnAction(event -> {
                Item last = selected.get(selected.size() - 1);
                if (last instanceof MediaItem) {
                    try {
                        Runtime.getRuntime().exec("explorer.exe /select, " + ((MediaItem) last).getFile().getAbsolutePath());
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Error opening file in explorer", e);
                    }
                }
            });
            cm.getItems().addAll(openDefault, explorer);
        }

        if (groupCount > 0 || mediaCount > 0) {
            cm.getItems().add(new SeparatorMenuItem());
            if (groupCount > 0) {
                MenuItem ungroup = new MenuItem("Ungroup");
                ungroup.setOnAction(event -> ungroupDialog(selected));
                cm.getItems().add(ungroup);
            }
            MenuItem forget = new MenuItem("Forget files");
            forget.setOnAction(event -> forgetFilesDialog(selected));
            forget.setStyle("-fx-text-fill: red;");
            MenuItem delete = new MenuItem("Delete files");
            delete.setOnAction(event -> deleteFilesDialog(selected));
            delete.setStyle("-fx-text-fill: red;");
            cm.getItems().addAll(forget, delete);
        }

        return cm;
    }

    /**
     * Opens a dialog to allow the user to rename a group
     *
     * @param group Group to rename
     */
    private void openGroupRenameDialog(GroupItem group) {
        new TextDialogScreen().open(screenPane, "Rename group", "Current: " + group.getTitle(), group.getTitle(), group::setTitle, null);
    }

    /**
     * Attempts to display an item's media in the preview viewport.
     *
     * @param item The item to display. Displays nothing when item is a GroupItem.
     */
    private void previewItem(Item item) {
        if (currentlyPreviewing != null) currentlyPreviewing.getTags().removeListener(previewTagListener);
        currentlyPreviewing = item;

        previewMediaView.preview(item);
        itemInfoBox.setItem(item);

        tagListView.getItems().clear();
        if (item != null) {
            tagListView.getItems().addAll(item.getTags());
            tagListView.getItems().sort(Comparator.comparing(Tag::getName));
        }

        if (item != null) item.getTags().addListener(previewTagListener);
    }

    /**
     * Opens the "find online" dialog. Uses existing plugins to reverse image search and find duplicates online.
     *
     * @param selected Items to search online for
     */
    private void findOnlineDialog(List<Item> selected) {
        List<MediaItem> items = new ArrayList<>();
        for (Item item : selected) {
            if (item instanceof MediaItem) {
                items.add((MediaItem) item);
            } else if (item instanceof GroupItem) {
                for (MediaItem element : ((GroupItem) item).getElements()) {
                    final String name = element.getFile().getName().toLowerCase();
                    if (name.endsWith(".jpg") || name.endsWith(".jpeg") || name.endsWith(".png") || name.endsWith(".bmp")) {
                        items.add(element);
                    }
                }
            }
        }

        List<DuplicateFinder> finders = new ArrayList<>();
        for (MenageriePlugin plugin : plugins) {
            if (plugin instanceof DuplicateFinder) {
                finders.add((DuplicateFinder) plugin);
            }
        }

        findOnlineScreen.open(screenPane, items, finders, this::selectItemInGridView);
    }

    /**
     * Opens a dialog asking for user confirmation to forget files from the menagerie without deleting the file.
     *
     * @param toForget Set of items to forget if user confirms.
     */
    private void forgetFilesDialog(List<Item> toForget) {
        List<Item> items = new ArrayList<>(toForget);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof GroupItem) items.addAll(((GroupItem) items.get(i)).getElements());
        }
        if (currentlyPreviewing instanceof MediaItem && items.contains(currentlyPreviewing) && ((MediaItem) currentlyPreviewing).isVideo())
            previewMediaView.stop();
        new ConfirmationScreen().open(screenPane, "Forget files", String.format("Remove selected files from database? (%d files)\n\n" + "This action CANNOT be undone", items.size()), () -> menagerie.forgetItems(items), null);
    }

    /**
     * Opens a dialog asking for user confirmation to delete files from the menagerie. Will delete files from the local disk.
     *
     * @param toDelete Set of items to forget and delete if user confirms.
     */
    private void deleteFilesDialog(List<Item> toDelete) {
        List<Item> items = new ArrayList<>(toDelete);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof GroupItem) items.addAll(((GroupItem) items.get(i)).getElements());
        }
        if (currentlyPreviewing instanceof MediaItem && items.contains(currentlyPreviewing) && ((MediaItem) currentlyPreviewing).isVideo())
            previewMediaView.stop();
        new ConfirmationScreen().open(screenPane, "Delete files", String.format("Permanently delete selected files? (%d files)\n\n" + "This action CANNOT be undone (files will be deleted)", items.size()), () -> menagerie.deleteItems(items), null);
    }

    /**
     * Opens a dialog asking for user confirmation to ungroup given groups. Items that are not a GroupItem will be ignored.
     *
     * @param items Set of items to ungroup if user confirms.
     */
    private void ungroupDialog(List<Item> items) {
        List<GroupItem> groups = new ArrayList<>();
        items.forEach(item -> {
            if (item instanceof GroupItem) groups.add((GroupItem) item);
        });
        new ConfirmationScreen().open(screenPane, "Ungroup group?", String.format("Are you sure you want to ungroup %d groups?", groups.size()), () -> groups.forEach(GroupItem::ungroup), null);
    }

    /**
     * Opens a dialog asking for user confirmation to group items into a new group. GroupItems will be merged.
     *
     * @param toGroup Set of items to group if user confirms.
     */
    private void groupDialog(List<Item> toGroup) {
        String title = null;
        for (Item item : toGroup) {
            if (item instanceof GroupItem) {
                title = ((GroupItem) item).getTitle();
                break;
            }
        }
        groupDialogScreen.open(screenPane, menagerie, title, toGroup, group -> {
            LOGGER.info("Created group: " + group);
            Platform.runLater(() -> {
                if (currentSearch.getResults().contains(group)) itemGridView.select(group, false, false);
            });
        });
    }

    /**
     * Opens the log screen and clears error/warning flags
     */
    private void openLog() {
        logError.set(false);
        logWarning.set(false);
        screenPane.open(logScreen);
    }

    /**
     * Opens the slideshow screen.
     *
     * @param items Set of items to show in the slideshow.
     */
    private void openSlideShow(List<Item> items, boolean reversed) {
        items = new ArrayList<>(items);
        for (int i = 0; i < items.size(); i++) {
            if (items.get(i) instanceof GroupItem) {
                GroupItem group = (GroupItem) items.remove(i);
                items.addAll(i, group.getElements());
            }
        }
        if (reversed) Collections.reverse(items);
        slideshowScreen.open(screenPane, menagerie, items);
    }

    /**
     * Attempts to revert to the previous search.
     */
    private void explorerGoBack() {
        if (searchHistory.empty()) {
            Toolkit.getDefaultToolkit().beep();
        } else {
            SearchHistory history = searchHistory.pop();

            listDescendingToggleButton.setSelected(history.isDescending());
            showGroupedToggleButton.setSelected(history.isShowGrouped());
            shuffledSearchButton.setSelected(history.isShuffled());
            searchTextField.setText(history.getSearch());
            applySearch(history.getSearch(), history.getGroupScope(), history.isDescending(), history.isShowGrouped(), history.isShuffled(), history.getShuffleSeed());
            searchHistory.pop(); // Pop history item that was JUST created by the new search.

            if (searchHistory.isEmpty()) backButton.setDisable(true);

            itemGridView.clearSelection();
            //            history.getSelected().forEach(item -> itemGridView.select(item, true, false));
            itemGridView.getSelected().addAll(history.getSelected());
            if (history.getSelected() != null && !history.getSelected().isEmpty()) {
                // Unselect and reselect last item
                itemGridView.select(history.getSelected().get(history.getSelected().size() - 1), true, false);
                itemGridView.select(history.getSelected().get(history.getSelected().size() - 1), true, false);
            }
        }
    }

    /**
     * Sets the search scope to a group search and applies an empty search.
     *
     * @param group Group scope.
     */
    private void explorerOpenGroup(GroupItem group) {
        searchTextField.setText(null);
        if (settings.explorerGroupAscending.getValue()) listDescendingToggleButton.setSelected(false);
        applySearch(searchTextField.getText(), group, listDescendingToggleButton.isSelected(), true, false);
    }

    private void applySearch(String search, GroupItem groupScope, boolean descending, boolean showGrouped, boolean shuffled) {
        applySearch(search, groupScope, descending, showGrouped, shuffled, System.currentTimeMillis());
    }

    /**
     * Parses a search string, applies the search, updates grid, registers search listeners, and previews first item.
     *
     * @param search      Search string to parse rules from.
     * @param descending  Order results in descending order.
     * @param showGrouped Show MediaItems that are in a group.
     */
    private void applySearch(String search, GroupItem groupScope, boolean descending, boolean showGrouped, boolean shuffled, long shuffleSeed) {
        LOGGER.info("Searching: \"" + search + "\", group:" + groupScope + ", descending:" + descending + ", showGrouped:" + showGrouped + ", shuffled:" + shuffled);

        // Clean up previous search
        if (currentSearch != null) {
            GroupItem scope = null;
            if (currentSearch instanceof GroupSearch) scope = ((GroupSearch) currentSearch).getGroup();
            searchHistory.push(new SearchHistory(currentSearch.getSearchString(), scope, itemGridView.getSelected(), currentSearch.isDescending(), currentSearch.isShowGrouped(), currentSearch.isShuffled(), currentSearch.getShuffleSeed()));
            backButton.setDisable(false);

            menagerie.unregisterSearch(currentSearch);
            currentSearch.getResults().removeListener(searchChangeListener);
        }
        previewItem(null);

        // Create new search
        boolean inGroup = groupScope != null;
        shuffledSearchButton.setDisable(inGroup);
        shuffledSearchButton.setSelected(shuffled && !inGroup);
        showGroupedToggleButton.setDisable(inGroup);
        showGroupedToggleButton.setSelected(showGrouped || inGroup);
        if (inGroup) {
            currentSearch = new GroupSearch(search, groupScope, descending, shuffled, shuffleSeed);
            scopeLabel.setText("Scope: " + groupScope.getTitle());
            Tooltip tt = new Tooltip(groupScope.getTitle());
            tt.setWrapText(true);
            scopeLabel.setTooltip(tt);
        } else {
            currentSearch = new Search(search, descending, showGrouped, shuffled, shuffleSeed);
            scopeLabel.setText("Scope: All");
            scopeLabel.setTooltip(null);
        }
        menagerie.registerSearch(currentSearch);
        currentSearch.refreshSearch(menagerie.getItems());
        currentSearch.getResults().addListener(searchChangeListener);

        itemGridView.clearSelection();
        itemGridView.getItems().clear();
        itemGridView.getItems().addAll(currentSearch.getResults());

        if (!itemGridView.getItems().isEmpty()) itemGridView.select(itemGridView.getItems().get(0), false, false);
    }

    /**
     * Sets the item grid width.
     *
     * @param n Width of grid in number of cells.
     */
    private void setGridWidth(int n) {
        final double width = 18 + (Thumbnail.THUMBNAIL_SIZE + ItemGridView.CELL_BORDER * 2 + itemGridView.getHorizontalCellSpacing() * 2) * n;
        gridPane.setMinWidth(width);
        gridPane.setMaxWidth(width);
        gridPane.setPrefWidth(width);
    }

    /**
     * Parses a string and applies tag edits to currently selected items. Opens a confirmation dialog if 100 or more items will be modified by this operation.
     *
     * @param input Tag edit string.
     */
    private void editTagsOfSelected(String input) {
        if (input == null || input.isEmpty() || itemGridView.getSelected().isEmpty()) return;
        lastEditTagString = input.trim();

        if (itemGridView.getSelected().size() < 100) {
            editTagsUtility(input);
        } else {
            new ConfirmationScreen().open(screenPane, "Editing large number of items", "You are attempting to edit " + itemGridView.getSelected().size() + " items. Continue?", () -> editTagsUtility(input), null);
        }
    }

    /**
     * Actual workhorse tag editing method. Parses tag edit string, makes changes, and verifies changed items against the search.
     *
     * @param input Tag edit string.
     */
    private void editTagsUtility(String input) {
        List<Item> changed = new ArrayList<>();
        Map<Item, List<Tag>> added = new HashMap<>();
        Map<Item, List<Tag>> removed = new HashMap<>();

        for (String text : input.split("\\s+")) {
            if (text.startsWith("-")) {
                Tag t = menagerie.getTagByName(text.substring(1));
                if (t != null) {
                    for (Item item : itemGridView.getSelected()) {
                        if (item.removeTag(t)) {
                            changed.add(item);

                            removed.computeIfAbsent(item, k -> new ArrayList<>()).add(t);
                        }
                    }
                }
            } else {
                Tag t = menagerie.getTagByName(text);
                if (t == null) t = menagerie.createTag(text);
                for (Item item : itemGridView.getSelected()) {
                    if (item.addTag(t)) {
                        changed.add(item);

                        added.computeIfAbsent(item, k -> new ArrayList<>()).add(t);
                    }
                }
            }
        }

        if (!changed.isEmpty()) {
            menagerie.refreshInSearches(changed);

            tagEditHistory.push(new TagEditEvent(added, removed));
        }
    }

    /**
     * Selects an item in the grid view. If the item is hidden in a group, selects the group instead.
     *
     * @param item Item to select
     */
    private void selectItemInGridView(Item item) {
        if (itemGridView.getItems().contains(item)) {
            itemGridView.select(item, false, false);
        } else if (item instanceof MediaItem) {
            GroupItem group = ((MediaItem) item).getGroup();
            if (group != null && itemGridView.getItems().contains(group)) {
                itemGridView.select(group, false, false);
            } else {
                Toolkit.getDefaultToolkit().beep();
            }
        }
    }

    // ---------------------------------- Compute Utilities -----------------------------

    /**
     * Attempts to resolve a filename conflict caused by a pre-existing file at the same path. Appends an incremented number surrounded by parenthesis to the file if it already exists.
     *
     * @param file File to resolve name for.
     * @return File pointing to a file that does not exist yet.
     */
    public static File resolveDuplicateFilename(File file) {
        while (file.exists()) {
            String name = file.getName();
            if (name.matches(".*\\s\\([0-9]+\\)\\..*")) {
                int count = Integer.parseInt(name.substring(name.lastIndexOf('(') + 1, name.lastIndexOf(')')));
                name = name.substring(0, name.lastIndexOf('(') + 1) + (count + 1) + name.substring(name.lastIndexOf(')'));
            } else {
                name = name.substring(0, name.lastIndexOf('.')) + " (2)" + name.substring(name.lastIndexOf('.'));
            }

            String parent = file.getParent();
            if (!parent.endsWith("/") && !parent.endsWith("\\")) parent += "/";
            file = new File(parent + name);
        }

        return file;
    }

    /**
     * Starts a folder watcher thread. Kills an active folder watcher thread first, if present.
     *
     * @param folder        Target folder to watch for new files.
     * @param moveToDefault Move found files to default folder as specified by settings object.
     */
    private void startWatchingFolderForImages(String folder, boolean moveToDefault) {
        File watchFolder = new File(folder);
        if (watchFolder.exists() && watchFolder.isDirectory()) {
            LOGGER.info("Starting folder watcher in folder: " + watchFolder);
            folderWatcherThread = new FolderWatcherThread(watchFolder, Filters.FILE_NAME_FILTER, 30000, files -> {
                for (File file : files) {
                    LOGGER.info("Folder watcher got file to import: " + file);
                    if (moveToDefault) {
                        String work = settings.defaultFolder.getValue();
                        if (!work.endsWith("/") && !work.endsWith("\\")) work += "/";
                        File f = new File(work + file.getName());
                        if (file.equals(f)) continue; //File is being "moved" to same folder

                        File dest = resolveDuplicateFilename(f);

                        if (!file.renameTo(dest)) {
                            continue;
                        }

                        file = dest;
                    }

                    importer.addJob(new ImportJob(file, null));
                }
            });
            folderWatcherThread.setDaemon(true);
            folderWatcherThread.start();
        }
    }

    /**
     * Cleanly exits the JFX application and releases all threads and resources.
     *
     * @param revertDatabase Revert database to last backup.
     */
    private void cleanExit(boolean revertDatabase) {
        LOGGER.info("Attempting clean exit");

        try {
            settings.save(new File(Main.SETTINGS_PATH));
        } catch (IOException e1) {
            LOGGER.log(Level.WARNING, "Failed to save settings to file", e1);
        }

        Platform.runLater(() -> {
            rootPane.getChildren().clear();
            Platform.exit();
        });

        DynamicVideoView.releaseAllVLCJ();
        Thumbnail.releaseVLCJResources();

        plugins.forEach(plugin -> {
            LOGGER.info("Attempting to close plugin: " + plugin.getPluginName());
            plugin.close();
        });

        new Thread(() -> {
            try {
                LOGGER.info("Attempting to shut down Menagerie database and defragment the file");
                menagerie.getDatabaseManager().shutdownDefrag();
                LOGGER.info("Done defragging database file");

                if (revertDatabase) {
                    File database = DatabaseManager.resolveDatabaseFile(settings.dbUrl.getValue());
                    File backup = new File(database + ".bak");
                    LOGGER.warning(String.format("Reverting to last backup database: %s", backup.toString()));
                    try {
                        Files.move(backup.toPath(), database.toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        LOGGER.log(Level.SEVERE, "Failed to revert the database: " + database, e);
                    }
                }

                LOGGER.info("Finished shutting down...");
            } catch (SQLException e) {
                LOGGER.log(Level.SEVERE, "SQL exception when shutting down with defrag", e);
            }

            System.exit(0);
        }, "Shutdown Menagerie").start();
    }

    // ---------------------------------- Action Event Handlers --------------------------

    public void searchButtonOnAction(ActionEvent event) {
        GroupItem scope = null;
        if (currentSearch instanceof GroupSearch) scope = ((GroupSearch) currentSearch).getGroup();
        applySearch(searchTextField.getText(), scope, listDescendingToggleButton.isSelected(), showGroupedToggleButton.isSelected(), shuffledSearchButton.isSelected());
        itemGridView.requestFocus();
        event.consume();
    }

    public void searchTextFieldOnAction(ActionEvent event) {
        GroupItem scope = null;
        if (currentSearch instanceof GroupSearch) scope = ((GroupSearch) currentSearch).getGroup();
        applySearch(searchTextField.getText(), scope, listDescendingToggleButton.isSelected(), showGroupedToggleButton.isSelected(), shuffledSearchButton.isSelected());
        itemGridView.requestFocus();
        event.consume();
    }

    public void importFilesMenuButtonOnAction(ActionEvent event) {
        screenPane.open(importDialogScreen);
        event.consume();
    }

    public void pruneFileLessMenuButtonOnAction(ActionEvent event) {
        ProgressScreen ps = new ProgressScreen();
        CancellableThread ct = new CancellableThread() {
            @Override
            public void run() {
                final int total = menagerie.getItems().size();
                int i = 0;

                List<Item> toDelete = new ArrayList<>();

                for (Item item : menagerie.getItems()) {
                    if (!running) break;
                    i++;

                    if (item instanceof MediaItem && !((MediaItem) item).getFile().exists()) {
                        toDelete.add(item);
                    }

                    final int finalI = i;
                    Platform.runLater(() -> ps.setProgress(finalI, total));
                }

                menagerie.forgetItems(toDelete);
                Platform.runLater(() -> {
                    ps.close();
                    new AlertDialogScreen().open(screenPane, "Pruning complete", toDelete.size() + " file-less items pruned.", null);
                });
            }
        };
        ps.open(screenPane, "Pruning Items", "Finding and pruning items that have become detached from their file...", ct::cancel);
        ct.setName("Fileless Pruner");
        ct.setDaemon(true);
        ct.start();

        event.consume();
    }

    public void rebuildSimilarityCacheMenuButtonOnAction(ActionEvent event) {
        ProgressScreen ps = new ProgressScreen();
        CancellableThread ct = new CancellableThread() {
            @Override
            public void run() {
                final int total = menagerie.getItems().size();
                final double confidenceSquare = 1 - (1 - MediaItem.MIN_CONFIDENCE) * (1 - MediaItem.MIN_CONFIDENCE);

                for (int i = 0; i < menagerie.getItems().size(); i++) {
                    if (!(menagerie.getItems().get(i) instanceof MediaItem)) continue;
                    MediaItem i1 = (MediaItem) menagerie.getItems().get(i);
                    if (i1.getHistogram() == null) continue;

                    boolean hasSimilar = false;
                    for (int j = 0; j < menagerie.getItems().size(); j++) {
                        if (i == j) continue;
                        if (!(menagerie.getItems().get(j) instanceof MediaItem)) continue;
                        MediaItem i2 = (MediaItem) menagerie.getItems().get(j);
                        if (i2.getHistogram() == null || i2.hasNoSimilar()) continue;

                        double similarity = i1.getSimilarityTo(i2);
                        if (similarity >= confidenceSquare || ((i1.getHistogram().isColorful() || i2.getHistogram().isColorful()) && similarity > MediaItem.MIN_CONFIDENCE)) {
                            hasSimilar = true;
                            break;
                        }
                    }

                    i1.setHasNoSimilar(!hasSimilar);

                    final int finalI = i;
                    Platform.runLater(() -> ps.setProgress(finalI, total));
                }

                Platform.runLater(ps::close);
            }
        };
        ps.open(screenPane, "Building similarity cache", "Caching items that have no possible similar items", ct::cancel);
        ct.setDaemon(true);
        ct.setName("Similarity Cache Builder");
        ct.start();

        event.consume();
    }

    public void settingsMenuButtonOnAction(ActionEvent event) {
        settingsScreen.open(screenPane, settings);
        event.consume();
    }

    public void helpMenuButtonOnAction(ActionEvent event) {
        screenPane.open(helpScreen);
        event.consume();
    }

    public void viewSlideShowSearchedMenuButtonOnAction(ActionEvent event) {
        slideshowScreen.open(screenPane, menagerie, currentSearch.getResults());
        event.consume();
    }

    public void viewSlideShowSelectedMenuButtonOnAction(ActionEvent event) {
        slideshowScreen.open(screenPane, menagerie, itemGridView.getSelected());
        event.consume();
    }

    public void viewSlideShowAllMenuButtonOnAction(ActionEvent event) {
        slideshowScreen.open(screenPane, menagerie, menagerie.getItems());
        event.consume();
    }

    public void viewTagsMenuButtonOnAction(ActionEvent event) {
        tagListScreen.open(screenPane, menagerie.getTags());
        event.consume();
    }

    public void revertDatabaseMenuButtonOnAction(ActionEvent event) {
        File database = DatabaseManager.resolveDatabaseFile(settings.dbUrl.getValue());
        File backup = new File(database + ".bak");
        if (backup.exists()) {
            new ConfirmationScreen().open(screenPane, "Revert database", "Revert to latest backup? (" + new Date(backup.lastModified()) + ")\n\nLatest backup: \"" + backup + "\"\n\nNote: Files will not be deleted!", () -> cleanExit(true), null);
        }
        event.consume();
    }

    public void importsButtonOnAction(ActionEvent event) {
        screenPane.open(importerScreen);
        event.consume();
    }

    public void logButtonOnAction(ActionEvent event) {
        openLog();
        event.consume();
    }

    public void backButtonOnAction(ActionEvent event) {
        explorerGoBack();
        event.consume();
    }

    // -------------------------------- Misc Event Handlers --------------------------------

    private void explorerRootPaneOnDragDropped(DragEvent event) {
        List<File> files = event.getDragboard().getFiles();
        String url = event.getDragboard().getUrl();

        rootPane.getScene().getWindow().requestFocus();

        if (files != null && !files.isEmpty()) {
            for (File file : files) {
                if (files.size() == 1 && file.isDirectory()) {
                    screenPane.open(importDialogScreen);
                    importDialogScreen.setGroupName(file.getName());
                    importDialogScreen.setFolder(file);
                } else if (Filters.FILE_NAME_FILTER.accept(file)) {
                    importer.addJob(new ImportJob(file, null));
                }
            }
        } else if (url != null && !url.isEmpty()) {
            try {
                String folder = settings.defaultFolder.getValue();
                String filename = new URL(url).getPath().replaceAll("^.*/", "");
                File target;
                if (!settings.urlFilename.getValue() || folder == null || folder.isEmpty() || !Files.isDirectory(Paths.get(folder))) {
                    do {
                        FileChooser fc = new FileChooser();
                        fc.setTitle("Save as");
                        fc.setSelectedExtensionFilter(Filters.getExtensionFilter());
                        if (folder != null && !folder.isEmpty()) fc.setInitialDirectory(new File(folder));
                        fc.setInitialFileName(filename);

                        target = fc.showSaveDialog(rootPane.getScene().getWindow());

                        if (target == null) return;
                    } while (target.exists() || !target.getParentFile().exists());
                } else {
                    target = resolveDuplicateFilename(new File(folder, filename));
                }
                if (Filters.FILE_NAME_FILTER.accept(target)) importer.addJob(new ImportJob(new URL(url), target, null));
            } catch (MalformedURLException e) {
                LOGGER.log(Level.WARNING, "File dragged from web has bad URL", e);
            }
        }
        event.consume();
    }

    private void explorerRootPaneOnDragOver(DragEvent event) {
        if (event.getGestureSource() == null && (event.getDragboard().hasFiles() || event.getDragboard().hasUrl())) {
            event.acceptTransferModes(TransferMode.ANY);
            event.consume();
        }
    }

    private void explorerRootPaneDisabledChanged(Boolean newValue) {
        if (newValue) {
            if (previewMediaView.isPlaying()) {
                previewMediaView.pause();
                playVideoAfterExplorerEnabled = true;
            }
        } else if (playVideoAfterExplorerEnabled) {
            previewMediaView.play();
            playVideoAfterExplorerEnabled = false;
        }
    }

    // ---------------------------------- Key Event Handlers -------------------------------

    public void explorerRootPaneOnKeyPressed(KeyEvent event) {
        if (event.isControlDown()) {
            switch (event.getCode()) {
                case F:
                    searchTextField.requestFocus();
                    event.consume();
                    break;
                case E:
                    editTagsTextField.setText(lastEditTagString);
                    editTagsTextField.requestFocus();
                    event.consume();
                    break;
                case Q:
                    menagerie.getDatabaseManager().enqueue(() -> cleanExit(false));
                    event.consume();
                    break;
                case S:
                    settingsScreen.open(screenPane, settings);
                    event.consume();
                    break;
                case T:
                    tagListScreen.open(screenPane, menagerie.getTags());
                    event.consume();
                    break;
                case I:
                    screenPane.open(importDialogScreen);
                    event.consume();
                    break;
                case H:
                    screenPane.open(helpScreen);
                    event.consume();
                    break;
                case D:
                    if (event.isShiftDown() && duplicateOptionsScreen.getDuplicatesScreen().getPairs() != null && !duplicateOptionsScreen.getDuplicatesScreen().getPairs().isEmpty()) {
                        duplicateOptionsScreen.getDuplicatesScreen().openWithOldPairs(screenPane, menagerie);
                    } else {
                        duplicateOptionsScreen.open(screenPane, menagerie, itemGridView.getSelected(), currentSearch.getResults(), menagerie.getItems());
                    }
                    event.consume();
                    break;
                case Z:
                    if (tagEditHistory.empty()) {
                        Toolkit.getDefaultToolkit().beep();
                    } else {
                        TagEditEvent peek = tagEditHistory.peek();
                        new ConfirmationScreen().open(screenPane, "Undo last tag edit?", "Tags were added to " + peek.getAdded().keySet().size() + " items.\nTags were removed from " + peek.getRemoved().keySet().size() + " others.", () -> {
                            TagEditEvent pop = tagEditHistory.pop();
                            pop.revertAction();

                            List<Item> list = new ArrayList<>();
                            pop.getAdded().keySet().forEach(item -> {
                                if (!list.contains(item)) list.add(item);
                            });
                            pop.getRemoved().keySet().forEach(item -> {
                                if (!list.contains(item)) list.add(item);
                            });
                            menagerie.refreshInSearches(list);
                        }, null);
                    }
                    event.consume();
                    break;
                case N:
                    screenPane.open(importerScreen);
                    event.consume();
                    break;
                case L:
                    openLog();
                    event.consume();
                    break;
                case M:
                    moveFilesScreen.open(screenPane, itemGridView.getSelected());
                    event.consume();
                    break;
                case R:
                    if (itemGridView.getSelected().size() == 1 && itemGridView.getSelected().get(0) instanceof GroupItem) {
                        openGroupRenameDialog((GroupItem) itemGridView.getSelected().get(0));
                    }
                    event.consume();
                    break;
                default:
                    break;
            }
        } else {
            switch (event.getCode()) {
                case S:
                    double scale = previewMediaView.getImageView().getScale().get();

                    if (scale > 2 && !previewMediaView.getImageView().isScaleApplied()) {
                        BufferedImage bimg = SwingFXUtils.fromFXImage(previewMediaView.getImageView().getTrueImage(), null);

                        ResampleOp resizeOp = new ResampleOp((int) (bimg.getWidth() / scale), (int) (bimg.getHeight() / scale));
                        resizeOp.setUnsharpenMask(AdvancedResizeOp.UnsharpenMask.Normal);
                        resizeOp.setFilter(ResampleFilters.getLanczos3Filter());
                        BufferedImage scaledImage = resizeOp.filter(bimg, bimg);

                        previewMediaView.getImageView().setAppliedScaleImage(SwingFXUtils.toFXImage(scaledImage, null));
                    }
                case ESCAPE:
                    itemGridView.requestFocus();
                    event.consume();
                    break;
                case ENTER:
                    if (itemGridView.getSelected().size() == 1) {
                        Item item = itemGridView.getSelected().get(0);
                        if (item instanceof GroupItem) {
                            explorerOpenGroup((GroupItem) item);
                            event.consume();
                        } else if (itemGridView.getSelected().get(0) instanceof MediaItem) {
                            try {
                                Desktop.getDesktop().open(((MediaItem) item).getFile());
                            } catch (IOException e) {
                                LOGGER.log(Level.SEVERE, "Failed to open file with system default: " + ((MediaItem) item).getFile(), e);
                            }
                            event.consume();
                        }
                    }
                    break;
                case BACK_SPACE:
                    explorerGoBack();
                    event.consume();
                    break;
            }
        }
    }

    public void editTagsTextFieldOnKeyPressed(KeyEvent event) {
        switch (event.getCode()) {
            case ENTER:
                editTagsOfSelected(editTagsTextField.getText());
                editTagsTextField.setText(null);
                itemGridView.requestFocus();
                event.consume();
                break;
            case ESCAPE:
                editTagsTextField.setText(null);
                itemGridView.requestFocus();
                event.consume();
                break;
            default:
                break;
        }
    }

    public void searchVBoxOnKeyPressed(KeyEvent event) {
        if (event.isControlDown()) {
            switch (event.getCode()) {
                case D:
                    listDescendingToggleButton.setSelected(!listDescendingToggleButton.isSelected());
                    event.consume();
                    break;
                case G:
                    showGroupedToggleButton.setSelected(!showGroupedToggleButton.isSelected());
                    event.consume();
                    break;
                case S:
                case R:
                    shuffledSearchButton.setSelected(!shuffledSearchButton.isSelected());
                    event.consume();
                    break;
            }
        }
    }

}
