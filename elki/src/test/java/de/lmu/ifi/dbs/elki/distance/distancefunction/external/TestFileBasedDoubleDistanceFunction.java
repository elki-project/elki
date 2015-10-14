package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

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
import de.lmu.ifi.dbs.elki.JUnit4Test;
import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.SLINK;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.ExtractFlatClusteringFromHierarchy;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.datasource.DBIDRangeDatabaseConnection;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for external distances.
 *
 * @author Erich Schubert
 */
public class TestFileBasedDoubleDistanceFunction extends AbstractSimpleAlgorithmTest implements JUnit4Test {
  final static String FILENAME = "data/testdata/unittests/distance/AsciiDistanceMatrix.ascii";

  @Test
  public void testExternalDistance() {
    ListParameterization params = new ListParameterization();
    params.addParameter(AbstractDatabase.Parameterizer.DATABASE_CONNECTION_ID, DBIDRangeDatabaseConnection.class);
    params.addParameter(DBIDRangeDatabaseConnection.Parameterizer.COUNT_ID, 4);

    Database db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, params);
    db.initialize();

    ListParameterization distparams = new ListParameterization();
    distparams.addParameter(FileBasedDoubleDistanceFunction.Parameterizer.MATRIX_ID, FILENAME);
    FileBasedDoubleDistanceFunction df = ClassGenericsUtil.parameterizeOrAbort(FileBasedDoubleDistanceFunction.class, distparams);
    SLINK<DBID> slink = new SLINK<>(df);
    ExtractFlatClusteringFromHierarchy clus = new ExtractFlatClusteringFromHierarchy(slink, 0.5, false, false);
    Clustering<DendrogramModel> c = clus.run(db);

    testClusterSizes(c, new int[] { 2, 2 });
  }
}
