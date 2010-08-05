package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.SpatialDistanceQuery;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.SpatialPrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.LeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.constraints.GreaterConstraint;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * RDkNNTree is a spatial index structure based on the concepts of the R*-Tree
 * supporting efficient processing of reverse k nearest neighbor queries. The
 * k-nn distance is stored in each entry of a node.
 * <p/>
 * TODO: noch nicht fertig!!!
 * 
 * @author Elke Achtert
 * @param <O> Object type
 * @param <D> Distance type
 * @param <N> Number type
 * 
 */
public class RdKNNTree<O extends NumberVector<O, ?>, D extends NumberDistance<D, N>, N extends Number> extends NonFlatRStarTree<O, RdKNNNode<D, N>, RdKNNEntry<D, N>> {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(RdKNNTree.class);
  
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("rdknn.k", "positive integer specifying the maximal number k of reverse " + "k nearest neighbors to be supported.");

  /**
   * Parameter for k
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, new GreaterConstraint(0));

  /**
   * The default distance function.
   */
  public static final Class<?> DEFAULT_DISTANCE_FUNCTION = EuclideanDistanceFunction.class;

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("rdknn.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Parameter for distance function
   */
  private final ObjectParameter<SpatialPrimitiveDistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<SpatialPrimitiveDistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, SpatialPrimitiveDistanceFunction.class, DEFAULT_DISTANCE_FUNCTION);

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
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public RdKNNTree(Parameterization config) {
    super(config);
    config = config.descend(this);
    logger.getWrappedLogger().setLevel(Level.OFF);

    // k_max
    if(config.grab(K_PARAM)) {
      k_max = K_PARAM.getValue();
    }
    // distance function
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
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
   * 
   * @param o the object to be deleted
   */
  @Override
  protected void postDelete(O o) {
    // reverse knn of o
    List<DistanceResultPair<D>> rnns = new ArrayList<DistanceResultPair<D>>();
    doReverseKNN(getRoot(), o.getID(), rnns);

    // knn of rnn
    ModifiableDBIDs ids = DBIDUtil.newArray();
    for(DistanceResultPair<D> rnn : rnns) {
      ids.add(rnn.getID());
    }

    final Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>(ids.size());
    for(DBID id : ids) {
      knnLists.put(id, new KNNHeap<D>(k_max, distanceQuery.getDistanceFactory().infiniteDistance()));
    }
    batchNN(getRoot(), distanceQuery, knnLists);

    // adjust knn distances
    adjustKNNDistance(getRootEntry(), knnLists);
  }

  /**
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructor and should be overwritten by subclasses if necessary.
   * 
   * @param objects the data objects to be indexed
   */
  @Override
  protected void bulkLoad(List<O> objects) {
    super.bulkLoad(objects);

    // adjust all knn distances
    final Map<DBID, KNNHeap<D>> knnLists = new HashMap<DBID, KNNHeap<D>>(objects.size());
    for(O object : objects) {
      knnLists.put(object.getID(), new KNNHeap<D>(k_max, distanceQuery.getDistanceFactory().infiniteDistance()));
    }
    batchNN(getRoot(), distanceQuery, knnLists);
    adjustKNNDistance(getRootEntry(), knnLists);

    // test
    if(extraIntegrityChecks) {
      getRoot().integrityCheck();
    }
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T extends Distance<T>> List<DistanceResultPair<T>> reverseKNNQuery(O object, int k, SpatialDistanceQuery<O, T> distanceFunction) {
    checkDistanceFunction(distanceFunction);
    if(k > k_max) {
      throw new IllegalArgumentException("Parameter k is not supported, k > k_max: " + k + " > " + k_max);
    }

    // get candidates
    List candidates = new ArrayList<DistanceResultPair<D>>();
    doReverseKNN(getRoot(), object.getID(), candidates);

    if(k == k_max) {
      Collections.sort(candidates);
      return candidates;
    }

    // refinement of candidates, if k < k_max
    Map<DBID, KNNHeap<T>> knnLists = new HashMap<DBID, KNNHeap<T>>();
    ModifiableDBIDs candidateIDs = DBIDUtil.newArray();
    for(int i = 0; i < candidates.size(); i++) {
      DistanceResultPair<D> candidate = (DistanceResultPair<D>) candidates.get(i);
      KNNHeap<T> knns = new KNNHeap<T>(k, distanceFunction.getDistanceFactory().infiniteDistance());
      knnLists.put(candidate.getID(), knns);
      candidateIDs.add(candidate.getID());
    }
    batchNN(getRoot(), distanceFunction, knnLists);

    List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
    for(DBID id : candidateIDs) {
      for(DistanceResultPair<T> qr : knnLists.get(id)) {
        if(qr.getID() == object.getID()) {
          result.add(new DistanceResultPair<T>(qr.getDistance(), id));
          break;
        }
      }
    }

    Collections.sort(result);
    return result;
  }

  @Override
  @SuppressWarnings({ "unchecked", "rawtypes" })
  public <T extends Distance<T>> List<List<DistanceResultPair<T>>> bulkReverseKNNQueryForID(DBIDs ids, int k, SpatialDistanceQuery<O, T> distanceFunction) {
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
      System.out.println("ddd");
      return resultList;
    }

    // refinement of candidates, if k < k_max
    // perform a knn query for the candidates
    Map<DBID, KNNHeap<T>> knnLists = new HashMap<DBID, KNNHeap<T>>();
    for(List<DistanceResultPair<D>> candidates : candidateMap.values()) {
      for(DistanceResultPair<D> candidate : candidates) {
        if(!knnLists.containsKey(candidate.getID())) {
          KNNHeap<T> knns = new KNNHeap<T>(k, distanceFunction.getDistanceFactory().infiniteDistance());
          knnLists.put(candidate.getID(), knns);
        }
      }
    }
    batchNN(getRoot(), distanceFunction, knnLists);

    // and add candidate c to the result if o is a knn of c
    List<List<DistanceResultPair<T>>> resultList = new ArrayList<List<DistanceResultPair<T>>>();
    for(DBID id : candidateMap.keySet()) {
      List<DistanceResultPair<D>> candidates = candidateMap.get(id);
      List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
      for(DistanceResultPair<D> candidate : candidates) {
        for(DistanceResultPair<T> qr : knnLists.get(candidate.getID())) {
          if(qr.getID() == id) {
            result.add(new DistanceResultPair<T>(qr.getDistance(), id));
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
  protected void initializeCapacities(O object) {
    int dimensionality = object.getDimensionality();
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
   * Sets the database in the distance function of this index.
   * 
   * @param database the database
   */
  @Override
  public void setDatabase(Database<O> database) {
    super.setDatabase(database);
    distanceQuery = (SpatialDistanceQuery<O, D>) database.getDistanceQuery(distanceFunction);
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
          DistanceResultPair<D> knn = new DistanceResultPair<D>(dist_pq, p.getDBID());
          knns_q.add(knn);
          if(knns_q.size() >= k_max) {
            knnDist_q = knns_q.getMaximumDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if(dist_pq.compareTo(p.getKnnDistance()) <= 0) {
          KNNHeap<D> knns_p = new KNNHeap<D>(k_max, distanceQuery.getDistanceFactory().infiniteDistance());
          knns_p.add(new DistanceResultPair<D>(dist_pq, ((LeafEntry) q).getDBID()));
          doKNNQuery(p.getDBID(), distanceQuery, knns_p);

          if(knns_p.size() < k_max) {
            p.setKnnDistance(distanceQuery.getDistanceFactory().undefinedDistance());
          }
          else {
            D knnDist_p = knns_p.getMaximumDistance();
            p.setKnnDistance(knnDist_p);
          }
        }
        knnDist_node = DistanceUtil.max(knnDist_node, p.getKnnDistance());
      }
    }
    // directory node
    else {
      List<DistanceEntry<D, RdKNNEntry<D, N>>> entries = getSortedEntries(node, ((LeafEntry) q).getDBID(), distanceQuery);
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
          result.add(new DistanceResultPair<D>(distance, entry.getDBID()));
        }
      }
    }
    // node is a inner node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D, N> entry = (RdKNNDirectoryEntry<D, N>) node.getEntry(i);
        D minDist = distanceQuery.minDist(entry.getMBR(), oid);
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
            result.get(id).add(new DistanceResultPair<D>(distance, entry.getDBID()));
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
          D minDist = distanceQuery.minDist(entry.getMBR(), id);
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
   * 
   * @param object the data object to be represented by the new entry
   */
  @Override
  protected RdKNNEntry<D, N> createNewLeafEntry(O object) {
    return new RdKNNLeafEntry<D, N>(object.getID(), getValues(object), distanceQuery.getDistanceFactory().undefinedDistance());
  }

  /**
   * Creates a new directory entry representing the specified node.
   * 
   * @param node the node to be represented by the new entry
   */
  @Override
  protected RdKNNEntry<D, N> createNewDirectoryEntry(RdKNNNode<D, N> node) {
    return new RdKNNDirectoryEntry<D, N>(node.getPageID(), node.mbr(), node.kNNDistance());
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
   * @param distanceQuery the distance function to be checked
   */
  private <T extends Distance<T>> void checkDistanceFunction(SpatialDistanceQuery<O, T> distanceQuery) {
    DistanceFunction<? super O, T> distanceFunction = distanceQuery.getDistanceFunction();
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