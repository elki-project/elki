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
 * Weighted average linkage clustering method (WPGMA).
 * <p>
 * This is somewhat a misnomer, as it actually ignores that the clusters should
 * likely be weighted differently according to their size when computing the
 * average linkage. See {@link GroupAverageLinkage} for the UPGMA method
 * that uses the group size to weight the objects the same way.
 * Because of this, it is sometimes also called "simple average".
 * <p>
 * There does not appear to be a closed form distance for this clustering,
 * but it is only defined recursively on the previous clusters simply by
 * \[d_{\text{WPGMA}}(A\cup B,C):=\tfrac{1}{2}d(A,C)+\tfrac{1}{2}d(B,C)\]
 * <p>
 * {@link MedianLinkage} (WPGMC) is similar in the sense that it is ignoring
 * the cluster sizes, and therefore the weight of points decreases with the
 * depth of the tree. The method is "weighted" in the sense that the new members
 * get the weight adjusted to match the old cluster members.
 * <p>
 * Reference:
 * <p>
 * R. R. Sokal, C. D. Michener<br>
 * A statistical method for evaluating systematic relationship<br>
 * University of Kansas science bulletin, 28, 1409-1438. (1958)
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "R. R. Sokal, C. D. Michener", //
    title = "A statistical method for evaluating systematic relationship", //
    booktitle = "University of Kansas science bulletin 28", //
    url = "https://archive.org/details/cbarchive_33927_astatisticalmethodforevaluatin1902", //
    bibkey = "journals/kansas/SokalM1902")
@Alias({ "wpgma", "WPGMA", "de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.WeightedAverageLinkageMethod" })
@Priority(Priority.DEFAULT - 1)
public class WeightedAverageLinkage implements Linkage {
  /**
   * Static instance of class.
   */
  public static final WeightedAverageLinkage STATIC = new WeightedAverageLinkage();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public WeightedAverageLinkage() {
    super();
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    return .5 * (dx + dy);
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
    protected WeightedAverageLinkage makeInstance() {
      return STATIC;
    }
  }
}
