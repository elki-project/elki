package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery.Instance;
import de.lmu.ifi.dbs.elki.database.query.knn.MetricalIndexKNNQueryInstance;
import de.lmu.ifi.dbs.elki.distance.DistanceUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.BreadthFirstEnumeration;
import de.lmu.ifi.dbs.elki.index.tree.DistanceEntry;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPath;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexPathComponent;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.split.Assignments;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.split.MLBDistSplit;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.split.MTreeSplit;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.util.PQNode;
import de.lmu.ifi.dbs.elki.utilities.datastructures.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.heap.DefaultHeap;
import de.lmu.ifi.dbs.elki.utilities.heap.Heap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Abstract super class for all M-Tree variants.
 * 
 * @author Elke Achtert
 * @param <O> the type of DatabaseObject to be stored in the metrical index
 * @param <D> the type of Distance used in the metrical index
 * @param <N> the type of MetricalNode used in the metrical index
 * @param <E> the type of MetricalEntry used in the metrical index
 */
public abstract class AbstractMTree<O extends DatabaseObject, D extends Distance<D>, N extends AbstractMTreeNode<O, D, N, E>, E extends MTreeEntry<D>> extends MetricalIndex<O, D, N, E> {
  /**
   * Debugging flag: do extra integrity checks
   */
  protected final static boolean extraIntegrityChecks = true;

  /**
   * OptionID for {@link #DISTANCE_FUNCTION_PARAM}
   */
  public static final OptionID DISTANCE_FUNCTION_ID = OptionID.getOrCreateOptionID("mtree.distancefunction", "Distance function to determine the distance between database objects.");

  /**
   * Parameter to specify the distance function to determine the distance
   * between database objects, must extend
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction}.
   * <p>
   * Key: {@code -mtree.distancefunction}
   * </p>
   * <p>
   * Default value:
   * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.EuclideanDistanceFunction}
   * </p>
   */
  protected final ObjectParameter<DistanceFunction<O, D>> DISTANCE_FUNCTION_PARAM = new ObjectParameter<DistanceFunction<O, D>>(DISTANCE_FUNCTION_ID, DistanceFunction.class, EuclideanDistanceFunction.class);

  /**
   * Holds the instance of the distance function specified by
   * {@link #DISTANCE_FUNCTION_PARAM}.
   */
  private DistanceFunction<O, D> distanceFunction;

  /**
   * The distance query
   */
  protected DistanceQuery<O, D> distanceQuery;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public AbstractMTree(Parameterization config) {
    super(config);
    config = config.descend(this);
    // parameter distance function
    if(config.grab(DISTANCE_FUNCTION_PARAM)) {
      distanceFunction = DISTANCE_FUNCTION_PARAM.instantiateClass(config);
    }
  }

  /**
   * Throws an UnsupportedOperationException since deletion of objects is not
   * yet supported by an M-Tree.
   * 
   * @throws UnsupportedOperationException thrown, since deletions aren't
   *         implemented yet.
   */
  @Override
  public final boolean delete(@SuppressWarnings("unused") O o) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  /**
   * Throws an UnsupportedOperationException since deletion of objects is not
   * yet supported by an M-Tree.
   * 
   * @throws UnsupportedOperationException
   */
  @Override
  protected final void postDelete(@SuppressWarnings("unused") O o) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  @Override
  public final List<DistanceResultPair<D>> rangeQuery(O object, D epsilon) {
    final List<DistanceResultPair<D>> result = new ArrayList<DistanceResultPair<D>>();

    doRangeQuery(null, getRoot(), object, epsilon, result);

    // sort the result according to the distances
    Collections.sort(result);
    return result;
  }

  @Override
  public final List<DistanceResultPair<D>> kNNQuery(O object, int k) {
    if(k < 1) {
      throw new IllegalArgumentException("At least one object has to be requested!");
    }

    final KNNHeap<D> knnList = new KNNHeap<D>(k, distanceFunction.getDistanceFactory().infiniteDistance());
    doKNNQuery(object, knnList);
    return knnList.toSortedArrayList();
  }

  @Override
  public final DistanceFunction<O, D> getDistanceFunction() {
    return distanceFunction;
  }

  @Override
  public final DistanceQuery<O, D> getDistanceQuery() {
    return distanceQuery;
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> Instance<O, S> getKNNQuery(Database<O> database, DistanceFunction<? super O, S> distanceFunction, int maxk) {
    if(!this.distanceFunction.equals(distanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    MetricalIndex<O, S, ?, ?> idx = (MetricalIndex<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = database.getDistanceQuery(distanceFunction);
    return new MetricalIndexKNNQueryInstance<O, S>(database, idx, dq, maxk);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> Instance<O, S> getKNNQuery(Database<O> database, DistanceQuery<O, S> distanceQuery, int maxk) {
    DistanceFunction<? super O, S> distanceFunction = distanceQuery.getDistanceFunction();
    if(!this.distanceFunction.equals(distanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    MetricalIndex<O, S, ?, ?> idx = (MetricalIndex<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = database.getDistanceQuery(distanceFunction);
    return new MetricalIndexKNNQueryInstance<O, S>(database, idx, dq, maxk);
  }

  /**
   * Get the distance factory
   * 
   * @return the distance factory used
   */
  protected final D getDistanceFactory() {
    return distanceFunction.getDistanceFactory();
  }

  /**
   * Returns a string representation of this M-Tree by performing a breadth
   * first enumeration on the tree and adding the string representation of the
   * visited nodes and their entries to the result.
   * 
   * @return a string representation of this M-Tree
   */
  @Override
  public String toString() {
    StringBuffer result = new StringBuffer();
    int dirNodes = 0;
    int leafNodes = 0;
    int objects = 0;
    int levels = 0;

    N node = getRoot();

    while(!node.isLeaf()) {
      if(node.getNumEntries() > 0) {
        E entry = node.getEntry(0);
        node = getNode(entry);
        levels++;
      }
    }

    BreadthFirstEnumeration<O, N, E> enumeration = new BreadthFirstEnumeration<O, N, E>(this, getRootPath());
    while(enumeration.hasMoreElements()) {
      TreeIndexPath<E> path = enumeration.nextElement();
      E entry = path.getLastPathComponent().getEntry();
      if(entry.isLeafEntry()) {
        objects++;
        result.append("\n    ").append(entry.toString());
      }
      else {
        node = getNode(entry);
        result.append("\n\n").append(node).append(", numEntries = ").append(node.getNumEntries());
        result.append("\n").append(entry.toString());

        if(node.isLeaf()) {
          leafNodes++;
        }
        else {
          dirNodes++;
        }
      }
    }

    result.append(getClass().getName()).append(" hat ").append((levels + 1)).append(" Ebenen \n");
    result.append("DirCapacity = ").append(dirCapacity).append("\n");
    result.append("LeafCapacity = ").append(leafCapacity).append("\n");
    result.append(dirNodes).append(" Directory Nodes \n");
    result.append(leafNodes).append(" Leaf Nodes \n");
    result.append(objects).append(" Objects \n");

    result.append("Logical Page Access: ").append(file.getLogicalPageAccess()).append("\n");
    result.append("Physical Read Access: ").append(file.getPhysicalReadAccess()).append("\n");
    result.append("Physical Write Access: ").append(file.getPhysicalWriteAccess()).append("\n");
    result.append("File ").append(file.getClass()).append("\n");

    return result.toString();
  }

  @Override
  public final void setDatabase(Database<O> database) {
    distanceQuery = database.getDistanceQuery(distanceFunction);
  }

  /**
   * Inserts the specified object into this M-Tree.
   * 
   * @param object the object to be inserted
   * @param withPreInsert if this flag is true, the preInsert method will be
   *        called before inserting the object
   */
  // todo: implement a bulk load for M-Tree and remove this method
  protected final void insert(O object, boolean withPreInsert) {
    if(getLogger().isDebugging()) {
      getLogger().debugFine("insert " + object.getID() + " " + object + "\n");
    }

    if(!initialized) {
      initialize(object);
    }

    // choose subtree for insertion
    TreeIndexPath<E> subtree = choosePath(object.getID(), getRootPath());
    if(getLogger().isDebugging()) {
      getLogger().debugFine("insertion-subtree " + subtree + "\n");
    }

    // determine parent distance
    E parentEntry = subtree.getLastPathComponent().getEntry();
    D parentDistance = distance(parentEntry.getRoutingObjectID(), object.getID());

    // create leaf entry and do pre insert
    E entry = createNewLeafEntry(object, parentDistance);
    if(withPreInsert) {
      preInsert(entry);
    }

    // get parent node
    N parent = getNode(parentEntry);
    parent.addLeafEntry(entry);
    file.writePage(parent);

    // adjust the tree from subtree to root
    adjustTree(subtree);

    // test
    if(extraIntegrityChecks) {
      if(withPreInsert) {
        getRoot().integrityCheck(this, getRootEntry());
      }
    }
  }

  @Override
  protected final void createEmptyRoot(@SuppressWarnings("unused") O object) {
    N root = createNewLeafNode(leafCapacity);
    file.writePage(root);
  }

  /**
   * Performs a k-nearest neighbor query for the given FeatureVector with the
   * given parameter k and the according distance function. The query result is
   * in ascending order to the distance to the query object.
   * 
   * @param q the id of the query object
   * @param knnList the query result list
   */
  protected final void doKNNQuery(O q, KNNHeap<D> knnList) {
    final Heap<D, Integer> pq = new DefaultHeap<D, Integer>();

    // push root
    pq.addNode(new PQNode<D>(getDistanceFactory().nullDistance(), getRootEntry().getEntryID(), null));
    D d_k = knnList.getKNNDistance();

    // search in tree
    while(!pq.isEmpty()) {
      PQNode<D> pqNode = (PQNode<D>) pq.getMinNode();

      if(pqNode.getKey().compareTo(d_k) > 0) {
        return;
      }

      N node = getNode(pqNode.getValue());
      DBID o_p = pqNode.getRoutingObjectID();

      // directory node
      if(!node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          E entry = node.getEntry(i);
          DBID o_r = entry.getRoutingObjectID();
          D r_or = entry.getCoveringRadius();
          D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
          D d2 = o_p != null ? distanceQuery.distance(o_r, o_p) : getDistanceFactory().nullDistance();

          D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

          D sum = d_k.plus(r_or);

          if(diff.compareTo(sum) <= 0) {
            D d3 = distance(o_r, q);
            D d_min = DistanceUtil.max(d3.minus(r_or), getDistanceFactory().nullDistance());
            if(d_min.compareTo(d_k) <= 0) {
              pq.addNode(new PQNode<D>(d_min, entry.getEntryID(), o_r));
            }
          }
        }

      }

      // data node
      else {
        for(int i = 0; i < node.getNumEntries(); i++) {
          E entry = node.getEntry(i);
          DBID o_j = entry.getRoutingObjectID();

          D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
          D d2 = o_p != null ? distanceQuery.distance(o_j, o_p) : getDistanceFactory().nullDistance();

          D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

          if(diff.compareTo(d_k) <= 0) {
            D d3 = distanceQuery.distance(o_j, q);
            if(d3.compareTo(d_k) <= 0) {
              DistanceResultPair<D> queryResult = new DistanceResultPair<D>(d3, o_j);
              knnList.add(queryResult);
              d_k = knnList.getKNNDistance();
            }
          }
        }
      }
    }
  }

  /**
   * Performs a k-nearest neighbor query for the given FeatureVector with the
   * given parameter k and the according distance function. The query result is
   * in ascending order to the distance to the query object.
   * 
   * @param q the id of the query object
   * @param knnList the query result list
   */
  protected final void doKNNQuery(DBID q, KNNHeap<D> knnList) {
    final Heap<D, Integer> pq = new DefaultHeap<D, Integer>();

    // push root
    pq.addNode(new PQNode<D>(getDistanceFactory().nullDistance(), getRootEntry().getEntryID(), null));
    D d_k = knnList.getKNNDistance();

    // search in tree
    while(!pq.isEmpty()) {
      PQNode<D> pqNode = (PQNode<D>) pq.getMinNode();

      if(pqNode.getKey().compareTo(d_k) > 0) {
        return;
      }

      N node = getNode(pqNode.getValue());
      DBID o_p = pqNode.getRoutingObjectID();

      // directory node
      if(!node.isLeaf()) {
        for(int i = 0; i < node.getNumEntries(); i++) {
          E entry = node.getEntry(i);
          DBID o_r = entry.getRoutingObjectID();
          D r_or = entry.getCoveringRadius();
          D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
          D d2 = o_p != null ? distanceQuery.distance(o_r, o_p) : getDistanceFactory().nullDistance();

          D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

          D sum = d_k.plus(r_or);

          if(diff.compareTo(sum) <= 0) {
            D d3 = distance(o_r, q);
            D d_min = DistanceUtil.max(d3.minus(r_or), getDistanceFactory().nullDistance());
            if(d_min.compareTo(d_k) <= 0) {
              pq.addNode(new PQNode<D>(d_min, entry.getEntryID(), o_r));
            }
          }
        }

      }

      // data node
      else {
        for(int i = 0; i < node.getNumEntries(); i++) {
          E entry = node.getEntry(i);
          DBID o_j = entry.getRoutingObjectID();

          D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
          D d2 = o_p != null ? distanceQuery.distance(o_j, o_p) : getDistanceFactory().nullDistance();

          D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

          if(diff.compareTo(d_k) <= 0) {
            D d3 = distanceQuery.distance(o_j, q);
            if(d3.compareTo(d_k) <= 0) {
              DistanceResultPair<D> queryResult = new DistanceResultPair<D>(d3, o_j);
              knnList.add(queryResult);
              d_k = knnList.getKNNDistance();
            }
          }
        }
      }
    }
  }

  /**
   * Chooses the best path of the specified subtree for insertion of the given
   * object.
   * 
   * @param subtree the subtree to be tested for insertion
   * @param objectID the id of the object to be inserted
   * @return the path of the appropriate subtree to insert the given object
   */
  private TreeIndexPath<E> choosePath(DBID objectID, TreeIndexPath<E> subtree) {
    N node = getNode(subtree.getLastPathComponent().getEntry());

    // leaf
    if(node.isLeaf()) {
      return subtree;
    }

    D nullDistance = getDistanceFactory().nullDistance();
    List<DistanceEntry<D, E>> candidatesWithoutExtension = new ArrayList<DistanceEntry<D, E>>();
    List<DistanceEntry<D, E>> candidatesWithExtension = new ArrayList<DistanceEntry<D, E>>();

    for(int i = 0; i < node.getNumEntries(); i++) {
      E entry = node.getEntry(i);
      D distance = distance(objectID, entry.getRoutingObjectID());
      D enlrg = distance.minus(entry.getCoveringRadius());

      if(enlrg.compareTo(nullDistance) <= 0) {
        candidatesWithoutExtension.add(new DistanceEntry<D, E>(entry, distance, i));
      }
      else {
        candidatesWithExtension.add(new DistanceEntry<D, E>(entry, enlrg, i));
      }
    }

    DistanceEntry<D, E> bestCandidate;
    if(!candidatesWithoutExtension.isEmpty()) {
      bestCandidate = Collections.min(candidatesWithoutExtension);
    }
    else {
      Collections.sort(candidatesWithExtension);
      bestCandidate = Collections.min(candidatesWithExtension);
      E entry = bestCandidate.getEntry();
      D cr = entry.getCoveringRadius();
      entry.setCoveringRadius(cr.plus(bestCandidate.getDistance()));
    }

    return choosePath(objectID, subtree.pathByAddingChild(new TreeIndexPathComponent<E>(bestCandidate.getEntry(), bestCandidate.getIndex())));
  }

  /**
   * Performs a batch k-nearest neigbor query for a list of query objects.
   * 
   * @param node the node reprsenting the subtree on which the query should be
   *        performed
   * @param ids the ids of th query objects
   * @param knnLists the knn lists of the query objcets
   */
  protected final void batchNN(N node, DBIDs ids, Map<DBID, KNNHeap<D>> knnLists) {
    if(node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        E p = node.getEntry(i);
        for(DBID q : ids) {
          KNNHeap<D> knns_q = knnLists.get(q);
          D knn_q_maxDist = knns_q.getKNNDistance();

          D dist_pq = distanceQuery.distance(p.getRoutingObjectID(), q);
          if(dist_pq.compareTo(knn_q_maxDist) <= 0) {
            knns_q.add(new DistanceResultPair<D>(dist_pq, p.getRoutingObjectID()));
          }
        }
      }
    }
    else {
      List<DistanceEntry<D, E>> entries = getSortedEntries(node, ids);
      for(DistanceEntry<D, E> distEntry : entries) {
        D minDist = distEntry.getDistance();
        for(DBID q : ids) {
          KNNHeap<D> knns_q = knnLists.get(q);
          D knn_q_maxDist = knns_q.getKNNDistance();

          if(minDist.compareTo(knn_q_maxDist) <= 0) {
            E entry = distEntry.getEntry();
            N child = getNode(entry);
            batchNN(child, ids, knnLists);
            break;
          }
        }
      }
    }
  }

  /**
   * Sorts the entries of the specified node according to their minimum distance
   * to the specified object.
   * 
   * @param node the node
   * @param q the id of the object
   * @return a list of the sorted entries
   */
  protected final List<DistanceEntry<D, E>> getSortedEntries(N node, DBID q) {
    List<DistanceEntry<D, E>> result = new ArrayList<DistanceEntry<D, E>>();

    for(int i = 0; i < node.getNumEntries(); i++) {
      E entry = node.getEntry(i);
      D distance = distance(entry.getRoutingObjectID(), q);
      D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ? getDistanceFactory().nullDistance() : distance.minus(entry.getCoveringRadius());

      result.add(new DistanceEntry<D, E>(entry, minDist, i));
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Sorts the entries of the specified node according to their minimum distance
   * to the specified objects.
   * 
   * @param node the node
   * @param ids the ids of the objects
   * @return a list of the sorted entries
   */
  protected final List<DistanceEntry<D, E>> getSortedEntries(N node, DBIDs ids) {
    List<DistanceEntry<D, E>> result = new ArrayList<DistanceEntry<D, E>>();

    for(int i = 0; i < node.getNumEntries(); i++) {
      E entry = node.getEntry(i);

      D minMinDist = getDistanceFactory().infiniteDistance();
      for(DBID q : ids) {
        D distance = distanceQuery.distance(entry.getRoutingObjectID(), q);
        D minDist = entry.getCoveringRadius().compareTo(distance) > 0 ? getDistanceFactory().nullDistance() : distance.minus(entry.getCoveringRadius());
        minMinDist = DistanceUtil.max(minMinDist, minDist);
      }
      result.add(new DistanceEntry<D, E>(entry, minMinDist, i));
    }

    Collections.sort(result);
    return result;
  }

  /**
   * Returns the distance between the two specified ids.
   * 
   * @param id1 the first id
   * @param id2 the second id
   * @return the distance between the two specified ids
   */
  protected final D distance(DBID id1, DBID id2) {
    if(id1 == null || id2 == null) {
      return getDistanceFactory().undefinedDistance();
    }
    return distanceQuery.distance(id1, id2);
  }

  /**
   * Returns the distance between the given object and the id.
   * 
   * @param id1 the first id
   * @param o2 the second object
   * @return the distance between the two specified objects
   */
  protected final D distance(DBID id1, O o2) {
    if(id1 == null) {
      return getDistanceFactory().undefinedDistance();
    }
    return distanceQuery.distance(id1, o2);
  }

  /**
   * Creates a new leaf entry representing the specified data object.
   * 
   * @param object the data object to be represented by the new entry
   * @param parentDistance the distance from the object to the routing object of
   *        the parent node
   * @return the newly created leaf entry
   */
  abstract protected E createNewLeafEntry(O object, D parentDistance);

  /**
   * Creates a new directory entry representing the specified node.
   * 
   * @param node the node to be represented by the new entry
   * @param routingObjectID the id of the routing object of the node
   * @param parentDistance the distance from the routing object of the node to
   *        the routing object of the parent node
   * @return the newly created directory entry
   */
  abstract protected E createNewDirectoryEntry(N node, DBID routingObjectID, D parentDistance);

  /**
   * Splits the specified node and returns the split result.
   * 
   * @param node the node to be split
   * @return the split result
   */
  private SplitResult split(N node) {
    // do the split
    // todo split stratgey
    MTreeSplit<O, D, N, E> split = new MLBDistSplit<O, D, N, E>(node, distanceQuery);
    Assignments<D, E> assignments = split.getAssignments();
    N newNode = node.splitEntries(assignments.getFirstAssignments(), assignments.getSecondAssignments());

    // write changes to file
    file.writePage(node);
    file.writePage(newNode);

    if(getLogger().isDebugging()) {
      String msg = "Split Node " + node.getPageID() + " (" + this.getClass() + ")\n" + "      newNode " + newNode.getPageID() + "\n" + "      firstPromoted " + assignments.getFirstRoutingObject() + "\n" + "      firstAssignments(" + node.getPageID() + ") " + assignments.getFirstAssignments() + "\n" + "      firstCR " + assignments.getFirstCoveringRadius() + "\n" + "      secondPromoted " + assignments.getSecondRoutingObject() + "\n" + "      secondAssignments(" + newNode.getPageID() + ") " + assignments.getSecondAssignments() + "\n" + "      secondCR " + assignments.getSecondCoveringRadius() + "\n";
      getLogger().debugFine(msg);
    }

    return new SplitResult(split, newNode);
  }

  /**
   * Sorts the entries of the specified node according to their minimum distance
   * to the specified objects.
   * 
   * @param node the node
   * @param ids the ids of the objects
   * @return a list of the sorted entries
   */
  // FIXME: Duplicate from above?
  /*
   * private List<DistanceEntry<D, E>> getSortedEntries(N node, DBIDs ids) {
   * List<DistanceEntry<D, E>> result = new ArrayList<DistanceEntry<D, E>>();
   * 
   * for(int i = 0; i < node.getNumEntries(); i++) { E entry = node.getEntry(i);
   * 
   * D minMinDist = distanceFunction.infiniteDistance(); for(DBID q : ids) { D
   * distance = distance(entry.getRoutingObjectID(), q); D minDist =
   * entry.getCoveringRadius().compareTo(distance) > 0 ?
   * distanceFunction.nullDistance() :
   * distance.minus(entry.getCoveringRadius()); if(minDist.compareTo(minMinDist)
   * < 0) { minMinDist = minDist; } } result.add(new DistanceEntry<D, E>(entry,
   * minMinDist, i)); }
   * 
   * Collections.sort(result); return result; }
   */

  /**
   * Performs a range query on the specified subtree. It recursively traverses
   * all paths from the specified node, which cannot be excluded from leading to
   * qualifying objects.
   * 
   * @param o_p the routing object of the specified node
   * @param node the root of the subtree to be traversed
   * @param q the id of the query object
   * @param r_q the query range
   * @param result the list holding the query results
   */
  private void doRangeQuery(DBID o_p, N node, DBID q, D r_q, List<DistanceResultPair<D>> result) {
    if(!node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        E entry = node.getEntry(i);
        DBID o_r = entry.getRoutingObjectID();

        D r_or = entry.getCoveringRadius();
        D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
        D d2 = o_p != null ? entry.getParentDistance() : getDistanceFactory().nullDistance(); // o_p
                                                                                              // !=
                                                                                              // null
                                                                                              // ?
                                                                                              // distanceFunction.distance(o_r,
                                                                                              // o_p)
                                                                                              // :
                                                                                              // distanceFunction.nullDistance();

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        D sum = r_q.plus(r_or);

        if(diff.compareTo(sum) <= 0) {
          D d3 = distanceQuery.distance(o_r, q);
          if(d3.compareTo(sum) <= 0) {
            N child = getNode(entry.getEntryID());
            doRangeQuery(o_r, child, q, r_q, result);
          }
        }

      }
    }

    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<D> entry = node.getEntry(i);
        DBID o_j = entry.getRoutingObjectID();

        D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
        D d2 = o_p != null ? distanceQuery.distance(o_j, o_p) : getDistanceFactory().nullDistance();

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        if(diff.compareTo(r_q) <= 0) {
          D d3 = distanceQuery.distance(o_j, q);
          if(d3.compareTo(r_q) <= 0) {
            DistanceResultPair<D> queryResult = new DistanceResultPair<D>(d3, o_j);
            result.add(queryResult);
          }
        }
      }
    }
  }

  /**
   * Performs a range query on the specified subtree. It recursively traverses
   * all paths from the specified node, which cannot be excluded from leading to
   * qualifying objects.
   * 
   * @param o_p the routing object of the specified node
   * @param node the root of the subtree to be traversed
   * @param q the id of the query object
   * @param r_q the query range
   * @param result the list holding the query results
   */
  private void doRangeQuery(DBID o_p, N node, O q, D r_q, List<DistanceResultPair<D>> result) {
    if(!node.isLeaf()) {
      for(int i = 0; i < node.getNumEntries(); i++) {
        E entry = node.getEntry(i);
        DBID o_r = entry.getRoutingObjectID();

        D r_or = entry.getCoveringRadius();
        D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
        D d2 = o_p != null ? entry.getParentDistance() : getDistanceFactory().nullDistance(); // o_p
                                                                                              // !=
                                                                                              // null
                                                                                              // ?
                                                                                              // distanceFunction.distance(o_r,
                                                                                              // o_p)
                                                                                              // :
                                                                                              // distanceFunction.nullDistance();

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        D sum = r_q.plus(r_or);

        if(diff.compareTo(sum) <= 0) {
          D d3 = distanceQuery.distance(o_r, q);
          if(d3.compareTo(sum) <= 0) {
            N child = getNode(entry.getEntryID());
            doRangeQuery(o_r, child, q, r_q, result);
          }
        }

      }
    }

    else {
      for(int i = 0; i < node.getNumEntries(); i++) {
        MTreeEntry<D> entry = node.getEntry(i);
        DBID o_j = entry.getRoutingObjectID();

        D d1 = o_p != null ? distanceQuery.distance(o_p, q) : getDistanceFactory().nullDistance();
        D d2 = o_p != null ? distanceQuery.distance(o_j, o_p) : getDistanceFactory().nullDistance();

        D diff = d1.compareTo(d2) > 0 ? d1.minus(d2) : d2.minus(d1);

        if(diff.compareTo(r_q) <= 0) {
          D d3 = distanceQuery.distance(o_j, q);
          if(d3.compareTo(r_q) <= 0) {
            DistanceResultPair<D> queryResult = new DistanceResultPair<D>(d3, o_j);
            result.add(queryResult);
          }
        }
      }
    }
  }

  /**
   * Adjusts the tree after insertion of some nodes.
   * 
   * @param subtree the subtree to be adjusted
   */
  private void adjustTree(TreeIndexPath<E> subtree) {
    if(getLogger().isDebugging()) {
      getLogger().debugFine("Adjust tree " + subtree + "\n");
    }

    // get the root of the subtree
    Integer nodeIndex = subtree.getLastPathComponent().getIndex();
    N node = getNode(subtree.getLastPathComponent().getEntry());

    // overflow in node; split the node
    if(hasOverflow(node)) {
      SplitResult splitResult = split(node);
      N splitNode = splitResult.newNode;
      Assignments<D, E> assignments = splitResult.split.getAssignments();

      // if root was split: create a new root that points the two split
      // nodes
      if(node.getPageID() == getRootEntry().getEntryID()) {
        // FIXME: stimmen die parentDistance der Kinder in node & splitNode?
        TreeIndexPath<E> newRootPath = createNewRoot(node, splitNode, assignments.getFirstRoutingObject(), assignments.getSecondRoutingObject());
        adjustTree(newRootPath);
      }
      // node is not root
      else {
        // get the parent and add the new split node
        E parentEntry = subtree.getParentPath().getLastPathComponent().getEntry();
        N parent = getNode(parentEntry);
        if(getLogger().isDebugging()) {
          getLogger().debugFine("parent " + parent);
        }
        D parentDistance2 = distance(parentEntry.getRoutingObjectID(), assignments.getSecondRoutingObject());
        // logger.warning("parent: "+parent.toString()+" split: " +
        // splitNode.toString()+ " dist:"+parentDistance2);
        parent.addDirectoryEntry(createNewDirectoryEntry(splitNode, assignments.getSecondRoutingObject(), parentDistance2));

        // adjust the entry representing the (old) node, that has been split
        D parentDistance1 = distance(parentEntry.getRoutingObjectID(), assignments.getFirstRoutingObject());
        // logger.warning("parent: "+parent.toString()+" node: " +
        // node.toString()+ " dist:"+parentDistance1);
        node.adjustEntry(parent.getEntry(nodeIndex), assignments.getFirstRoutingObject(), parentDistance1, this);

        // write changes in parent to file
        file.writePage(parent);
        adjustTree(subtree.getParentPath());
      }
    }
    // no overflow, only adjust parameters of the entry representing the
    // node
    else {
      if(node.getPageID() != getRootEntry().getEntryID()) {
        E parentEntry = subtree.getParentPath().getLastPathComponent().getEntry();
        N parent = getNode(parentEntry);
        int index = subtree.getLastPathComponent().getIndex();
        E entry = parent.getEntry(index);
        node.adjustEntry(entry, entry.getRoutingObjectID(), entry.getParentDistance(), this);
        // write changes in parent to file
        file.writePage(parent);
        adjustTree(subtree.getParentPath());
      }
      // root level is reached
      else {
        E rootEntry = getRootEntry();
        node.adjustEntry(rootEntry, rootEntry.getRoutingObjectID(), rootEntry.getParentDistance(), this);
      }
    }
  }

  /**
   * Returns true if in the specified node an overflow has occurred, false
   * otherwise.
   * 
   * @param node the node to be tested for overflow
   * @return true if in the specified node an overflow has occurred, false
   *         otherwise
   */
  private boolean hasOverflow(N node) {
    if(node.isLeaf()) {
      return node.getNumEntries() == leafCapacity;
    }

    return node.getNumEntries() == dirCapacity;
  }

  /**
   * Creates a new root node that points to the two specified child nodes and
   * return the path to the new root.
   * 
   * @param oldRoot the old root of this RTree
   * @param newNode the new split node
   * @param firstRoutingObjectID the id of the routing objects of the first
   *        child node
   * @param secondRoutingObjectID the id of the routing objects of the second
   *        child node
   * @return the path to the new root node that points to the two specified
   *         child nodes
   */
  private TreeIndexPath<E> createNewRoot(final N oldRoot, final N newNode, DBID firstRoutingObjectID, DBID secondRoutingObjectID) {
    N root = createNewDirectoryNode(dirCapacity);
    file.writePage(root);

    // switch the ids
    oldRoot.setPageID(root.getPageID());
    if(!oldRoot.isLeaf()) {
      // FIXME: what is happening here?
      for(int i = 0; i < oldRoot.getNumEntries(); i++) {
        N node = getNode(oldRoot.getEntry(i));
        file.writePage(node);
      }
    }

    root.setPageID(getRootEntry().getEntryID());
    // FIXME: doesn't the root by definition not have a routing object?
    // D parentDistance1 = distance(getRootEntry().getRoutingObjectID(),
    // firstRoutingObjectID);
    // D parentDistance2 = distance(getRootEntry().getRoutingObjectID(),
    // secondRoutingObjectID);
    E oldRootEntry = createNewDirectoryEntry(oldRoot, firstRoutingObjectID, null);
    E newRootEntry = createNewDirectoryEntry(newNode, secondRoutingObjectID, null);
    root.addDirectoryEntry(oldRootEntry);
    root.addDirectoryEntry(newRootEntry);

    // logger.warning("new root: " + getRootEntry().toString() + " childs: " +
    // oldRootEntry.toString() + "," + newRootEntry.toString() + " dists: " +
    // parentDistance1 + ", " + parentDistance2);

    file.writePage(root);
    file.writePage(oldRoot);
    file.writePage(newNode);
    if(getLogger().isDebugging()) {
      String msg = "Create new Root: ID=" + root.getPageID();
      msg += "\nchild1 " + oldRoot;
      msg += "\nchild2 " + newNode;
      getLogger().debugFine(msg);
    }

    return new TreeIndexPath<E>(new TreeIndexPathComponent<E>(getRootEntry(), null));
  }

  /**
   * Encapsulates a split object and the newly created node.
   */
  private class SplitResult {
    protected MTreeSplit<O, D, N, E> split;

    protected N newNode;

    public SplitResult(MTreeSplit<O, D, N, E> split, N newNode) {
      this.split = split;
      this.newNode = newNode;
    }
  }

  /** {@inheritDoc} */
  @Override
  public List<E> getLeaves() {
    List<E> result = new ArrayList<E>();
    BreadthFirstEnumeration<O, N, E> enumeration = new BreadthFirstEnumeration<O, N, E>(this, getRootPath());
    while(enumeration.hasMoreElements()) {
      TreeIndexPath<E> path = enumeration.nextElement();
      E entry = path.getLastPathComponent().getEntry();
      if(entry.isLeafEntry()) {
        // ignore, we are within a leaf!
      }
      else {
        // TODO: any way to skip unnecessary reads?
        N node = getNode(entry);
        if(node.isLeaf()) {
          result.add(entry);
        }
      }
    }
    return result;
  }

  /**
   * FIXME: expensive depth computation by following a path.
   * 
   * @return depth
   */
  public int getHeight() {
    int levels = 0;
    N node = getRoot();

    while(!node.isLeaf()) {
      if(node.getNumEntries() > 0) {
        E entry = node.getEntry(0);
        node = getNode(entry);
        levels++;
      }
    }
    return levels;
  }

  @Override
  public String getLongName() {
    return "Abstract M-Tree";
  }

  @Override
  public String getShortName() {
    return "mtree";
  }
}