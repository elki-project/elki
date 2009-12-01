package experimentalcode.shared.algorithm.clustering;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

/**
 * Simple Interface for an heuristic to pick reference points.
 * 
 * @author Erich Schubert
 *
 * @param <O> Object type
 */
public interface ReferencePointsHeuristic<O extends NumberVector<O,?>> extends Parameterizable {
  /**
   * Get the reference points for the given database.
   * 
   * @param db Database
   * @return Collection of reference points.
   */
  public Collection<O> getReferencePoints(Database<O> db);
}
