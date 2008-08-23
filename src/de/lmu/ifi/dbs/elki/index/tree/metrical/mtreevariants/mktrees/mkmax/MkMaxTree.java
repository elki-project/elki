package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkmax;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.distance.Distance;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree;
import de.lmu.ifi.dbs.elki.utilities.KNNList;
import de.lmu.ifi.dbs.elki.utilities.QueryResult;
import de.lmu.ifi.dbs.elki.utilities.QueryStatistic;
import de.lmu.ifi.dbs.elki.utilities.Util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * MkMaxTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for parameter
 * k <= k_max. The k-nearest neigbor distance for k = k_max is stored in each entry of a node.
 *
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be stored in the MkMaxTree
 * @param <D> the type of Distance used in the MkMaxTree
 */
public class MkMaxTree<O extends DatabaseObject, D extends Distance<D>>
    extends AbstractMkTree<O, D, MkMaxTreeNode<O, D>, MkMaxEntry<D>> {

    /**
     * Provides some statistics about performed reverse knn-queries.
     */
    private QueryStatistic rkNNStatistics = new QueryStatistic();

    /**
     * Creates a new MkMaxTree.
     */
    public MkMaxTree() {
        super();
        this.debug = true;
    }

    /**
     * Inserts the specified object into this MkMax-Tree
     * by calling {@link AbstractMTree#insert(de.lmu.ifi.dbs.elki.data.DatabaseObject,boolean)
     * AbstractMTree.insert(object, true)}.
     *
     * @param object the object to be inserted
     */
    public void insert(O object) {
        this.insert(object, true);
    }

    /**
     * Performs a reverse k-nearest neighbor query for the given object ID.
     * In the first step the candidates are chosen by performing
     * a reverse k-nearest neighbor query with k = {@link #k_max}. Then
     * these candidates are refined in a second step.
     */
    public List<QueryResult<D>> reverseKNNQuery(O object, int k) {
        if (k > this.k_max) {
            throw new IllegalArgumentException("Parameter k has to be equal or less than " + "parameter k of the MkMax-Tree!");
        }

        // get the candidates
        List<QueryResult<D>> candidates = new ArrayList<QueryResult<D>>();
        doReverseKNNQuery(object.getID(), getRoot(), null, candidates);

        if (k == this.k_max) {
            Collections.sort(candidates);
            rkNNStatistics.addTrueHits(candidates.size());
            rkNNStatistics.addResults(candidates.size());
            return candidates;
        }

        // refinement of candidates
        Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();
        List<Integer> candidateIDs = new ArrayList<Integer>();
        for (QueryResult<D> candidate : candidates) {
            KNNList<D> knns = new KNNList<D>(k, getDistanceFunction().infiniteDistance());
            knnLists.put(candidate.getID(), knns);
            candidateIDs.add(candidate.getID());
        }
        batchNN(getRoot(), candidateIDs, knnLists);

        List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
        for (Integer id : candidateIDs) {
            List<QueryResult<D>> knns = knnLists.get(id).toList();
            for (QueryResult<D> qr : knns) {
                if (qr.getID() == object.getID()) {
                    result.add(new QueryResult<D>(id, qr.getDistance()));
                    break;
                }
            }
        }

        rkNNStatistics.addResults(result.size());
        rkNNStatistics.addCandidates(candidates.size());
        Collections.sort(result);
        return result;
    }

    /**
     * Returns the statistic for performed rknn queries.
     *
     * @return the statistic for performed rknn queries
     */
    public QueryStatistic getRkNNStatistics() {
        return rkNNStatistics;
    }

    /**
     * Clears the values of the statistic for performed rknn queries
     */
    public void clearRkNNStatistics() {
        rkNNStatistics.clear();
    }

    /**
     * Adapts the knn distances before insertion of the specified entry.
     *
     */
    protected void preInsert(MkMaxEntry<D> entry) {
        KNNList<D> knns_o = new KNNList<D>(k_max, getDistanceFunction().infiniteDistance());
        preInsert(entry, getRootEntry(), knns_o);
    }

    /**
     * Adjusts the knn distance in the subtree of the specified root entry.
     */
    protected void kNNdistanceAdjustment(MkMaxEntry<D> entry, Map<Integer, KNNList<D>> knnLists) {
        MkMaxTreeNode<O, D> node = file.readPage(entry.getID());
        D knnDist_node = getDistanceFunction().nullDistance();
        if (node.isLeaf()) {
            for (int i = 0; i < node.getNumEntries(); i++) {
                MkMaxEntry<D> leafEntry = node.getEntry(i);
                leafEntry.setKnnDistance(knnLists.get(leafEntry.getID()).getKNNDistance());
                knnDist_node = Util.max(knnDist_node, leafEntry.getKnnDistance());
            }
        }
        else {
            for (int i = 0; i < node.getNumEntries(); i++) {
                MkMaxEntry<D> dirEntry = node.getEntry(i);
                kNNdistanceAdjustment(dirEntry, knnLists);
                knnDist_node = Util.max(knnDist_node, dirEntry.getKnnDistance());
            }
        }
        entry.setKnnDistance(knnDist_node);
    }

    /**
     * Performs a reverse k-nearest neighbor query in the specified subtree
     * for the given query object with k = {@link #k_max}.
     * It recursively traverses all paths from the specified node, which cannot be excluded from leading to
     * qualififying objects.
     *
     * @param q          the id of the query object
     * @param node       the node of the subtree on which the query is performed
     * @param node_entry the entry representing the node
     * @param result     the list for the query result
     */
    private void doReverseKNNQuery(Integer q,
                                   MkMaxTreeNode<O, D> node,
                                   MkMaxEntry<D> node_entry,
                                   List<QueryResult<D>> result) {

        // data node
        if (node.isLeaf()) {
            for (int i = 0; i < node.getNumEntries(); i++) {
                MkMaxEntry<D> entry = node.getEntry(i);
                D distance = getDistanceFunction().distance(entry.getRoutingObjectID(), q);
                if (distance.compareTo(entry.getKnnDistance()) <= 0) {
                    result.add(new QueryResult<D>(entry.getRoutingObjectID(), distance));
                }
            }
        }

        // directory node
        else {
            for (int i = 0; i < node.getNumEntries(); i++) {
                MkMaxEntry<D> entry = node.getEntry(i);
                D node_knnDist = node_entry != null ? node_entry.getKnnDistance() : getDistanceFunction().infiniteDistance();

                D distance = getDistanceFunction().distance(entry.getRoutingObjectID(), q);
                D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                    getDistanceFunction().nullDistance() :
                    distance.minus(entry.getCoveringRadius());

                if (minDist.compareTo(node_knnDist) <= 0) {
                    MkMaxTreeNode<O, D> childNode = getNode(entry.getID());
                    doReverseKNNQuery(q, childNode, entry, result);
                }
            }
        }
    }

    /**
     * Adapts the knn distances before insertion of entry q.
     *
     * @param q         the entry to be inserted
     * @param nodeEntry the entry representing the root of thge current subtree
     * @param knns_q    the knns of q
     */
    private void preInsert(MkMaxEntry<D> q, MkMaxEntry<D> nodeEntry, KNNList<D> knns_q) {
        if (this.debug) {
            debugFine("\npreInsert " + q + " - " + nodeEntry + "\n");
        }

        D knnDist_q = knns_q.getKNNDistance();
        MkMaxTreeNode<O, D> node = file.readPage(nodeEntry.getID());
        D knnDist_node = getDistanceFunction().nullDistance();

        // leaf node
        if (node.isLeaf()) {
            for (int i = 0; i < node.getNumEntries(); i++) {
                MkMaxEntry<D> p = node.getEntry(i);
                D dist_pq = getDistanceFunction().distance(p.getRoutingObjectID(), q.getRoutingObjectID());

                // p is nearer to q than the farthest kNN-candidate of q
                // ==> p becomes a knn-candidate
                if (dist_pq.compareTo(knnDist_q) <= 0) {
                    QueryResult<D> knn = new QueryResult<D>(p.getRoutingObjectID(), dist_pq);
                    knns_q.add(knn);
                    if (knns_q.size() >= k_max) {
                        knnDist_q = knns_q.getMaximumDistance();
                        q.setKnnDistance(knnDist_q);
                    }

                }
                // p is nearer to q than to its farthest knn-candidate
                // q becomes knn of p
                if (dist_pq.compareTo(p.getKnnDistance()) <= 0) {
                    KNNList<D> knns_p = new KNNList<D>(k_max, getDistanceFunction().infiniteDistance());
                    knns_p.add(new QueryResult<D>(q.getRoutingObjectID(), dist_pq));
                    doKNNQuery(p.getRoutingObjectID(), knns_p);

                    if (knns_p.size() < k_max) {
                        p.setKnnDistance(getDistanceFunction().undefinedDistance());
                    }
                    else {
                        D knnDist_p = knns_p.getMaximumDistance();
                        p.setKnnDistance(knnDist_p);
                    }
                }
                knnDist_node = Util.max(knnDist_node, p.getKnnDistance());
            }
        }
        // directory node
        else {
            List<DistanceEntry<D, MkMaxEntry<D>>> entries = getSortedEntries(node, q.getRoutingObjectID());
            for (DistanceEntry<D, MkMaxEntry<D>> distEntry : entries) {
                MkMaxEntry<D> dirEntry = distEntry.getEntry();
                D entry_knnDist = dirEntry.getKnnDistance();

                if (distEntry.getDistance().compareTo(entry_knnDist) < 0 || distEntry.getDistance().compareTo(knnDist_q) < 0) {
                    preInsert(q, dirEntry, knns_q);
                    knnDist_q = knns_q.getKNNDistance();
                }
                knnDist_node = Util.max(knnDist_node, dirEntry.getKnnDistance());
            }
        }
        if (debug) {
            debugFine(nodeEntry + "set knn dist " + knnDist_node);
        }
        nodeEntry.setKnnDistance(knnDist_node);
    }

    protected void initializeCapacities(O object, boolean verbose) {
        D dummyDistance = getDistanceFunction().nullDistance();
        int distanceSize = dummyDistance.externalizableSize();

        // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
        double overhead = 12.125;
        if (pageSize - overhead < 0) {
            throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
        }

        // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
        // coveringRadius + parentDistance + knnDistance) + 1
        dirCapacity = (int) (pageSize - overhead) / (4 + 4 + 3 * distanceSize) + 1;

        if (dirCapacity <= 1) {
            throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
        }

        if (dirCapacity < 10) {
            warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
        }

        // leafCapacity = (pageSize - overhead) / (objectID + parentDistance +
        // knnDistance) + 1
        leafCapacity = (int) (pageSize - overhead) / (4 + 2 * distanceSize) + 1;

        if (leafCapacity <= 1) {
            throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
        }

        if (leafCapacity < 10) {
            warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
        }
    }

    /**
     * @return a new MkMaxTreeNode which is a leaf node
     */
    protected MkMaxTreeNode<O, D> createNewLeafNode(int capacity) {
        return new MkMaxTreeNode<O, D>(file, capacity, true);
    }

    /**
     * @return a new MkMaxTreeNode which is a directory node
     */
    protected MkMaxTreeNode<O, D> createNewDirectoryNode(int capacity) {
        return new MkMaxTreeNode<O, D>(file, capacity, false);
    }

    /**
     * @return a new MkMaxLeafEntry representing the specified data object
     */
    protected MkMaxEntry<D> createNewLeafEntry(O object, D parentDistance) {
        KNNList<D> knns = new KNNList<D>(k_max - 1, getDistanceFunction().infiniteDistance());
        doKNNQuery(object.getID(), knns);
        D knnDistance = knns.getKNNDistance();
        return new MkMaxLeafEntry<D>(object.getID(), parentDistance, knnDistance);
    }

    /**
     * @return a new MkMaxDirectoryEntry representing the specified node
     */
    protected MkMaxEntry<D> createNewDirectoryEntry(MkMaxTreeNode<O, D> node, Integer routingObjectID, D parentDistance) {
        return new MkMaxDirectoryEntry<D>(
            routingObjectID,
            parentDistance,
            node.getID(),
            node.coveringRadius(routingObjectID, this),
            node.kNNDistance(getDistanceFunction()));
    }

    /**
     * @return a new MkMaxDirectoryEntry by calling
     *         <code>new MkMaxDirectoryEntry<D>(null, null, 0, null)</code>
     */
    protected MkMaxEntry<D> createRootEntry() {
        return new MkMaxDirectoryEntry<D>(null, null, 0, null, null);
    }
}
