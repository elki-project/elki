package de.lmu.ifi.dbs.elki.distance.distancefunction.probabilistic;

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
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
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
 * TODO: support acceleration for sparse vectors
 * 
 * @author Erich Schubert
 * @since 0.7.0
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
    final int dim1 = fv1.getDimensionality(), dim2 = fv2.getDimensionality();
    final int mindim = (dim1 < dim2) ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      final double v = Math.sqrt(fv1.doubleValue(d)) - Math.sqrt(fv2.doubleValue(d));
      agg += v * v;
    }
    for(int d = mindim; d < dim1; d++) {
      agg += Math.abs(fv1.doubleValue(d));
    }
    for(int d = mindim; d < dim2; d++) {
      agg += Math.abs(fv2.doubleValue(d));
    }
    return MathUtil.SQRTHALF * Math.sqrt(agg);
  }

  @Override
  public double similarity(final NumberVector o1, final NumberVector o2) {
    // TODO: accelerate sparse!
    final int dim1 = o1.getDimensionality(), dim2 = o2.getDimensionality();
    final int mindim = (dim1 < dim2) ? dim1 : dim2;
    double agg = 0.;
    for(int d = 0; d < mindim; d++) {
      agg += Math.sqrt(o1.doubleValue(d) * o2.doubleValue(d));
    }
    return agg;
  }

  @Override
  public boolean isMetric() {
    return true; // as this equals Euclidean in sqrt space
  }

  @Override
  public <T extends NumberVector> DistanceSimilarityQuery<T> instantiate(Relation<T> database) {
    return new PrimitiveDistanceSimilarityQuery<>(database, this, this);
  }

  @Override
  public SimpleTypeInformation<? super NumberVector> getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH;
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
