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
 * Unit test for prior probability classification.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class PriorProbabilityClassifierTest extends AbstractSimpleAlgorithmTest {
  @Test
  public void testPriorResults() {
    ListParameterization params = new ListParameterization();
    params.addParameter(AbstractDatabaseConnection.Par.FILTERS_ID, ClassLabelFilter.class);
    params.addParameter(ClassLabelFilter.Par.CLASS_LABEL_INDEX_ID, 0);
    Database db = makeSimpleDatabase(UNITTEST + "3clusters-and-noise-2d.csv", 330, params);
    PriorProbabilityClassifier prior = new ELKIBuilder<>(PriorProbabilityClassifier.class).build();
    // Run:
    Relation<NumberVector> relation = db.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    Relation<ClassLabel> labels = db.getRelation(TypeUtil.CLASSLABEL);
    prior.buildClassifier(db, labels);
    int correct = 0;
    for(DBIDIter iter = relation.iterDBIDs(); iter.valid(); iter.advance()) {
      if(labels.get(iter).equals(prior.classify(relation.get(iter)))) {
        ++correct;
      }
    }
    assertEquals("Prior probability", 150 /* majority class size */, correct);
  }
}
