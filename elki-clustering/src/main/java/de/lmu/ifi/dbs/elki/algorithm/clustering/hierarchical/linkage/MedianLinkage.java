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
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Median-linkage &mdash; weighted pair group method using centroids (WPGMC).
 * <p>
 * Similar to {@link WeightedAverageLinkage} (WPGMA), the weight of points in
 * this method decreases with the depth of the tree. This yields to difficult to
 * understand semantics of the result, as it does <em>not yield the distance of
 * medians</em>. The method is best defined recursively:
 * \[d_{\text{WPGMC}}(A\cup B,C):=\tfrac{1}{2}d(A,C)+\tfrac{1}{2}d(B,C)
 * -\tfrac{1}{4}d(A,B)\]
 * <p>
 * Reference:
 * <p>
 * J. C. Gower<br>
 * A comparison of some methods of cluster analysis<br>
 * Biometrics (1967): 623-637.
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "J. C. Gower", //
    title = "A comparison of some methods of cluster analysis", //
    booktitle = "Biometrics (1967)", //
    url = "https://doi.org/10.2307/2528417", //
    bibkey = "doi:10.2307/2528417")
@Alias({ "wpgmc", "WPGMC", "weighted-centroid", "de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.MedianLinkageMethod" })
public class MedianLinkage implements Linkage {
  /**
   * Static instance of class.
   */
  public static final MedianLinkage STATIC = new MedianLinkage();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public MedianLinkage() {
    super();
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    return .5 * (dx + dy) - .25 * dxy;
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
    protected MedianLinkage makeInstance() {
      return STATIC;
    }
  }
}
