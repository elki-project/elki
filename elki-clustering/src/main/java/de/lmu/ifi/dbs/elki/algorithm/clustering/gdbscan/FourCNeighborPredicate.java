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
package de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan;

import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.minusEquals;
import static de.lmu.ifi.dbs.elki.math.linearalgebra.VMath.transposeTimesTimes;

import de.lmu.ifi.dbs.elki.algorithm.clustering.correlation.FourC;
import de.lmu.ifi.dbs.elki.algorithm.clustering.gdbscan.PreDeConNeighborPredicate.PreDeConModel;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.SimpleTypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeInformation;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.datastore.DataStore;
import de.lmu.ifi.dbs.elki.database.ids.*;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.range.RangeQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.database.relation.RelationUtil;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.*;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.EigenPairFilter;
import de.lmu.ifi.dbs.elki.math.linearalgebra.pca.filter.LimitEigenPairFilter;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * 4C identifies local subgroups of data objects sharing a uniform correlation.
 * The algorithm is based on a combination of PCA and density-based clustering
 * (DBSCAN).
 * <p>
 * Reference:
 * <p>
 * Christian Böhm, Karin Kailing, Peer Kröger, Arthur Zimek<br>
 * Computing Clusters of Correlation Connected Objects<br>
 * Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2004)
 * 
 * 
 * @author Arthur Zimek
 * @author Erich Schubert
 * @since 0.7.0
 * 
 * @param <V> the type of NumberVector handled by this Algorithm
 */
@Reference(authors = "Christian Böhm, Karin Kailing, Peer Kröger, Arthur Zimek", //
    title = "Computing Clusters of Correlation Connected Objects", //
    booktitle = "Proc. ACM SIGMOD Int. Conf. on Management of Data (SIGMOD 2004)", //
    url = "https://doi.org/10.1145/1007568.1007620", //
    bibkey = "DBLP:conf/sigmod/BohmKKZ04")
public class FourCNeighborPredicate<V extends NumberVector> extends AbstractRangeQueryNeighborPredicate<V, PreDeConModel, PreDeConModel> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FourCNeighborPredicate.class);

  /**
   * 4C settings class.
   */
  private FourC.Settings settings;

  /**
   * Tool to help with parameterization.
   */
  private MeanVariance mvSize = new MeanVariance(),
      mvSize2 = new MeanVariance(), mvCorDim = new MeanVariance();

  /**
   * The Filtered PCA Runner
   */
  private PCARunner pca;

  /**
   * Filter for selecting eigenvectors.
   */
  private EigenPairFilter filter;

  /**
   * Constructor.
   * 
   * @param settings 4C settings
   */
  public FourCNeighborPredicate(FourC.Settings settings) {
    super(settings.epsilon, EuclideanDistanceFunction.STATIC);
    this.settings = settings;
    this.pca = new PCARunner(new StandardCovarianceMatrixBuilder());
    this.filter = new LimitEigenPairFilter(settings.delta, settings.absolute);
  }

  @Override
  public Instance instantiate(Database database) {
    Relation<V> relation = database.getRelation(getInputTypeRestriction());
    DistanceQuery<V> dq = database.getDistanceQuery(relation, distFunc);
    RangeQuery<V> rq = database.getRangeQuery(dq);
    mvSize.reset();
    mvSize2.reset();
    mvCorDim.reset();
    DataStore<PreDeConModel> storage = preprocess(PreDeConModel.class, relation, rq);
    if(LOG.isVerbose()) {
      LOG.verbose("Average neighborhood size: " + mvSize.toString());
      LOG.verbose("Average correlation dimensionality: " + mvCorDim.toString());
      LOG.verbose("Average correlated neighborhood size: " + mvSize2.toString());
      final int dim = RelationUtil.dimensionality(relation);
      if(mvSize.getMean() < 5 * dim) {
        LOG.verbose("The epsilon parameter may be chosen too small.");
      }
      else if(mvSize.getMean() > .5 * relation.size()) {
        LOG.verbose("The epsilon parameter may be chosen too large.");
      }
      else if(mvSize2.getMean() < 10) {
        LOG.verbose("The epsilon parameter may be chosen too large, or delta too small.");
      }
      else if(mvSize2.getMean() < settings.minpts) {
        LOG.verbose("The minPts parameter may be chosen too large.");
      }
      else {
        LOG.verbose("As a first guess, you can try minPts < " + ((int) mvSize2.getMean()) //
            + ", but you will need to experiment with these parameters and epsilon.");
      }
    }
    return new Instance(dq.getRelation().getDBIDs(), storage);
  }

  @Override
  protected PreDeConModel computeLocalModel(DBIDRef id, DoubleDBIDList neighbors, Relation<V> relation) {
    mvSize.put(neighbors.size());
    PCAResult epairs = pca.processIds(neighbors, relation);
    int cordim = filter.filter(epairs.getEigenvalues());
    PCAFilteredResult pcares = new PCAFilteredResult(epairs.getEigenPairs(), cordim, settings.kappa, 1.);
    double[][] m_hat = pcares.similarityMatrix();
    double[] obj = relation.get(id).toArray();

    // To save computing the square root below.
    double sqeps = settings.epsilon * settings.epsilon;

    HashSetModifiableDBIDs survivors = DBIDUtil.newHashSet(neighbors.size());
    for(DBIDIter iter = neighbors.iter(); iter.valid(); iter.advance()) {
      // Compute weighted / projected distance:
      double[] diff = minusEquals(relation.get(iter).toArray(), obj);
      double dist = transposeTimesTimes(diff, m_hat, diff);
      if(dist <= sqeps) {
        survivors.add(iter);
      }
    }
    if(cordim <= settings.lambda) {
      mvSize2.put(survivors.size());
    }
    mvCorDim.put(cordim);
    return new PreDeConModel(cordim, survivors);
  }

  @Override
  Logging getLogger() {
    return LOG;
  }

  @Override
  public TypeInformation getInputTypeRestriction() {
    return TypeUtil.NUMBER_VECTOR_FIELD;
  }

  @Override
  public SimpleTypeInformation<PreDeConModel> getOutputType() {
    return new SimpleTypeInformation<>(PreDeConModel.class);
  }

  /**
   * Instance for a particular data set.
   * 
   * @author Erich Schubert
   */
  public static class Instance extends AbstractRangeQueryNeighborPredicate.Instance<PreDeConModel, PreDeConModel> {
    /**
     * Constructor.
     * 
     * @param ids IDs this is defined for.
     * @param storage Stored models
     */
    public Instance(DBIDs ids, DataStore<PreDeConModel> storage) {
      super(ids, storage);
    }

    @Override
    public PreDeConModel getNeighbors(DBIDRef reference) {
      final PreDeConModel asymmetric = storage.get(reference);
      // Check for mutual preference reachability:
      HashSetModifiableDBIDs ids = DBIDUtil.newHashSet(asymmetric.ids.size());
      for(DBIDIter neighbor = asymmetric.ids.iter(); neighbor.valid(); neighbor.advance()) {
        if(storage.get(neighbor).ids.contains(reference)) {
          ids.add(neighbor);
        }
      }
      return new PreDeConModel(asymmetric.pdim, ids);
    }

    @Override
    public DBIDIter iterDBIDs(PreDeConModel neighbors) {
      return neighbors.ids.iter();
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Parameterizer<O extends NumberVector> extends AbstractParameterizer {
    /**
     * 4C settings.
     */
    protected FourC.Settings settings;

    @Override
    protected void makeOptions(Parameterization config) {
      settings = config.tryInstantiate(FourC.Settings.class);
    }

    @Override
    protected FourCNeighborPredicate<O> makeInstance() {
      return new FourCNeighborPredicate<>(settings);
    }
  }
}
