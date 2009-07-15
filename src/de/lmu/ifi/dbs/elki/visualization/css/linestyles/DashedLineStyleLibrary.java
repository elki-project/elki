package de.lmu.ifi.dbs.elki.visualization.css.linestyles;

import org.apache.batik.util.CSSConstants;

import de.lmu.ifi.dbs.elki.utilities.output.FormatUtil;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;

/**
 * Line library using various dashed and dotted line styles.
 * 
 * This library is particularly useful for black and white output.
 * 
 * {@link #LineStyleLibrary.FLAG_STRONG} will result in thicker lines.
 * 
 * {@link #LineStyleLibrary.FLAG_WEAK} will result in thinner and semi-transparent lines.
 * 
 * {@link #LineStyleLibrary.FLAG_INTERPOLATED} will result in shorter dashing patterns.
 * 
 * @author Erich Schubert
 *
 */
public class DashedLineStyleLibrary implements LineStyleLibrary {
  private ColorLibrary colors;

  /** Dash patterns to regularly use */
  private double[][] dashpatterns = {
  // solid, no dashing
  {},
  // half-half
  { .5, .5 },
  // quarters
  { .25, .25, .25, .25 },
  // alternating long-quart
  { .75, .25 },
  // dash-dot
  { .7, .1, .1, .1 }, };
  /** Replacement for the solid pattern in 'interpolated' mode */
  private double[] solidreplacement = { .1, .1 };

  private int dashnum = dashpatterns.length;
  
  public DashedLineStyleLibrary(ColorLibrary colors) {
    super();
    this.colors = colors;
    // TODO: Do a divisor-check to optimize colors * styles combinations?
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
    cls.setStatement(CSSConstants.CSS_STROKE_WIDTH_PROPERTY, FormatUtil.format(width * 100, FormatUtil.NF4)+"%");
    // handle dashing
    int styleflav = style % dashnum;
    if(!interpolated) {
      double[] pat = dashpatterns[styleflav];
      assert (pat.length % 2 == 0);
      if(pat.length > 0) {
        StringBuffer pattern = new StringBuffer();
        for(int i = 0; i < pat.length; i++) {
          if(i > 0) {
            pattern.append(",");
          }
          pattern.append(FormatUtil.format(pat[i] * width * 30, FormatUtil.NF4));
          //pattern.append("%");
        }
        cls.setStatement(CSSConstants.CSS_STROKE_DASHARRAY_PROPERTY, pattern.toString());
      }
    } else {
      double[] pat = dashpatterns[styleflav];
      if (styleflav == 0) {
        pat = solidreplacement;
      }
      assert (pat.length % 2 == 0);
      // TODO: add dotting.
      if(pat.length > 0) {
        StringBuffer pattern = new StringBuffer();
        for(int i = 0; i < pat.length; i++) {
          if(i > 0) {
            pattern.append(",");
          }
          pattern.append(FormatUtil.format(pat[i] * width * 10, FormatUtil.NF4));
          //pattern.append("%");
        }
        cls.setStatement(CSSConstants.CSS_STROKE_DASHARRAY_PROPERTY, pattern.toString());
      }      
    }
  }
}
