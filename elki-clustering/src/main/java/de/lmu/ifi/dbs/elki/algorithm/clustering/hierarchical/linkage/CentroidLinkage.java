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
 * Centroid linkage &mdash; Unweighted Pair-Group Method using Centroids
 * (UPGMC).
 * <p>
 * This is closely related to {@link GroupAverageLinkage} (UPGMA), but the
 * resulting distance corresponds to the distance of the cluster centroids when
 * used with squared Euclidean distance.
 * <p>
 * For Lance-Williams, we can then obtain the following recursive definition:
 * \[d_{\text{UPGMC}}(A\cup B,C)=\tfrac{|A|}{|A|+|B|} d(A,C) +
 * \tfrac{|B|}{|A|+|B|} d(B,C) - \tfrac{|A|\cdot|B|}{(|A|+|B|)^2} d(A,B)\]
 * <p>
 * With <em>squared</em> Euclidean distance, we then get the cluster distance:
 * \[d_{\text{UPGMC}}(A,B)=||\tfrac{1}{|A|}\sum\nolimits_{a\in A} a,
 * \tfrac{1}{|B|}\sum\nolimits_{b\in B} b||^2\]
 * but for other distances, this will not generally be true.
 * <p>
 * Because the ELKI implementations use Lance-Williams, this linkage should only
 * be used with (squared) Euclidean distance.
 * <p>
 * While titled "unweighted", this method does take cluster sizes into account
 * when merging clusters with Lance-Williams.
 * <p>
 * While the idea of this method &mdash; at least for squared Euclidean &mdash;
 * is compelling (distance of cluster centers), it is not as well behaved as one
 * may think. It can yield so called "inversions", where a later merge has a
 * smaller distance than an early merge, because a cluster center <em>can</em>
 * be closer to a neighboring cluster than any of the individual points. Because
 * of this, the {@link GroupAverageLinkage} (UPGMA) is usually preferable.
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
@Alias({ "centroid", "upgmc", "de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.CentroidLinkageMethod" })
public class CentroidLinkage implements Linkage {
  /**
   * Static instance of class.
   */
  public static final CentroidLinkage STATIC = new CentroidLinkage();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public CentroidLinkage() {
    super();
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    final double f = 1. / (sizex + sizey);
    return (sizex * dx + sizey * dy - (sizex * sizey) * f * dxy) * f;
  }

  /**
   * Class parameterizer.
   * <p>
   * Returns the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected CentroidLinkage makeInstance() {
      return STATIC;
    }
  }
}
