package experimentalcode.erich.approxknn;

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
import java.util.List;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.algorithm.outlier.KNNOutlier;
import de.lmu.ifi.dbs.elki.algorithm.outlier.lof.LOF;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.data.type.TypeUtil;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.StaticArrayDatabase;
import de.lmu.ifi.dbs.elki.database.ids.DBIDIter;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.database.ids.DBIDs;
import de.lmu.ifi.dbs.elki.database.ids.HashSetModifiableDBIDs;
import de.lmu.ifi.dbs.elki.database.relation.Relation;
import de.lmu.ifi.dbs.elki.datasource.AbstractDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.datasource.filter.ClassLabelFilter;
import de.lmu.ifi.dbs.elki.distance.distancefunction.DistanceFunction;
import de.lmu.ifi.dbs.elki.distance.distancefunction.minkowski.ManhattanDistanceFunction;
import de.lmu.ifi.dbs.elki.evaluation.roc.ROC;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.MaterializeKNNPreprocessor;
import de.lmu.ifi.dbs.elki.index.preprocessed.knn.RandomSampleKNNPreprocessor;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.progress.FiniteProgress;
import de.lmu.ifi.dbs.elki.math.geometry.XYCurve;
import de.lmu.ifi.dbs.elki.persistent.AbstractPageFileFactory;
import de.lmu.ifi.dbs.elki.result.ResultUtil;
import de.lmu.ifi.dbs.elki.result.outlier.OutlierResult;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.DatabaseUtil;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;

/**
 * Simple experiment to estimate the effects of approximating the kNN distances.
 * 
 * @author Erich Schubert
 */
public class RandomSampleKNNExperiment {
  private static final Logging LOG = Logging.getLogger(RandomSampleKNNExperiment.class);

  DistanceFunction<? super NumberVector> distanceFunction = ManhattanDistanceFunction.STATIC;

  private void run() {
    Database database = loadDatabase();
    Relation<NumberVector> rel = database.getRelation(TypeUtil.NUMBER_VECTOR_FIELD);
    DBIDs ids = rel.getDBIDs();
    HashSetModifiableDBIDs pos = DBIDUtil.newHashSet();

    // Number of iterations and step size
    final int iters = 10;
    final int step = 1;
    final int maxk = iters * step;

    // Build positive ids (outliers) once.
    {
      Pattern p = Pattern.compile("Outlier", Pattern.CASE_INSENSITIVE);
      Relation<String> srel = DatabaseUtil.guessLabelRepresentation(database);
      for(DBIDIter id = ids.iter(); id.valid(); id.advance()) {
        String s = srel.get(id);
        if(s == null) {
          LOG.warning("Object without label: " + id);
        }
        else if(p.matcher(s).matches()) {
          pos.add(id);
        }
      }
    }
    ROC.DBIDsTest positive = new ROC.DBIDsTest(pos);
    
    // Collect the data for output
    double[][] data = new double[iters][6];
    // Results for full kNN:
    {
      // Setup preprocessor
      MaterializeKNNPreprocessor.Factory<NumberVector> ppf = new MaterializeKNNPreprocessor.Factory<>(maxk + 1, distanceFunction);
      MaterializeKNNPreprocessor<NumberVector> pp = ppf.instantiate(rel);
      database.addIndex(pp);

      {
        FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("kNN iterations", iters, LOG) : null;
        for(int i = 1; i <= iters; i++) {
          final int k = i * step;
          KNNOutlier<NumberVector> knn = new KNNOutlier<>(distanceFunction, k);
          OutlierResult res = knn.run(database, rel);
          XYCurve roccurve = ROC.materializeROC(positive, new ROC.OutlierScoreAdapter(res));
          double auc = XYCurve.areaUnderCurve(roccurve);
          data[i - 1][0] = auc;
          LOG.incrementProcessed(prog);
        }

        LOG.ensureCompleted(prog);
      }
      {
        FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("LOF iterations", iters, LOG) : null;
        for(int i = 1; i <= iters; i++) {
          final int k = i * step;
          LOF<NumberVector> lof = new LOF<>(k, distanceFunction);
          OutlierResult res = lof.run(database, rel);
          XYCurve roccurve = ROC.materializeROC(positive, new ROC.OutlierScoreAdapter(res));
          double auc = XYCurve.areaUnderCurve(roccurve);
          data[i - 1][3] = auc;
          LOG.incrementProcessed(prog);
        }

        LOG.ensureCompleted(prog);
      }

      // Remove the preprocessor again.
      database.removeIndex(pp);
      ResultUtil.removeRecursive(database.getHierarchy(), pp);

      // Trigger GC cleanup
      pp = null;
      ppf = null;
      System.gc();
    }

    // Partial kNN outlier
    {
      FiniteProgress prog = LOG.isVerbose() ? new FiniteProgress("Approximations.", iters - 1, LOG) : null;
      for(int i = 1; i < iters; i++) {
        final int k = i * step;
        double share = i / (double) iters;
        // Setup preprocessor
        RandomSampleKNNPreprocessor.Factory<NumberVector> ppf = new RandomSampleKNNPreprocessor.Factory<>(maxk + 1, distanceFunction, share, new RandomFactory(1L));
        RandomSampleKNNPreprocessor<NumberVector> pp = ppf.instantiate(rel);
        database.addIndex(pp);

        // Max k kNNOutlier run
        {
          KNNOutlier<NumberVector> knn = new KNNOutlier<>(distanceFunction, maxk);
          OutlierResult res = knn.run(database, rel);
          XYCurve roccurve = ROC.materializeROC(positive, new ROC.OutlierScoreAdapter(res));
          double auc = XYCurve.areaUnderCurve(roccurve);
          data[i - 1][1] = auc;
        }
        // Scaled k kNNOutlier run
        {
          KNNOutlier<NumberVector> knn = new KNNOutlier<>(distanceFunction, k);
          OutlierResult res = knn.run(database, rel);
          XYCurve roccurve = ROC.materializeROC(positive, new ROC.OutlierScoreAdapter(res));
          double auc = XYCurve.areaUnderCurve(roccurve);
          data[i - 1][2] = auc;
        }
        // Max k LOF run
        {
          LOF<NumberVector> lof = new LOF<>(maxk, distanceFunction);
          OutlierResult res = lof.run(database, rel);
          XYCurve roccurve = ROC.materializeROC(positive, new ROC.OutlierScoreAdapter(res));
          double auc = XYCurve.areaUnderCurve(roccurve);
          data[i - 1][4] = auc;
        }
        // Scaled k LOF run
        {
          LOF<NumberVector> lof = new LOF<>(k, distanceFunction);
          OutlierResult res = lof.run(database, rel);
          XYCurve roccurve = ROC.materializeROC(positive, new ROC.OutlierScoreAdapter(res));
          double auc = XYCurve.areaUnderCurve(roccurve);
          data[i - 1][5] = auc;
        }
        // Remove preprocessor
        database.removeIndex(pp);
        ResultUtil.removeRecursive(database.getHierarchy(), pp);

        // Trigger GC cleanup
        pp = null;
        ppf = null;
        System.gc();

        LOG.incrementProcessed(prog);
        System.out.println(k + " " + FormatUtil.format(data[i - 1], " "));
      }
      LOG.ensureCompleted(prog);
    }
    for(int i = 1; i < iters; i++) {
      final int k = i * step;
      System.out.println(k + " " + FormatUtil.format(data[i - 1], " "));
    }
  }

  private Database loadDatabase() {
    try {
      ListParameterization dbpar = new ListParameterization();
      // Input file
      dbpar.addParameter(FileBasedDatabaseConnection.Parameterizer.INPUT_ID, "/nfs/multimedia/images/ALOI/ColorHistograms/outlier/aloi-27d-75000-max4-tot717.csv.gz");
      // Index
      dbpar.addParameter(StaticArrayDatabase.Parameterizer.INDEX_ID, "tree.spatial.rstarvariants.rstar.RStarTreeFactory");
      dbpar.addParameter(AbstractPageFileFactory.Parameterizer.PAGE_SIZE_ID, "10000");
      // Class label filter
      List<Object> list = new ArrayList<>(1);
      list.add(ClassLabelFilter.class);
      dbpar.addParameter(AbstractDatabaseConnection.Parameterizer.FILTERS_ID, list);
      dbpar.addParameter(ClassLabelFilter.Parameterizer.CLASS_LABEL_INDEX_ID, 2);
      // Instantiate
      Database db = ClassGenericsUtil.tryInstantiate(Database.class, StaticArrayDatabase.class, dbpar);
      db.initialize();
      return db;
    }
    catch(Exception e) {
      throw new RuntimeException("Cannot load database.", e);
    }
  }

  public static void main(String[] args) {
    // LoggingConfiguration.setDefaultLevel(Level.INFO);
    // logger.getWrappedLogger().setLevel(Level.INFO);
    try {
      new RandomSampleKNNExperiment().run();
    }
    catch(Exception e) {
      LOG.exception(e);
    }
  }
}