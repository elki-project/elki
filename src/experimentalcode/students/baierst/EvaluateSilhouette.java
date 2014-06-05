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
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBIDArrayIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.EuclideanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
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
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.documentation.Reference;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the silhouette of a data set and the alternative silhouette.
 * 
 * Reference:
 * <p>
 * P. J. Rousseeuw<br />
 * Silhouettes: A graphical aid to the interpretation and validation of cluster
 * analysis<br />
 * In: Journal of Computational and Applied Mathematics Volume 20, November 1987
 * </p>
 * 
 * TODO: keep all silhouette values, and allow visualization!
 * 
 * @author Erich Schubert, Stephan Baier
 * 
 * @param <O> Object type
 */
@Reference(authors = "P. J. Rousseeuw", //
title = "Silhouettes: A graphical aid to the interpretation and validation of cluster analysis", //
booktitle = "Journal of Computational and Applied Mathematics, Volume 20", //
url = "http://dx.doi.org/10.1016%2F0377-0427%2887%2990125-7")
public class EvaluateSilhouette<O> implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateSilhouette.class);

  /**
   * Option for noise handling.
   */
  private NoiseOption noiseOption = NoiseOption.IGNORE_NOISE_WITH_PENALTY;

  /**
   * Distance function to use.
   */
  private DistanceFunction<? super O> distance;

  /**
   * Epsilon parameter for alternative silhouette computation.
   */
  private double eps;

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, not singletons
   */
  public EvaluateSilhouette(DistanceFunction<? super O> distance, NoiseOption noiseOpt, double eps) {
    super();
    this.distance = distance;
    this.noiseOption = noiseOpt;
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
  public void evaluateClustering(Database db, Relation<O> rel, DistanceQuery<O> dq, Clustering<?> c) {

    List<? extends Cluster<?>> clusters;

    if(noiseOption.equals(NoiseOption.TREAT_NOISE_AS_SINGLETONS)) {
      clusters = ClusteringUtils.convertNoiseToSingletons(c);
    }
    else {
      clusters = c.getAllClusters();
    }

    int countNoise = 0;

    MeanVariance msil = new MeanVariance();
    MeanVariance msilAlt = new MeanVariance();
    for(Cluster<?> cluster : clusters) {

      if(cluster.isNoise() && (noiseOption.equals(NoiseOption.IGNORE_NOISE) || noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY))) {
        countNoise += cluster.size();
        continue;
      }

      if(cluster.size() <= 1) {
        // As suggested in Rousseeuw, we use 0 for singletons.
        msil.put(0., cluster.size());
        continue;
      }
      ArrayDBIDs ids = DBIDUtil.ensureArray(cluster.getIDs());
      double[] as = new double[ids.size()]; // temporary storage.
      DBIDArrayIter it1 = ids.iter(), it2 = ids.iter();
      for(it1.seek(0); it1.valid(); it1.advance()) {
        // a: In-cluster distances
        double a = as[it1.getOffset()]; // Already computed distances
        for(it2.seek(it1.getOffset() + 1); it2.valid(); it2.advance()) {
          final double dist = dq.distance(it1, it2);
          a += dist;
          as[it2.getOffset()] += dist;
        }
        a /= (ids.size() - 1);
        // b: other clusters:
        double min = Double.POSITIVE_INFINITY;
        for(Cluster<?> ocluster : clusters) {

          if(ocluster.isNoise() && (noiseOption.equals(NoiseOption.IGNORE_NOISE) || noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY))) {
            continue;
          }

          if(ocluster == cluster) {
            continue;
          }
          final DBIDs oids = ocluster.getIDs();
          double b = 0.;
          for(DBIDIter it3 = oids.iter(); it3.valid(); it3.advance()) {
            b += dq.distance(it1, it3);
          }
          b /= oids.size();
          if(b < min) {
            min = b;
          }
        }
        msil.put((min - a) / Math.max(min, a));
        msilAlt.put(min / (a + this.eps));
      }
    }

    double msilMean = msil.getMean();
    double msilAltMean = msilAlt.getMean();

    if(noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY)) {

      double penalty = 1;

      if(countNoise != 0) {
        penalty = ((double) rel.size() - (double) countNoise) / (double) rel.size();
      }

      msilMean = penalty * msilMean;
      msilAltMean = penalty * msilAltMean;
    }

    if(LOG.isVerbose()) {
      LOG.verbose("Mean Silhouette: " + msilMean);
      LOG.verbose("Mean Silhouette Alternative: " + msilAltMean);
    }
    // Build a primitive result attachment:
    Collection<DoubleVector> col = new ArrayList<>();
    col.add(new DoubleVector(new double[] { msilMean }));
    col.add(new DoubleVector(new double[] { msilAltMean }));
    db.getHierarchy().add(c, new CollectionResult<>("Silhouette coefficient", "silhouette-coefficient", col));
  }

  @Override
  public void processNewResult(HierarchicalResult baseResult, Result result) {
    List<Clustering<?>> crs = ResultUtil.getClusteringResults(result);
    if(crs.size() < 1) {
      return;
    }
    Database db = ResultUtil.findDatabase(baseResult);
    Relation<O> rel = db.getRelation(distance.getInputTypeRestriction());
    DistanceQuery<O> dq = db.getDistanceQuery(rel, distance);
    for(Clustering<?> c : crs) {
      evaluateClustering(db, rel, dq, c);
    }
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert, Stephan Baier
   * 
   * @apiviz.exclude
   */
  public static class Parameterizer<O> extends AbstractParameterizer {
    /**
     * Parameter for choosing the distance function.
     */
    public static final OptionID DISTANCE_ID = new OptionID("silhouette.distance", "Distance function to use for computing the silhouette.");

    /**
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_OPTION_ID = new OptionID("silhouette.noiseoption", "option, how noise should be treated.");

    /**
     * Parameter for epsilon value of alternative silhouette.
     */
    public static final OptionID EPS_ID = new OptionID("silhouette.alternative_eps", "Epsilon parameter for alternative silhouette.");

    /**
     * Distance function to use.
     */
    private DistanceFunction<? super O> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseOption noiseOption = NoiseOption.IGNORE_NOISE_WITH_PENALTY;

    /**
     * Epsilon for alternative silhouette.
     */
    private double eps;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<DistanceFunction<? super O>> distP = new ObjectParameter<>(DISTANCE_ID, DistanceFunction.class, EuclideanDistanceFunction.class);
      if(config.grab(distP)) {
        distance = distP.instantiateClass(config);
      }

      EnumParameter<NoiseOption> noiseP = new EnumParameter<NoiseOption>(NOISE_OPTION_ID, NoiseOption.class, NoiseOption.IGNORE_NOISE_WITH_PENALTY);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }

      DoubleParameter epsP = new DoubleParameter(EPS_ID, 1e-6);
      if(config.grab(epsP)) {
        eps = epsP.doubleValue();
      }

    }

    @Override
    protected EvaluateSilhouette<O> makeInstance() {
      return new EvaluateSilhouette<>(distance, noiseOption, eps);
    }
  }
}
