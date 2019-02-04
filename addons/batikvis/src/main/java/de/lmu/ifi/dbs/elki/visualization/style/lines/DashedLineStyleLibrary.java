/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.visualization.style.lines;

import org.apache.batik.util.CSSConstants;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;
import de.lmu.ifi.dbs.elki.visualization.style.StyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.svg.SVGUtil;

/**
 * Line library using various dashed and dotted line styles.
 * 
 * This library is particularly useful for black and white output.
 * 
 * {@link LineStyleLibrary#FLAG_STRONG} will result in thicker lines.
 * 
 * {@link LineStyleLibrary#FLAG_WEAK} will result in thinner and
 * semi-transparent lines.
 * 
 * {@link LineStyleLibrary#FLAG_INTERPOLATED} will result in shorter dashing
 * patterns.
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @composed - - - ColorLibrary
 */
public class DashedLineStyleLibrary implements LineStyleLibrary {
  /**
   * The style library we use for colors
   */
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

  /**
   * Color of "uncolored" dots
   */
  private String dotcolor;

  /**
   * Color of "greyed out" dots
   */
  private String greycolor;

  /**
   * Constructor
   * 
   * @param style Style library
   */
  public DashedLineStyleLibrary(StyleLibrary style) {
    super();
    this.colors = style.getColorSet(StyleLibrary.PLOT);
    this.dotcolor = style.getColor(StyleLibrary.MARKERPLOT);
    this.greycolor = style.getColor(StyleLibrary.PLOTGREY);
  }

  @Override
  public void formatCSSClass(CSSClass cls, int style, double width, Object... flags) {
    if(style == -2) {
      cls.setStatement(CSSConstants.CSS_STROKE_PROPERTY, greycolor);
    }
    else if(style == -1) {
      cls.setStatement(CSSConstants.CSS_STROKE_PROPERTY, dotcolor);
    }
    else {
      cls.setStatement(CSSConstants.CSS_STROKE_PROPERTY, colors.getColor(style));
    }
    boolean interpolated = false;
    // process flavoring flags
    for(Object flag : flags) {
      if(LineStyleLibrary.FLAG_STRONG.equals(flag)) {
        width = width * 1.5;
      }
      else if(LineStyleLibrary.FLAG_WEAK.equals(flag)) {
        cls.setStatement(CSSConstants.CSS_STROKE_OPACITY_PROPERTY, ".50");
        width = width * 0.75;
      }
      else if(LineStyleLibrary.FLAG_INTERPOLATED.equals(flag)) {
        interpolated = true;
      }
    }
    cls.setStatement(CSSConstants.CSS_STROKE_WIDTH_PROPERTY, SVGUtil.fmt(width));
    // handle dashing
    int styleflav = (style > 0) ? (style % dashnum) : (-style % dashnum);
    if(!interpolated) {
      double[] pat = dashpatterns[styleflav];
      assert (pat.length % 2 == 0);
      if(pat.length > 0) {
        StringBuilder pattern = new StringBuilder();
        for(int i = 0; i < pat.length; i++) {
          if(i > 0) {
            pattern.append(',');
          }
          pattern.append(SVGUtil.fmt(pat[i] * width * 30));
          // pattern.append("%");
        }
        cls.setStatement(CSSConstants.CSS_STROKE_DASHARRAY_PROPERTY, pattern.toString());
      }
    }
    else {
      double[] pat = dashpatterns[styleflav];
      if(styleflav == 0) {
        pat = solidreplacement;
      }
      assert (pat.length % 2 == 0);
      // TODO: add dotting.
      if(pat.length > 0) {
        StringBuilder pattern = new StringBuilder();
        for(int i = 0; i < pat.length; i++) {
          if(i > 0) {
            pattern.append(',');
          }
          pattern.append(SVGUtil.fmt(pat[i] * width));
          // pattern.append("%");
        }
        cls.setStatement(CSSConstants.CSS_STROKE_DASHARRAY_PROPERTY, pattern.toString());
      }
    }
  }
}