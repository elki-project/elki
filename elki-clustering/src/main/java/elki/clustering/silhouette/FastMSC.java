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

import elki.clustering.kmeans.initialization.RandomlyChosen;
import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
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
import elki.math.linearalgebra.VMath;
import elki.result.EvaluationResult;
import elki.result.Metadata;
import elki.result.EvaluationResult.MeasurementGroup;
import elki.utilities.documentation.Reference;
import elki.utilities.exceptions.AbortException;

/**
 * Fast Medoid Silhouette Clustering.
 * <p>
 * This clustering algorithm tries to find an optimal silhouette clustering
 * for an approximation to the silhouette called "medoid silhouette" using
 * a swap-based heuristic similar to PAM. By also caching the distance to the
 * third nearest center (compare to FastPAM, which only used the second
 * nearest), we are able to reduce the runtime per iteration to just O(n²),
 * which yields an acceptable run time for many use cases, while often finding
 * a solution with better silhouette than other clustering methods.
 * <p>
 * Reference:
 * <p>
 * Lars Lenssen and Erich Schubert<br>
 * Clustering by Direct Optimization of the Medoid Silhouette<br>
 * Int. Conf. on Similarity Search and Applications, SISAP 2022
 *
 * @author Erich Schubert
 *
 * @param <O>
 */
@Reference(authors = "Lars Lenssen and Erich Schubert", //
    title = "Clustering by Direct Optimization of the Medoid Silhouette", //
    booktitle = "Int. Conf. on Similarity Search and Applications, SISAP 2022", //
    url = "https://doi.org/10.1007/978-3-031-17849-8_15", bibkey = "DBLP:conf/sisap/LenssenS22")
public class FastMSC<O> extends PAMMEDSIL<O> {
  /**
   * The logger for this class.
   */
  private static final Logging LOG = Logging.getLogger(FastMSC.class);

  /**
   * Constructor.
   *
   * @param distance Distance function
   * @param k Number of cluster
   * @param maxiter Maximum number of iterations
   * @param initializer Initialization
   */
  public FastMSC(Distance<? super O> distance, int k, int maxiter, KMedoidsInitialization<O> initializer) {
    super(distance, k, maxiter, initializer);
  }

  @Override
  public Clustering<MedoidModel> run(Relation<O> relation, int k, DistanceQuery<? super O> distQ) {
    DBIDs ids = relation.getDBIDs();
    ArrayModifiableDBIDs medoids = initialMedoids(distQ, ids, k);
    WritableIntegerDataStore assignment = DataStoreUtil.makeIntegerStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, -1);
    Duration optd = getLogger().newDuration(getClass().getName() + ".optimization-time").begin();
    DoubleDataStore silhouettes;
    double sil;
    if(k == 2) { // optimized codepath for k=2
      Instance2 instance = new Instance2(distQ, ids, assignment);
      sil = instance.run(medoids, maxiter);
      silhouettes = instance.silhouetteScores();
    }
    else {
      Instance instance = new Instance(distQ, ids, assignment);
      sil = instance.run(medoids, maxiter);
      silhouettes = instance.silhouetteScores();
    }
    getLogger().statistics(optd.end());
    Clustering<MedoidModel> res = wrapResult(ids, assignment, medoids, "FastMSC Clustering");
    Metadata.hierarchyOf(res).addChild(new MaterializedDoubleRelation(Silhouette.SILHOUETTE_NAME, ids, silhouettes));
    EvaluationResult ev = EvaluationResult.findOrCreate(res, "Internal Clustering Evaluation");
    MeasurementGroup g = ev.findOrCreateGroup("Distance-based");
    g.addMeasure("Medoid Silhouette", sil, -1., 1., 0., false);
    return res;
  }

  /**
   * Simplified FastMSC clustering instance for k=2.
   * <p>
   * For k=2, we can use a much simpler logic.
   *
   * @author Erich Schubert
   */
  protected class Instance2 {
    /**
     * Ids to process.
     */
    protected DBIDs ids;

    /**
     * Distance function to use.
     */
    protected DistanceQuery<?> distQ;

    /**
     * Distances to the first medoid.
     */
    protected WritableDoubleDataStore dm0;

    /**
     * Distances to the second medoid.
     */
    protected WritableDoubleDataStore dm1;

    /**
     * Output cluster mapping.
     */
    protected WritableIntegerDataStore assignment;

    /**
     * Constructor.
     *
     * @param distQ Distance query
     * @param ids IDs to process
     * @param assignment Cluster assignment
     */
    public Instance2(DistanceQuery<?> distQ, DBIDs ids, WritableIntegerDataStore assignment) {
      this.distQ = distQ;
      this.ids = ids;
      this.dm0 = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
      this.dm1 = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP);
      this.assignment = assignment;
    }

    /**
     * Run the FastMSC optimization phase.
     *
     * @param medoids Initial medoids list
     * @param maxiter Maximum number of iterations
     * @return final medoid Silhouette
     */
    protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
      final int k = medoids.size();
      assert k == 2;
      // Initial assignment to nearest medoids
      double sil = assignToNearestCluster(medoids);
      DBIDArrayIter m = medoids.iter();
      String key = getClass().getName().replace("$Instance", "");
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(key + ".iteration-" + 0 + ".medoid-silhouette", sil));
      }
      double[] scratch = new double[k];

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("FastMSC iteration", LOG) : null;
      // Swap phase
      DBIDVar bestid = DBIDUtil.newVar();
      int iteration = 0;
      while(iteration < maxiter || maxiter <= 0) {
        ++iteration;
        LOG.incrementProcessed(prog);
        // Try to swap a non-medoid with a medoid member:
        double best = 0;
        int bestcluster = -1;
        // Iterate over all non-medoids:
        for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
          // Compare object to its own medoid.
          if(DBIDUtil.equal(m.seek(assignment.intValue(j)), j)) {
            continue; // This is a medoid.
          }
          Arrays.fill(scratch, 0);
          findBestSwap(j, scratch);
          int b = scratch[0] > scratch[1] ? 0 : 1;
          double l = scratch[b];
          if(l > best) {
            best = l;
            bestid.set(j);
            bestcluster = b;
          }
        }
        if(best <= sil) {
          break;
        }
        medoids.set(bestcluster, bestid);
        sil = doSwap(medoids, bestcluster, bestid);
        if(LOG.isStatistics()) {
          LOG.statistics(new DoubleStatistic(key + ".iteration-" + iteration + ".medoid-silhouette", sil));
        }
      }
      LOG.setCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(key + ".iterations", iteration));
        LOG.statistics(new DoubleStatistic(key + ".final-medoid-silhouette", sil));
      }
      return sil;
    }

    /**
     * Assign each object to the nearest cluster.
     *
     * @param means Cluster medoids
     * @return loss
     */
    protected double assignToNearestCluster(ArrayDBIDs means) {
      DBIDArrayIter miter = means.iter();
      double silsum = 0;
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        final double di0 = distQ.distance(iditer, miter.seek(0));
        final double di1 = distQ.distance(iditer, miter.seek(1));
        assignment.putInt(iditer, di0 < di1 ? 0 : 1);
        dm0.putDouble(iditer, di0);
        dm1.putDouble(iditer, di1);
        silsum += di0 < di1 ? loss(di0, di1) : loss(di1, di0);
      }
      return 1. - silsum / ids.size();
    }

    /**
     * Compute the loss change when choosing j as new medoid.
     *
     * @param j New medoid
     * @param ploss Loss array
     */
    protected void findBestSwap(DBIDRef j, double[] ploss) {
      for(DBIDIter o = ids.iter(); o.valid(); o.advance()) {
        final double djo = distQ.distance(j, o);
        final double dm0o = dm0.doubleValue(o);
        final double dm1o = dm1.doubleValue(o);
        ploss[0] += (djo < dm1o) ? loss(djo, dm1o) : loss(dm1o, djo);
        ploss[1] += (djo < dm0o) ? loss(djo, dm0o) : loss(dm0o, djo);
      }
      ploss[0] = 1 - ploss[0] / ids.size();
      ploss[1] = 1 - ploss[1] / ids.size();
    }

    /**
     * Assign each object to the nearest cluster when replacing one medoid.
     *
     * @param medoids Cluster medoids
     * @param b Medoid position index
     * @param j New medoid
     * @return medoid silhouette
     */
    protected double doSwap(ArrayDBIDs medoids, int b, DBIDRef j) {
      double silsum = 0;
      WritableDoubleDataStore dmm = b == 0 ? dm0 : dm1;
      DoubleDataStore dmx = b == 0 ? dm1 : dm0;
      for(DBIDIter o = ids.iter(); o.valid(); o.advance()) {
        final double djo = distQ.distance(j, o);
        dmm.putDouble(o, djo);
        final double dmo = dmx.doubleValue(o);
        int a = djo < dmo ? b : djo > dmo ? 1 - b : assignment.intValue(o);
        assignment.putInt(o, a);
        silsum += djo < dmo ? loss(djo, dmo) : loss(dmo, djo);
      }
      return 1. - silsum / ids.size();
    }

    /**
     * Get the silhouette scores per point (must be run() first)
     *
     * @return Silhouette scores
     */
    public DoubleDataStore silhouetteScores() {
      WritableDoubleDataStore silhouettes = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        final int a = assignment.intValue(iter);
        final double d1 = (a == 0 ? dm0 : dm1).doubleValue(iter);
        final double d2 = (a == 0 ? dm1 : dm0).doubleValue(iter);
        silhouettes.putDouble(iter, d1 > 0 ? 1. - d1 / d2 : 1.);
      }
      return silhouettes;
    }
  }

  /**
   * Data stored per point. Unfortunately, this introduces noticable overheads
   * from Java memory management over the Rust version.
   *
   * @author Erich Schubert
   */
  protected static class Record {
    /** Nearest medoid */
    int m1 = -1;

    /** Second nearest medoid */
    int m2 = -1;

    /** Third nearest medoid */
    int m3 = -1;

    /** Distance to nearest medoid */
    double d1 = Double.POSITIVE_INFINITY;

    /** Distance to second nearest medoid */
    double d2 = Double.POSITIVE_INFINITY;

    /** Distance to third nearest medoid */
    double d3 = Double.POSITIVE_INFINITY;

    @Override
    public String toString() {
      return "Record [m1=" + m1 + ", m2=" + m2 + ", m3=" + m3 + ", d1=" + d1 + ", d2=" + d2 + ", d3=" + d3 + "]";
    }

  }

  /**
   * FastMSC clustering instance for a particular data set.
   *
   * @author Erich Schubert
   */
  protected class Instance {
    /**
     * Ids to process.
     */
    protected DBIDs ids;

    /**
     * Distance function to use.
     */
    protected DistanceQuery<?> distQ;

    /**
     * Distances and nearest medoids.
     */
    protected WritableDataStore<Record> assignment;

    /**
     * Output cluster mapping.
     */
    protected WritableIntegerDataStore output;

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
      this.assignment = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_HOT | DataStoreFactory.HINT_TEMP, Record.class);
      this.output = assignment;
    }

    /**
     * Run the FastMSC optimization phase.
     *
     * @param medoids Initial medoids list
     * @param maxiter Maximum number of iterations
     * @return final medoid Silhouette
     */
    protected double run(ArrayModifiableDBIDs medoids, int maxiter) {
      final int k = medoids.size();
      // Initial assignment to nearest medoids
      double sil = assignToNearestCluster(medoids);
      DBIDArrayIter m = medoids.iter();
      String key = getClass().getName().replace("$Instance", "");
      if(LOG.isStatistics()) {
        LOG.statistics(new DoubleStatistic(key + ".iteration-" + 0 + ".medoid-silhouette", sil));
      }
      double[] losses = new double[k], scratch = new double[k];
      updateRemovalLoss(losses);

      IndefiniteProgress prog = LOG.isVerbose() ? new IndefiniteProgress("FastMSC iteration", LOG) : null;
      // Swap phase
      DBIDVar bestid = DBIDUtil.newVar();
      int iteration = 0;
      while(iteration < maxiter || maxiter <= 0) {
        ++iteration;
        LOG.incrementProcessed(prog);
        // Try to swap a non-medoid with a medoid member:
        double best = 0;
        int bestcluster = -1;
        // Iterate over all non-medoids:
        for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
          // Compare object to its own medoid.
          if(DBIDUtil.equal(m.seek(assignment.get(j).m1), j)) {
            continue; // This is a medoid.
          }
          System.arraycopy(losses, 0, scratch, 0, k);
          double acc = findBestSwap(j, scratch);
          // Find the best possible swap for j:
          int b = VMath.argmax(scratch);
          double l = scratch[b] + acc;
          if(l > best) {
            best = l;
            bestid.set(j);
            bestcluster = b;
          }
        }
        if(best <= 0.) {
          break;
        }
        medoids.set(bestcluster, bestid);
        sil = doSwap(medoids, bestcluster, bestid);
        if(LOG.isStatistics()) {
          LOG.statistics(new DoubleStatistic(key + ".iteration-" + iteration + ".medoid-silhouette", sil));
        }
        updateRemovalLoss(losses);
      }
      LOG.setCompleted(prog);
      if(LOG.isStatistics()) {
        LOG.statistics(new LongStatistic(key + ".iterations", iteration));
        LOG.statistics(new DoubleStatistic(key + ".final-medoid-silhouette", sil));
      }
      // Unwrap records into simple labeling:
      for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
        output.putInt(j, assignment.get(j).m1);
      }
      return sil;
    }

    /**
     * Assign each object to the nearest cluster.
     *
     * @param means Cluster medoids
     * @return loss
     */
    protected double assignToNearestCluster(ArrayDBIDs means) {
      DBIDArrayIter miter = means.iter();
      double loss = 0;
      for(DBIDIter iditer = ids.iter(); iditer.valid(); iditer.advance()) {
        Record rec = new Record();
        for(miter.seek(0); miter.valid(); miter.advance()) {
          final double dist = distQ.distance(iditer, miter);
          if(dist < rec.d1) {
            rec.m3 = rec.m2;
            rec.d3 = rec.d2;
            rec.m2 = rec.m1;
            rec.d2 = rec.d1;
            rec.m1 = miter.getOffset();
            rec.d1 = dist;
          }
          else if(dist < rec.d2) {
            rec.m3 = rec.m2;
            rec.d3 = rec.d2;
            rec.m2 = miter.getOffset();
            rec.d2 = dist;
          }
          else if(dist < rec.d3) {
            rec.m3 = miter.getOffset();
            rec.d3 = dist;
          }
        }
        if(rec.m2 < 0) {
          throw new AbortException("Too many infinite distances. Cannot assign objects.");
        }
        assignment.put(iditer, rec);
        loss += rec.d1 / rec.d2;
        assert rec.m1 != rec.m2 && rec.m1 != rec.m3 && rec.m2 != rec.m3;
        assert rec.d1 <= rec.d2;
        assert rec.d2 <= rec.d3;
      }
      return 1. - loss / ids.size();
    }

    /**
     * Compute the loss change when choosing j as new medoid.
     *
     * @param j New medoid
     * @param ploss Loss array
     * @return Shared loss term
     */
    protected double findBestSwap(DBIDRef j, double[] ploss) {
      double acc = 0;
      for(DBIDIter o = ids.iter(); o.valid(); o.advance()) {
        final double djo = distQ.distance(j, o);
        Record reco = assignment.get(o);
        if(djo < reco.d1) {
          acc += loss(reco.d1, reco.d2) - loss(djo, reco.d1);
          ploss[reco.m1] += loss(djo, reco.d1) + loss(reco.d2, reco.d3) - loss(reco.d1 + djo, reco.d2);
          ploss[reco.m2] += loss(reco.d1, reco.d3) - loss(reco.d1, reco.d2);
        }
        else if(djo < reco.d2) {
          acc += loss(reco.d1, reco.d2) - loss(reco.d1, djo);
          ploss[reco.m1] += loss(reco.d1, djo) + loss(reco.d2, reco.d3) - loss(reco.d1 + djo, reco.d2);
          ploss[reco.m2] += loss(reco.d1, reco.d3) - loss(reco.d1, reco.d2);
        }
        else if(djo < reco.d3) {
          ploss[reco.m1] += loss(reco.d2, reco.d3) - loss(reco.d2, djo);
          ploss[reco.m2] += loss(reco.d1, reco.d3) - loss(reco.d1, djo);
        }
      }
      return acc;
    }

    /**
     * Assign each object to the nearest cluster when replacing one medoid.
     *
     * @param medoids Cluster medoids
     * @param b Medoid position index
     * @param j New medoid
     * @return medoid silhouette
     */
    protected double doSwap(ArrayDBIDs medoids, int b, DBIDRef j) {
      DBIDArrayIter miter = medoids.iter();
      assert DBIDUtil.equal(j, miter.seek(b)); // must already be filled
      double silsum = 0;
      for(DBIDIter o = ids.iter(); o.valid(); o.advance()) {
        final Record rec = assignment.get(o);
        if(DBIDUtil.equal(j, o)) {
          // new medoid:
          if(rec.m1 != b) {
            if(rec.m2 != b) {
              rec.m3 = rec.m2;
              rec.d3 = rec.d2;
            }
            rec.m2 = rec.m1;
            rec.d2 = rec.d1;
          }
          rec.m1 = b;
          rec.d1 = 0;
          continue;
        }
        final double djo = distQ.distance(j, o);
        if(rec.m1 == b) {
          // Nearest replaced
          if(djo < rec.d2) {
            rec.d1 = djo;
          }
          else if(djo < rec.d3) {
            rec.m1 = rec.m2;
            rec.d1 = rec.d2;
            rec.m2 = b;
            rec.d2 = djo;
          }
          else {
            rec.m1 = rec.m2;
            rec.d1 = rec.d2;
            rec.m2 = rec.m3;
            rec.d2 = rec.d3;
            updateThirdNearest(o, rec, b, djo, miter);
          }
        }
        else if(rec.m2 == b) {
          // second nearest replaced
          if(djo < rec.d1) {
            rec.m2 = rec.m1;
            rec.d2 = rec.d1;
            rec.m1 = b;
            rec.d1 = djo;
          }
          else if(djo < rec.d3) {
            rec.m2 = b;
            rec.d2 = djo;
          }
          else {
            rec.m2 = rec.m3;
            rec.d2 = rec.d3;
            updateThirdNearest(o, rec, b, djo, miter);
          }
        }
        else {
          // third or later
          if(djo < rec.d1) {
            rec.m3 = rec.m2;
            rec.d3 = rec.d2;
            rec.m2 = rec.m1;
            rec.d2 = rec.d1;
            rec.m1 = b;
            rec.d1 = djo;
          }
          else if(djo < rec.d2) {
            rec.m3 = rec.m2;
            rec.d3 = rec.d2;
            rec.m2 = b;
            rec.d2 = djo;
          }
          else if(djo < rec.d3) {
            rec.m3 = b;
            rec.d3 = djo;
          }
          else if(rec.m3 == b) {
            updateThirdNearest(o, rec, b, djo, miter);
          }
        }
        silsum += loss(rec.d1, rec.d2);
      }
      return 1. - silsum / ids.size();
    }

    /**
     * Update the third nearest in the record.
     *
     * @param j Current object
     * @param rec Current record
     * @param m Medoid id replaced
     * @param bestd distance to medoid
     * @param miter Medoid iterator
     */
    protected void updateThirdNearest(DBIDRef j, Record rec, int m, double bestd, DBIDArrayIter miter) {
      if(k == 3) {
        rec.m3 = m;
        rec.d3 = bestd;
        return;
      }
      int best = m;
      for(miter.seek(0); miter.valid(); miter.advance()) {
        if(miter.getOffset() == m || miter.getOffset() == rec.m1 || miter.getOffset() == rec.m2) {
          continue;
        }
        final double d = distQ.distance(j, miter);
        if(d < bestd) {
          best = miter.getOffset();
          bestd = d;
        }
      }
      rec.m3 = best;
      rec.d3 = bestd;
      assert rec.m1 != rec.m2 && rec.m1 != rec.m3 && rec.m2 != rec.m3;
      assert rec.d1 <= rec.d2;
      assert rec.d2 <= rec.d3;
    }

    /**
     * Update the share removal loss data
     *
     * @param losses Removal loss storage
     */
    protected void updateRemovalLoss(double[] losses) {
      Arrays.fill(losses, 0);
      for(DBIDIter j = ids.iter(); j.valid(); j.advance()) {
        Record reco = assignment.get(j);
        final double l12 = loss(reco.d1, reco.d2);
        losses[reco.m1] += l12 - loss(reco.d2, reco.d3);
        losses[reco.m2] += l12 - loss(reco.d1, reco.d3);
      }
    }

    /**
     * Get the silhouette scores per point (must be run() first)
     *
     * @return Silhouette scores
     */
    public DoubleDataStore silhouetteScores() {
      WritableDoubleDataStore silhouettes = DataStoreUtil.makeDoubleStorage(ids, DataStoreFactory.HINT_DB);
      for(DBIDIter iter = ids.iter(); iter.valid(); iter.advance()) {
        final Record rec = assignment.get(iter);
        silhouettes.putDouble(iter, rec.d1 > 0 ? 1. - rec.d1 / rec.d2 : 1.);
      }
      return silhouettes;
    }
  }

  /**
   * Loss function used - here simply a/b, 0 if a=b=0.
   *
   * @param a distance to nearest
   * @param b distance to second
   * @return loss, a/b or 0.
   */
  protected static final double loss(double a, double b) {
    return a > 0 && b > 0 ? a / b : 0;
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
  public static class Par<O> extends PAMMEDSIL.Par<O> {
    @SuppressWarnings("rawtypes")
    @Override
    protected Class<? extends KMedoidsInitialization> defaultInitializer() {
      return RandomlyChosen.class;
    }

    @Override
    public FastMSC<O> make() {
      return new FastMSC<>(distance, k, maxiter, initializer);
    }
  }
}
