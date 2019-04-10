package menagerie.model;

/**
 * An object representing a pair and their similarity.
 *
 * @param <T> Object type.
 */
public class SimilarPair<T> {

    private final T obj1, obj2;
    private final double similarity;


    /**
     * Constructs a similar pair.
     *
     * @param obj1       First object.
     * @param obj2       Second object.
     * @param similarity Similarity of the two objects. [0.0-1.0]
     *
     * @throws NullPointerException     When obj1 or obj2 is null.
     * @throws IllegalArgumentException When similarity is outside of range [0.0-1.0]
     */
    public SimilarPair(T obj1, T obj2, double similarity) {
        if (obj1 == null || obj2 == null) throw new NullPointerException("Objects must not be null");
        if (obj1.equals(obj2)) throw new IllegalArgumentException("Objects must not be equal");
        if (similarity < 0 || similarity > 1)
            throw new IllegalArgumentException("Similarity must be between 0 and 1, inclusive");

        this.obj1 = obj1;
        this.obj2 = obj2;
        this.similarity = similarity;
    }

    /**
     * @return The similarity between the pair as a percentage. [0.0-1.0]
     */
    public double getSimilarity() {
        return similarity;
    }

    /**
     * @return The first object.
     */
    public T getObject1() {
        return obj1;
    }

    /**
     * @return The second object.
     */
    public T getObject2() {
        return obj2;
    }

}
