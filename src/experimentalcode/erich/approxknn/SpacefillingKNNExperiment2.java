package experimentalcode.erich.approxknn;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
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
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.ArrayDBIDs;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.DoubleDistanceResultPair;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.PrimitiveDoubleDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.AbstractSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.PeanoSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.datastructures.heap.KNNHeap;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;
import experimentalcode.erich.HilbertSpatialSorter;

/**
 * Simple experiment to estimate the effects of approximating the kNN with space
 * filling curves.
 * 
 * @author Erich Schubert
 */
public class SpacefillingKNNExperiment2 {
  private static final Logging logger = Logging.getLogger(SpacefillingKNNExperiment2.class);

  PrimitiveDoubleDistanceFunction<? super NumberVector<?, ?>> distanceFunction = ManhattanDistanceFunction.STATIC;

  private void run() {
    Database database = loadDatabase();
    Relation<NumberVector<?, ?>> rel = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    DBIDs ids = rel.getDBIDs();
    Random rnd = new Random(0);

    List<SpatialRef> zs = new ArrayList<SpatialRef>(ids.size());
    List<SpatialRef> ps = new ArrayList<SpatialRef>(ids.size());
    List<SpatialRef> hs = new ArrayList<SpatialRef>(ids.size());
    List<SpatialRef> zs2 = new ArrayList<SpatialRef>(ids.size());
    List<SpatialRef> ps2 = new ArrayList<SpatialRef>(ids.size());
    List<SpatialRef> hs2 = new ArrayList<SpatialRef>(ids.size());
    List<SpatialRef> zs3 = new ArrayList<SpatialRef>(ids.size());
    List<SpatialRef> ps3 = new ArrayList<SpatialRef>(ids.size());
    List<SpatialRef> hs3 = new ArrayList<SpatialRef>(ids.size());
    {
      for(DBID id : ids) {
        final NumberVector<?, ?> v = rel.get(id);
        SpatialRef ref = new SpatialRef(id, v);
        zs.add(ref);
        ps.add(ref);
        hs.add(ref);
        zs2.add(ref);
        ps2.add(ref);
        hs2.add(ref);
        zs3.add(ref);
        ps3.add(ref);
        hs3.add(ref);
      }

      // Sort spatially
      double[] mms = AbstractSpatialSorter.computeMinMax(zs);
      (new ZCurveSpatialSorter()).sort(zs, 0, zs.size(), mms);
      (new PeanoSpatialSorter()).sort(ps, 0, ps.size(), mms);
      (new HilbertSpatialSorter()).sort(hs, 0, hs.size(), mms);
      double[] mms2 = new double[mms.length];
      double[] mms3 = new double[mms.length];
      for(int i = 0; i < mms.length; i += 2) {
        double len = mms[i + 1] - mms[i];
        mms2[i] = mms[i] - len * .1234;
        mms2[i + 1] = mms[i + 1] + len * .3784123;
        mms3[i] = mms[i] - len * .321078;
        mms3[i + 1] = mms[i + 1] + len * .51824172;
      }
      (new ZCurveSpatialSorter()).sort(zs2, 0, zs2.size(), mms2);
      (new PeanoSpatialSorter()).sort(ps2, 0, ps2.size(), mms2);
      (new HilbertSpatialSorter()).sort(hs2, 0, hs2.size(), mms2);
      (new ZCurveSpatialSorter()).sort(zs3, 0, zs3.size(), mms3);
      (new PeanoSpatialSorter()).sort(ps3, 0, ps3.size(), mms3);
      (new HilbertSpatialSorter()).sort(hs3, 0, hs3.size(), mms3);
    }
    // Build position index, DBID -> position in the three curves
    WritableDataStore<int[]> positions = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, int[].class);
    {
      Iterator<SpatialRef> it = zs.iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        positions.put(r.id, new int[] { i, -1, -1, -1, -1, -1, -1, -1, -1  });
      }
    }
    {
      Iterator<SpatialRef> it = ps.iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        int[] data = positions.get(r.id);
        data[1] = i;
        positions.put(r.id, data);
      }
    }
    {
      Iterator<SpatialRef> it = hs.iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        int[] data = positions.get(r.id);
        data[2] = i;
        positions.put(r.id, data);
      }
    }
    {
      Iterator<SpatialRef> it = zs2.iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        int[] data = positions.get(r.id);
        data[3] = i;
        positions.put(r.id, data);
      }
    }
    {
      Iterator<SpatialRef> it = ps2.iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        int[] data = positions.get(r.id);
        data[4] = i;
        positions.put(r.id, data);
      }
    }
    {
      Iterator<SpatialRef> it = hs2.iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        int[] data = positions.get(r.id);
        data[5] = i;
        positions.put(r.id, data);
      }
    }
    {
      Iterator<SpatialRef> it = zs3.iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        int[] data = positions.get(r.id);
        data[6] = i;
        positions.put(r.id, data);
      }
    }
    {
      Iterator<SpatialRef> it = ps3.iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        int[] data = positions.get(r.id);
        data[7] = i;
        positions.put(r.id, data);
      }
    }
    {
      Iterator<SpatialRef> it = hs3.iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        int[] data = positions.get(r.id);
        data[8] = i;
        positions.put(r.id, data);
      }
    }

    // True kNN value
    final int k = 50;
    final int maxoff = 50 * k + 1;
    final int numcurves = 29;
    DistanceQuery<NumberVector<?, ?>, DoubleDistance> distq = database.getDistanceQuery(rel, distanceFunction);
    KNNQuery<NumberVector<?, ?>, DoubleDistance> knnq = database.getKNNQuery(distq, k);

    ArrayList<MeanVariance[]> mrec = new ArrayList<MeanVariance[]>();
    ArrayList<MeanVariance[]> mdic = new ArrayList<MeanVariance[]>();
    ArrayList<MeanVariance[]> merr = new ArrayList<MeanVariance[]>();
    for(int i = 0; i < maxoff; i++) {
      mrec.add(MeanVariance.newArray(numcurves));
      mdic.add(MeanVariance.newArray(numcurves));
      merr.add(MeanVariance.newArray(numcurves));
    }

    ArrayList<Pair<ModifiableDBIDs, KNNHeap<DoubleDistance>>> rec = new ArrayList<Pair<ModifiableDBIDs, KNNHeap<DoubleDistance>>>();
    for(int i = 0; i < numcurves; i++) {
      ModifiableDBIDs cand = DBIDUtil.newHashSet(maxoff * 2);
      KNNHeap<DoubleDistance> heap = new KNNHeap<DoubleDistance>(k);
      rec.add(new Pair<ModifiableDBIDs, KNNHeap<DoubleDistance>>(cand, heap));
    }

    for(DBID id : ids) {
      final NumberVector<?, ?> vec = rel.get(id);
      final KNNResult<DoubleDistance> trueNN = knnq.getKNNForObject(vec, k);
      final DBIDs trueIds = DBIDUtil.ensureSet(trueNN.asDBIDs());
      final int[] posi = positions.get(id);

      // Reinit:
      for(int i = 0; i < numcurves; i++) {
        initCandidates(rec.get(i).first, rec.get(i).second, id);
      }

      { // Random sample
        ArrayDBIDs rand = DBIDUtil.ensureArray(DBIDUtil.randomSample(ids, maxoff * 2, rnd.nextLong()));
        for(int i = 0; i < maxoff; i++) {
          if(2 * i + 1 >= rand.size()) {
            break;
          }
          DBID cid = rand.get(i * 2);
          if(rec.get(0).first.add(cid)) {
            final double d = distanceFunction.doubleDistance(vec, rel.get(cid));
            rec.get(0).second.add(new DoubleDistanceResultPair(d, cid));
          }
          cid = rand.get(i * 2 + 1);
          if(rec.get(0).first.add(cid)) {
            final double d = distanceFunction.doubleDistance(vec, rel.get(cid));
            rec.get(0).second.add(new DoubleDistanceResultPair(d, cid));
          }
          // Candidate set size: distance computations
          mdic.get(i)[0].put(rec.get(0).first.size());
          // Intersection size = recall
          final int isize = DBIDUtil.intersection(trueIds, rec.get(0).first).size();
          mrec.get(i)[0].put(isize);
          // Error = quotient of distances
          double err = rec.get(0).second.getMaximumDistance().doubleValue() / trueNN.getKNNDistance().doubleValue();
          merr.get(i)[0].put(err);
        }
      }
      // Spatial curves
      for(int off = 1; off < maxoff; off++) {
        // Candidates from Z curve
        addCandidates(zs, posi[0], off, vec, rel, rec, 1, 10, 13, 19, 22, 25, 26, 28);
        // Candidates from Peano curve
        addCandidates(ps, posi[1], off, vec, rel, rec, 2, 11, 14, 19, 23, 25, 26, 28);
        // Candidates from Hilbert curve
        addCandidates(hs, posi[2], off, vec, rel, rec, 3, 12, 15, 19, 24, 25, 26, 28);
        // Candidates from second Z curve
        addCandidates(zs2, posi[3], off, vec, rel, rec, 4, 10, 16, 20, 22, 25, 27, 28);
        // Candidates from second Peanocurve
        addCandidates(ps2, posi[4], off, vec, rel, rec, 5, 11, 17, 20, 23, 25, 27, 28);
        // Candidates from second Hilbert curve
        addCandidates(hs2, posi[5], off, vec, rel, rec, 6, 12, 18, 20, 24, 25, 27, 28);
        // Candidates from third Z curve
        addCandidates(zs3, posi[6], off, vec, rel, rec, 7, 13, 16, 21, 22, 26, 27, 28);
        // Candidates from third Peanocurve
        addCandidates(ps3, posi[7], off, vec, rel, rec, 8, 14, 17, 21, 23, 26, 27, 28);
        // Candidates from third Hilbert curve
        addCandidates(hs3, posi[8], off, vec, rel, rec, 9, 15, 18, 21, 24, 26, 27, 28);
        // Evaluate curve performances
        for(int i = 1; i < numcurves; i++) {
          // Candidate set size: distance computations
          mdic.get(off)[i].put(rec.get(i).first.size());
          // Intersection size = recall
          final int isize = DBIDUtil.intersection(trueIds, rec.get(i).first).size();
          mrec.get(off)[i].put(isize);
          // Error = quotient of distances
          double err = rec.get(i).second.getMaximumDistance().doubleValue() / trueNN.getKNNDistance().doubleValue();
          merr.get(off)[i].put(err);
        }
      }
    }

    String[] labels = new String[] { "R", //
    "Z1", "P1", "H1", //
    "Z2", "P2", "H2", //
    "Z3", "P3", "H3", //
    "Z1Z2", "P1P2", "H1H2", //
    "Z1Z3", "P1P3", "H1H3", //
    "Z2Z3", "P2P3", "H2H3", //
    "ZPH", "Z2P2H2", "Z3P3H3", //
    "Z123", "P123", "H123", //
    "Z12P12H12", "Z13P13H13", "Z23P23H23", //
    "Z123P123H123"};
    assert(labels.length == numcurves);
    System.out.print("# i");
    // Recall of exact NN:
    for(String s : labels) {
      System.out.print(" " + s + "-recall");
    }
    // Distance computations:
    for(String s : labels) {
      System.out.print(" " + s + "-distc");
    }
    // Distance error:
    for(String s : labels) {
      System.out.print(" " + s + "-distance-err");
    }
    System.out.println();
    for(int i = 1; i < maxoff; i++) {
      System.out.print(i);
      MeanVariance[] mr = mrec.get(i);
      for(int j = 0; j < mr.length; j++) {
        System.out.print(" " + (mr[j].getMean() / k));
        // + " " + mv[j].getNaiveStddev());
      }
      MeanVariance[] md = mdic.get(i);
      for(int j = 0; j < mr.length; j++) {
        System.out.print(" " + (md[j].getMean() / ids.size()));
        // + " " + md[j].getNaiveStddev());
      }
      MeanVariance[] me = merr.get(i);
      for(int j = 0; j < mr.length; j++) {
        System.out.print(" " + me[j].getMean());
        // + " " + me[j].getNaiveStddev());
      }
      System.out.println();
    }
  }

  protected void initCandidates(ModifiableDBIDs candz, KNNHeap<DoubleDistance> heapz, DBID id) {
    candz.clear();
    candz.add(id);
    heapz.clear();
    heapz.add(new DoubleDistanceResultPair(0, id));
  }

  protected void addCandidates(List<SpatialRef> zs, final int pos, int off, NumberVector<?, ?> vec, Relation<NumberVector<?, ?>> rel, List<Pair<ModifiableDBIDs, KNNHeap<DoubleDistance>>> pairs, int... runs) {
    if(pos - off >= 0) {
      final DBID cid = zs.get(pos - off).id;
      double d = Double.NaN;
      for(int i = 0; i < runs.length; i++) {
        if(pairs.get(runs[i]).first.add(cid)) {
          if(Double.isNaN(d)) {
            d = distanceFunction.doubleDistance(vec, rel.get(cid));
          }
          pairs.get(runs[i]).second.add(new DoubleDistanceResultPair(d, cid));
        }
      }
    }
    if(pos + off < zs.size()) {
      final DBID cid = zs.get(pos + off).id;
      double d = Double.NaN;
      for(int i = 0; i < runs.length; i++) {
        if(pairs.get(runs[i]).first.add(cid)) {
          if(Double.isNaN(d)) {
            d = distanceFunction.doubleDistance(vec, rel.get(cid));
          }
          pairs.get(runs[i]).second.add(new DoubleDistanceResultPair(d, cid));
        }
      }
    }
  }

  private Database loadDatabase() {
    try {
      ListParameterization dbpar = new ListParameterization();
      // Input file
      dbpar.addParameter(FileBasedDatabaseConnection.INPUT_ID, "/nfs/multimedia/images/ALOI/ColorHistograms/aloi-hsb-7x2x2.csv.gz");
      // Index
      dbpar.addParameter(StaticArrayDatabase.INDEX_ID, RStarTreeFactory.class);
      dbpar.addParameter(TreeIndexFactory.PAGE_SIZE_ID, "10000");
      // Instantiate
      Database db = ClassGenericsUtil.tryInstantiate(Database.class, StaticArrayDatabase.class, dbpar);
      db.initialize();
      return db;
    }
    catch(Exception e) {
      throw new RuntimeException("Cannot load database." + e, e);
    }
  }

  /**
   * Object used in spatial sorting, combining the spatial object and the object
   * ID.
   * 
   * @author Erich Schubert
   */
  static class SpatialRef implements SpatialComparable {
    protected DBID id;

    protected NumberVector<?, ?> vec;

    /**
     * Constructor.
     * 
     * @param id
     * @param vec
     */
    protected SpatialRef(DBID id, NumberVector<?, ?> vec) {
      super();
      this.id = id;
      this.vec = vec;
    }

    @Override
    public int getDimensionality() {
      return vec.getDimensionality();
    }

    @Override
    public double getMin(int dimension) {
      return vec.getMin(dimension);
    }

    @Override
    public double getMax(int dimension) {
      return vec.getMax(dimension);
    }
  }

  public static void main(String[] args) {
    // LoggingConfiguration.setDefaultLevel(Level.INFO);
    // logger.getWrappedLogger().setLevel(Level.INFO);
    try {
      new SpacefillingKNNExperiment2().run();
    }
    catch(Exception e) {
      logger.exception(e);
    }
  }
}