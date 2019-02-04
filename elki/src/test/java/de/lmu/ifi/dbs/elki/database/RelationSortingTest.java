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
package de.lmu.ifi.dbs.elki.database;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.algorithm.AbstractSimpleAlgorithmTest;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.VectorUtil;
import de.lmu.ifi.dbs.elki.data.VectorUtil.SortDBIDsBySingleDimension;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.ids.ArrayModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;

/**
 * Unit test that loads a data file and sorts it. This tests some key parts of
 * the database and ID layers.
 * 
 * @author Erich Schubert
 * @since 0.7.0
 * 
 */
public class RelationSortingTest {
  public static final String filename = "elki/testdata/unittests/hierarchical-3d2d1d.csv";

  @Test
  public void testSorting() {
    Database db = AbstractSimpleAlgorithmTest.makeSimpleDatabase(filename, -1);
    Relation<? extends NumberVector> rel = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);

    ArrayModifiableDBIDs ids = DBIDUtil.newArray(rel.getDBIDs());
    final int size = rel.size();

    int dims = RelationUtil.dimensionality(rel);
    SortDBIDsBySingleDimension sorter = new VectorUtil.SortDBIDsBySingleDimension(rel);

    for(int d = 0; d < dims; d++) {
      sorter.setDimension(d);
      ids.sort(sorter);
      assertEquals("Lost some DBID during sorting?!?", size, DBIDUtil.newHashSet(ids).size());

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
