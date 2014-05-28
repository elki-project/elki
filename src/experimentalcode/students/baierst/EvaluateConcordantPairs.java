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
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
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
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.EnumParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the Gamma Criterion of a data set.
 * 
 * Reference:
 * <p>
 * F.B. Baker and L. J. Hubert <br />
 * Measuring the power of hierachical clustering analysis <br />
 * J Am Stat Assoc 70, 1975
 * </p>
 * 
 * @author Stephan Baier
 * 
 */
public class EvaluateConcordantPairs<O> implements Evaluator {
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
  private PrimitiveDistanceFunction<? super NumberVector> distanceFunction;

  /**
   * Constructor.
   * 
   * @param distance Distance function
   * @param mergenoise Flag to treat noise as clusters, not singletons
   */
  public EvaluateConcordantPairs(PrimitiveDistanceFunction<? super NumberVector> distance, NoiseOption noiseOpt) {
    super();
    this.distanceFunction = distance;
    this.noiseOption = noiseOpt;
  }

  /**
   * Evaluate a single clustering.
   * 
   * @param db Database
   * @param rel Data relation
   * @param c Clustering
   */
  public void evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {

    List<? extends Cluster<?>> clusters;

    if(noiseOption.equals(NoiseOption.TREAT_NOISE_AS_SINGLETONS)) {
      clusters = ClusteringUtils.convertNoiseToSingletons(c);
    }
    else {
      clusters = c.getAllClusters();
    }

    int countNoise = 0;

    int concordantPairs = 0;
    int discordantPairs = 0;

    for(Cluster<?> cluster : clusters) {
      if(cluster.isNoise() && (noiseOption.equals(NoiseOption.IGNORE_NOISE) || noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY))) {
        countNoise += cluster.size();
        continue;
      }
      ArrayDBIDs ids = DBIDUtil.ensureArray(cluster.getIDs());
      DBIDArrayIter it1 = ids.iter();
      for(it1.seek(0); it1.valid(); it1.advance()) {
        DBIDArrayIter it2 = ids.iter();
        for(it2.seek(it1.getOffset() + 1); it2.valid(); it2.advance()) {
          if(DBIDUtil.equal(it1, it2)) {
            continue;
          }
          double withinDist = distanceFunction.distance(rel.get(it1), rel.get(it2));
          for(int i = 0; i < clusters.size(); i++) {
            Cluster<?> ocluster1 = clusters.get(i);
            if(ocluster1.isNoise() && (noiseOption.equals(NoiseOption.IGNORE_NOISE) || noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY))) {
              continue;
            }
            for(int j = i + 1; j < clusters.size(); j++) {
              Cluster<?> ocluster2 = clusters.get(j);
              if(ocluster2.isNoise() && (noiseOption.equals(NoiseOption.IGNORE_NOISE) || noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY))) {
                continue;
              }
              for(DBIDIter oit1 = ocluster1.getIDs().iter(); oit1.valid(); oit1.advance()) {
                for(DBIDIter oit2 = ocluster2.getIDs().iter(); oit2.valid(); oit2.advance()) {
                  double betweenDist = distanceFunction.distance(rel.get(oit1), rel.get(oit2));
                  if(withinDist < betweenDist)
                    concordantPairs++;
                  if(withinDist > betweenDist)
                    discordantPairs++;
                }
              }
            }
          }
        }
      }
    }

    double gamma = (concordantPairs - discordantPairs) / (concordantPairs + discordantPairs);

    double t;
    if(noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY) || noiseOption.equals(NoiseOption.IGNORE_NOISE)) {
      t = ((rel.size() - countNoise) * (rel.size() - countNoise - 1)) / 2.;
    }
    else {
      t = ((rel.size()) * (rel.size() - 1)) / 2.;
    }

    double gPlus = (2 * discordantPairs) / (t * (t - 1));

    if(noiseOption.equals(NoiseOption.IGNORE_NOISE_WITH_PENALTY)) {

      double penalty = 1;

      if(countNoise != 0) {
        penalty = (double) countNoise / (double) rel.size();
      }
      gamma = penalty * gamma;
      gPlus = gamma * gPlus;
    }

    if(LOG.isVerbose()) {
      LOG.verbose("gamma: " + gamma);
      LOG.verbose("gPlus: " + gPlus);
    }
    // Build a primitive result attachment:
    Collection<DoubleVector> col = new ArrayList<>();
    col.add(new DoubleVector(new double[] { gamma }));
    db.getHierarchy().add(c, new CollectionResult<>("concordant-pair-based-measures", "concordant-pair-based-measures", col));

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
    public static final OptionID DISTANCE_ID = new OptionID("concordant.distance", "Distance function to use for measuring concordant and discordant pairs.");

    /**
     * Parameter for the option, how noise should be treated.
     */
    public static final OptionID NOISE_OPTION_ID = new OptionID("davies-bouldin.noiseoption", "option, how noise should be treated.");

    /**
     * Distance function to use.
     */
    private PrimitiveDistanceFunction<NumberVector> distance;

    /**
     * Option, how noise should be treated.
     */
    private NoiseOption noiseOption = NoiseOption.IGNORE_NOISE_WITH_PENALTY;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PrimitiveDistanceFunction<NumberVector>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, PrimitiveDistanceFunction.class, ManhattanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      EnumParameter<NoiseOption> noiseP = new EnumParameter<NoiseOption>(NOISE_OPTION_ID, NoiseOption.class, NoiseOption.IGNORE_NOISE_WITH_PENALTY);
      if(config.grab(noiseP)) {
        noiseOption = noiseP.getValue();
      }

    }

    @Override
    protected EvaluateConcordantPairs<O> makeInstance() {
      return new EvaluateConcordantPairs<>(distance, noiseOption);
    }
  }

}
