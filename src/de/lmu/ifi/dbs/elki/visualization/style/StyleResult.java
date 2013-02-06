package de.lmu.ifi.dbs.elki.visualization.style;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
import de.lmu.ifi.dbs.elki.result.Result;

/**
 * Result to encapsulate active styling rules.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.composedOf StylingPolicy
 */
public class StyleResult implements Result {
  /**
   * Styling policy
   */
  StylingPolicy policy;

  /**
   * Style library
   */
  StyleLibrary library;

  /**
   * Get the active styling policy
   * 
   * @return Styling policy
   */
  public StylingPolicy getStylingPolicy() {
    return policy;
  }

  /**
   * Set the active styling policy
   * 
   * @param policy new Styling policy
   */
  public void setStylingPolicy(StylingPolicy policy) {
    this.policy = policy;
  }

  /**
   * Get the style library
   * 
   * @return Style library
   */
  public StyleLibrary getStyleLibrary() {
    return library;
  }

  /**
   * Get the style library
   * 
   * @param library Style library
   */
  public void setStyleLibrary(StyleLibrary library) {
    this.library = library;
  }

  @Override
  public String getLongName() {
    return "Style policy";
  }

  @Override
  public String getShortName() {
    return "style-policy";
  }
}
