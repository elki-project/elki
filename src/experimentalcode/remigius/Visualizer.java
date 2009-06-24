package experimentalcode.remigius;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;

public interface Visualizer<O extends DatabaseObject> {
	
	public Double getPositioned(O o, int dimx);
	public Visualization visualize(SVGPlot svgp, int dimx, int dimy);
}
