package experimentalcode.remigius;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;

public interface AlgorithmAdapter<O extends DatabaseObject> {

	public Collection<Visualizer<O>> getVisualizationGenerators(Database<O> db, Result r, VisualizationManager<O> v);
}