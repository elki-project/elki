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
package elki.outlier.lof;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Random;

import org.junit.Test;

import elki.data.DoubleVector;
import elki.data.VectorUtil;
import elki.data.type.TypeUtil;
import elki.database.HashmapDatabase;
import elki.database.UpdatableDatabase;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDUtil;
import elki.database.ids.DBIDs;
import elki.database.relation.DoubleRelation;
import elki.database.relation.Relation;
import elki.database.relation.RelationUtil;
import elki.datasource.FileBasedDatabaseConnection;
import elki.datasource.bundle.MultipleObjectsBundle;
import elki.distance.CosineDistance;
import elki.distance.minkowski.EuclideanDistance;
import elki.index.KNNIndex;
import elki.outlier.AbstractOutlierAlgorithmTest;
import elki.result.Metadata;
import elki.result.Metadata.Hierarchy;
import elki.result.outlier.OutlierResult;
import elki.utilities.ELKIBuilder;

/**
 * Tests the OnlineLOF algorithm. Compares the result of the static LOF
 * algorithm to the result of the OnlineLOF algorithm, where some insertions and
 * deletions (of the previously inserted objects) have been applied to the
 * database.
 *
 * @author Elke Achtert
 * @since 0.7.0
 */
public class OnlineLOFTest extends AbstractOutlierAlgorithmTest {
  @Test
  public void testOnlineLOF() throws IOException {
    UpdatableDatabase db = new ELKIBuilder<>(HashmapDatabase.class) //
        .with(FileBasedDatabaseConnection.Par.INPUT_ID, //
            getClass().getClassLoader().getResource(UNITTEST + "3clusters-and-noise-2d.csv")) //
        .build();
    // Initialize database.
    db.initialize();
    Relation<DoubleVector> rep = db.getRelation(TypeUtil.DOUBLE_VECTOR_FIELD);
    final int k = 5;

    // 1. Run LOF on original data:
    FlexibleLOF<DoubleVector> lof = new FlexibleLOF<>(k, k, EuclideanDistance.STATIC, CosineDistance.STATIC);
    DoubleRelation scores1 = lof.autorun(db).getScores();
    Hierarchy h = Metadata.hierarchyOf(rep);
    h.iterChildren().filter(KNNIndex.class).forEach(h::removeChild);

    // 2. Run OnlineLOF
    OnlineLOF<DoubleVector> onlinelof = new OnlineLOF<>(k, k, EuclideanDistance.STATIC, CosineDistance.STATIC);
    OutlierResult result = onlinelof.autorun(db);

    // prepare synthetic new objects
    ArrayList<DoubleVector> insertions = new ArrayList<>();
    int dim = RelationUtil.dimensionality(rep);
    Random random = new Random(5);
    for(int i = 0; i < 50; i++) {
      insertions.add(VectorUtil.randomVector(DoubleVector.FACTORY, dim, random));
    }
    // insert objects
    DBIDs deletions = db.insert(MultipleObjectsBundle.makeSimple(rep.getDataTypeInformation(), insertions));

    // delete objects
    db.delete(deletions);

    // Get final OnlineLOF scores.
    DoubleRelation scores2 = result.getScores();

    // 3. Compare results
    for(DBIDIter id = scores1.getDBIDs().iter(); id.valid(); id.advance()) {
      double lof1 = scores1.doubleValue(id), lof2 = scores2.doubleValue(id);
      assertEquals("lof(" + DBIDUtil.toString(id) + ") != lof(" + DBIDUtil.toString(id) + "): " + lof1 + " != " + lof2, lof1, lof2, 1e-10);
    }
  }
}
