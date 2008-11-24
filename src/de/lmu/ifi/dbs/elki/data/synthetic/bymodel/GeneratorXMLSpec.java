package de.lmu.ifi.dbs.elki.data.synthetic.bymodel;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Random;
import java.util.regex.Pattern;

import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.events.Attribute;
import javax.xml.stream.events.StartElement;
import javax.xml.stream.events.XMLEvent;

import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.Distribution;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.math.linearalgebra.Vector;
import de.lmu.ifi.dbs.elki.utilities.UnableToComplyException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.IntParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.wrapper.StandAloneWrapper;

/**
 * Generate a data set based on a specified model (using an XML specification)
 * 
 * @author Erich Schubert
 */
public class GeneratorXMLSpec extends StandAloneWrapper {
  /**
   * A pattern defining whitespace.
   */
  public static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

  /**
   * OptionID for {@link #CONFIGFILE_PARAM}
   */
  public static final OptionID CONFIGFILE_ID = OptionID.getOrCreateOptionID(
      "bymodel.spec", "The generator specification file.");

  /**
   * Parameter to give the configuration file
   */
  private final FileParameter CONFIGFILE_PARAM = new FileParameter(CONFIGFILE_ID,
      FileParameter.FileType.INPUT_FILE);

  /**
   * OptionID for {@link #RANDOMSEED_PARAM}
   */
  public static final OptionID RANDOMSEED_ID = OptionID.getOrCreateOptionID(
      "bymodel.randomseed", "The random generator seed.");

  /**
   * Parameter to give the configuration file
   */
  private final IntParameter RANDOMSEED_PARAM = new IntParameter(RANDOMSEED_ID, null, true);

  /**
   * OptionID for {@link #SIZE_SCALE_PARAM}
   */
  public static final OptionID SIZE_SCALE_ID = OptionID.getOrCreateOptionID(
      "bymodel.sizescale", "Factor for scaling the specified cluster sizes.");

  /**
   * Parameter to give the configuration file
   */
  private final DoubleParameter SIZE_SCALE_PARAM = new DoubleParameter(SIZE_SCALE_ID, 1.0);

  /**
   * The configuration file.
   */
  File specfile;

  /**
   * Parameter for scaling the cluster sizes.
   */
  double sizescale = 1.0;
  
  /**
   * The actual generator class
   */
  public GeneratorMain gen = new GeneratorMain();

  /**
   * Random generator used for initializing cluster generators.
   */
  private Random clusterRandom = new Random();
  
  /**
   * Generator
   */
  public GeneratorXMLSpec() {
    super();
    addOption(RANDOMSEED_PARAM);
    addOption(CONFIGFILE_PARAM);
    addOption(SIZE_SCALE_PARAM);
  }

  /**
   * Sets the file parameter.
   */
  @Override
  public String[] setParameters(String[] args) throws ParameterException {
    String[] remainingParameters = super.setParameters(args);

    specfile = CONFIGFILE_PARAM.getValue();
    sizescale = SIZE_SCALE_PARAM.getValue();
    
    if (RANDOMSEED_PARAM.isSet())
      clusterRandom = new Random(RANDOMSEED_PARAM.getValue());

    setParameters(args, remainingParameters);
    return remainingParameters;
  }

  /**
   * Load the XML configuration file.
   * 
   * @throws UnableToComplyException
   */
  private void loadXMLSpecification(File specfile) throws UnableToComplyException {
    XMLInputFactory inputFactory = XMLInputFactory.newInstance();
    // Use validation if supported.
    if (inputFactory.isPropertySupported("javax.xml.stream.isValidating"))
      try {
        inputFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.TRUE);
      } catch(IllegalArgumentException e) {
        // probably the parser just doesn't support DTD validation.
        // we can live without.
      }
    InputStream in;
    try {
      in = new FileInputStream(specfile);
      XMLEventReader eventReader = inputFactory.createXMLEventReader(in);
      while(eventReader.hasNext()) {
        XMLEvent event = eventReader.nextEvent();
        if(event.isStartElement()) {
          if(event.asStartElement().getName().getLocalPart() == ("dataset")) {
            processElementDataset(event, eventReader);
            continue;
          }
          else
            warning("Unknown start element in XML specification file: " + event.asStartElement().getName());
        }
        else if(event.isEndElement())
          warning("Unknown end element in XML specification file: " + event.asEndElement().getName());
      }
    }
    catch(FileNotFoundException e) {
      throw new UnableToComplyException("Can't open specification file.", e);
    }
    catch(XMLStreamException e) {
      throw new UnableToComplyException("Can't parse specification file.", e);
    }
  }

  /**
   * Process a 'dataset' Element in the XML stream.
   * 
   * @param event
   * @param eventReader
   * @throws XMLStreamException
   * @throws UnableToComplyException
   */
  private void processElementDataset(XMLEvent event, XMLEventReader eventReader) throws XMLStreamException, UnableToComplyException {
    StartElement se = event.asStartElement();
    for(Iterator<?> i = se.getAttributes(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      
      // *** get parameters
      if(attr.getName().getLocalPart() == "random-seed")
        clusterRandom = new Random((int) (Integer.valueOf(attr.getValue()) * sizescale));
      // ***/
      else
        warning("Unknown attribute in XML specification file: " + attr.getName());
    }
    while(eventReader.hasNext()) {
      event = eventReader.nextEvent();
      if(event.isStartElement()) {
        if(event.asStartElement().getName().getLocalPart() == "cluster") {
          processElementCluster(event, eventReader);
        }
        else if(event.asStartElement().getName().getLocalPart() == "static") {
          processElementStatic(event, eventReader);
        }
        else
          warning("Unknown start element in XML specification file: " + event.asStartElement().getName());
      }
      else if(event.isEndElement()) {
        if(event.asEndElement().getName().getLocalPart() == "dataset")
          break;
        else
          warning("Unknown end element in XML specification file: " + event.asEndElement().getName());
      }
    }
    if(!eventReader.hasNext())
      warning("Parsing did not work correctly, didn't exit 'dataset' tag correctly.");
  }

  /**
   * Process a 'cluster' Element in the XML stream.
   * 
   * @param event
   * @param eventReader
   * @throws XMLStreamException
   * @throws UnableToComplyException
   */
  private void processElementCluster(XMLEvent event, XMLEventReader eventReader) throws XMLStreamException, UnableToComplyException {
    StartElement se = event.asStartElement();
    int size = -1;
    double overweight = 1.0;
    String name = null;
    for(Iterator<?> i = se.getAttributes(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      
      // *** get parameters
      if(attr.getName().getLocalPart() == "size")
        // size can be scaled by a given factor (useful for benchmarking scripts)
        size = (int) (Integer.valueOf(attr.getValue()) * sizescale);
      else if(attr.getName().getLocalPart() == "name")
        name = attr.getValue();
      else if(attr.getName().getLocalPart() == "density-correction")
        overweight = Double.valueOf(attr.getValue());
      // ***/
      else
        warning("Unknown attribute in XML specification file: " + attr.getName());
    }
    if(size < 0)
      throw new UnableToComplyException("No valid cluster size given in specification file.");
    if(name == null)
      throw new UnableToComplyException("No cluster name given in specification file.");
    
    // *** add new cluster object
    Random newRand = new Random(clusterRandom.nextLong());
    GeneratorSingleCluster cluster = new GeneratorSingleCluster(name, size, overweight, newRand);
    
    while(eventReader.hasNext()) {
      event = eventReader.nextEvent();
      if(event.isStartElement()) {
        if(event.asStartElement().getName().getLocalPart() == "uniform") {
          processElementUniform(cluster, event, eventReader);
        }
        else if(event.asStartElement().getName().getLocalPart() == "normal") {
          processElementNormal(cluster, event, eventReader);
        }
        else if(event.asStartElement().getName().getLocalPart() == "rotate") {
          processElementRotate(cluster, event, eventReader);
        }
        else if(event.asStartElement().getName().getLocalPart() == "translate") {
          processElementTranslate(cluster, event, eventReader);
        }
        else if(event.asStartElement().getName().getLocalPart() == "clip") {
          processElementClipping(cluster, event, eventReader);
        }
        else
          warning("Unknown start element in XML specification file: " + event.asStartElement().getName());
      }
      else if(event.isEndElement()) {
        if(event.asEndElement().getName().getLocalPart() == "cluster") {
          gen.addCluster(cluster);
          if(isVerbose())
            verbose("Loaded cluster " + cluster.getName() + " from specification.");
          break;
        }
        else
          warning("Unknown end element in XML specification file: " + event.asEndElement().getName());
      }
    }
    if(!eventReader.hasNext())
      warning("Parsing did not work correctly, didn't exit 'cluster' tag correctly.");
  }

  /**
   * Process a 'uniform' Element in the XML stream.
   * 
   * @param cluster
   * @param event
   * @param eventReader
   * @throws XMLStreamException
   * @throws UnableToComplyException
   */
  private void processElementUniform(GeneratorSingleCluster cluster, XMLEvent event, XMLEventReader eventReader) throws XMLStreamException, UnableToComplyException {
    StartElement se = event.asStartElement();
    double min = 0.0;
    double max = 1.0;
    for(Iterator<?> i = se.getAttributes(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      // *** Get parameters
      if(attr.getName().getLocalPart() == "min")
        min = Double.valueOf(attr.getValue());
      else if(attr.getName().getLocalPart() == "max")
        max = Double.valueOf(attr.getValue());
      else
        warning("Unknown attribute in XML specification file: " + attr.getName());
    }
    
    // *** new uniform generator
    Random random = cluster.getNewRandomGenerator();
    Distribution generator = new UniformDistribution(min, max, random);
    
    while(eventReader.hasNext()) {
      event = eventReader.nextEvent();
      if(event.isStartElement()) {
        warning("Unknown start element in XML specification file: " + event.asStartElement().getName());
      }
      else if(event.isEndElement()) {
        if(event.asEndElement().getName().getLocalPart() == "uniform") {
          cluster.addGenerator(generator);
          break;
        }
        else
          warning("Unknown end element in XML specification file: " + event.asEndElement().getName());
      }
    }
    if(!eventReader.hasNext())
      warning("Parsing did not work correctly, didn't exit 'uniform' tag correctly.");
  }

  /**
   * Process a 'normal' Element in the XML stream.
   * 
   * @param cluster
   * @param event
   * @param eventReader
   * @throws XMLStreamException
   * @throws UnableToComplyException
   */
  private void processElementNormal(GeneratorSingleCluster cluster, XMLEvent event, XMLEventReader eventReader) throws XMLStreamException, UnableToComplyException {
    StartElement se = event.asStartElement();
    double mean = 0.0;
    double stddev = 1.0;
    for(Iterator<?> i = se.getAttributes(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      
      // *** Get parameters
      if(attr.getName().getLocalPart() == "mean")
        mean = Double.valueOf(attr.getValue());
      else if(attr.getName().getLocalPart() == "stddev")
        stddev = Double.valueOf(attr.getValue());
      else
        warning("Unknown attribute in XML specification file: " + attr.getName());
    }
    
    // *** New normal distribution generator
    Random random = cluster.getNewRandomGenerator();
    Distribution generator = new NormalDistribution(mean, stddev, random);
    
    while(eventReader.hasNext()) {
      event = eventReader.nextEvent();
      if(event.isStartElement()) {
        warning("Unknown start element in XML specification file: " + event.asStartElement().getName());
      }
      else if(event.isEndElement()) {
        if(event.asEndElement().getName().getLocalPart() == "normal") {
          cluster.addGenerator(generator);
          break;
        }
        else
          warning("Unknown end element in XML specification file: " + event.asEndElement().getName());
      }
    }
    if(!eventReader.hasNext())
      warning("Parsing did not work correctly, didn't exit 'normal' tag correctly.");
  }

  /**
   * Process a 'rotate' Element in the XML stream.
   * 
   * @param cluster
   * @param event
   * @param eventReader
   * @throws XMLStreamException
   * @throws UnableToComplyException
   */
  private void processElementRotate(GeneratorSingleCluster cluster, XMLEvent event, XMLEventReader eventReader) throws XMLStreamException, UnableToComplyException {
    StartElement se = event.asStartElement();
    int axis1 = 0;
    int axis2 = 0;
    double angle = 0.0;
    for(Iterator<?> i = se.getAttributes(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      // Get parameters
      if(attr.getName().getLocalPart() == "axis1")
        axis1 = Integer.valueOf(attr.getValue());
      else if(attr.getName().getLocalPart() == "axis2")
        axis2 = Integer.valueOf(attr.getValue());
      else if(attr.getName().getLocalPart() == "angle")
        angle = Double.valueOf(attr.getValue());
      else
        warning("Unknown attribute in XML specification file: " + attr.getName());
    }
    if(axis1 <= 0 || axis1 > cluster.getDim())
      throw new UnableToComplyException("Invalid axis1 number given in specification file.");
    if(axis1 <= 0 || axis1 > cluster.getDim())
      throw new UnableToComplyException("Invalid axis2 number given in specification file.");
    if(axis1 == axis2)
      throw new UnableToComplyException("Invalid axis numbers given in specification file.");
    
    // Add rotation to cluster.
    cluster.addRotation(axis1 - 1, axis2 - 1, Math.toRadians(angle));
    
    while(eventReader.hasNext()) {
      event = eventReader.nextEvent();
      if(event.isStartElement()) {
        warning("Unknown start element in XML specification file: " + event.asStartElement().getName());
      }
      else if(event.isEndElement()) {
        if(event.asEndElement().getName().getLocalPart() == "rotate")
          break;
        else
          warning("Unknown end element in XML specification file: " + event.asEndElement().getName());
      }
    }
    if(!eventReader.hasNext())
      warning("Parsing did not work correctly, didn't exit 'rotate' tag correctly.");
  }

  /**
   * Process a 'translate' Element in the XML stream.
   * 
   * @param cluster
   * @param event
   * @param eventReader
   * @throws XMLStreamException
   * @throws UnableToComplyException
   */
  private void processElementTranslate(GeneratorSingleCluster cluster, XMLEvent event, XMLEventReader eventReader) throws XMLStreamException, UnableToComplyException {
    StartElement se = event.asStartElement();
    Vector offset = null;
    for(Iterator<?> i = se.getAttributes(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      // *** get parameters
      if(attr.getName().getLocalPart() == "vector")
        offset = parseVector(attr.getValue());
      else
        warning("Unknown attribute in XML specification file: " + attr.getName());
    }
    if(offset == null)
      throw new UnableToComplyException("No translation vector given.");
    
    // *** add new translation
    cluster.addTranslation(offset);
    
    while(eventReader.hasNext()) {
      event = eventReader.nextEvent();
      if(event.isStartElement()) {
        warning("Unknown start element in XML specification file: " + event.asStartElement().getName());
      }
      else if(event.isEndElement()) {
        if(event.asEndElement().getName().getLocalPart() == "translate")
          break;
        else
          warning("Unknown end element in XML specification file: " + event.asEndElement().getName());
      }
    }
    if(!eventReader.hasNext())
      warning("Parsing did not work correctly, didn't exit 'translate' tag correctly.");
  }

  /**
   * Process a 'clipping' Element in the XML stream.
   * 
   * @param cluster
   * @param event
   * @param eventReader
   * @throws XMLStreamException
   * @throws UnableToComplyException
   */
  private void processElementClipping(GeneratorSingleCluster cluster, XMLEvent event, XMLEventReader eventReader) throws XMLStreamException, UnableToComplyException {
    StartElement se = event.asStartElement();
    Vector cmin = null;
    Vector cmax = null;
    for(Iterator<?> i = se.getAttributes(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      // get parameters 
      if(attr.getName().getLocalPart() == "min")
        cmin = parseVector(attr.getValue());
      else if(attr.getName().getLocalPart() == "max")
        cmax = parseVector(attr.getValue());
      else
        warning("Unknown attribute in XML specification file: " + attr.getName());
    }
    if(cmin == null || cmax == null)
      throw new UnableToComplyException("No or incomplete clipping vectors given.");
    
    // *** set clipping
    cluster.setClipping(cmin, cmax);
    
    while(eventReader.hasNext()) {
      event = eventReader.nextEvent();
      if(event.isStartElement()) {
        warning("Unknown start element in XML specification file: " + event.asStartElement().getName());
      }
      else if(event.isEndElement()) {
        if(event.asEndElement().getName().getLocalPart() == "clip")
          break;
        else
          warning("Unknown end element in XML specification file: " + event.asEndElement().getName());
      }
    }
    if(!eventReader.hasNext())
      warning("Parsing did not work correctly, didn't exit 'clipping' tag correctly.");
  }

  /**
   * Process a 'static' cluster Element in the XML stream.
   * 
   * @param event
   * @param eventReader
   * @throws XMLStreamException
   * @throws UnableToComplyException
   */
  private void processElementStatic(XMLEvent event, XMLEventReader eventReader) throws XMLStreamException, UnableToComplyException {
    StartElement se = event.asStartElement();
    String name = null;
    for(Iterator<?> i = se.getAttributes(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      
      // *** get parameters
      if(attr.getName().getLocalPart() == "name")
        name = attr.getValue();
      else
        warning("Unknown attribute in XML specification file: " + attr.getName());
    }
    if(name == null)
      throw new UnableToComplyException("No cluster name given in specification file.");
    
    LinkedList<Vector> points = new LinkedList<Vector>();
    while(eventReader.hasNext()) {
      event = eventReader.nextEvent();
      if(event.isStartElement()) {
        if(event.asStartElement().getName().getLocalPart() == "point") {
          processElementPoint(points, event, eventReader);
        }
        else
          warning("Unknown start element in XML specification file: " + event.asStartElement().getName());
      }
      else if(event.isEndElement()) {
        if(event.asEndElement().getName().getLocalPart() == "static") {          
          // *** add new cluster object
          GeneratorStatic cluster = new GeneratorStatic(name, points);
          
          gen.addCluster(cluster);
          if(isVerbose())
            verbose("Loaded cluster " + cluster.name + " from specification.");
          break;
        }
        else
          warning("Unknown end element in XML specification file: " + event.asEndElement().getName());
      }
    }
    if(!eventReader.hasNext())
      warning("Parsing did not work correctly, didn't exit 'cluster' tag correctly.");
  }

  /**
   * Parse a 'point' element (point vector for a static cluster)
   * 
   * @param points current list of points (to append to)
   * @param event XML event
   * @param eventReader XML event reader
   * @throws XMLStreamException
   * @throws UnableToComplyException
   */
  private void processElementPoint(LinkedList<Vector> points, XMLEvent event, XMLEventReader eventReader) throws XMLStreamException, UnableToComplyException {
    StartElement se = event.asStartElement();
    Vector point = null;
    for(Iterator<?> i = se.getAttributes(); i.hasNext();) {
      Attribute attr = (Attribute) i.next();
      // *** get parameters
      if(attr.getName().getLocalPart() == "vector")
        point = parseVector(attr.getValue());
      else
        warning("Unknown attribute in XML specification file: " + attr.getName());
    }
    if(point == null)
      throw new UnableToComplyException("No translation vector given.");
    
    // *** add new point
    points.add(point);
    
    while(eventReader.hasNext()) {
      event = eventReader.nextEvent();
      if(event.isStartElement()) {
        warning("Unknown start element in XML specification file: " + event.asStartElement().getName());
      }
      else if(event.isEndElement()) {
        if(event.asEndElement().getName().getLocalPart() == "point")
          break;
        else
          warning("Unknown end element in XML specification file: " + event.asEndElement().getName());
      }
    }
    if(!eventReader.hasNext())
      warning("Parsing did not work correctly, didn't exit 'translate' tag correctly.");
  }

  /**
   * Parse a string into a vector.
   * 
   * TODO: move this into utility package?
   * 
   * @param s String to parse
   * @return Vector
   * @throws UnableToComplyException
   */
  private Vector parseVector(String s) throws UnableToComplyException {
    String[] entries = WHITESPACE_PATTERN.split(s);
    double[] d = new double[entries.length];
    for(int i = 0; i < entries.length; i++) {
      try {
        d[i] = Double.valueOf(entries[i]);
      }
      catch(NumberFormatException e) {
        throw new UnableToComplyException("Could not parse vector.");
      }
    }
    return new Vector(d);
  }

  /**
   * Main method to run this wrapper.
   * 
   * @param args the arguments to run this wrapper
   */
  public static void main(String[] args) {
    new GeneratorXMLSpec().runCLIWrapper(args);
  }

  /**
   * Runs the wrapper with the specified arguments.
   */
  public void run() throws UnableToComplyException {
    if(isVerbose())
      verbose("Loading specification ...");
    loadXMLSpecification(specfile);
    if(isVerbose())
      verbose("Generating clusters ...");
    gen.generate();
    if(isVerbose())
      verbose("Writing output ...");
    try {
      File outputFile = getOutput();
      if(outputFile.exists()) {
        if(isVerbose()) {
          verbose("The file " + outputFile + " already exists, " + "the generator result will be appended.");
        }
      }

      OutputStreamWriter outStream = new FileWriter(outputFile, true);
      gen.writeClusters(outStream);

      outStream.flush();
      outStream.close();
    }
    catch(FileNotFoundException e) {
      throw new UnableToComplyException(e.getMessage(), e);
    }
    catch(IOException e) {
      throw new UnableToComplyException(e.getMessage(), e);
    }
    if(isVerbose())
      verbose("Done.");
  }

  /**
   * Describe wrapper output.
   */
  @Override
  public String getOutputDescription() {
    return "the file to write the generated data set into, " + "if the file already exists, the generated points will be appended to this file.";
  }
}
