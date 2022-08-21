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
package elki.classification;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import elki.algorithm.AbstractSimpleAlgorithmTest;
import elki.data.ClassLabel;
import elki.data.NumberVector;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ids.DBIDIter;
import elki.database.relation.Relation;
import elki.datasource.AbstractDatabaseConnection;
import elki.datasource.filter.typeconversions.ClassLabelFilter;
import elki.utilities.ELKIBuilder;
import elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Unit test for KNN classification.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class KNNClassifierTest extends AbstractSimpleAlgorithmTest {
  @Test
  public void testKNNResults() {
    ListParameterization params = new ListParameterization();
    params.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, ClassLabelFilter.class);
    params.addParameter(ClassLabelFilter.Par.CLASS_LABEL_INDEX_ID, 0);
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330, params);
    Relation<NumberVector> relation = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    Relation<ClassLabel> labels = db.getRelation(TypeUtil.CLASSLABEL);
    // Build with k=1 with no train-test split, hence completely overfitting
    KNNClassifier<NumberVector> knn = new ELKIBuilder<KNNClassifier<NumberVector>>(KNNClassifier.class).build();
    knn.buildClassifier(db, labels);
    int correct = 0;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      if(labels.get(iter).equals(knn.classify(relation.get(iter)))) {
        ++correct;
      }
    }
    assertEquals("1NN on training not 100%", relation.size(), correct);

    // Build with k=1 with no train-test split, hence completely overfitting
    knn = new ELKIBuilder<KNNClassifier<NumberVector>>(KNNClassifier.class) //
        .with(KNNClassifier.Par.K_ID, 5) //
        .build();
    knn.buildClassifier(db, labels);
    correct = 0;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      if(labels.get(iter).equals(knn.classify(relation.get(iter)))) {
        ++correct;
      }
    }
    assertEquals("5NN accuracy changed.", 322, correct);
  }
}
