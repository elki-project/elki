/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.clustering.hierarchical.linkage;

import elki.distance.minkowski.SquaredEuclideanDistance;
import elki.math.linearalgebra.VMath;
import elki.utilities.Alias;
import elki.utilities.Priority;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.Parameterizer;

/**
 * Group-average linkage clustering method (UPGMA).
 * <p>
 * This is a good default linkage to use with hierarchical clustering, as it
 * neither exhibits the single-link chaining effect, nor has the strong tendency
 * of complete linkage to split large clusters. It is also easy to understand,
 * and it can be used with arbitrary distances and similarity functions.
 * <p>
 * The distances of two clusters is defined as the between-group average
 * distance of two points $a$ and $b$, one from each cluster. It should be noted
 * that this is not the average distance within the resulting cluster, because
 * it does not take within-cluster distances into account.
 * <p>
 * The distance of two clusters in this method is:
 * \[d_{\text{UPGMA}}(A,B)=\tfrac{1}{|A|\cdot|B|}
 * \sum\nolimits_{a\in A}\sum\nolimits_{b\in B} d(a,b)\]
 * <p>
 * For Lance-Williams, we can then obtain the following recursive definition:
 * \[d_{\text{UPGMA}}(A\cup B,C)=\tfrac{|A|}{|A|+|B|} d(A,C) +
 * \tfrac{|B|}{|A|+|B|} d(B,C)\]
 * <p>
 * While the method is also called "Unweighted Pair Group Method with Arithmetic
 * mean", it uses weights in the Lance-Williams formulation that account for the
 * cluster size. It is unweighted in the sense that every point keeps the same
 * weight, whereas in {@link WeightedAverageLinkage} (WPGMA), the weight of
 * points effectively depends on the depth in the cluster tree.
 * <p>
 * Reference:
 * <p>
 * R. R. Sokal, C. D. Michener<br>
 * A statistical method for evaluating systematic relationship<br>
 * University of Kansas science bulletin, 28, 1409-1438.
 *
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "R. R. Sokal, C. D. Michener", //
    title = "A statistical method for evaluating systematic relationship", //
    booktitle = "University of Kansas science bulletin 28", //
    url = "https://archive.org/details/cbarchive_33927_astatisticalmethodforevaluatin1902", //
    bibkey = "journals/kansas/SokalM1958")
@Alias({ "upgma", "average", "average-link", "average-linkage", "UPGMA" })
@Priority(Priority.RECOMMENDED + 1)
public class GroupAverageLinkage implements GeometricLinkage {
  /**
   * Static instance of class.
   */
  public static final GroupAverageLinkage STATIC = new GroupAverageLinkage();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public GroupAverageLinkage() {
    super();
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    return (sizex * dx + sizey * dy) / (double) (sizex + sizey);
  }

  @Override
  public double[] merge(double[] x, int sizex, double[] y, int sizey) {
    return VMath.timesPlusTimes(x, sizex / (double) (sizex + sizey), y, sizey / (double) (sizex + sizey));
  }

  // Geometric linkages are for squared Euclidean only, really!
  @Override
  public double linkage(double[] x, int sizex, double[] y, int sizey) {
    return SquaredEuclideanDistance.STATIC.distance(x, y);
  }

  /**
   * Class parameterizer.
   * <p>
   * Returns the static instance.
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    @Override
    public GroupAverageLinkage make() {
      return STATIC;
    }
  }
}
