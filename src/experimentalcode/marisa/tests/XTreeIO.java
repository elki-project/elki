package experimentalcode.marisa.tests;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;
import java.util.Locale;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.ClassGenericsUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import experimentalcode.marisa.index.xtree.common.XTree;
import experimentalcode.marisa.utils.Zeit;

public class XTreeIO {

  public static long CACHE_SIZE = 409600000;

  public static double REINSERT_FRACTION = -1;

  public static boolean DATA_OVERLAP = false;

  public static boolean VERBOSE = true;

  public static int VERBOSE_STEP = 1000;

  public static XTree<DoubleVector> buildXTree(String csvInputFile, String outputFile, int pageSize, double maxOverlap, double minFanoutFraction, long numInstances, boolean commit) throws ParameterException, NumberFormatException, IOException {

    // parameters
    String paramStr = "-treeindex.pagesize " + pageSize + " " + "-treeindex.cachesize " + CACHE_SIZE + " -xtree.max_overlap_fraction " + maxOverlap;
    if(outputFile != null && outputFile.length() > 0)
      paramStr += " -treeindex.file " + outputFile;
    if(REINSERT_FRACTION != -1)
      paramStr += " -xtree.reinsert_fraction " + REINSERT_FRACTION;
    if(DATA_OVERLAP)
      paramStr += " -xtree.overlap_type DataOverlap";
    if(!Double.isNaN(minFanoutFraction))
      paramStr += " -xtree.min_fanout_fraction " + minFanoutFraction;
    String[] split = paramStr.split("\\s");

    // init xTree
    SerializedParameterization config = new SerializedParameterization(split);
    XTree<DoubleVector> xTree = ClassGenericsUtil.parameterizeOrAbort(XTree.class, config);
    config.failOnErrors();

//    xTree.initializeTree(new DoubleVector(new double[15]));
//    System.out.println(xTree.toString());

    FileInputStream fis = new FileInputStream(csvInputFile);
    DataInputStream in = new DataInputStream(fis);

    Date jetzt = new Date();
    long t1 = System.currentTimeMillis();
    for(long i = 0; (in.available() != 0); i++) {
      if(i < 0)
        throw new RuntimeException("numInstances = " + numInstances + " is too large for this framework! Can only deal with at most " + Integer.MAX_VALUE + " entries");
      if(i == numInstances)
        break;
      xTree.insert(DBIDUtil.importInteger((int)i), readNext(in));
      if(VERBOSE && i % VERBOSE_STEP == 0)
        System.out.println("Inserted " + i + " elements: " + (((double) i) / 1000000) + "% in " + ((double) (System.currentTimeMillis() - t1)) / 60000 + " minutes");
    }
    in.close();
    fis.close();
    if(commit)
      xTree.commit();
    if(VERBOSE)
      System.out.println("took " + Zeit.wieLange(jetzt));
    return xTree;
  }

  public static XTree<DoubleVector> loadXTree(String xtFilename) throws ParameterException {
    String[] split = ("-treeindex.file " + xtFilename + " " + "-treeindex.cachesize " + CACHE_SIZE).split("\\s");
    SerializedParameterization config = new SerializedParameterization(Arrays.asList(split));
    XTree<DoubleVector> xt = ClassGenericsUtil.parameterizeOrAbort(XTree.class, config);
    config.failOnErrors();
    xt.initializeFromFile();
    return xt;
  }

  public static DoubleVector readNext(DataInputStream in) throws NumberFormatException, IOException {
    String[] d = null;
    double[] coords = null;
    int dimension;
    d = in.readLine().split("\\s");
    if(d.length == 1)
      d = in.readLine().split(";");
    dimension = d.length;
    coords = new double[dimension];
    for(int i = 0; i < dimension; i++) {
      coords[i] = Double.valueOf(d[i]);
    }
    return new DoubleVector(coords);
  }

  public static void buildTrees(String csvInputFile, String outputPrefix, int[] pageSizes, double[] maxOverlaps, double[] reInsertFractions, double[] minFanoutFractions, long[] numInstances) throws NumberFormatException, ParameterException, IOException {
    String fn;
    for(int i = 0; i < pageSizes.length; i++) {
      for(int j = 0; j < maxOverlaps.length; j++) {
        for(int k = 0; k < reInsertFractions.length; k++) {
          for(int l = 0; l < minFanoutFractions.length; l++) {
            for(int m = 0; m < numInstances.length; m++) {
              if(reInsertFractions[k] != -1) {
                REINSERT_FRACTION = reInsertFractions[k];
                fn = String.format(Locale.ENGLISH, "%sp%dmO%.3frI%.3fmFO%.3fI%d", outputPrefix, pageSizes[i], maxOverlaps[j], reInsertFractions[k], minFanoutFractions[l], numInstances[m]);
              }
              else {
                REINSERT_FRACTION = -1;
                fn = String.format(Locale.ENGLISH, "%sp%dmO%.3fmFO%.3fI%d", outputPrefix, pageSizes[i], maxOverlaps[j], minFanoutFractions[l], numInstances[m]);
              }
              if(VERBOSE)
                System.out.println("Building XTree '" + fn + "'");
              XTree<DoubleVector> xt = buildXTree(csvInputFile, fn, pageSizes[i], maxOverlaps[j], minFanoutFractions[l], numInstances[m], true);
              if(VERBOSE)
                System.out.println(xt.toString());
              xt.commit();
              xt.close();
            }
          }
        }
      }
    }
  }

  public static void main(String[] args) throws NumberFormatException, ParameterException, IOException {
    LoggingConfiguration.reconfigureLogging("experimentalcode.marisa.tests", "logging-cli.properties");
    // LoggingConfiguration.setVerbose(true);
    // String inputCSV = "C:/WORK/Theseus/data/synthetic/15DUniform.csv";
    // String outputPrefix = "C:/WORK/Theseus/Experimente/xtrees/15DUniformXT";
    // String inputCSV = "/theseus/data/synthetic/15DUniform.csv";
    String inputCSV = "/tmp/marisa/15DUniform.csv";
    String outputPrefix = "/theseus/data/Experimente/index/xTree/elki/15DUniformXT/";
     buildTrees(inputCSV, outputPrefix, new int[] { 4096, 8192 }, new double[] { /*1, .9, .5,*/ .2, .1 }, new double[] { 0, .15, .3 }, new double[] { .2, .3, .4 }, new long[] { 1000000 });
    outputPrefix += "DO";
    DATA_OVERLAP = true;
    buildTrees(inputCSV, outputPrefix, new int[] { 4096, 8192 }, new double[] { 1, .9, .5, .2, .1 }, new double[] { 0, .15, .3 }, new double[] { .2, .3, .4 }, new long[] { 1000000 });
  }
}
