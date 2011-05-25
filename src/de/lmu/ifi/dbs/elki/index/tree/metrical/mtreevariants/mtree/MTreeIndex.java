package de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mtree;

import java.util.ArrayList;
import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.DatabaseQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.KNNIndex;
import de.lmu.ifi.dbs.elki.index.RangeIndex;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.AbstractMTree;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.MTreeLeafEntry;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MetricalIndexKNNQuery;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.query.MetricalIndexRangeQuery;
import de.lmu.ifi.dbs.elki.persistent.PageFile;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Class for using an m-tree as database index.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 * @param <D> Distance type
 */
public class MTreeIndex<O, D extends Distance<D>> extends MTree<O, D> implements RangeIndex<O>, KNNIndex<O> {
  /**
   * The relation indexed.
   */
  private Relation<O> relation;

  /**
   * Constructor.
   *
   * @param relation Relation indexed
   * @param pagefile Page file
   * @param distanceQuery Distance query
   * @param distanceFunction Distance function
   */
  public MTreeIndex(Relation<O> relation, PageFile<MTreeNode<O, D>> pagefile, DistanceQuery<O, D> distanceQuery, DistanceFunction<O, D> distanceFunction) {
    super(pagefile, distanceQuery, distanceFunction);
    this.relation = relation;
    this.initialize();
  }

  /**
   * @return a new MTreeLeafEntry representing the specified data object
   */
  protected MTreeEntry<D> createNewLeafEntry(DBID id, O object, D parentDistance) {
    return new MTreeLeafEntry<D>(id, parentDistance);
  }

  @Override
  public void insert(DBID id) {
    insert(createNewLeafEntry(id, relation.get(id), getDistanceFactory().undefinedDistance()), false);
  }

  @Override
  public void insertAll(DBIDs ids) {
    List<MTreeEntry<D>> objs = new ArrayList<MTreeEntry<D>>(ids.size());
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

  @Override
  public String getLongName() {
    return "M-Tree";
  }

  @Override
  public String getShortName() {
    return "mtree";
  }
}