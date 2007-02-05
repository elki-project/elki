package de.lmu.ifi.dbs.index.metrical.mtreevariants.mkapp;

import de.lmu.ifi.dbs.data.DatabaseObject;
import de.lmu.ifi.dbs.distance.NumberDistance;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.index.metrical.mtreevariants.util.PQNode;
import de.lmu.ifi.dbs.math.statistics.PolynomialRegression;
import de.lmu.ifi.dbs.utilities.Identifiable;
import de.lmu.ifi.dbs.utilities.KNNList;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.utilities.output.Format;
import de.lmu.ifi.dbs.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.utilities.heap.Heap;
import de.lmu.ifi.dbs.utilities.optionhandling.AttributeSettings;
import de.lmu.ifi.dbs.utilities.optionhandling.Flag;
import de.lmu.ifi.dbs.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.utilities.optionhandling.constraints.GreaterConstraint;

import java.util.*;

/**
 * MkAppTree is a metrical index structure based on the concepts of the M-Tree
 * supporting efficient processing of reverse k nearest neighbor queries for
 * parameter k < kmax.
 *
 * @author Elke Achtert (<a
 *         href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public class MkAppTree<O extends DatabaseObject, D extends NumberDistance<D>> extends AbstractMTree<O, D, MkAppTreeNode<O, D>, MkAppEntry<D>> {

  /**
   * Flag nolog.
   */
  public static final String NOLOG_F = "nolog";

  /**
   * Description for parameter nolog.
   */
  public static final String NOLOG_D = " flag to indicate that the approximation is done in " +
                                       "the ''normal'' space instead of the log-log space (which is default).";

  /**
   * Parameter k.
   */
  public static final String K_P = "k";

  /**
   * Description for parameter k.
   */
  public static final String K_D = "positive integer specifying the maximal number k of reverse" +
                                   "k nearest neighbors to be supported.";

  /**
   * Parameter p.
   */
  public static final String P_P = "p";

  /**
   * Description for parameter p.
   */
  public static final String P_D = "positive integer specifying the order of the polynomial approximation.";

  /**
   * Parameter k.
   */
  private int k_max;

  /**
   * Parameter p.
   */
  private int p;

  /**
   * Flag log.
   */
  private boolean log;

  /**
   * Creates a new MkCopTree.
   */
  public MkAppTree() {
    super();

    optionHandler.put(K_P, new IntParameter(K_P, K_D, new GreaterConstraint(0)));
    optionHandler.put(P_P, new IntParameter(P_P, P_D, new GreaterConstraint(0)));
    optionHandler.put(NOLOG_F, new Flag(NOLOG_F, NOLOG_D));

//    this.debug = true;
  }

  /**
   * Inserts the specified object into this MDkNNTree-Tree. This operation is
   * not supported.
   *
   * @param object the object to be inserted
   */
  public void insert(O object) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  /**
   * Performs necessary operations before inserting the specified entry.
   *
   * @param entry the entry to be inserted
   */
  protected void preInsert(MkAppEntry<D> entry) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  /**
   * Inserts the specified objects into this MkApp-Tree.
   *
   * @param objects the object to be inserted
   */
  public void insert(List<O> objects) {
    if (this.debug) {
      debugFine("insert " + objects + "\n");
    }

    if (!initialized) {
      initialize(objects.get(0));
    }

    List<Integer> ids = new ArrayList<Integer>();
    Map<Integer, KNNList<D>> knnLists = new HashMap<Integer, KNNList<D>>();

    for (O object : objects) {
      // create knnList for the object
      ids.add(object.getID());
      knnLists.put(object.getID(), new KNNList<D>(k_max + 1, getDistanceFunction().infiniteDistance()));

      // insert the object
      super.insert(object, false);
    }

    // do batch nn
    batchNN(getRoot(), ids, knnLists);

    // adjust the knn distances
    adjustApproximatedKNNDistances(getRootEntry(), knnLists);

    if (debug) {
      getRoot().test(this, getRootEntry());
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
  public List<QueryResult<D>> reverseKNNQuery(O object, int k) {
    List<QueryResult<D>> result = doReverseKNNQuery(k, object.getID());
    Collections.sort(result);
    return result;
  }

  /**
   * Returns the value of the k_max parameter.
   *
   * @return the value of the k_max parameter
   */
  public int getK_max() {
    return k_max;
  }

  /**
   * @see de.lmu.ifi.dbs.utilities.optionhandling.Parameterizable#setParameters(String[])
   */
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    k_max = (Integer) optionHandler.getOptionValue(K_P);
    p = (Integer) optionHandler.getOptionValue(P_P);
    log = ! optionHandler.isSet(NOLOG_F);

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
    mySettings.addSetting(P_P, Integer.toString(p));
    mySettings.addSetting(NOLOG_F, Boolean.toString(!log));
    return attributeSettings;
  }

  /**
   * Determines the maximum and minimum number of entries in a node.
   */
  protected void initializeCapacities(O object, boolean verbose) {
    NumberDistance dummyDistance = getDistanceFunction().nullDistance();
    int distanceSize = dummyDistance.externalizableSize();

    // overhead = index(4), numEntries(4), id(4), isLeaf(0.125)
    double overhead = 12.125;
    if (pageSize - overhead < 0) {
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");
    }

    // dirCapacity = (pageSize - overhead) / (nodeID + objectID +
    // coveringRadius + parentDistance + approx) + 1
    dirCapacity = (int) (pageSize - overhead)
                  / (4 + 4 + distanceSize + distanceSize + (p + 1) * 4 + 2) + 1;

    if (dirCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");
    }

    if (dirCapacity < 10) {
      warning("Page size is choosen too small! Maximum number of entries "
              + "in a directory node = " + (dirCapacity - 1));
    }

    // leafCapacity = (pageSize - overhead) / (objectID + parentDistance +
    // approx) + 1
    leafCapacity = (int) (pageSize - overhead)
                   / (4 + distanceSize + (p + 1) * 4 + 2) + 1;

    if (leafCapacity <= 1) {
      throw new RuntimeException("Node size of " + pageSize
                                 + " Bytes is chosen too small!");
    }

    if (leafCapacity < 10) {
      warning("Page size is choosen too small! Maximum number of entries "
              + "in a leaf node = " + (leafCapacity - 1));
    }

    initialized = true;

    if (verbose) {
      verbose("Directory Capacity: " + (dirCapacity - 1) + "\nLeaf Capacity:    " + (leafCapacity - 1));
    }
  }

  /**
   * Performs a reverse knn query.
   *
   * @param k the parametr k of the rknn query
   * @param q the id of the query object
   */
  private List<QueryResult<D>> doReverseKNNQuery(int k, Integer q) {

    List<QueryResult<D>> result = new ArrayList<QueryResult<D>>();
    final Heap<D, Identifiable> pq = new DefaultHeap<D, Identifiable>();

    // push root
    pq.addNode(new PQNode<D>(getDistanceFunction().nullDistance(), getRootEntry().getID(), null));

    // search in tree
    while (!pq.isEmpty()) {
      PQNode<D> pqNode = (PQNode<D>) pq.getMinNode();

      MkAppTreeNode<O, D> node = getNode(pqNode.getValue().getID());

      // directory node
      if (!node.isLeaf()) {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MkAppEntry<D> entry = node.getEntry(i);
          D distance = getDistanceFunction().distance(entry.getRoutingObjectID(), q);
          D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ?
                      getDistanceFunction().nullDistance() :
                      distance.minus(entry.getCoveringRadius());

          double approxValue = log ?
                               Math.exp(entry.approximatedValueAt(k)):
                               entry.approximatedValueAt(k);
          if (approxValue < 0) approxValue = 0;
          D approximatedKnnDist = getDistanceFunction().valueOf(Double.toString(approxValue));

          if (minDist.compareTo(approximatedKnnDist) <= 0) {
            pq.addNode(new PQNode<D>(minDist, entry.getID(), entry.getRoutingObjectID()));
          }
        }
      }
      // data node
      else {
        for (int i = 0; i < node.getNumEntries(); i++) {
          MkAppLeafEntry<D> entry = (MkAppLeafEntry<D>) node.getEntry(i);
          D distance = getDistanceFunction().distance(entry.getRoutingObjectID(), q);
          double approxValue = log ?
                               Math.exp(entry.approximatedValueAt(k)):
                               entry.approximatedValueAt(k);
          if (approxValue < 0) approxValue = 0;
          D approximatedKnnDist = getDistanceFunction().valueOf(Double.toString(approxValue));

          if (distance.compareTo(approximatedKnnDist) <= 0) {
            result.add(new QueryResult<D>(entry.getRoutingObjectID(), distance));
          }
        }
      }
    }
    return result;
  }

  private List<D> getMeanKNNList(List<Integer> ids, Map<Integer, KNNList<D>> knnLists) {
    double[] means = new double[k_max];
    for (Integer id : ids) {
      KNNList<D> knns = knnLists.get(id);
      List<D> knnDists = knns.distancesToList();
      for (int k = 0; k < k_max; k++) {
        D knnDist = knnDists.get(k);
        means[k] += knnDist.getDoubleValue();
      }
    }

    List<D> result = new ArrayList<D>();
    for (int k = 0; k < k_max; k++) {
      means[k] /= ids.size();
      result.add(getDistanceFunction().valueOf(Double.toString(means[k])));
    }

    return result;
  }

  /**
   * Adjusts the knn distance in the subtree of the specified root entry.
   *
   * @param entry    the root entry of the current subtree
   * @param knnLists a map of knn lists for each leaf entry
   */
  private void adjustApproximatedKNNDistances(MkAppEntry<D> entry, Map<Integer, KNNList<D>> knnLists) {
    MkAppTreeNode<O, D> node = file.readPage(entry.getID());

    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkAppLeafEntry<D> leafEntry = (MkAppLeafEntry<D>) node.getEntry(i);
//        approximateKnnDistances(leafEntry, getKNNList(leafEntry.getRoutingObjectID(), knnLists));

        List<Integer> ids = new ArrayList<Integer>();
        ids.add(leafEntry.getID());
        PolynomialApproximation approx = approximateKnnDistances(leafEntry.getID(), getMeanKNNList(ids, knnLists));
        leafEntry.setKnnDistanceApproximation(approx);
      }
    }
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkAppEntry<D> dirEntry = node.getEntry(i);
        adjustApproximatedKNNDistances(dirEntry, knnLists);
      }
    }

//    PolynomialApproximation approx1 = node.knnDistanceApproximation();
//    System.out.println("\napprox1 " + approx1);
    List<Integer> ids = new ArrayList<Integer>();
    leafEntryIDs(node, ids);
    PolynomialApproximation approx = approximateKnnDistances(node.getID(), getMeanKNNList(ids, knnLists));
//    System.out.println("approx2 " + approx);
    // todo
    entry.setKnnDistanceApproximation(approx);
  }

  /**
   * Determines the ids of the leaf entries stored in the specified subtree.
   *
   * @param node   the root of the subtree
   * @param result the result list containing the ids of the leaf entries stored in the specified subtree
   */
  private void leafEntryIDs(MkAppTreeNode<O, D> node, List<Integer> result) {
    if (node.isLeaf()) {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkAppEntry<D> entry = node.getEntry(i);
        result.add(entry.getID());
      }
    }
    else {
      for (int i = 0; i < node.getNumEntries(); i++) {
        MkAppTreeNode<O, D> childNode = getNode(node.getEntry(i));
        leafEntryIDs(childNode, result);
      }
    }
  }

  /**
   * Computes the polynomial approximation
   * of the specified knn-distances.
   *
   * @param knnDistances the knn-distances of the leaf entry
   */
  private PolynomialApproximation approximateKnnDistances(int id, List<D> knnDistances) {
    StringBuffer msg = new StringBuffer();

    // count the zero distances (necessary of log-log space is used)
    int k_0 = 0;
    if (log) {
      for (int i = 0; i < k_max; i++) {
        double dist = knnDistances.get(i).getDoubleValue();
        if (dist == 0) {
          k_0++;
        }
        else {
          break;
        }
      }
    }

    de.lmu.ifi.dbs.math.linearalgebra.Vector x = new de.lmu.ifi.dbs.math.linearalgebra.Vector(k_max - k_0);
    de.lmu.ifi.dbs.math.linearalgebra.Vector y = new de.lmu.ifi.dbs.math.linearalgebra.Vector(k_max - k_0);

    for (int k = 0; k < k_max - k_0; k++) {
      if (log) {
        x.set(k, Math.log(k + k_0));
        y.set(k, Math.log(knnDistances.get(k + k_0).getDoubleValue()));
      }
      else {
        x.set(k, k + k_0);
        y.set(k, knnDistances.get(k + k_0).getDoubleValue());
      }
    }

    if (id == 0 && p == 1) {
      System.out.println("\nx " + Format.format(x.getColumnPackedCopy(), 4));
      System.out.println("\ny " + Format.format(y.getColumnPackedCopy(), 4));
    }


    PolynomialRegression regression = new PolynomialRegression(y, x, p);
    PolynomialApproximation approximation = new PolynomialApproximation(regression.getEstimatedCoefficients().getColumnPackedCopy());

    if (id == 0)
      System.out.println("\nregression_" + p + " " + Format.format(regression.getEstimatedCoefficients().getColumnPackedCopy(), 4));

    if (debug) {
      msg.append("\napproximation " + approximation);
    }

    if (debug) {
      debugFine(msg.toString());
    }
    return approximation;

  }

  /**
   * Creates a new leaf node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new leaf node
   */
  protected MkAppTreeNode<O, D> createNewLeafNode(int capacity) {
    return new MkAppTreeNode<O, D>(file, capacity, true);
  }

  /**
   * Creates a new directory node with the specified capacity.
   *
   * @param capacity the capacity of the new node
   * @return a new directory node
   */
  protected MkAppTreeNode<O, D> createNewDirectoryNode(int capacity) {
    return new MkAppTreeNode<O, D>(file, capacity, false);
  }

  /**
   * Creates a new leaf entry representing the specified data object
   * in the specified subtree.
   *
   * @param object         the data object to be represented by the new entry
   * @param parentDistance the distance from the object to the routing object of the parent node
   */
  protected MkAppEntry<D> createNewLeafEntry(O object, D parentDistance) {
    MkAppLeafEntry<D> leafEntry = new MkAppLeafEntry<D>(object.getID(), parentDistance, null);
    return leafEntry;
  }

  /**
   * Creates a new directory entry representing the specified node.
   *
   * @param node            the node to be represented by the new entry
   * @param routingObjectID the id of the routing object of the node
   * @param parentDistance  the distance from the routing object of the node to the routing object of the parent node
   */
  protected MkAppEntry<D> createNewDirectoryEntry(MkAppTreeNode<O, D> node, Integer routingObjectID, D parentDistance) {
    return new MkAppDirectoryEntry<D>(routingObjectID, parentDistance, node.getID(),
                                      node.coveringRadius(routingObjectID, this),
                                      null);
  }

  /**
   * Creates an entry representing the root node.
   *
   * @return an entry representing the root node
   */
  protected MkAppEntry<D> createRootEntry() {
    return new MkAppDirectoryEntry<D>(null, null, 0, null, null);
  }
}
