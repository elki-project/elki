/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.io.FilenameFilter;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
import de.lmu.ifi.dbs.elki.application.StandAloneInputApplication;
import de.lmu.ifi.dbs.elki.data.NumberVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.frankenb.model.ConstantSizeIntegerSerializer;
import experimentalcode.frankenb.model.DistanceList;
import experimentalcode.frankenb.model.DistanceListSerializer;
import experimentalcode.frankenb.model.DynamicBPlusTree;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PartitionPairing;

/**
 * This class merges the results precalculated on the cluster network
 * to a single precalculated knn file with a given max k neighbors.
 * <p/>
 * Usage:
 * <br/>
 * <code>-dbc.parser DoubleVectorLabelParser -dbc.in D:\Coding\Projects\ELKI\data\synthetic\outlier-scenarios\3-gaussian-2d.csv -app.out D:/temp/knnparts -app.in D:/temp/knnparts -k 10</code>
 * @author Florian Frankenberger
 */
public class KnnDataMerger extends StandAloneInputApplication {

  private static final Logging LOG = Logging.getLogger(KnnDataMerger.class);
  
  /**
   * OptionID for {@link #K_PARAM}
   */
  public static final OptionID K_ID = OptionID.getOrCreateOptionID("k", "");
  
  /**
   * Parameter that specifies the number of neighbors to keep with respect
   * to the definition of a k-nearest neighbor.
   * <p>
   * Key: {@code -k}
   * </p>
   */
  private final IntParameter K_PARAM = new IntParameter(K_ID, false);
  
  
  private final DatabaseConnection<NumberVector<?, ?>> databaseConnection;
  private final int k;
  
  /**
   * @param config
   */
  public KnnDataMerger(Parameterization config) {
    super(config);
    
    LoggingConfiguration.setLevelFor(this.getClass().getCanonicalName(), Level.ALL.getName());
    
    if (!config.grab(K_PARAM)) {
      throw new RuntimeException("You forgot parameter k");
    } 
    k = K_PARAM.getValue();      
    databaseConnection = new FileBasedDatabaseConnection<NumberVector<?, ?>>(config);
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.StandAloneInputApplication#getInputDescription()
   */
  @Override
  public String getInputDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.StandAloneApplication#getOutputDescription()
   */
  @Override
  public String getOutputDescription() {
    // TODO Auto-generated method stub
    return null;
  }

  /* (non-Javadoc)
   * @see de.lmu.ifi.dbs.elki.application.AbstractApplication#run()
   */
  @Override
  public void run() throws UnableToComplyException {
    try {
      Database<NumberVector<?, ?>> database = databaseConnection.getDatabase(null);
      
      File[] packageDirectories = getInput().listFiles(new FilenameFilter() {
  
        @Override
        public boolean accept(File dir, String name) {
          return name.matches("^package[0-9]{5}$");
        }
        
      });
      
      //open all result files
      List<DynamicBPlusTree<Integer, DistanceList>> resultBPlusTrees = new ArrayList<DynamicBPlusTree<Integer, DistanceList>>();
      for (File packageDirectory : packageDirectories) {
        File[] packageDescriptorCandidates = packageDirectory.listFiles(new FilenameFilter() {

          @Override
          public boolean accept(File dir, String name) {
            return name.matches("^package[0-9]{5}_descriptor.xml$");
          }

        });
        
        if (packageDescriptorCandidates.length > 0) {
          LOG.fine("Opening result of " + packageDirectory.getName() + " ...");
          File packageDescriptorFile = packageDescriptorCandidates[0];
          PackageDescriptor packageDescriptor = PackageDescriptor.loadFromFile(packageDescriptorFile);
          for (PartitionPairing pairing : packageDescriptor.getPartitionPairings()) {
            if (pairing.hasResult()) {
              DynamicBPlusTree<Integer, DistanceList> bPlusTree = pairing.getResult();
              resultBPlusTrees.add(bPlusTree);
            }
          }
        } else {
          LOG.warning("Skipping directory " + packageDirectory + " because the package descriptor could not be found.");
        }
      }
      
      File resultDirectory = new File(this.getOutput(), "result.dir");
      if (resultDirectory.exists())
        resultDirectory.delete();
      File resultData = new File(this.getOutput(), "result.dat");
      if (resultData.exists())
        resultData.delete();
      
      
      DynamicBPlusTree<Integer, DistanceList> resultTree = new DynamicBPlusTree<Integer, DistanceList>(
          resultDirectory,
          resultData,
          new ConstantSizeIntegerSerializer(),
          new DistanceListSerializer(),
          100
      );
      
      LOG.fine("Merging data ...");
      for (DBID dbid : database.getIDs()) {
        DistanceList totalDistanceList = new DistanceList(dbid.getIntegerID());
        for (DynamicBPlusTree<Integer, DistanceList> aResult : resultBPlusTrees) {
          DistanceList aDistanceList = aResult.get(dbid.getIntegerID());
          if (aDistanceList != null) {
            totalDistanceList.addAll(aDistanceList, k);
          }
        }
        if (totalDistanceList.getSize() != k) {
          System.out.println("K is now: " + k);
        }
        resultTree.put(dbid.getIntegerID(), totalDistanceList);
      }
      
      System.out.println("Created result tree with " + resultTree.getSize() + " entries (" + database.size() + ")");
    } catch (Exception e) {
      LOG.log(Level.SEVERE, "Could not merge data", e);
    }
    
  }

  /**
   * @param args
   */
  public static void main(String[] args) {
    StandAloneApplication.runCLIApplication(KnnDataMerger.class, args);
  }

}
