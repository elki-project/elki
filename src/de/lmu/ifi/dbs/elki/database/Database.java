package de.lmu.ifi.dbs.elki.database;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.StaticDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;

/**
 * Database specifies the requirements for any database implementation. Note
 * that any implementing class is supposed to provide a constructor without
 * parameters for dynamic instantiation.
 * 
 * @author Elke Achtert
 * 
 * @apiviz.landmark
 * @apiviz.has DataQuery oneway - - provides
 * @apiviz.has DistanceQuery oneway - - provides
 * @apiviz.has KNNQuery oneway - - provides
 * @apiviz.has RangeQuery oneway - - provides
 * @apiviz.has RKNNQuery oneway - - provides
 * @apiviz.has Representation
 * @apiviz.has Index oneway - - manages
 * @apiviz.uses DataStoreListener oneway - - invokes
 */
public interface Database extends HierarchicalResult {
  /**
   * Initialize the database, for example by loading the input data. (Since this
   * should NOT be done on construction time!)
   */
  public void initialize();

  /**
   * Returns the number of objects contained in this Database.
   * 
   * @return the number of objects in this Database
   */
  @Deprecated
  int size();
  
  /**
   * Get all relations of a database.
   * 
   * @return All relations in the database
   */
  Collection<Relation<?>> getRelations();

  /**
   * Get an object representation.
   * 
   * @param <O> Object type
   * @param restriction Type restriction
   * @param hints Optimizer hints
   * @return representation
   */
  <O> Relation<O> getRelation(TypeInformation restriction, Object... hints) throws NoSupportedDataTypeException;

  /**
   * Get all matching object representations.
   * 
   * @param <O> Object type
   * @param restriction Type restriction
   * @param hints Optimizer hints
   * @return Object representation
   */
  // TODO: add
  // <O> Collection<DataQuery<O>> getObjectQueries(TypeInformation restriction,
  // Object... hints);

  /**
   * Get the distance query for a particular distance function.
   * 
   * @param <O> Object type
   * @param <D> Distance result type
   * @param relation Relation used
   * @param distanceFunction Distance function to use
   * @param hints Optimizer hints
   * @return Instance to query the database with this distance
   */
  <O, D extends Distance<D>> DistanceQuery<O, D> getDistanceQuery(Relation<O> relation, DistanceFunction<? super O, D> distanceFunction, Object... hints);

  /**
   * Get the similarity query for a particular similarity function.
   * 
   * @param <O> Object type
   * @param <D> Similarity result type
   * @param relation Relation used
   * @param similarityFunction Similarity function to use
   * @param hints Optimizer hints
   * @return Instance to query the database with this similarity
   */
  <O, D extends Distance<D>> SimilarityQuery<O, D> getSimilarityQuery(Relation<O> relation, SimilarityFunction<? super O, D> similarityFunction, Object... hints);

  /**
   * Get a KNN query object for the given distance query.
   * 
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * 
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK} bulk
   * query needed</li>
   * </ul>
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param distanceQuery Distance query
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  <O, D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints);

  /**
   * Get a range query object for the given distance query.
   * 
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * 
   * Hints include:
   * <ul>
   * <li>Distance object: Maximum query range</li>
   * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK} bulk
   * query needed</li>
   * </ul>
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param distanceQuery Distance query
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  <O, D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceQuery<O, D> distanceQuery, Object... hints);

  /**
   * Get a rKNN query object for the given distance query.
   * 
   * When possible, this will use an index, but it may default to an expensive
   * linear scan.
   * 
   * Hints include:
   * <ul>
   * <li>Integer: maximum value for k needed</li>
   * <li>{@link de.lmu.ifi.dbs.elki.database.query.DatabaseQuery#HINT_BULK} bulk
   * query needed</li>
   * </ul>
   * 
   * @param <O> Object type
   * @param <D> Distance type
   * @param distanceQuery Distance query
   * @param hints Optimizer hints
   * @return KNN Query object
   */
  <O, D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints);

  /**
   * Returns the DatabaseObject represented by the specified id.
   * 
   * @param id the id of the Object to be obtained from the Database
   * @return Bundle containing the objects' data
   */
  SingleObjectBundle getBundle(DBID id);

  /**
   * Returns the DatabaseObject represented by the specified id.
   * 
   * @param id the id of the Object to be obtained from the Database
   * @return Bundle containing the object data
   * @throws ObjectNotFoundException when the DBID was not found.
   */
  // TODO: add
  // MultipleObjectsBundle getBundles(DBIDs id) throws ObjectNotFoundException;

  /**
   * Returns a list comprising all IDs currently in use.
   * 
   * The list returned shall not be linked to any actual list possibly hold in
   * the database implementation.
   * 
   * @return a list comprising all IDs currently in use
   */
  @Deprecated
  StaticDBIDs getDBIDs();

  /**
   * Add a new index to the database.
   * 
   * @param index Index to add
   */
  public void addIndex(Index index);

  /**
   * Collection of known indexes
   * 
   * @param Collection
   */
  public Collection<Index> getIndexes();

  /**
   * Remove a particular index
   * 
   * @param index Index to remove
   */
  public void removeIndex(Index index);

  /**
   * Adds a listener for the <code>DataStoreEvent</code> posted after the
   * content of the database changes.
   * 
   * @param l the listener to add
   * @see #removeDataStoreListener(DataStoreListener)
   * @see DataStoreListener
   * @see DataStoreEvent
   */
  void addDataStoreListener(DataStoreListener l);

  /**
   * Removes a listener previously added with
   * {@link #addDataStoreListener(DataStoreListener)}.
   * 
   * @param l the listener to remove
   * @see #addDataStoreListener(DataStoreListener)
   * @see DataStoreListener
   * @see DataStoreEvent
   */
  void removeDataStoreListener(DataStoreListener l);

  /**
   * Collects all insertion, deletion and update events until
   * {@link #flushDataStoreEvents()} is called.
   * 
   * @see DataStoreEvent
   */
  void accumulateDataStoreEvents();

  /**
   * Fires all collected insertion, deletion and update events as one
   * DataStoreEvent, i.e. notifies all registered DataStoreListener how the
   * content of the database has been changed since
   * {@link #accumulateDataStoreEvents()} has been called.
   * 
   * @see DataStoreListener
   * @see DataStoreEvent
   */
  void flushDataStoreEvents();
}