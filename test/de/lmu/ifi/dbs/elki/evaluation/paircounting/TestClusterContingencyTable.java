package de.lmu.ifi.dbs.elki.evaluation.paircounting;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2011
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
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.ByLabelClustering;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.TrivialAllInOne;
import de.lmu.ifi.dbs.elki.algorithm.clustering.trivial.TrivialAllNoise;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.Model;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.evaluation.paircounting.ClusterContingencyTable;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Validate {@link ClusterContingencyTable} with respect to its ability to compare
 * data clusterings.
 * 
 * @author Erich Schubert
 */
public class TestClusterContingencyTable implements JUnit4Test {
  // the following values depend on the data set used!
  String dataset = "data/testdata/unittests/hierarchical-3d2d1d.csv";

  // size of the data set
  int shoulds = 600;

  /**
   * Validate {@link ClusterContingencyTable} with respect to its ability to
   * compare data clusterings.
   * 
   * @throws ParameterException on errors.
   */
  @Test
  public void testCompareDatabases() {
    ListParameterization params = new ListParameterization();
    // Input
    params.addParameter(FileBasedDatabaseConnection.INPUT_ID, dataset);

    // get database
    Database db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, params);
    db.initialize();

    // verify data set size.
    Relation<?> rel = db.getRelation(TypeUtil.ANY);
    assertTrue(rel.size() == shoulds);

    // run all-in-one
    TrivialAllInOne allinone = new TrivialAllInOne();
    Clustering<Model> rai = allinone.run(db);

    // run all-in-noise
    TrivialAllNoise allinnoise = new TrivialAllNoise();
    Clustering<Model> ran = allinnoise.run(db);

    // run by-label
    ByLabelClustering bylabel = new ByLabelClustering();
    Clustering<?> rbl = bylabel.run(db);

    assertEquals(1.0, computeFMeasure(rai, rai, false), Double.MIN_VALUE);
    assertEquals(1.0, computeFMeasure(ran, ran, false), Double.MIN_VALUE);
    assertEquals(1.0, computeFMeasure(rbl, rbl, false), Double.MIN_VALUE);

    assertEquals(0.009950248756218905, computeFMeasure(ran, rbl, true), Double.MIN_VALUE);
    assertEquals(0.0033277870216306157, computeFMeasure(rai, ran, true), Double.MIN_VALUE);

    assertEquals(0.5 /* 0.3834296724470135 */, computeFMeasure(rai, rbl, false), Double.MIN_VALUE);
  }

  private double computeFMeasure(Clustering<?> c1, Clustering<?> c2, boolean noise) {
    ClusterContingencyTable ct = new ClusterContingencyTable(true, noise);
    ct.process(c1, c2);
    return ct.pairF1Measure();
  }
}