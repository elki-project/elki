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

import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.CLINK;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.Priority;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Complete-linkage ("maximum linkage") clustering method.
 * <p>
 * The distance of two clusters is simply the maximum of all pairwise distances
 * between the two clusters.
 * <p>
 * The distance of two clusters is defined as:
 * \[d_{\max}(A,B):=\max_{a\in A}\max_{b\in B} d(a,b)\]
 * <p>
 * This can be computed recursively using:
 * \[d_{\max}(A\cup B,C) = \max(d(A,C), d(B,C))\]
 * <p>
 * Note that with similarity functions, one would need to use the minimum
 * instead to get the same effect.
 * <p>
 * The algorithm {@link CLINK} is a faster algorithm to find such clusterings,
 * but it is very much order dependent and tends to find worse solutions.
 * <p>
 * References:
 * <p>
 * This is attributed to different sources that are not easily verifiable. Lance
 * and Williams (1967) attribute the idea to Macnaughton-Smith, albeit he may
 * have suggested a divisive rather than agglomerative procedure, but Sørensen
 * may have used this already in 1948 (and is credited, e.g., by Johnson 1967).
 * <p>
 * T. Sørensen<br>
 * A method of establishing groups of equal amplitude in plant sociology based
 * on similarity of species and its application to analyses of the vegetation on
 * Danish commons<br>
 * Biologiske Skrifter 5 (4)
 * <p>
 * P. Macnaughton-Smith<br>
 * Some statistical and other numerical techniques for classifying
 * individuals<br>
 * Home Office Res. Rpt. No. 6, H.M.S.O., London
 * <p>
 * G. N. Lance, W. T. Williams<br>
 * A general theory of classificatory sorting strategies<br>
 * 1. Hierarchical systems<br>
 * The Computer Journal 9.4
 * <p>
 * S. C. Johnson<br>
 * Hierarchical clustering schemes<br>
 * Psychometrika 32
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Alias({ "complete", "clink", "complete-link", "farthest-neighbor", "de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.CompleteLinkageMethod" })
@Priority(Priority.IMPORTANT)
@Reference(authors = "T. Sørensen", //
    title = "A method of establishing groups of equal amplitude in plant sociology based on similarity of species and its application to analyses of the vegetation on Danish commons", //
    booktitle = "Biologiske Skrifter 5 (4)", //
    bibkey = "journals/misc/Sorensen48")
@Reference(authors = "P. Macnaughton-Smith", //
    title = "Some statistical and other numerical techniques for classifying individuals", //
    booktitle = "Home Office Res. Rpt. No. 6, H.M.S.O., London", //
    bibkey = "journals/misc/MacnaughtonSmith65")
@Reference(authors = "G. N. Lance, W. T. Williams", //
    title = "A general theory of classificatory sorting strategies 1. Hierarchical systems", //
    booktitle = "The Computer Journal 9.4", //
    url = "https://doi.org/10.1093/comjnl/9.4.373", //
    bibkey = "doi:10.1093/comjnl/9.4.373")
@Reference(authors = "S. C. Johnson", //
    title = "Hierarchical clustering schemes", //
    booktitle = "Psychometrika 32", //
    url = "https://doi.org/10.1007/BF02289588", //
    bibkey = "doi:10.1007/BF02289588")
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
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected CompleteLinkage makeInstance() {
      return STATIC;
    }
  }
}
