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

import net.jafama.FastMath;

/**
 * Ward's method clustering method.
 * <p>
 * This criterion minimizes the <em>increase</em> of squared errors, and should
 * be used with <em>squared Euclidean</em> distance. Usually, ELKI will try to
 * automatically square distances when you combine this with Euclidean distance.
 * For performance reasons, the direct use of squared distances is preferable!
 * <p>
 * The distance of two clusters in this method is:
 * \[ d_{\text{Ward}}(A,B):=\text{SSE}(A\cup B)-\text{SSE}(A)-\text{SSE}(B) \]
 * where the sum of squared errors is defined as:
 * \[ \text{SSE}(X):=\sum\nolimits_{x\in X} (x-\mu_X)^2 \qquad \text{with }
 * \mu_X=\tfrac{1}{|X|}\sum\nolimits_{x\in X} X \]
 * This objective can be rewritten to
 * \[ d_{\text{Ward}}(A,B):=\tfrac{|A|\cdot|B|}{|A|+|B|} ||\mu_A-\mu_B||^2
 * = \tfrac{1}{1/|A|+1/|B|} ||\mu_A-\mu_B||^2 \]
 * <p>
 * For Lance-Williams, we can then obtain the following recursive definition:
 * \[d_{\text{Ward}}(A\cup B,C)=\tfrac{|A|+|C|}{|A|+|B|+|C|} d(A,C) +
 * \tfrac{|B|+|C|}{|A|+|B|+|C|} d(B,C) - \tfrac{|C|}{|A|+|B|+|C|} d(A,B)\]
 * <p>
 * These transformations rely on properties of the L2-norm, so they cannot be
 * used with arbitrary metrics, unless they are equivalent to the L2-norm in
 * some transformed space.
 * <p>
 * Because the resulting distances are squared, when used with a non-squared
 * distance, ELKI implementations will apply the square root before returning
 * the final result. This is statistically somewhat questionable, but usually
 * yields more interpretable distances that &mdash; roughly &mdash; correspond
 * to the increase in standard deviation. With ELKI, you can get both behavior:
 * Either choose squared Euclidean distance, or regular Euclidean distance.
 * <p>
 * This method is also referred to as "minimize increase of sum of squares"
 * (MISSQ) by Podani.
 * <p>
 * Reference:
 * <p>
 * J. H. Ward Jr.<br>
 * Hierarchical grouping to optimize an objective function<br>
 * Journal of the American statistical association 58.301
 * <p>
 * The formulation using Lance-Williams equations is due to:
 * <p>
 * D. Wishart<br>
 * 256. Note: An Algorithm for Hierarchical Classifications<br>
 * Biometrics 25(1)
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "J. H. Ward Jr.", //
    title = "Hierarchical grouping to optimize an objective function", //
    booktitle = "Journal of the American statistical association 58.301", //
    url = "https://doi.org/10.1080/01621459.1963.10500845", //
    bibkey = "doi:10.1080/01621459.1963.10500845")
@Reference(authors = "D. Wishart", //
    title = "256. Note: An Algorithm for Hierarchical Classifications", //
    booktitle = "BBiometrics 25(1)", //
    url = "https://doi.org/10.2307/2528688", //
    bibkey = "doi:10.2307/2528688")
@Alias({ "ward", "MISSQ", "de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.WardLinkageMethod" })
@Priority(Priority.IMPORTANT + 1)
public class WardLinkage implements Linkage {
  /**
   * Static instance of class.
   */
  public static final WardLinkage STATIC = new WardLinkage();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public WardLinkage() {
    super();
  }

  @Override
  public double initial(double d, boolean issquare) {
    return .5 * (issquare ? d : (d * d));
  }

  @Override
  public double restore(double d, boolean issquare) {
    return issquare ? 2. * d : FastMath.sqrt(2. * d);
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    return ((sizex + sizej) * dx + (sizey + sizej) * dy - sizej * dxy) / (double) (sizex + sizey + sizej);
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
    protected WardLinkage makeInstance() {
      return STATIC;
    }
  }
}
