package de.lmu.ifi.dbs.index.tree.spatial.rstarvariants.rdknn;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.database.Database;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.distance.distancefunction.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.index.tree.TreeIndexHeader;
import de.lmu.ifi.dbs.index.tree.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.tree.spatial.rstarvariants.NonFlatRStarTree;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.ClassParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * RDkNNTree is a spatial index structure based on the concepts of the R*-Tree
 * supporting efficient processing of reverse k nearest neighbor queries. The
 * k-nn distance is stored in each entry of a node.
 * <p/>
 * todo: noch nicht fertig!!!
 *
 * @author Elke Achtert 
 */
public class RdKNNTree<O extends NumberVector<O,? >, D extends NumberDistance<D>> extends NonFlatRStarTree<O, RdKNNNode<D>, RdKNNEntry<D>> {

  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "positive integer specifying the maximal number k of reverse " +
                                   "k nearest neighbors to be supported.";

  /**
   * The default distance function.
   */
  public static final String DEFAULT_DISTANCE_FUNCTION = EuklideanDistanceFunction.class.getName();

  /**
   * Parameter for distance function.
   */
  public static final String DISTANCE_FUNCTION_P = "distancefunction";

  /**
   * Description for parameter distance function.
   */
  public static final String DISTANCE_FUNCTION_D = "the distance function to determine the distance between database objects " +
                                                   Properties.KDD_FRAMEWORK_PROPERTIES.restrictionString(DistanceFunction.class) +
                                                   ". Default: " + DEFAULT_DISTANCE_FUNCTION;

  /**
   * Parameter k.
   */
  private int k_max;

  /**
   * The distance function.
   */
  private SpatialDistanceFunction<O, D> distanceFunction;

  /**
   * Creates a new DeLiClu-Tree.
   */
  public RdKNNTree() {
    super();
    this.debug = true;

    optionHandler.put(new IntParameter(K_P, K_D, new GreaterConstraint(0)));

    ClassParameter<DistanceFunction<O,D>> distFunction = new ClassParameter(DISTANCE_FUNCTION_P,
                                                                                         DISTANCE_FUNCTION_D,
                                                                                         DistanceFunction.class);
    distFunction.setDefaultValue(DEFAULT_DISTANCE_FUNCTION);
    optionHandler.put(distFunction);
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   *
   * @param entry the entry to be inserted
   */
  protected void preInsert(RdKNNEntry<D> entry) {
    KNNList<D> knns_o = new KNNList<D>(k_max, distanceFunction.infiniteDistance());
    preInsert(entry, getRootEntry(), knns_o);
  }

  /**
   * Performs necessary operations after deleting the specified object.
   *
   * @param o the object to be deleted
   */
  protected void postDelete(O o) {
    // reverse knn of o
    List<QueryResult<D>> rnns = new ArrayList<QueryResult<D>>();
    doReverseKNN(getRoot(), o, rnns);

    // knn of rnn
    List<Integer> ids = new ArrayList<Integer>();
    for (QueryResult<D> rnn : rnns) {
      ids.add(rnn.getID());
    }

    final Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>(ids.size());
    for (Integer id : ids) {
      knnLists.put(id, new KNNList<D>(k_max, distanceFunction.infiniteDistance()));
    }
    batchNN(getRoot(), distanceFunction, knnLists);

    // adjust knn distances
    adjustKNNDistance(getRootEntry(), knnLists);
  }

  /**
   * Performs a bulk load on this RTree with the specified data. Is called by
   * the constructur and should be overwritten by subclasses if necessary.
   *
   * @param objects the data objects to be indexed
   */
  protected void bulkLoad(List<O> objects) {
    super.bulkLoad(objects);

    // adjust all knn distances
    final Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>(objects.size());
    for (O object : objects) {
      knnLists.put(object.getID(), new KNNList<D>(k_max, distanceFunction.infiniteDistance()));
    }
    batchNN(getRoot(), distanceFunction, knnLists);
    adjustKNNDistance(getRootEntry(), knnLists);

    // test
    if (debug) {
      getRoot().test();
    }
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param object the query object
   * @param k      the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public <T extends Distance<T>> List<QueryResult<T>> reverseKNNQuery(O object, int k, DistanceFunction<O, T> distanceFunction) {
    if (!distanceFunction.getClass().equals(this.distanceFunction.getClass())) {
      throw new IllegalArgumentException("Wrong distancefuction!");
    }

    if (k > k_max) {
      throw new IllegalArgumentException("Parameter k is not supported!");
    }

    // get candidates
    List<QueryResult<D>> candidates = new ArrayList<QueryResult<D>>();
    doReverseKNN(getRoot(), object, candidates);

    if (k == k_max) {
      Collections.sort(candidates);
      List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
      for (QueryResult<D> qr : candidates) {
        //noinspection unchecked
        result.add((QueryResult<T>) qr);
      }
      return result;
    }

    // refinement of candidates
    Map<Integer, KNNList<T>> knnLists = new HashMap<Integer, KNNList<T>>();
    List<Integer> candidateIDs = new ArrayList<Integer>();
    for (QueryResult<D> candidate : candidates) {
      KNNList<T> knns = new KNNList<T>(k, distanceFunction.infiniteDistance());
      knnLists.put(candidate.getID(), knns);
      candidateIDs.add(candidate.getID());
    }
    batchNN(getRoot(), (SpatialDistanceFunction<O, T>) distanceFunction, knnLists);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (Integer id : candidateIDs) {
      List<QueryResult<T>> knns = knnLists.get(id).toList();
      for (QueryResult<T> qr : knns) {
        if (qr.getID() == object.getID()) {
          result.add(new QueryResult<T>(id, qr.getDistance()));
          break;
        }
      }
    }

    Collections.sort(result);
    return result;

  }

  /**
   * @see de.lmu.ifi.dbs.index.tree.TreeIndex#createHeader()
   */
  protected TreeIndexHeader createHeader() {
    return new RdKNNTreeHeader(pageSize, dirCapacity, leafCapacity, dirMinimum, leafCapacity, k_max);
  }

  /**
   * @see de.lmu.ifi.dbs.index.tree.TreeIndex#initializeCapacities(de.lmu.ifi.dbs.data.DatabaseObject,boolean)
   */
  protected void initializeCapacities(O object, boolean verbose) {
    int dimensionality = object.getDimensionality();
    D dummyDistance = distanceFunction.nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), parentID(4), id(4), isLeaf(0.125)
    double overhead = 16.125;
    if (pageSize - overhead < 0) {
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (childID + childMBR + knnDistance) + 1
    dirCapacity = (int) ((pageSize - overhead) / (4 + 16 * dimensionality + distanceSize)) + 1;

    if (dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");
    }

    if (dirCapacity < 10) {
      warning("Page size is choosen too small! Maximum number of entries "
              + "in a directory node = " + (dirCapacity - 1));
    }

    // minimum entries per directory node
    dirMinimum = (int) Math.round((dirCapacity - 1) * 0.5);
    if (dirMinimum < 2) {
      dirMinimum = 2;
    }

    // leafCapacity = (pageSize - overhead) / (childID + childValues + knnDistance) + 1
    leafCapacity = (int) ((pageSize - overhead) / (4 + 8 * dimensionality + distanceSize)) + 1;

    if (leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");
    }

    if (leafCapacity < 10) {
      warning("Page size is choosen too small! Maximum number of entries "
              + "in a leaf node = " + (leafCapacity - 1));
    }

    // minimum entries per leaf node
    leafMinimum = (int) Math.round((leafCapacity - 1) * 0.5);
    if (leafMinimum < 2) {
      leafMinimum = 2;
    }

    if (verbose) {
      verbose("Directory Capacity: " + dirCapacity +
              "\nLeaf Capacity: " + leafCapacity);
    }
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // distance function
    String className = (String) optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
    try {
      // noinspection unchecked
        // todo
      distanceFunction = Util.instantiate(SpatialDistanceFunction.class, className);
    }
    catch (UnableToComplyException e) {
      throw new WrongParameterValueException(DISTANCE_FUNCTION_P,
                                             className, DISTANCE_FUNCTION_D, e);
    }

    remainingParameters = distanceFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

    // k_max
    k_max = (Integer) optionHandler.getOptionValue(K_P);

    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    attributeSettings.addAll(distanceFunction.getAttributeSettings());
    return attributeSettings;
  }

  /**
   * Sets the databse in the distance function of this index.
   *
   * @param database the database
   */
  public void setDatabase(Database<O> database) {
    super.setDatabase(database);
    distanceFunction.setDatabase(database, false, false);
  }

  /**
   * Adapts the knn distances before insertion of entry q.
   *
   * @param q         the entry to be inserted
   * @param nodeEntry the entry representing the root of thge current subtree
   * @param knns_q    the knns of q
   */
  private void preInsert(RdKNNEntry<D> q, RdKNNEntry<D> nodeEntry, KNNList<D> knns_q) {
    D knnDist_q = knns_q.getKNNDistance();
    RdKNNNode<D> node = file.readPage(nodeEntry.getID());
    D knnDist_node = distanceFunction.nullDistance();

    // leaf node
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D> p = (RdKNNLeafEntry<D>) node.getEntry(i);
        D dist_pq = distanceFunction.distance(p.getID(), q.getID());

        // p is nearer to q than the farthest kNN-candidate of q
        // ==> p becomes a knn-candidate
        if (dist_pq.compareTo(knnDist_q) <= 0) {
          QueryResult<D> knn = new QueryResult<D>(p.getID(), dist_pq);
          knns_q.add(knn);
          if (knns_q.size() >= k_max) {
            knnDist_q = knns_q.getMaximumDistance();
            q.setKnnDistance(knnDist_q);
          }

        }
        // p is nearer to q than to its farthest knn-candidate
        // q becomes knn of p
        if (dist_pq.compareTo(p.getKnnDistance()) <= 0) {
          KNNList<D> knns_p = new KNNList<D>(k_max, distanceFunction.infiniteDistance());
          knns_p.add(new QueryResult<D>(q.getID(), dist_pq));
          doKNNQuery(p.getID(), distanceFunction, knns_p);

          if (knns_p.size() < k_max) {
            p.setKnnDistance(distanceFunction.undefinedDistance());
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
      List<DistanceEntry<D, RdKNNEntry<D>>> entries = getSortedEntries(node, q.getID(), distanceFunction);
      for (DistanceEntry<D, RdKNNEntry<D>> distEntry : entries) {
        RdKNNEntry<D> entry = distEntry.getEntry();
        D entry_knnDist = entry.getKnnDistance();

        if (distEntry.getDistance().compareTo(entry_knnDist) < 0
            || distEntry.getDistance().compareTo(knnDist_q) < 0) {
          preInsert(q, entry, knns_q);
          knnDist_q = knns_q.getKNNDistance();
        }
        knnDist_node = Util.max(knnDist_node, entry.getKnnDistance());
      }
    }
    nodeEntry.setKnnDistance(knnDist_node);
  }

  /**
   * Performs a reverse knn query in the specified subtree.
   *
   * @param node   the root node of the current subtree
   * @param o      the id of the object for which the rknn query is performed
   * @param result the list conrtaining the query results
   */
  private void doReverseKNN(RdKNNNode<D> node, O o, List<QueryResult<D>> result) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D> entry = (RdKNNLeafEntry<D>) node.getEntry(i);
        D distance = distanceFunction.distance(entry.getID(), o);
        if (distance.compareTo(entry.getKnnDistance()) <= 0) {
          result.add(new QueryResult<D>(entry.getID(), distance));
        }
      }
    }
    // node is a inner node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D> entry = (RdKNNDirectoryEntry<D>) node.getEntry(i);
        D minDist = distanceFunction.minDist(entry.getMBR(), o);
        if (minDist.compareTo(entry.getKnnDistance()) <= 0) {
          doReverseKNN(file.readPage(entry.getID()), o, result);
        }
      }
    }
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   *
   * @param entry    the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  private void adjustKNNDistance(RdKNNEntry<D> entry, Map<Integer, KNNList<D>> knnLists) {
    RdKNNNode<D> node = file.readPage(entry.getID());
    D knnDist_node = distanceFunction.nullDistance();
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry<D> leafEntry = node.getEntry(i);
        KNNList<D> knns = knnLists.get(leafEntry.getID());
        if (knns != null) {
          leafEntry.setKnnDistance(knnLists.get(leafEntry.getID()).getKNNDistance());
        }
        knnDist_node = Util.max(knnDist_node, leafEntry.getKnnDistance());
      }
    }
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        RdKNNEntry<D> dirEntry = node.getEntry(i);
        adjustKNNDistance(dirEntry, knnLists);
        knnDist_node = Util.max(knnDist_node, dirEntry.getKnnDistance());
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
  protected RdKNNNode<D> createNewLeafNode(int capacity) {
    return new RdKNNNode<D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected RdKNNNode<D> createNewDirectoryNode(int capacity) {
    return new RdKNNNode<D>(file, capacity, false);
  }

  /**
   * Creates a new leaf entry representing the specified data object.
   *
   * @param object the data object to be represented by the new entry
   */
  protected RdKNNEntry<D> createNewLeafEntry(O object) {
    return new RdKNNLeafEntry<D>(object.getID(), getValues(object), distanceFunction.undefinedDistance());
  }

  /**
   * Creates a new directory entry representing the specified node.
   *
   * @param node the node to be represented by the new entry
   */
  protected RdKNNEntry<D> createNewDirectoryEntry(RdKNNNode<D> node) {
    return new RdKNNDirectoryEntry<D>(node.getID(), node.mbr(), node.kNNDistance());
  }

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  protected RdKNNEntry<D> createRootEntry() {
    return new RdKNNDirectoryEntry<D>(0, null, null);
  }
}