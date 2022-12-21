package menagerie.model.search.rules;

import menagerie.model.menagerie.GroupItem;
import menagerie.model.menagerie.MediaItem;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class TypeRuleTest {

    @Test
    void testAccept_groupItem() {
        GroupItem groupItem = new GroupItem(null, 1, 1, "");

        TypeRule groupRule = new TypeRule(TypeRule.Type.GROUP, false);
        TypeRule mediaRule = new TypeRule(TypeRule.Type.MEDIA, false);
        TypeRule imageRule = new TypeRule(TypeRule.Type.IMAGE, false);
        TypeRule videoRule = new TypeRule(TypeRule.Type.VIDEO, false);

        assertTrue(groupRule.accept(groupItem));
        assertFalse(mediaRule.accept(groupItem));
        assertFalse(imageRule.accept(groupItem));
        assertFalse(videoRule.accept(groupItem));
    }

    @Test
    void testAccept_mediaItem() {
        MediaItem mediaItemImage = mock(MediaItem.class);
        when(mediaItemImage.isImage()).thenReturn(true);

        TypeRule groupRule = new TypeRule(TypeRule.Type.GROUP, false);
        TypeRule mediaRule = new TypeRule(TypeRule.Type.MEDIA, false);
        TypeRule imageRule = new TypeRule(TypeRule.Type.IMAGE, false);
        TypeRule videoRule = new TypeRule(TypeRule.Type.VIDEO, false);

        assertFalse(groupRule.accept(mediaItemImage));
        assertTrue(mediaRule.accept(mediaItemImage));
        assertTrue(imageRule.accept(mediaItemImage));
        assertFalse(videoRule.accept(mediaItemImage));
    }
}