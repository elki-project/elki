package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.utilities.QueryResult;

import java.util.List;

/**
 * Defines the requirements for a spatial index that can be used to efficiently store data.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialIndex {

  /**
   * Inserts the specified object into this index.
   *
   * @param o the data object to be inserted
   */
  void insert(SpatialData o);

  /**
   * Deletes the specified obect from this index.
   *
   * @param o the object to be deleted
   * @return true if this index did contain the object, false otherwise
   */
  boolean delete(SpatialData o);

  /**
   * Performs a range query for the given spatial objec with the given
   * epsilon range and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param obj              the query object
   * @param epsilon          the string representation of the query range
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
//  List<QueryResult> rangeQuery(final SpatialData obj, final String epsilon,
//                               final SpatialDistanceFunction distanceFunction);

//  List<DBNeighbor> kNNQuery(final Indexable o, final int k,
//                        final SpatialDistanceFunction distanceFunction);

//  IndexableIterator dataIterator();

//  int getIOAccess();

//  void resetIOAccess();

//  SpatialNode getNode(int nodeID);

//  SpatialNode getRoot();

//  LeafIterator leafIterator();

}
