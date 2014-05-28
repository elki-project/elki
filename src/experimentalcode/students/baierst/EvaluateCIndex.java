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
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.Cluster;
import de.lmu.ifi.dbs.elki.data.Clustering;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.Evaluator;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.result.CollectionResult;
import de.lmu.ifi.dbs.elki.result.HierarchicalResult;
import de.lmu.ifi.dbs.elki.result.Result;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ObjectParameter;

/**
 * Compute the C-index of a data set.
 * 
 * Reference:
 * <p>
 * L. J. Hubert and J.R. Levin <br />
 * A general statistical framework for assessing categorical clustering in free
 * recall<br />
 * Psychol Bull 10, 1976
 * </p>
 * 
 * @author Stephan Baier
 * 
 */
public class EvaluateCIndex<O> implements Evaluator {
  /**
   * Logger for debug output.
   */
  private static final Logging LOG = Logging.getLogger(EvaluateSilhouette.class);

  /**
   * Keep noise "clusters" merged.
   */
  private boolean mergenoise = false;

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
  public EvaluateCIndex(PrimitiveDistanceFunction<? super NumberVector> distance, boolean mergenoise) {
    super();
    this.distanceFunction = distance;
    this.mergenoise = mergenoise;
  }

  /**
   * Evaluate a single clustering.
   * 
   * @param db Database
   * @param rel Data relation
   * @param c Clustering
   */
  public void evaluateClustering(Database db, Relation<? extends NumberVector> rel, Clustering<?> c) {
    List<? extends Cluster<?>> clusters = c.getAllClusters();

    /* w is the number of within group distances */
    int w = 0;
    ArrayList<Double> pairDists = new ArrayList<Double>();
    double theta = 0;

    for(Cluster<?> cluster : clusters) {
      for(DBIDIter it1 = cluster.getIDs().iter(); it1.valid(); it1.advance()) {
        for(Cluster<?> ocluster : clusters) {
          for(DBIDIter it2 = ocluster.getIDs().iter(); it2.valid(); it2.advance()) {
            if(DBIDUtil.equal(it1, it2)) {
              continue;
            }
            double dist = distanceFunction.distance(rel.get(it1), rel.get(it2));
            pairDists.add(dist);
            if(ocluster == cluster && ((!cluster.isNoise() && !mergenoise) || mergenoise)) {
              theta += dist;
              w++;
            }
          }
        }
      }
    }

    Collections.sort(pairDists);
    double min = 0;
    int i = 0;
    for(double dist : pairDists) {
      if(i >= w) {
        break;
      }
      min += dist;
      i++;
    }

    Collections.reverse(pairDists);
    double max = 0;
    i = 0;
    for(double dist : pairDists) {
      if(i >= w) {
        break;
      }
      max += dist;
      i++;
    }

    double cIndex = (theta - min) / (max - min);

    if(LOG.isVerbose()) {
      LOG.verbose("c-index: " + cIndex);
    }
    // Build a primitive result attachment:
    Collection<DoubleVector> col = new ArrayList<>();
    col.add(new DoubleVector(new double[] { cIndex }));
    db.getHierarchy().add(c, new CollectionResult<>("C-Index", "c-index", col));

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
    public static final OptionID DISTANCE_ID = new OptionID("c-index.distance", "Distance function to use for computing the c-index.");

    /**
     * Parameter to treat noise as a single cluster.
     */
    public static final OptionID MERGENOISE_ID = new OptionID("c-index.noisecluster", "Treat noise as a cluster, not as singletons.");

    /**
     * Distance function to use.
     */
    private PrimitiveDistanceFunction<NumberVector> distance;

    /**
     * Keep noise "clusters" merged.
     */
    private boolean mergenoise = false;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      ObjectParameter<PrimitiveDistanceFunction<NumberVector>> distanceFunctionP = new ObjectParameter<>(DISTANCE_ID, PrimitiveDistanceFunction.class, ManhattanDistanceFunction.class);
      if(config.grab(distanceFunctionP)) {
        distance = distanceFunctionP.instantiateClass(config);
      }

      Flag noiseP = new Flag(MERGENOISE_ID);
      if(config.grab(noiseP)) {
        mergenoise = noiseP.isTrue();
      }

    }

    @Override
    protected EvaluateCIndex<O> makeInstance() {
      return new EvaluateCIndex<>(distance, mergenoise);
    }
  }

}
