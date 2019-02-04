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

import static org.junit.Assert.*;

import java.io.IOException;
import java.io.InputStream;

import org.junit.Test;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.SimpleClassLabel;
import de.lmu.ifi.dbs.elki.data.SparseDoubleVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.AbstractDataSourceTest;
import de.lmu.ifi.dbs.elki.datasource.InputStreamDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.utilities.ELKIBuilder;

/**
 * Test the ARFF format parser filter.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public class ArffParserTest extends AbstractDataSourceTest {
  @Test
  public void dense() throws IOException {
    String filename = UNITTEST + "parsertest.arff";
    Parser parser = new ELKIBuilder<>(ArffParser.class).build();
    MultipleObjectsBundle bundle;
    try (InputStream is = open(filename);
        InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, null, parser)) {
      bundle = dbc.loadData();
    }

    // Ensure that the filter has correctly formed the bundle.
    // We expect that the bundle's first column is a number vector field.
    // We expect that the bundle's second column is a LabelList

    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));
    assertTrue("Test file not as expected", TypeUtil.CLASSLABEL.isAssignableFromType(bundle.meta(1)));
    assertTrue("Test file not as expected", TypeUtil.LABELLIST.isAssignableFromType(bundle.meta(2)));
    assertTrue("Test file not as expected", TypeUtil.EXTERNALID.isAssignableFromType(bundle.meta(3)));

    assertEquals("Length", 11, bundle.dataLength());
    assertEquals("Length", 4, ((NumberVector) bundle.data(0, 0)).getDimensionality());

    // Dense missing values are supposed to be NaN
    NumberVector nv = (NumberVector) bundle.data(10, 0);
    assertTrue("Expected NaN for missing data", Double.isNaN(nv.doubleValue(1)));
    assertTrue("Expected NaN for missing data", Double.isNaN(nv.doubleValue(3)));

    // Ensure that the third column are the LabelList objects.
    assertEquals("Unexpected data type", DoubleVector.class, bundle.data(0, 0).getClass());
    assertEquals("Unexpected data type", SimpleClassLabel.class, bundle.data(0, 1).getClass());
  }

  @Test
  public void sparse() throws IOException {
    String filename = UNITTEST + "parsertest.sparse.arff";
    Parser parser = new ELKIBuilder<>(ArffParser.class).build();
    MultipleObjectsBundle bundle;
    try (InputStream is = open(filename);
        InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, null, parser)) {
      bundle = dbc.loadData();
    }

    // Ensure that the filter has correctly formed the bundle.
    // We expect that the bundle's first column is a number vector field.
    // We expect that the bundle's second column is a LabelList

    // Ensure the first column are the vectors.
    assertTrue("Test file not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(bundle.meta(0)));
    assertTrue("Test file not as expected", TypeUtil.CLASSLABEL.isAssignableFromType(bundle.meta(1)));

    assertEquals("Length", 2, bundle.dataLength());
    assertEquals("Length", 4, ((NumberVector) bundle.data(0, 0)).getDimensionality());

    // Sparse missing values are supposed to be 0.
    NumberVector nv = (NumberVector) bundle.data(1, 0);
    assertEquals("Not 0 for missing data", 0., nv.doubleValue(0), 0.);
    assertEquals("Not 0 for missing data", 0., nv.doubleValue(2), 0.);

    // Ensure that the third column are the LabelList objects.
    assertEquals("Unexpected data type", SparseDoubleVector.class, bundle.data(0, 0).getClass());
    assertEquals("Unexpected data type", SimpleClassLabel.class, bundle.data(0, 1).getClass());
  }
}
