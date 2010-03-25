package de.lmu.ifi.dbs.elki.visualization.style.lines;

import org.apache.batik.util.CSSConstants;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Line style library featuring solid lines for default styles only
 * (combine with a color library to obtain enough classes!)
 * 
 * {@link LineStyleLibrary#FLAG_STRONG} will result in thicker lines.
 * 
 * {@link LineStyleLibrary#FLAG_WEAK} will result in thinner and semi-transparent lines.
 * 
 * {@link LineStyleLibrary#FLAG_INTERPOLATED} will result in dashed lines.
 * 
 * @author Erich Schubert
 *
 */
public class SolidLineStyleLibrary implements LineStyleLibrary {
  /**
   * Reference to the color library.
   */
  private ColorLibrary colors;
  
  /**
   * Constructor.
   * 
   * @param style Style library to use.
   */
  public SolidLineStyleLibrary(StyleLibrary style) {
    super();
    this.colors = style.getColorSet(StyleLibrary.PLOT);
  }

  @Override
  public void formatCSSClass(CSSClass cls, int style, double width, Object... flags) {
    cls.setStatement(CSSConstants.CSS_STROKE_PROPERTY, colors.getColor(style));
    boolean interpolated = false;
    // process flavoring flags
    for(Object flag : flags) {
      if(flag == LineStyleLibrary.FLAG_STRONG) {
        width = width * 1.5;
      }
      else if(flag == LineStyleLibrary.FLAG_WEAK) {
        cls.setStatement(CSSConstants.CSS_STROKE_OPACITY_PROPERTY, ".50");
        width = width * 0.75;
      }
      else if(flag == LineStyleLibrary.FLAG_INTERPOLATED) {
        interpolated = true;
      }
    }
    cls.setStatement(CSSConstants.CSS_STROKE_WIDTH_PROPERTY, SVGUtil.fmt(width));
    if(interpolated) {
      cls.setStatement(CSSConstants.CSS_STROKE_DASHARRAY_PROPERTY, ""+SVGUtil.fmt(width/StyleLibrary.SCALE*2)+","+SVGUtil.fmt(width/StyleLibrary.SCALE*2));
    }
  }
}
