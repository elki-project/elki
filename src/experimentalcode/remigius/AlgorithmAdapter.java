package experimentalcode.remigius;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;

public interface AlgorithmAdapter<O extends DoubleVector> {

	public Collection<Visualizer<O>> getVisualizationGenerators(Database<O> db, Result r);
}
