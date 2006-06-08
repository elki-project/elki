package de.lmu.ifi.dbs.index.spatial.rstar.rdnn;

import de.lmu.ifi.dbs.data.NumberVector;
import de.lmu.ifi.dbs.distance.Distance;
import de.lmu.ifi.dbs.distance.DistanceFunction;
import de.lmu.ifi.dbs.distance.EuklideanDistanceFunction;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.spatial.SpatialDirectoryEntry;
import de.lmu.ifi.dbs.index.spatial.SpatialLeafEntry;
import de.lmu.ifi.dbs.index.spatial.MBR;
import de.lmu.ifi.dbs.index.spatial.SpatialDistanceFunction;
import de.lmu.ifi.dbs.index.spatial.rstar.RTree;
import de.lmu.ifi.dbs.index.spatial.rstar.RTreeHeader;
import de.lmu.ifi.dbs.index.spatial.rstar.RTreeNode;
import de.lmu.ifi.dbs.index.spatial.rstar.rdnn.RdKNNDirectoryEntry;
import de.lmu.ifi.dbs.index.spatial.rstar.rdnn.RdKNNEntry;
import de.lmu.ifi.dbs.index.spatial.rstar.rdnn.RdKNNLeafEntry;
import de.lmu.ifi.dbs.index.spatial.DistanceEntry;
import de.lmu.ifi.dbs.properties.Properties;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.utilities.Util;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.OptionHandler;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.WrongParameterValueException;

import java.util.ArrayList;
import java.util.List;

/**
 * RDkNNTree is a spatial index structure based on the concepts of the R*-Tree
 * supporting efficient processing of reverse k nearest neighbor queries. The
 * k-nn distance is stored in each entry of a node.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class RdKNNTree<O extends NumberVector, D extends NumberDistance<D>> extends RTree<O> {
  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "<k>positive integer specifying the maximal number k of reverse " +
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
  public static final String DISTANCE_FUNCTION_D = "<class>the distance function to determine the distance between database objects " +
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
    parameterToDescription.put(K_P + OptionHandler.EXPECTS_VALUE, K_D);
    parameterToDescription.put(DISTANCE_FUNCTION_P + OptionHandler.EXPECTS_VALUE, DISTANCE_FUNCTION_D);
    optionHandler = new OptionHandler(parameterToDescription, this.getClass().getName());
  }

  /**
   * Inserts the specified reel vector object into this index.
   *
   * @param o the vector to be inserted
   */
  public void insert(O o) {
    if (DEBUG) {
      logger.fine("insert " + o + "\n");
    }

    if (!initialized) {
      init(o.getDimensionality());
    }

    double[] values = getValues(o);
    RdKNNLeafEntry<D> entry = (RdKNNLeafEntry<D>) createNewLeafEntry(o.getID(), values);
    KNNList<D> knns_o = new KNNList<D>(k_max, distanceFunction.infiniteDistance());
    preInsert(entry, (RdKNNTreeNode<D>) getRoot(), knns_o);

    reinsertions.clear();
    insert(entry);
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
    doReverseKNN(((RdKNNTreeNode<D>) getRoot()), object.getID(), rnns);

    List<QueryResult<T>> result = new ArrayList<QueryResult<T>>();
    for (QueryResult<D> qr : rnns) {
      //noinspection unchecked
      result.add((QueryResult<T>) qr);
    }
    return result;

  }

  /**
   * Creates a header for this RDkNN-Tree.
   * Subclasses may need to overwrite this method.
   */
  protected RTreeHeader createHeader() {
    return new RdKNNTreeHeader(pageSize, dirCapacity, leafCapacity, dirMinimum, leafCapacity, k_max);
  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected RTreeNode createNewLeafNode(int capacity) {
    return new RdKNNTreeNode<D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected RTreeNode createNewDirectoryNode(int capacity) {
    return new RdKNNTreeNode<D>(file, capacity, false);
  }

  /**
   * Creates a new leaf entry with the specified parameters.
   *
   * @param id     the unique id of the underlying data object
   * @param values the values of the underlying data object
   */
  protected SpatialLeafEntry createNewLeafEntry(int id, double[] values) {
    return new RdKNNLeafEntry<D>(id, values, distanceFunction.undefinedDistance());
  }

  /**
   * Creates a new leaf entry with the specified parameters.
   *
   * @param id  the unique id of the underlying spatial object
   * @param mbr the minmum bounding rectangle of the underlying spatial object
   */
  protected SpatialDirectoryEntry createNewDirectoryEntry(int id, MBR mbr) {
    return new RdKNNDirectoryEntry<D>(id, mbr, distanceFunction.undefinedDistance());
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   *
   * @param pageSize       the size of a page in Bytes
   * @param dimensionality the dimensionality of the data to be indexed
   */
  protected void initCapacities(int pageSize, int dimensionality) {
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
      logger.severe("Page size is choosen too small! Maximum number of entries "
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
      logger.severe("Page size is choosen too small! Maximum number of entries "
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
  private D preInsert(RdKNNLeafEntry<D> q, RdKNNTreeNode<D> node, KNNList<D> knns_q) {
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
      List<DistanceEntry<D>> entries = getSortedEntries(node, q.getID(), distanceFunction);
      for (DistanceEntry<D> distEntry : entries) {
        RdKNNDirectoryEntry<D> entry = (RdKNNDirectoryEntry<D>) distEntry.getEntry();
        D entry_knnDist = entry.getKnnDistance();

        if (distEntry.getDistance().compareTo(entry_knnDist) < 0
            || distEntry.getDistance().compareTo(knnDist_q) < 0) {
          RdKNNTreeNode<D> childNode = (RdKNNTreeNode<D>) getNode(entry.getID());
          D entry_knnDist1 = preInsert(q, childNode, knns_q);
          entry.setKnnDistance(entry_knnDist1);
          knnDist_q = knns_q.getKNNDistance();
        }
        maxDist = Util.max(maxDist, entry.getKnnDistance());
      }
    }
    return maxDist;
  }

  private void doReverseKNN(RdKNNTreeNode<D> node, Integer id, List<QueryResult<D>> result) {
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
          doReverseKNN((RdKNNTreeNode<D>) file.readPage(entry.getID()), id, result);
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
    RdKNNTreeNode node = (RdKNNTreeNode) file.readPage(entry.getID());
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



  }
