package experimentalcode.remigius.Adapter;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import experimentalcode.remigius.AbstractAlgorithmAdapter;
import experimentalcode.remigius.VisualizationManager;
import experimentalcode.remigius.Visualizers.DotVisualizer;

public class DefaultAdapter<O extends DoubleVector> extends AbstractAlgorithmAdapter<O>{

private DotVisualizer<O> dotVisualizer;
	
	public DefaultAdapter(){
		super();
		dotVisualizer = new DotVisualizer<O>();
		visualizers.add(dotVisualizer);
	}

	@Override
	public boolean canVisualize(Result r){
		return true;
	}
	
	@Override
	protected void initVisualizer(Database<O> database, Result result, VisualizationManager<O> visManager) {
		dotVisualizer.setup(database, visManager);
	}
}