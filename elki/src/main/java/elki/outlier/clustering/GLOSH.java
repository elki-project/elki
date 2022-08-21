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
package elki.outlier.clustering;

import elki.clustering.hierarchical.extraction.HDBSCANHierarchyExtraction;
import elki.data.Clustering;
import elki.data.NumberVector;
import elki.data.type.TypeInformation;
import elki.data.type.TypeUtil;
import elki.database.Database;
import elki.database.datastore.WritableDoubleDataStore;
import elki.database.ids.DBIDIter;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.math.DoubleMinMax;
import elki.outlier.OutlierAlgorithm;
import elki.result.Metadata;
import elki.result.outlier.BasicOutlierScoreMeta;
import elki.result.outlier.OutlierResult;
import elki.result.outlier.OutlierScoreMeta;
import elki.utilities.datastructures.iterator.It;
import elki.utilities.documentation.Description;
import elki.utilities.documentation.Reference;
import elki.utilities.documentation.Title;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Global-Local Outlier Scores from Hierarchies.
 * <p>
 * The method for the outlier detection module unifies both
 * the global and local flavors of the outlier detection problem
 * into a single definition of an outlier detection measure.
 * <p>
 * Reference:
 * <p>
 * R. J. G. B. Campello, D. Moulavi, A. Zimek, J. Sander<br>
 * Hierarchical Density Estimates for Data Clustering, Visualization, and
 * Outlier Detection<br>
 * ACM Trans. Knowl. Discov. Data 10(1)
 * 
 * @author Braulio V.S. Vinces (ELKIfication)
 * @since 0.8.0
 */
@Title("GLOSH: Global-Local Outlier Scores from Hierarchies")
@Description("The GLOSH outlier score assigned is based on a sound statistical interpretation from the HDBSCAN hierarchy.")
@Reference(authors = "R. J. G. B. Campello, D. Moulavi, A. Zimek, J. Sander", //
    title = "Hierarchical Density Estimates for Data Clustering, Visualization, and Outlier Detection", //
    booktitle = "ACM Trans. Knowl. Discov. Data 10(1)", //
    url = "https://doi.org/10.1145/2733381", //
    bibkey = "DBLP:journals/tkdd/CampelloMZS15")
public class GLOSH implements OutlierAlgorithm {
  /**
   * Inner algorithm.
   */
  private HDBSCANHierarchyExtraction hdbscanExtraction;

  /**
   * Constructor with parameters.
   * 
   * @param hdbscanExtraction HDBSCAN* extraction to use
   */
  public GLOSH(HDBSCANHierarchyExtraction hdbscanExtraction) {
    super();
    this.hdbscanExtraction = hdbscanExtraction;
  }

  public OutlierResult run(Database db, Relation<? extends NumberVector> relation) {
    Clustering<?> hdbscanresult = hdbscanExtraction.autorun(db);
    WritableDoubleDataStore scores = null;
    for(It<WritableDoubleDataStore> iter = Metadata.hierarchyOf(hdbscanresult).iterChildren().filter(WritableDoubleDataStore.class); iter.valid(); iter.advance()) {
      scores = iter.get();
    }
    if(scores == null) {
      throw new IllegalStateException("Were not GLOSH scores generated from clustering hierarchies?");
    }
    DoubleMinMax minmax = new DoubleMinMax();
    for(DBIDIter iditer = relation.iterDBIDs(); iditer.valid(); iditer.advance()) {
      minmax.put(scores.doubleValue(iditer));
    }
    // Wrap the result in the standard containers
    OutlierScoreMeta meta = new BasicOutlierScoreMeta(minmax.getMin(), minmax.getMax(), 0, 1);
    OutlierResult result = new OutlierResult(meta, new MaterializedDoubleRelation("GLOSH score", relation.getDBIDs(), scores));
    // Confuses auto-evaluation:
    // Metadata.hierarchyOf(result).addChild(hdbscanresult);
    return result;
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(TypeUtil.NUMBER_VECTOR_FIELD);
  }

  /**
   * Parameterization class.
   * 
   * @author Braulio V.S. Vinces
   */
  public static class Par implements Parameterizer {
    /**
     * Clustering algorithm to run.
     */
    protected HDBSCANHierarchyExtraction hdbscanExtraction;

    @Override
    public void configure(Parameterization config) {
      hdbscanExtraction = config.tryInstantiate(HDBSCANHierarchyExtraction.class);
    }

    @Override
    public GLOSH make() {
      return new GLOSH(hdbscanExtraction);
    }
  }
}
