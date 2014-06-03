package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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
import de.lmu.ifi.dbs.elki.database.query.DistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.AbstractNumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.PrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.math.MathUtil;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Hellinger kernel / Hellinger distance are used with SIFT vectors, and also
 * known as Bhattacharyya distance / coefficient.
 * 
 * This distance is appropriate for histograms, and is equal to A) L1
 * normalizing the vectors and then B) using Euclidean distance. As this is
 * usually much faster, this is the recommended way of handling such data.
 * 
 * Reference:
 * <p>
 * E. Hellinger<br />
 * Neue Begründung der Theorie quadratischer Formen von unendlichvielen
 * Veränderlichen<br />
 * Journal für die reine und angewandte Mathematik
 * </p>
 * 
 * @author Erich Schubert
 */
@Reference(authors = "E. Hellinger", //
title = "Neue Begründung der Theorie quadratischer Formen von unendlichvielen Veränderlichen", //
booktitle = "Journal für die reine und angewandte Mathematik ", //
url = "http://resolver.sub.uni-goettingen.de/purl?GDZPPN002166941")
@Alias({ "hellinger", "bhattacharyya" })
public class HellingerDistanceFunction extends AbstractNumberVectorDistanceFunction implements PrimitiveSimilarityFunction<NumberVector> {
  /**
   * Static instance.
   */
  public static final HellingerDistanceFunction STATIC = new HellingerDistanceFunction();

  /**
   * Hellinger kernel. Use static instance {@link #STATIC}!
   */
  @Deprecated
  public HellingerDistanceFunction() {
    super();
  }

  @Override
  public double distance(final NumberVector fv1, final NumberVector fv2) {
    final int dim = dimensionality(fv1, fv2);
    double sum = 0.;
    for(int i = 0; i < dim; i++) {
      final double v = Math.sqrt(fv1.doubleValue(i)) - Math.sqrt(fv2.doubleValue(i));
      sum += v * v;
    }
    return MathUtil.SQRTHALF * Math.sqrt(sum);
  }

  @Override
  public double similarity(final NumberVector o1, final NumberVector o2) {
    final int dim = dimensionality(o1, o2);
    double sim = 0.;
    for(int i = 0; i < dim; i++) {
      sim += Math.sqrt(o1.doubleValue(i) * o2.doubleValue(i));
    }
    return sim;
  }

  @Override
  public boolean isMetric() {
    return true; // as this equals Euclidean in sqrt space
  }

  @Override
  public <T extends NumberVector> DistanceSimilarityQuery<T> instantiate(Relation<T> database) {
    return new PrimitiveDistanceSimilarityQuery<>(database, this, this);
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
    protected HellingerDistanceFunction makeInstance() {
      return HellingerDistanceFunction.STATIC;
    }
  }
}
