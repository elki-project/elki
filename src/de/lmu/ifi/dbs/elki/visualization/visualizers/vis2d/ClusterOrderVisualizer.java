package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreEvent;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreListener;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection2D;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualizer;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize an OPTICS cluster order by drawing connection lines.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.has ClusterOrderVisualization oneway - - produces
 *
 * @param <NV> object type
 */
public class ClusterOrderVisualizer<NV extends NumberVector<NV,?>> extends Projection2DVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Cluster Order";
  
  /**
   * CSS class name
   */
  private static final String CSSNAME = "co";
  
  /**
   * The result we visualize
   */
  protected ClusterOrderResult<?> result;
  
  /**
   * Constructor, adhering to
   * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
   */
  public ClusterOrderVisualizer() {
    super(NAME);
  }

  /**
   * Initialize the visualizer.
   * 
   * @param context Context
   * @param result Result class.
   */
  public void init(VisualizerContext<? extends NV> context, ClusterOrderResult<?> result) {
    super.init(context);
    this.result = result;
  }

  @Override
  public Visualization visualize(SVGPlot svgp, Projection2D proj, double width, double height) {
    return new ClusterOrderVisualization(context, svgp, proj, width, height);
  }

  /**
   * Cluster order visualizer.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.has de.lmu.ifi.dbs.elki.result.ClusterOrderResult oneway - - visualizes
   */
  // TODO: listen for CLUSTER ORDER changes.
  protected class ClusterOrderVisualization extends Projection2DVisualization<NV> implements DataStoreListener<NV> {
    /**
     * Constructor.
     * 
     * @param context Context
     * @param svgp Plot
     * @param proj Projection
     * @param width Width
     * @param height Height
     */
    public ClusterOrderVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, Projection2D proj, double width, double height) {
      super(context, svgp, proj, width, height, Visualizer.LEVEL_STATIC);
      context.addDataStoreListener(this);
      incrementalRedraw();
    }

    @Override
    public void destroy() {
      super.destroy();
      context.removeDataStoreListener(this);
    }

    @Override
    public void redraw() {
      CSSClass cls = new CSSClass(this, CSSNAME);
      context.getLineStyleLibrary().formatCSSClass(cls, 0, context.getStyleLibrary().getLineWidth(StyleLibrary.CLUSTERORDER));
      
      svgp.addCSSClassOrLogError(cls);
      
      // get the Database
      Database<? extends NV> database = context.getDatabase();
      for (ClusterOrderEntry<?> ce : result) {
        DBID thisId = ce.getID();
        DBID prevId = ce.getPredecessorID();
        if (thisId == null || prevId == null) {
          continue;
        }
        double[] thisVec = proj.fastProjectDataToRenderSpace(database.get(thisId));
        double[] prevVec = proj.fastProjectDataToRenderSpace(database.get(prevId));
        
        Element arrow = svgp.svgLine(prevVec[0], prevVec[1], thisVec[0], thisVec[1]);
        SVGUtil.setCSSClass(arrow, cls.getName());
        
        layer.appendChild(arrow);
      }
    }

    @Override
    public void contentChanged(@SuppressWarnings("unused") DataStoreEvent<NV> e) {
      synchronizedRedraw();
    }
  }
}