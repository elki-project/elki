package experimentalcode.remigius.Visualizers;

import org.w3c.dom.Element;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClassManager.CSSNamingConflict;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGPlot;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGSimpleLinearAxis;

/**
 * Generates a SVG-Element containing axes, including labeling.
 * 
 * TODO: This visualizer could be more useful if it only would draw 1 axis with
 * its position depending on a parameter.
 * 
 * @author Remigius Wojdanowski
 * 
 * @param <NV> Type of the DatabaseObject being visualized.
 */
public class Projection2DAxisVisualizer<NV extends NumberVector<NV, ?>> extends Projection2DVisualizer<NV> {
  /**
   * A short name characterizing this Visualizer.
   */
  private static final String NAME = "Axes";

  /**
   * Initializes this Visualizer.
   * 
   * @param database contains all objects to be processed.
   */
  public void init(VisualizerContext context) {
    super.init(NAME, context);
    super.setLevel(Visualizer.LEVEL_BACKGROUND);
  }

  @Override
  public Element visualize(SVGPlot plot) {
    Element layer = super.visualize(plot);
    int dim = context.getDatabase().dimensionality();
    
    // origin
    Vector orig = proj.projectScaledToRender(new Vector(dim));
    // diagonal point opposite to origin
    Vector diag = new Vector(dim);
    for(int d2 = 0; d2 < dim; d2++) {
      diag.set(d2, 1);
    }
    diag = proj.projectScaledToRender(diag);
    // compute angle to diagonal line, used for axis labeling.
    double diaga = Math.atan2(diag.get(1) - orig.get(1), diag.get(0) - orig.get(0));
    
    // draw axes
    for(int d = 1; d <= dim; d++) {
      Vector v = new Vector(dim);
      v.set(d-1,1);
      // projected endpoint of axis
      Vector ax = proj.projectScaledToRender(v);
      boolean righthand = false;
      double axa = Math.atan2(ax.get(1) - orig.get(1), ax.get(0) - orig.get(0));
      if (axa > diaga || (diaga > 0 && axa > diaga + Math.PI)) {
        righthand = true;
      }
      //System.err.println(ax.get(0) + " "+ ax.get(1)+ " "+(axa*180/Math.PI)+" "+(diaga*180/Math.PI));
      if(ax.get(0) != orig.get(0) || ax.get(1) != orig.get(1)) {
        try {
          SVGSimpleLinearAxis.drawAxis(plot, layer, proj.getScale(d), orig.get(0), orig.get(1), ax.get(0), ax.get(1), true, righthand);
        }
        catch(CSSNamingConflict e) {
          throw new RuntimeException("Conflict in CSS naming for axes.", e);
        }
      }
    }
    // FIXME: caller should also call updateStyleElement at the end.
    plot.updateStyleElement();
    return layer;
  }
}
