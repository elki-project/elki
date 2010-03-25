package de.lmu.ifi.dbs.elki.visualization.style;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Properties;

import de.lmu.ifi.dbs.elki.algorithm.AbortException;
import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.AnyMap;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.ListBasedColorLibrary;

/**
 * Style library loading the parameters from a properties file.
 * 
 * @author Erich Schubert
 */
// TODO: also use Caching for String values?
public class PropertiesBasedStyleLibrary implements StyleLibrary {
  /**
   * Logger
   */
  protected static final Logging logger = Logging.getLogger(PropertiesBasedStyleLibrary.class);

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
  private static final String DEFAULT_PROPERTIES_PATH = PropertiesBasedStyleLibrary.class.getPackage().getName().replace('.', File.separatorChar) + File.separatorChar;

  /**
   * Separator for lists.
   */
  public static final String LIST_SEPARATOR = ",";

  /**
   * Properties file to use.
   */
  private Properties properties;

  /**
   * Style scheme name
   */
  private String name;

  /**
   * Cache
   */
  private AnyMap<String> cache = new AnyMap<String>();

  /**
   * Constructor without a properties file name.
   */
  public PropertiesBasedStyleLibrary() {
    this(DEFAULT_SCHEME_FILENAME, DEFAULT_SCHEME_NAME);
  }

  /**
   * Constructor with a given file name.
   * 
   * @param filename Name of properties file.
   * @param name NAme for this style
   */
  public PropertiesBasedStyleLibrary(String filename, String name) {
    this.properties = new Properties();
    this.name = name;
    InputStream stream = null;
    try {
      stream = FileUtil.openSystemFile(filename);
    }
    catch(FileNotFoundException e) {
      try {
        stream = FileUtil.openSystemFile(filename + DEFAULT_PROPERTIES_EXTENSION);
      }
      catch(FileNotFoundException e2) {
        try {
          stream = FileUtil.openSystemFile(DEFAULT_PROPERTIES_PATH + filename + DEFAULT_PROPERTIES_EXTENSION);
        }
        catch(FileNotFoundException e3) {
          throw new AbortException("Could not find style scheme file '" + filename + "' for scheme '" + name + "'!");
        }
      }
    }
    try {
      properties.load(stream);
    }
    catch(Exception e) {
      throw new AbortException("Error loading properties file " + filename + ".\n", e);
    }
  }

  /**
   * Get the style scheme name.
   * 
   * @return the name
   */
  protected String getName() {
    return name;
  }

  /**
   * Get a value from the cache (to avoid repeated parsing)
   * 
   * @param <T> Type
   * @param prefix Tree name
   * @param postfix Property name
   * @param cls Class restriction
   * @return Resulting value
   */
  private <T> T getCached(String prefix, String postfix, Class<T> cls) {
    return cache.get(prefix + "." + postfix, cls);
  }

  /**
   * Set a cache value
   * 
   * @param <T> Type
   * @param prefix Tree name
   * @param postfix Property name
   * @param data Data
   */
  private <T> void setCached(String prefix, String postfix, T data) {
    cache.put(prefix + "." + postfix, data);
  }

  /**
   * Retrieve the property value for a particular path + type pair.
   * 
   * @param prefix Path
   * @param postfix Type
   * @return Value
   */
  protected String getPropertyValue(String prefix, String postfix) {
    String ret = properties.getProperty(prefix + "." + postfix);
    if(ret != null) {
      // logger.debugFine("Found property: "+prefix + "." +
      // postfix+" for "+prefix);
      return ret;
    }
    int pos = prefix.length();
    while(pos > 0) {
      pos = prefix.lastIndexOf(".", pos) - 1;
      if(pos < 0) {
        break;
      }
      ret = properties.getProperty(prefix.substring(0, pos) + "." + postfix);
      if(ret != null) {
        // logger.debugFine("Found property: "+prefix.substring(0, pos) + "." +
        // postfix+" for "+prefix);
        return ret;
      }
    }
    ret = properties.getProperty(postfix);
    if(ret != null) {
      // logger.debugFine("Found property: "+postfix+" for "+prefix);
      return ret;
    }
    return null;
  }

  @Override
  public String getColor(String key) {
    return getPropertyValue(key, COLOR);
  }

  @Override
  public String getBackgroundColor(String key) {
    return getPropertyValue(key, BACKGROUND_COLOR);
  }

  @Override
  public String getTextColor(String key) {
    return getPropertyValue(key, TEXT_COLOR);
  }

  @Override
  public ColorLibrary getColorSet(String key) {
    ColorLibrary cl = getCached(key, COLORSET, ColorLibrary.class);
    if(cl == null) {
      String[] colors = getPropertyValue(key, COLORSET).split(LIST_SEPARATOR);
      cl = new ListBasedColorLibrary(colors, "Default");
      setCached(key, COLORSET, cl);
    }
    return cl;
  }

  @Override
  public double getLineWidth(String key) {
    Double lw = getCached(key, LINE_WIDTH, Double.class);
    if(lw == null) {
      lw = Double.parseDouble(getPropertyValue(key, LINE_WIDTH)) * SCALE;
    }
    return lw;
  }

  @Override
  public double getTextSize(String key) {
    Double lw = getCached(key, TEXT_SIZE, Double.class);
    if(lw == null) {
      lw = Double.parseDouble(getPropertyValue(key, TEXT_SIZE)) * SCALE;
    }
    return lw;
  }

  @Override
  public String getFontFamily(String key) {
    return getPropertyValue(key, FONT_FAMILY);
  }

  @Override
  public double getSize(String key) {
    Double lw = getCached(key, GENERIC_SIZE, Double.class);
    if(lw == null) {
      lw = Double.parseDouble(getPropertyValue(key, GENERIC_SIZE)) * SCALE;
    }
    return lw;
  }
}