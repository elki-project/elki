/**
 * 
 */
package experimentalcode.frankenb.model;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Comment;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * This is the descriptor of a precalculation processing package
 * 
 * @author Florian Frankenberger
 */
public class PackageDescriptor {

  @SuppressWarnings("unused")
  private static final Logging LOG = Logging.getLogger(PackageDescriptor.class);
  
  public class Pairing extends Pair<Integer, Integer>{

    private File resultFile; 
    private int resultFilePageSize;
    
    public Pairing(Integer first, Integer second) {
      this(first, second, null, 0);
    }
    /**
     * @param first
     * @param second
     */
    public Pairing(Integer first, Integer second, File resultFile, int blocksize) {
      super(first, second);
      this.resultFile = resultFile;
      this.resultFilePageSize = blocksize;
    }
    
    public File getFirstPartitionFile() {
      return getFileForPartition(this.first);
    }
    
    public File getSecondPartitionFile() {
      return getFileForPartition(this.second);
    }
    
    /**
     * @return the resultFile
     */
    public File getResultFile() {
      return this.resultFile;
    }
    
    public boolean hasResult() {
      return this.resultFile != null;
    }
    
    /**
     * @return the resulFileBlockSize
     */
    public int getResulFilePageSize() {
      return this.resultFilePageSize;
    }
    
    /**
     * @param resultFile the resultFile to set
     */
    public void setResultFile(File resultFile, int pageSize) {
      this.resultFile = resultFile;
      this.resultFilePageSize = pageSize;
    }
    
  }
  
  private List<File> partitionFiles = new ArrayList<File>();
  private List<Pairing> partitionPairings = new ArrayList<Pairing>();
  private int dimensionality = 0;
  
  private final int id;
  
  public PackageDescriptor(int id) {
    this.id = id;
  }
  
  public void setDimensionality(int dimensionality) {
    this.dimensionality = dimensionality;
  }
  
  /**
   * @return the dimensionality
   */
  public int getDimensionality() {
    return this.dimensionality;
  }
  
  /**
   * @return the id
   */
  public int getId() {
    return this.id;
  }
  
  /**
   * Adds a pairing of partitions and automatically
   * adds the partition files to this descriptor if
   * they are contained already
   * 
   * @param pair
   */
  public void addPartitionPairing(Pair<File, File> pair) {
    int firstPosition = this.findOrInsertPartitionFileName(pair.first);
    int secondPosition = this.findOrInsertPartitionFileName(pair.second);
    
    this.partitionPairings.add(new Pairing(firstPosition, secondPosition));
  }
  
  public List<Pairing> getPartitionPairings() {
    return this.partitionPairings;
  }
  
  private File getFileForPartition(int paritionid) {
    return this.partitionFiles.get(paritionid);
  }
  
  /**
   * Searches the position of the given partitonName within the partitionFilenames
   * or inserts it to the list if it is not already contained and returns its new position
   * 
   * @param partitionName
   * @return
   */
  private int findOrInsertPartitionFileName(File partitionFilename) {
    int position = 0; 
    for (File aPartitionFile : this.partitionFiles) {
      if (aPartitionFile.equals(partitionFilename)) {
        return position; 
      }
      position++;
    }
    
    this.partitionFiles.add(partitionFilename);
    return this.partitionFiles.size() - 1;
  }
  
  public void saveToFile(File file) throws IOException {
    try {
      DocumentBuilderFactory dbfac = DocumentBuilderFactory.newInstance();
      DocumentBuilder docBuilder = dbfac.newDocumentBuilder();
      Document doc = docBuilder.newDocument();
  
      Comment comment = doc.createComment(
            "\nKNN precalculation package descriptor" +
            "\n    Created: " + new SimpleDateFormat("dd.MM.yyyy").format(new Date()) + 
            "\n"
          );
      doc.appendChild(comment);
      
      Element rootElement = doc.createElement("package");
      doc.appendChild(rootElement);

      Element idElement = doc.createElement("id");
      idElement.setTextContent(String.valueOf(this.id));
      rootElement.appendChild(idElement);
      
      Element dimensionalityElement = doc.createElement("dimensionality");
      rootElement.appendChild(dimensionalityElement);
      dimensionalityElement.setTextContent(String.valueOf(dimensionality));
      
      Element partitionsElement = doc.createElement("partitions");
      rootElement.appendChild(partitionsElement);
  
      Element partitionFilenamesElement = doc.createElement("filenames");
      partitionsElement.appendChild(partitionFilenamesElement);
      
      for (int index = 0; index < partitionFiles.size(); ++index) {
        File partitionFile = partitionFiles.get(index);
        Element partitionElement = doc.createElement("partition");
        partitionElement.setAttribute("id", String.valueOf(index));
        partitionFilenamesElement.appendChild(partitionElement);
        partitionElement.setTextContent(partitionFile.getName());
      }
      
      Element partitionsPairingsElement = doc.createElement("pairings");
      partitionsElement.appendChild(partitionsPairingsElement);
      for (Pairing partitionPairing : this.partitionPairings) {
        Element partitionsPairingElement = doc.createElement("pairing");
        partitionsPairingElement.setAttribute("what", String.valueOf(partitionPairing.first));
        partitionsPairingElement.setAttribute("with", String.valueOf(partitionPairing.second));

        //result
        if (partitionPairing.hasResult()) {
          Element resultElement = doc.createElement("result");
          
          resultElement.setTextContent(partitionPairing.getResultFile().getName());
          resultElement.setAttribute("pagesize", String.valueOf(partitionPairing.getResulFilePageSize()));
          partitionsPairingElement.appendChild(resultElement);
        }
        
        partitionsPairingsElement.appendChild(partitionsPairingElement);
      }
      
      TransformerFactory transfac = TransformerFactory.newInstance();
      transfac.setAttribute("indent-number", new Integer(4));
      
      Transformer trans = transfac.newTransformer();
      trans.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "no");
      trans.setOutputProperty(OutputKeys.ENCODING, "UTF-8");
      trans.setOutputProperty(OutputKeys.INDENT, "yes");
  
      //create xml file
      FileWriter fileWriter = null;
      try {
        fileWriter = new FileWriter(file);
        
        StreamResult result = new StreamResult(fileWriter);
        DOMSource source = new DOMSource(doc);
        trans.transform(source, result);
      } finally {
        if (fileWriter != null) {
          fileWriter.close();
        }
      }
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Could not write package descriptor", e);
    }    
  }

  public static PackageDescriptor loadFromFile(File file) throws IOException {
    try {
      DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
      DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
      Document doc = dBuilder.parse(file);
      Element rootElement = doc.getDocumentElement();
      
      PackageDescriptor packageDescriptor = new PackageDescriptor(Integer.valueOf(getRequiredSubElement(rootElement, "id").getTextContent()));
      
      Element dimensionalityElement = getRequiredSubElement(rootElement, "dimensionality");
      packageDescriptor.setDimensionality(Integer.valueOf(dimensionalityElement.getTextContent()));
      
      Element partitionsElement = getRequiredSubElement(rootElement, "partitions");
      readPartitionFiles(file.getParentFile(), packageDescriptor, partitionsElement);
      readPartitionPairings(file.getParentFile(), packageDescriptor, partitionsElement);
      
      return packageDescriptor;
    } catch (RuntimeException e) {
      throw e;
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException("Could not read package descriptor", e);
    }
  }
  
  private static Element getRequiredSubElement(Element parentElement, String tagName) {
    return getSubElement(parentElement, tagName, true);
  }
  
  private static Element getSubElement(Element parentElement, String tagName, boolean required) {
    NodeList list = parentElement.getElementsByTagName(tagName);
    if (required && list.getLength() < 1)
      throw new IllegalStateException("Required tag \"" + tagName + "\" not found");
    
    if (list.getLength() < 1) return null;
    
    return (Element) list.item(0);
  }
  
  private static void readPartitionFiles(File parentDirectory, PackageDescriptor packageDescriptor, Element partitionsElement) {
    Element filenamesElement = getRequiredSubElement(partitionsElement, "filenames");
    NodeList nodes = filenamesElement.getChildNodes();
    for (int i = 0; i < nodes.getLength(); ++i) {
      Node node =  nodes.item(i);
      if (!node.getNodeName().equals("partition")) continue;
      File file = new File(parentDirectory, node.getTextContent());
      System.out.println(file);
      packageDescriptor.partitionFiles.add(file);
    }
  }
  
  private static void readPartitionPairings(File parentDirectory, PackageDescriptor packageDescriptor, Element partitionsElement) {
    Element filenamesElement = getRequiredSubElement(partitionsElement, "pairings");
    NodeList nodes = filenamesElement.getChildNodes();
    for (int i = 0; i < nodes.getLength(); ++i) {
      Node node =  nodes.item(i);
      if (!node.getNodeName().equals("pairing")) continue;
      NamedNodeMap map = node.getAttributes();
      int what = Integer.valueOf(map.getNamedItem("what").getTextContent());
      int with = Integer.valueOf(map.getNamedItem("with").getTextContent());
      
      Element resultElement = getSubElement((Element) node, "result", false);
      if (resultElement != null) {
        int pageSize = Integer.valueOf(resultElement.getAttribute("pagesize"));
        File file = new File(parentDirectory, resultElement.getTextContent());
        packageDescriptor.partitionPairings.add(packageDescriptor.new Pairing(what, with, file, pageSize));
      } else {
        packageDescriptor.partitionPairings.add(packageDescriptor.new Pairing(what, with));
      }
    }
  }
}
