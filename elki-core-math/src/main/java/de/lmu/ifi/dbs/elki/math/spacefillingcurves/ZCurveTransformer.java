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
package de.lmu.ifi.dbs.elki.math.spacefillingcurves;

import java.util.Arrays;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;

/**
 * Class to transform a relation to its Z coordinates.
 * 
 * @author Erich Schubert
 * @since 0.5.0
 */
public class ZCurveTransformer {
  /**
   * Maximum values in each dimension.
   */
  private final double[] maxValues;

  /**
   * Minimum values in each dimension.
   */
  private final double[] minValues;

  /**
   * Dimensionality.
   */
  private final int dimensionality;

  /**
   * Constructor.
   * 
   * @param relation Relation to transform
   * @param ids IDs subset to process
   */
  public ZCurveTransformer(Relation<? extends NumberVector> relation, DBIDs ids) {
    this.dimensionality = RelationUtil.dimensionality(relation);
    this.minValues = new double[dimensionality];
    this.maxValues = new double[dimensionality];

    // Compute scaling of vector space
    Arrays.fill(minValues, Double.POSITIVE_INFINITY);
    Arrays.fill(maxValues, Double.NEGATIVE_INFINITY);
    for (DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
      NumberVector vector = relation.get(iter);
      for(int dim = 0; dim < dimensionality; ++dim) {
        double dimValue = vector.doubleValue(dim);
        minValues[dim] = Math.min(minValues[dim], dimValue);
        maxValues[dim] = Math.max(maxValues[dim], dimValue);
      }
    }
  }

  /**
   * Transform a single vector.
   * 
   * @param vector Vector to transform
   * @return Z curve value as byte array
   */
  public byte[] asByteArray(NumberVector vector) {
    final long[] longValueList = new long[dimensionality];

    for(int dim = 0; dim < dimensionality; ++dim) {
      final double minValue = minValues[dim];
      final double maxValue = maxValues[dim];
      double dimValue = vector.doubleValue(dim);

      dimValue = (dimValue - minValue) / (maxValue - minValue);
      longValueList[dim] = (long) (dimValue * (Long.MAX_VALUE));
    }

    final byte[] bytes = new byte[Long.SIZE * dimensionality * (Long.SIZE / Byte.SIZE)];
    int shiftCounter = 0;
    for(int i = 0; i < Long.SIZE; ++i) {
      for(int dim = 0; dim < dimensionality; ++dim) {
        long byteValue = longValueList[dim];

        int localShift = shiftCounter % Byte.SIZE;
        bytes[(bytes.length - 1) - (shiftCounter / Byte.SIZE)] |= ((byteValue >> i) & 0x01) << localShift;

        shiftCounter++;
      }
    }
    return bytes;
  }
}