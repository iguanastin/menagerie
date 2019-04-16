class TagTests {

    //    @Test
    //    void createGet() {
    //        int id = 1;
    //        String name = "tag";
    //        Tag t = new Tag(id, name);
    //        assertEquals(id, t.getId());
    //        assertEquals(name, t.getName());
    //
    //        id = 54321;
    //        name = "tag_two";
    //        t = new Tag(id, name);
    //        assertEquals(id, t.getId());
    //        assertEquals(name, t.getName());
    //    }
    //
    //    @Test
    //    void createNullName() {
    //        assertThrows(NullPointerException.class, () -> new Tag(1, null));
    //    }
    //
    //    @Test
    //    void createBadName() {
    //        assertThrows(IllegalArgumentException.class, () -> new Tag(1, "tag with spaces"));
    //        assertThrows(IllegalArgumentException.class, () -> new Tag(1, "tag with newline\n"));
    //        assertThrows(IllegalArgumentException.class, () -> new Tag(1, "tag with backslash\\"));
    //    }
    //
    //    @Test
    //    void testFrequency() throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
    //        Tag t = new Tag(1, "tag");
    //
    //        assertEquals(0, t.getFrequency());
    //
    //        Method incrementFrequency = Tag.class.getDeclaredMethod("incrementFrequency");
    //        incrementFrequency.setAccessible(true);
    //        incrementFrequency.invoke(t);
    //
    //        assertEquals(1, t.getFrequency());
    //
    //        Method decrementFrequency = Tag.class.getDeclaredMethod("decrementFrequency");
    //        decrementFrequency.setAccessible(true);
    //        decrementFrequency.invoke(t);
    //
    //        assertEquals(0, t.getFrequency());
    //    }
    //
    //    @Test
    //    void equality() {
    //        Tag t1 = new Tag(1, "tag_1");
    //        Tag t2 = new Tag(1, "tag_1");
    //        Tag t3 = new Tag(1, "tag_2");
    //        Tag t4 = new Tag(2, "tag_1");
    //
    //        assertEquals(t1, t1);
    //        assertEquals(t1, t2);
    //        assertEquals(t1, t3);
    //        assertNotEquals(t1, t4);
    //
    //        assertEquals(t1.hashCode(), t2.hashCode());
    //        assertNotEquals(t1.hashCode(), t4.hashCode());
    //    }
    //
    //    @Test
    //    void compare() {
    //        Tag t1 = new Tag(1, "tag_1");
    //        Tag t2 = new Tag(1, "tag_2");
    //        Tag t3 = new Tag(2, "tag_3");
    //
    //        assertEquals(0, t1.compareTo(t1));
    //        assertEquals(0, t1.compareTo(t2));
    //        assertTrue(t1.compareTo(t3) < 0);
    //        assertEquals(-t1.compareTo(t3), t3.compareTo(t1));
    //    }

}
