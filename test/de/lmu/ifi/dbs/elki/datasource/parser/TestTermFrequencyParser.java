package de.lmu.ifi.dbs.elki.datasource.parser;

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

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

import java.util.ArrayList;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.filter.SparseVectorFieldFilter;
import de.lmu.ifi.dbs.elki.datasource.filter.normalization.TFIDFNormalization;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ArcCosineDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SparseEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test the term frequency parser by loading an example data set derived
 * from DBLP.
 * 
 * TODO: maybe also run an example algorithm?
 * 
 * @author Erich Schubert
 */
public class TestTermFrequencyParser implements JUnit4Test {
  /** Test data set to use. */
  public static String DBLP_DATA = "data/testdata/parsing/termfreq-dblp.ascii.gz";

  @Test
  public void testDBLPData() {
    ListParameterization config = new ListParameterization();
    config.addParameter(AbstractDatabaseConnection.Parameterizer.PARSER_ID, TermFrequencyParser.class);
    config.addParameter(FileBasedDatabaseConnection.Parameterizer.INPUT_ID, DBLP_DATA);

    ArrayList<Object> filters = new ArrayList<>();
    filters.add(TFIDFNormalization.class);
    // Note: this filter is needed for the non-sparse Euclidean distance below.
    filters.add(SparseVectorFieldFilter.class);
    config.addParameter(AbstractDatabaseConnection.Parameterizer.FILTERS_ID, filters);

    Database db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, config);

    if (config.hasUnusedParameters()) {
      fail("Unused parameters: " + config.getRemainingParameters());
    }
    if (config.hasErrors()) {
      config.logAndClearReportedErrors();
      fail("Parameterization errors.");
    }

    db.initialize();

    Relation<SparseNumberVector<?>> rel = db.getRelation(TypeUtil.SPARSE_VECTOR_FIELD);

    // Get first three objects:
    DBIDIter iter = rel.iterDBIDs();
    SparseNumberVector<?> v1 = rel.get(iter);
    iter.advance();
    SparseNumberVector<?> v2 = rel.get(iter);
    iter.advance();
    SparseNumberVector<?> v3 = rel.get(iter);

    // "Dense" euclidean distance:
    double euclid1_12 = EuclideanDistanceFunction.STATIC.doubleDistance(v1, v2);
    double euclid1_13 = EuclideanDistanceFunction.STATIC.doubleDistance(v1, v3);
    double euclid1_23 = EuclideanDistanceFunction.STATIC.doubleDistance(v2, v3);
    double euclid1_21 = EuclideanDistanceFunction.STATIC.doubleDistance(v2, v1);
    // Sparse euclidean distance:
    double euclid2_12 = SparseEuclideanDistanceFunction.STATIC.doubleDistance(v1, v2);
    double euclid2_13 = SparseEuclideanDistanceFunction.STATIC.doubleDistance(v1, v3);
    double euclid2_23 = SparseEuclideanDistanceFunction.STATIC.doubleDistance(v2, v3);
    double euclid2_21 = SparseEuclideanDistanceFunction.STATIC.doubleDistance(v2, v1);
    // (Auto-switching) angular distance:
    double arccos_12 = ArcCosineDistanceFunction.STATIC.doubleDistance(v1, v2);
    double arccos_13 = ArcCosineDistanceFunction.STATIC.doubleDistance(v1, v3);
    double arccos_23 = ArcCosineDistanceFunction.STATIC.doubleDistance(v2, v3);
    double arccos_21 = ArcCosineDistanceFunction.STATIC.doubleDistance(v2, v1);

    assertEquals("Euclidean self-distance is not 0.", 0., EuclideanDistanceFunction.STATIC.doubleDistance(v1, v1), Double.MIN_VALUE);
    assertEquals("Sparse Euclidean self-distance is not 0.", 0., SparseEuclideanDistanceFunction.STATIC.doubleDistance(v1, v1), Double.MIN_VALUE);
    assertEquals("Arccos self-distance is not 0.", 0., ArcCosineDistanceFunction.STATIC.doubleDistance(v1, v1), Double.MIN_VALUE);
    assertEquals("Euclidean distance not symmetric.", euclid1_12, euclid1_21, Double.MIN_VALUE);
    assertEquals("Sparse Euclidean distance not symmetric.", euclid2_12, euclid2_21, Double.MIN_VALUE);
    assertEquals("Arccos distance not symmetric.", arccos_12, arccos_21, Double.MIN_VALUE);

    assertEquals("Euclidean distance 1-2 not as expected.", 684.4165398352088, euclid1_12, 1e-20);
    assertEquals("Sparse Euclidean distance 1-2 not as expected.", 684.4165398352088, euclid2_12, 1e-20);
    assertEquals("Arccos distance 1-2 not as expected.", 0.1901934493141418, arccos_12, 1e-20);
    assertEquals("Euclidean distance 1-3 not as expected.", 654.9862593978594, euclid1_13, 1e-20);
    assertEquals("Sparse Euclidean distance 1-3 not as expected.", 654.9862593978594, euclid2_13, 1e-20);
    assertEquals("Arccos distance 1-3 not as expected.", 0.18654347641726107, arccos_13, 1e-20);
    assertEquals("Euclidean distance 2-3 not as expected.", 231.78653972998518, euclid1_23, 1e-20);
    assertEquals("Sparse Euclidean distance 2-3 not as expected.", 231.78653972998518, euclid2_23, 1e-20);
    assertEquals("Arccos distance 2-3 not as expected.", 0.11138352337990769, arccos_23, 1e-20);
  }
}
