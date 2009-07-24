package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizable;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.PropertiesBasedColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.scales.LinearScale;
import de.lmu.ifi.dbs.elki.visualization.scales.Scales;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import experimentalcode.remigius.ShapeLibrary;
import experimentalcode.remigius.VisualizationManager;
import experimentalcode.remigius.visualization.ScalarVisualization;
import experimentalcode.remigius.visualization.Visualization;

// TODO: Replace DoubleVector with NumberVector
public abstract class NumberVectorVisualizer<O extends DoubleVector, V extends Visualization> extends AbstractParameterizable implements Visualizer<O,V> {

  protected Database<O> database;
  protected LinearScale[] scales;
  
  protected VisualizationManager<O> visManager;

  protected static final ColorLibrary COLORS = new PropertiesBasedColorLibrary();
  
  private int level;
  private String name;
  
  public void init(Database<O> db, VisualizationManager<O> v){
    init(db, v, 0);
  }
  
  public void init(Database<O> db, VisualizationManager<O> v, int level){
    this.database = db;
    this.scales = Scales.calcScales(db);
    this.visManager = v;
    this.level = level;
  }
  
  @Override
  public int getLevel() {
    return level;
  }

  @Override
  public String getName() {
    return name;
  }

  @Override
  public Double getPositioned(O o, int dimx) {
    return scales[dimx].getScaled(o.getValue(dimx));
  }

  @Override
  public V visualize(SVGPlot svgp, int dimx, int dimy){
    
    Element layer = ShapeLibrary.createSVG(svgp.getDocument());
    return visualize(svgp, layer, dimx, dimy);
  }
  
  protected abstract V visualize(SVGPlot svgp, Element layer, int dimx, int dimy);
}
