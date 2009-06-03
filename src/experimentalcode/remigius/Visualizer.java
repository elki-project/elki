package experimentalcode.remigius;

import org.w3c.dom.Document;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PublicationColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;

public abstract class Visualizer<O extends DoubleVector> {
	
	protected Database<O> database;
	private LinearScale[] scales;
	
	protected static final CommonSVGShapes SHAPEGEN = new CommonSVGShapes();
	protected static final ColorLibrary COLORS = new PublicationColorLibrary();
	
	public Visualizer(Database<O> db){
		this.database = db;
		this.scales = Scales.calcScales(db);
	}
	
	public Double getPositioned(O o, int dimx){
			return scales[dimx].getScaled(o.getValue(dimx));
	}
	
	public abstract Visualization visualize(Document doc, int dimx, int dimy);
}
