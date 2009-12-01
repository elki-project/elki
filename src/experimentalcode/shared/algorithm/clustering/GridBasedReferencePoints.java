package experimentalcode.shared.algorithm.clustering;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

public class GridBasedReferencePoints<O extends NumberVector<O,?>> extends AbstractParameterizable implements ReferencePointsHeuristic<O> {
  @Override
  public Collection<O> getReferencePoints(Database<O> db) {
    Pair<O, O> minmax = DatabaseUtil.computeMinMax(db);
    // FIXME: Continue
    return null;
  }
}
