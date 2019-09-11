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
package elki.data.model;

import elki.math.linearalgebra.LinearEquationSystem;
import elki.result.textwriter.TextWriteable;
import elki.result.textwriter.TextWriterStream;

/**
 * Cluster model containing a linear equation system for the cluster.
 * 
 * @author Erich Schubert
 * @since 0.2
 *
 * @composed - - - LinearEquationSystem
 */
public class LinearEquationModel implements Model, TextWriteable {
  /**
   * Equation system
   */
  private LinearEquationSystem les;

  /**
   * Constructor
   * @param les equation system
   */
  public LinearEquationModel(LinearEquationSystem les) {
    super();
    this.les = les;
  }

  /**
   * Get assigned Linear Equation System
   * 
   * @return linear equation system
   */
  public LinearEquationSystem getLes() {
    return les;
  }

  /**
   * Assign new Linear Equation System.
   * 
   * @param les new linear equation system
   */
  public void setLes(LinearEquationSystem les) {
    this.les = les;
  }
  
  @Override
  public void writeToText(TextWriterStream out, String label) {
    if(label != null) {
      out.commentPrintLn(label);
    }
    out.commentPrintLn("Model class: " + getClass().getName());
    out.commentPrintLn(les.equationsToString(6));
  }
}
