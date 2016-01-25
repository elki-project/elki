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
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Median-linkage clustering method: Weighted pair group method using centroids
 * (WPGMC).
 * 
 * Reference:
 * <p>
 * J.C. Gower<br/>
 * A comparison of some methods of cluster analysis<br/>
 * Biometrics (1967): 623-637.
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.3
 */
@Reference(authors = "J. C. Gower", //
title = "A comparison of some methods of cluster analysis", //
booktitle = "Biometrics (1967)", //
url = "http://www.jstor.org/stable/10.2307/2528417")
@Alias({ "wpgmc", "WPGMC", "weighted-centroid" })
public class MedianLinkageMethod implements LinkageMethod {
  /**
   * Static instance of class.
   */
  public static final MedianLinkageMethod STATIC = new MedianLinkageMethod();

  /**
   * Constructor.
   * 
   * @deprecated use the static instance {@link #STATIC} instead.
   */
  @Deprecated
  public MedianLinkageMethod() {
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
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected MedianLinkageMethod makeInstance() {
      return STATIC;
    }
  }
}
