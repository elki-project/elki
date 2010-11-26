package de.lmu.ifi.dbs.elki.database.query.rknn;

import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Instance of a rKNN query for a particular spatial index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree
 */
public class MkTreeRKNNQuery<O extends DatabaseObject, D extends Distance<D>> extends AbstractRKNNQuery<O, D> {
  /**
   * The index to use
   */
  protected final AbstractMkTree<O, D, ?, ?> index;

  /**
   * Constructor.
   *
   * @param database Database to use
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MkTreeRKNNQuery(Database<? extends O> database, AbstractMkTree<O, D, ?, ?> index, DistanceQuery<O, D> distanceQuery) {
    super(database, distanceQuery);
    this.index = index;
  }

  @Override
  public List<DistanceResultPair<D>> getRKNNForObject(O obj, int k) {
    return index.reverseKNNQuery(obj, k);
  }

  @Override
  public List<DistanceResultPair<D>> getRKNNForDBID(DBID id, int k) {
    // TODO: do this in the DB layer, we might have a better index?
    return getRKNNForObject(database.get(id), k);
  }

  @SuppressWarnings("unused")
  @Override
  public List<List<DistanceResultPair<D>>> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    // TODO: implement
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }
}