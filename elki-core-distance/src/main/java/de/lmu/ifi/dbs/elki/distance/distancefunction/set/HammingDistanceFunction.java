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
import de.lmu.ifi.dbs.elki.distance.distancefunction.NumberVectorDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;

/**
 * Computes the Hamming distance of arbitrary vectors - i.e. counting, on how
 * many places they differ.
 * <p>
 * Reference:
 * <p>
 * R. W. Hamming<br>
 * Error detecting and error correcting codes<br>
 * Bell System technical journal, 29(2)
 * <p>
 * TODO: add a sparse (but not binary) optimized version?
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
@Reference(authors = "R. W. Hamming", //
    title = "Error detecting and error correcting codes", //
    booktitle = "Bell System technical journal, 29(2)", //
    url = "https://doi.org/10.1002/j.1538-7305.1950.tb00463.x", //
    bibkey = "doi:10.1002/j.1538-7305.1950.tb00463.x")
public class HammingDistanceFunction extends AbstractSetDistanceFunction<FeatureVector<?>> implements NumberVectorDistanceFunction<FeatureVector<?>> {
  /**
   * Static instance.
   */
  public static final HammingDistanceFunction STATIC = new HammingDistanceFunction();

  @Override
  public boolean isMetric() {
    return true;
  }

  @Override
  public double distance(FeatureVector<?> o1, FeatureVector<?> o2) {
    if(o1 instanceof BitVector && o2 instanceof BitVector) {
      return ((BitVector) o1).hammingDistance((BitVector) o2);
    }
    if(o1 instanceof NumberVector && o2 instanceof NumberVector) {
      return hammingDistanceNumberVector((NumberVector) o1, (NumberVector) o2);
    }
    final int d1 = o1.getDimensionality(), d2 = o2.getDimensionality();
    int differences = 0;
    int d = 0;
    for(; d < d1 && d < d2; d++) {
      Object v1 = o1.getValue(d), v2 = o2.getValue(d);
      final boolean n1 = isNull(v1), n2 = isNull(v2);
      if(n1 && n2) {
        continue;
      }
      if(v1 instanceof Double && Double.isNaN((Double) v1)) {
        continue;
      }
      if(v2 instanceof Double && Double.isNaN((Double) v2)) {
        continue;
      }
      // One must be set.
      if(n1 || n2 || !v1.equals(v2)) {
        ++differences;
      }
    }
    for(; d < d1; d++) {
      Object v1 = o1.getValue(d);
      if(!isNull(v1)) {
        if(v1 instanceof Double && Double.isNaN((Double) v1)) {
          continue;
        }
        ++differences;
      }
    }
    for(; d < d2; d++) {
      Object v2 = o2.getValue(d);
      if(!isNull(v2)) {
        if(v2 instanceof Double && Double.isNaN((Double) v2)) {
          continue;
        }
        ++differences;
      }
    }
    return differences;
  }

  @Override
  public double distance(NumberVector o1, NumberVector o2) {
    if(o1 instanceof BitVector && o2 instanceof BitVector) {
      return ((BitVector) o1).hammingDistance((BitVector) o2);
    }
    return hammingDistanceNumberVector(o1, o2);
  }

  /**
   * Version for number vectors.
   * 
   * @param o1 First vector
   * @param o2 Second vector
   * @return hamming distance
   */
  private double hammingDistanceNumberVector(NumberVector o1, NumberVector o2) {
    final int d1 = o1.getDimensionality(), d2 = o2.getDimensionality();
    int differences = 0;
    int d = 0;
    for(; d < d1 && d < d2; d++) {
      double v1 = o1.doubleValue(d), v2 = o2.doubleValue(d);
      if(v1 != v1 || v2 != v2) { /* NaN */
        continue;
      }
      if(v1 != v2) {
        ++differences;
      }
    }
    for(; d < d1; d++) {
      double v1 = o1.doubleValue(d);
      if(v1 != 0. && v1 == v1 /* not NaN */) {
        ++differences;
      }
    }
    for(; d < d2; d++) {
      double v2 = o2.doubleValue(d);
      if(v2 != 0. && v2 == v2 /* not NaN */) {
        ++differences;
      }
    }
    return differences;
  }

  @Override
  public SimpleTypeInformation<? super FeatureVector<?>> getInputTypeRestriction() {
    return FeatureVector.TYPE;
  }

  @Override
  public boolean equals(Object obj) {
    return obj == this || (obj != null && this.getClass().equals(obj.getClass()));
  }

  @Override
  public int hashCode() {
    return getClass().hashCode();
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractParameterizer {
    @Override
    protected HammingDistanceFunction makeInstance() {
      return STATIC;
    }
  }
}
