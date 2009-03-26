package de.lmu.ifi.dbs.elki.utilities;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Logger;

import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.logging.LogLevel;
import de.lmu.ifi.dbs.elki.utilities.pairs.ComparablePair;

/**
 * A wrapper class for storing the k most similar comparable objects.
 *
 * @author Elke Achtert
 */
public class KNNList<D extends Distance<D>> {
    /**
     * The underlying set.
     */
    private SortedSet<ComparablePair<D, Integer>> list;

    /**
     * The maximum size of this list.
     */
    private int k;

    /**
     * The infinite distance.
     */
    private D infiniteDistance;

    /**
     * Creates a new KNNList with the specified parameters.
     *
     * @param k                the number k of objects to be stored
     * @param infiniteDistance the infinite distance
     */
    public KNNList(int k, D infiniteDistance) {
        this.list = new TreeSet<ComparablePair<D, Integer>>();
        this.k = k;
        this.infiniteDistance = infiniteDistance;
    }

    /**
     * Adds a new object to this list. If this list contains already k entries
     * and the key of the specified object o is less than the key of the last
     * entry, the last entry will be deleted.
     *
     * @param o the object to be added
     * @return true, if o has been added, false otherwise.
     */
    public boolean add(ComparablePair<D, Integer> o) {
        if (list.size() < k) {
            list.add(o);
            return true;
        }

        try {
          ComparablePair<D, Integer> last = list.last();
            D lastDist = last.getFirst();

            if (o.getFirst().compareTo(lastDist) < 0) {
                SortedSet<ComparablePair<D, Integer>> lastList = list.subSet(new ComparablePair<D, Integer>(lastDist, 0), new ComparablePair<D, Integer>(lastDist, Integer.MAX_VALUE));

                int llSize = lastList.size();
                if (list.size() - llSize >= k - 1) {
                    for (int i = 0; i < llSize; i++) {
                        list.remove(list.last());
                    }
                }
                list.add(o);
                return true;
            }

            if (o.getFirst().compareTo(last.getFirst()) == 0) {
                list.add(o);
                return true;
            }

            return false;
        }
        catch (Exception e) { // TODO more specialized??
            Logger.getLogger(this.getClass().getName()).log(LogLevel.EXCEPTION, "k "+k+"\n"+"list "+list, e);
            throw new RuntimeException(e);
        }
    }

    /**
     * Returns the k-th distance of this list (e.g. the key of the k-th
     * element). If this list is empty or contains less than k elements, an
     * infinite key will be returned.
     *
     * @return the maximum distance of this list
     */
    public D getKNNDistance() {
        if (list.size() < k) {
            return infiniteDistance;
        }
        return getMaximumDistance();
    }

    /**
     * Returns the maximum distance of this list (e.g. the key of the last
     * element). If this list is empty an infinite key will be returned.
     *
     * @return the maximum distance of this list
     */
    public D getMaximumDistance() {
        if (list.isEmpty()) {
            return infiniteDistance;
        }
        ComparablePair<D, Integer> last = list.last();
        return last.getFirst();
    }

    /**
     * Returns a list representation of this KList.
     *
     * @return a list representation of this KList
     */
    public List<ComparablePair<D, Integer>> toList() {
        return new ArrayList<ComparablePair<D, Integer>>(list);
    }

    public List<D> distancesToList() {
        List<D> knnDistances = new ArrayList<D>();
        List<ComparablePair<D, Integer>> qr = toList();

        for (int i = 0; i < qr.size() && i < k; i++) {
            knnDistances.add(qr.get(i).getFirst());
        }

        for (int i = qr.size(); i < k; i++) {
            knnDistances.add(infiniteDistance);
        }

        return knnDistances;
    }

    public List<Integer> idsToList() {
        List<Integer> ids = new ArrayList<Integer>(k);
        List<ComparablePair<D, Integer>> qr = toList();
        for (int i = 0; i < qr.size() && i < k; i++) {
            ids.add(qr.get(i).getSecond());
        }
        return ids;
    }

    /**
     * Returns the current size of this list.
     *
     * @return the current size of this list
     */
    public int size() {
        return list.size();
    }

    /**
     * Returns the maximum size of this list.
     *
     * @return the maximum size of this list
     */
    public int getK() {
        return k;
    }

    /**
     * Returns a string representation of the object.
     *
     * @return a string representation of the object.
     */
    @Override
    public String toString() {
        return list + " , knn-dist = " + getKNNDistance();
    }

    /**
     * Indicates whether some other object is "equal to" this one.
     *
     * @param o the reference object with which to compare.
     * @return <code>true</code> if this object is the same as the obj
     *         argument; <code>false</code> otherwise.
     */
    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        // noinspection unchecked
        final KNNList<D> knnList = (KNNList<D>) o;

        if (k != knnList.k) {
            return false;
        }
        Iterator<ComparablePair<D, Integer>> it = list.iterator();
        Iterator<ComparablePair<D, Integer>> other_it = knnList.list.iterator();

        while (it.hasNext()) {
            ComparablePair<D, Integer> next = it.next();
            ComparablePair<D, Integer> other_next = other_it.next();

            if (!next.equals(other_next)) {
                return false;
            }

        }
        return list.equals(knnList.list);
    }

    @Override
    public int hashCode() {
        int result;
        result = list.hashCode();
        result = 29 * result + k;
        return result;
    }
}
