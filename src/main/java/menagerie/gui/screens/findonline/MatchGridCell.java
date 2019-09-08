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

package menagerie.gui.screens.findonline;

import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.effect.DropShadow;
import javafx.scene.image.Image;
import javafx.scene.layout.StackPane;
import menagerie.duplicates.Match;
import menagerie.gui.grid.ItemGridCell;
import menagerie.gui.media.DynamicImageView;
import org.controlsfx.control.GridCell;

import java.lang.ref.SoftReference;
import java.util.HashMap;
import java.util.Map;

public class MatchGridCell extends GridCell<Match> {

    private final DynamicImageView view = new DynamicImageView();
    private final Label sourceLabel = new Label();
    private final Label resSimLabel = new Label();

    private static final Map<String, SoftReference<Image>> thumbCache = new HashMap<>();


    public MatchGridCell() {
        super();
        this.getStyleClass().add(ItemGridCell.DEFAULT_STYLE_CLASS);

        view.setFitWidth(150);
        view.setFitHeight(150);
        sourceLabel.setPadding(new Insets(5));
        resSimLabel.setPadding(new Insets(5));
        DropShadow ds = new DropShadow();
        ds.setSpread(0.5);
        sourceLabel.setEffect(ds);
        resSimLabel.setEffect(ds);
        StackPane.setAlignment(view, Pos.CENTER);
        StackPane.setAlignment(sourceLabel, Pos.TOP_LEFT);
        StackPane.setAlignment(resSimLabel, Pos.BOTTOM_RIGHT);
        setGraphic(new StackPane(view, sourceLabel, resSimLabel));
        setPrefSize(156, 156);
        setMinSize(USE_PREF_SIZE, USE_PREF_SIZE);
        setMaxSize(USE_PREF_SIZE, USE_PREF_SIZE);
    }

    @Override
    protected void updateItem(Match item, boolean empty) {
        super.updateItem(item, empty);

        if (item == null || item.getThumbnailURL() == null || item.getThumbnailURL().isEmpty()) {
            view.setImage(null);
        } else {
            SoftReference<Image> ref = thumbCache.get(item.getThumbnailURL());
            Image image;
            if (ref == null) {
                image = new Image(item.getThumbnailURL(), 150, 150, true, true, true);
                thumbCache.put(item.getThumbnailURL(), new SoftReference<>(image));
            } else {
                image = ref.get();
                if (image == null) image = new Image(item.getThumbnailURL(), 150, 150, true, true, true);
            }
            view.setImage(image);

            sourceLabel.setText(item.getPageURL().replace("http://", "").replace("https://", "").replaceAll("/.*", ""));
            resSimLabel.setText(item.getWidth() + "x" + item.getHeight() + " - " + (int) (item.getSimilarity() * 100) + "%");
        }
    }

}
