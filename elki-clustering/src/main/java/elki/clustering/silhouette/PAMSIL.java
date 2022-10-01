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
package elki.clustering.silhouette;

import java.util.Arrays;

import elki.clustering.kmedoids.PAM;
import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.clustering.kmedoids.initialization.KMedoidsKMedoidsInitialization;
import elki.data.Clustering;
import elki.data.model.MedoidModel;
import elki.database.datastore.*;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.MaterializedDoubleRelation;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.evaluation.clustering.internal.Silhouette;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.logging.statistics.LongStatistic;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;

/**
 * Clustering to optimize the Silhouette coefficient with a PAM-based swap
 * heuristic. This is the baseline algorithm, but it is fairly expensive:
 * each iteration it considers (n-k)k swaps, for each the Silhouette needs to
 * be evaluated at n² cost. The overall complexity per iteration hence is
 * O((n-k)k ((n-k)+n²)), i.e., O(n³) for small k, making this a quite expensive
 * clustering method.
 * <p>
 * Reference:
 * <p>
 * M. Van der Laan, K. Pollard, J. Bryan<br>
 * A new partitioning around medoids algorithm<br>
 * Journal of Statistical Computation and Simulation 73(8)
 * <p>
 * This already incorporates some optimizations from:
 * <p>
 * Lars Lenssen and Erich Schubert<br>
 * Clustering by Direct Optimization of the Medoid Silhouette<br>
 * Int. Conf. on Similarity Search and Applications, SISAP 2022
 * 
 * @author Erich Schubert
 * @since 0.8.0
 *
 * @param <O> Input data type
 */
@Reference(authors = "M. Van der Laan, K. Pollard, J. Bryan", //
    title = "A new partitioning around medoids algorithm", //
    booktitle = "Journal of Statistical Computation and Simulation 73(8)", //
    url = "https://doi.org/10.1080/0094965031000136012", //
    bibkey = "doi:10.1080/0094965031000136012")
@Reference(authors = "Lars Lenssen and Erich Schubert", //
    title = "Clustering by Direct Optimization of the Medoid Silhouette", //
    booktitle = "Int. Conf. on Similarity Search and Applications, SISAP 2022", //
    url = "https://doi.org/10.1007/978-3-031-17849-8_15", bibkey = "DBLP:conf/sisap/LenssenS22")
public class PAMSIL<O> extends PAM<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PAMSIL.class);

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public PAMSIL(Distance<? super O> distance, int k, int maxiter, KMedoidsInitialization<O> initializer) {
    super(distance, k, maxiter, initializer);
  }

  @Override
  public Clustering<MedoidModel> run(Relation<O> relation, int k, DistanceQuery<? super O> distQ) {
    DBIDs ids = relation.getDBIDs();
    ArrayModifiableDBIDs medoids = initialMedoids(distQ, ids, k);
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
    Duration optd = getLogger().newDuration(getClass().getName() + ".optimization-time").begin();
    Instance instance = new Instance(distQ, ids, assignment);
    double sil = instance.run(medoids, maxiter);
    DoubleDataStore silhouettes = instance.silhouetteScores();
    getLogger().statistics(optd.end());
    Clustering<MedoidModel> res = wrapResult(ids, assignment, medoids, "PAMSIL Clustering");
    Metadata.hierarchyOf(res).addChild(new MaterializedDoubleRelation(Silhouette.SILHOUETTE_NAME, ids, silhouettes));
    EvaluationResult ev = EvaluationResult.findOrCreate(res, "Internal Clustering Evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based");
    g.addMeasure("Silhouette", sil, -1., 1., 0., false);
    return res;
  }

  /**
   * Instance for a single dataset.
   *
   * @author Erich Schubert
   */
  protected static class Instance {
    /**
     * Ids to process.
     */
    DBIDs ids;

    /**
     * Distance function to use.
     */
    DistanceQuery<?> distQ;

    /**
     * Cluster mapping.
     */
    WritableIntegerDataStore assignment;

    /**
     * Scratch cluster mapping.
     */
    WritableIntegerDataStore scratch;

    /**
     * Store the per-point silhouette scores for plotting.
     */
    WritableDoubleDataStore silhouettes;

    /**
     * Constructor.
     *
     * @param distQ Distance query
     * @param ids IDs to process
     * @param assignment Cluster assignment
     */
    public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment) {
      this.distQ = distQ;
      this.ids = ids;
      this.assignment = assignment;
      this.scratch = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
      this.silhouettes = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
    }

    /**
     * Run the PAMSIL optimization phase.
     *
     * @param medoids Initial medoids list
     * @param maxiter Maximum number of iterations
     * @return final Silhouette
     */
    protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
      final int k = medoids.size();
      // Initial assignment to nearest medoids
      assignToNearestCluster(medoids);
      double sil = silhouette(assignment, medoids.size());
      String key = getClass().getName().replace("$Instance", "");
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(key + ".iteration-" + 0 + ".silhouette", sil));
      }

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("PAMSIL iteration", LOG) : null;
      // Swap phase
      DBIDVar bestid = DBIDUtil.newVar();
      DBIDArrayIter m = medoids.iter();
      int iteration = 0;
      while(iteration < maxiter || maxiter <= 0) {
        ++iteration;
        LOG.incrementProcessed(prog);
        // Try to swap a non-medoid with a medoid member:
        double best = sil;
        int bestcluster = -1;
        // Iterate over all non-medoids:
        for(DBIDIter h = ids.iter(); h.valid(); h.advance()) {
          // Compare object to its own medoid.
          if(DBIDUtil.equal(m.seek(assignment.intValue(h)), h)) {
            continue; // This is a medoid.
          }
          // Find the best possible swap for h:
          for(int pi = 0; pi < k; pi++) {
            reassignToNearestCluster(assignment, scratch, medoids, pi, h);
            final double csil = silhouette(scratch, k);
            if(csil > best) {
              best = csil;
              bestid.set(h);
              bestcluster = pi;
            }
          }
        }
        if(best <= sil) {
          break;
        }
        medoids.set(bestcluster, bestid);
        if(LOG.isStatistics()) {
          LOG.statistics(new DoubleStatistic(key + ".iteration-" + iteration + ".silhouette", best));
        }
        sil = best;
        reassignToNearestCluster(assignment, assignment, medoids, bestcluster, bestid);
      }
      LOG.setCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(key + ".iterations", iteration));
        LOG.statistics(new DoubleStatistic(key + ".final-silhouette", sil));
      }
      return sil;
    }

    /**
     * Assign each object to the nearest cluster.
     *
     * @param medoids Cluster medoids
     */
    protected void assignToNearestCluster(ArrayDBIDs medoids) {
      DBIDArrayIter miter = medoids.iter();
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        double mindist = Double.POSITIVE_INFINITY;
        int minindx = -1;
        for(miter.seek(0); miter.valid(); miter.advance()) {
          final double dist = distQ.distance(iditer, miter);
          if(dist < mindist) {
            minindx = miter.getOffset();
            mindist = dist;
          }
        }
        if(minindx < 0) {
          throw new AbortException("Too many infinite distances. Cannot assign objects.");
        }
        assert minindx < medoids.size();
        assignment.put(iditer, minindx);
      }
    }

    /**
     * Evaluate the average Silhouette of the current cluster assignment
     *
     * @param assignment cluster assignment
     * @param k number of clusters (for memory allocation)
     * @return Average silhouette width
     */
    protected double silhouette(IntegerDataStore assignment, int k) {
      double silsum = 0;
      double[] sums = new double[k];
      int[] count = new int[k];
      for(DBIDIter x = ids.iter(); x.valid(); x.advance()) {
        Arrays.fill(sums, 0);
        Arrays.fill(count, 0);
        for(DBIDIter y = ids.iter(); y.valid(); y.advance()) {
          if(DBIDUtil.equal(x, y)) {
            continue;
          }
          final int c = assignment.intValue(y);
          sums[c] += distQ.distance(x, y);
          count[c] += 1;
        }
        final int c = assignment.intValue(x);
        if(count[c] == 0) {
          continue; // else: s(x)=0
        }
        final double a = sums[c] / count[c];
        double b = Double.POSITIVE_INFINITY;
        for(int i = 0; i < k; i++) {
          if(i != c) {
            final double avg = sums[i] / count[i];
            b = !Double.isNaN(avg) && avg < b ? avg : b;
          }
        }
        final double s = (b - a) / (a > b ? a : b);
        silhouettes.putDouble(x, s);
        silsum += s;
      }
      return silsum / ids.size();
    }

    /**
     * Assign each object to the nearest cluster when replacing one medoid.
     *
     * @param prev Previous assignment
     * @param assignment New assignment
     * @param medoids Previous cluster medoids
     * @param pi Medoid position
     * @param h New medoid
     */
    protected void reassignToNearestCluster(IntegerDataStore prev, WritableIntegerDataStore assignment, ArrayDBIDs medoids, int pi, DBIDRef h) {
      DBIDArrayIter miter = medoids.iter();
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        double mindist = distQ.distance(iditer, h); // new distance
        int minindx = prev.intValue(iditer);
        final double od = DBIDUtil.equal(miter.seek(minindx), h) ? mindist : distQ.distance(iditer, miter); // old
        if(mindist <= od) {
          minindx = pi; // assign to new center, must be closest
        }
        else if(minindx == pi) {
          for(miter.seek(0); miter.valid(); miter.advance()) {
            if(miter.getOffset() != pi) {
              final double dist = distQ.distance(iditer, miter);
              if(dist < mindist) {
                minindx = miter.getOffset();
                mindist = dist;
              }
            }
          }
        }
        if(minindx < 0) {
          throw new AbortException("Too many infinite distances. Cannot assign objects.");
        }
        assignment.put(iditer, minindx);
      }
    }

    public DoubleDataStore silhouetteScores() {
      return silhouettes;
    }
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Par<O> extends PAM.Par<O> {
    @SuppressWarnings("rawtypes")
    @Override
    protected Class<? extends KMedoidsInitialization> defaultInitializer() {
      return KMedoidsKMedoidsInitialization.class;
    }

    @Override
    public PAMSIL<O> make() {
      return new PAMSIL<>(distance, k, maxiter, initializer);
    }
  }
}
