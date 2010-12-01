package de.lmu.ifi.dbs.elki.visualization.visualizers.vis2d;

import java.util.ArrayList;

import org.apache.batik.util.SVGConstants;
import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.result.AnyResult;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.projections.Projection;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisFactory;
import de.lmu.ifi.dbs.elki.visualization.visualizers.Visualization;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizationTask;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerContext;
import de.lmu.ifi.dbs.elki.visualization.visualizers.VisualizerUtil;

/**
 * Generates a SVG-Element containing axes, including labeling.
 * 
 * @author Remigius Wojdanowski
 * 
 * @apiviz.uses SVGSimpleLinearAxis
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class AxisVisualization<NV extends NumberVector<NV, ?>> extends P2DVisualization<NV> {
  /**
   * Constructor.
   * 
   * @param task VisualizationTask
   */
  public AxisVisualization(VisualizationTask task) {
    super(task, VisFactory.LEVEL_BACKGROUND);
    incrementalRedraw();
  }

  @Override
  protected void redraw() {
    int dim = DatabaseUtil.dimensionality(context.getDatabase());

    // origin
    double[] orig = proj.fastProjectScaledToRender(new Vector(dim));
    // diagonal point opposite to origin
    double[] diag = new double[dim];
    for(int d2 = 0; d2 < dim; d2++) {
      diag[d2] = 1;
    }
    diag = proj.fastProjectScaledToRender(new Vector(diag));
    // compute angle to diagonal line, used for axis labeling.
    double diaga = Math.atan2(diag[1] - orig[1], diag[0] - orig[0]);

    double alfontsize = 1.2 * context.getStyleLibrary().getTextSize(StyleLibrary.AXIS_LABEL);
    CSSClass alcls = new CSSClass(svgp, "unmanaged");
    alcls.setStatement(SVGConstants.CSS_FONT_SIZE_PROPERTY, SVGUtil.fmt(alfontsize));
    alcls.setStatement(SVGConstants.CSS_FILL_PROPERTY, context.getStyleLibrary().getTextColor(StyleLibrary.AXIS_LABEL));
    alcls.setStatement(SVGConstants.CSS_FONT_FAMILY_PROPERTY, context.getStyleLibrary().getFontFamily(StyleLibrary.AXIS_LABEL));

    // draw axes
    for(int d = 0; d < dim; d++) {
      Vector v = new Vector(dim);
      v.set(d, 1);
      // projected endpoint of axis
      double[] ax = proj.fastProjectScaledToRender(v);
      boolean righthand = false;
      double axa = Math.atan2(ax[1] - orig[1], ax[0] - orig[0]);
      if(axa > diaga || (diaga > 0 && axa > diaga + Math.PI)) {
        righthand = true;
      }
      // System.err.println(ax.get(0) + " "+ ax.get(1)+
      // " "+(axa*180/Math.PI)+" "+(diaga*180/Math.PI));
      if(ax[0] != orig[0] || ax[1] != orig[1]) {
        try {
          SVGSimpleLinearAxis.drawAxis(svgp, layer, proj.getScale(d), orig[0], orig[1], ax[0], ax[1], true, righthand, context.getStyleLibrary());
          // TODO: move axis labeling into drawAxis function.
          double offx = (righthand ? 1 : -1) * 0.02 * Projection.SCALE;
          double offy = (righthand ? 1 : -1) * 0.02 * Projection.SCALE;
          Element label = svgp.svgText(ax[0] + offx, ax[1] + offy, "Dim. " + SVGUtil.fmt(d + 1));
          SVGUtil.setAtt(label, SVGConstants.SVG_STYLE_ATTRIBUTE, alcls.inlineCSS());
          SVGUtil.setAtt(label, SVGConstants.SVG_TEXT_ANCHOR_ATTRIBUTE, righthand ? SVGConstants.SVG_START_VALUE : SVGConstants.SVG_END_VALUE);
          layer.appendChild(label);
        }
        catch(CSSNamingConflict e) {
          throw new RuntimeException("Conflict in CSS naming for axes.", e);
        }
      }
    }
  }

  /**
   * Factory for axis visualizations
   * 
   * @author Erich Schubert
   * 
   * @apiviz.stereotype factory
   * @apiviz.has AxisVisualization oneway - - produces
   * 
   * @param <NV>
   */
  public static class Factory<NV extends NumberVector<NV, ?>> extends P2DVisFactory<NV> {
    /**
     * A short name characterizing this Visualizer.
     */
    private static final String NAME = "Axes";

    /**
     * Constructor, adhering to
     * {@link de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizable}
     */
    public Factory() {
      super(NAME, VisFactory.LEVEL_BACKGROUND);
    }
    
    @Override
    public Visualization makeVisualization(VisualizationTask task) {
      return new AxisVisualization<NV>(task);
    }

    @Override
    public void addVisualizers(VisualizerContext<? extends NV> context, AnyResult result) {
      ArrayList<Database<?>> databases = ResultUtil.filterResults(result, Database.class);
      for(Database<?> database : databases) {
        if(!VisualizerUtil.isNumberVectorDatabase(database)) {
          continue;
        }
        context.addVisualizer(database, this);
      }
    }
    
    @Override
    public boolean allowThumbnails(@SuppressWarnings("unused") VisualizationTask task) {
      // Don't use thumbnails
      return false;
    }
  }
}