package elki.clustering.hierarchical.betula;

import elki.clustering.hierarchical.betula.features.ClusterFeature;
import elki.utilities.exceptions.AbortException;

public class CFDistanceMatrix {

    int size;

    ClusterFeature[] cfs;

    double[] matrix;

    /**
     * Constructor.
     *
     * @param ids Database ids.
     */
    public CFDistanceMatrix(ClusterFeature[] cfs) {
        size = cfs.length;
        if(size > 0x10000) {
            throw new AbortException("This implementation does not scale to data sets larger than " + //
                    0x10000 // = 65535
                    + " instances (~16 GB RAM), at which point the Java maximum array size is reached.");
        }
        this.cfs = cfs;
        matrix = new double[triangleSize(size)];
    }

    /**
     * Compute the size of a complete x by x triangle (minus diagonal)
     *
     * @param x Offset
     * @return Size of complete triangle
     */
    public static int triangleSize(int x) {
        return (x * (x - 1)) >>> 1;
    }

    /**
     * Get a value from the (upper triangular) distance matrix.
     * <p>
     * Note: in many cases, linear iteration over the matrix will be fastet than
     * repeated calls to this method!
     *
     * @param x First object
     * @param y Second object
     * @return Distance
     */
    public double get(int x, int y) {
        return x == y ? 0 : x < y ? matrix[triangleSize(y) + x] : matrix[triangleSize(x) + y];
    }
}
