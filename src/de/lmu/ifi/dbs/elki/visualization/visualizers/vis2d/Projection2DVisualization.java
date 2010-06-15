package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.visualization.VisualizationProjection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.AbstractVisualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangeListener;
import de.lmu.ifi.dbs.elki.visualization.visualizers.events.ContextChangedEvent;

/**
 * Default class to handle 2D projected visualizations.
 * 
 * @author Erich Schubert
 */
public abstract class Projection2DVisualization<NV extends NumberVector<NV, ?>> extends AbstractVisualization implements ContextChangeListener {
  /**
   * Our context
   */
  protected VisualizerContext<? extends NV> context;

  /**
   * The plot we are attached to
   */
  protected SVGPlot svgp;

  /**
   * The current projection
   */
  protected VisualizationProjection proj;
  
  /**
   * Pending redraw
   */
  protected Runnable pendingRedraw = null;

  /**
   * Constructor.
   * 
   * @param context Visualization context
   * @param svgp Plot
   * @param proj Projection
   * @param width Width
   * @param height Height
   * @param level Level
   */
  public Projection2DVisualization(VisualizerContext<? extends NV> context, SVGPlot svgp, VisualizationProjection proj, double width, double height, Integer level) {
    super(width, height, level);
    this.context = context;
    this.svgp = svgp;
    this.proj = proj;
    final double margin = context.getStyleLibrary().getSize(StyleLibrary.MARGIN);
    this.layer = setupCanvas(svgp, proj, margin, width, height);
  }

  @Override
  public void destroy() {
    context.removeContextChangeListener(this);
  }

  @Override
  public void contextChanged(@SuppressWarnings("unused") ContextChangedEvent e) {
    // FIXME: update projection?
    synchronizedRedraw();
  }

  /**
   * Trigger a redraw, but avoid excessive redraws.
   */
  protected final void synchronizedRedraw() {
    Runnable pr = new Runnable() {
      @Override
      public void run() {
        if (pendingRedraw == this) {
          pendingRedraw = null;
          incrementalRedraw();
        }
      }
    };
    pendingRedraw = pr;
    svgp.scheduleUpdate(pr);
  }

  /**
   * Redraw the visualization (maybe incremental).
   * 
   * Optional - by default, it will do a full redraw, which often is faster!
   */
  protected void incrementalRedraw() {
    Element oldcontainer = null;
    if(layer.hasChildNodes()) {
      oldcontainer = layer;
      layer = (Element) layer.cloneNode(false);
    }
    redraw();
    if(oldcontainer != null && oldcontainer.getParentNode() != null) {
      oldcontainer.getParentNode().replaceChild(layer, oldcontainer);
    }
  }

  /**
   * Perform a full redraw.
   */
  protected abstract void redraw();

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
  public static Element setupCanvas(SVGPlot svgp, VisualizationProjection proj, double margin, double width, double height) {
    Element layer = SVGUtil.svgElement(svgp.getDocument(), SVGConstants.SVG_G_TAG);
    SVGUtil.setAtt(layer, SVGConstants.SVG_TRANSFORM_ATTRIBUTE, proj.estimateTransformString(margin, width, height));
    return layer;
  }
}