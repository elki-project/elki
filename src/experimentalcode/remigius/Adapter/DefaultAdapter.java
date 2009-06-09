package experimentalcode.remigius.Adapter;

import java.util.Collection;
import java.util.HashSet;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import experimentalcode.remigius.AlgorithmAdapter;
import experimentalcode.remigius.VisualizationManager;
import experimentalcode.remigius.Visualizer;
import experimentalcode.remigius.Visualizers.DotVisualizer;

public class DefaultAdapter<O extends DoubleVector> implements AlgorithmAdapter<O>{

	public Collection<Visualizer<O>> getVisualizationGenerators(
			Database<O> db, Result r, VisualizationManager<O> v) {
		
		Collection<Visualizer<O>> col = new HashSet<Visualizer<O>>();

		col.add(new DotVisualizer<O>(db, v));

		return col;
	}
}