package de.lmu.ifi.dbs.elki.algorithm.clustering;
/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2013
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.util.ArrayList;

import de.lmu.ifi.dbs.elki.algorithm.AbstractDistanceBasedAlgorithm;
import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.model.ClusterModel;
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
import de.lmu.ifi.dbs.elki.distance.distancevalue.Distance;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.WrongParameterValueException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DistanceParameter;

/**
 * Canopy pre-clustering is a simple preprocessing step for clustering.
 * 
 * <p>
 * Reference:<br>
 * A. McCallum, K. Nigam, L.H. Ungar<br />
 * Efficient Clustering of High Dimensional Data Sets with Application to
 * Reference Matching<br />
 * Proc. 6th ACM SIGKDD international conference on Knowledge discovery and data
 * mining
 * </p>
 * 
 * @author Erich Schubert
 * 
 * @param <O> Object type
 * @param <D> Distance type
 */
@Reference(authors = "A. McCallum, K. Nigam, L.H. Ungar", title = "Efficient Clustering of High Dimensional Data Sets with Application to Reference Matching", booktitle = "Proc. 6th ACM SIGKDD international conference on Knowledge discovery and data mining", url = "http://dx.doi.org/10.1145%2F347090.347123")
public class CanopyPreClustering<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm<O, D, Clustering<ClusterModel>> implements ClusteringAlgorithm<Clustering<ClusterModel>> {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(CanopyPreClustering.class);

  /**
   * Threshold for inclusion
   */
  private D t1;

  /**
   * Threshold for removal
   */
  private D t2;

  /**
   * Constructor.
   * 
   * @param distanceFunction Distance function
   * @param t1 Inclusion threshold
   * @param t2 Exclusion threshold
   */
  public CanopyPreClustering(DistanceFunction<? super O, D> distanceFunction, D t1, D t2) {
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
  public Clustering<ClusterModel> run(Database database, Relation<O> relation) {
    DistanceQuery<O, D> dq = database.getDistanceQuery(relation, getDistanceFunction());
    ModifiableDBIDs ids = DBIDUtil.newHashSet(relation.getDBIDs());
    ArrayList<Cluster<ClusterModel>> clusters = new ArrayList<>();
    final int size = relation.size();

    if(t1.compareTo(t2) <= 0) {
      LOG.warning(Parameterizer.T1_ID.getName() + " must be larger than " + Parameterizer.T2_ID.getName());
    }

    FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Canopy clustering", size, LOG) : null;

    DBIDVar first = DBIDUtil.newVar();
    while(!ids.isEmpty()) {
      // Remove first element:
      DBIDMIter iter = ids.iter();
      first.set(iter);
      iter.remove();
      iter.advance();

      // Start a new cluster:
      ModifiableDBIDs cids = DBIDUtil.newArray();
      cids.add(first);

      // Compare to remaining objects:
      for(; iter.valid(); iter.advance()) {
        D dist = dq.distance(first, iter);
        // Inclusion threshold:
        if(t1.compareTo(dist) >= 0) {
          cids.add(iter);
        }
        // Removal threshold:
        if(t2.compareTo(dist) >= 0) {
          iter.remove();
        }
      }
      // TODO: remember the central object using a CanopyModel?
      // Construct cluster:
      clusters.add(new Cluster<>(cids, ClusterModel.CLUSTER));

      if(prog != null) {
        prog.setProcessed(size - ids.size(), LOG);
      }
    }
    if(prog != null) {
      prog.ensureCompleted(LOG);
    }

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
   * @apiviz.exclude
   * 
   * @param <O> Object type
   * @param <D> Distance type
   */
  public static class Parameterizer<O, D extends Distance<D>> extends AbstractDistanceBasedAlgorithm.Parameterizer<O, D> {
    /**
     * Parameter for the inclusion threshold of canopy clustering.
     * 
     * Note: t1 > t2
     * 
     * Syntax:
     * 
     * <pre>
     * -canopy.t1 &lt;value&gt;
     * </pre>
     */
    public static final OptionID T1_ID = new OptionID("canopy.t1", "Inclusion threshold for canopy clustering. t1 > t2!");

    /**
     * Parameter for the removal threshold of canopy clustering.
     * 
     * Note: t1 > t2
     * 
     * Syntax:
     * 
     * <pre>
     * -canopy.t2 &lt;value&gt;
     * </pre>
     */
    public static final OptionID T2_ID = new OptionID("canopy.t2", "Removal threshold for canopy clustering. t1 > t2!");

    /**
     * Threshold for inclusion
     */
    private D t1;

    /**
     * Threshold for removal
     */
    private D t2;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      DistanceParameter<D> t1P = new DistanceParameter<>(T1_ID, distanceFunction);
      if(config.grab(t1P)) {
        t1 = t1P.getValue();
      }

      DistanceParameter<D> t2P = new DistanceParameter<>(T2_ID, distanceFunction);
      // TODO: add distance constraint t1 > t2
      if(config.grab(t2P)) {
        t2 = t2P.getValue();
        if(t1.compareTo(t2) <= 0) {
          config.reportError(new WrongParameterValueException(t2P, T1_ID.getName() + " must be larger than " + T2_ID.getName()));
        }
      }
    }

    @Override
    protected CanopyPreClustering<O, D> makeInstance() {
      return new CanopyPreClustering<>(distanceFunction, t1, t2);
    }

  }
}
