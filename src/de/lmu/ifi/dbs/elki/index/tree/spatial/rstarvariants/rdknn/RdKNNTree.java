package de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rdknn;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.data.KNNList;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.elki.index.tree.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
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
 */
public class RdKNNTree<O extends NumberVector<O, ?>, D extends NumberDistance<D, N>, N extends Number> extends NonFlatRStarTree<O, RdKNNNode<D, N>, RdKNNEntry<D, N>> {
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
  private final ObjectParameter<SpatialDistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<SpatialDistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, SpatialDistanceFunction.class, DEFAULT_DISTANCE_FUNCTION);

  /**
   * Parameter k.
   */
  private int k_max;

  /**
   * The distance function.
   */
  private SpatialDistanceFunction<O, D> distanceFunction;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public RdKNNTree(Parameterization config) {
    super(config);
    logger.getWrappedLogger().setLevel(Level.ALL);

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
    KNNList<D> knns_o = new KNNList<D>(k_max, distanceFunction.infiniteDistance());
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
    doReverseKNN(getRoot(), o, rnns);

    // knn of rnn
    List<Integer> ids = new ArrayList<Integer>();
    for(DistanceResultPair<D> rnn : rnns) {
      ids.add(rnn.getID());
    }

    final Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>(ids.size());
    for(Integer id : ids) {
      knnLists.put(id, new KNNList<D>(k_max, distanceFunction.infiniteDistance()));
    }
    batchNN(getRoot(), distanceFunction, knnLists);

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
    final Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>(objects.size());
    for(O object : objects) {
      knnLists.put(object.getID(), new KNNList<D>(k_max, distanceFunction.infiniteDistance()));
    }
    batchNN(getRoot(), distanceFunction, knnLists);
    adjustKNNDistance(getRootEntry(), knnLists);

    // test
    if(extraIntegrityChecks) {
      getRoot().integrityCheck();
    }
  }

  @Override
  @SuppressWarnings("unchecked")
  public <T extends Distance<T>> List<DistanceResultPair<T>> reverseKNNQuery(O object, int k, SpatialDistanceFunction<O, T> distanceFunction) {
    checkDistanceFunction(distanceFunction);
    if(k > k_max) {
      throw new IllegalArgumentException("Parameter k is not supported, k > k_max: " + k + " > " + k_max);
    }

    // get candidates
    List<DistanceResultPair<D>> candidates = new ArrayList<DistanceResultPair<D>>();
    doReverseKNN(getRoot(), object, candidates);

    if(k == k_max) {
      Collections.sort(candidates);
      List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
      for(DistanceResultPair<D> qr : candidates) {
        result.add((DistanceResultPair<T>) qr);
      }
      return result;
    }

    // refinement of candidates
    Map<Integer, KNNList<T>> knnLists = new HashMap<Integer, KNNList<T>>();
    List<Integer> candidateIDs = new ArrayList<Integer>();
    for(DistanceResultPair<D> candidate : candidates) {
      KNNList<T> knns = new KNNList<T>(k, distanceFunction.infiniteDistance());
      knnLists.put(candidate.getID(), knns);
      candidateIDs.add(candidate.getID());
    }
    batchNN(getRoot(), distanceFunction, knnLists);

    List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
    for(Integer id : candidateIDs) {
      List<DistanceResultPair<T>> knns = knnLists.get(id).toList();
      for(DistanceResultPair<T> qr : knns) {
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
  public <T extends Distance<T>> List<List<DistanceResultPair<T>>> bulkReverseKNNQueryForID(List<Integer> ids, int k, SpatialDistanceFunction<O, T> distanceFunction) {
    checkDistanceFunction(distanceFunction);
    if(k > k_max) {
      throw new IllegalArgumentException("Parameter k is not supported, k > k_max: " + k + " > " + k_max);
    }

    // get candidates
    Map<Integer, List<DistanceResultPair<D>>> candidateMap = new HashMap<Integer, List<DistanceResultPair<D>>>();
    for(Integer id : ids) {
      candidateMap.put(id, new ArrayList<DistanceResultPair<D>>());
    }
    doBulkReverseKNN(getRoot(), ids, candidateMap);

    if(k == k_max) {
      for(Integer id: ids) {
        List<DistanceResultPair<D>> candidates = candidateMap.get(id);
        Collections.sort(candidates);
        List<DistanceResultPair<T>> result = new ArrayList<DistanceResultPair<T>>();
        for(DistanceResultPair<D> qr : candidates) {
          result.add((DistanceResultPair<T>) qr);
        }
      }
      //return result;
    }
    
    // TODO!!! noch nicht fertig!!!

    return super.bulkReverseKNNQueryForID(ids, k, distanceFunction);
  }

  @Override
  protected TreeIndexHeader createHeader() {
    return new RdKNNTreeHeader(pageSize, dirCapacity, leafCapacity, dirMinimum, leafCapacity, k_max);
  }

  @Override
  protected void initializeCapacities(O object, boolean verbose) {
    int dimensionality = object.getDimensionality();
    D dummyDistance = distanceFunction.nullDistance();
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

    if(verbose) {
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
    distanceFunction.setDatabase(database);
  }

  /**
   * Adapts the knn distances before insertion of entry q.
   * 
   * @param q the entry to be inserted
   * @param nodeEntry the entry representing the root of the current subtree
   * @param knns_q the knns of q
   */
  private void preInsert(RdKNNEntry<D, N> q, RdKNNEntry<D, N> nodeEntry, KNNList<D> knns_q) {
    D knnDist_q = knns_q.getKNNDistance();
    RdKNNNode<D, N> node = file.readPage(nodeEntry.getID());
    D knnDist_node = distanceFunction.nullDistance();

    // leaf node
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D, N> p = (RdKNNLeafEntry<D, N>) node.getEntry(i);
        D dist_pq = distanceFunction.distance(p.getID(), q.getID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if(dist_pq.compareTo(knnDist_q) <= 0) {
          DistanceResultPair<D> knn = new DistanceResultPair<D>(dist_pq, p.getID());
          knns_q.add(knn);
          if(knns_q.size() >= k_max) {
            knnDist_q = knns_q.getMaximumDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if(dist_pq.compareTo(p.getKnnDistance()) <= 0) {
          KNNList<D> knns_p = new KNNList<D>(k_max, distanceFunction.infiniteDistance());
          knns_p.add(new DistanceResultPair<D>(dist_pq, q.getID()));
          doKNNQuery(p.getID(), distanceFunction, knns_p);

          if(knns_p.size() < k_max) {
            p.setKnnDistance(distanceFunction.undefinedDistance());
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
      List<DistanceEntry<D, RdKNNEntry<D, N>>> entries = getSortedEntries(node, q.getID(), distanceFunction);
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
   * @param o the id of the object for which the rknn query is performed
   * @param result the list containing the query results
   */
  private void doReverseKNN(RdKNNNode<D, N> node, O o, List<DistanceResultPair<D>> result) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D, N> entry = (RdKNNLeafEntry<D, N>) node.getEntry(i);
        D distance = distanceFunction.distance(entry.getID(), o);
        if(distance.compareTo(entry.getKnnDistance()) <= 0) {
          result.add(new DistanceResultPair<D>(distance, entry.getID()));
        }
      }
    }
    // node is a inner node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D, N> entry = (RdKNNDirectoryEntry<D, N>) node.getEntry(i);
        D minDist = distanceFunction.minDist(entry.getMBR(), o);
        if(minDist.compareTo(entry.getKnnDistance()) <= 0) {
          doReverseKNN(file.readPage(entry.getID()), o, result);
        }
      }
    }
  }

  /**
   * Performs a bulk reverse knn query in the specified subtree.
   * 
   * @param node the root node of the current subtree
   * @param objects the objects for which the rknn query is performed
   * @param result the map containing the query results for each object
   */
  private void doBulkReverseKNN(RdKNNNode<D, N> node, List<Integer> ids, Map<Integer, List<DistanceResultPair<D>>> result) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D, N> entry = (RdKNNLeafEntry<D, N>) node.getEntry(i);
        for(Integer id : ids) {
          D distance = distanceFunction.distance(entry.getID(), id);
          if(distance.compareTo(entry.getKnnDistance()) <= 0) {
            result.get(id).add(new DistanceResultPair<D>(distance, entry.getID()));
          }
        }
      }
    }
    // node is a inner node
    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D, N> entry = (RdKNNDirectoryEntry<D, N>) node.getEntry(i);
        List<Integer> candidates = new ArrayList<Integer>();
        for(Integer id : ids) {
          D minDist = distanceFunction.minDist(entry.getMBR(), id);
          if(minDist.compareTo(entry.getKnnDistance()) <= 0) {
            candidates.add(id);
          }
          if(!candidates.isEmpty()) {
            doBulkReverseKNN(file.readPage(entry.getID()), candidates, result);
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
  private void adjustKNNDistance(RdKNNEntry<D, N> entry, Map<Integer, KNNList<D>> knnLists) {
    RdKNNNode<D, N> node = file.readPage(entry.getID());
    D knnDist_node = distanceFunction.nullDistance();
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry<D, N> leafEntry = node.getEntry(i);
        KNNList<D> knns = knnLists.get(leafEntry.getID());
        if(knns != null) {
          leafEntry.setKnnDistance(knnLists.get(leafEntry.getID()).getKNNDistance());
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
    return new RdKNNLeafEntry<D, N>(object.getID(), getValues(object), distanceFunction.undefinedDistance());
  }

  /**
   * Creates a new directory entry representing the specified node.
   * 
   * @param node the node to be represented by the new entry
   */
  @Override
  protected RdKNNEntry<D, N> createNewDirectoryEntry(RdKNNNode<D, N> node) {
    return new RdKNNDirectoryEntry<D, N>(node.getID(), node.mbr(), node.kNNDistance());
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
  private <T extends Distance<T>> void checkDistanceFunction(DistanceFunction<O, T> distanceFunction) {
    // todo: the same class does not necessarily indicate the same
    // distancefunction!!! (e.g.dim selecting df!)
    if(!distanceFunction.getClass().equals(this.distanceFunction.getClass())) {
      throw new IllegalArgumentException("Parameter distanceFunction must be an instance of " + this.distanceFunction.getClass() + ", but is " + distanceFunction.getClass());
    }
  }
}