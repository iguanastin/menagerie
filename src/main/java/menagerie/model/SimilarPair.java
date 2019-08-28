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
     * @throws NullPointerException     When obj1 or obj2 is null.
     * @throws IllegalArgumentException When similarity is outside of range [0.0-1.0]
     */
    public SimilarPair(T obj1, T obj2, double similarity) {
        if (obj1 == null || obj2 == null) throw new NullPointerException("Objects must not be null");
        if (obj1.equals(obj2)) throw new IllegalArgumentException("Objects must not be equal");
        if (similarity < 0 || similarity > 1) throw new IllegalArgumentException("Similarity must be between 0 and 1, inclusive");

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

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof SimilarPair)) return false;

        if (getObject1().equals(((SimilarPair) obj).getObject1()) && getObject2().equals(((SimilarPair) obj).getObject2())) {
            return true;
        } else {
            return getObject1().equals(((SimilarPair) obj).getObject2()) && getObject2().equals(((SimilarPair) obj).getObject1());
        }
    }

    @Override
    public int hashCode() {
        return obj1.hashCode() + obj2.hashCode();
    }

}
