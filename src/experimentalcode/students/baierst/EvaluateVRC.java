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
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;

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
public class EvaluateVRC<O> implements Evaluator {

  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateSilhouetteSimplified.class);

  /**
   * Option for noise handling.
   */
  private NoiseOption noiseOption = NoiseOption.IGNORE_NOISE_WITH_PENALTY;

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, not singletons
   */
  public EvaluateVRC(NoiseOption opt) {
    super();
    this.noiseOption = opt;
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

    List<? extends Cluster<?>> clusters;
    
    if(noiseOption.equals(NoiseOption.TREAT_NOISE_AS_SINGLETONS)){
      clusters = ClusteringUtils.convertNoiseToSingletons(c);
    }else{
      clusters = c.getAllClusters();
    }
       
    int countNoise = 0;

    // precompute all centroids
    ArrayList<NumberVector> centroids = new ArrayList<NumberVector>();
    for(Cluster<?> cluster : clusters) {      
      centroids.add(Centroid.make((Relation<? extends NumberVector>) rel, cluster.getIDs()).toVector(rel));
    }
    NumberVector dataCentroid = Centroid.make((Relation<? extends NumberVector>) rel).toVector(rel);

    // a: Distance to own centroid
    double a = 0;
    // b: Distance to overall centroid
    double b = 0;
    int i = 0;
    for(Cluster<?> cluster : clusters) {    
      
      if(cluster.isNoise() && (noiseOption.equals(NoiseOption.IGNORE_NOISE) || noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY))){
        countNoise += cluster.size();
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

    if(noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY)){
      double penalty = countNoise / rel.size();
      vrc = penalty * vrc;
    }
    
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
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_OPTION_ID = new OptionID("vrc.noiseoption", "option, how noise should be treated.");


    /**
     * Option, how noise should be treated.
     */
    private NoiseOption noiseOption = NoiseOption.IGNORE_NOISE_WITH_PENALTY;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);

      EnumParameter<NoiseOption> noiseP = new EnumParameter<NoiseOption>(NOISE_OPTION_ID, NoiseOption.class, NoiseOption.IGNORE_NOISE_WITH_PENALTY);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }

    }

    @Override
    protected EvaluateVRC<? extends NumberVector> makeInstance() {
      return new EvaluateVRC<>(noiseOption);
    }
  }

}
