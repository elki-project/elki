package de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2015
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * Ward's method clustering method.
 * 
 * This criterion minimizes variances, and makes most sense when used with
 * squared Euclidean distance, see
 * {@link de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction}
 * 
 * Reference:
 * <p>
 * Ward Jr, Joe H.<br />
 * Hierarchical grouping to optimize an objective function<br />
 * Journal of the American statistical association 58.301 (1963): 236-244.
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "J. H. Ward Jr", //
title = "Hierarchical grouping to optimize an objective function", //
booktitle = "Journal of the American statistical association 58.301", //
url = "http://dx.doi.org/10.1080/01621459.1963.10500845")
@Alias({ "ward", "variance" })
public class WardLinkageMethod implements LinkageMethod {
  /**
   * Static instance of class.
   */
  public static final WardLinkageMethod STATIC = new WardLinkageMethod();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public WardLinkageMethod() {
    super();
  }

  @Override
  public double combine(int sizex, double dx, int sizey, double dy, int sizej, double dxy) {
    final double wx = (sizex + sizej) / (double) (sizex + sizey + sizej);
    final double wy = (sizey + sizej) / (double) (sizex + sizey + sizej);
    final double beta = sizej / (double) (sizex + sizey + sizej);
    return wx * dx + wy * dy - beta * dxy;
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
    protected WardLinkageMethod makeInstance() {
      return STATIC;
    }
  }
}
