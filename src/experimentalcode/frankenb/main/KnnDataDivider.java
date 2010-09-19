/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import de.lmu.ifi.dbs.elki.application.StandAloneApplication;
import de.lmu.ifi.dbs.elki.data.DoubleVector;
import de.lmu.ifi.dbs.elki.database.Database;
import de.lmu.ifi.dbs.elki.database.connection.DatabaseConnection;
import de.lmu.ifi.dbs.elki.database.connection.FileBasedDatabaseConnection;
import de.lmu.ifi.dbs.elki.database.ids.DBID;
import de.lmu.ifi.dbs.elki.persistent.OnDiskArray;
import de.lmu.ifi.dbs.elki.utilities.exceptions.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.IntParameter;

/**
 * This application divides a given database into
 * a given numbers of packages to calculate knn
 * on a distributed system like the sun cluster
 * <p />
 * Example usage:
 * <br />
 * <code>-dbc.parser DoubleVectorLabelParser -dbc.in /ELKI/data/synthetic/outlier-scenarios/3-gaussian-2d.csv -app.out D:/tmp/knnparts -packagequantity 10</code>
 * 
 * @author Florian Frankenberger
 */
public class KnnDataDivider extends StandAloneApplication {

  private static final int MAGIC_NUMBER = 830920;
  
  /**
   * OptionID for {@link #PACKAGES_PARAM}
   */
  public static final OptionID PACKAGES_ID = OptionID.getOrCreateOptionID("packagequantity", "");

  /**
   * Parameter that specifies the number of segments to create (= # of computers)
   * <p>
   * Key: {@code -packages}
   * </p>
   */
  private final IntParameter PACKAGES_PARAM = new IntParameter(PACKAGES_ID, false);

  private int packageQuantity = 0;
  private final DatabaseConnection<DoubleVector> databaseConnection;
  
  /**
   * @param config
   */
  public KnnDataDivider(Parameterization config) {
    super(config);

    config = config.descend(this);
    PACKAGES_PARAM.setShortDescription(getPackagesDescription());
    if (config.grab(PACKAGES_PARAM)) {
      packageQuantity = PACKAGES_PARAM.getValue();      
    }
    
    databaseConnection = new FileBasedDatabaseConnection<DoubleVector>(config);
  }

  /**
   * @return
   */
  private String getPackagesDescription() {
    // TODO Auto-generated method stub
    return "# of packages(computers) to split the data in";
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
      Database<DoubleVector> database = databaseConnection.getDatabase(null);
      File outputDir = this.getOutput();
      
      if (outputDir.isFile()) 
        throw new UnableToComplyException("You need to specify an output directory not a file!");
      if (!outputDir.exists()) {
        if (!outputDir.mkdirs()) throw new UnableToComplyException("Could not create output directory");
      }
      
      int segmentQuantity = packagesQuantityToSegmentsQuantity(packageQuantity);
      int itemsPerSegment = (int) Math.floor(database.size() / segmentQuantity);
      List<DBID> ids = new ArrayList<DBID>(database.getIDs().asCollection());
      
      Random random = new Random(System.currentTimeMillis());
      
      for (int i = 0; i < segmentQuantity; ++i) {
        int itemsToWrite = (i == segmentQuantity - 1 ? ids.size() : itemsPerSegment);
        
        String filename = String.format("%03d.bin", i);
        System.out.print(String.format("Writing %7s (package %03d of %03d / %05d items remain to be distributed) ... ", filename, i + 1, segmentQuantity, ids.size()));
        
        File file = new File(outputDir, filename);
        if (file.exists()) {
          if (!file.delete()) throw new UnableToComplyException("File " + filename + " already exists and could not be removed.");
        }
        
        OnDiskArray onDiskArray = new OnDiskArray(
              file, 
              MAGIC_NUMBER, 
              4, // = 4 byte header = 1 int (32bit)
              database.dimensionality() * 8, // = 64bit of a double * dimensionality 
              itemsToWrite
            );
        
        ByteBuffer header = onDiskArray.getExtraHeader();
        
        //save dimensionality
        header.putInt(database.dimensionality());
        
        for (int j = 0; j < itemsToWrite; ++j) { // the last one gets the rest
          
          int id = random.nextInt(ids.size());
          ByteBuffer buffer = onDiskArray.getRecordBuffer(j);
          DoubleVector vector = database.get(ids.remove(id));

          // at this point we assume that all elements have the same
          // dimensionality within a database
          for (int k = 1; k <= database.dimensionality(); ++k) {
            buffer.putDouble(vector.getValue(k));
          }
          
        }
        
        onDiskArray.close();
        System.out.println("done.");
      }
      
    } catch (RuntimeException e) {
      throw e;
    } catch (UnableToComplyException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }
  
  /**
   * calculates the segments necessary to split the db into to calculate
   * the given number of packages
   * 
   * @return
   * @throws UnableToComplyException 
   */
  private static int packagesQuantityToSegmentsQuantity(int packageQuantity) throws UnableToComplyException {
    if (packageQuantity < 3) {
      throw new UnableToComplyException("Minimum is 3 packages");
    }
    return (int)Math.floor(Math.sqrt(1 + packageQuantity * 8) - 1 / 2.0);
  }
  
  public static void main(String[] args) {
    StandAloneApplication.runCLIApplication(KnnDataDivider.class, args);
  }



}
