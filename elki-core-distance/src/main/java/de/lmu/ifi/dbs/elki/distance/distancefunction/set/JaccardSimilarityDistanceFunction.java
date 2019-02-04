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
package de.lmu.ifi.dbs.elki.distance.distancefunction.set;

import de.lmu.ifi.dbs.elki.data.BitVector;
import de.lmu.ifi.dbs.elki.data.FeatureVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.database.query.DistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.query.distance.PrimitiveDistanceSimilarityQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.similarityfunction.NormalizedPrimitiveSimilarityFunction;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;

/**
 * A flexible extension of Jaccard similarity to non-binary vectors.
 * <p>
 * Jaccard coefficient is commonly defined as \(\frac{A\cap B}{A\cup B}\).
 * <p>
 * We can extend this definition to non-binary vectors as follows:
 * \(\tfrac{|\{i\mid a_i = b_i\}|}{|\{i\mid a_i = 0 \wedge b_i = 0\}|}\)
 * <p>
 * For binary vectors, this will obviously be the same quantity. However, this
 * version is more useful for categorical data.
 * <p>
 * Reference:
 * <p>
 * P. Jaccard<br>
 * Distribution de la florine alpine dans la Bassin de Dranses et dans quelques
 * regiones voisines<br>
 * Bulletin del la Société Vaudoise des Sciences Naturelles
 * 
 * @author Erich Schubert
 * @since 0.6.0
 */
@Reference(authors = "P. Jaccard", //
    title = "Distribution de la florine alpine dans la Bassin de Dranses et dans quelques regiones voisines", //
    booktitle = "Bulletin del la Société Vaudoise des Sciences Naturelles", //
    url = "http://data.rero.ch/01-R241574160", //
    bibkey = "journals/misc/Jaccard1902")
@Alias("de.lmu.ifi.dbs.elki.distance.similarityfunction.JaccardPrimitiveSimilarityFunction")
public class JaccardSimilarityDistanceFunction extends AbstractSetDistanceFunction<FeatureVector<?>> implements NormalizedPrimitiveSimilarityFunction<FeatureVector<?>>, NumberVectorDistanceFunction<FeatureVector<?>>, PrimitiveDistanceFunction<FeatureVector<?>> {
  /**
   * Constructor. No parameters.
   */
  public JaccardSimilarityDistanceFunction() {
    super();
  }

  @Override
  public double similarity(FeatureVector<?> o1, FeatureVector<?> o2) {
    if(o1 instanceof BitVector && o2 instanceof BitVector) {
      return ((BitVector) o1).jaccardSimilarity((BitVector) o2);
    }
    if(o1 instanceof NumberVector && o2 instanceof NumberVector) {
      return similarityNumberVector((NumberVector) o1, (NumberVector) o2);
    }
    final int d1 = o1.getDimensionality(), d2 = o2.getDimensionality();
    int intersection = 0, union = 0;
    int d = 0;
    for(; d < d1 && d < d2; d++) {
      Object v1 = o1.getValue(d), v2 = o2.getValue(d);
      final boolean n1 = isNull(v1), n2 = isNull(v2);
      if(v1 instanceof Double && Double.isNaN((Double) v1) //
          || v2 instanceof Double && Double.isNaN((Double) v2)) {
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
      Object v1 = o1.getValue(d);
      if(!isNull(v1)) {
        if(v1 instanceof Double && Double.isNaN((Double) v1)) {
          continue;
        }
        ++union;
      }
    }
    for(; d < d2; d++) {
      Object v2 = o2.getValue(d);
      if(!isNull(v2)) {
        if(v2 instanceof Double && Double.isNaN((Double) v2)) {
          continue;
        }
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
  public static double similarityNumberVector(NumberVector o1, NumberVector o2) {
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
  public double distance(FeatureVector<?> o1, FeatureVector<?> o2) {
    return 1. - similarity(o1, o2);
  }

  @Override
  public double distance(NumberVector o1, NumberVector o2) {
    if(o1 instanceof BitVector && o2 instanceof BitVector) {
      return 1. - ((BitVector) o1).jaccardSimilarity((BitVector) o2);
    }
    return 1. - similarityNumberVector(o1, o2);
  }

  @Override
  public boolean isSymmetric() {
    return true;
  }

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public SimpleTypeInformation<? super FeatureVector<?>> getInputTypeRestriction() {
    return FeatureVector.TYPE;
  }

  @Override
  public <T extends FeatureVector<?>> DistanceSimilarityQuery<T> instantiate(Relation<T> relation) {
    return new PrimitiveDistanceSimilarityQuery<>(relation, this, this);
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()));
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }
}
