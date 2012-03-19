package de.lmu.ifi.dbs.elki.result;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.math.scales.LinearScale;
import de.lmu.ifi.dbs.elki.math.scales.Scales;

/**
 * Class to keep shared scales across visualizers.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.composedOf LinearScale
 */
public class ScalesResult extends BasicResult {
  /**
   * The scales in use.
   */
  private LinearScale[] scales;

  /**
   * Constructor.
   * 
   * @param relation
   */
  public ScalesResult(Relation<? extends NumberVector<?, ?>> relation) {
    super("scales", "scales");
    scales = Scales.calcScales(relation);
  }

  /**
   * Get the scale for dimension dim (starting at 1!).
   * 
   * @param dim Dimension
   * @return Scale
   */
  public LinearScale getScale(int dim) {
    return scales[dim - 1];
  }

  /**
   * Set the scale for dimension dim (starting at 1!).
   * 
   * Note: you still need to trigger an event. This is not done automatically,
   * as you might want to set more than one scale!
   * 
   * @param dim Dimension
   * @param scale New scale
   * @return Scale
   */
  public void setScale(int dim, LinearScale scale) {
    scales[dim - 1] = scale;
  }
}