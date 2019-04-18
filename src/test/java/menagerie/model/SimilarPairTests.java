package menagerie.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class SimilarPairTests {

    @Test
    void createGet() {
        String s1 = "string1", s2 = "String 2";
        double similarity = 0.95;
        SimilarPair<String> s = new SimilarPair<>(s1, s2, similarity);

        assertEquals(s1, s.getObject1());
        assertEquals(s2, s.getObject2());
        assertEquals(similarity, s.getSimilarity());
    }

    @Test
    void createNull() {
        String s1 = "string1";
        String s2 = "String 2";
        double similarity = 0.5;

        assertThrows(NullPointerException.class, () -> new SimilarPair<>(null, s2, similarity));
        assertThrows(NullPointerException.class, () -> new SimilarPair<>(s1, null, similarity));
        assertThrows(NullPointerException.class, () -> new SimilarPair<>(null, null, similarity));
    }

    @Test
    void similarRange() {
        String s1 = "string1", s2 = "String 2";

        assertDoesNotThrow(() -> new SimilarPair<>(s1, s2, 0));
        assertDoesNotThrow(() -> new SimilarPair<>(s1, s2, 1));
        assertDoesNotThrow(() -> new SimilarPair<>(s1, s2, 0.25));

        assertThrows(IllegalArgumentException.class, () -> new SimilarPair<>(s1, s2, -1.2345));
        assertThrows(IllegalArgumentException.class, () -> new SimilarPair<>(s1, s2, -1000000));
        assertThrows(IllegalArgumentException.class, () -> new SimilarPair<>(s1, s2, 1.1));
        assertThrows(IllegalArgumentException.class, () -> new SimilarPair<>(s1, s2, 12312312.5));
    }

    @Test
    void equalObjects() {
        String str = "string1";

        assertThrows(IllegalArgumentException.class, () -> new SimilarPair<>(str, str, 0.5));
    }

}
