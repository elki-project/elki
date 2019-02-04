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
package de.lmu.ifi.dbs.elki.visualization.style;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FileUtil;
import de.lmu.ifi.dbs.elki.utilities.io.ParseUtil;
import de.lmu.ifi.dbs.elki.visualization.colors.ColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.colors.ListBasedColorLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.lines.LineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.lines.SolidLineStyleLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.marker.MarkerLibrary;
import de.lmu.ifi.dbs.elki.visualization.style.marker.PrettyMarkers;

/**
 * Style library loading the parameters from a properties file.
 * 
 * @author Erich Schubert
 * @since 0.3
 */
// TODO: also use Caching for String values?
public class PropertiesBasedStyleLibrary implements StyleLibrary {
  /**
   * Logger
   */
  private static final Logging LOG = Logging.getLogger(PropertiesBasedStyleLibrary.class);

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
   * Property string for the line style library
   */
  public static final String PROP_LINES_LIBRARY = "lines-library";

  /**
   * Property string for the marker style library
   */
  public static final String PROP_MARKER_LIBRARY = "marker-library";

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
  private Map<String, Object> cache = new HashMap<>();

  /**
   * Line style library to use
   */
  private LineStyleLibrary linelib = null;

  /**
   * Marker library to use
   */
  private MarkerLibrary markerlib = null;

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
    } catch (FileNotFoundException e) {
      try {
        stream = FileUtil.openSystemFile(filename + DEFAULT_PROPERTIES_EXTENSION);
      } catch (FileNotFoundException e2) {
        try {
          stream = FileUtil.openSystemFile(DEFAULT_PROPERTIES_PATH + filename + DEFAULT_PROPERTIES_EXTENSION);
        } catch (FileNotFoundException e3) {
          throw new AbortException("Could not find style scheme file '" + filename + "' for scheme '" + name + "'!");
        }
      }
    }
    try {
      properties.load(stream);
    } catch (Exception e) {
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
    return cls.cast(cache.get(prefix + '.' + postfix));
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
    cache.put(prefix + '.' + postfix, data);
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
    if (ret != null) {
      // logger.debugFine("Found property: "+prefix + "." +
      // postfix+" for "+prefix);
      return ret;
    }
    int pos = prefix.length();
    while (pos > 0) {
      pos = prefix.lastIndexOf('.', pos - 1);
      if (pos <= 0) {
        break;
      }
      ret = properties.getProperty(prefix.substring(0, pos) + '.' + postfix);
      if (ret != null) {
        // logger.debugFine("Found property: "+prefix.substring(0, pos) + "." +
        // postfix+" for "+prefix);
        return ret;
      }
    }
    ret = properties.getProperty(postfix);
    if (ret != null) {
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
    if (cl == null) {
      String[] colors = getPropertyValue(key, COLORSET).split(LIST_SEPARATOR);
      cl = new ListBasedColorLibrary(colors, "Default");
      setCached(key, COLORSET, cl);
    }
    return cl;
  }

  @Override
  public double getLineWidth(String key) {
    Double lw = getCached(key, LINE_WIDTH, Double.class);
    if (lw == null) {
      try {
        lw = Double.valueOf(ParseUtil.parseDouble(getPropertyValue(key, LINE_WIDTH)) * SCALE);
      } catch (NullPointerException e) {
        throw new AbortException("Missing/invalid value in style library: " + key + '.' + LINE_WIDTH);
      }
    }
    return lw.doubleValue();
  }

  @Override
  public double getTextSize(String key) {
    Double lw = getCached(key, TEXT_SIZE, Double.class);
    if (lw == null) {
      try {
        lw = Double.valueOf(ParseUtil.parseDouble(getPropertyValue(key, TEXT_SIZE)) * SCALE);
      } catch (NullPointerException e) {
        throw new AbortException("Missing/invalid value in style library: " + key + '.' + TEXT_SIZE);
      }
    }
    return lw.doubleValue();
  }

  @Override
  public String getFontFamily(String key) {
    return getPropertyValue(key, FONT_FAMILY);
  }

  @Override
  public double getSize(String key) {
    Double lw = getCached(key, GENERIC_SIZE, Double.class);
    if (lw == null) {
      try {
        lw = Double.valueOf(ParseUtil.parseDouble(getPropertyValue(key, GENERIC_SIZE)) * SCALE);
      } catch (NullPointerException e) {
        throw new AbortException("Missing/invalid value in style library: " + key + '.' + GENERIC_SIZE);
      }
    }
    return lw.doubleValue();
  }

  @Override
  public double getOpacity(String key) {
    Double lw = getCached(key, OPACITY, Double.class);
    if (lw == null) {
      try {
        lw = Double.valueOf(ParseUtil.parseDouble(getPropertyValue(key, OPACITY)));
      } catch (NullPointerException e) {
        throw new AbortException("Missing/invalid value in style library: " + key + '.' + OPACITY);
      }
    }
    return lw.doubleValue();
  }

  @Override
  public LineStyleLibrary lines() {
    if (linelib == null) {
      String libname = properties.getProperty(PROP_LINES_LIBRARY, SolidLineStyleLibrary.class.getName());
      try {
        Class<?> cls;
        try {
          cls = Class.forName(libname);
        } catch (ClassNotFoundException e) {
          cls = Class.forName(LineStyleLibrary.class.getPackage().getName() + '.' + libname);
        }
        linelib = (LineStyleLibrary) cls.getConstructor(StyleLibrary.class).newInstance(this);
      } catch (Exception e) {
        LOG.exception(e);
        linelib = new SolidLineStyleLibrary(this);
      }
    }
    return linelib;
  }

  @Override
  public MarkerLibrary markers() {
    if (markerlib == null) {
      String libname = properties.getProperty(PROP_MARKER_LIBRARY, PrettyMarkers.class.getName());
      try {
        Class<?> cls;
        try {
          cls = Class.forName(libname);
        } catch (ClassNotFoundException e) {
          cls = Class.forName(MarkerLibrary.class.getPackage().getName() + '.' + libname);
        }
        markerlib = (MarkerLibrary) cls.getConstructor(StyleLibrary.class).newInstance(this);
      } catch (Exception e) {
        LOG.exception(e);
        markerlib = new PrettyMarkers(this);
      }
    }
    return markerlib;
  }
}
