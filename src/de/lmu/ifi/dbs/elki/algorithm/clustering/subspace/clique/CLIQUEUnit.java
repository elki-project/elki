package de.lmu.ifi.dbs.elki.algorithm.clustering.subspace.clique;

import de.lmu.ifi.dbs.elki.data.RealVector;
import de.lmu.ifi.dbs.elki.utilities.Interval;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;

/**
 * Represents a unit in the CLIQUE algorithm.
 *
 * @author Elke Achtert
 * @param <V> the type of RealVector handled by this Algorithm
 */
public class CLIQUEUnit<V extends RealVector<V, ?>> {
    /**
     * The one-dimensional intervals of which this unit is build.
     */
    private SortedSet<Interval> intervals;

    /**
     * Provides a mapping of particular dimensions to the intervals of which this unit is build.
     */
    private Map<Integer, Interval> dimensionToInterval;

    /**
     * The ids of the feature vectors this unit contains.
     */
    private Set<Integer> ids;

    /**
     * Flag that indicates if this unit is already assigned to a cluster.
     */
    private boolean assigned;

    /**
     * Creates a new k-dimensional unit for the given intervals.
     *
     * @param intervals the intervals belonging to this unit
     * @param ids       the ids of the feature vectors belonging to this unit
     */
    public CLIQUEUnit(SortedSet<Interval> intervals, Set<Integer> ids) {
        this.intervals = intervals;

        dimensionToInterval = new HashMap<Integer, Interval>();
        for (Interval interval : intervals) {
            dimensionToInterval.put(interval.getDimension(), interval);
        }

        this.ids = ids;

        assigned = false;
    }

    /**
     * Creates a new one-dimensional unit for the given interval.
     *
     * @param interval the interval belonging to this unit
     */
    public CLIQUEUnit(Interval interval) {
        intervals = new TreeSet<Interval>();
        intervals.add(interval);

        dimensionToInterval = new HashMap<Integer, Interval>();
        dimensionToInterval.put(interval.getDimension(), interval);

        ids = new HashSet<Integer>();

        assigned = false;
    }

    /**
     * Retuns true, if the intervals of this unit contain the specified
     * feature vector.
     *
     * @param vector the feature vector to be tested for containment
     * @return true, if the intervals of this unit contain the specified
     *         feature vector, false otherwise
     */
    public boolean contains(V vector) {
        for (Interval interval : intervals) {
            double value = vector.getValue(interval.getDimension() + 1).doubleValue();
            if (interval.getMin() > value || value >= interval.getMax())
                return false;
        }
        return true;
    }

    /**
     * Adds the id of the specified feature vector to this unit, if
     * this unit contains the feature vector.
     *
     * @param vector the feature vector to be added
     * @return true, if this unit contains the specified
     *         feature vector, false otherwise
     */
    public boolean addFeatureVector(V vector) {
        if (contains(vector)) {
            ids.add(vector.getID());
            return true;
        }
        return false;
    }

    /**
     * Returns the number of feature vectors this unit contains.
     *
     * @return the number of feature vectors this unit contains
     */
    public int numberOfFeatureVectors() {
        return ids.size();
    }

    /**
     * Returns the selectivity of this unit, which is defined
     * as the fraction of total feature vectors contained in this unit.
     *
     * @param total the total number of feature vectors
     * @return the selectivity of this unit
     */
    public double selectivity(double total) {
        return ((double) ids.size()) / total;
    }

    /**
     * Returns a collection of the intervals of which this unit is build.
     *
     * @return a collection of the intervals of which this unit is build
     */
    public Collection<Interval> getIntervals() {
        return intervals;
    }

    /**
     * Returns the interval of the specified dimension.
     *
     * @param dimension the dimension of the interval to be returned
     * @return the interval of the specified dimension
     */
    public Interval getInterval(Integer dimension) {
        return dimensionToInterval.get(dimension);
    }

    /**
     * Returns true if this unit contains the
     * left neighbor of the specified interval.
     *
     * @param i the interval
     * @return true if this unit contains the
     *         left neighbor of the specified interval, false otherwise
     */
    public boolean containsLeftNeighbor(Interval i) {
        Interval interval = dimensionToInterval.get(i.getDimension());
        if (interval == null) return false;
        return interval.getMax() == i.getMin();
    }

    /**
     * Returns true if this unit contains the
     * right neighbor of the specified interval.
     *
     * @param i the interval
     * @return true if this unit contains the
     *         right neighbor of the specified interval, false otherwise
     */
    public boolean containsRightNeighbor(Interval i) {
        Interval interval = dimensionToInterval.get(i.getDimension());
        if (interval == null) return false;
        return interval.getMin() == i.getMax();
    }

    /**
     * Returns true if this unit is already assigned to a cluster.
     *
     * @return true if this unit is already assigned to a cluster, false otherwise.
     */
    public boolean isAssigned() {
        return assigned;
    }

    /**
     * Marks this unit as assigned to a cluster.
     */
    public void markAsAssigned() {
        this.assigned = true;
    }

    /**
     * Returns the ids of the feature vectors this unit contains.
     *
     * @return the ids of the feature vectors this unit contains
     */
    public Set<Integer> getIds() {
        return ids;
    }

    /**
     * Joins this unit with the specified unit.
     *
     * @param other the unit to be joined
     * @param all   the overall number of featuer vectors
     * @param tau   the density threshold for the selectivity of a unit
     * @return the joined unit if the selectivity of the join result is equal
     *         or greater than tau, null otwerwise
     */
    public CLIQUEUnit<V> join(CLIQUEUnit<V> other, double all, double tau) {
        Interval i1 = this.intervals.last();
        Interval i2 = other.intervals.last();
        if (i1.getDimension() >= i2.getDimension()) {
            return null;
        }

        Iterator<Interval> it1 = this.intervals.iterator();
        Iterator<Interval> it2 = other.intervals.iterator();
        SortedSet<Interval> resultIntervals = new TreeSet<Interval>();
        for (int i = 0; i < this.intervals.size() - 1; i++) {
            i1 = it1.next();
            i2 = it2.next();
            if (!i1.equals(i2)) {
                return null;
            }
            resultIntervals.add(i1);
        }
        resultIntervals.add(this.intervals.last());
        resultIntervals.add(other.intervals.last());

        Set<Integer> resultIDs = new TreeSet<Integer>(this.ids);
        resultIDs.retainAll(other.ids);

        if (resultIDs.size() / all >= tau)
            return new CLIQUEUnit<V>(resultIntervals, resultIDs);

        return null;
    }

    /**
     * Returns a string representation of this unit
     * that contains the intervals of this unit.
     *
     * @return a string representation of this unit
     */
    public String toString() {
        StringBuffer result = new StringBuffer();
        for (Interval interval : intervals)
            result.append(interval).append(" ");

        return result.toString();
    }
}
