package de.lmu.ifi.dbs.elki.database;

import java.util.BitSet;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.LinearScanRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.DBIDView;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.DatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.ObjectBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * This database class uses array-based storage and thus does not allow for
 * dynamic insert, delete and update operations. However, array access is
 * expected to be faster and use less memory.
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.composedOf DatabaseEventManager
 * @apiviz.composedOf WritableDataStore
 * @apiviz.composedOf Index
 * @apiviz.composedOf DBIDs
 */
@Description("Database using an in-memory hashtable and at least providing linear scans.")
public class StaticArrayDatabase extends AbstractHierarchicalResult implements Database, Parameterizable {
  /**
   * Our logger
   */
  private static final Logging logger = Logging.getLogger(StaticArrayDatabase.class);

  /**
   * Parameter to specify the indexes to use.
   * <p>
   * Key: {@code -db.index}
   * </p>
   */
  public static final OptionID INDEX_ID = OptionID.getOrCreateOptionID("db.index", "Database indexes to add.");

  /**
   * The representations we have.
   */
  protected final List<Relation<?>> relations;

  /**
   * IDs of this database
   */
  private ArrayDBIDs ids;

  /**
   * The event manager, collects events and fires them on demand.
   */
  protected final DatabaseEventManager eventManager = new DatabaseEventManager();

  /**
   * The DBID representation we use
   */
  private DBIDView idrep;

  /**
   * Indexes
   */
  final List<Index> indexes;

  /**
   * Index factories
   */
  final Collection<IndexFactory<?, ?>> indexFactories;

  /**
   * The data source we get the initial data from.
   */
  protected DatabaseConnection databaseConnection;

  /**
   * Constructor.
   * 
   * @param databaseConnection Database connection to get the initial data from.
   * @param indexFactories Indexes to add
   */
  public StaticArrayDatabase(DatabaseConnection databaseConnection, Collection<IndexFactory<?, ?>> indexFactories) {
    super();
    this.databaseConnection = databaseConnection;
    this.ids = DBIDUtil.EMPTYDBIDS;
    this.relations = new java.util.Vector<Relation<?>>();
    this.idrep = new DBIDView(this, this.ids);
    this.relations.add(idrep);
    this.addChildResult(idrep);
    this.indexes = new java.util.Vector<Index>();

    // Add indexes.
    this.indexFactories = new java.util.Vector<IndexFactory<?, ?>>();
    if(indexFactories != null) {
      this.indexFactories.addAll(indexFactories);
    }
  }

  /**
   * Constructor with no indexes.
   */
  public StaticArrayDatabase() {
    this(null, null);
  }

  /**
   * Initialize the database by getting the initial data from the database
   * connection.
   */
  @Override
  public void initialize() {
    if(databaseConnection != null) {
      if(logger.isDebugging()) {
        logger.debugFine("Loading data from database connection.");
      }
      MultipleObjectsBundle objpackages = databaseConnection.loadData();
      // Run at most once.
      databaseConnection = null;

      // Find DBID column
      int idrepnr = findDBIDColumn(objpackages);
      // Build DBID array
      if(idrepnr == -1) {
        ids = DBIDUtil.generateStaticDBIDRange(objpackages.dataLength());
      }
      else {
        final ArrayModifiableDBIDs newids = DBIDUtil.newArray(objpackages.dataLength());
        for(int j = 0; j < objpackages.dataLength(); j++) {
          DBID newid = (DBID) objpackages.data(j, idrepnr);
          newids.add(newid);
        }
        ids = newids;
      }
      // Replace id representation.
      // TODO: this is an ugly hack
      idrep = new DBIDView(this, this.ids);
      relations.set(0, idrep);

      // insert into db - note: DBIDs should have been prepared before this!
      Relation<?>[] targets = alignColumns(objpackages);

      for(int j = 0; j < objpackages.dataLength(); j++) {
        // insert object
        final DBID newid = ids.get(j);
        for(int i = 0; i < targets.length; i++) {
          // DBIDs were handled above.
          if(i == idrepnr) {
            continue;
          }
          @SuppressWarnings("unchecked")
          final Relation<Object> relation = (Relation<Object>) targets[i];
          relation.set(newid, objpackages.data(j, i));
        }
      }

      for(Relation<?> relation : relations) {
        SimpleTypeInformation<?> meta = relation.getDataTypeInformation();
        // Try to add indexes where appropriate
        for(IndexFactory<?, ?> factory : indexFactories) {
          if(factory.getInputTypeRestriction().isAssignableFromType(meta)) {
            @SuppressWarnings("unchecked")
            final IndexFactory<Object, ?> ofact = (IndexFactory<Object, ?>) factory;
            @SuppressWarnings("unchecked")
            final Relation<Object> orep = (Relation<Object>) relation;
            final Index index = ofact.instantiate(orep);
            addIndex(index);
            index.insertAll(ids);
          }
        }
      }

      // fire insertion event
      eventManager.fireObjectsInserted(ids);
    }
  }

  @Override
  public void addIndex(Index index) {
    this.indexes.add(index);
    // TODO: actually add index to the representation used?
    this.addChildResult(index);
  }

  @Override
  public Collection<Index> getIndexes() {
    return Collections.unmodifiableList(this.indexes);
  }

  @Override
  public void removeIndex(Index index) {
    this.indexes.remove(index);
    this.getHierarchy().remove(this, index);
  }

  /**
   * Find an DBID column.
   * 
   * @param pack Package to process
   * @return DBID column
   */
  protected int findDBIDColumn(ObjectBundle pack) {
    for(int i = 0; i < pack.metaLength(); i++) {
      SimpleTypeInformation<?> meta = pack.meta(i);
      if(TypeUtil.DBID.isAssignableFromType(meta)) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Find a mapping from package columns to database columns, eventually adding
   * new database columns when needed.
   * 
   * @param pack Package to process
   * @return Column mapping
   */
  protected Relation<?>[] alignColumns(ObjectBundle pack) {
    // align representations.
    Relation<?>[] targets = new Relation<?>[pack.metaLength()];
    BitSet used = new BitSet(relations.size());
    for(int i = 0; i < targets.length; i++) {
      SimpleTypeInformation<?> meta = pack.meta(i);
      // TODO: aggressively try to match exact metas first?
      // Try to match unused representations only
      for(int j = used.nextClearBit(0); j >= 0 && j < relations.size(); j = used.nextClearBit(j + 1)) {
        Relation<?> relation = relations.get(j);
        if(relation.getDataTypeInformation().isAssignableFromType(meta)) {
          targets[i] = relation;
          used.set(j);
          break;
        }
      }
      if(targets[i] == null) {
        targets[i] = addNewRepresentation(meta);
        used.set(relations.size() - 1);
      }
    }
    return targets;
  }

  /**
   * Add a new representation for the given meta.
   * 
   * @param meta meta data
   * @return new representation
   */
  private Relation<?> addNewRepresentation(SimpleTypeInformation<?> meta) {
    @SuppressWarnings("unchecked")
    SimpleTypeInformation<Object> ometa = (SimpleTypeInformation<Object>) meta;
    Relation<?> relation = new MaterializedRelation<Object>(this, ometa, ids);
    relations.add(relation);
    getHierarchy().add(this, relation);
    return relation;
  }

  @Override
  public final int size() {
    return ids.size();
  }

  @Override
  public final SingleObjectBundle getBundle(DBID id) throws ObjectNotFoundException {
    assert (id != null);
    assert (ids.contains(id));
    try {
      // Build an object package
      SingleObjectBundle ret = new SingleObjectBundle();
      for(Relation<?> relation : relations) {
        ret.append(relation.getDataTypeInformation(), relation.get(id));
      }
      return ret;
    }
    catch(RuntimeException e) {
      if(id == null) {
        throw new UnsupportedOperationException("AbstractDatabase.getPackage(null) called!");
      }
      if(!ids.contains(id)) {
        throw new UnsupportedOperationException("AbstractDatabase.getPackage() called for unknown id!");
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
   * @see de.lmu.ifi.dbs.elki.database.Database#getDBIDs()
   */
  @Override
  public DBIDs getDBIDs() {
    return DBIDUtil.makeUnmodifiable(ids);
  }

  @SuppressWarnings({ "unchecked", "unused" })
  @Override
  public <O> Relation<O> getRelation(TypeInformation restriction, Object... hints) throws NoSupportedDataTypeException {
    // Get first match
    for(Relation<?> relation : relations) {
      if(restriction.isAssignableFromType(relation.getDataTypeInformation())) {
        return (Relation<O>) relation;
      }
    }
    throw new NoSupportedDataTypeException(restriction);
  }

  @SuppressWarnings("unused")
  @Override
  public <O, D extends Distance<D>> DistanceQuery<O, D> getDistanceQuery(Relation<O> objQuery, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    if(distanceFunction == null) {
      throw new AbortException("Distance query requested for 'null' distance!");
    }
    return distanceFunction.instantiate(objQuery);
  }

  @SuppressWarnings("unused")
  @Override
  public <O, D extends Distance<D>> SimilarityQuery<O, D> getSimilarityQuery(Relation<O> objQuery, SimilarityFunction<? super O, D> similarityFunction, Object... hints) {
    if(similarityFunction == null) {
      throw new AbortException("Similarity query requested for 'null' similarity!");
    }
    return similarityFunction.instantiate(objQuery);
  }

  @Override
  public <O, D extends Distance<D>> KNNQuery<O, D> getKNNQuery(Relation<O> objQuery, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    if(distanceFunction == null) {
      throw new AbortException("kNN query requested for 'null' distance!");
    }
    DistanceQuery<O, D> distanceQuery = getDistanceQuery(objQuery, distanceFunction);
    return getKNNQuery(distanceQuery, hints);
  }

  @Override
  public <O, D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("kNN query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index idx = indexes.get(i);
      if(idx instanceof KNNIndex) {
        KNNQuery<O, D> q = ((KNNIndex<O>) idx).getKNNQuery(distanceQuery, hints);
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
    return QueryUtil.getLinearScanKNNQuery(distanceQuery);
  }

  @Override
  public <O, D extends Distance<D>> RangeQuery<O, D> getRangeQuery(Relation<O> objQuery, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    if(distanceFunction == null) {
      throw new AbortException("Range query requested for 'null' distance!");
    }
    DistanceQuery<O, D> distanceQuery = getDistanceQuery(objQuery, distanceFunction);
    return getRangeQuery(distanceQuery, hints);
  }

  @Override
  public <O, D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("Range query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index idx = indexes.get(i);
      if(idx instanceof RangeIndex) {
        RangeQuery<O, D> q = ((RangeIndex<O>) idx).getRangeQuery(distanceQuery, hints);
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
    return QueryUtil.getLinearScanRangeQuery(distanceQuery);
  }

  @Override
  public <O, D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(Relation<O> objQuery, DistanceFunction<? super O, D> distanceFunction, Object... hints) {
    if(distanceFunction == null) {
      throw new AbortException("RKNN query requested for 'null' distance!");
    }
    DistanceQuery<O, D> distanceQuery = getDistanceQuery(objQuery, distanceFunction);
    return getRKNNQuery(distanceQuery, hints);
  }

  @Override
  public <O, D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("RKNN query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index idx = indexes.get(i);
      if(idx instanceof RKNNIndex) {
        RKNNQuery<O, D> q = ((RKNNIndex<O>) idx).getRKNNQuery(distanceQuery, hints);
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
      if(hint instanceof Integer && maxk == null) {
        maxk = (Integer) hint;
      }
    }
    KNNQuery<O, D> knnQuery = getKNNQuery(distanceQuery, DatabaseQuery.HINT_BULK, maxk);
    return new LinearScanRKNNQuery<O, D>(distanceQuery.getRelation(), distanceQuery, knnQuery, maxk);
  }

  @Override
  public void addDataStoreListener(DataStoreListener l) {
    eventManager.addListener(l);
  }

  @Override
  public void removeDataStoreListener(DataStoreListener l) {
    eventManager.removeListener(l);
  }

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

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    /**
     * Holds the database connection to get the initial data from.
     */
    protected DatabaseConnection databaseConnection = null;

    /**
     * Indexes to add.
     */
    private Collection<IndexFactory<?, ?>> indexFactories;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Get database connection.
      final ObjectParameter<DatabaseConnection> dbcP = new ObjectParameter<DatabaseConnection>(OptionID.DATABASE_CONNECTION, DatabaseConnection.class, FileBasedDatabaseConnection.class);
      if(config.grab(dbcP)) {
        databaseConnection = dbcP.instantiateClass(config);
      }
      // Get indexes.
      final ObjectListParameter<IndexFactory<?, ?>> indexFactoryP = new ObjectListParameter<IndexFactory<?, ?>>(INDEX_ID, IndexFactory.class, true);
      if(config.grab(indexFactoryP)) {
        indexFactories = indexFactoryP.instantiateClasses(config);
      }
    }

    @Override
    protected StaticArrayDatabase makeInstance() {
      return new StaticArrayDatabase(databaseConnection, indexFactories);
    }
  }
}