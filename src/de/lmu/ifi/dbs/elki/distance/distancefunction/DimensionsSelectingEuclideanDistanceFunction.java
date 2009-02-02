package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;

/**
 * Provides a distance function that computes the Euclidean distance
 * between feature vectors only in specified dimensions.
 *
 * @author Elke Achtert
 * @param <V> the type of NumberVector to compute the distances in between
 */
public class DimensionsSelectingEuclideanDistanceFunction<V extends NumberVector<V, ?>>
    extends AbstractDimensionsSelectingDoubleDistanceFunction<V>
    implements SpatialDistanceFunction<V, DoubleDistance> {

    /**
     * Provides a distance function that computes the Euclidean distance
     * between feature vectors only in specified dimensions
     */
    public DimensionsSelectingEuclideanDistanceFunction() {
        super();
    }


    /**
     * Provides the Euclidean distance
     * between two given feature vectors in the selected dimensions.
     *
     * @param v1 first feature vector
     * @param v2 second feature vector
     * @return the Euclidean distance
     *         between two given feature vectors in the selected dimensions
     */
    public DoubleDistance distance(V v1, V v2) {
        if (v1.getDimensionality() != v2.getDimensionality()) {
            throw new IllegalArgumentException("Different dimensionality of NumberVectors\n  " +
                "first argument: " + v1 + "\n  " +
                "second argument: " + v2);
        }


        if (v1.getDimensionality() < getSelectedDimensions().cardinality()) {
            throw new IllegalArgumentException("The dimensionality of the feature space " +
                "is not consistent with the specified dimensions " +
                "to be considered for distance computation.\n  " +
                "dimensionality of the feature space: " + v1.getDimensionality() + "\n  " +
                "specified dimensions: " + getSelectedDimensions());
        }


        double sqrDist = 0;
        for (int d = getSelectedDimensions().nextSetBit(0); d >= 0; d = getSelectedDimensions().nextSetBit(d + 1)) {
            double manhattanI = v1.getValue(d + 1).doubleValue() - v2.getValue(d + 1).doubleValue();
            sqrDist += manhattanI * manhattanI;
        }
        return new DoubleDistance(Math.sqrt(sqrDist));
    }

    @Override
    public String parameterDescription() {
        return "Euclidean distance for feature vectors considering only specified dimensions. " +
            "No parameters required. " +
            "Pattern for defining a range: \"" + requiredInputPattern() + "\".";
    }

    public DoubleDistance minDist(HyperBoundingBox mbr, V v) {
        if (mbr.getDimensionality() != v.getDimensionality()) {
            throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr.toString() + "\n  " + "second argument: " + v.toString());
        }
        if (v.getDimensionality() < getSelectedDimensions().size()) {
            throw new IllegalArgumentException("The dimensionality of the feature space " +
                "is not consistent with the specified dimensions " +
                "to be considered for distance computation.\n  " +
                "dimensionality of the feature space: " + v.getDimensionality() + "\n  " +
                "specified dimensions: " + getSelectedDimensions());
        }

        double sqrDist = 0;
        for (int d = getSelectedDimensions().nextSetBit(0); d >= 0; d = getSelectedDimensions().nextSetBit(d + 1)) {
            double value = v.getValue(d).doubleValue();
            double r;
            if (value < mbr.getMin(d))
                r = mbr.getMin(d);
            else if (value > mbr.getMax(d))
                r = mbr.getMax(d);
            else
                r = value;

            double manhattanI = value - r;
            sqrDist += manhattanI * manhattanI;
        }
        return new DoubleDistance(Math.sqrt(sqrDist));
    }

    public DoubleDistance minDist(HyperBoundingBox mbr, Integer id) {
        return minDist(mbr, getDatabase().get(id));
    }

    public DoubleDistance distance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
        if (mbr1.getDimensionality() != mbr2.getDimensionality()) {
            throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
        }
        if (mbr1.getDimensionality() < getSelectedDimensions().size()) {
            throw new IllegalArgumentException("The dimensionality of the feature space " +
                "is not consistent with the specified dimensions " +
                "to be considered for distance computation.\n  " +
                "dimensionality of the feature space: " + mbr1.getDimensionality() + "\n  " +
                "specified dimensions: " + getSelectedDimensions());
        }

        double sqrDist = 0;
        for (int d = getSelectedDimensions().nextSetBit(0); d >= 0; d = getSelectedDimensions().nextSetBit(d + 1)) {
            double m1, m2;
            if (mbr1.getMax(d) < mbr2.getMin(d)) {
                m1 = mbr1.getMax(d);
                m2 = mbr2.getMin(d);
            }
            else if (mbr1.getMin(d) > mbr2.getMax(d)) {
                m1 = mbr1.getMin(d);
                m2 = mbr2.getMax(d);
            }
            else { // The mbrs intersect!
                m1 = 0;
                m2 = 0;
            }
            double manhattanI = m1 - m2;
            sqrDist += manhattanI * manhattanI;
        }
        return new DoubleDistance(Math.sqrt(sqrDist));
    }

    public DoubleDistance centerDistance(HyperBoundingBox mbr1, HyperBoundingBox mbr2) {
        if (mbr1.getDimensionality() != mbr2.getDimensionality()) {
            throw new IllegalArgumentException("Different dimensionality of objects\n  " + "first argument: " + mbr1.toString() + "\n  " + "second argument: " + mbr2.toString());
        }
        if (mbr1.getDimensionality() < getSelectedDimensions().size()) {
            throw new IllegalArgumentException("The dimensionality of the feature space " +
                "is not consistent with the specified dimensions " +
                "to be considered for distance computation.\n  " +
                "dimensionality of the feature space: " + mbr1.getDimensionality() + "\n  " +
                "specified dimensions: " + getSelectedDimensions());
        }

        double sqrDist = 0;
        for (int d = getSelectedDimensions().nextSetBit(0); d >= 0; d = getSelectedDimensions().nextSetBit(d + 1)) {
            double c1 = (mbr1.getMin(d) + mbr1.getMax(d)) / 2;
            double c2 = (mbr2.getMin(d) + mbr2.getMax(d)) / 2;

            double manhattanI = c1 - c2;
            sqrDist += manhattanI * manhattanI;
        }
        return new DoubleDistance(Math.sqrt(sqrDist));
    }
}
