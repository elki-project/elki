package de.lmu.ifi.dbs.elki.index.lsh.hashfamilies;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
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

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.VectorFieldTypeInformation;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDRef;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.index.lsh.hashfunctions.LocalitySensitiveHashFunction;
import de.lmu.ifi.dbs.elki.math.random.RandomFactory;
import de.lmu.ifi.dbs.elki.result.ResultHierarchy;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

public class CosineHashFunctionFamilyTest extends AbstractSimpleAlgorithmTest {

  @Test
  public void testHashFunctionOneProjection() {
    // test with {1,1,-1,1,-1}
    int numberOfProjections = 1;
    LocalitySensitiveHashFunction<? super NumberVector> hashFunction = createCosineHashFunction(numberOfProjections);
    assertEquals(1, hashFunction.hashObject(new DoubleVector(new double[] { 1, 1, 1, 1, 1 })));
    assertEquals(0, hashFunction.hashObject(new DoubleVector(new double[] { 1, 1, 3, 1, 1 })));
  }

  @Test
  public void testHashFunctionTwoProjections() {
    // test with {1,1,-1,1,-1}
    int numberOfProjections = 2;
    LocalitySensitiveHashFunction<? super NumberVector> hashFunction = createCosineHashFunction(numberOfProjections);
    assertEquals(3, hashFunction.hashObject(new DoubleVector(new double[] { 1, 1, 1, 1, 1 })));
    assertEquals(2, hashFunction.hashObject(new DoubleVector(new double[] { 1, 1, 1, 1, 3 })));
  }

  private LocalitySensitiveHashFunction<? super NumberVector> createCosineHashFunction(int numberOfProjections) {
    ListParameterization params = new ListParameterization();
    params.addParameter(CosineHashFunctionFamily.Parameterizer.RANDOM_ID, RandomFactory.get(3l));
    params.addParameter(CosineHashFunctionFamily.Parameterizer.NUMPROJ_ID, numberOfProjections);
    CosineHashFunctionFamily cosineFamily = ClassGenericsUtil.parameterizeOrAbort(CosineHashFunctionFamily.class, params);
    LocalitySensitiveHashFunction<? super NumberVector> hashFunction = cosineFamily.generateHashFunctions(mockRelation(5), numberOfProjections).get(0);
    return hashFunction;
  }

  private Relation<NumberVector> mockRelation(final int dimension) {
    return new Relation<NumberVector>() {

      @Override
      public ResultHierarchy getHierarchy() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void setHierarchy(ResultHierarchy hierarchy) {
        // TODO Auto-generated method stub

      }

      @Override
      public String getLongName() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public String getShortName() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public Database getDatabase() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public NumberVector get(DBIDRef id) {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public void set(DBIDRef id, NumberVector val) {
        // TODO Auto-generated method stub

      }

      @Override
      public void delete(DBIDRef id) {
        // TODO Auto-generated method stub

      }

      @Override
      public SimpleTypeInformation<NumberVector> getDataTypeInformation() {
        return VectorFieldTypeInformation.typeRequest(NumberVector.class, dimension, dimension);
      }

      @Override
      public DBIDs getDBIDs() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public DBIDIter iterDBIDs() {
        // TODO Auto-generated method stub
        return null;
      }

      @Override
      public int size() {
        // TODO Auto-generated method stub
        return 0;
      }
    };
  }

}
