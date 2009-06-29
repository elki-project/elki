package experimentalcode.remigius;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

public interface Visualizer<O extends DatabaseObject> extends Parameterizable {
	
	public Double getPositioned(O o, int dimx);
	public Visualization visualize(SVGPlot svgp, int dimx, int dimy);
}
