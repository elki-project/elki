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
package de.lmu.ifi.dbs.elki.distance.distancefunction;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import net.jafama.FastMath;

/**
 * Mahalanobis quadratic form distance for feature vectors.
 * <p>
 * For a weight matrix M, this distance is defined as
 * \[ \text{Mahalanobis}_M(\vec{x},\vec{y}) :=
 * \sqrt{(\vec{x}-\vec{y})^T \cdot M \cdot (\vec{x}-\vec{y})} \]
 * <p>
 * Reference:
 * <p>
 * P. C. Mahalanobis<br>
 * On the generalized distance in statistics<br>
 * Proceedings of the National Institute of Sciences of India. 2 (1)
 * <p>
 * This is the implementation as quadratic form distance function. In many cases
 * it will be more convenient to use the static function
 * {@link de.lmu.ifi.dbs.elki.math.linearalgebra.VMath#mahalanobisDistance}
 * instead.
 * <p>
 * TODO: Add a factory with parameterizable weight matrix! Right now, this can
 * only be used from Java and from subclasses, not from command line or MiniGUI.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
@Reference(authors = "P. C. Mahalanobis", //
    title = "On the generalized distance in statistics", //
    booktitle = "Proceedings of the National Institute of Sciences of India. 2 (1)", //
    bibkey = "journals/misc/Mahalanobis36")
public class MahalanobisDistanceFunction extends MatrixWeightedQuadraticDistanceFunction {
  /**
   * Constructor.
   *
   * @param weightMatrix Weight matrix.
   */
  public MahalanobisDistanceFunction(double[][] weightMatrix) {
    super(weightMatrix);
  }

  @Override
  public double distance(NumberVector o1, NumberVector o2) {
    return FastMath.sqrt(super.distance(o1, o2));
  }

  @Override
  public double norm(NumberVector obj) {
    return Math.sqrt(super.norm(obj));
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public boolean isSquared() {
    return false;
  }
}
