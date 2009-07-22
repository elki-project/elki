package de.lmu.ifi.dbs.elki.visualization.colors;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Color library loading the color names from a properties file.
 * 
 * @author Erich Schubert
 */
public class PropertiesBasedColorLibrary implements ColorLibrary {
  /**
   * Logger
   */
  protected static final Logging logger = Logging.getLogger(PropertiesBasedColorLibrary.class);

  /**
   * Name of the default color scheme.
   */
  public static final String DEFAULT_SCHEME_NAME = "Default";
  
  /**
   * File name of the default color scheme.
   */
  public static final String DEFAULT_SCHEME_FILENAME = "default";
  
  /**
   * File extension
   */
  public static final String DEFAULT_PROPERTIES_EXTENSION = ".properties";

  /**
   * Default properties path
   */
  private static final String DEFAULT_PROPERTIES_PATH = PropertiesBasedColorLibrary.class.getPackage().getName().replace('.', File.separatorChar)+File.separatorChar;

  /**
   * Separator for color lists.
   */
  public static final String COLOR_LIST_SEPARATOR = ",";

  /**
   * Properties file to use.
   */
  private Properties properties;

  /**
   * Array of color names.
   */
  private String[] colors;
  
  /**
   * Color scheme name
   */
  private String name;
  
  /**
   * Constructor without a properties file name.
   */
  public PropertiesBasedColorLibrary() {
    this(DEFAULT_SCHEME_FILENAME, DEFAULT_SCHEME_NAME);
  }

  /**
   * Constructor with a given file name.
   * 
   * @param filename Name of properties file.
   */
  public PropertiesBasedColorLibrary(String filename, String name) {
    this.properties = new Properties();
    this.name = name;
    InputStream stream = null;
    stream = tryOpen(filename);
    if (stream == null) {
      stream = tryOpen(filename + DEFAULT_PROPERTIES_EXTENSION);
    }
    if (stream == null) {
      stream = tryOpen(DEFAULT_PROPERTIES_PATH + filename + DEFAULT_PROPERTIES_EXTENSION);
    }
    if (stream == null) {
      throw new AbortException("Could not find color scheme file '"+filename+"' for scheme '"+name+"'!");
    }
    try {
      properties.load(stream);
    }
    catch(Exception e) {
      throw new AbortException("Error loading properties file " + filename + ".\n", e);
    }
    colors = getNamedColor(ColorLibrary.COLOR_LINE_COLORS).split(COLOR_LIST_SEPARATOR);
  }

  /**
   * Try to open a given file name
   * @param filename
   * @return
   */
  private InputStream tryOpen(String filename) {
    try {
      logger.warning("Trying "+filename);
      return new FileInputStream(filename);
    }
    catch(FileNotFoundException e) {
      // try with classloader
      String resname = filename.replace(File.separatorChar,'/');
      logger.warning("Trying "+resname);
      return ClassLoader.getSystemResourceAsStream(resname);
    }
  }

  @Override
  public String getColor(int index) {
    return colors[index % colors.length];
  }

  @Override
  public int getNumberOfNativeColors() {
    return colors.length;
  }

  @Override
  public String getNamedColor(String name) {
    return this.properties.getProperty(name);
  }

  /**
   * Get the color scheme name.
   * 
   * @return the name
   */
  protected String getName() {
    return name;
  }
}