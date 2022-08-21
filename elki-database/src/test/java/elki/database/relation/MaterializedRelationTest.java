/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.database.relation;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Random;

import org.junit.Test;

import elki.data.NumberVector;
import elki.data.VectorUtil;
import elki.data.VectorUtil.SortDBIDsBySingleDimension;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.StaticArrayDatabase;
import elki.database.ids.ArrayModifiableDBIDs;
import elki.database.ids.DBIDArrayIter;
import elki.database.ids.DBIDUtil;
import elki.datasource.ArrayAdapterDatabaseConnection;

/**
 * Unit test that loads a data file and sorts it. This tests some key parts of
 * the database and ID layers.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 */
public class MaterializedRelationTest {
  @Test
  public void testSorting() throws IOException {
    Random rnd = new Random(0L);
    double[][] data = new double[1000][];
    for(int i = 0; i < data.length; i++) {
      // ints are intentional, to have duplicate values.
      data[i] = new double[] { rnd.nextDouble(), rnd.nextInt(100), rnd.nextDouble(), rnd.nextInt(10) };
    }
    Database db = new StaticArrayDatabase(new ArrayAdapterDatabaseConnection(data));
    db.initialize();
    Relation<? extends NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);

    ArrayModifiableDBIDs ids = DBIDUtil.newArray(rel.getDBIDs());

    int dims = RelationUtil.dimensionality(rel);
    SortDBIDsBySingleDimension sorter = new VectorUtil.SortDBIDsBySingleDimension(rel);

    for(int d = 0; d < dims; d++) {
      sorter.setDimension(d);
      ids.sort(sorter);
      assertEquals("Lost some DBID during sorting?!?", rel.size(), DBIDUtil.newHashSet(ids).size());

      DBIDArrayIter it = ids.iter();
      double prev = rel.get(it).doubleValue(d);
      for(it.advance(); it.valid(); it.advance()) {
        double next = rel.get(it).doubleValue(d);
        assertTrue("Not correctly sorted: " + prev + " > " + next + " at pos " + it.getOffset(), prev <= next);
        prev = next;
      }
    }
  }
}
