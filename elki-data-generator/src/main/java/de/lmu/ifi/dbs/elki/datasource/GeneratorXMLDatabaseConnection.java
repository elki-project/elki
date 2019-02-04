/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2019
 * ELKI Development Team
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package de.lmu.ifi.dbs.elki.datasource;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.regex.Pattern;

import javax.xml.XMLConstants;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.SAXException;

import de.lmu.ifi.dbs.elki.application.GeneratorXMLSpec;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorMain;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorSingleCluster;
import de.lmu.ifi.dbs.elki.data.synthetic.bymodel.GeneratorStatic;
import de.lmu.ifi.dbs.elki.datasource.bundle.MultipleObjectsBundle;
import de.lmu.ifi.dbs.elki.datasource.filter.ObjectFilter;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.Distribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.GammaDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.HaltonUniformDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.NormalDistribution;
import de.lmu.ifi.dbs.elki.math.statistics.distribution.UniformDistribution;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.OptionID;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.DoubleParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.PatternParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.RandomParameter;
import de.lmu.ifi.dbs.elki.utilities.random.RandomFactory;
import de.lmu.ifi.dbs.elki.utilities.xml.XMLNodeIterator;

/**
 * Data source from an XML specification.
 *
 * This data source will generate random (or pseudo-random, fixed seeds are
 * supported) data sets that satisfy a given specification file.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @composed - - - GeneratorMain
 */
public class GeneratorXMLDatabaseConnection extends AbstractDatabaseConnection {
  /** Dataset tag */
  public static final String TAG_DATASET = "dataset";

  /** Cluster tag */
  public static final String TAG_CLUSTER = "cluster";

  /** Uniform distribution */
  public static final String TAG_UNIFORM = "uniform";

  /** Normal distribution */
  public static final String TAG_NORMAL = "normal";

  /** Gamma distribution */
  public static final String TAG_GAMMA = "gamma";

  /**
   * Halton pseudo uniform distribution.
   */
  public static final String TAG_HALTON = "halton";

  /** Rotation */
  public static final String TAG_ROTATE = "rotate";

  /** Translation */
  public static final String TAG_TRANSLATE = "translate";

  /** Clipping */
  public static final String TAG_CLIP = "clip";

  /** Static cluster */
  public static final String TAG_STATIC = "static";

  /** Point in static cluster */
  public static final String TAG_POINT = "point";

  /** Attribute to control model testing */
  public static final String ATTR_TEST = "test-model";

  /** Random seed */
  public static final String ATTR_SEED = "random-seed";

  /** Density correction factor */
  public static final String ATTR_DENSITY = "density-correction";

  /** Cluster nane */
  public static final String ATTR_NAME = "name";

  /** Cluster size */
  public static final String ATTR_SIZE = "size";

  /** Minimum value */
  public static final String ATTR_MIN = "min";

  /** Maximum value */
  public static final String ATTR_MAX = "max";

  /** Mean */
  public static final String ATTR_MEAN = "mean";

  /** Standard deviation */
  public static final String ATTR_STDDEV = "stddev";

  /** Gamma k */
  public static final String ATTR_K = "k";

  /** Gamma theta */
  public static final String ATTR_THETA = "theta";

  /** Vector */
  public static final String ATTR_VECTOR = "vector";

  /** First axis for rotation plane */
  public static final String ATTR_AXIS1 = "axis1";

  /** Second axis for rotation plane */
  public static final String ATTR_AXIS2 = "axis2";

  /** Rotation angle */
  public static final String ATTR_ANGLE = "angle";

  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(GeneratorXMLDatabaseConnection.class);

  /**
   * A pattern defining whitespace.
   */
  public static final Pattern WHITESPACE_PATTERN = Pattern.compile("\\s+");

  /**
   * File name of the generators XML Schema file.
   */
  public static final String GENERATOR_SCHEMA_FILE = GeneratorXMLSpec.class.getPackage().getName().replace('.', '/') + '/' + "GeneratorByModel.xsd";

  /**
   * The configuration file.
   */
  File specfile;

  /**
   * Parameter for scaling the cluster sizes.
   */
  double sizescale = 1.0;

  /**
   * Pattern for clusters to reassign.
   */
  Pattern reassign = null;

  /**
   * Random generator used for initializing cluster generators.
   */
  private RandomFactory clusterRandom = null;

  /**
   * Set testAgainstModel flag
   */
  private Boolean testAgainstModel;

  /**
   * Reassign objects by distance instead of density
   */
  private boolean reassignByDistance;

  /**
   * Constructor.
   *
   * @param filters Filters.
   * @param specfile Specification file
   * @param sizescale Size scaling
   * @param reassign Reassignment pattern
   * @param reassignByDistance Reassign objects by distance instead of density
   * @param clusterRandom Random number generator
   */
  public GeneratorXMLDatabaseConnection(List<ObjectFilter> filters, File specfile, double sizescale, Pattern reassign, boolean reassignByDistance, RandomFactory clusterRandom) {
    super(filters);
    this.specfile = specfile;
    this.sizescale = sizescale;
    this.reassign = reassign;
    this.reassignByDistance = reassignByDistance;
    this.clusterRandom = clusterRandom;
  }

  @Override
  public MultipleObjectsBundle loadData() {
    if(LOG.isVerbose()) {
      LOG.verbose("Loading specification ...");
    }
    GeneratorMain gen = loadXMLSpecification();
    if(testAgainstModel != null) {
      gen.setTestAgainstModel(testAgainstModel.booleanValue());
    }
    gen.setReassignPattern(reassign);
    gen.setReassignByDistance(reassignByDistance);
    if(LOG.isVerbose()) {
      LOG.verbose("Generating clusters ...");
    }
    return super.invokeBundleFilters(gen.generate());
  }

  /**
   * Load the XML configuration file.
   *
   * @return Generator
   */
  private GeneratorMain loadXMLSpecification() {
    try {
      DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
      dbf.setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false);
      URL url = ClassLoader.getSystemResource(GENERATOR_SCHEMA_FILE);
      if(url != null) {
        try {
          Schema schema = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI).newSchema(url);
          dbf.setSchema(schema);
          dbf.setIgnoringElementContentWhitespace(true);
        }
        catch(Exception e) {
          LOG.warning("Could not set up XML Schema validation for specification file.", e);
        }
      }
      else {
        LOG.warning("Could not set up XML Schema validation for specification file.");
      }
      Document doc = dbf.newDocumentBuilder().parse(specfile);
      Node root = doc.getDocumentElement();
      if(TAG_DATASET.equals(root.getNodeName())) {
        GeneratorMain gen = new GeneratorMain();
        processElementDataset(gen, root);
        return gen;
      }
      else {
        throw new AbortException("Experiment specification has incorrect document element: " + root.getNodeName());
      }
    }
    catch(FileNotFoundException e) {
      throw new AbortException("Can't open specification file.", e);
    }
    catch(SAXException e) {
      throw new AbortException("Error parsing specification file.", e);
    }
    catch(IOException e) {
      throw new AbortException("IO Exception loading specification file.", e);
    }
    catch(ParserConfigurationException e) {
      throw new AbortException("Parser Configuration Error", e);
    }
  }

  /**
   * Process a 'dataset' Element in the XML stream.
   *
   * @param gen Generator
   * @param cur Current document nod
   */
  private void processElementDataset(GeneratorMain gen, Node cur) {
    // *** get parameters
    String seedstr = ((Element) cur).getAttribute(ATTR_SEED);
    if(clusterRandom != RandomFactory.DEFAULT && seedstr != null && seedstr.length() > 0) {
      clusterRandom = new RandomFactory((long) (ParseUtil.parseIntBase10(seedstr) * sizescale));
    }
    String testmod = ((Element) cur).getAttribute(ATTR_TEST);
    if(testmod != null && testmod.length() > 0) {
      testAgainstModel = Boolean.valueOf(ParseUtil.parseIntBase10(testmod) != 0);
    }
    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(TAG_CLUSTER.equals(child.getNodeName())) {
        processElementCluster(gen, child);
      }
      else if(TAG_STATIC.equals(child.getNodeName())) {
        processElementStatic(gen, child);
      }
      else if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'cluster' Element in the XML stream.
   *
   * @param gen Generator
   * @param cur Current document nod
   */
  private void processElementCluster(GeneratorMain gen, Node cur) {
    int size = -1;
    double overweight = 1.0;

    String sizestr = ((Element) cur).getAttribute(ATTR_SIZE);
    if(sizestr != null && sizestr.length() > 0) {
      size = (int) (ParseUtil.parseIntBase10(sizestr) * sizescale);
    }

    String name = ((Element) cur).getAttribute(ATTR_NAME);

    String dcostr = ((Element) cur).getAttribute(ATTR_DENSITY);
    if(dcostr != null && dcostr.length() > 0) {
      overweight = ParseUtil.parseDouble(dcostr);
    }

    if(size < 0) {
      throw new AbortException("No valid cluster size given in specification file.");
    }
    if(name == null || name.length() == 0) {
      throw new AbortException("No cluster name given in specification file.");
    }

    // *** add new cluster object
    Random newRand = clusterRandom.getSingleThreadedRandom();
    GeneratorSingleCluster cluster = new GeneratorSingleCluster(name, size, overweight, newRand);

    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(TAG_UNIFORM.equals(child.getNodeName())) {
        processElementUniform(cluster, child);
      }
      else if(TAG_NORMAL.equals(child.getNodeName())) {
        processElementNormal(cluster, child);
      }
      else if(TAG_GAMMA.equals(child.getNodeName())) {
        processElementGamma(cluster, child);
      }
      else if(TAG_HALTON.equals(child.getNodeName())) {
        processElementHalton(cluster, child);
      }
      else if(TAG_ROTATE.equals(child.getNodeName())) {
        processElementRotate(cluster, child);
      }
      else if(TAG_TRANSLATE.equals(child.getNodeName())) {
        processElementTranslate(cluster, child);
      }
      else if(TAG_CLIP.equals(child.getNodeName())) {
        processElementClipping(cluster, child);
      }
      else if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }

    gen.addCluster(cluster);
  }

  /**
   * Process a 'uniform' Element in the XML stream.
   *
   * @param cluster
   * @param cur Current document nod
   */
  private void processElementUniform(GeneratorSingleCluster cluster, Node cur) {
    double min = 0.0;
    double max = 1.0;

    String minstr = ((Element) cur).getAttribute(ATTR_MIN);
    if(minstr != null && minstr.length() > 0) {
      min = ParseUtil.parseDouble(minstr);
    }
    String maxstr = ((Element) cur).getAttribute(ATTR_MAX);
    if(maxstr != null && maxstr.length() > 0) {
      max = ParseUtil.parseDouble(maxstr);
    }

    // *** new uniform generator
    Random random = cluster.getNewRandomGenerator();
    Distribution generator = new UniformDistribution(min, max, random);
    cluster.addGenerator(generator);

    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'normal' Element in the XML stream.
   *
   * @param cluster
   * @param cur Current document nod
   */
  private void processElementNormal(GeneratorSingleCluster cluster, Node cur) {
    double mean = 0.0;
    double stddev = 1.0;
    String meanstr = ((Element) cur).getAttribute(ATTR_MEAN);
    if(meanstr != null && meanstr.length() > 0) {
      mean = ParseUtil.parseDouble(meanstr);
    }
    String stddevstr = ((Element) cur).getAttribute(ATTR_STDDEV);
    if(stddevstr != null && stddevstr.length() > 0) {
      stddev = ParseUtil.parseDouble(stddevstr);
    }

    // *** New normal distribution generator
    Random random = cluster.getNewRandomGenerator();
    Distribution generator = new NormalDistribution(mean, stddev, random);
    cluster.addGenerator(generator);

    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'gamma' Element in the XML stream.
   *
   * @param cluster
   * @param cur Current document nod
   */
  private void processElementGamma(GeneratorSingleCluster cluster, Node cur) {
    double k = 1.0;
    double theta = 1.0;
    String kstr = ((Element) cur).getAttribute(ATTR_K);
    if(kstr != null && kstr.length() > 0) {
      k = ParseUtil.parseDouble(kstr);
    }
    String thetastr = ((Element) cur).getAttribute(ATTR_THETA);
    if(thetastr != null && thetastr.length() > 0) {
      theta = ParseUtil.parseDouble(thetastr);
    }

    // *** New normal distribution generator
    Random random = cluster.getNewRandomGenerator();
    Distribution generator = new GammaDistribution(k, theta, random);
    cluster.addGenerator(generator);

    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'halton' Element in the XML stream.
   *
   * @param cluster
   * @param cur Current document nod
   */
  private void processElementHalton(GeneratorSingleCluster cluster, Node cur) {
    double min = 0.0;
    double max = 1.0;

    String minstr = ((Element) cur).getAttribute(ATTR_MIN);
    if(minstr != null && minstr.length() > 0) {
      min = ParseUtil.parseDouble(minstr);
    }
    String maxstr = ((Element) cur).getAttribute(ATTR_MAX);
    if(maxstr != null && maxstr.length() > 0) {
      max = ParseUtil.parseDouble(maxstr);
    }

    // *** new uniform generator
    Random random = cluster.getNewRandomGenerator();
    Distribution generator = new HaltonUniformDistribution(min, max, random);
    cluster.addGenerator(generator);

    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'rotate' Element in the XML stream.
   *
   * @param cluster
   * @param cur Current document nod
   */
  private void processElementRotate(GeneratorSingleCluster cluster, Node cur) {
    int axis1 = 0;
    int axis2 = 0;
    double angle = 0.0;

    String a1str = ((Element) cur).getAttribute(ATTR_AXIS1);
    if(a1str != null && a1str.length() > 0) {
      axis1 = ParseUtil.parseIntBase10(a1str);
    }
    String a2str = ((Element) cur).getAttribute(ATTR_AXIS2);
    if(a2str != null && a2str.length() > 0) {
      axis2 = ParseUtil.parseIntBase10(a2str);
    }
    String anstr = ((Element) cur).getAttribute(ATTR_ANGLE);
    if(anstr != null && anstr.length() > 0) {
      angle = ParseUtil.parseDouble(anstr);
    }
    if(axis1 <= 0 || axis1 > cluster.getDim()) {
      throw new AbortException("Invalid axis1 number given in specification file.");
    }
    if(axis2 <= 0 || axis2 > cluster.getDim()) {
      throw new AbortException("Invalid axis2 number given in specification file.");
    }
    if(axis1 == axis2) {
      throw new AbortException("Invalid axis numbers given in specification file.");
    }

    // Add rotation to cluster.
    cluster.addRotation(axis1 - 1, axis2 - 1, Math.toRadians(angle));

    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'translate' Element in the XML stream.
   *
   * @param cluster
   * @param cur Current document nod
   */
  private void processElementTranslate(GeneratorSingleCluster cluster, Node cur) {
    double[] offset = null;
    String vstr = ((Element) cur).getAttribute(ATTR_VECTOR);
    if(vstr != null && vstr.length() > 0) {
      offset = parseVector(vstr);
    }
    if(offset == null) {
      throw new AbortException("No translation vector given.");
    }

    // *** add new translation
    cluster.addTranslation(offset);

    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'clipping' Element in the XML stream.
   *
   * @param cluster
   * @param cur Current document nod
   */
  private void processElementClipping(GeneratorSingleCluster cluster, Node cur) {
    double[] cmin = null, cmax = null;

    String minstr = ((Element) cur).getAttribute(ATTR_MIN);
    if(minstr != null && minstr.length() > 0) {
      cmin = parseVector(minstr);
    }
    String maxstr = ((Element) cur).getAttribute(ATTR_MAX);
    if(maxstr != null && maxstr.length() > 0) {
      cmax = parseVector(maxstr);
    }
    if(cmin == null || cmax == null) {
      throw new AbortException("No or incomplete clipping vectors given.");
    }

    // *** set clipping
    cluster.setClipping(cmin, cmax);

    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Process a 'static' cluster Element in the XML stream.
   *
   * @param gen Generator
   * @param cur Current document nod
   */
  private void processElementStatic(GeneratorMain gen, Node cur) {
    String name = ((Element) cur).getAttribute(ATTR_NAME);
    if(name == null) {
      throw new AbortException("No cluster name given in specification file.");
    }

    ArrayList<double[]> points = new ArrayList<>();
    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(TAG_POINT.equals(child.getNodeName())) {
        processElementPoint(points, child);
      }
      else if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
    // *** add new cluster object
    GeneratorStatic cluster = new GeneratorStatic(name, points);

    gen.addCluster(cluster);
    if(LOG.isVerbose()) {
      LOG.verbose("Loaded cluster " + cluster.name + " from specification.");
    }
  }

  /**
   * Parse a 'point' element (point vector for a static cluster)
   *
   * @param points current list of points (to append to)
   * @param cur Current document nod
   */
  private void processElementPoint(List<double[]> points, Node cur) {
    double[] point = null;
    String vstr = ((Element) cur).getAttribute(ATTR_VECTOR);
    if(vstr != null && vstr.length() > 0) {
      point = parseVector(vstr);
    }
    if(point == null) {
      throw new AbortException("No translation vector given.");
    }

    // *** add new point
    points.add(point);

    // TODO: check for unknown attributes.
    XMLNodeIterator iter = new XMLNodeIterator(cur.getFirstChild());
    while(iter.hasNext()) {
      Node child = iter.next();
      if(child.getNodeType() == Node.ELEMENT_NODE) {
        LOG.warning("Unknown element in XML specification file: " + child.getNodeName());
      }
    }
  }

  /**
   * Parse a string into a vector.
   *
   * TODO: move this into utility package?
   *
   * @param s String to parse
   * @return Vector
   */
  private double[] parseVector(String s) {
    String[] entries = WHITESPACE_PATTERN.split(s);
    double[] d = new double[entries.length];
    for(int i = 0; i < entries.length; i++) {
      try {
        d[i] = ParseUtil.parseDouble(entries[i]);
      }
      catch(NumberFormatException e) {
        throw new AbortException("Could not parse vector.");
      }
    }
    return d;
  }

  @Override
  protected Logging getLogger() {
    return LOG;
  }

  /**
   * Parameterization class.
   *
   * @author Erich Schubert
   */
  public static class Parameterizer extends AbstractDatabaseConnection.Parameterizer {
    /**
     * Parameter to give the configuration file
     */
    public static final OptionID CONFIGFILE_ID = new OptionID("bymodel.spec", "The generator specification file.");

    /**
     * Parameter to give the configuration file
     */
    public static final OptionID SIZE_SCALE_ID = new OptionID("bymodel.sizescale", "Factor for scaling the specified cluster sizes.");

    /**
     * Parameter for cluster reassignment
     */
    public static final OptionID REASSIGN_ID = new OptionID("bymodel.reassign", "Pattern to specify clusters to reassign.");

    /**
     * Reassign by cluster distance
     */
    public static final OptionID REASSIGN_DISTANCE_ID = new OptionID("bymodel.reassign-bydistance", "Reassign by distance, not by density.");

    /**
     * Parameter to give the configuration file
     */
    public static final OptionID RANDOMSEED_ID = new OptionID("bymodel.randomseed", "The random generator seed.");

    /**
     * The configuration file.
     */
    File specfile = null;

    /**
     * Parameter for scaling the cluster sizes.
     */
    double sizescale = 1.;

    /**
     * Pattern for clusters to reassign.
     */
    Pattern reassign = null;

    /**
     * Reassign by distance, not density.
     */
    boolean reassignByDistance = false;

    /**
     * Random generator used for initializing cluster generators.
     */
    RandomFactory clusterRandom;

    @Override
    protected void makeOptions(Parameterization config) {
      super.makeOptions(config);
      // Specification file
      final FileParameter cfgparam = new FileParameter(CONFIGFILE_ID, FileParameter.FileType.INPUT_FILE);
      if(config.grab(cfgparam)) {
        specfile = cfgparam.getValue();
      }
      // Cluster size scaling
      final DoubleParameter scalepar = new DoubleParameter(SIZE_SCALE_ID, 1.);
      if(config.grab(scalepar)) {
        sizescale = scalepar.getValue().doubleValue();
      }
      // Reassign pattern
      final PatternParameter reassignP = new PatternParameter(REASSIGN_ID) //
      .setOptional(true);
      if(config.grab(reassignP)) {
        reassign = reassignP.getValue();
      }
      if (reassign != null) {
        final Flag bydistanceF = new Flag(REASSIGN_DISTANCE_ID);
        if (config.grab(bydistanceF)) {
          reassignByDistance = bydistanceF.isTrue();
        }
      }
      // Random generator
      final RandomParameter rndP = new RandomParameter(RANDOMSEED_ID);
      if(config.grab(rndP)) {
        clusterRandom = rndP.getValue();
      }
      super.configFilters(config);
    }

    @Override
    protected GeneratorXMLDatabaseConnection makeInstance() {
      return new GeneratorXMLDatabaseConnection(filters, specfile, sizescale, reassign, reassignByDistance, clusterRandom);
    }
  }
}
