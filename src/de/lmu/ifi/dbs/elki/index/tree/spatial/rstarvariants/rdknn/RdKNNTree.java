package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.GenericDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.spatial.BulkSplit.Strategy;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;

/**
 * RDkNNTree is a spatial index structure based on the concepts of the R*-Tree
 * supporting efficient processing of reverse k nearest neighbor queries. The
 * k-nn distance is stored in each entry of a node.
 * <p/>
 * TODO: noch nicht fertig!!!
 * 
 * @author Elke Achtert
 * 
 * @apiviz.has RdKNNNode oneway - - contains
 * @apiviz.has RdKNNTreeHeader
 * 
 * @param <O> Object type
 * @param <D> Distance type
 * @param <N> Number type
 * 
 *        FIXME: currently does not yet return RKNNQuery objects!
 */
public class RdKNNTree<O extends NumberVector<O, ?>, D extends NumberDistance<D, N>, N extends Number> extends NonFlatRStarTree<O, RdKNNNode<D, N>, RdKNNEntry<D, N>> implements RKNNIndex<O> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(RdKNNTree.class);

  /**
   * Parameter k.
   */
  private int k_max;

  /**
   * The distance function.
   */
  private SpatialPrimitiveDistanceFunction<O, D> distanceFunction;

  /**
   * The distance function.
   */
  private SpatialDistanceQuery<O, D> distanceQuery;

  /**
   * Internal knn query object, for updating the rKNN.
   */
  private KNNQuery<O, D> knnQuery;

  /**
   * Constructor.
   * 
   * @param relation Relation
   * @param fileName File name
   * @param pageSize Page size
   * @param cacheSize Cache size
   * @param bulk Bulk flag
   * @param bulkLoadStrategy bulk loading strategy
   * @param insertionCandidates Insertion candidates limit
   * @param k_max max k
   * @param distanceFunction distance function
   */
  public RdKNNTree(Relation<O> relation, String fileName, int pageSize, long cacheSize, boolean bulk, Strategy bulkLoadStrategy, int insertionCandidates, int k_max, SpatialPrimitiveDistanceFunction<O, D> distanceFunction) {
    super(relation, fileName, pageSize, cacheSize, bulk, bulkLoadStrategy, insertionCandidates);
    this.k_max = k_max;
    this.distanceFunction = distanceFunction;
    this.distanceQuery = distanceFunction.instantiate(relation);
    this.knnQuery = this.getKNNQuery(distanceQuery);
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   * 
   * @param entry the entry to be inserted
   */
  @Override
  protected void preInsert(RdKNNEntry<D, N> entry) {
    KNNHeap<D> knns_o = new KNNHeap<D>(k_max, distanceQuery.getDistanceFactory().infiniteDistance());
    preInsert(entry, getRootEntry(), knns_o);
  }

  /**
   * Performs necessary operations after deleting the specified object.
   */
  @Override
  protected void postDelete(DBID oid) {
    // reverse knn of o
    List<DistanceResultPair<D>> rnns = new ArrayList<DistanceResultPair<D>>();
    doReverseKNN(getRoot(), oid, rnns);

    // knn of rnn
    ModifiableDBIDs ids = DBIDUtil.newArray();
    for(DistanceResultPair<D> rnn : rnns) {
      ids.add(rnn.getDBID());
    }

    final Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>(ids.size());
    for(DBID id : ids) {
      knnLists.put(id, new KNNHeap<D>(k_max, distanceQuery.getDistanceFactory().infiniteDistance()));
    }
    knnQuery.getKNNForBulkHeaps(knnLists);

    // adjust knn distances
    adjustKNNDistance(getRootEntry(), knnLists);
  }

  /**
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructor and should be overwritten by subclasses if necessary.
   */
  @Override
  protected void bulkLoad(DBIDs ids) {
    super.bulkLoad(ids);

    // adjust all knn distances
    final Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>(ids.size());
    for(DBID id : ids) {
      knnLists.put(id, new KNNHeap<D>(k_max, distanceQuery.getDistanceFactory().infiniteDistance()));
    }
    knnQuery.getKNNForBulkHeaps(knnLists);
    adjustKNNDistance(getRootEntry(), knnLists);

    // test
    if(extraIntegrityChecks) {
      getRoot().integrityCheck();
    }
  }

  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(DistanceFunction<? super O, S> distanceFunction, Object... hints) {
    // FIXME: re-add
    return null;
  }

  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    // FIXME: re-add
    return null;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T extends Distance<T>> List<DistanceResultPair<T>> reverseKNNQuery(DBID oid, int k, SpatialPrimitiveDistanceFunction<? super O, T> distanceFunction) {
    checkDistanceFunction(distanceFunction);
    if(k > k_max) {
      throw new IllegalArgumentException("Parameter k is not supported, k > k_max: " + k + " > " + k_max);
    }

    // get candidates
    List candidates = new ArrayList<DistanceResultPair<D>>();
    doReverseKNN(getRoot(), oid, candidates);

    if(k == k_max) {
      Collections.sort(candidates);
      return candidates;
    }

    // refinement of candidates, if k < k_max
    Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>();
    ModifiableDBIDs candidateIDs = DBIDUtil.newArray();
    for(int i = 0; i < candidates.size(); i++) {
      DistanceResultPair<D> candidate = (DistanceResultPair<D>) candidates.get(i);
      KNNHeap<T> knns = new KNNHeap<T>(k, distanceFunction.getDistanceFactory().infiniteDistance());
      knnLists.put(candidate.getDBID(), (KNNHeap<D>) knns);
      candidateIDs.add(candidate.getDBID());
    }
    knnQuery.getKNNForBulkHeaps(knnLists);

    List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
    for(DBID id : candidateIDs) {
      for(DistanceResultPair<D> qr : knnLists.get(id)) {
        if(oid.equals(qr.getDBID())) {
          result.add(new GenericDistanceResultPair<T>((T) qr.getDistance(), id));
          break;
        }
      }
    }

    Collections.sort(result);
    return result;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T extends Distance<T>> List<List<DistanceResultPair<T>>> bulkReverseKNNQueryForID(DBIDs ids, int k, SpatialPrimitiveDistanceFunction<? super O, T> distanceFunction) {
    checkDistanceFunction(distanceFunction);
    if(k > k_max) {
      throw new IllegalArgumentException("Parameter k is not supported, k > k_max: " + k + " > " + k_max);
    }

    // get candidates
    Map<DBID, List<DistanceResultPair<D>>> candidateMap = new HashMap<DBID, List<DistanceResultPair<D>>>();
    for(DBID id : ids) {
      candidateMap.put(id, new ArrayList<DistanceResultPair<D>>());
    }
    doBulkReverseKNN(getRoot(), ids, candidateMap);

    if(k == k_max) {
      List<List<DistanceResultPair<T>>> resultList = new ArrayList<List<DistanceResultPair<T>>>();
      for(List candidates : candidateMap.values()) {
        Collections.sort(candidates);
        resultList.add(candidates);
      }
      return resultList;
    }

    // refinement of candidates, if k < k_max
    // perform a knn query for the candidates
    Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>();
    for(List<DistanceResultPair<D>> candidates : candidateMap.values()) {
      for(DistanceResultPair<D> candidate : candidates) {
        if(!knnLists.containsKey(candidate.getDBID())) {
          KNNHeap<T> knns = new KNNHeap<T>(k, distanceFunction.getDistanceFactory().infiniteDistance());
          knnLists.put(candidate.getDBID(), (KNNHeap<D>) knns);
        }
      }
    }
    knnQuery.getKNNForBulkHeaps(knnLists);

    // and add candidate c to the result if o is a knn of c
    List<List<DistanceResultPair<T>>> resultList = new ArrayList<List<DistanceResultPair<T>>>();
    for(DBID id : candidateMap.keySet()) {
      List<DistanceResultPair<D>> candidates = candidateMap.get(id);
      List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
      for(DistanceResultPair<D> candidate : candidates) {
        for(DistanceResultPair<D> qr : knnLists.get(candidate.getDBID())) {
          if(qr.getDBID() == id) {
            result.add(new GenericDistanceResultPair<T>((T) qr.getDistance(), id));
            break;
          }
        }
      }
      resultList.add(result);
    }
    return resultList;
  }

  @Override
  protected TreeIndexHeader createHeader() {
    return new RdKNNTreeHeader(pageSize, dirCapacity, leafCapacity, dirMinimum, leafCapacity, k_max);
  }

  @Override
  protected void initializeCapacities(RdKNNEntry<D, N> exampleLeaf) {
    int dimensionality = exampleLeaf.getDimensionality();
    D dummyDistance = distanceQuery.getDistanceFactory().nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), parentID(4), id(4), isLeaf(0.125)
    double overhead = 16.125;
    if(pageSize - overhead < 0) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (childID + childMBR + knnDistance)
    // + 1
    dirCapacity = (int) ((pageSize - overhead) / (4 + 16 * dimensionality + distanceSize)) + 1;

    if(dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if(dirCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a directory node = " + (dirCapacity - 1));
    }

    // minimum entries per directory node
    dirMinimum = (int) Math.round((dirCapacity - 1) * 0.5);
    if(dirMinimum < 2) {
      dirMinimum = 2;
    }

    // leafCapacity = (pageSize - overhead) / (childID + childValues +
    // knnDistance) + 1
    leafCapacity = (int) ((pageSize - overhead) / (4 + 8 * dimensionality + distanceSize)) + 1;

    if(leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if(leafCapacity < 10) {
      logger.warning("Page size is choosen too small! Maximum number of entries " + "in a leaf node = " + (leafCapacity - 1));
    }

    // minimum entries per leaf node
    leafMinimum = (int) Math.round((leafCapacity - 1) * 0.5);
    if(leafMinimum < 2) {
      leafMinimum = 2;
    }

    if(logger.isVerbose()) {
      logger.verbose("Directory Capacity: " + dirCapacity + "\nLeaf Capacity: " + leafCapacity);
    }
  }

  /**
   * Adapts the knn distances before insertion of entry q.
   * 
   * @param q the entry to be inserted
   * @param nodeEntry the entry representing the root of the current subtree
   * @param knns_q the knns of q
   */
  private void preInsert(RdKNNEntry<D, N> q, RdKNNEntry<D, N> nodeEntry, KNNHeap<D> knns_q) {
    D knnDist_q = knns_q.getKNNDistance();
    RdKNNNode<D, N> node = file.readPage(nodeEntry.getEntryID());
    D knnDist_node = distanceQuery.getDistanceFactory().nullDistance();

    // leaf node
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D, N> p = (RdKNNLeafEntry<D, N>) node.getEntry(i);
        D dist_pq = distanceQuery.distance(p.getDBID(), ((LeafEntry) q).getDBID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if(dist_pq.compareTo(knnDist_q) <= 0) {
          knns_q.add(dist_pq, p.getDBID());
          if(knns_q.size() >= k_max) {
            knnDist_q = knns_q.getMaximumDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if(dist_pq.compareTo(p.getKnnDistance()) <= 0) {
          O obj = relation.get(p.getDBID());
          List<DistanceResultPair<D>> knns_without_q = knnQuery.getKNNForObject(obj, k_max);
          
          if(knns_without_q.size() + 1 < k_max) {
            p.setKnnDistance(distanceQuery.getDistanceFactory().undefinedDistance());
          }
          else {
            D knnDist_p = DistanceUtil.min(knns_without_q.get(knns_without_q.size() - 1).getDistance(), dist_pq);
            p.setKnnDistance(knnDist_p);
          }
        }
        knnDist_node = DistanceUtil.max(knnDist_node, p.getKnnDistance());
      }
    }
    // directory node
    else {
      O obj = relation.get(((LeafEntry) q).getDBID());
      List<DistanceEntry<D, RdKNNEntry<D, N>>> entries = getSortedEntries(node, obj, distanceFunction);
      for(DistanceEntry<D, RdKNNEntry<D, N>> distEntry : entries) {
        RdKNNEntry<D, N> entry = distEntry.getEntry();
        D entry_knnDist = entry.getKnnDistance();

        if(distEntry.getDistance().compareTo(entry_knnDist) < 0 || distEntry.getDistance().compareTo(knnDist_q) < 0) {
          preInsert(q, entry, knns_q);
          knnDist_q = knns_q.getKNNDistance();
        }
        knnDist_node = DistanceUtil.max(knnDist_node, entry.getKnnDistance());
      }
    }
    nodeEntry.setKnnDistance(knnDist_node);
  }

  /**
   * Performs a reverse knn query in the specified subtree.
   * 
   * @param node the root node of the current subtree
   * @param oid the id of the object for which the rknn query is performed
   * @param result the list containing the query results
   */
  private void doReverseKNN(RdKNNNode<D, N> node, DBID oid, List<DistanceResultPair<D>> result) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D, N> entry = (RdKNNLeafEntry<D, N>) node.getEntry(i);
        D distance = distanceQuery.distance(entry.getDBID(), oid);
        if(distance.compareTo(entry.getKnnDistance()) <= 0) {
          result.add(new GenericDistanceResultPair<D>(distance, entry.getDBID()));
        }
      }
    }
    // node is a inner node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D, N> entry = (RdKNNDirectoryEntry<D, N>) node.getEntry(i);
        D minDist = distanceQuery.minDist(entry, oid);
        if(minDist.compareTo(entry.getKnnDistance()) <= 0) {
          doReverseKNN(file.readPage(entry.getEntryID()), oid, result);
        }
      }
    }
  }

  /**
   * Performs a bulk reverse knn query in the specified subtree.
   * 
   * @param node the root node of the current subtree
   * @param ids the object ids for which the rknn query is performed
   * @param result the map containing the query results for each object
   */
  private void doBulkReverseKNN(RdKNNNode<D, N> node, DBIDs ids, Map<DBID, List<DistanceResultPair<D>>> result) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D, N> entry = (RdKNNLeafEntry<D, N>) node.getEntry(i);
        for(DBID id : ids) {
          D distance = distanceQuery.distance(entry.getDBID(), id);
          if(distance.compareTo(entry.getKnnDistance()) <= 0) {
            result.get(id).add(new GenericDistanceResultPair<D>(distance, entry.getDBID()));
          }
        }
      }
    }
    // node is a inner node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D, N> entry = (RdKNNDirectoryEntry<D, N>) node.getEntry(i);
        ModifiableDBIDs candidates = DBIDUtil.newArray();
        for(DBID id : ids) {
          D minDist = distanceQuery.minDist(entry, id);
          if(minDist.compareTo(entry.getKnnDistance()) <= 0) {
            candidates.add(id);
          }
          if(!candidates.isEmpty()) {
            doBulkReverseKNN(file.readPage(entry.getEntryID()), candidates, result);
          }
        }
      }
    }
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   * 
   * @param entry the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  private void adjustKNNDistance(RdKNNEntry<D, N> entry, Map<DBID, KNNHeap<D>> knnLists) {
    RdKNNNode<D, N> node = file.readPage(entry.getEntryID());
    D knnDist_node = distanceQuery.getDistanceFactory().nullDistance();
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry<D, N> leafEntry = node.getEntry(i);
        KNNHeap<D> knns = knnLists.get(((LeafEntry) leafEntry).getDBID());
        if(knns != null) {
          leafEntry.setKnnDistance(knnLists.get(((LeafEntry) leafEntry).getDBID()).getKNNDistance());
        }
        knnDist_node = DistanceUtil.max(knnDist_node, leafEntry.getKnnDistance());
      }
    }
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry<D, N> dirEntry = node.getEntry(i);
        adjustKNNDistance(dirEntry, knnLists);
        knnDist_node = DistanceUtil.max(knnDist_node, dirEntry.getKnnDistance());
      }
    }
    entry.setKnnDistance(knnDist_node);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   * 
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  @Override
  protected RdKNNNode<D, N> createNewLeafNode(int capacity) {
    return new RdKNNNode<D, N>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   * 
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  @Override
  protected RdKNNNode<D, N> createNewDirectoryNode(int capacity) {
    return new RdKNNNode<D, N>(file, capacity, false);
  }

  /**
   * Creates a new leaf entry representing the specified data object.
   */
  @Override
  protected RdKNNEntry<D, N> createNewLeafEntry(DBID id) {
    return new RdKNNLeafEntry<D, N>(id, relation.get(id), distanceQuery.getDistanceFactory().undefinedDistance());
  }

  /**
   * Creates a new directory entry representing the specified node.
   * 
   * @param node the node to be represented by the new entry
   */
  @Override
  protected RdKNNEntry<D, N> createNewDirectoryEntry(RdKNNNode<D, N> node) {
    return new RdKNNDirectoryEntry<D, N>(node.getPageID(), node.computeMBR(), node.kNNDistance());
  }

  /**
   * Creates an entry representing the root node.
   * 
   * @return an entry representing the root node
   */
  @Override
  protected RdKNNEntry<D, N> createRootEntry() {
    return new RdKNNDirectoryEntry<D, N>(0, null, null);
  }

  /**
   * Return the node base class.
   * 
   * @return node base class
   */
  @Override
  protected Class<RdKNNNode<D, N>> getNodeClass() {
    return ClassGenericsUtil.uglyCastIntoSubclass(RdKNNNode.class);
  }

  /**
   * Throws an IllegalArgumentException if the specified distance function is
   * not an instance of the distance function used by this index.
   * 
   * @throws IllegalArgumentException
   * @param <T> distance type
   * @param distanceFunction the distance function to be checked
   */
  private <T extends Distance<T>> void checkDistanceFunction(SpatialPrimitiveDistanceFunction<? super O, T> distanceFunction) {
    // todo: the same class does not necessarily indicate the same
    // distancefunction!!! (e.g.dim selecting df!)
    if(!distanceFunction.getClass().equals(this.distanceFunction.getClass())) {
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " + this.distanceQuery.getClass() + ", but is " + distanceFunction.getClass());
    }
  }

  @Override
  protected Logging getLogger() {
    return logger;
  }
}