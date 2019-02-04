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
package de.lmu.ifi.dbs.elki.algorithm.clustering;

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.PrototypeModel;
import de.lmu.ifi.dbs.elki.data.model.SimplePrototypeModel;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDMIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDVar;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;

/**
 * Canopy pre-clustering is a simple preprocessing step for clustering.
 * <p>
 * Reference:
 * <p>
 * A. McCallum, K. Nigam, L. H. Ungar<br>
 * Efficient Clustering of High Dimensional Data Sets with Application to
 * Reference Matching<br>
 * Proc. 6th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining
 *
 * @author Erich Schubert
 * @since 0.6.0
 *
 * @param <O> Object type
 */
@Reference(authors = "A. McCallum, K. Nigam, L. H. Ungar", //
    title = "Efficient Clustering of High Dimensional Data Sets with Application to Reference Matching", //
    booktitle = "Proc. 6th ACM SIGKDD Int. Conf. on Knowledge Discovery and Data Mining", //
    url = "https://doi.org/10.1145/347090.347123", //
    bibkey = "DBLP:conf/kdd/McCallumNU00")
public class CanopyPreClustering<O> extends AbstractDistanceBasedAlgorithm<O, Clustering<PrototypeModel<O>>> implements ClusteringAlgorithm<Clustering<PrototypeModel<O>>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CanopyPreClustering.class);

  /**
   * Threshold for inclusion
   */
  private double t1;

  /**
   * Threshold for removal
   */
  private double t2;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param t1 Inclusion threshold
   * @param t2 Exclusion threshold
   */
  public CanopyPreClustering(DistanceFunction<? super O> distanceFunction, double t1, double t2) {
    super(distanceFunction);
    this.t1 = t1;
    this.t2 = t2;
  }

  /**
   * Run the algorithm
   * 
   * @param database Database
   * @param relation Relation to process
   */
  public Clustering<PrototypeModel<O>> run(Database database, Relation<O> relation) {
    if(!(t1 >= t2)) {
      throw new AbortException("T1 must be at least as large as T2.");
    }

    DistanceQuery<O> dq = database.getDistanceQuery(relation, getDistanceFunction());
    ModifiableDBIDs ids = DBIDUtil.newHashSet(relation.getDBIDs());
    ArrayList<Cluster<PrototypeModel<O>>> clusters = new ArrayList<>();
    final int size = relation.size();
    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Canopy clustering", size, LOG) : null;

    DBIDVar first = DBIDUtil.newVar();
    while(!ids.isEmpty()) {
      // Remove first element:
      ids.pop(first);

      // Start a new cluster:
      ModifiableDBIDs cids = DBIDUtil.newArray();
      cids.add(first);

      // Compare to remaining objects:
      for(DBIDMIter iter = ids.iter(); iter.valid(); iter.advance()) {
        double dist = dq.distance(first, iter);
        // Inclusion threshold:
        if(dist > t1) {
          continue;
        }
        cids.add(iter);
        // Removal threshold:
        if(dist <= t2) {
          iter.remove();
        }
      }
      // TODO: remember the central object using a CanopyModel?
      // Construct cluster:
      clusters.add(new Cluster<>(cids, new SimplePrototypeModel<>(relation.get(first))));

      if(prog != null) {
        prog.setProcessed(size - ids.size(), LOG);
      }
    }
    LOG.ensureCompleted(prog);
    return new Clustering<>("Canopy clustering", "canopy-clustering", clusters);
  }

  @Override
  public TypeInformation[] getInputTypeRestriction() {
    return TypeUtil.array(getDistanceFunction().getInputTypeRestriction());
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class
   * 
   * @author Erich Schubert
   * 
   * @hidden
   * 
   * @param <O> Object type
   */
  public static class Parameterizer<O> extends AbstractDistanceBasedAlgorithm.Parameterizer<O> {
    /**
     * Parameter for the inclusion threshold of canopy clustering.
     * 
     * Note: t1 >= t2
     * 
     * Syntax:
     * 
     * <pre>
     * -canopy.t1 &lt;value&gt;
     * </pre>
     */
    public static final OptionID T1_ID = new OptionID("canopy.t1", "Inclusion threshold for canopy clustering. t1 >= t2!");

    /**
     * Parameter for the removal threshold of canopy clustering.
     * 
     * Note: t1 >= t2
     * 
     * Syntax:
     * 
     * <pre>
     * -canopy.t2 &lt;value&gt;
     * </pre>
     */
    public static final OptionID T2_ID = new OptionID("canopy.t2", "Removal threshold for canopy clustering. t1 >= t2!");

    /**
     * Threshold for inclusion
     */
    private double t1;

    /**
     * Threshold for removal
     */
    private double t2;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DoubleParameter t1P = new DoubleParameter(T1_ID);
      if(config.grab(t1P)) {
        t1 = t1P.doubleValue();
      }

      DoubleParameter t2P = new DoubleParameter(T2_ID);
      if(config.grab(t2P)) {
        t2 = t2P.doubleValue();
      }
      // Non-formalized parameter constraint: t1 >= t2
      if(t1 < t2) {
        config.reportError(new WrongParameterValueException(t1P, "must be larger than", t2P, ""));
      }
    }

    @Override
    protected CanopyPreClustering<O> makeInstance() {
      return new CanopyPreClustering<>(distanceFunction, t1, t2);
    }
  }
}
