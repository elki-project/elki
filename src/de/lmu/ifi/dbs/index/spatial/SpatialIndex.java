package de.lmu.ifi.dbs.index.spatial;

import de.lmu.ifi.dbs.data.RealVector;
import de.lmu.ifi.dbs.utilities.QueryResult;
import de.lmu.ifi.dbs.distance.Distance;

import java.util.List;

/**
 * Defines the requirements for a spatial index that can be used to efficiently store data.
 *
 * @author Elke Achtert (<a href="mailto:achtert@dbs.ifi.lmu.de">achtert@dbs.ifi.lmu.de</a>)
 */
public interface SpatialIndex<O extends RealVector> {

  /**
   * Inserts the specified reel vector object into this index.
   *
   * @param o the vector to be inserted
   */
  void insert(O o);

  /**
   * Deletes the specified obect from this index.
   *
   * @param o the object to be deleted
   * @return true if this index did contain the object, false otherwise
   */
  boolean delete(O o);

  /**
   * Performs a range query for the given RealVectorc with the given
   * epsilon range and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param obj              the query object
   * @param epsilon          the string representation of the query range
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  <D extends Distance> List<QueryResult<D>> rangeQuery(final O obj, final String epsilon,
                               final SpatialDistanceFunction<O,D> distanceFunction);

  /**
   * Performs a k-nearest neighbor query for the given RealVector with the given
   * parameter k and the according distance function.
   * The query result is in ascending order to the distance to the
   * query object.
   *
   * @param obj              the query object
   * @param k                the number of nearest neighbors to be returned
   * @param distanceFunction the distance function that computes the distances beween the objects
   * @return a List of the query results
   */
  <D extends Distance> List<QueryResult<D>> kNNQuery(final O obj, final int k,
                             final SpatialDistanceFunction<O,D> distanceFunction);

  /**
   * Returns the IO-Access of this index.
   *
   * @return the IO-Access of this index
   */
  long getIOAccess();

  /**
   * Resets the IO-Access of this index.
   */
  void resetIOAccess();

  /**
   * Returns a list of entries pointing to the leaf nodes of this spatial index.
   *
   * @return a list of entries pointing to the leaf nodes of this spatial index
   */
  List<DirectoryEntry> getLeaves();

  /**
   * Returns the spatial node with the specified ID.
   *
   * @param nodeID the id of the node to be returned
   * @return the spatial node with the specified ID
   */
  SpatialNode getNode(int nodeID);

  /**
   * Returns the entry that denotes the root.
   * @return the entry that denotes the root
   */
  DirectoryEntry getRootEntry();
}
