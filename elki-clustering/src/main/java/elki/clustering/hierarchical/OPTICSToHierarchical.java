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
package elki.clustering.hierarchical;

import elki.clustering.optics.ClusterOrder;
import elki.clustering.optics.OPTICSHeap;
import elki.clustering.optics.OPTICSTypeAlgorithm;
import elki.data.type.TypeInformation;
import elki.database.Database;
import elki.database.ids.*;
import elki.math.MathUtil;
import elki.utilities.datastructures.arrays.DoubleIntegerArrayQuickSort;
import elki.utilities.documentation.Reference;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.Parameterizer;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Convert a OPTICS ClusterOrder to a hierarchical clustering.
 * <p>
 * While this is not a literal implementation of below reference due to the
 * hierarchy representation currently used in ELKI instead of the
 * object-oriented tree used in the publication, the basic idea of converting
 * OPTICS plots into dendrograms is given by Sander et al.
 * <p>
 * TODO: this currently does not use the additional predecessor information that
 * should prove useful to disambiguate some cases.
 * <p>
 * Reference:
 * <p>
 * Jörg Sander, Xuejie Qin, Zhiyong Lu, Nan Niu, Alex Kovarsky<br>
 * Automatic Extraction of Clusters from Hierarchical Clustering
 * Representations<br>
 * 7th Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining, PAKDD
 * 
 * @author Erich Schubert
 * @since 0.8.0
 */
@Reference(authors = "Jörg Sander, Xuejie Qin, Zhiyong Lu, Nan Niu, Alex Kovarsky", //
    title = "Automatic Extraction of Clusters from Hierarchical Clustering Representations", //
    booktitle = "7th Pacific-Asia Conf. Advances in Knowledge Discovery and Data Mining, PAKDD", //
    url = "https://doi.org/10.1007/3-540-36175-8_8", bibkey = "DBLP:conf/pakdd/SanderQLNK03")
public class OPTICSToHierarchical implements HierarchicalClusteringAlgorithm {
  /**
   * Inner OPTICS clustering algorithm.
   */
  OPTICSTypeAlgorithm inner;

  /**
   * Constructor.
   *
   * @param inner OPTICS algorithm to use.
   */
  public OPTICSToHierarchical(OPTICSTypeAlgorithm inner) {
    this.inner = inner;
  }

  @Override
  public ClusterMergeHistory autorun(Database database) {
    ClusterOrder in = inner.autorun(database);
    ArrayDBIDs ids = in.getDBIDs();

    int[] ord = MathUtil.sequence(1, ids.size());
    double[] d = new double[ord.length];
    for(DBIDArrayIter it = ids.iter().seek(1); it.valid(); it.advance()) {
      d[it.getOffset() - 1] = in.getReachability(it);
    }
    DoubleIntegerArrayQuickSort.sort(d, ord, d.length);

    ClusterMergeHistoryBuilder builder = new ClusterMergeHistoryBuilder(ids, false);
    for(int i = 0; i < ord.length; i++) {
      builder.add(ord[i] - 1, d[i], ord[i]);
    }
    return builder.complete();
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return inner.getInputTypeRestriction();
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   */
  public static class Par implements Parameterizer {
    /**
     * Option ID for the OPTICS algorithm.
     */
    public static final OptionID OPTICS_ID = new OptionID("optics", "OPTICS algorithm to use.");

    /**
     * Inner OPTICS clustering algorithm.
     */
    OPTICSTypeAlgorithm inner;

    @Override
    public void configure(Parameterization config) {
      new ObjectParameter<OPTICSTypeAlgorithm>(OPTICS_ID, OPTICSTypeAlgorithm.class, OPTICSHeap.class) //
          .grab(config, x -> inner = x);
    }

    @Override
    public OPTICSToHierarchical make() {
      return new OPTICSToHierarchical(inner);
    }
  }
}
