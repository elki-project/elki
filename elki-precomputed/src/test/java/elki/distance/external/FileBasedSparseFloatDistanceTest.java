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
package elki.distance.external;

import java.io.IOException;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.hierarchical.SLINK;
import elki.clustering.hierarchical.extraction.CutDendrogramByHeight;
import elki.data.Clustering;
import elki.data.model.DendrogramModel;
import elki.database.AbstractDatabase;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.DBID;
import elki.datasource.DBIDRangeDatabaseConnection;
import elki.datasource.parser.CSVReaderFormat;
import elki.utilities.ELKIBuilder;
import elki.utilities.io.FileUtil;

/**
 * Unit test for external distances.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class FileBasedSparseFloatDistanceTest extends AbstractClusterAlgorithmTest {
  final static String FILENAME = "elki/testdata/unittests/distance/AsciiDistanceMatrix.ascii";

  @Test
  public void testExternalDistance() throws IOException {
    Database db = new ELKIBuilder<>(StaticArrayDatabase.class) //
        .with(AbstractDatabase.Par.DATABASE_CONNECTION_ID, DBIDRangeDatabaseConnection.class) //
        .with(DBIDRangeDatabaseConnection.Par.COUNT_ID, 4) //
        .build();
    db.initialize();

    FileBasedSparseFloatDistance df = new FileBasedSparseFloatDistance(//
        new AsciiDistanceParser(CSVReaderFormat.DEFAULT_FORMAT), null, Float.POSITIVE_INFINITY);
    // We need to read from a resource, instead of a file.
    df.loadCache(4, FileUtil.openSystemFile(FILENAME));
    SLINK<DBID> slink = new SLINK<>(df);
    CutDendrogramByHeight clus = new CutDendrogramByHeight(slink, 0.5, false);
    Clustering<DendrogramModel> c = clus.run(db);

    testClusterSizes(c, new int[] { 2, 2 });
  }
}
