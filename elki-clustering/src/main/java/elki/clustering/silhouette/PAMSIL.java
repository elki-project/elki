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
import elki.database.datastore.DataStoreFactory;
import elki.database.datastore.DataStoreUtil;
import elki.database.datastore.IntegerDataStore;
import elki.database.datastore.WritableIntegerDataStore;
import elki.database.ids.*;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.Relation;
import elki.distance.Distance;
import elki.logging.Logging;
import elki.logging.progress.IndefiniteProgress;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.LongStatistic;
import elki.result.Metadata;
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
 * 
 * @author Erich Schubert
 *
 * @param <O> Input data type
 */
@Reference(authors = "M. Van der Laan, K. Pollard, J. Bryan", //
    title = "A new partitioning around medoids algorithm", //
    booktitle = "Journal of Statistical Computation and Simulation 73(8)", //
    url = "https://doi.org/10.1080/0094965031000136012", //
    bibkey = "doi:10.1080/0094965031000136012")
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

  /**
   * Run PAMSIL
   *
   * @param relation relation to use
   * @return result
   */
  @Override
  public Clustering<MedoidModel> run(Relation<O> relation) {
    Clustering<MedoidModel> result = super.run(relation);
    Metadata.of(result).setLongName("PAMSIL Clustering");
    return result;
  }

  @Override
  protected void run(DistanceQuery<? super O> distQ, DBIDs ids, ArrayModifiableDBIDs medoids, WritableIntegerDataStore assignment) {
    new Instance(distQ, ids, assignment).run(medoids, maxiter);
  }

  /**
   * Instance for a single dataset.
   * <p>
   * Note: we experimented with not caching the distance to nearest and second
   * nearest, but only the assignments. The matrix lookup was more expensive, so
   * this is probably worth the 2*n doubles in storage.
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
     * @param means Cluster medoids
     */
    protected void assignToNearestCluster(ArrayDBIDs means) {
      DBIDArrayIter miter = means.iter();
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
          int c = assignment.intValue(y);
          sums[c] += distQ.distance(x, y);
          count[c] += 1;
        }
        int c = assignment.intValue(x);
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
        silsum += (b - a) / (a > b ? a : b);
      }
      return silsum / ids.size();
    }

    /**
     * Assign each object to the nearest cluster when replacing one medoid.
     *
     * @param prev Previous assignment
     * @param assignment New assignment
     * @param means Cluster medoids
     * @param pi Medoid position
     * @param h New medoid
     */
    protected void reassignToNearestCluster(IntegerDataStore prev, WritableIntegerDataStore assignment, ArrayDBIDs means, int pi, DBIDRef h) {
      DBIDArrayIter miter = means.iter();
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        final double d = distQ.distance(iditer, h); // new distance
        int minindx = prev.intValue(iditer);
        final double od = distQ.distance(iditer, miter.seek(minindx)); // old
        if(d < od) {
          minindx = pi; // assign to new center
        }
        else if(minindx == pi) {
          double mindist = d;
          minindx = pi;
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
