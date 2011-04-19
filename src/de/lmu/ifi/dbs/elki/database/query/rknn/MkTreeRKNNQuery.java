package de.lmu.ifi.dbs.elki.database.query.rknn;

import java.util.List;

import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.query.DistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ExceptionMessages;

/**
 * Instance of a rKNN query for a particular spatial index.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses de.lmu.ifi.dbs.elki.index.tree.metrical.mtreevariants.mktrees.AbstractMkTree
 */
public class MkTreeRKNNQuery<O, D extends Distance<D>> extends AbstractRKNNQuery<O, D> {
  /**
   * The index to use
   */
  protected final AbstractMkTree<O, D, ?, ?> index;

  /**
   * Constructor.
   *
   * @param rep Representation to use
   * @param index Index to use
   * @param distanceQuery Distance query used
   */
  public MkTreeRKNNQuery(Relation<? extends O> rep, AbstractMkTree<O, D, ?, ?> index, DistanceQuery<O, D> distanceQuery) {
    super(rep, distanceQuery);
    this.index = index;
  }

  @SuppressWarnings("unused")
  @Override
  public List<DistanceResultPair<D>> getRKNNForObject(O obj, int k) {
    throw new AbortException("Preprocessor KNN query only supports ID queries.");
  }

  @Override
  public List<DistanceResultPair<D>> getRKNNForDBID(DBID id, int k) {
    return index.reverseKNNQuery(id, k);
  }

  @SuppressWarnings("unused")
  @Override
  public List<List<DistanceResultPair<D>>> getRKNNForBulkDBIDs(ArrayDBIDs ids, int k) {
    // TODO: implement
    throw new UnsupportedOperationException(ExceptionMessages.UNSUPPORTED_NOT_YET);
  }
}