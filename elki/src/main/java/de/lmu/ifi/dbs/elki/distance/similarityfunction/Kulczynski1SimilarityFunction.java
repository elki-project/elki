package de.lmu.ifi.dbs.elki.distance.similarityfunction;

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

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Kulczynski similarity 1.
 * 
 * Reference:
 * <p>
 * M.-M. Deza and E. Deza<br />
 * Dictionary of distances
 * </p>
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "M.-M. Deza and E. Deza", title = "Dictionary of distances", booktitle = "Dictionary of distances")
public class Kulczynski1SimilarityFunction extends AbstractVectorSimilarityFunction {
  /**
   * Static instance.
   */
  public static final Kulczynski1SimilarityFunction STATIC = new Kulczynski1SimilarityFunction();

  /**
   * Constructor.
   * 
   * @deprecated Use {@link #STATIC} instance instead.
   */
  @Deprecated
  public Kulczynski1SimilarityFunction() {
    super();
  }

  @Override
  public double similarity(NumberVector v1, NumberVector v2) {
    final int dim = AbstractNumberVectorDistanceFunction.dimensionality(v1, v2);
    double sumdiff = 0., summin = 0.;
    for (int i = 0; i < dim; i++) {
      double xi = v1.doubleValue(i), yi = v2.doubleValue(i);
      sumdiff += Math.abs(xi - yi);
      summin += Math.min(xi, yi);
    }
    return summin / sumdiff;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected Kulczynski1SimilarityFunction makeInstance() {
      return Kulczynski1SimilarityFunction.STATIC;
    }
  }
}
