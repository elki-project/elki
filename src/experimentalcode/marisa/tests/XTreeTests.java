package experimentalcode.marisa.tests;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Date;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.ids.DBIDUtil;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import experimentalcode.marisa.index.xtree.common.XTree;
import experimentalcode.marisa.utils.Zeit;

public class XTreeTests {

  public static long CACHE_SIZE = 409600000;

  public static boolean verbose = true;

  public static XTree<DoubleVector> buildXTree() throws ParameterException, NumberFormatException, IOException {
    String csvInputFile = "C:/WORK/Theseus/data/synthetic/15DUniform.csv";
    // String outputFile = "15DUniformXTree_default_mO1_ps4300";
    String outputFile = "15DUniformXTree_default_mO1";

    // parameter
    // String[] split = ("-treeindex.pagesize 4096 " +
    // "-treeindex.file C:/WORK/Theseus/Experimente/xtrees/" + outputFile + " "
    // + "-treeindex.cachesize 409600000").split("\\s");
    String[] split = ("-treeindex.pagesize 4096 " + "-treeindex.file C:/WORK/Theseus/Experimente/xtrees/" + outputFile + " " + "-treeindex.cachesize 409600000 -xtree.max_overlap_fraction 1").split("\\s");
    // String[] split =
    // ("-treeindex.pagesize 4300 -treeindex.file C:/WORK/Theseus/Experimente/xtrees/"+outputFile+" -treeindex.cachesize 409600000 -xtree.max_overlap_fraction 1.0").split("\\s");

    // init xTree
    SerializedParameterization config = new SerializedParameterization(split);
    XTree<DoubleVector> xTree = new XTree<DoubleVector>(config);
    config.failOnErrors();

    FileInputStream fis = new FileInputStream(csvInputFile);
    DataInputStream in = new DataInputStream(fis);

    Date jetzt = new Date();
    long t1 = System.currentTimeMillis();
    int stop = 10000;
    // int stop = 524; // this works fine with the deletion test in main
    for(int i = 0; (in.available() != 0); i++) {
      if(i == stop)
        break;
      xTree.insert(readNext(in, i));
      if(i % 1000 == 0)
        System.out.println("Inserted " + i + " elements: " + (((double) i) / 1000000) + "% in " + ((double) (System.currentTimeMillis() - t1)) / 60000 + " minutes");
    }
    in.close();
    fis.close();
    xTree.commit();
    System.out.println("took " + Zeit.wieLange(jetzt));
    return xTree;
  }

  public static DoubleVector readNext(DataInputStream in, int id) throws NumberFormatException, IOException {
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
    DoubleVector dv = new DoubleVector(coords);
    dv.setID(DBIDUtil.importInteger(id));
    return dv;
  }

  public static DoubleVector readNext(DataInputStream in, int id, int dim) throws NumberFormatException, IOException {
    String[] d = null;
    double[] coords = null;
    int dimension;
    d = in.readLine().split("\\s");
    if(d.length == 1)
      d = in.readLine().split(";");
    if(d.length == 1)
      d = in.readLine().split(",");
    if(d.length < dim)
      throw new IllegalArgumentException("Dimension " + dim + " cannot be read from file (only have " + d.length + ")");
    coords = new double[dim];
    for(int i = 0; i < dim; i++) {
      coords[i] = Double.valueOf(d[i]);
    }
    DoubleVector dv = new DoubleVector(coords);
    dv.setID(DBIDUtil.importInteger(id));
    return dv;
  }

  public static XTree<DoubleVector> loadXTree() throws ParameterException {
    String outputFile = "15DUniformXTree_default_mO1";
    // String outputFile = "15DUniformXTree_default_mO1";
    String[] split = ("-treeindex.file C:/WORK/Theseus/Experimente/xtrees/" + outputFile + " " + "-treeindex.cachesize " + CACHE_SIZE).split("\\s");
    SerializedParameterization config = new SerializedParameterization(Arrays.asList(split));
    XTree<DoubleVector> xt = new XTree<DoubleVector>(config);
    config.failOnErrors();
    xt.initializeFromFile();
    return xt;
  }

  public static XTree<DoubleVector> loadXTree(String xtFilename) throws ParameterException {
    String[] split = ("-treeindex.file " + xtFilename + " " + "-treeindex.cachesize " + CACHE_SIZE).split("\\s");
    SerializedParameterization config = new SerializedParameterization(Arrays.asList(split));
    XTree<DoubleVector> xt = new XTree<DoubleVector>(config);
    config.failOnErrors();
    xt.initializeFromFile();
    return xt;
  }

  public static XTree<DoubleVector> buildXTree(String csvInputFile, String outputFile, int pageSize, double maxOverlap, long numInstances) throws ParameterException, NumberFormatException, IOException {

    // parameter
    String[] split = ("-treeindex.pagesize " + pageSize + " " + "-treeindex.file " + outputFile + " " + "-treeindex.cachesize " + CACHE_SIZE + " -xtree.max_overlap_fraction " + maxOverlap).split("\\s");

    // init xTree
    SerializedParameterization config = new SerializedParameterization(split);
    XTree<DoubleVector> xTree = new XTree<DoubleVector>(config);
    config.failOnErrors();

    FileInputStream fis = new FileInputStream(csvInputFile);
    DataInputStream in = new DataInputStream(fis);

    Date jetzt = new Date();
    long t1 = System.currentTimeMillis();
    for(long i = 0; (in.available() != 0); i++) {
      if(i < 0)
        throw new RuntimeException("numInstances = " + numInstances + " is too large for this framework! Can only deal with at most " + Integer.MAX_VALUE + " entries");
      if(i == numInstances)
        break;
      xTree.insert(readNext(in, (int) i));
      if(verbose && i % 1000 == 0)
        System.out.println("Inserted " + i + " elements: " + (((double) i) / 1000000) + "% in " + ((double) (System.currentTimeMillis() - t1)) / 60000 + " minutes");
    }
    in.close();
    fis.close();
    xTree.commit();
    System.out.println("took " + Zeit.wieLange(jetzt));
    return xTree;
  }

  public static void main(String[] args) throws NumberFormatException, ParameterException, IOException {
    // LoggingConfiguration.reconfigureLogging("experimentalcode.marisa.tests",
    // "logging-cli.properties");
    LoggingConfiguration.DEBUG = true;
    XTree<DoubleVector> xt;
    xt = buildXTree();
    System.out.println("XT: " + xt.toString());
    xt.close();

    // delete a data entry causing both a data node underflow and a directory
    // node underflow
    // DoubleVector v = new DoubleVector(new double[] { 0.09674009112079396,
    // 0.8072466030718443, 0.2530943477333132, 0.4385654105281851,
    // 0.27254527319575905, 0.71617898201738, 0.48354443846788775,
    // 0.23561237168947458, 0.909934154931721, 0.8349833720008567,
    // 0.02824066241589951, 0.48225228889160154, 0.911642336389473,
    // 0.3709312818862476, 0.38712729482936314 });
    // v.setID(502);
    // boolean deleted = xt.delete(v);
    // System.out.println("Deleted: " + deleted);
    // System.out.println("XT after delete: " + xt.toString());
    // xt.close();
    // xt = loadXTree();
    // System.out.println("XT loaded: " + xt.toString());
    // xt.insert(v);
    // System.out.println("XT after insert: " + xt.toString());
    // xt.close();
  }

}
