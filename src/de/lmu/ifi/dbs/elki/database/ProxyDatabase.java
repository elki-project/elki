package de.lmu.ifi.dbs.elki.database;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.type.NoSupportedDataTypeException;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
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
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.bundle.SingleObjectBundle;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.SimilarityFunction;
import de.lmu.ifi.dbs.elki.index.Index;
import de.lmu.ifi.dbs.elki.result.AbstractHierarchicalResult;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ObjectNotFoundException;

/**
 * A proxy database to use e.g. for projections and partitions.
 * 
 * @author Erich Schubert
 */
// FIXME: allow indexes (preprocessors!), allow events and listeners
// FIXME: add an auto-proxy option to proxy all existing relations.
public class ProxyDatabase extends AbstractHierarchicalResult implements Database {
  /**
   * Our DBIDs
   */
  final protected DBIDs ids;

  /**
   * The representations we have.
   */
  final protected List<Relation<?>> relations;

  /**
   * Our DBID representation
   */
  final protected DBIDView idrep;

  /**
   * Constructor.
   * 
   * @param ids DBIDs to use
   */
  public ProxyDatabase(DBIDs ids) {
    super();
    this.ids = ids;
    this.relations = new java.util.Vector<Relation<?>>();
    this.idrep = new DBIDView(this, this.ids);
    this.relations.add(idrep);
    this.addChildResult(idrep);
  }
  
  @Override
  public void initialize() {
    // Nothing to do - we were initialized on construction time.
  }

  /**
   * Add a new representation.
   * 
   * @param relation Representation to add.
   */
  public void addRelation(Relation<?> relation) {
    this.relations.add(relation);
  }

  @Override
  public String getLongName() {
    return "Proxy database";
  }

  @Override
  public String getShortName() {
    return "Proxy database";
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public int size() {
    return ids.size();
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
    /*
     * FIXME: re-add index support for(int i = indexes.size() - 1; i >= 0; i--)
     * { Index<?> idx = indexes.get(i); if(idx instanceof KNNIndex) {
     * KNNQuery<O, D> q = ((KNNIndex<O>) idx).getKNNQuery(objQuery,
     * distanceFunction, hints); if(q != null) { return q; } } }
     */
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
    /*
     * FIXME: re-add index support for(int i = indexes.size() - 1; i >= 0; i--)
     * { Index<?> idx = indexes.get(i); if(idx instanceof KNNIndex) {
     * KNNQuery<O, D> q = ((KNNIndex<O>) idx).getKNNQuery(distanceQuery, hints);
     * if(q != null) { return q; } } }
     */
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
    /*
     * FIXME: re-add index support for(int i = indexes.size() - 1; i >= 0; i--)
     * { Index<?> idx = indexes.get(i); if(idx instanceof RangeIndex) {
     * RangeQuery<O, D> q = ((RangeIndex<O>) idx).getRangeQuery(this,
     * distanceFunction, hints); if(q != null) { return q; } } }
     */
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
    /*
     * FIXME: re-add index support for(int i = indexes.size() - 1; i >= 0; i--)
     * { Index<?> idx = indexes.get(i); if(idx instanceof RangeIndex) {
     * RangeQuery<O, D> q = ((RangeIndex<O>) idx).getRangeQuery(this,
     * distanceQuery, hints); if(q != null) { return q; } } }
     */
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
    /*
     * FIXME: re-add index support for(int i = indexes.size() - 1; i >= 0; i--)
     * { Index<?> idx = indexes.get(i); if(idx instanceof RKNNIndex) {
     * RKNNQuery<O, D> q = ((RKNNIndex<O>) idx).getRKNNQuery(this,
     * distanceFunction, hints); if(q != null) { return q; } } }
     */
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
    /*
     * FIXME: re-add index support for(int i = indexes.size() - 1; i >= 0; i--)
     * { Index<?> idx = indexes.get(i); if(idx instanceof RKNNIndex) {
     * RKNNQuery<O, D> q = ((RKNNIndex<O>) idx).getRKNNQuery(this,
     * distanceQuery, hints); if(q != null) { return q; } } }
     */
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

  @SuppressWarnings("unused")
  @Override
  public SingleObjectBundle getBundle(DBID id) throws ObjectNotFoundException {
    throw new UnsupportedOperationException("FIXME: Proxy databases currently do not yet allow retrieving object packages.");
  }

  @SuppressWarnings("deprecation")
  @Deprecated
  @Override
  public DBIDs getDBIDs() {
    return DBIDUtil.makeUnmodifiable(ids);
  }

  @Override
  public void addIndex(Index index) {
    throw new UnsupportedOperationException("FIXME: Proxy databases currently do not yet allow indexes.");
  }
  
  @Override
  public Collection<Index> getIndexes() {
    final List<Index> indexes = Collections.emptyList();
    return indexes;
  }

  @Override
  public void removeIndex(Index index) {
    throw new UnsupportedOperationException("FIXME: Proxy databases currently do not yet allow indexes.");
  }

  @SuppressWarnings("unused")
  @Override
  public void addDataStoreListener(DataStoreListener l) {
    throw new UnsupportedOperationException("FIXME: Proxy databases currently do not yet allow listeners.");
  }

  @SuppressWarnings("unused")
  @Override
  public void removeDataStoreListener(DataStoreListener l) {
    throw new UnsupportedOperationException("FIXME: Proxy databases currently do not yet allow listeners.");
  }

  @Override
  public void accumulateDataStoreEvents() {
    // FIXME: implement
  }

  @Override
  public void flushDataStoreEvents() {
    // FIXME: implement
  }
}