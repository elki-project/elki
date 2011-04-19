package de.lmu.ifi.dbs.elki.database.query.knn;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Instance of a KNN query for a particular spatial index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.index.tree.metrical.MetricalIndex
 */
public class MetricalIndexKNNQuery<O, D extends Distance<D>> extends AbstractDistanceKNNQuery<O, D> {
  /**
   * The index to use
   */
  protected final MetricalIndex<O, D, ?, ?> index;

  /**
   * Constructor.
   *
   * @param rep Representation to use
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MetricalIndexKNNQuery(Relation<? extends O> rep, MetricalIndex<O, D, ?, ?> index, DistanceQuery<O, D> distanceQuery) {
    super(rep, distanceQuery);
    this.index = index;
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForObject(O obj, int k) {
    return index.kNNQuery(obj, k);
  }

  @Override
  public List<DistanceResultPair<D>> getKNNForDBID(DBID id, int k) {
    // TODO: do this in the DB layer, we might have a better index?
    return getKNNForObject(rep.get(id), k);
  }

  @SuppressWarnings("unused")
  @Override
  public List<List<DistanceResultPair<D>>> getKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    // TODO: implement
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }
}