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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

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
import experimentalcode.frankenb.model.ifaces.Partition;

/**
 * This is the descriptor of a precalculation processing package
 * 
 * @author Florian Frankenberger
 */
public class PackageDescriptor {

  @SuppressWarnings("unused")
  private static final Logging LOG = Logging.getLogger(PackageDescriptor.class);
    
  private List<PartitionPairing> partitionPairings = new ArrayList<PartitionPairing>();
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
  public void addPartitionPairing(PartitionPairing partitionPairing) {
    this.partitionPairings.add(partitionPairing);
  }
  
  public List<PartitionPairing> getPartitionPairings() {
    return this.partitionPairings;
  }
  
  /**
   * Creates a subdirectory containing all necessary files for this
   * package below the given directory
   * 
   * @param packageDescriptorFile
   * @throws IOException
   */
  public void saveToFile(File packageDescriptorFile) throws IOException {
    try {
      File packageDirectory = packageDescriptorFile.getParentFile();
      
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
      
      //search all partitions involved and assign them a unique id
      int counter = 0;
      Map<Partition, Integer> partitions = new HashMap<Partition, Integer>();
      for (PartitionPairing partitionPairing : this.partitionPairings) {
        for (Partition partition : new Partition[] { partitionPairing.getPartitionOne(), partitionPairing.getPartitionTwo() }) {
          if (!partitions.containsKey(partition)) {
            partitions.put(partition, counter++);
          }
        }
      }
           
      for (Entry<Partition, Integer> entry : partitions.entrySet()) {
        Partition partition = entry.getKey();
        int id = entry.getValue();
        
        File partitionFile = new File(packageDirectory, String.format("package%05d_partition%05d.dat", this.id, id));
        if (!partitionFile.exists()) {
          partition.copyToFile(partitionFile);
        }
        
        Element partitionElement = doc.createElement("partition");
        partitionElement.setAttribute("id", String.valueOf(id));
        partitionFilenamesElement.appendChild(partitionElement);
        partitionElement.setTextContent(partitionFile.getName());
      }
      
      Element partitionsPairingsElement = doc.createElement("pairings");
      partitionsElement.appendChild(partitionsPairingsElement);
      for (PartitionPairing partitionPairing : this.partitionPairings) {
        Element partitionsPairingElement = doc.createElement("pairing");
        partitionsPairingElement.setAttribute("what", String.valueOf(partitions.get(partitionPairing.getPartitionOne())));
        partitionsPairingElement.setAttribute("with", String.valueOf(partitions.get(partitionPairing.getPartitionTwo())));

        //result
        if (partitionPairing.hasResult()) {
          Element resultElement = doc.createElement("resultdir");
          resultElement.setTextContent(partitionPairing.getResult().getDirectoryStorage().getSource().getName());
          partitionsPairingElement.appendChild(resultElement);
          
          resultElement = doc.createElement("resultdat");
          resultElement.setTextContent(partitionPairing.getResult().getDataStorage().getSource().getName());
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
        fileWriter = new FileWriter(packageDescriptorFile);
        
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
      Map<Integer, Partition> partitions = readPartitionFiles(file.getParentFile(), packageDescriptor, partitionsElement);
      readPartitionPairings(file.getParentFile(), packageDescriptor, partitions, partitionsElement);
      
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
  
  private static Map<Integer, Partition> readPartitionFiles(File parentDirectory, PackageDescriptor packageDescriptor, Element partitionsElement) throws IOException {
    Element filenamesElement = getRequiredSubElement(partitionsElement, "filenames");
    NodeList nodes = filenamesElement.getChildNodes();
    
    Map<Integer, Partition> partitions = new HashMap<Integer, Partition>();
    for (int i = 0; i < nodes.getLength(); ++i) {
      Node node =  nodes.item(i);
      if (!node.getNodeName().equals("partition")) continue;
      File file = new File(parentDirectory, node.getTextContent());
      int id = Integer.valueOf(node.getAttributes().getNamedItem("id").getNodeValue());
      
      partitions.put(id, BufferedDiskBackedPartition.loadFromFile(packageDescriptor.getDimensionality(), file));
    }
    
    return partitions;
  }
  
  private static void readPartitionPairings(File parentDirectory, PackageDescriptor packageDescriptor, Map<Integer, Partition> partitions, Element partitionsElement) throws IOException {
    Element filenamesElement = getRequiredSubElement(partitionsElement, "pairings");
    NodeList nodes = filenamesElement.getChildNodes();
    for (int i = 0; i < nodes.getLength(); ++i) {
      Node node =  nodes.item(i);
      if (!node.getNodeName().equals("pairing")) continue;
      NamedNodeMap map = node.getAttributes();
      int what = Integer.valueOf(map.getNamedItem("what").getTextContent());
      Partition whatPartition = partitions.get(what);
      
      int with = Integer.valueOf(map.getNamedItem("with").getTextContent());
      Partition withPartition = partitions.get(with);
      
      Element resultDirElement = getSubElement((Element) node, "resultdir", false);
      Element resultDatElement = getSubElement((Element) node, "resultdat", false);
      if (resultDirElement != null && resultDatElement != null) {
        File resultFileDir = new File(parentDirectory, resultDirElement.getTextContent());
        File resultFileDat = new File(parentDirectory, resultDatElement.getTextContent());
        
        DynamicBPlusTree<Integer, DistanceList> result = new DynamicBPlusTree<Integer, DistanceList>(
            new BufferedDiskBackedDataStorage(resultFileDir), 
            new HandlerFreeDiskBackedDataStorage(resultFileDat), 
            new ConstantSizeIntegerSerializer(),
            new DistanceListSerializer()
            );
        packageDescriptor.partitionPairings.add(new PartitionPairing(whatPartition, withPartition, result));
      } else {
        packageDescriptor.partitionPairings.add(new PartitionPairing(whatPartition, withPartition));
      }
    }
  }
}
