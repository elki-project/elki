package experimentalcode.remigius.Adapter;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.result.Result;
import experimentalcode.remigius.VisualizationManager;
import experimentalcode.remigius.Visualizers.AxisVisualizer;
import experimentalcode.remigius.Visualizers.DotVisualizer;
import experimentalcode.remigius.Visualizers.HistogramVisualizer;

public class DefaultAdapter<NV extends NumberVector<NV, N>, N extends Number> extends AbstractAlgorithmAdapter<NV>{

	private DotVisualizer<NV, N> dotVisualizer;
	private AxisVisualizer<NV, N> axisVisualizer;
	private HistogramVisualizer<NV, N> histoVisualizer;

	public DefaultAdapter(){
		super();
		dotVisualizer = new DotVisualizer<NV, N>();
		axisVisualizer = new AxisVisualizer<NV, N>();
		histoVisualizer = new HistogramVisualizer<NV, N>();
		visualizers.add(dotVisualizer);
		visualizers.add(axisVisualizer);
		visualizers.add(histoVisualizer);
	}

	@Override
	public boolean canVisualize(Result r){
		return true;
	}

	@Override
	protected void initVisualizer(Database<NV> database, Result result, VisualizationManager<NV> visManager) {
		axisVisualizer.setup(database, visManager);
		dotVisualizer.setup(database, visManager);
		histoVisualizer.setup(database, visManager);
	}
}