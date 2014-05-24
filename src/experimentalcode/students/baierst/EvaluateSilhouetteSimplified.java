package experimentalcode.students.baierst;

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
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Centroid;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the simplified silhouette of a data set and the simplified
 * alternative silhouette.
 * 
 * TODO add Reference
 * 
 * 
 * @author Stephan Baier
 * 
 * @param <O> Object type
 */
public class EvaluateSilhouetteSimplified<O> implements Evaluator {

  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateSilhouetteSimplified.class);

  /**
   * Keep noise "clusters" merged.
   */
  private boolean mergenoise = false;

  /**
   * Distance function to use.
   */
  private PrimitiveDistanceFunction<? super NumberVector> distanceFunction;

  /**
   * Epsilon parameter for alternative silhouette computation.
   */
  private double eps;

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, not singletons
   * @param eps Epsilon parameter of alternative silhouette
   */
  public EvaluateSilhouetteSimplified(PrimitiveDistanceFunction<? super NumberVector> distance, boolean mergenoise, double eps) {
    super();
    this.distanceFunction = distance;
    this.mergenoise = mergenoise;
    this.eps = eps;
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
    MeanVariance mssil = new MeanVariance();
    MeanVariance mssilAlt = new MeanVariance();
    for(Cluster<?> cluster : clusters) {

      NumberVector centroid = Centroid.make((Relation<? extends NumberVector>) rel, cluster.getIDs()).toVector(rel);

      ArrayDBIDs ids = DBIDUtil.ensureArray(cluster.getIDs());
      DBIDArrayIter it1 = ids.iter();
      for(it1.seek(0); it1.valid(); it1.advance()) {
        // a: Distance to own centroid

        double a = distanceFunction.distance(centroid, rel.get(it1));

        // b: Distance to other clusters centroids:
        double min = Double.POSITIVE_INFINITY;
        for(Cluster<?> ocluster : clusters) {
          if(ocluster == /* yes, reference identity */cluster && !cluster.isNoise()) {
            continue;
          }

          if(!mergenoise && ocluster.isNoise()) {
            // Treat noise cluster as singletons:
            for(DBIDIter it2 = ocluster.getIDs().iter(); it2.valid(); it2.advance()) {
              if(it1.internalGetIndex() == it2.internalGetIndex()) {
                continue;
              }
              double b = distanceFunction.distance(rel.get(it1), rel.get(it2));
              if(b < min) {
                min = b;
              }
            }
            continue;
          }

          // TODO: calculate each centroid only once!
          NumberVector ocentroid = Centroid.make((Relation<? extends NumberVector>) rel, ocluster.getIDs()).toVector(rel);

          double b = distanceFunction.distance(ocentroid, rel.get(it1));

          if(b < min) {
            min = b;
          }
        }

        if(cluster.size() <= 1 || (!mergenoise && cluster.isNoise())) {
          // simplified silhouette is always one for singletons
          mssil.put(1);
          LOG.verbose("mssil put: " + 1);
          mssilAlt.put(min / (0 + this.eps));
          LOG.verbose("mssilAlt put: " + min / (0 + this.eps));

          continue;
        }

        mssil.put((min - a) / Math.max(min, a));
        LOG.verbose("mssil put: " + (min - a) / Math.max(min, a));
        mssilAlt.put(min / (a + this.eps));
        LOG.verbose("mssilAlt put: " + min / (a + this.eps));
      }
    }
    if(LOG.isVerbose()) {
      LOG.verbose("Mean Simplified Silhouette: " + mssil);
      LOG.verbose("Mean Simplified Silhouette Alternative: " + mssilAlt);
    }
    // Build a primitive result attachment:
    Collection<DoubleVector> col = new ArrayList<>();
    col.add(new DoubleVector(new double[] { mssil.getMean(), mssil.getSampleStddev() }));
    col.add(new DoubleVector(new double[] { mssilAlt.getMean(), mssilAlt.getSampleStddev() }));
    db.getHierarchy().add(c, new CollectionResult<>("Simplified Silhouette coefficient", "simplified-silhouette-coefficient", col));
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs.size() < 1) {
      return;
    }
    Database db = ResultUtil.findDatabase(baseResult);
    Relation<? extends NumberVector> rel = db.getRelation(this.distanceFunction.getInputTypeRestriction());

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
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("simplified-silhouette.distance", "Distance function to use for computing the silhouette.");

    /**
     * Parameter to treat noise as a single cluster.
     */
    public static final OptionID MERGENOISE_ID = new OptionID("simplified-silhouette.noisecluster", "Treat noise as a cluster, not as singletons.");

    /**
     * Parameter for epsilon value of alternative silhouette.
     */
    public static final OptionID EPS_ID = new OptionID("simplified-silhouette.alternative_eps", "Epsilon parameter for alternative silhouette.");

    /**
     * Distance function to use.
     */
    private PrimitiveDistanceFunction<NumberVector> distance;

    /**
     * Keep noise "clusters" merged.
     */
    private boolean mergenoise = false;

    /**
     * Epsilon for alternative silhouette.
     */
    private double eps;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      ObjectParameter<PrimitiveDistanceFunction<NumberVector>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, PrimitiveDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      Flag noiseP = new Flag(MERGENOISE_ID);
      if(config.grab(noiseP)) {
        mergenoise = noiseP.isTrue();
      }

      DoubleParameter epsP = new DoubleParameter(EPS_ID, 1e-6);
      if(config.grab(epsP)) {
        eps = epsP.doubleValue();
      }

    }

    @Override
    protected EvaluateSilhouetteSimplified<? extends NumberVector> makeInstance() {
      return new EvaluateSilhouetteSimplified<>(distance, mergenoise, eps);
    }
  }

}
