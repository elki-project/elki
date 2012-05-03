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
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.spatial.SpatialComparable;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreFactory;
import de.lmu.ifi.dbs.elki.database.datastore.DataStoreUtil;
import de.lmu.ifi.dbs.elki.database.datastore.WritableDataStore;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.ModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.query.distance.DistanceQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNQuery;
import de.lmu.ifi.dbs.elki.database.query.knn.KNNResult;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancevalue.DoubleDistance;
import de.lmu.ifi.dbs.elki.index.tree.TreeIndexFactory;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTreeFactory;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.DoubleMinMax;
import de.lmu.ifi.dbs.elki.math.MeanVariance;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.PeanoSpatialSorter;
import de.lmu.ifi.dbs.elki.math.spacefillingcurves.ZCurveSpatialSorter;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import experimentalcode.erich.BitsUtil;
import experimentalcode.erich.HilbertSpatialSorter;

/**
 * Simple experiment to estimate the effects of approximating the kNN with space
 * filling curves.
 * 
 * @author Erich Schubert
 */
public class SpacefillingKNNExperiment {
  private static final Logging logger = Logging.getLogger(SpacefillingKNNExperiment.class);

  DistanceFunction<? super NumberVector<?, ?>, DoubleDistance> distanceFunction = ManhattanDistanceFunction.STATIC;

  private void run() {
    Database database = loadDatabase();
    Relation<NumberVector<?, ?>> rel = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    final int dim = DatabaseUtil.dimensionality(rel);
    DBIDs ids = rel.getDBIDs();

    List<SpatialRef> zs = new ArrayList<SpatialRef>(ids.size());
    List<SpatialRef> ps = new ArrayList<SpatialRef>(ids.size());
    List<HilbertRef> hs = new ArrayList<HilbertRef>(ids.size());
    {
      DoubleMinMax[] mms = DoubleMinMax.newArray(dim);
      for(DBID id : ids) {
        final NumberVector<?, ?> v = rel.get(id);
        SpatialRef ref = new SpatialRef(id, v);
        zs.add(ref);
        ps.add(ref);
        for(int d = 0; d < dim; d++) {
          mms[d].put(v.doubleValue(d + 1));
        }
      }
      // Build hilbert curves
      int[] buf = new int[dim];
      for(DBID id : ids) {
        final NumberVector<?, ?> v = rel.get(id);
        // Convert into integers
        for(int d = 0; d < dim; d++) {
          double val = (v.doubleValue(d + 1) - mms[d].getMin()) / mms[d].getDiff() * Integer.MAX_VALUE;
          buf[d] = (int) val;
        }
        HilbertRef ref = new HilbertRef(id, v, HilbertSpatialSorter.coordinatesToHilbert(buf, Integer.SIZE - 1));
        hs.add(ref);
      }

      // Sort spatially
      (new ZCurveSpatialSorter()).sort(zs);
      (new PeanoSpatialSorter()).sort(ps);
      Collections.sort(hs);
      // Discard bits, we don't need them anymore
      for(HilbertRef ref : hs) {
        ref.bits = null;
      }
    }
    // Build position index, DBID -> position in the three curves
    WritableDataStore<int[]> positions = DataStoreUtil.makeStorage(ids, DataStoreFactory.HINT_TEMP | DataStoreFactory.HINT_HOT, int[].class);
    {
      Iterator<SpatialRef> it = zs.iterator();
      for(int i = 0; it.hasNext(); i++) {
        SpatialRef r = it.next();
        positions.put(r.id, new int[] { i, -1, -1 });
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
      Iterator<HilbertRef> it = hs.iterator();
      for(int i = 0; it.hasNext(); i++) {
        HilbertRef r = it.next();
        int[] data = positions.get(r.id);
        data[2] = i;
        positions.put(r.id, data);
      }
    }

    // True kNN value
    final int k = 50;
    final int maxoff = 101;
    DistanceQuery<NumberVector<?, ?>, DoubleDistance> distq = database.getDistanceQuery(rel, distanceFunction);
    KNNQuery<NumberVector<?, ?>, DoubleDistance> knnq = database.getKNNQuery(distq, k);

    ArrayList<MeanVariance[]> mvs = new ArrayList<MeanVariance[]>();
    for(int i = 0; i < maxoff; i++) {
      mvs.add(new MeanVariance[] { new MeanVariance(), new MeanVariance(), new MeanVariance(), new MeanVariance() });
    }
    for(DBID id : ids) {
      final KNNResult<DoubleDistance> trueNN = knnq.getKNNForDBID(id, k);
      DBIDs trueIds = DBIDUtil.ensureSet(trueNN.asDBIDs());
      int[] posi = positions.get(id);
      // Approximate NN in Z curve only
      {
        ModifiableDBIDs candz = DBIDUtil.newHashSet();
        candz.add(zs.get(posi[0]).id);
        assert (candz.size() == 1);
        for(int off = 1; off < maxoff; off++) {
          if(posi[0] - off >= 0) {
            candz.add(zs.get(posi[0] - off).id);
          }
          if(posi[0] + off < ids.size()) {
            candz.add(zs.get(posi[0] + off).id);
          }

          final int isize = DBIDUtil.intersection(trueIds, candz).size();
          mvs.get(off)[0].put(isize);
        }
      }
      // Approximate NN in Peano curve only
      {
        ModifiableDBIDs candp = DBIDUtil.newHashSet();
        candp.add(ps.get(posi[1]).id);
        assert (candp.size() == 1);
        for(int off = 1; off < maxoff; off++) {
          if(posi[1] - off >= 0) {
            candp.add(ps.get(posi[1] - off).id);
          }
          if(posi[1] + off < ids.size()) {
            candp.add(ps.get(posi[1] + off).id);
          }

          final int isize = DBIDUtil.intersection(trueIds, candp).size();
          mvs.get(off)[1].put(isize);
        }
      }
      // Approximate NN in Hilbert curve only
      {
        ModifiableDBIDs candh = DBIDUtil.newHashSet();
        candh.add(ps.get(posi[2]).id);
        assert (candh.size() == 1);
        for(int off = 1; off < maxoff; off++) {
          if(posi[2] - off >= 0) {
            candh.add(ps.get(posi[2] - off).id);
          }
          if(posi[2] + off < ids.size()) {
            candh.add(ps.get(posi[2] + off).id);
          }

          final int isize = DBIDUtil.intersection(trueIds, candh).size();
          mvs.get(off)[2].put(isize);
        }
      }
      // Approximate NN in Z + Peano + Hilbert curves
      {
        ModifiableDBIDs cands = DBIDUtil.newHashSet();
        cands.add(zs.get(posi[0]).id);
        cands.add(ps.get(posi[1]).id);
        assert (cands.size() == 1);
        for(int off = 1; off < maxoff; off++) {
          if(posi[0] - off >= 0) {
            cands.add(zs.get(posi[0] - off).id);
          }
          if(posi[0] + off < ids.size()) {
            cands.add(zs.get(posi[0] + off).id);
          }
          if(posi[1] - off >= 0) {
            cands.add(ps.get(posi[1] - off).id);
          }
          if(posi[1] + off < ids.size()) {
            cands.add(ps.get(posi[1] + off).id);
          }
          if(posi[2] - off >= 0) {
            cands.add(hs.get(posi[2] - off).id);
          }
          if(posi[2] + off < ids.size()) {
            cands.add(hs.get(posi[2] + off).id);
          }

          final int isize = DBIDUtil.intersection(trueIds, cands).size();
          mvs.get(off)[3].put(isize);
        }
      }
    }

    for(int i = 1; i < maxoff; i++) {
      MeanVariance[] mv = mvs.get(i);
      System.out.print(i);
      System.out.print(" " + mv[0].getMean() + " " + mv[0].getNaiveStddev());
      System.out.print(" " + mv[1].getMean() + " " + mv[1].getNaiveStddev());
      System.out.print(" " + mv[2].getMean() + " " + mv[2].getNaiveStddev());
      System.out.print(" " + mv[3].getMean() + " " + mv[3].getNaiveStddev());
      System.out.println();
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

  /**
   * Object used in spatial sorting, combining the spatial object and the object
   * ID.
   * 
   * @author Erich Schubert
   */
  static class HilbertRef implements Comparable<HilbertRef> {
    protected DBID id;

    protected NumberVector<?, ?> vec;

    protected long[] bits;

    /**
     * Constructor.
     * 
     * @param id
     * @param vec
     */
    protected HilbertRef(DBID id, NumberVector<?, ?> vec, long[] bits) {
      super();
      this.id = id;
      this.vec = vec;
      this.bits = bits;
    }

    @Override
    public int compareTo(HilbertRef o) {
      return BitsUtil.compare(this.bits, o.bits);
    }
  }

  public static void main(String[] args) {
    // LoggingConfiguration.setDefaultLevel(Level.INFO);
    // logger.getWrappedLogger().setLevel(Level.INFO);
    try {
      new SpacefillingKNNExperiment().run();
    }
    catch(Exception e) {
      logger.exception(e);
    }
  }
}