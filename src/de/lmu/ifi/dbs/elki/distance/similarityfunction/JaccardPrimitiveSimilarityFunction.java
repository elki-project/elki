package de.lmu.ifi.dbs.elki.distance.similarityfunction;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
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
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.query.DistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * A flexible extension of Jaccard similarity to non-binary vectors.
 * 
 * Jaccard coefficient is commonly defined as {@code |intersection|/|union|}.
 * 
 * We can extend this definition as follows:
 * 
 * {@code |non-zero and equal attributes|/|non-zero attributes|}.
 * 
 * For binary vectors, this will obviously be the same quantity. However, this
 * version is more useful for categorical data.
 * 
 * Reference:
 * <p>
 * P. Jaccard<br />
 * Étude comparative de la distribution florale dans une portion des Alpes et
 * des Jura<br />
 * Bulletin del la Société Vaudoise des Sciences Naturelles
 * </p>
 * 
 * TODO: add optimized implementations for binary vectors.
 * 
 * @author Erich Schubert
 * 
 * @param <O> Vector type
 */
@Reference(authors = "P. Jaccard", title = "Étude comparative de la distribution florale dans une portion des Alpes et des Jura", booktitle = "Bulletin del la Société Vaudoise des Sciences Naturelles")
public class JaccardPrimitiveSimilarityFunction<O extends FeatureVector<?>> extends AbstractPrimitiveSimilarityFunction<O, DoubleDistance> implements NormalizedPrimitiveSimilarityFunction<O>, PrimitiveDoubleDistanceFunction<O> {
  /**
   * Constants for checking null.
   */
  private static final Integer INTEGER_NULL = Integer.valueOf(0);

  /**
   * Constants for checking null.
   */
  private static final Double DOUBLE_NULL = Double.valueOf(0.);

  /**
   * Empty string.
   */
  private static final String STRING_NULL = "";

  /**
   * Constructor. No parameters.
   */
  public JaccardPrimitiveSimilarityFunction() {
    super();
  }

  @Override
  public double doubleSimilarity(O o1, O o2) {
    if(o1 instanceof NumberVector && o2 instanceof NumberVector) {
      return doubleSimilarityNumberVector((NumberVector<?>) o1, (NumberVector<?>) o2);
    }
    final int d1 = o1.getDimensionality(), d2 = o2.getDimensionality();
    int intersection = 0, union = 0;
    int d = 0;
    for(; d < d1 && d < d2; d++) {
      Object v1 = o1.getValue(d), v2 = o2.getValue(d);
      final boolean n1 = isNull(v1), n2 = isNull(v2);
      if(v1 instanceof Double && Double.isNaN((Double) v1)) {
        continue;
      }
      if(v2 instanceof Double && Double.isNaN((Double) v2)) {
        continue;
      }
      if(!n1 || !n2) {
        ++union;
        if(!n1 && v1.equals(v2)) {
          ++intersection;
        }
      }
    }
    for(; d < d1; d++) {
      if(!isNull(o1.getValue(d))) {
        ++union;
      }
    }
    for(; d < d2; d++) {
      if(!isNull(o2.getValue(d))) {
        ++union;
      }
    }
    return intersection / (double) union;
  }

  /**
   * Compute Jaccard similarity for two number vectors.
   * 
   * @param o1 First vector
   * @param o2 Second vector
   * @return Jaccard similarity
   */
  public static double doubleSimilarityNumberVector(NumberVector<?> o1, NumberVector<?> o2) {
    final int d1 = o1.getDimensionality(), d2 = o2.getDimensionality();
    int intersection = 0, union = 0;
    int d = 0;
    for(; d < d1 && d < d2; d++) {
      double v1 = o1.doubleValue(d), v2 = o2.doubleValue(d);
      if(v1 != v1 || v2 != v2) { // Skip NaNs.
        continue;
      }
      if(v1 != 0. || v2 != 0) {
        ++union;
        if(v1 == v2) {
          ++intersection;
        }
      }
    }
    for(; d < d1; d++) {
      if(o1.doubleValue(d) != 0) {
        ++union;
      }
    }
    for(; d < d2; d++) {
      if(o2.doubleValue(d) != 0) {
        ++union;
      }
    }
    return intersection / (double) union;
  }

  @Override
  public DoubleDistance similarity(O o1, O o2) {
    return new DoubleDistance(doubleSimilarity(o1, o2));
  }

  /**
   * Test a value for null.
   * 
   * TODO: delegate to {@link FeatureVector} instead?
   * 
   * @param val Value
   * @return true when null
   */
  private static boolean isNull(Object val) {
    return (val == null) || STRING_NULL.equals(val) || DOUBLE_NULL.equals(val) || INTEGER_NULL.equals(val);
  }

  @Override
  public DoubleDistance distance(O o1, O o2) {
    return new DoubleDistance(1. - doubleSimilarity(o1, o2));
  }

  @Override
  public double doubleDistance(O o1, O o2) {
    return 1. - doubleSimilarity(o1, o2);
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public DoubleDistance getDistanceFactory() {
    return DoubleDistance.FACTORY;
  }

  @Override
  public SimpleTypeInformation<? super O> getInputTypeRestriction() {
    return TypeUtil.FEATURE_VECTORS;
  }

  @Override
  public <T extends O> DistanceSimilarityQuery<T, DoubleDistance> instantiate(Relation<T> relation) {
    return new PrimitiveDistanceSimilarityQuery<>(relation, this, this);
  }
}
