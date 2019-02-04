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
package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.linkage;

import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Single-linkage ("minimum") clustering method.
 * <p>
 * The distance of two clusters is simply the minimum of all pairwise distances
 * between the two clusters.
 * <p>
 * The distance of two clusters is defined as:
 * \[d_{\min}(A,B):=\min_{a\in A}\min_{b\in B} d(a,b)\]
 * <p>
 * This can be computed recursively using:
 * \[d_{\min}(A\cup B,C) = \min(d(A,C), d(B,C))\]
 * <p>
 * Note that with similarity functions, one would need to use the maximum
 * instead to get the same effect.
 * <p>
 * Reference:
 * <p>
 * K. Florek, J. Łukaszewicz, J. Perkal, H. Steinhaus, S. Zubrzycki<br>
 * Sur la liaison et la division des points d'un ensemble fini<br>
 * Colloquium Mathematicae 2(3-4)
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "K. Florek, J. Łukaszewicz, J. Perkal, H. Steinhaus, S. Zubrzycki", //
    title = "Sur la liaison et la division des points d'un ensemble fini", //
    booktitle = "Colloquium Mathematicae 2(3-4)", //
    bibkey = "journals/misc/FlorekLPSZ51")
@Alias({ "single-link", "single", "slink", "nearest", "nearest-neighbor", "de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.SingleLinkageMethod" })
@Priority(Priority.IMPORTANT)
public class SingleLinkage implements Linkage {
  /**
   * Static instance of class.
   */
  public static final SingleLinkage STATIC = new SingleLinkage();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public SingleLinkage() {
    super();
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    return dx < dy ? dx : dy;
  }

  /**
   * Class parameterizer.
   * 
   * Returns the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected SingleLinkage makeInstance() {
      return STATIC;
    }
  }
}
