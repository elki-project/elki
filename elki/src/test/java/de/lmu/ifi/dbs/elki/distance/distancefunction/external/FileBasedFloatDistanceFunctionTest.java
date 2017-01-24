/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2017
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
package de.lmu.ifi.dbs.elki.distance.distancefunction.external;

import java.io.IOException;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.SLINK;
import de.lmu.ifi.dbs.elki.algorithm.clustering.hierarchical.extraction.CutDendrogramByHeight;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.DendrogramModel;
import de.lmu.ifi.dbs.elki.database.AbstractDatabase;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.datasource.DBIDRangeDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.parser.CSVReaderFormat;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.io.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for external distances.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class FileBasedFloatDistanceFunctionTest extends AbstractSimpleAlgorithmTest {
  final static String FILENAME = "data/testdata/unittests/distance/AsciiDistanceMatrix.ascii";

  @Test
  public void testExternalDistance() throws IOException {
    ListParameterization params = new ListParameterization();
    params.addParameter(AbstractDatabase.Parameterizer.DATABASE_CONNECTION_ID, DBIDRangeDatabaseConnection.class);
    params.addParameter(DBIDRangeDatabaseConnection.Parameterizer.COUNT_ID, 4);

    Database db = ClassGenericsUtil.parameterizeOrAbort(StaticArrayDatabase.class, params);
    db.initialize();

    ListParameterization distparams = new ListParameterization();
    distparams.addParameter(FileBasedDoubleDistanceFunction.Parameterizer.MATRIX_ID, FILENAME);
    FileBasedFloatDistanceFunction df = new FileBasedFloatDistanceFunction(//
        new AsciiDistanceParser(CSVReaderFormat.DEFAULT_FORMAT), null);
    // We need to read from a resource, instead of a file.
    df.loadCache(FileUtil.openSystemFile(FILENAME));
    SLINK<DBID> slink = new SLINK<>(df);
    CutDendrogramByHeight clus = new CutDendrogramByHeight(slink, 0.5, false);
    Clustering<DendrogramModel> c = clus.run(db);

    testClusterSizes(c, new int[] { 2, 2 });
  }
}
