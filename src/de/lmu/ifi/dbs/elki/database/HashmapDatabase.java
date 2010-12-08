package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.ClassLabel;
import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.LinearScanRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.LinearScanRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.persistent.PageFileStatistics;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.result.AnnotationBuiltins;
import de.lmu.ifi.dbs.elki.result.IDResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Provides a mapping for associations based on a Hashtable and functions to get
 * the next usable ID for insertion, making IDs reusable after deletion of the
 * entry.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * @param <O> the type of DatabaseObject as element of the database
 * 
 * @apiviz.composedOf de.lmu.ifi.dbs.elki.database.DatabaseEventManager
 */
@Description("Database using an in-memory hashtable and at least providing linear scans.")
public class HashmapDatabase<O extends DatabaseObject> extends AbstractHierarchicalResult implements Database<O> {
  /**
   * OptionID for {@link #INDEX_PARAM}
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("db.index", "Database indexes to add.");

  /**
   * Parameter to specify the indexes to use.
   * <p>
   * Key: {@code -db.index}
   * </p>
   */
  private final ObjectListParameter<IndexFactory<O, ?>> INDEX_PARAM = new ObjectListParameter<IndexFactory<O, ?>>(INDEX_ID, IndexFactory.class, true);

  /**
   * Map to hold the objects of the database.
   */
  private WritableDataStore<O> content;

  /**
   * Map to hold the object labels
   */
  private WritableDataStore<String> objectlabels = null;

  /**
   * Map to hold the class labels
   */
  private WritableDataStore<ClassLabel> classlabels = null;

  /**
   * Map to hold the external ids
   */
  private WritableDataStore<String> externalids = null;

  /**
   * IDs of this database
   */
  private TreeSetModifiableDBIDs ids;

  /**
   * Object factory
   */
  private O objectFactory;

  /**
   * The event manager, collects events and fires them on demand.
   */
  protected DatabaseEventManager<O> eventManager = new DatabaseEventManager<O>();

  /**
   * Indexes
   */
  final List<Index<O>> indexes;

  /**
   * Store own parameters, needed for partitioning.
   */
  // TODO: add some point, it would be nice to get rid of this partitioning.
  private Collection<Pair<OptionID, Object>> params;

  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   * 
   * @param config Parameterization
   */
  public HashmapDatabase(Parameterization config) {
    super();
    config.descend(this);
    TrackParameters track = new TrackParameters(config);

    this.ids = DBIDUtil.newTreeSet();
    this.content = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, DatabaseObject.class);
    //this.primaryResults = new java.util.Vector<Result>(4);
    //this.derivedResults = new java.util.Vector<Result>();
    this.addChildResult(new IDResult());
    this.indexes = new java.util.Vector<Index<O>>();

    // Add indexes.
    if(track.grab(INDEX_PARAM)) {
      for(IndexFactory<O, ?> idx : INDEX_PARAM.instantiateClasses(track)) {
        addIndex(idx.instantiate(this));
      }
    }
    params = track.getGivenParameters();
  }

  @Override
  public void addIndex(Index<O> index) {
    this.indexes.add(index);
    this.addChildResult(index);
  }

  /**
   * Calls {@link #doInsert} for each element of the specified list and fires an
   * insertion event.
   * 
   * @throws UnableToComplyException if database reached limit of storage
   *         capacity
   */
  @Override
  public DBIDs insert(List<Pair<O, DatabaseObjectMetadata>> objectsAndAssociationsList) throws UnableToComplyException {
    if(objectsAndAssociationsList.isEmpty()) {
      return DBIDUtil.EMPTYDBIDS;
    }
    // insert into db
    Pair<List<O>, DBIDs> objectsAndIds = doInsert(objectsAndAssociationsList);
    // insert into indexes
    List<O> objects = objectsAndIds.first;
    for(Index<O> index : indexes) {
      index.insert(objects);
    }
    // fire insertion event
    eventManager.fireObjectsInserted(objectsAndIds.first);

    return objectsAndIds.second;
  }

  /**
   * Calls {@link #doInsert(Pair)} and fires an insertion event.
   * 
   * @throws UnableToComplyException if database reached limit of storage
   *         capacity or no transaction has been started
   */
  @Override
  public DBID insert(Pair<O, DatabaseObjectMetadata> objectAndAssociations) throws UnableToComplyException {
    // insert into db
    Pair<O, DBID> objectAndID = doInsert(objectAndAssociations);
    // insert into indexes
    for(Index<O> index : indexes) {
      index.insert(objectAndAssociations.getFirst());
    }
    // fire insertion event
    eventManager.fireObjectInserted(objectAndID.first);

    return objectAndID.second;
  }

  /**
   * Inserts the given object into this database.
   * 
   * @param objectAndAssociations the object and its associations to be inserted
   * @return the ID assigned to the inserted object and the object
   * @throws UnableToComplyException if database reached limit of storage
   *         capacity
   */
  protected Pair<O, DBID> doInsert(Pair<O, DatabaseObjectMetadata> objectAndAssociations) throws UnableToComplyException {
    O object = objectAndAssociations.getFirst();
    if(object == null) {
      throw new UnableToComplyException("Insertion of null objects is not allowed!");
    }

    // insert object
    DBID id = setNewID(object);
    content.put(id, object);
    ids.add(id);
    // insert associations
    DatabaseObjectMetadata associations = objectAndAssociations.getSecond();
    if(associations != null) {
      if(associations.objectlabel != null) {
        setObjectLabel(id, associations.objectlabel);
      }
      if(associations.classlabel != null) {
        setClassLabel(id, associations.classlabel);
      }
      if(associations.externalid != null) {
        setExternalID(id, associations.externalid);
      }
    }

    return new Pair<O, DBID>(object, id);
  }

  /**
   * Convenience method, calls {@link #doInsert(Pair)} for each element.
   * 
   * @param objectsAndAssociationsList
   * @return the IDs assigned to the inserted objects
   * @throws UnableToComplyException
   */
  protected Pair<List<O>, DBIDs> doInsert(List<Pair<O, DatabaseObjectMetadata>> objectsAndAssociationsList) throws UnableToComplyException {
    List<O> objects = new ArrayList<O>(objectsAndAssociationsList.size());
    ModifiableDBIDs ids = DBIDUtil.newArray(objectsAndAssociationsList.size());

    for(Pair<O, DatabaseObjectMetadata> objectAndAssociations : objectsAndAssociationsList) {
      Pair<O, DBID> objectAndID = doInsert(objectAndAssociations);
      objects.add(objectAndID.first);
      ids.add(objectAndID.second);
    }
    return new Pair<List<O>, DBIDs>(objects, ids);
  }

  /**
   * Removes an object from the database and indexes and fires a deletion event.
   * 
   * @throws UnableToComplyException if no transaction has been started
   */
  @Override
  public O delete(DBID id) {
    final O existing;
    try {
      existing = get(id);
    }
    catch(ObjectNotFoundException e) {
      return null;
    }
    // Remove from all indexes.
    for(Index<O> index : indexes) {
      index.delete(existing);
    }

    ids.remove(id);
    content.delete(id);
    if(objectlabels != null) {
      objectlabels.delete(id);
    }
    if(classlabels != null) {
      classlabels.delete(id);
    }
    if(externalids != null) {
      externalids.delete(id);
    }

    restoreID(id);

    // fire deletion event
    eventManager.fireObjectRemoved(existing);

    return existing;
  }

  @Override
  public final int size() {
    return ids.size();
  }

  @Override
  public O getObjectFactory() {
    if(objectFactory == null) {
      throw new UnsupportedOperationException("No object factory / project was added to the database.");
    }
    return objectFactory;
  }

  @Override
  public void setObjectFactory(O objectFactory) {
    this.objectFactory = objectFactory;
  }

  @Override
  public final O get(DBID id) throws ObjectNotFoundException {
    try {
      O ret = content.get(id);
      if(ret == null) {
        throw new ObjectNotFoundException(id);
      }
      return ret;
    }
    catch(RuntimeException e) {
      if(id == null) {
        throw new UnsupportedOperationException("AbstractDatabase.get(null) called!");
      }
      // throw e upwards.
      throw e;
    }
  }

  /**
   * Returns a list of all ids currently in use in the database.
   * 
   * The list is not affected of any changes made to the database in the future
   * nor vice versa.
   * 
   * @see de.lmu.ifi.dbs.elki.database.Database#getIDs()
   */
  @Override
  public DBIDs getIDs() {
    return DBIDUtil.makeUnmodifiable(ids);
  }

  /**
   * Returns an iterator iterating over all keys of the database.
   * 
   * @return an iterator iterating over all keys of the database
   */
  @Override
  public final Iterator<DBID> iterator() {
    return getIDs().iterator();
  }

  @Override
  public ClassLabel getClassLabel(DBID id) {
    if(id == null) {
      LoggingUtil.warning("Trying to get class label for 'null' id.");
      return null;
    }
    if(classlabels == null) {
      return null;
    }
    return classlabels.get(id);
  }

  @Override
  public String getExternalID(DBID id) {
    if(id == null) {
      LoggingUtil.warning("Trying to get external id for 'null' id.");
      return null;
    }
    if(externalids == null) {
      return null;
    }
    return externalids.get(id);
  }

  @Override
  public String getObjectLabel(DBID id) {
    if(id == null) {
      LoggingUtil.warning("Trying to get object label for 'null' id.");
      return null;
    }
    if(objectlabels == null) {
      return null;
    }
    return objectlabels.get(id);
  }

  @Override
  public void setClassLabel(DBID id, ClassLabel label) {
    if(id == null) {
      LoggingUtil.warning("Trying to set class label for 'null' id.");
      return;
    }
    if(classlabels == null) {
      classlabels = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, ClassLabel.class);
      this.addChildResult(new AnnotationBuiltins.ClassLabelAnnotation(this));
    }
    classlabels.put(id, label);
  }

  @Override
  public void setExternalID(DBID id, String externalid) {
    if(id == null) {
      LoggingUtil.warning("Trying to set external id for 'null' id.");
      return;
    }
    if(externalids == null) {
      externalids = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, String.class);
      this.addChildResult(new AnnotationBuiltins.ExternalIDAnnotation(this));
    }
    externalids.put(id, externalid);
  }

  @Override
  public void setObjectLabel(DBID id, String label) {
    if(id == null) {
      LoggingUtil.warning("Trying to set object label for 'null' id.");
      return;
    }
    if(objectlabels == null) {
      objectlabels = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_DB, String.class);
      this.addChildResult(new AnnotationBuiltins.ObjectLabelAnnotation(this));
    }
    objectlabels.put(id, label);
  }

  /**
   * Provides a new id for the specified database object suitable as key for a
   * new insertion and sets this id in the specified database object.
   * 
   * @param object the object for which a new id should be provided
   * @return a new id suitable as key for a new insertion
   * @throws UnableToComplyException if the database has reached the limit and,
   *         therefore, new insertions are not possible
   */
  protected DBID setNewID(O object) throws UnableToComplyException {
    if(object.getID() != null) {
      if(ids.contains(object.getID())) {
        throw new UnableToComplyException("ID " + object.getID() + " is already in use!");
      }
      // TODO: register ID with id manager?
      return object.getID();
    }

    DBID id = DBIDFactory.FACTORY.generateSingleDBID();
    object.setID(id);
    return id;
  }

  /**
   * Makes the given id reusable for new insertion operations.
   * 
   * @param id the id to become reusable
   */
  protected void restoreID(final DBID id) {
    DBIDFactory.FACTORY.deallocateSingleDBID(id);
  }

  @Override
  public Database<O> partition(DBIDs ids) throws UnableToComplyException {
    Map<Integer, DBIDs> partitions = new HashMap<Integer, DBIDs>();
    partitions.put(0, ids);
    return partition(partitions, null, null).get(0);
  }

  @Override
  public Map<Integer, Database<O>> partition(Map<Integer, ? extends DBIDs> partitions) throws UnableToComplyException {
    return partition(partitions, null, null);
  }

  @SuppressWarnings("unchecked")
  @Override
  public Map<Integer, Database<O>> partition(Map<Integer, ? extends DBIDs> partitions, Class<? extends Database<O>> dbClass, Collection<Pair<OptionID, Object>> dbParameters) throws UnableToComplyException {
    if(dbClass == null) {
      dbClass = ClassGenericsUtil.uglyCrossCast(this.getClass(), Database.class);
      dbParameters = getParameters();
    }

    Map<Integer, Database<O>> databases = new Hashtable<Integer, Database<O>>();
    for(Integer partitionID : partitions.keySet()) {
      List<Pair<O, DatabaseObjectMetadata>> objectAndAssociationsList = new ArrayList<Pair<O, DatabaseObjectMetadata>>();
      DBIDs ids = partitions.get(partitionID);
      for(DBID id : ids) {
        final O object = get(id);
        DatabaseObjectMetadata associations = new DatabaseObjectMetadata(this, id);
        objectAndAssociationsList.add(new Pair<O, DatabaseObjectMetadata>(object, associations));
      }

      Database<O> database;
      ListParameterization config = new ListParameterization(dbParameters);
      try {
        database = ClassGenericsUtil.tryInstantiate(Database.class, dbClass, config);
      }
      catch(Exception e) {
        throw new UnableToComplyException(e);
      }

      database.insert(objectAndAssociationsList);
      databases.put(partitionID, database);
    }
    return databases;
  }

  /**
   * Get the parameters, for database cloning/partitioning
   * 
   * @return Parameters for reconfiguration
   */
  protected Collection<Pair<OptionID, Object>> getParameters() {
    return params;
  }

  @Override
  public final DBIDs randomSample(int k, long seed) {
    if(k < 0 || k > this.size()) {
      throw new IllegalArgumentException("Illegal value for size of random sample: " + k);
    }

    ModifiableDBIDs sample = DBIDUtil.newHashSet(k);
    ArrayModifiableDBIDs aids = DBIDUtil.newArray(this.ids);
    Random random = new Random(seed);
    // FIXME: Never sample the same two objects?
    while(sample.size() < k) {
      sample.add(aids.get(random.nextInt(aids.size())));
    }
    return sample;
  }

  @Override
  public int dimensionality() throws UnsupportedOperationException {
    Iterator<DBID> iter = this.iterator();
    if(iter.hasNext()) {
      final O entry = this.get(iter.next());
      if(FeatureVector.class.isInstance(entry)) {
        return ((FeatureVector<?, ?>) entry).getDimensionality();
      }
      else {
        throw new UnsupportedOperationException("Database entries are not implementing interface " + NumberVector.class.getName() + ".");
      }
    }
    else {
      throw new UnsupportedOperationException(ExceptionMessages.DATABASE_EMPTY);
    }
  }

  /**
   * Helper method to extract the list of database objects from the specified
   * list of objects and their associations.
   * 
   * @param objectAndAssociationsList the list of objects and their associations
   * @return the list of database objects
   */
  protected List<O> getObjects(List<Pair<O, DatabaseObjectMetadata>> objectAndAssociationsList) {
    List<O> objects = new ArrayList<O>(objectAndAssociationsList.size());
    for(Pair<O, DatabaseObjectMetadata> objectAndAssociations : objectAndAssociationsList) {
      objects.add(objectAndAssociations.getFirst());
    }
    return objects;
  }

  @Override
  public <D extends Distance<D>> DistanceQuery<O, D> getDistanceQuery(DistanceFunction<? super O, D> distanceFunction) {
    if(distanceFunction == null) {
      throw new AbortException("Distance query requested for 'null' distance!");
    }
    return distanceFunction.instantiate(this);
  }

  @Override
  public <D extends Distance<D>> SimilarityQuery<O, D> getSimilarityQuery(SimilarityFunction<? super O, D> similarityFunction) {
    if(similarityFunction == null) {
      throw new AbortException("Similarity query requested for 'null' similarity!");
    }
    return similarityFunction.instantiate(this);
  }

  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    if(distanceFunction == null) {
      throw new AbortException("kNN query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index<O> idx = indexes.get(i);
      if(idx instanceof KNNIndex) {
        KNNQuery<O, D> q = ((KNNIndex<O>) idx).getKNNQuery(this, distanceFunction, hints);
        if(q != null) {
          return q;
        }
      }
    }
    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    DistanceQuery<O, D> distanceQuery = getDistanceQuery(distanceFunction);
    return new LinearScanKNNQuery<O, D>(this, distanceQuery);
  }

  @Override
  public <D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("kNN query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index<O> idx = indexes.get(i);
      if(idx instanceof KNNIndex) {
        KNNQuery<O, D> q = ((KNNIndex<O>) idx).getKNNQuery(this, distanceQuery, hints);
        if(q != null) {
          return q;
        }
      }
    }
    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    return new LinearScanKNNQuery<O, D>(this, distanceQuery);
  }

  @Override
  public <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    if(distanceFunction == null) {
      throw new AbortException("Range query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index<O> idx = indexes.get(i);
      if(idx instanceof RangeIndex) {
        RangeQuery<O, D> q = ((RangeIndex<O>) idx).getRangeQuery(this, distanceFunction, hints);
        if(q != null) {
          return q;
        }
      }
    }
    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    DistanceQuery<O, D> distanceQuery = getDistanceQuery(distanceFunction);
    return new LinearScanRangeQuery<O, D>(this, distanceQuery);
  }

  @Override
  public <D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("Range query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index<O> idx = indexes.get(i);
      if(idx instanceof RangeIndex) {
        RangeQuery<O, D> q = ((RangeIndex<O>) idx).getRangeQuery(this, distanceQuery, hints);
        if(q != null) {
          return q;
        }
      }
    }
    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    return new LinearScanRangeQuery<O, D>(this, distanceQuery);
  }

  @Override
  public <D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    if(distanceFunction == null) {
      throw new AbortException("RKNN query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index<O> idx = indexes.get(i);
      if(idx instanceof RKNNIndex) {
        RKNNQuery<O, D> q = ((RKNNIndex<O>) idx).getRKNNQuery(this, distanceFunction, hints);
        if(q != null) {
          return q;
        }
      }
    }
    Integer maxk = null;
    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
      if(hint instanceof Integer) {
        maxk = (Integer) hint;
      }
    }
    DistanceQuery<O, D> distanceQuery = getDistanceQuery(distanceFunction);
    return new LinearScanRKNNQuery<O, D>(this, distanceQuery, maxk);
  }

  @Override
  public <D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("RKNN query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index<O> idx = indexes.get(i);
      if(idx instanceof RKNNIndex) {
        RKNNQuery<O, D> q = ((RKNNIndex<O>) idx).getRKNNQuery(this, distanceQuery, hints);
        if(q != null) {
          return q;
        }
      }
    }
    Integer maxk = null;
    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
      if(hint instanceof Integer) {
        maxk = (Integer) hint;
      }
    }
    return new LinearScanRKNNQuery<O, D>(this, distanceQuery, maxk);
  }

  @Override
  public void addDataStoreListener(DataStoreListener<O> l) {
    eventManager.addListener(l);
  }

  @Override
  public void removeDataStoreListener(DataStoreListener<O> l) {
    eventManager.removeListener(l);
  }

  /*@Override
  public void addResultListener(ResultListener l) {
    eventManager.addListener(l);
  }*/

  /*@Override
  public void removeResultListener(ResultListener l) {
    eventManager.removeListener(l);
  }*/

  @Override
  public void reportPageAccesses(Logging logger) {
    if(logger.isVerbose() && indexes.size() > 0) {
      StringBuffer msg = new StringBuffer();
      for(Index<O> index : indexes) {
        PageFileStatistics pf = index.getPageFileStatistics();
        if(pf != null) {
          msg.append(getClass().getName()).append(" physical read access : ").append(pf.getPhysicalReadAccess()).append("\n");
          msg.append(getClass().getName()).append(" physical write access : ").append(pf.getPhysicalWriteAccess()).append("\n");
          msg.append(getClass().getName()).append(" logical page access : ").append(pf.getLogicalPageAccess()).append("\n");
        }
      }
      logger.verbose(msg.toString());
    }
  }

  /*@Override
  public Collection<Result> getPrimary() {
    return Collections.unmodifiableCollection(primaryResults);
  }*/

  /*@Override
  public Collection<Result> getDerived() {
    return Collections.unmodifiableCollection(derivedResults);
  }*/

  /*@Override
  public void addDerivedResult(Result r) {
    if(r == null) {
      LoggingUtil.warning("Null result added.", new Throwable());
      return;
    }
    r.addResultListener(this);
    derivedResults.add(r);
    eventManager.fireResultAdded(r, this);
  }

  @Override
  public void resultAdded(Result r, Result parent) {
    eventManager.fireResultAdded(r, parent);
  }

  @Override
  public void resultRemoved(Result r, Result parent) {
    eventManager.fireResultRemoved(r, parent);
  }*/

  @Override
  public String getLongName() {
    return "Database";
  }

  @Override
  public String getShortName() {
    return "database";
  }

  @Override
  public void accumulateDataStoreEvents() {
    eventManager.accumulateDataStoreEvents();
  }

  @Override
  public void flushDataStoreEvents() {
    eventManager.flushDataStoreEvents();
  }
}