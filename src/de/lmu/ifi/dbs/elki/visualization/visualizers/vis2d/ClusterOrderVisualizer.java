package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.ClusterOrderEntry;
import de.lmu.ifi.dbs.elki.result.ClusterOrderResult;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;

/**
 * Visualize an OPTICS cluster order by drawing connection lines.
 * 
 * @author Erich Schubert
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
  private ClusterOrderResult<?> result;
  
  /**
   * Initialize the visualizer.
   * 
   * @param context Context
   * @param result Result class.
   */
  public void init(VisualizerContext context, ClusterOrderResult<?> result) {
    super.init(NAME, context);
    this.result = result;
  }

  @Override
  public Element visualize(SVGPlot svgp, VisualizationProjection proj) {
    Database<NV> database = context.getDatabase();
    Element layer = super.setupCanvas(svgp, proj);
    
    CSSClass cls = new CSSClass(this, CSSNAME);
    context.getLineStyleLibrary().formatCSSClass(cls, 0, 0.001);
    
    try {
      svgp.getCSSClassManager().addClass(cls);
    }
    catch(CSSNamingConflict e) {
      logger.error("CSS naming conflict.", e);
    }
    
    for (ClusterOrderEntry<?> ce : result) {
      Integer thisId = ce.getID();
      Integer prevId = ce.getPredecessorID();
      if (thisId == null || prevId == null) {
        continue;
      }
      Vector thisVec = proj.projectDataToRenderSpace(database.get(thisId));
      Vector prevVec = proj.projectDataToRenderSpace(database.get(prevId));
      
      Element arrow = svgp.svgLine(prevVec.get(0), prevVec.get(1), thisVec.get(0), thisVec.get(1));
      SVGUtil.setCSSClass(arrow, cls.getName());
      
      layer.appendChild(arrow);
    }
    
    return layer;
  }
}
