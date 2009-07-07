package experimentalcode.remigius;

import java.util.Collection;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;

public interface AlgorithmAdapter<O extends DatabaseObject> {

	public boolean canVisualize(Result result);
	public Collection<Visualizer<O>> getVisualizers();
	public void init(Database<O> database, Result result, VisualizationManager<O> visManager);
}