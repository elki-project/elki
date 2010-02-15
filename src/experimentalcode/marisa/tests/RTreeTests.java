package experimentalcode.marisa.tests;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Date;

import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.index.tree.spatial.rstarvariants.rstar.RStarTree;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.SerializedParameterization;
import experimentalcode.marisa.utils.Zeit;

public class RTreeTests {

  public static RStarTree<DoubleVector> buildRStarTree() throws ParameterException, NumberFormatException, IOException {
    String csvInputFile = "C:/WORK/Theseus/data/synthetic/15DUniform.csv";
    String outputFile = "15DUniformRSTree_default";
    
    // parameter
//    String[] splitted = ("-treeindex.pagesize 4096 " + "-treeindex.file C:/WORK/Theseus/Experimente/xtrees/"+outputFile+" " + "-treeindex.cachesize 409600000").split("\\s");
    String[] splitted = ("-treeindex.pagesize 4096 " + "-treeindex.file C:/WORK/Theseus/Experimente/rstartrees/"+outputFile+" " + "-treeindex.cachesize 409600000").split("\\s");

    //init RTrees
    SerializedParameterization config = new SerializedParameterization(splitted);
    RStarTree<DoubleVector> rTree = new RStarTree<DoubleVector>(config);
    config.failOnErrors();
    
    FileInputStream fis = new FileInputStream(csvInputFile);
    DataInputStream in = new DataInputStream(fis);

    Date jetzt = new Date();
    long t1 = System.currentTimeMillis();
    int stop = 10000; 
    for(int i = 0;(in.available()!=0);i++){
      if (i == stop)
        break;
      rTree.insert(readNext(in, i));
      if(i%1000 == 0)System.out.println("Inserted "+i+" elements: "+(((double)i)/1000000)+"% in "+((double)(System.currentTimeMillis()-t1))/60000+" minutes");
    }
    System.out.println("took " + Zeit.wieLange(jetzt));
    return rTree;
  }
  
  public static DoubleVector readNext(DataInputStream in, int id) throws NumberFormatException, IOException{
    String[] d = null;
    double[] coords = null;
    int dimension;
    d = in.readLine().split("\\s");
    if(d.length ==1)
      d = in.readLine().split(";");
    dimension = d.length;
    coords = new double[dimension];
    for (int i = 0; i < dimension; i++) {
      coords[i] = Double.valueOf(d[i]);
    }
    DoubleVector dv = new  DoubleVector(coords);
    dv.setID(id);
    return dv;
  }
  
  public static RStarTree<DoubleVector> loadRStarTree() throws ParameterException {
    String outputFile = "15DUniformRSTree_default";
    String[] split = ("-treeindex.pagesize 4096 " + "-treeindex.file C:/WORK/Theseus/Experimente/rstartrees/"+outputFile+" " + "-treeindex.cachesize 409600000").split("\\s");
    SerializedParameterization config = new SerializedParameterization(split);
    RStarTree<DoubleVector> rt = new RStarTree<DoubleVector>(config);
    config.failOnErrors();
    rt.insert(new ArrayList<DoubleVector>());
    return rt;
  }
  
  public static void main(String[] args) throws NumberFormatException, ParameterException, IOException {
    RStarTree<DoubleVector> rt;
    rt = buildRStarTree();
    System.out.println("RT: "+rt.toString());
    rt.close();
    rt = loadRStarTree();
    System.out.println("RT: "+rt.toString());
    rt.close();
  }
  
}
