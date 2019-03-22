package menagerie.gui.screens;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.*;
import menagerie.gui.ItemInfoBox;
import menagerie.gui.TagListCell;
import menagerie.gui.media.DynamicMediaView;
import menagerie.model.SimilarPair;
import menagerie.model.menagerie.MediaItem;
import menagerie.model.menagerie.Menagerie;
import menagerie.model.menagerie.Tag;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class DuplicatesScreen extends Screen {

    private DynamicMediaView leftMediaView, rightMediaView;
    private ListView<Tag> leftTagList, rightTagList;
    private ItemInfoBox leftInfoBox, rightInfoBox;

    private Label similarityLabel;

    private Menagerie menagerie = null;
    private List<SimilarPair> pairs = null;
    private SimilarPair currentPair = null;

    private boolean consolidateTags = true;
    private boolean deleteFile = true;


    public DuplicatesScreen() {
        addEventHandler(KeyEvent.KEY_PRESSED, event -> {
            if (event.getCode() == KeyCode.ESCAPE) {
                close();
                event.consume();
            } else if (event.getCode() == KeyCode.LEFT) {
                previewPrev();
                event.consume();
            } else if (event.getCode() == KeyCode.RIGHT) {
                previewNext();
                event.consume();
            }
        });

        setStyle("-fx-background-color: -fx-base;");

        // ---------------------------------------------- Center Element -----------------------------------------------
        leftMediaView = new DynamicMediaView();
        rightMediaView = new DynamicMediaView();
        leftTagList = new ListView<>();
        leftTagList.setCellFactory(param -> new TagListCell());
        leftTagList.setPrefWidth(200);
        rightTagList = new ListView<>();
        rightTagList.setCellFactory(param -> new TagListCell());
        rightTagList.setPrefWidth(200);
        leftInfoBox = new ItemInfoBox();
        leftInfoBox.setAlignment(Pos.BOTTOM_LEFT);
        leftInfoBox.setMaxHeight(USE_PREF_SIZE);
        leftInfoBox.setOpacity(0.75);
        BorderPane.setAlignment(leftInfoBox, Pos.BOTTOM_LEFT);
        rightInfoBox = new ItemInfoBox();
        rightInfoBox.setAlignment(Pos.BOTTOM_RIGHT);
        rightInfoBox.setMaxHeight(USE_PREF_SIZE);
        rightInfoBox.setOpacity(0.75);
        BorderPane.setAlignment(rightInfoBox, Pos.BOTTOM_RIGHT);
        BorderPane lbp = new BorderPane(null, null, leftTagList, null, leftInfoBox);
        lbp.setPickOnBounds(false);
        BorderPane rbp = new BorderPane(null, null, rightInfoBox, null, rightTagList);
        rbp.setPickOnBounds(false);
        SplitPane sp = new SplitPane(new StackPane(leftMediaView, lbp), new StackPane(rightMediaView, rbp));
        sp.setOnMouseEntered(event -> {
            leftTagList.setDisable(false);
            rightTagList.setDisable(false);
            leftTagList.setOpacity(0.75);
            rightTagList.setOpacity(0.75);
        });
        sp.setOnMouseExited(event -> {
            leftTagList.setDisable(true);
            rightTagList.setDisable(true);
            leftTagList.setOpacity(0);
            rightTagList.setOpacity(0);
        });
        setCenter(sp);

        // ---------------------------------------- Bottom element -----------------------------------------------------
        VBox bottom = new VBox(5);
        bottom.setPadding(new Insets(5));
        // Construct first element
        similarityLabel = new Label("N/A");
        Button leftDeleteButton = new Button("Delete");
        leftDeleteButton.setOnAction(event -> deleteItem(currentPair.getImg1(), currentPair.getImg2()));
        Button rightDeleteButton = new Button("Delete");
        rightDeleteButton.setOnAction(event -> deleteItem(currentPair.getImg2(), currentPair.getImg1()));
        HBox hbl = new HBox(leftDeleteButton);
        hbl.setAlignment(Pos.CENTER_LEFT);
        HBox hbr = new HBox(rightDeleteButton);
        hbr.setAlignment(Pos.CENTER_RIGHT);
        bottom.getChildren().add(new BorderPane(similarityLabel, null, hbr, null, hbl));
        // Construct second element
        Button prevPairButton = new Button("<-");
        prevPairButton.setOnAction(event -> previewPrev());
        Button closeButton = new Button("Close");
        closeButton.setOnAction(event -> close());
        Button nextPairButton = new Button("->");
        nextPairButton.setOnAction(event -> previewNext());
        HBox hb = new HBox(5, prevPairButton, closeButton, nextPairButton);
        hb.setAlignment(Pos.CENTER);
        bottom.getChildren().add(hb);
        setBottom(bottom);


        setDefaultFocusNode(closeButton);
    }

    public void open(ScreenPane manager, Menagerie menagerie, List<SimilarPair> pairs) {
        if (manager == null || menagerie == null || pairs == null || pairs.isEmpty()) return;

        this.menagerie = menagerie;
        this.pairs = pairs;
        preview(pairs.get(0));

        leftTagList.setDisable(false);
        rightTagList.setDisable(false);
        leftTagList.setOpacity(0.75);
        rightTagList.setOpacity(0.75);

        manager.open(this);
    }

    private void previewNext() {
        if (pairs == null || pairs.isEmpty()) return;

        if (currentPair == null) {
            preview(pairs.get(0));
        } else {
            int i = pairs.indexOf(currentPair);
            if (i >= 0) {
                if (i + 1 < pairs.size()) preview(pairs.get(i + 1));
            } else {
                preview(pairs.get(0));
            }
        }
    }

    private void previewPrev() {
        if (pairs == null || pairs.isEmpty()) return;

        if (currentPair == null) {
            preview(pairs.get(0));
        } else {
            int i = pairs.indexOf(currentPair);
            if (i > 0) {
                preview(pairs.get(i - 1));
            } else {
                preview(pairs.get(0));
            }
        }
    }

    private void preview(SimilarPair pair) {
        currentPair = pair;

        if (pair != null) {
            leftMediaView.preview(pair.getImg1());
            rightMediaView.preview(pair.getImg2());

            leftTagList.getItems().clear();
            leftTagList.getItems().addAll(pair.getImg1().getTags());
            leftTagList.getItems().sort(Comparator.comparing(Tag::getName));

            rightTagList.getItems().clear();
            rightTagList.getItems().addAll(pair.getImg2().getTags());
            rightTagList.getItems().sort(Comparator.comparing(Tag::getName));

            leftInfoBox.setItem(pair.getImg1());
            rightInfoBox.setItem(pair.getImg2());

            DecimalFormat df = new DecimalFormat("#.##");
            similarityLabel.setText((pairs.indexOf(pair) + 1) + "/" + pairs.size() + ": " + df.format(pair.getSimilarity() * 100) + "%");
        } else {
            leftMediaView.preview(null);
            rightMediaView.preview(null);

            leftTagList.getItems().clear();
            rightTagList.getItems().clear();

            leftInfoBox.setItem(null);
            rightInfoBox.setItem(null);

            similarityLabel.setText("N/A");
        }
    }

    private void deleteItem(MediaItem toDelete, MediaItem toKeep) {
        if (menagerie == null) return;

        int index = pairs.indexOf(currentPair);

        menagerie.removeImages(Collections.singletonList(toDelete), isDeleteFile());

        //Consolidate tags
        if (isConsolidateTags()) {
            toDelete.getTags().forEach(toKeep::addTag);
        }

        //Remove other pairs containing the deleted image
        for (SimilarPair pair : new ArrayList<>(pairs)) {
            if (toDelete.equals(pair.getImg1()) || toDelete.equals(pair.getImg2())) {
                int i = pairs.indexOf(pair);
                pairs.remove(pair);
                if (i < index) {
                    index--;
                }
            }
        }

        if (index > pairs.size() - 1) index = pairs.size() - 1;

        if (pairs.isEmpty()) {
            close();
        } else {
            preview(pairs.get(index));
        }
    }

    public void releaseMediaPlayers() {
        leftMediaView.releaseMediaPlayer();
        rightMediaView.releaseMediaPlayer();
    }

    public boolean isConsolidateTags() {
        return consolidateTags;
    }

    public boolean isDeleteFile() {
        return deleteFile;
    }

    public void setConsolidateTags(boolean consolidateTags) {
        this.consolidateTags = consolidateTags;
    }

    public void setDeleteFile(boolean deleteFile) {
        this.deleteFile = deleteFile;
    }

    @Override
    protected void onOpen() {
        if (pairs == null || pairs.isEmpty()) {
            close();
            return;
        }

        preview(pairs.get(0));
    }

    @Override
    protected void onClose() {
        menagerie = null;
        pairs = null;
        currentPair = null;

        preview(null);
    }

}
