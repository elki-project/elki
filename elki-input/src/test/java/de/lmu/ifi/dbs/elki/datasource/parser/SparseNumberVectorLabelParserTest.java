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
package de.lmu.ifi.dbs.elki.datasource.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.LabelList;
import de.lmu.ifi.dbs.elki.data.SparseDoubleVector;
import de.lmu.ifi.dbs.elki.data.SparseNumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.InputStreamDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the sparse number vector format parser filter.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class SparseNumberVectorLabelParserTest extends AbstractDataSourceTest {
  @Test
  public void parameters() throws IOException {
    String filename = UNITTEST + "parsertest.sparse";
    Parser parser = new ELKIBuilder<>(SparseNumberVectorLabelParser.class) //
        .with(NumberVectorLabelParser.Parameterizer.VECTOR_TYPE_ID, SparseDoubleVector.Factory.class)//
        .build();
    MultipleObjectsBundle bundle;
    try (InputStream is = open(filename);
        InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, null, parser)) {
      bundle = dbc.loadData();
    }

    // Ensure that the filter has correctly formed the bundle.
    // We expect that the bundle's first column is a number vector field.
    // We expect that the bundle's second column is a LabelList

    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.SPARSE_VECTOR_VARIABLE_LENGTH.isAssignableFromType(bundle.meta(0)));
    assertTrue("Test file not as expected", TypeUtil.LABELLIST.isAssignableFromType(bundle.meta(1)));

    assertEquals("Length", 3, bundle.dataLength());
    assertEquals("Length", 4, ((SparseNumberVector) bundle.data(0, 0)).getDimensionality());

    // Ensure that the third column are the LabelList objects.
    assertEquals("Unexpected data type", SparseDoubleVector.class, bundle.data(0, 0).getClass());
    assertEquals("Unexpected data type", LabelList.class, bundle.data(0, 1).getClass());
  }
}
