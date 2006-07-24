package de.lmu.ifi.dbs.index.spatial.rstarvariants.rdnn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.DistanceEntry;
import de.lmu.ifi.dbs.index.IndexHeader;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.rstarvariants.NoFlatRStarTree;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Parameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

/**
 * RDkNNTree is a spatial index structure based on the concepts of the R*-Tree
 * supporting efficient processing of reverse k nearest neighbor queries. The
 * k-nn distance is stored in each entry of a node.
 *
 * todo: noch nicht fertig!!!
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RdKNNTree<O extends NumberVector, D extends NumberDistance<D>> extends NoFlatRStarTree<O, RdKNNNode<D>, RdKNNEntry<D>> {

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
    optionHandler.put(K_P, new Parameter(K_P,K_D,Parameter.Types.INT));
    optionHandler.put(DISTANCE_FUNCTION_P, new Parameter(DISTANCE_FUNCTION_P,DISTANCE_FUNCTION_D,Parameter.Types.CLASS));
  }

  /**
   * Inserts the specified reel vector object into this index.
   *
   * @param o the vector to be inserted
   */
  public void insert(O o) {
    if (this.debug) {
    	debugFiner("insert " + o + "\n");
    }

    if (!initialized) {
      initialize(o);
    }

    RdKNNEntry<D> entry = createNewLeafEntry(o);
    KNNList<D> knns_o = new KNNList<D>(k_max, distanceFunction.infiniteDistance());
    preInsert(entry, getRoot(), knns_o);

    reinsertions.clear();
    insertLeafEntry(entry);
  }

  /**
   * Deletes the specified obect from this index.
   *
   * @param o the object to be deleted
   * @return true if this index did contain the object with the specified id,
   *         false otherwise
   */
  public boolean delete(O o) {
    boolean delete = super.delete(o);
    if (! delete) return delete;

    // reverse knn of o
    List<QueryResult<D>> rnns = new ArrayList<QueryResult<D>>();
    doReverseKNN(getRoot(), o.getID(), rnns);

    // knn of rnn
    List<Integer> ids = new ArrayList<Integer>();
    for (QueryResult<D> rnn: rnns) {
      ids.add(rnn.getID());
    }

    final Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>(ids.size());
    for (Integer id : ids) {
      knnLists.put(id, new KNNList<D>(k_max, distanceFunction.infiniteDistance()));
    }
    batchNN(getRoot(), ids, distanceFunction, knnLists);

    // todo knn dist in leaf entry
    // adjust knn distances
    adjustKNNDistance(getRootEntry());

    return delete;
  }

  /**
   * Performs a reverse k-nearest neighbor query for the given object ID. The
   * query result is in ascending order to the distance to the query object.
   *
   * @param object the query object
   * @param k      the number of nearest neighbors to be returned
   * @return a List of the query results
   */
  public <T extends Distance<T>>List<QueryResult<T>> reverseKNNQuery(O object, int k, DistanceFunction<O, T> distanceFunction) {
    if (! distanceFunction.getClass().equals(this.distanceFunction.getClass()))
      throw new IllegalArgumentException("Wrong distancefuction!");

    if (k != k_max) {
      throw new IllegalArgumentException("Parameter k is not supported!");
    }

    List<QueryResult<D>> rnns = new ArrayList<QueryResult<D>>();
    doReverseKNN(getRoot(), object.getID(), rnns);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (QueryResult<D> qr : rnns) {
      //noinspection unchecked
      result.add((QueryResult<T>) qr);
    }
    return result;

  }

  /**
   * @see de.lmu.ifi.dbs.index.Index#createHeader()
   */
  protected IndexHeader createHeader() {
    return new RdKNNTreeHeader(pageSize, dirCapacity, leafCapacity, dirMinimum, leafCapacity, k_max);
  }

  /**
   * @see de.lmu.ifi.dbs.index.Index#initializeCapacities(de.lmu.ifi.dbs.data.DatabaseObject)
   * todo
   */
  protected void initializeCapacities(O object) {
    int dimensionality = object.getDimensionality();
    NumberDistance dummyDistance = distanceFunction.nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), parentID(4), id(4), isLeaf(0.125)
    double overhead = 16.125;
    if (pageSize - overhead < 0)
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");

    // dirCapacity = (pageSize - overhead) / (childID + childMBR + knnDistance) + 1
    dirCapacity = (int) ((pageSize - overhead) / (4 + 16 * dimensionality + distanceSize)) + 1;

    if (dirCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize + " Bytes is chosen too small!");

    if (dirCapacity < 10)
    	warning("Page size is choosen too small! Maximum number of entries "
                    + "in a directory node = " + (dirCapacity - 1));

    // minimum entries per directory node
    dirMinimum = (int) Math.round((dirCapacity - 1) * 0.5);
    if (dirMinimum < 2)
      dirMinimum = 2;

    // leafCapacity = (pageSize - overhead) / (childID + childValues + knnDistance) + 1
    leafCapacity = (int) ((pageSize - overhead) / (4 + 8 * dimensionality + distanceSize)) + 1;

    if (leafCapacity <= 1)
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");

    if (leafCapacity < 10)
    	warning("Page size is choosen too small! Maximum number of entries "
                    + "in a leaf node = " + (leafCapacity - 1));

    // minimum entries per leaf node
    leafMinimum = (int) Math.round((leafCapacity - 1) * 0.5);
    if (leafMinimum < 2)
      leafMinimum = 2;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    // distance function
    if (optionHandler.isSet(DISTANCE_FUNCTION_P)) {
      String className = optionHandler.getOptionValue(DISTANCE_FUNCTION_P);
      try {
        // noinspection unchecked
        distanceFunction = Util.instantiate(SpatialDistanceFunction.class, className);
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(DISTANCE_FUNCTION_P,
                                               className, DISTANCE_FUNCTION_D, e);
      }
    }
    else {
      try {
        // noinspection unchecked
        distanceFunction = Util.instantiate(SpatialDistanceFunction.class,
                                            DEFAULT_DISTANCE_FUNCTION);
      }
      catch (UnableToComplyException e) {
        throw new WrongParameterValueException(DISTANCE_FUNCTION_P,
                                               DEFAULT_DISTANCE_FUNCTION, DISTANCE_FUNCTION_D, e);
      }
    }

    remainingParameters = distanceFunction.setParameters(remainingParameters);
    setParameters(args, remainingParameters);

    // k_max
    try {
      k_max = Integer.parseInt(optionHandler.getOptionValue(K_P));
      if (k_max <= 0)
        throw new WrongParameterValueException(K_P, optionHandler.getOptionValue(K_P), K_D);
    }
    catch (NumberFormatException e) {
      throw new WrongParameterValueException(K_P, optionHandler.getOptionValue(K_P), K_D, e);
    }
    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Returns the parameter setting of the attributes.
   *
   * @return the parameter setting of the attributes
   */
  public List<AttributeSettings> getAttributeSettings() {
    List<AttributeSettings> attributeSettings = super.getAttributeSettings();
    AttributeSettings mySettings = attributeSettings.get(0);
    mySettings.addSetting(K_P, Integer.toString(k_max));
    return attributeSettings;
  }

  /**
   * Adapts the knn distances.
   *
   * @param node
   * @param q
   * @param knns_q
   */
  private D preInsert(RdKNNEntry<D> q, RdKNNNode<D> node, KNNList<D> knns_q) {
    D maxDist = distanceFunction.nullDistance();
    D knnDist_q = knns_q.getKNNDistance();

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
          doKNNQuery(p, distanceFunction, knns_p);

          if (knns_p.size() < k_max)
            p.setKnnDistance(distanceFunction.undefinedDistance());
          else {
            D knnDist_p = knns_p.getMaximumDistance();
            p.setKnnDistance(knnDist_p);
          }
        }
        maxDist = Util.max(maxDist, p.getKnnDistance());
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
          RdKNNNode<D> childNode = getNode(entry);
          D entry_knnDist1 = preInsert(q, childNode, knns_q);
          entry.setKnnDistance(entry_knnDist1);
          knnDist_q = knns_q.getKNNDistance();
        }
        maxDist = Util.max(maxDist, entry.getKnnDistance());
      }
    }
    return maxDist;
  }

  private void doReverseKNN(RdKNNNode<D> node, Integer id, List<QueryResult<D>> result) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D> entry = (RdKNNLeafEntry<D>) node.getEntry(i);
        D distance = distanceFunction.distance(entry.getID(), id);
        if (distance.compareTo(entry.getKnnDistance()) <= 0) {
          result.add(new QueryResult<D>(entry.getID(), distance));
        }
      }
    }
    // node is a inner node
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D> entry = (RdKNNDirectoryEntry<D>) node.getEntry(i);
        D minDist = distanceFunction.minDist(entry.getMBR(), id);
        if (minDist.compareTo(entry.getKnnDistance()) <= 0) {
          doReverseKNN(file.readPage(entry.getID()), id, result);
        }
      }
    }
  }

  /**
   * Adjusts the knn distance in the specified entry.
   *
   * @param entry the entry
   */
  private void adjustKNNDistance(RdKNNEntry<D> entry) {
    RdKNNNode<D> node = file.readPage(entry.getID());
    D knnDist_node = distanceFunction.undefinedDistance();
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        RdKNNLeafEntry<D> leafEntry = (RdKNNLeafEntry<D>) node.getEntry(i);
        Util.max(knnDist_node, leafEntry.getKnnDistance());
      }
      entry.setKnnDistance(knnDist_node);
    }
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        RdKNNDirectoryEntry<D> dirEntry = (RdKNNDirectoryEntry<D>) node.getEntry(i);
        adjustKNNDistance(dirEntry);
        knnDist_node = Util.max(knnDist_node, dirEntry.getKnnDistance());
      }
    }
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
    return new RdKNNLeafEntry<D>(0, null, distanceFunction.undefinedDistance());
  }

}
