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
 * Line style library featuring solid lines for default styles only (combine
 * with a color library to obtain enough classes!)
 * 
 * {@link LineStyleLibrary#FLAG_STRONG} will result in thicker lines.
 * 
 * {@link LineStyleLibrary#FLAG_WEAK} will result in thinner and
 * semi-transparent lines.
 * 
 * {@link LineStyleLibrary#FLAG_INTERPOLATED} will result in dashed lines.
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @composed - - - ColorLibrary
 */
public class SolidLineStyleLibrary implements LineStyleLibrary {
  /**
   * Reference to the color library.
   */
  private ColorLibrary colors;

  /**
   * Color of "uncolored" dots
   */
  private String dotcolor;

  /**
   * Color of "greyed out" dots
   */
  private String greycolor;

  /**
   * Constructor.
   * 
   * @param style Style library to use.
   */
  public SolidLineStyleLibrary(StyleLibrary style) {
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
    if(interpolated) {
      cls.setStatement(CSSConstants.CSS_STROKE_DASHARRAY_PROPERTY, SVGUtil.fmt(width / StyleLibrary.SCALE * 2.) + "," + SVGUtil.fmt(width / StyleLibrary.SCALE * 2.));
    }
  }
}
