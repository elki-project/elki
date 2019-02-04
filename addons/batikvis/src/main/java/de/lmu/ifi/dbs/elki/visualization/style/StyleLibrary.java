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
package de.lmu.ifi.dbs.elki.visualization.style;

import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.lines.LineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;

/**
 * Style library interface. A style library allows the user to customize the
 * visual rendering, for example for print media or screen presentation without
 * having to change program code.
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @composed - - - ColorLibrary
 * @composed - - - LineStyleLibrary
 * @composed - - - MarkerLibrary
 */
public interface StyleLibrary {
  /**
   * Default
   */
  String DEFAULT = "";

  /**
   * Page
   */
  String PAGE = "page";

  /**
   * Plot
   */
  String PLOT = "plot";

  /**
   * Axis
   */
  String AXIS = "axis";

  /**
   * Axis tick
   */
  String AXIS_TICK = "axis.tick";

  /**
   * Axis minor tick
   */
  String AXIS_TICK_MINOR = "axis.tick.minor";

  /**
   * Axis label
   */
  String AXIS_LABEL = "axis.label";

  /**
   * Key
   */
  String KEY = "key";

  /**
   * Clusterorder
   */
  String CLUSTERORDER = "plot.clusterorder";

  /**
   * Margin
   */
  String MARGIN = "margin";

  /**
   * Bubble size
   */
  String BUBBLEPLOT = "plot.bubble";

  /**
   * Marker size
   */
  String MARKERPLOT = "plot.marker";

  /**
   * Dot size
   */
  String DOTPLOT = "plot.dot";

  /**
   * Grayed out objects
   */
  String PLOTGREY = "plot.grey";

  /**
   * Reference points color and size
   */
  String REFERENCE_POINTS = "plot.referencepoints";

  /**
   * Polygons style
   */
  String POLYGONS = "plot.polygons";

  /**
   * Selection color and opacity
   */
  String SELECTION = "plot.selection";

  /**
   * Selection color and opacity during selecting process
   */
  String SELECTION_ACTIVE = "plot.selection.active";

  /**
   * Scaling constant. Keep in sync with
   * {@link de.lmu.ifi.dbs.elki.visualization.projections.Projection#SCALE}
   */
  double SCALE = 100.0;

  /* ** Property types ** */
  /**
   * Color
   */
  String COLOR = "color";

  /**
   * Background color
   */
  String BACKGROUND_COLOR = "background-color";

  /**
   * Text color
   */
  String TEXT_COLOR = "text-color";

  /**
   * Color set
   */
  String COLORSET = "colorset";

  /**
   * Line width
   */
  String LINE_WIDTH = "line-width";

  /**
   * Text size
   */
  String TEXT_SIZE = "text-size";

  /**
   * Font family
   */
  String FONT_FAMILY = "font-family";

  /**
   * Generic size
   */
  String GENERIC_SIZE = "size";

  /**
   * Opacity (transparency)
   */
  String OPACITY = "opacity";

  /**
   * XY curve styling.
   */
  String XYCURVE = "xycurve";

  /**
   * Retrieve a color for an item
   * 
   * @param name Reference name
   * @return color in CSS/SVG valid format: hexadecimal (#aabbcc) or names such
   *         as "red"
   */
  String getColor(String name);

  /**
   * Retrieve background color for an item
   * 
   * @param name Reference name
   * @return color in CSS/SVG valid format: hexadecimal (#aabbcc) or names such
   *         as "red"
   */
  String getBackgroundColor(String name);

  /**
   * Retrieve text color for an item
   * 
   * @param name Reference name
   * @return color in CSS/SVG valid format: hexadecimal (#aabbcc) or names such
   *         as "red"
   */
  String getTextColor(String name);

  /**
   * Retrieve colorset for an item
   * 
   * @param name Reference name
   * @return color library
   */
  ColorLibrary getColorSet(String name);

  /**
   * Get line width
   * 
   * @param key Key
   * @return line width as double
   */
  double getLineWidth(String key);

  /**
   * Get generic size
   * 
   * @param key Key
   * @return size as double
   */
  double getSize(String key);

  /**
   * Get text size
   * 
   * @param key Key
   * @return line width as double
   */
  double getTextSize(String key);

  /**
   * Get font family
   * 
   * @param key Key
   * @return font family CSS string
   */
  String getFontFamily(String key);

  /**
   * Get opacity
   * 
   * @param key Key
   * @return size as double
   */
  double getOpacity(String key);
  
  /**
   * Get the line style library to use.
   * 
   * @return line style library
   */
  LineStyleLibrary lines();
  
  /**
   * Get the marker library to use.
   * 
   * @return marker library
   */
  MarkerLibrary markers();
}