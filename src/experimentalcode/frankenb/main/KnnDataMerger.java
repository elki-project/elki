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
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;
import experimentalcode.erich.utilities.OnDiskArrayPageStorageManager;
import experimentalcode.erich.utilities.tree.bplus.BPlusFixedTree;
import experimentalcode.frankenb.model.DistanceList;
import experimentalcode.frankenb.model.PackageDescriptor;
import experimentalcode.frankenb.model.PackageDescriptor.Pairing;

/**
 * This class merges the results precalculated on the cluster network
 * to a single precalculated knn file with a given max k neighbors.
 * 
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
  protected KnnDataMerger(Parameterization config) {
    super(config);
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
      
      File[] packageDescriptorFiles = getInput().listFiles(new FilenameFilter() {
  
        @Override
        public boolean accept(File dir, String name) {
          return name.endsWith(".xml");
        }
        
      });
      
      //open all result files
      List<BPlusFixedTree<Integer, DistanceList>> resultBPlusTrees = new ArrayList<BPlusFixedTree<Integer, DistanceList>>();
      for (File packageDescriptorFile : packageDescriptorFiles) {
        PackageDescriptor packageDescriptor = PackageDescriptor.loadFromFile(packageDescriptorFile);
        for (Pairing pairing : packageDescriptor.getPartitionPairings()) {
          if (pairing.hasResult()) {
            BPlusFixedTree<Integer, DistanceList> bPlusTree = new BPlusFixedTree<Integer, DistanceList>(
                new OnDiskArrayPageStorageManager(pairing.getResultFile().getAbsolutePath(), pairing.getResulFilePageSize()),
                5, //directory size 5
                1, 
                Integer.class,
                DistanceList.class
                );
            resultBPlusTrees.add(bPlusTree);
          }
        }
      }
      
      for (DBID dbid : database.getIDs()) {
        DistanceList totalDistanceList = new DistanceList(dbid.getIntegerID());
        for (BPlusFixedTree<Integer, DistanceList> aResult : resultBPlusTrees) {
          DistanceList aDistanceList = aResult.search(dbid.getIntegerID());
          if (aDistanceList != null) {
            totalDistanceList.addAll(aDistanceList, k);
          }
        }
        
        
      }
      
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
