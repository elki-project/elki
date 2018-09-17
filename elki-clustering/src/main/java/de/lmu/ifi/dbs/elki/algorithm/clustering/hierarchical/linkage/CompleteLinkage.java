/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Complete-linkage ("maximum") clustering method.
 *
 * The distance of two clusters is simply the maximum of all pairwise distances
 * between the two clusters.
 *
 * The distance of two clusters is defined as:
 * \[d_{\max}(A,B):=\max_{a\in A}\max_{b\in B} d(a,b)\]
 * 
 * This can be computed recursively using:
 * \[d_{\max}(A\cup B,C) = \max(d(A,C), d(B,C))\]
 *
 * Note that with similarity functions, one would need to use the minimum
 * instead to get the same effect.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "complete", "clink", "complete-link", "farthest-neighbor", "de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.CompleteLinkageMethod" })
@Priority(Priority.IMPORTANT)
public class CompleteLinkage implements Linkage {
  /**
   * Static instance of class.
   */
  public static final CompleteLinkage STATIC = new CompleteLinkage();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public CompleteLinkage() {
    super();
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    return dx > dy ? dx : dy;
  }

  /**
   * Class parameterizer.
   * 
   * Returns the static instance.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected CompleteLinkage makeInstance() {
      return STATIC;
    }
  }
}
