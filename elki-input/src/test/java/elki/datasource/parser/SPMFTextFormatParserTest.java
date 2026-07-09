/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2026
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
package elki.datasource.parser;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

import org.junit.Test;

import elki.data.IntegerVector;
import elki.data.type.TypeUtil;
import elki.datasource.AbstractDataSourceTest;
import elki.datasource.InputStreamDatabaseConnection;
import elki.datasource.bundle.BundleStreamSource;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.utilities.ELKIBuilder;

/**
 * Test the SPMF text format parser.
 */
public class SPMFTextFormatParserTest extends AbstractDataSourceTest {
  /**
   * Test from a file with mixed separators and comments.
   */
  @Test
  public void fileTest() throws IOException {
    String filename = UNITTEST + "parsertest.spmf";
    Parser parser = new ELKIBuilder<>(SPMFTextFormatParser.class).build();
    MultipleObjectsBundle bundle;
    try (InputStream is = open(filename);
        InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, null, parser)) {
      bundle = dbc.loadData();
    }

    // Metadata should indicate IntegerVector type.
    assertTrue("First column should be IntegerVector", TypeUtil.INTEGER_VECTOR.isAssignableFromType(bundle.meta(0)));

    // Should have 5 records (empty lines/records and comments are skipped).
    assertEquals("Should have 5 non-empty records", 5, bundle.dataLength());

    // Record 1: "1 2 3 -2" -> [1, 2, 3]
    IntegerVector v0 = (IntegerVector) bundle.data(0, 0);
    assertEquals("Record 0 dim", 3, v0.getDimensionality());
    assertEquals("Record 0 val 0", 1, v0.intValue(0));
    assertEquals("Record 0 val 1", 2, v0.intValue(1));
    assertEquals("Record 0 val 2", 3, v0.intValue(2));

    // Record 2: "4 5 -2" -> [4, 5]
    IntegerVector v1 = (IntegerVector) bundle.data(1, 0);
    assertEquals("Record 1 dim", 2, v1.getDimensionality());
    assertEquals("Record 1 val 0", 4, v1.intValue(0));
    assertEquals("Record 1 val 1", 5, v1.intValue(1));

    // Record 3: "6 -1 7 -2" -> [6, -1, 7] (with internal -1 separator)
    IntegerVector v2 = (IntegerVector) bundle.data(2, 0);
    assertEquals("Record 2 dim", 3, v2.getDimensionality());
    assertEquals("Record 2 val 0", 6, v2.intValue(0));
    assertEquals("Record 2 val 1", -1, v2.intValue(1));
    assertEquals("Record 2 val 2", 7, v2.intValue(2));

    // Record 4: "8 9 10 11 12 -2" -> [8, 9, 10, 11, 12]
    IntegerVector v3 = (IntegerVector) bundle.data(3, 0);
    assertEquals("Record 3 dim", 5, v3.getDimensionality());
    assertEquals("Record 3 val 0", 8, v3.intValue(0));
    assertEquals("Record 3 val 4", 12, v3.intValue(4));

    // Record 5: "13 14" (trailing newline = implicit end) -> [13, 14]
    IntegerVector v4 = (IntegerVector) bundle.data(4, 0);
    assertEquals("Record 4 dim", 2, v4.getDimensionality());
    assertEquals("Record 4 val 0", 13, v4.intValue(0));
    assertEquals("Record 4 val 1", 14, v4.intValue(1));

    // Check that we got variable-length records.
    assertFalse("Records should have variable length", v0.getDimensionality() == v1.getDimensionality());
  }

  /**
   * Test with raw byte stream: -2 as separator and trailing marker.
   */
  @Test
  public void rawStreamTest() throws IOException {
    // Three records separated by -2, ending with -2
    String input = "10 20 -2\n30 40 50 -2\n60 -2";
    byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
    InputStream is = new ByteArrayInputStream(bytes);

    Parser parser = new ELKIBuilder<>(SPMFTextFormatParser.class).build();
    try (
        InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, null, parser)) {
      MultipleObjectsBundle bundle = dbc.loadData();
      assertEquals("Should have 3 records", 3, bundle.dataLength());

      IntegerVector v0 = (IntegerVector) bundle.data(0, 0);
      assertEquals("Record 0 dim", 2, v0.getDimensionality());
      assertEquals("Record 0 val 0", 10, v0.intValue(0));
      assertEquals("Record 0 val 1", 20, v0.intValue(1));

      IntegerVector v1 = (IntegerVector) bundle.data(1, 0);
      assertEquals("Record 1 dim", 3, v1.getDimensionality());
      assertEquals("Record 1 val 2", 50, v1.intValue(2));

      IntegerVector v2 = (IntegerVector) bundle.data(2, 0);
      assertEquals("Record 2 dim", 1, v2.getDimensionality());
      assertEquals("Record 2 val 0", 60, v2.intValue(0));
    }
  }

  /**
   * Test that empty records are skipped, but -1 is preserved as a value.
   */
  @Test
  public void emptyRecordSkipTest() throws IOException {
    // First two lines have only -2 (empty record separator), so they produce no
    // records.
    // Third line has values without trailing -2 (emitted at end-of-line).
    String input = "-2\n-2 -2\n1 2 3\n";
    byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
    InputStream is = new ByteArrayInputStream(bytes);

    Parser parser = new ELKIBuilder<>(SPMFTextFormatParser.class).build();
    try (
        InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, null, parser)) {
      MultipleObjectsBundle bundle = dbc.loadData();
      assertEquals("Should have 1 record", 1, bundle.dataLength());

      IntegerVector v0 = (IntegerVector) bundle.data(0, 0);
      assertEquals("Record dim", 3, v0.getDimensionality());
      assertEquals("Val 0", 1, v0.intValue(0));
      assertEquals("Val 1", 2, v0.intValue(1));
      assertEquals("Val 2", 3, v0.intValue(2));
    }
  }

  /**
   * Test that -1 is preserved within a record.
   */
  @Test
  public void separatorInRecordTest() throws IOException {
    // -1 should be kept as a value, not treated as separator.
    String input = "1 -1 -1 4 -2\n";
    byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
    InputStream is = new ByteArrayInputStream(bytes);

    Parser parser = new ELKIBuilder<>(SPMFTextFormatParser.class).build();
    try (
        InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, null, parser)) {
      MultipleObjectsBundle bundle = dbc.loadData();
      assertEquals("Should have 1 record", 1, bundle.dataLength());

      IntegerVector v0 = (IntegerVector) bundle.data(0, 0);
      assertEquals("Record dim", 4, v0.getDimensionality());
      assertEquals("Val 0", 1, v0.intValue(0));
      assertEquals("Val 1", -1, v0.intValue(1));
      assertEquals("Val 2", -1, v0.intValue(2));
      assertEquals("Val 3", 4, v0.intValue(3));
    }
  }

  /**
   * Test comment handling.
   */
  @Test
  public void commentHandling() throws IOException {
    String input = "# full line comment\n1 2 3 -2\n";
    byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
    InputStream is = new ByteArrayInputStream(bytes);

    Parser parser = new ELKIBuilder<>(SPMFTextFormatParser.class).build();
    try (
        InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, null, parser)) {
      MultipleObjectsBundle bundle = dbc.loadData();
      assertEquals("Should have 1 record", 1, bundle.dataLength());

      IntegerVector v0 = (IntegerVector) bundle.data(0, 0);
      assertEquals("Record dim", 3, v0.getDimensionality());
      assertEquals("Val 0", 1, v0.intValue(0));
      assertEquals("Val 1", 2, v0.intValue(1));
      assertEquals("Val 2", 3, v0.intValue(2));
    }
  }

  /**
   * Test streaming API event sequence directly.
   */
  @Test
  public void streamingEventOrderTest() throws IOException {
    String input = "10 -2\n20 30 -2\n";
    byte[] bytes = input.getBytes(StandardCharsets.UTF_8);
    InputStream is = new ByteArrayInputStream(bytes);

    SPMFTextFormatParser parser = new ELKIBuilder<>(SPMFTextFormatParser.class).build();
    parser.initStream(is);

    // First event should be META_CHANGED (initial).
    assertEquals("First event", BundleStreamSource.Event.META_CHANGED, parser.nextEvent());

    // Second event: first record.
    assertEquals("Second event", BundleStreamSource.Event.NEXT_OBJECT, parser.nextEvent());
    IntegerVector v0 = (IntegerVector) parser.data(0);
    assertEquals("Record 0 dim", 1, v0.getDimensionality());
    assertEquals("Record 0 val 0", 10, v0.intValue(0));

    // Third event: second record.
    assertEquals("Third event", BundleStreamSource.Event.NEXT_OBJECT, parser.nextEvent());
    IntegerVector v1 = (IntegerVector) parser.data(0);
    assertEquals("Record 1 dim", 2, v1.getDimensionality());

    // Fourth event: final META_CHANGED (with updated dims).
    assertEquals("Fourth event", BundleStreamSource.Event.META_CHANGED, parser.nextEvent());

    // Fifth event: end of stream.
    assertEquals("Fifth event", BundleStreamSource.Event.END_OF_STREAM, parser.nextEvent());

    parser.cleanup();
  }
}
