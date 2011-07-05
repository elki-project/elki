package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.mkcop;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.query.rknn.RKNNQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.distance.distancevalue.NumberDistance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RKNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MetricalIndexKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MetricalIndexRangeQuery;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MkTreeRKNNQuery;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * MkCoPTree used as database index.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MkCoPTreeIndex<O, D extends NumberDistance<D, ?>> extends MkCoPTree<O, D> implements RangeIndex<O>, KNNIndex<O>, RKNNIndex<O> {
  /**
   * Relation indexed
   */
  private Relation<O> relation;

  /**
   * Constructor.
   * 
   * @param relation Relation to index.
   * @param pageFile Page file
   * @param distanceQuery Distance query
   * @param distanceFunction Distance function
   * @param k_max Maximum value of k supported
   */
  public MkCoPTreeIndex(Relation<O> relation, PageFile<MkCoPTreeNode<O, D>> pageFile, DistanceQuery<O, D> distanceQuery, DistanceFunction<O, D> distanceFunction, int k_max) {
    super(pageFile, distanceQuery, distanceFunction, k_max);
    this.relation = relation;
    this.initialize();
  }

  /**
   * Creates a new leaf entry representing the specified data object in the
   * specified subtree.
   * 
   * @param object the data object to be represented by the new entry
   * @param parentDistance the distance from the object to the routing object of
   *        the parent node
   */
  protected MkCoPEntry<D> createNewLeafEntry(DBID id, O object, D parentDistance) {
    MkCoPLeafEntry<D> leafEntry = new MkCoPLeafEntry<D>(id, parentDistance, null, null);
    return leafEntry;
  }

  @SuppressWarnings("unused")
  @Override
  public void insert(DBID id) {
    throw new UnsupportedOperationException("Insertion of single objects is not supported!");
  }

  @Override
  public void insertAll(DBIDs ids) {
    List<MkCoPEntry<D>> objs = new ArrayList<MkCoPEntry<D>>(ids.size());
    for(DBID id : ids) {
      final O object = relation.get(id);
      objs.add(createNewLeafEntry(id, object, getDistanceFactory().undefinedDistance()));
    }
    insertAll(objs);
  }

  /**
   * Throws an UnsupportedOperationException since deletion of objects is not
   * yet supported by an M-Tree.
   * 
   * @throws UnsupportedOperationException thrown, since deletions aren't
   *         implemented yet.
   */
  @SuppressWarnings("unused")
  @Override
  public final boolean delete(DBID id) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  /**
   * Throws an UnsupportedOperationException since deletion of objects is not
   * yet supported by an M-Tree.
   * 
   * @throws UnsupportedOperationException thrown, since deletions aren't
   *         implemented yet.
   */
  @SuppressWarnings("unused")
  @Override
  public void deleteAll(DBIDs ids) {
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> KNNQuery<O, S> getKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O, S> distanceFunction = distanceQuery.getDistanceFunction();
    if(!this.distanceFunction.equals(distanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    AbstractMTree<O, S, ?, ?> idx = (AbstractMTree<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = distanceFunction.instantiate(relation);
    return new MetricalIndexKNNQuery<O, S>(idx, dq);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RangeQuery<O, S> getRangeQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    // Query on the relation we index
    if(distanceQuery.getRelation() != relation) {
      return null;
    }
    DistanceFunction<? super O, S> distanceFunction = distanceQuery.getDistanceFunction();
    if(!this.distanceFunction.equals(distanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    AbstractMTree<O, S, ?, ?> idx = (AbstractMTree<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = distanceFunction.instantiate(relation);
    return new MetricalIndexRangeQuery<O, S>(idx, dq);
  }

  @SuppressWarnings("unchecked")
  @Override
  public <S extends Distance<S>> RKNNQuery<O, S> getRKNNQuery(DistanceQuery<O, S> distanceQuery, Object... hints) {
    DistanceFunction<? super O, S> distanceFunction = distanceQuery.getDistanceFunction();
    if(!this.getDistanceFunction().equals(distanceFunction)) {
      if(getLogger().isDebugging()) {
        getLogger().debug("Distance function not supported by index - or 'equals' not implemented right!");
      }
      return null;
    }
    // Bulk is not yet supported
    for(Object hint : hints) {
      if(hint == DatabaseQuery.HINT_BULK) {
        return null;
      }
    }
    AbstractMkTree<O, S, ?, ?> idx = (AbstractMkTree<O, S, ?, ?>) this;
    DistanceQuery<O, S> dq = distanceFunction.instantiate(relation);
    return new MkTreeRKNNQuery<O, S>(idx, dq);
  }

  @Override
  public String getLongName() {
    return "MkCoP-Tree";
  }

  @Override
  public String getShortName() {
    return "mkcoptree";
  }
}