package experimentalcode.students.roedler.parallelCoordinates.visualizer;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.VisualizationTask;
import experimentalcode.students.roedler.parallelCoordinates.projections.ProjectionParallel;


/**
 * Default class to handle parallel visualizations.
 * 
 * @author Erich Schubert
 * 
 */
public abstract class ParallelVisualization<NV extends NumberVector<?, ?>> extends AbstractVisualization {

  /**
   * The current projection
   */
  final protected ProjectionParallel proj;

  /**
   * The representation we visualize
   */
  final protected Relation<NV> rep;

  /**
   * Constructor.
   * 
   * @param task Visualization task
   */
  public ParallelVisualization(VisualizationTask task) {
    super(task);
    this.proj = task.getProj();
    this.rep = task.getRelation();
    final double margin = 0.; //context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    this.layer = setupCanvas(svgp, proj, margin, task.getWidth(), task.getHeight());
  }

  /**
   * Utility function to setup a canvas element for the visualization.
   * 
   * @param svgp Plot element
   * @param proj Projection to use
   * @param margin Margin to use
   * @param width Width
   * @param height Height
   * @return wrapper element with appropriate view box.
   */
  public static Element setupCanvas(SVGPlot svgp, ProjectionParallel proj, double margin, double width, double height) {
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    final String transform = SVGUtil.makeMarginTransform(width, height, proj.getSizeX(), proj.getSizeY(), 0.);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, transform);
    return layer;
  }

}
