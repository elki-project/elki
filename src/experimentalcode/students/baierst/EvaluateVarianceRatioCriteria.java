package experimentalcode.students.baierst;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.SquaredEuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2014
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

/**
 * 
 * Compute the Variance Ratio Criteria of a data set
 * 
 * Reference:
 * <p>
 * R. B. Calinski and J. Harabasz <br />
 * A dendrite method for cluster analysis <br />
 * In: Commun Stat 3, 1-27, 1974
 * </p>
 * 
 * @author Stephan Baier
 * @param <O> Object type
 * 
 */
public class EvaluateVarianceRatioCriteria<O> implements Evaluator {

  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateSilhouetteSimplified.class);

  /**
   * Keep noise "clusters" merged.
   */
  private boolean mergenoise = false;

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, not singletons
   */
  public EvaluateVarianceRatioCriteria(boolean mergenoise) {
    super();
    this.mergenoise = mergenoise;
  }

  /**
   * Evaluate a single clustering.
   * 
   * @param db Database
   * @param rel Data relation
   * @param dq Distance query
   * @param c Clustering
   */
  public void evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {

    List<? extends Cluster<?>> clusters = c.getAllClusters();

    // precompute all centroids
    ArrayList<NumberVector> centroids = new ArrayList<NumberVector>();
    for(Cluster<?> cluster : clusters) {
      if(!mergenoise && cluster.isNoise()) {
        for(DBIDIter it1 = cluster.getIDs().iter(); it1.valid(); it1.advance()) {
          centroids.add(rel.get(it1));
        }
        continue;
      }
      centroids.add(Centroid.make((Relation<? extends NumberVector>) rel, cluster.getIDs()).toVector(rel));

    }
    NumberVector dataCentroid = Centroid.make((Relation<? extends NumberVector>) rel).toVector(rel);

    // a: Distance to own centroid
    double a = 0;
    // b: Distance to overall centroid
    double b = 0;
    int i = 0;
    for(Cluster<?> cluster : clusters) {

      if(cluster.size() <= 1 || (!mergenoise && cluster.isNoise())) {
        for(DBIDIter it1 = cluster.getIDs().iter(); it1.valid(); it1.advance()) {
          b += SquaredEuclideanDistanceFunction.STATIC.distance(dataCentroid, rel.get(it1));
          i++;
        }
        continue;
      }

      ArrayDBIDs ids = DBIDUtil.ensureArray(cluster.getIDs());
      DBIDArrayIter it2 = ids.iter();
      for(it2.seek(0); it2.valid(); it2.advance()) {
        a += SquaredEuclideanDistanceFunction.STATIC.distance(centroids.get(i), rel.get(it2));
        b += SquaredEuclideanDistanceFunction.STATIC.distance(dataCentroid, rel.get(it2));
      }
      i++;
    }

    double vrc = ((b - a) / a) * ((rel.size() - centroids.size()) / (centroids.size() - 1.));

    if(LOG.isVerbose()) {
      LOG.verbose("VRC: " + vrc);
    }
    // Build a primitive result attachment:
    Collection<DoubleVector> col = new ArrayList<>();
    col.add(new DoubleVector(new double[] { vrc }));
    db.getHierarchy().add(c, new CollectionResult<>("VRC", "vrc", col));

  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs.size() < 1) {
      return;
    }
    Database db = ResultUtil.findDatabase(baseResult);
    Relation<? extends NumberVector> rel = db.getRelation(EuclideanDistanceFunction.STATIC.getInputTypeRestriction());

    for(Clustering<?> c : crs) {
      evaluateClustering(db, (Relation<? extends NumberVector>) rel, c);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Stephan Baier
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractParameterizer {

    /**
     * Parameter to treat noise as a single cluster.
     */
    public static final OptionID MERGENOISE_ID = new OptionID("vrc.noisecluster", "Treat noise as a cluster, not as singletons.");

    /**
     * Keep noise "clusters" merged.
     */
    private boolean mergenoise = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      Flag noiseP = new Flag(MERGENOISE_ID);
      if(config.grab(noiseP)) {
        mergenoise = noiseP.isTrue();
      }

    }

    @Override
    protected EvaluateVarianceRatioCriteria<? extends NumberVector> makeInstance() {
      return new EvaluateVarianceRatioCriteria<>(mergenoise);
    }
  }

}
