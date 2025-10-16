/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2025
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
package elki.clustering.kmedoids;

import java.util.ArrayList;

import org.junit.Test;

import elki.clustering.AbstractClusterAlgorithmTest;
import elki.clustering.kmeans.KMeans;
import elki.data.Clustering;
import elki.data.DoubleVector;
import elki.data.LabelList;
import elki.data.model.MedoidModel;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.ProxyDatabase;
import elki.database.ids.DBIDIter;
import elki.database.relation.MaterializedRelation;
import elki.database.relation.Relation;
import elki.utilities.ELKIBuilder;

/**
 * Performs a full PAM run, and compares the result with a clustering derived
 * from the data set labels. This test ensures that PAM's performance doesn't
 * unexpectedly drop on this data set (and also ensures that the algorithms
 * work, as a side effect).
 *
 * @author Andreas Lang
 */
public class LabeledOptimizedPAMTest extends AbstractClusterAlgorithmTest {
  @Test
  public void testKMedoidsPAMLabeled() {
    Database db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Clustering<MedoidModel> result = new ELKIBuilder<LabeledOptimizedPAM<DoubleVector>>(LabeledOptimizedPAM.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 0) //
        .build().autorun(db);
    assertFMeasure(db, result, 1);
    assertClusterSizes(result, new int[] { 200, 200, 200, 200, 200 });
  }

  @Test
  public void testKMedoidsPAM() {
    Database file_db = makeSimpleDatabase(UNITTEST + "different-densities-2d-no-noise.ascii", 1000);
    Relation<DoubleVector> relation = file_db.getRelation(TypeUtil.NUMBER_VECTOR_VARIABLE_LENGTH);
    Relation<LabelList> labelRelation = file_db.getRelation(TypeUtil.LABELLIST);
    MaterializedRelation<LabelList> newLabels = new MaterializedRelation<>(null, labelRelation.getDataTypeInformation(), labelRelation.getDBIDs());
    int i=0;
    for(DBIDIter it = labelRelation.getDBIDs().iter();it.valid();it.advance(),i++){
        if(i % 100 == 0){
          newLabels.insert(it, labelRelation.get(it));
        }else{
          ArrayList<String> l= new ArrayList<>();
          l.add("");
          newLabels.insert(it, LabelList.make(l));
        }
    }
    ArrayList<Relation<?>> relations= new ArrayList<>();
    relations.add(relation);
    relations.add(newLabels);

    ProxyDatabase db = new ProxyDatabase(relation.getDBIDs(), relations);
    Clustering<MedoidModel> result = new ELKIBuilder<LabeledPAM<DoubleVector>>(LabeledPAM.class) //
        .with(KMeans.K_ID, 5) //
        .with(KMeans.SEED_ID, 0) //
        .build().autorun(db);
    // FMeasure compares the non Labels with lables
    assertFMeasure(db, result, 0.332240);
    assertClusterSizes(result, new int[] { 199, 200, 200, 200, 201 });
  }
}
