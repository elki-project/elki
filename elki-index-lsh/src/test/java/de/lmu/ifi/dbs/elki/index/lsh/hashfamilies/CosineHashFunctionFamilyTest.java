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
package de.lmu.ifi.dbs.elki.index.lsh.hashfamilies;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.MaterializedRelation;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.lsh.hashfunctions.LocalitySensitiveHashFunction;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for random hyperplane / cosine distance.
 *
 * @author Evgeniy Faerman
 * @since 0.7.0
 */
public class CosineHashFunctionFamilyTest extends AbstractSimpleAlgorithmTest {
  @Test
  public void testHashFunctionOneProjection() {
    LocalitySensitiveHashFunction<? super NumberVector> hashFunction = createCosineHashFunction(1);
    assertEquals(0, hashFunction.hashObject(DoubleVector.wrap(new double[] { 1, 1, 1, 1, 1 })));
    assertEquals(0, hashFunction.hashObject(DoubleVector.wrap(new double[] { 1, 1, 3, 1, 1 })));
  }

  @Test
  public void testHashFunctionTwoProjections() {
    LocalitySensitiveHashFunction<? super NumberVector> hashFunction = createCosineHashFunction(2);
    assertEquals(0, hashFunction.hashObject(DoubleVector.wrap(new double[] { 1, 1, 1, 1, 1 })));
    assertEquals(1, hashFunction.hashObject(DoubleVector.wrap(new double[] { 1, 1, 1, 1, 3 })));
  }

  private LocalitySensitiveHashFunction<? super NumberVector> createCosineHashFunction(int numberOfProjections) {
    ListParameterization params = new ListParameterization();
    params.addParameter(CosineHashFunctionFamily.Parameterizer.RANDOM_ID, 0L);
    params.addParameter(CosineHashFunctionFamily.Parameterizer.NUMPROJ_ID, numberOfProjections);
    CosineHashFunctionFamily cosineFamily = ClassGenericsUtil.parameterizeOrAbort(CosineHashFunctionFamily.class, params);
    LocalitySensitiveHashFunction<? super NumberVector> hashFunction = cosineFamily.generateHashFunctions(mockRelation(5), numberOfProjections).get(0);
    return hashFunction;
  }

  private Relation<NumberVector> mockRelation(final int dimension) {
    return new MaterializedRelation<>(VectorFieldTypeInformation.typeRequest(NumberVector.class, dimension, dimension), DBIDUtil.EMPTYDBIDS);
  }
}
