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

import de.lmu.ifi.dbs.elki.visualization.css.CSSClass;

/**
 * Interface to obtain CSS classes for plot lines.
 * 
 * {@code meta} is a set of Objects, usually constants that may or may not be
 * used by the {@link LineStyleLibrary} to generate variants of the style.
 * 
 * Predefined meta flags that are usually supported are:
 * <dl>
 * <dt>{@link #FLAG_STRONG}</dt>
 * <dd>Request a "stronger" version of the same style</dd>
 * <dt>{@link #FLAG_WEAK}</dt>
 * <dd>Request a "weaker" version of the same style</dd>
 * <dt>{@link #FLAG_INTERPOLATED}</dt>
 * <dd>Request an "interpolated" version of the same style (e.g. lighter or
 * dashed)</dd>
 * </dl>
 * 
 * @author Erich Schubert
 * @since 0.3
 * 
 * @navassoc - - - CSSClass
 */
public interface LineStyleLibrary {
  /**
   * Meta flag to request a 'stronger' version of the style
   */
  String FLAG_STRONG = "strong";

  /**
   * Meta flag to request a 'weaker' version of the style
   */
  String FLAG_WEAK = "weak";

  /**
   * Meta flag to request an 'interpolated' version of the style
   */
  String FLAG_INTERPOLATED = "interpolated";

  /**
   * Add the formatting statements to the given CSS class.
   * 
   * Note: this can overwrite some existing properties of the CSS class.
   * 
   * @param cls CSS class to modify
   * @param style style number
   * @param width line width
   * @param meta meta objects to request line variants
   */
  void formatCSSClass(CSSClass cls, int style, double width, Object... meta);
}
