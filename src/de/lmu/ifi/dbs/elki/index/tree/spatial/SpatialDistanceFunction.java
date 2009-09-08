package de.lmu.ifi.dbs.elki.index.tree.spatial;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.HyperBoundingBox;

/**
 * Defines the requirements for a distance function that can used in spatial index
 * to measure the dissimilarity between spatial data objects.
 *
 * @author Elke Achtert
 * @param <V> the type of FeatureVector to compute the distances in between
 * @param <D> distance type
 */
public interface SpatialDistanceFunction<V extends NumberVector<V, ?>, D extends Distance<D>> extends DistanceFunction<V, D> {

    /**
     * Computes the minimum distance between the given MBR and the FeatureVector object
     * according to this distance function.
     *
     * @param mbr the MBR object
     * @param v   the FeatureVector object
     * @return the minimum distance between the given MBR and the FeatureVector object
     *         according to this distance function
     */
    D minDist(HyperBoundingBox mbr, V v);

    /**
     * Computes the minimum distance between the given MBR and the FeatureVector object
     * with the given id according to this distance function.
     *
     * @param mbr the MBR object
     * @param id  the id of the FeatureVector object
     * @return the minimum distance between the given MBR and the FeatureVector object
     *         according to this distance function
     */
    D minDist(HyperBoundingBox mbr, Integer id);

    /**
     * Computes the distance between the two given MBRs
     * according to this distance function.
     *
     * @param mbr1 the first MBR object
     * @param mbr2 the second MBR object
     * @return the distance between the two given MBRs according to this distance function
     */
    D distance(HyperBoundingBox mbr1, HyperBoundingBox mbr2);

    /**
     * Computes the distance between the centroids of the two given MBRs
     * according to this distance function.
     *
     * @param mbr1 the first MBR object
     * @param mbr2 the second MBR object
     * @return the distance between the centroids of the two given MBRs
     *         according to this distance function
     */
    D centerDistance(HyperBoundingBox mbr1, HyperBoundingBox mbr2);
}
