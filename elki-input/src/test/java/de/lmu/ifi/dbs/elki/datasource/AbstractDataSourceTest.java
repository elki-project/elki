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
package de.lmu.ifi.dbs.elki.datasource;

import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.zip.GZIPInputStream;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.type.FieldTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.datasource.parser.NumberVectorLabelParser;

/**
 * Abstract base class for testing data sources.
 *
 * @author Erich Schubert
 * @since 0.7.5
 */
public abstract class AbstractDataSourceTest {
  /**
   * Base path for unit test files.
   */
  public final static String UNITTEST = "elki/testdata/input/";

  /**
   * Open a resource input stream. Use gzip if the name ends with .gz.
   * (Autodetection currently does not work on resource streams.)
   * 
   * @param filename resource name
   * @return Input stream
   * @throws IOException
   */
  public static InputStream open(String filename) throws IOException {
    InputStream is = AbstractDataSourceTest.class.getClassLoader().getResourceAsStream(filename);
    if(is == null) {
      throw new IOException("Resource not found: " + filename);
    }
    return filename.endsWith(".gz") ? new GZIPInputStream(is) : is;
  }

  /**
   * Read data as a bundle, as we do not yet have Database objects in this
   * module. Bundles don't have nearest neighbor search, so usually you will
   * want to use Database or Relation objects instead in real code.
   * 
   * @param filename Resource filename to load.
   * @param filterlist Filters
   * @return Bundle
   */
  protected static MultipleObjectsBundle readBundle(String filename, ObjectFilter... filterlist) {
    NumberVectorLabelParser<DoubleVector> parser = new NumberVectorLabelParser<>(DoubleVector.FACTORY);
    try (InputStream is = open(filename);
        InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, Arrays.asList(filterlist), parser)) {
      return dbc.loadData();
    }
    catch(IOException e) {
      throw new AssertionError(e); // Fail the test.
    }
  }

  /**
   * Type-checked get a value from a bundle.
   *
   * @param bundle Bundle
   * @param row Row
   * @param col Column
   * @param kind Type
   * @return Object
   */
  protected static <T> T get(MultipleObjectsBundle bundle, int row, int col, Class<T> kind) {
    Object obj = bundle.data(row, col);
    assertTrue("Unexpected data type", kind.isInstance(obj));
    return kind.cast(obj);
  }

  /**
   * Assert that we have a (vector-) field, and get the dimensionality.
   * 
   * @param bundle Bundle
   * @param col Column
   * @param type Type restriction of the column to check
   * @return Dimensionality
   */
  protected static int getFieldDimensionality(MultipleObjectsBundle bundle, int col, TypeInformation type) {
    SimpleTypeInformation<?> meta = bundle.meta(col);
    assertTrue("Column type not as expected", TypeUtil.NUMBER_VECTOR_FIELD.isAssignableFromType(meta));
    assertTrue("Expected a vector field", meta instanceof FieldTypeInformation);
    return ((FieldTypeInformation) meta).getDimensionality();
  }
}
