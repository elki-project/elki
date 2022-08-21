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
package elki.application.experiments;

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Arrays;
import java.util.regex.Pattern;

import elki.Algorithm;
import elki.application.AbstractApplication;
import elki.clustering.ClusteringAlgorithm;
import elki.clustering.kmeans.KMeans;
import elki.clustering.kmedoids.*;
import elki.clustering.kmedoids.initialization.BUILD;
import elki.clustering.kmedoids.initialization.KMedoidsInitialization;
import elki.data.Cluster;
import elki.data.Clustering;
import elki.data.model.MedoidModel;
import elki.data.projection.random.RandomSubsetProjectionFamily;
import elki.database.ids.DBID;
import elki.database.ids.DBIDIter;
import elki.database.ids.DBIDRange;
import elki.database.ids.DBIDUtil;
import elki.database.query.distance.DistanceQuery;
import elki.database.relation.DBIDView;
import elki.distance.AbstractDBIDRangeDistance;
import elki.distance.DBIDDistance;
import elki.distance.Distance;
import elki.index.DistanceIndex;
import elki.logging.Logging;
import elki.logging.LoggingConfiguration;
import elki.logging.statistics.DoubleStatistic;
import elki.logging.statistics.Duration;
import elki.math.MathUtil;
import elki.result.Metadata;
import elki.utilities.io.FileUtil;
import elki.utilities.io.TokenizedReader;
import elki.utilities.io.Tokenizer;
import elki.utilities.optionhandling.OptionID;
import elki.utilities.optionhandling.parameterization.Parameterization;
import elki.utilities.optionhandling.parameters.*;
import elki.utilities.random.RandomFactory;

/**
 * Load an ORlib problem to evaluate k-medoids clustering quality.
 *
 * @author Erich Schubert
 * @since 0.8.0
 */
public class ORLibBenchmark extends AbstractApplication {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ORLibBenchmark.class);

  /**
   * Input file.
   */
  private URI file = null;

  /**
   * Clustering algorithm
   */
  private Class<? extends ClusteringAlgorithm<?>> alg;

  /**
   * Initialization method.
   */
  private KMedoidsInitialization<DBID> init;

  /**
   * Number of clusters override (optional)
   */
  private int k;

  /**
   * Random generator for shuffling.
   */
  private RandomFactory rnd;

  /**
   * Constructor.
   *
   * @param file Input file name
   * @param alg Algorithm class
   * @param init Random generator
   * @param k Override for the number of clusters
   * @param rnd Random generator for shuffling
   */
  public ORLibBenchmark(URI file, Class<? extends ClusteringAlgorithm<?>> alg, KMedoidsInitialization<DBID> init, int k, RandomFactory rnd) {
    this.file = file;
    this.alg = alg;
    this.init = init;
    this.k = k;
    this.rnd = rnd;
  }

  @Override
  public void run() {
    try (InputStream is = FileUtil.open(file);
        TokenizedReader reader = new TokenizedReader(Pattern.compile(" "), null, Pattern.compile("^c "))) {
      reader.reset(is);
      Tokenizer tok = reader.getTokenizer();
      if(!reader.nextLine() || !tok.valid()) {
        throw new IOException("Empty file: " + file);
      }
      // ORLib often has a leading space; dimacs uses p for indicating the
      // problem format; the remainder is unchanged in Werneck's test files
      if(tok.isEmpty() || tok.getLength() == 1 && tok.getChar(0) == 'p') {
        tok.advance();
      }
      int n = tok.getIntBase10();
      if(n > 0xFFFF) {
        throw new ArrayIndexOutOfBoundsException("Distance matrix size overflow.");
      }
      if(!tok.advance().valid()) {
        throw new IOException("Invalid file format.");
      }
      // We ignore this value. It could be either the number of edges to follow
      // (but there may be duplicate edges), or sometimes a n*p format.
      @SuppressWarnings("unused")
      int e = tok.getIntBase10();
      tok.advance();
      if(tok.valid()) { // Optional, only for some variants.
        int p = tok.getIntBase10();
        k = k > 0 ? k : p; // Use given override value, else read value.
        if(tok.advance().valid()) {
          throw new IOException("Invalid file format - extra entries.");
        }
      }
      else if(k <= 0) {
        throw new IllegalStateException("If the data set does not specify k, it must be given as parameter.");
      }
      // Load the edge list
      double[] imat = readEdges(n, reader, tok);
      allShortestPaths(n, imat);
      double[] mat = randomShuffle(n, imat);
      DBIDRange ids = DBIDUtil.generateStaticDBIDRange(n);
      DBIDDistance dist = new AbstractDBIDRangeDistance() {
        @Override
        public void checkRange(DBIDRange range) {
          if(range != ids) {
            throw new IllegalArgumentException("Invalid DBID range");
          }
        }

        @Override
        public double distance(int i1, int i2) {
          return i1 != i2 ? mat[computeOffset(i1, i2)] : 0.;
        }
      };
      DBIDView view = new DBIDView(ids);
      // Hack: add a fake index for this, to avoid making another distance
      // matrix.
      Metadata.hierarchyOf(view).addChild(new DistanceIndex<DBID>() {
        @Override
        public void initialize() {
        }

        @Override
        public DistanceQuery<DBID> getDistanceQuery(Distance<? super DBID> distanceFunction) {
          return distanceFunction == dist ? dist.instantiate(view) : null;
        }
      });
      Duration time = LOG.newDuration(ORLibBenchmark.class.getName() + ".time").begin();
      Clustering<MedoidModel> clustering;
      if(alg == null || SingleAssignmentKMedoids.class.equals(alg)) {
        clustering = new SingleAssignmentKMedoids<>(dist, k, init).run(view);
      }
      else if(PAM.class.equals(alg)) {
        clustering = new PAM<>(dist, k, -1, init).run(view);
      }
      else if(ReynoldsPAM.class.equals(alg)) {
        clustering = new ReynoldsPAM<>(dist, k, -1, init).run(view);
      }
      else if(AlternatingKMedoids.class.equals(alg)) {
        clustering = new AlternatingKMedoids<>(dist, k, -1, init).run(view);
      }
      else if(EagerPAM.class.equals(alg)) {
        clustering = new EagerPAM<>(dist, k, -1, init).run(view);
      }
      else if(FastPAM.class.equals(alg)) {
        clustering = new FastPAM<>(dist, k, -1, init).run(view);
      }
      else if(FastPAM1.class.equals(alg)) {
        clustering = new FastPAM1<>(dist, k, -1, init).run(view);
      }
      else if(FasterPAM.class.equals(alg)) {
        clustering = new FasterPAM<>(dist, k, -1, init).run(view);
      }
      else {
        throw new IllegalArgumentException("Unsupported algorithm: " + alg.toString());
      }
      LOG.statistics(time.end());
      // Verify cluster cost.
      double cost = 0.;
      for(Cluster<MedoidModel> cl : clustering.getAllClusters()) {
        int medoid = ids.getOffset(cl.getModel().getMedoid());
        for(DBIDIter iter = cl.getIDs().iter(); iter.valid(); iter.advance()) {
          int ob = ids.getOffset(iter);
          cost += medoid != ob ? mat[computeOffset(medoid, ob)] : 0.;
        }
      }
      LOG.statistics(new DoubleStatistic(getClass().getName() + ".cost", cost));
    }
    catch(IOException e) {
      LOG.exception(e);
    }
  }

  /**
   * Load the edge list.
   *
   * @param n Data set size
   * @param reader Reader
   * @param tok Tokenizer
   * @return Matrix (missing values are infinity)
   * @throws IOException
   */
  private static double[] readEdges(int n, TokenizedReader reader, Tokenizer tok) throws IOException {
    double[] mat = new double[(n * (n - 1)) >>> 1];
    Arrays.fill(mat, Double.POSITIVE_INFINITY);
    while(reader.nextLine() && tok.valid()) {
      try {
        // ORLib: space, DIMACS: edge, pmm: ?
        if(tok.isEmpty() || tok.getLength() == 1 && //
            (tok.getChar(0) == 'e' || tok.getChar(0) == 'a')) {
          tok.advance();
        }
        int n1 = tok.getIntBase10();
        if(!tok.advance().valid()) {
          throw new IOException("Invalid file format.");
        }
        int n2 = tok.getIntBase10();
        if(!tok.advance().valid()) {
          throw new IOException("Invalid file format.");
        }
        double d = tok.getDouble(); // Actually an int.
        if(tok.advance().valid()) {
          throw new IOException("Invalid file format.");
        }
        if(n1 == n2) { // redundant self-edge. Should have d=0 though.
          if(d != 0) {
            LOG.warning("Non-zero self-edge in line #" + reader.getLineNumber() + ": " + reader.getBuffer());
          }
          continue;
        }
        mat[computeOffset(n1 - 1, n2 - 1)] = d;
      }
      catch(NumberFormatException ex) {
        throw new IllegalArgumentException("Failed to parse line #" + reader.getLineNumber() + ": " + reader.getBuffer(), ex);
      }
    }
    return mat;
  }

  /**
   * Floyd's all-pairs shortest paths
   *
   * @param n Number of nodes
   * @param mat Matrix to complete
   */
  private static void allShortestPaths(int n, double[] mat) {
    for(int i = 0; i < n; i++) {
      for(int x = 0, y = 1, j = 0; j < mat.length; j++, x++) {
        if(x == y) {
          y++;
          x = 0;
        }
        if(x != i && y != i) {
          mat[j] = Math.min(mat[j], mat[computeOffset(x, i)] + mat[computeOffset(i, y)]);
        }
      }
    }
  }

  /**
   * Random shuffle of the matrix.
   *
   * @param n Data set size
   * @param mat Matrix
   * @return New matrix
   */
  private double[] randomShuffle(int n, double[] mat) {
    if(rnd == null || rnd == RandomFactory.DEFAULT) {
      return mat; // Unmodified
    }
    int[] map = MathUtil.sequence(0, n);
    RandomSubsetProjectionFamily.randomPermutation(map, rnd.getSingleThreadedRandom());
    double[] mat2 = new double[mat.length];
    // Iterate over old matrix:
    for(int x = 0, y = 1, mapy = map[1], j = 0; j < mat.length; j++, x++) {
      if(x == y) {
        mapy = map[++y];
        x = 0;
      }
      mat2[computeOffset(map[x], mapy)] = mat[j];
    }
    return mat2;
  }

  /**
   * Compute the offset within the file.
   *
   * @param x First coordinate
   * @param y Second coordinate
   * @return Linear offset
   */
  private static int computeOffset(int x, int y) {
    return x == y ? -1 : y > x ? ((y * (y - 1)) >>> 1) + x : ((x * (x - 1)) >>> 1) + y;
  }

  /**
   * Parameterization class.
   * 
   * @author Erich Schubert
   */
  public static class Par<O> extends AbstractApplication.Par {
    /**
     * Parameter that specifies the name of the directory to be re-parsed.
     */
    public static final OptionID FILE_ID = new OptionID("orlib.file", "Data file to load.");

    /**
     * Random seed for shuffling.
     */
    public static final OptionID SHUFFLE_ID = new OptionID("orlib.seed", "Random seed for shuffling.");

    /**
     * Input file.
     */
    private URI file = null;

    /**
     * Clustering algorithm
     */
    private Class<? extends ClusteringAlgorithm<?>> alg;

    /**
     * Initialization method.
     */
    private KMedoidsInitialization<DBID> init;

    /**
     * Number of clusters override (optional)
     */
    int k;

    /**
     * Random generator for shuffling.
     */
    RandomFactory rnd;

    @Override
    public void configure(Parameterization config) {
      new FileParameter(FILE_ID, FileParameter.FileType.INPUT_FILE) //
          .grab(config, x -> file = x);
      ClassParameter<ClusteringAlgorithm<?>> algP = new ClassParameter<ClusteringAlgorithm<?>>(Algorithm.Utils.ALGORITHM_ID, ClusteringAlgorithm.class);
      if(config.grab(algP)) {
        alg = algP.getValue();
      }
      new ObjectParameter<KMedoidsInitialization<DBID>>(KMeans.INIT_ID, KMedoidsInitialization.class, BUILD.class) //
          .setOptional(true) //
          .grab(config, x -> init = x);
      new IntParameter(KMeans.K_ID).setOptional(true) //
          .grab(config, x -> k = x);
      new RandomParameter(SHUFFLE_ID).setOptional(true) //
          .grab(config, x -> rnd = x);
    }

    @Override
    public ORLibBenchmark make() {
      return new ORLibBenchmark(file, alg, init, k, rnd);
    }
  }

  /**
   * Main method, delegate to super class.
   * 
   * @param args Command line arguments
   */
  public static void main(String[] args) {
    LoggingConfiguration.setStatistics();
    runCLIApplication(ORLibBenchmark.class, args);
  }
}
