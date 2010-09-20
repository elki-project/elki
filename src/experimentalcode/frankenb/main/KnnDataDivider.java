/**
 * 
 */
package experimentalcode.frankenb.main;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

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
import de.lmu.ifi.dbs.elki.utilities.pairs.CPair;

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
      
      //create and fill the segments
      List<Set<DBID>> segments = new ArrayList<Set<DBID>>();
      for (int i = 0; i < segmentQuantity; ++i) {
        int itemsToWrite = (i == segmentQuantity - 1 ? ids.size() : itemsPerSegment); // the last one gets the rest
        
        Set<DBID> segment = new HashSet<DBID>();
        segments.add(segment);
        
        for (int j = 0; j < itemsToWrite; ++j) { 
          int id = random.nextInt(ids.size());
          segment.add(ids.remove(id));
        }        
        
      }
      
      //create permutations
      Set<CPair<Integer, Integer>> segmentPermutations = permutateSegments(segmentQuantity);
      
      System.out.println(segmentPermutations);
      
      int i = 0;
      for (CPair<Integer, Integer> segmentPermutation : segmentPermutations) {
        
        System.out.println(String.format("Writing package %03d of %03d", i + 1, segmentPermutations.size()));
        String filenamePrefix = String.format("p%03d_", i);
        
        Set<DBID> segmentOne = segments.get(segmentPermutation.getFirst());
        Set<DBID> segmentTwo = segments.get(segmentPermutation.getSecond());
        
        List<Set<DBID>> selectedSegments = new ArrayList<Set<DBID>>();
        selectedSegments.add(segmentOne);
        if (!segmentPermutation.getFirst().equals(segmentPermutation.getSecond())) {
          selectedSegments.add(segmentTwo);
        }
        
        List<String> segmentFilenames = new ArrayList<String>();
        int segmentCounter = 0;
        for (Set<DBID> segment : selectedSegments) {
          String segmentFilename = filenamePrefix + String.format("s%01d.dat", segmentCounter++);
          segmentFilenames.add(segmentFilename);
          
          File segmentFile = new File(outputDir, segmentFilename);
          System.out.print(String.format("\tsegment %1d of %1d ... ", segmentCounter, selectedSegments.size()));
          deleteAlreadyExistingFile(segmentFile);
          
          OnDiskArray onDiskArray = new OnDiskArray(
              segmentFile, 
              MAGIC_NUMBER, 
              4, // = 4 byte header = 1 int (32bit)
              database.dimensionality() * 8 + 4, // = 64bit of a double * dimensionality + 1 int id
              segment.size()
            );

          //saving dimensionality
          onDiskArray.getExtraHeader().putInt(database.dimensionality());
          
          int dataPointer = 0;
          for (DBID segmentDataID : segment) { // the last one gets the rest
            
            ByteBuffer buffer = onDiskArray.getRecordBuffer(dataPointer++);
            DoubleVector vector = database.get(segmentDataID);

            //first we write the id
            buffer.putInt(segmentDataID.getIntegerID());
            
            // at this point we assume that all elements have the same
            // dimensionality within a database
            for (int k = 1; k <= database.dimensionality(); ++k) {
              buffer.putDouble(vector.getValue(k));
            }
            
          }
          
          onDiskArray.close();
          System.out.println("done.");
        }
        
        File headerFile = new File(outputDir, filenamePrefix + "header.xml");
        
        //write xml header
        writeHeaderFile(headerFile, segmentFilenames, database.dimensionality());
        
        i++;
      }
      
    } catch (RuntimeException e) {
      throw e;
    } catch (UnableToComplyException e) {
      throw e;
    } catch (Exception e) {
      throw new UnableToComplyException(e);
    }
  }
  
  private static void deleteAlreadyExistingFile(File file) throws UnableToComplyException {
    if (file.exists()) {
      if (!file.delete()) throw new UnableToComplyException("File " + file.getName() + " already exists and could not be removed.");
    }
    
  }
  
  /**
   * Returns all possible permutations
   * 
   * @param i
   * @param segmentQuantity
   * @return
   */
  private static Set<CPair<Integer, Integer>> permutateSegments(int segmentQuantity) {
    Set<CPair<Integer, Integer>> permutations = new HashSet<CPair<Integer, Integer>>();
    for (int i = 0; i < segmentQuantity; ++i) {
      for (int j = i; j < segmentQuantity; ++j) {
        permutations.add(new CPair<Integer, Integer>(i, j));
      }
    }
    
    return permutations;
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
    return (int)Math.floor((Math.sqrt(1 + packageQuantity * 8) - 1) / 2.0);
  }
  
  private static void writeHeaderFile(File headerFile, List<String> segmentFilenames, int dimensionality) throws ParserConfigurationException, TransformerException, IOException {
    DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
    DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
    Document doc = docBuilder.newDocument();

    Comment comment = doc.createComment("KNN cluster precalculation data");
    doc.appendChild(comment);
    
    Element rootElement = doc.createElement("package");
    doc.appendChild(rootElement);
    
    Element dimensionalityElement = doc.createElement("dimensionality");
    rootElement.appendChild(dimensionalityElement);
    dimensionalityElement.setTextContent(String.valueOf(dimensionality));
    
    Element segmentsElement = doc.createElement("segments");
    rootElement.appendChild(segmentsElement);

    if (segmentFilenames.size() == 1) {
      segmentFilenames.add(segmentFilenames.get(0));
    }
    
    for (String segmentFilename : segmentFilenames) {
      Element segmentElement = doc.createElement("segment");
      segmentsElement.appendChild(segmentElement);
      segmentElement.setTextContent(segmentFilename);
    }
    
    TransformerFactory transfac = TransformerFactory.newInstance();
    Transformer trans = transfac.newTransformer();
    trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
    trans.setOutputProperty(OutputKeys.INDENT, "yes");

    //create xml file
    FileWriter fileWriter = null;
    try {
      fileWriter = new FileWriter(headerFile);
      
      StreamResult result = new StreamResult(fileWriter);
      DOMSource source = new DOMSource(doc);
      trans.transform(source, result);
    } finally {
      if (fileWriter != null) {
        fileWriter.close();
      }
    }
  }
  
  public static void main(String[] args) {
    StandAloneApplication.runCLIApplication(KnnDataDivider.class, args);
  }



}
