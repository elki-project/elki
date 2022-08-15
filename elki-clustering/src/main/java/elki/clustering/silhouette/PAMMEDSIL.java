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

import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.Clustering;
import elki.data.model.MedoidModel;
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

/**
 * Clustering to optimize the Medoid Silhouette coefficient with a PAM-based
 * swap heuristic. This is less expensive than using the full Silhouette, as
 * the medoid Silhouette is only in O(kn) as opposed to O(n²) for the full
 * Silhouette. Each iteration considers (n-k)k swaps, and hence the overall
 * complexity per iteration hence is
 * O((n-k)k(n-k)k), i.e., roughly O(n²k²).
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
public class PAMMEDSIL<O> extends PAMSIL<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(PAMMEDSIL.class);

  /**
   * Constructor.
   *
   * @param distance distance function
   * @param k k parameter
   * @param maxiter Maxiter parameter
   * @param initializer Function to generate the initial means
   */
  public PAMMEDSIL(Distance<? super O> distance, int k, int maxiter, KMedoidsInitialization<O> initializer) {
    super(distance, k, maxiter, initializer);
  }

  /**
   * Run PAMMEDSIL
   *
   * @param relation relation to use
   * @return result
   */
  @Override
  public Clustering<MedoidModel> run(Relation<O> relation) {
    Clustering<MedoidModel> result = super.run(relation);
    Metadata.of(result).setLongName("PAMMEDSIL Clustering");
    return result;
  }

  @Override
  protected void run(DistanceQuery<? super O> distQ, DBIDs ids, ArrayModifiableDBIDs medoids, WritableIntegerDataStore assignment) {
    new Instance(distQ, ids, assignment).run(medoids, maxiter);
  }

  /**
   * Instance for a single dataset.
   *
   * @author Erich Schubert
   */
  protected static class Instance extends PAMSIL.Instance {
    /**
     * Constructor.
     *
     * @param distQ Distance query
     * @param ids IDs to process
     * @param assignment Cluster assignment
     */
    public Instance(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment) {
      super(distQ, ids, assignment);
    }

    /**
     * Run the PAMMEDSIL optimization phase.
     *
     * @param medoids Initial medoids list
     * @param maxiter Maximum number of iterations
     * @return final medoid Silhouette
     */
    @Override
    protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
      final int k = medoids.size();
      // Initial assignment to nearest medoids
      assignToNearestCluster(medoids);
      DBIDArrayIter m = medoids.iter();
      double sil = medoidsilhouette(assignment, m);
      String key = getClass().getName().replace("$Instance", "");
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(key + ".iteration-" + 0 + ".medoid-silhouette", sil));
      }

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("PAMMEDSIL iteration", LOG) : null;
      // Swap phase
      DBIDVar bestid = DBIDUtil.newVar();
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
            final double csil = medoidsilhouette(scratch, m);
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
          LOG.statistics(new DoubleStatistic(key + ".iteration-" + iteration + ".medoid-silhouette", best));
        }
        sil = best;
        reassignToNearestCluster(assignment, assignment, medoids, bestcluster, bestid);
      }
      LOG.setCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(key + ".iterations", iteration));
        LOG.statistics(new DoubleStatistic(key + ".final-medoid-silhouette", sil));
      }
      return sil;
    }

    /**
     * Evaluate the average medoid Silhouette of the current cluster assignment
     *
     * @param assignment cluster assignment
     * @param m medoid iterator
     * @return Average silhouette width
     */
    protected double medoidsilhouette(IntegerDataStore assignment, DBIDArrayIter m) {
      double silsum = 0;
      for(DBIDIter x = ids.iter(); x.valid(); x.advance()) {
        int c = assignment.intValue(x);
        double a = Double.NaN, b = Double.POSITIVE_INFINITY;
        for(m.seek(0); m.valid(); m.advance()) {
          if(m.getOffset() == c) {
            a = DBIDUtil.equal(x, m) ? 0 : distQ.distance(x, m);
          }
          else {
            double d = distQ.distance(x, m);
            b = d < b ? d : b;
          }
        }
        silsum += (b - a) / (a > b ? a : b);
      }
      return silsum / ids.size();
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
  public static class Par<O> extends PAMSIL.Par<O> {
    @Override
    public PAMMEDSIL<O> make() {
      return new PAMMEDSIL<>(distance, k, maxiter, initializer);
    }
  }
}
