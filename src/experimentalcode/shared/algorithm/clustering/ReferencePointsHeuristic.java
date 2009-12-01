package experimentalcode.shared.algorithm.clustering;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;

public interface ReferencePointsHeuristic<O extends NumberVector<O,?>> extends Parameterizable {
  public Collection<O> getReferencePoints(Database<O> db);
}
