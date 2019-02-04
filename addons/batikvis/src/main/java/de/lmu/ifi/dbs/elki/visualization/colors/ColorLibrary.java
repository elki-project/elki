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
package de.lmu.ifi.dbs.elki.visualization.colors;

/**
 * Color scheme interface
 * 
 * @author Erich Schubert
 * @since 0.2
 */
public interface ColorLibrary {
  /**
   * List of line colors
   */
  String COLOR_LINE_COLORS = "line.colors";

  /**
   * Named color for the page background
   */
  String COLOR_PAGE_BACKGROUND = "page.background";

  /**
   * Named color for a typical axis
   */
  String COLOR_AXIS_LINE = "axis.line";

  /**
   * Named color for a typical axis tick mark
   */
  String COLOR_AXIS_TICK = "axis.tick";

  /**
   * Named color for a typical axis tick mark
   */
  String COLOR_AXIS_MINOR_TICK = "axis.tick.minor";

  /**
   * Named color for a typical axis label
   */
  String COLOR_AXIS_LABEL = "axis.label";

  /**
   * Named color for the background of the key box
   */
  String COLOR_KEY_BACKGROUND = "key.background";

  /**
   * Named color for a label in the key part
   */
  String COLOR_KEY_LABEL = "key.label";

  /**
   * Background color for plot area
   */
  String COLOR_PLOT_BACKGROUND = "plot.background";

  /**
   * Return the number of native colors available. These are guaranteed to be
   * unique.
   * 
   * @return number of native colors
   */
  int getNumberOfNativeColors();

  /**
   * Return the i'th color.
   * 
   * @param index color index
   * @return color in hexadecimal notation (#aabbcc) or color name ("red") as
   *         valid in CSS and SVG.
   */
  String getColor(int index);
}
