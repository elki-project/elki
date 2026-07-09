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
package elki.sequencemining;

import static org.junit.Assert.*;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.Test;

import elki.data.IntegerVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.relation.Relation;
import elki.database.StaticArrayDatabase;
import elki.datasource.InputStreamDatabaseConnection;
import elki.datasource.parser.SPMFTextFormatParser;
import elki.result.FrequentSubsequencesResult;
import elki.utilities.ELKIBuilder;
import static elki.sequencemining.GSP.Sequence;

/**
 * Unit test for GSP algorithm with IntegerVector input data.
 *
 * @author Schubert
 * @since 0.8.0
 */
public class GSPTest {
  /** Resource path to the controlled test data file in testFixtures. */
  private static final String CONTROLLED_TEST_DATA = "elki/testdata/unittests/controlled-gsp-sequence.txt";

  /**
   * Load a resource as an InputStream from the classpath.
   */
  private static InputStream loadResource(String path) throws IOException {
    URL url = GSPTest.class.getClassLoader().getResource(path);
    if(url == null) {
      throw new IOException("Resource not found: " + path);
    }
    return url.openStream();
  }

  /**
   * Create a Database from a byte stream using SPMFTextFormatParser.
   */
  private static Database makeDB(InputStream is, int expectedSize) throws IOException {
    SPMFTextFormatParser parser = new ELKIBuilder<>(SPMFTextFormatParser.class).build();
    InputStreamDatabaseConnection dbc = new InputStreamDatabaseConnection(is, null, parser);
    Database db = new StaticArrayDatabase(dbc);
    db.initialize();
    Relation<IntegerVector> relation = db.getRelation(TypeUtil.INTEGER_VECTOR);
    assertNotNull("Database should have an IntegerVector relation", relation);
    assertEquals("Expected " + expectedSize + " sequences but got " + relation.getDBIDs().size(),
                 expectedSize, relation.getDBIDs().size());
    return db;
  }

  /**
   * Test controlled dataset with a known length-4 target pattern.
   * The file has one sequence of length 4 appearing 4 times, with many
   * distractor items (each <= 3 occurrences) and 3 unique distractor sequences.
   */
  @Test
  public void testControlledDataset() throws IOException {
    InputStream is = loadResource(CONTROLLED_TEST_DATA);
    Database db = makeDB(is, 12);

    // Target: [4, -1, 3, -1, 2, -1, 1] should be found with support=4 (minSupp=4)
    List<Sequence> result = runGSP(db, 4, Integer.MAX_VALUE);
    assertTrue("Should find target pattern", !result.isEmpty());

    boolean foundTarget = false;
    for(Sequence seq : result) {
      if(seq.values.length == 7 && seq.values[0] == 4 && seq.values[1] == GSP.TIME_STEP &&
        seq.values[2] == 3 && seq.values[3] == GSP.TIME_STEP &&
        seq.values[4] == 2 && seq.values[5] == GSP.TIME_STEP &&
        seq.values[6] == 1) {
        foundTarget = true;
        break;
      }
    }
    assertTrue("Should find [4, 3, 2, 1]", foundTarget);

    // A few noisy singleton values stay below support 4 in the fixture.
    assertFalse("[50] should not be frequent at minSupp=4", anyContains(result, new int[]{50}));
    assertFalse("[90] should not be frequent at minSupp=4", anyContains(result, new int[]{90}));
    assertFalse("[95] should not be frequent at minSupp=4", anyContains(result, new int[]{95}));
  }

  /**
   * Test basic sequence mining with sequences of length >= 3.
   */
  @Test
  public void testBasic() throws IOException {
    String data = "1 2 3 -2\n2 3 4 -2\n3 4 5 -2\n";

    Database db = makeDB(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), 3);
    List<Sequence> result = runGSP(db, 1, Integer.MAX_VALUE);

    assertTrue("Should have results", !result.isEmpty());

    boolean found1 = false, found12 = false, found123 = false;
    boolean found3 = false, found34 = false, found345 = false;
    for(Sequence seq : result) {
      if(seq.values.length == 1 && seq.values[0] == 1) found1 = true;
      if(seq.values.length == 2 && seq.values[0] == 1 && seq.values[1] == 2) found12 = true;
      if(seq.values.length == 3 && seq.values[0] == 1 && seq.values[1] == 2 && seq.values[2] == 3) found123 = true;
      if(seq.values.length == 1 && seq.values[0] == 3) found3 = true;
      if(seq.values.length == 2 && seq.values[0] == 3 && seq.values[1] == 4) found34 = true;
      if(seq.values.length == 3 && seq.values[0] == 3 && seq.values[1] == 4 && seq.values[2] == 5) found345 = true;
    }
    assertTrue("Should find [1]", found1);
    assertTrue("Should find [1,2]", found12);
    assertTrue("Should find [1,2,3]", found123);
    assertTrue("Should find [3]", found3);
    assertTrue("Should find [3,4]", found34);
    assertTrue("Should find [3,4,5]", found345);
  }

  /**
   * Test minimum support threshold filtering with length-3 sequences.
   */
  @Test
  public void testMinSupport() throws IOException {
    String data = "1 2 3 -2\n1 2 3 -2\n1 2 4 -2\n5 6 7 -2\n";

    Database db = makeDB(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), 4);

    // minSupport=3: [1] and [2] appear in 3 seqs
    List<Sequence> res3 = runGSP(db, 3, Integer.MAX_VALUE);
    assertTrue("Should find [1]", anyContains(res3, new int[]{1}));
    assertTrue("Should find [2]", anyContains(res3, new int[]{2}));
    assertFalse("[3] should NOT be frequent at minSupport=3", anyContains(res3, new int[]{3}));
    assertFalse("[4] should NOT be frequent at minSupp=3", anyContains(res3, new int[]{4}));

    // Verify that [1,2] is also found with support 3
    assertTrue("Should find [1,2] with support 3", anyContains(res3, new int[]{1, 2}));

    // minSupport=6: nothing frequent (max possible is 4 for [5])
    List<Sequence> res6 = runGSP(db, 6, Integer.MAX_VALUE);
    assertEquals("MinSupport=6 should find none", 0, res6.size());
  }

  /**
   * Test variable-length sequences and subsequence containment.
   */
  @Test
  public void testVariableLength() throws IOException {
    String data = "1 -2\n1 2 3 -2\n1 2 3 4 -2\n";

    Database db = makeDB(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), 3);
    List<Sequence> result = runGSP(db, 1, Integer.MAX_VALUE);

    boolean found1 = false, found12 = false, found123 = false, found1234 = false;
    for(Sequence seq : result) {
      if(seq.values.length == 1 && seq.values[0] == 1) found1 = true;
      if(seq.values.length == 2 && seq.values[0] == 1 && seq.values[1] == 2) found12 = true;
      if(seq.values.length == 3 && seq.values[0] == 1 && seq.values[1] == 2 && seq.values[2] == 3) found123 = true;
      if(seq.values.length == 4 && seq.values[0] == 1 && seq.values[1] == 2 && seq.values[2] == 3 && seq.values[3] == 4) found1234 = true;
    }
    assertTrue("Should find [1]", found1);
    assertTrue("Should find [1,2]", found12);
    assertTrue("Should find [1,2,3]", found123);
    assertTrue("Should find [1,2,3,4]", found1234);
  }

  /**
   * Test maximum length limit with length-5 sequence.
   */
  @Test
  public void testMaxLength() throws IOException {
    String data = "1 2 3 4 5 -2\n";

    Database db = makeDB(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), 1);

    // maxLength=2: only sequences up to length 2 should be returned
    List<Sequence> res2 = runGSP(db, 1, 2);
    int maxLenInResult = 0;
    for(Sequence seq : res2) {
      if(seq.values.length > maxLenInResult) maxLenInResult = seq.values.length;
    }
    assertTrue("maxLength=2: no sequence longer than 2", maxLenInResult <= 2);

    // maxLength=5: should find sequences up to length 5
    List<Sequence> res5 = runGSP(db, 1, 5);
    assertTrue("maxLength=5: should find sequences", !res5.isEmpty());
    assertTrue("maxLength=5: should find [1,2,3]", anyContains(res5, new int[]{1, 2, 3}));
  }

  /**
   * Test that multiple sequences are properly aggregated for length-3 subsequences.
   */
  @Test
  public void testMultiSequenceAggregation() throws IOException {
    String data = "1 2 3 -2\n1 2 3 -2\n1 2 4 -2\n5 6 7 -2\n";

    Database db = makeDB(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), 4);

    // At minSupport=2: [1], [2], [1,2] should all be frequent
    List<Sequence> res = runGSP(db, 2, Integer.MAX_VALUE);
    assertTrue("Should find [1] with support 3", anyContains(res, new int[]{1}));
    assertTrue("Should find [2] with support 3", anyContains(res, new int[]{2}));
    assertTrue("Should find [1,2] with support 3", anyContains(res, new int[]{1, 2}));

    // At minSupport=3: only [1], [2] and [1,2] remain frequent (support=3)
    List<Sequence> res3 = runGSP(db, 3, Integer.MAX_VALUE);
    assertTrue("Should find [1] with support 3", anyContains(res3, new int[]{1}));
    assertFalse("[3] should NOT be frequent at minSupport=3", anyContains(res3, new int[]{3}));
    assertFalse("[4] should NOT be frequent at minSupport=3", anyContains(res3, new int[]{4}));
  }

  /**
   * Test sequence mining with -1 as transaction separator.
   */
  @Test
  public void testSequenceMining() throws IOException {
    // Two sequences with -1 transaction separators:
    // Seq1: {1} {10} {2} {11} {12} {3}
    // Seq2: {4} {13} {1} {15} {16} {2} {17} {3}
    // Expected: [1, -1, 2, -1, 3] should be found as frequent (appears in both)
    String data = "1 -1 10 -1 2 -1 11 -1 12 -1 3 -2\n4 -1 13 -1 1 -1 15 -1 16 -1 2 -1 17 -1 3 -2\n";

    Database db = makeDB(new ByteArrayInputStream(data.getBytes(StandardCharsets.UTF_8)), 2);
    List<Sequence> result = runGSP(db, 1, Integer.MAX_VALUE);

    assertTrue("Should find sequence pattern", !result.isEmpty());
    boolean found = false;
    for(Sequence seq : result) {
      if(seq.values.length == 5 && seq.values[0] == 1 && seq.values[1] == -1 && seq.values[2] == 2 && seq.values[3] == -1 && seq.values[4] == 3) {
        found = true;
        break;
      }
    }
    assertTrue("Should find [1, -1, 2, -1, 3]", found);
  }

  private static List<Sequence> runGSP(Database db, int minSupp, int maxLength) {
    FrequentSubsequencesResult result = new ELKIBuilder<>(GSP.class)
        .with(GSP.Par.MINSUPP_ID, minSupp)
        .with(GSP.Par.MAXLENGTH_ID, maxLength)
        .build().autorun(db);
    return result.getSubsequences();
  }

  /**
   * Check if result contains a sequence with the given values.
   */
  private boolean anyContains(List<Sequence> result, int[] expected) {
    for(Sequence seq : result) {
      if(seq.values.length == expected.length) {
        boolean match = true;
        for(int i = 0; i < expected.length; i++) {
          if(seq.values[i] != expected[i]) {
            match = false;
            break;
          }
        }
        if(match) return true;
      }
    }
    return false;
  }
}
