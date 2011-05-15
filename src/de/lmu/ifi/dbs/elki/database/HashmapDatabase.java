package de.lmu.ifi.dbs.elki.database;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDFactory;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.TreeSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanPrimitiveDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanRawDoubleDistanceKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.LinearScanKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.LinearScanPrimitiveDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.LinearScanRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.LinearScanRawDoubleDistanceRangeQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.LinearScanRKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.query.similarity.SimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.DBIDView;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.ObjectBundle;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.index.IndexFactory;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.documentation.Description;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectListParameter;

/**
 * Provides a mapping for associations based on a Hashtable and functions to get
 * the next usable ID for insertion, making IDs reusable after deletion of the
 * entry.
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
public class HashmapDatabase extends AbstractHierarchicalResult implements Database, Parameterizable {
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
  private TreeSetModifiableDBIDs ids;

  /**
   * The event manager, collects events and fires them on demand.
   */
  protected final DatabaseEventManager eventManager = new DatabaseEventManager();

  /**
   * The DBID representation we use
   */
  private final DBIDView idrep;

  /**
   * Indexes
   */
  final List<Index> indexes;

  /**
   * Index factories
   */
  final Collection<IndexFactory<?, ?>> indexFactories;

  /**
   * Constructor.
   * 
   * @param indexFactories Indexes to add
   */
  public HashmapDatabase(Collection<IndexFactory<?, ?>> indexFactories) {
    super();
    this.ids = DBIDUtil.newTreeSet();
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
  public HashmapDatabase() {
    this(null);
  }

  @Override
  public void addIndex(Index index) {
    this.indexes.add(index);
    // TODO: actually add index to the representation used?
    this.addChildResult(index);
  }

  @Override
  public DBIDs insert(MultipleObjectsBundle objpackages) {
    if(objpackages.dataLength() == 0) {
      return DBIDUtil.EMPTYDBIDS;
    }
    // insert into db
    ArrayModifiableDBIDs newids = DBIDUtil.newArray(objpackages.dataLength());
    Relation<?>[] targets = alignColumns(objpackages);

    int idrepnr = -1;
    for(int i = 0; i < targets.length; i++) {
      if(targets[i] == idrep) {
        idrepnr = i;
        break;
      }
    }

    for(int j = 0; j < objpackages.dataLength(); j++) {
      // insert object
      final DBID newid;
      if(idrepnr < 0) {
        newid = DBIDUtil.generateSingleDBID();
      }
      else {
        newid = (DBID) objpackages.data(j, idrepnr);
      }
      if(ids.contains(newid)) {
        throw new AbortException("Duplicate DBID conflict.");
      }
      ids.add(newid);
      for(int i = 0; i < targets.length; i++) {
        // DBIDs were handled above.
        if(i == idrepnr) {
          continue;
        }
        @SuppressWarnings("unchecked")
        final Relation<Object> relation = (Relation<Object>) targets[i];
        relation.set(newid, objpackages.data(j, i));
      }
      newids.add(newid);

      // Notify indexes of insertions
      // FIXME: support bulk...
      for(Index index : indexes) {
        for(int i = 0; i < targets.length; i++) {
          if(index.getRelation() == targets[i]) {
            index.insert(newid);
          }
        }
      }
    }

    // fire insertion event
    eventManager.fireObjectsInserted(newids);
    return newids;
  }

  @Override
  public DBID insert(SingleObjectBundle objpackage) {
    Relation<?>[] targets = alignColumns(objpackage);

    DBID newid = null;
    for(int i = 0; i < targets.length; i++) {
      if(targets[i] == idrep) {
        newid = (DBID) objpackage.data(i);
        break;
      }
    }
    if(newid == null) {
      newid = DBIDUtil.generateSingleDBID();
    }

    if(ids.contains(newid)) {
      throw new AbortException("Duplicate DBID conflict.");
    }
    ids.add(newid);

    // insert object into representations
    for(int i = 0; i < targets.length; i++) {
      @SuppressWarnings("unchecked")
      final Relation<Object> relation = (Relation<Object>) targets[i];
      // Skip the DBID we used above
      if((Object) relation == idrep) {
        continue;
      }
      relation.set(newid, objpackage.data(i));
    }

    // insert into indexes
    for(Index index : indexes) {
      for(int i = 0; i < targets.length; i++) {
        if(index.getRelation() == targets[i]) {
          index.insert(newid);
        }
      }
    }

    // fire insertion event
    eventManager.fireObjectInserted(newid);
    return newid;
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
    {
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
    // Try to add indexes where appropriate
    for(IndexFactory<?, ?> factory : indexFactories) {
      if(factory.getInputTypeRestriction().isAssignableFromType(meta)) {
        @SuppressWarnings("unchecked")
        final IndexFactory<Object, ?> ofact = (IndexFactory<Object, ?>) factory;
        @SuppressWarnings("unchecked")
        final Relation<Object> orep = (Relation<Object>) relation;
        addIndex(ofact.instantiate(orep));
      }
    }
    return relation;
  }

  /**
   * Removes the objects from the database (by calling {@link #doDelete(DBID)})
   * and from all indexes and fires a deletion event.
   */
  @Override
  public SingleObjectBundle delete(DBID id) {
    final SingleObjectBundle existing;
    try {
      existing = getBundle(id);
    }
    catch(ObjectNotFoundException e) {
      return null;
    }
    // remove from all indexes
    for(Index index : indexes) {
      index.delete(id);
    }
    // remove from db
    doDelete(id);
    // fire deletion event
    eventManager.fireObjectRemoved(id);
    return existing;
  }

  /**
   * Removes the objects from the database (by calling {@link #doDelete(DBID)}
   * for each object) and indexes and fires a deletion event.
   */
  @Override
  public List<SingleObjectBundle> delete(DBIDs ids) {
    final List<SingleObjectBundle> existing = new ArrayList<SingleObjectBundle>(ids.size());
    for(DBID id : ids) {
      try {
        existing.add(getBundle(id));
      }
      catch(ObjectNotFoundException e) {
        // do nothing?
      }
    }
    // remove from db
    for(DBID id : ids) {
      doDelete(id);
    }
    // Remove from indexes
    for(Index index : indexes) {
      index.deleteAll(ids);
    }
    // fire deletion event
    eventManager.fireObjectsRemoved(ids);

    return existing;
  }

  /**
   * Removes the object with the specified id from this database.
   * 
   * @param id id the id of the object to be removed
   */
  private void doDelete(DBID id) {
    // Remove id
    ids.remove(id);
    // Remove from all representations.
    for(Relation<?> relation : relations) {
      // ID has already been removed, and this would loop...
      if(relation != idrep) {
        relation.delete(id);
      }
    }
    restoreID(id);
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

  /**
   * Makes the given id reusable for new insertion operations.
   * 
   * @param id the id to become reusable
   */
  protected void restoreID(final DBID id) {
    DBIDFactory.FACTORY.deallocateSingleDBID(id);
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
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index idx = indexes.get(i);
      if(idx instanceof KNNIndex) {
        if(idx.getRelation() == objQuery) {
          KNNQuery<O, D> q = ((KNNIndex<O>) idx).getKNNQuery(distanceFunction, hints);
          if(q != null) {
            return q;
          }
        }
      }
    }

    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    DistanceQuery<O, D> distanceQuery = getDistanceQuery(objQuery, distanceFunction);
    return QueryUtil.getLinearScanKNNQuery(distanceQuery);
  }

  @Override
  public <O, D extends Distance<D>> KNNQuery<O, D> getKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("kNN query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index idx = indexes.get(i);
      if(idx instanceof KNNIndex) {
        if(idx.getRelation() == distanceQuery.getRelation()) {
          KNNQuery<O, D> q = ((KNNIndex<O>) idx).getKNNQuery(distanceQuery, hints);
          if(q != null) {
            return q;
          }
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
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index idx = indexes.get(i);
      if(idx instanceof RangeIndex) {
        if(idx.getRelation() == objQuery) {
          RangeQuery<O, D> q = ((RangeIndex<O>) idx).getRangeQuery(distanceFunction, hints);
          if(q != null) {
            return q;
          }
        }
      }
    }

    // Default
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_OPTIMIZED_ONLY) {
        return null;
      }
    }
    DistanceQuery<O, D> distanceQuery = getDistanceQuery(objQuery, distanceFunction);
    return QueryUtil.getLinearScanRangeQuery(distanceQuery);
  }

  @Override
  public <O, D extends Distance<D>> RangeQuery<O, D> getRangeQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("Range query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index idx = indexes.get(i);
      if(idx instanceof RangeIndex) {
        if(idx.getRelation() == distanceQuery.getRelation()) {
          RangeQuery<O, D> q = ((RangeIndex<O>) idx).getRangeQuery(distanceQuery, hints);
          if(q != null) {
            return q;
          }
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
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index idx = indexes.get(i);
      if(idx instanceof RKNNIndex) {
        if(idx.getRelation() == objQuery) {
          RKNNQuery<O, D> q = ((RKNNIndex<O>) idx).getRKNNQuery(distanceFunction, hints);
          if(q != null) {
            return q;
          }
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
    DistanceQuery<O, D> distanceQuery = getDistanceQuery(objQuery, distanceFunction);
    KNNQuery<O, D> knnQuery = getKNNQuery(distanceQuery, DatabaseQuery.HINT_BULK, maxk);
    return new LinearScanRKNNQuery<O, D>(objQuery, distanceQuery, knnQuery, maxk);
  }

  @Override
  public <O, D extends Distance<D>> RKNNQuery<O, D> getRKNNQuery(DistanceQuery<O, D> distanceQuery, Object... hints) {
    if(distanceQuery == null) {
      throw new AbortException("RKNN query requested for 'null' distance!");
    }
    for(int i = indexes.size() - 1; i >= 0; i--) {
      Index idx = indexes.get(i);
      if(idx instanceof RKNNIndex) {
        if(idx.getRelation() == distanceQuery.getRelation()) {
          RKNNQuery<O, D> q = ((RKNNIndex<O>) idx).getRKNNQuery(distanceQuery, hints);
          if(q != null) {
            return q;
          }
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
     * Indexes to add.
     */
    private Collection<IndexFactory<?, ?>> indexFactories;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Get indexes.
      final ObjectListParameter<IndexFactory<?, ?>> indexFactoryP = new ObjectListParameter<IndexFactory<?, ?>>(INDEX_ID, IndexFactory.class, true);
      if(config.grab(indexFactoryP)) {
        indexFactories = indexFactoryP.instantiateClasses(config);
      }
    }

    @Override
    protected HashmapDatabase makeInstance() {
      return new HashmapDatabase(indexFactories);
    }
  }
}