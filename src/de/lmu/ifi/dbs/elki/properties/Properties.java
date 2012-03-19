package de.lmu.ifi.dbs.elki.properties;

/*
 This file is part of ELKI:
 Environment for Developing KDD-Applications Supported by Index-Structures

 Copyright (C) 2012
 Ludwig-Maximilians-Universität München
 Lehr- und Forschungseinheit für Datenbanksysteme
 ELKI Development Team

 This program is free software: you can redistribute it and/or modify
 it under the terms of the GNU Affero General Public License as published by
 the Free Software Foundation, either version 3 of the License, or
 (at your option) any later version.

 This program is distributed in the hope that it will be useful,
 but WITHOUT ANY WARRANTY; without even the implied warranty of
 MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License
 along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

import java.io.File;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.Set;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.logging.LoggingConfiguration;
import de.lmu.ifi.dbs.elki.utilities.FileUtil;

/**
 * Provides management of properties.
 * 
 * @author Arthur Zimek
 */
public final class Properties {
  /**
   * Out logger
   */
  private static final Logging logger = Logging.getLogger(Properties.class);
  
  /**
   * The pattern to split for separate entries in a property string, which is a
   * &quot;,&quot;.
   */
  public static final Pattern PROPERTY_SEPARATOR = Pattern.compile(",");

  /**
   * The Properties for ELKI.
   */
  public static final Properties ELKI_PROPERTIES;

  static {
    String name = Properties.class.getPackage().getName().replace('.', File.separatorChar) + File.separatorChar + "ELKI.properties";
    ELKI_PROPERTIES = new Properties(name);
  }

  /**
   * Stores the properties as defined by a property-file.
   */
  protected final java.util.Properties properties;
  
  /**
   * Provides the properties as defined in the designated file.
   * 
   * @param filename name of a file to provide property-definitions.
   */
  private Properties(String filename) {
    LoggingConfiguration.assertConfigured();
    this.properties = new java.util.Properties();
    try {
      InputStream stream = FileUtil.openSystemFile(filename);
      properties.load(stream);
    }
    catch(FileNotFoundException e) {
      logger.warning("Unable to get properties file " + filename + ".\n");
      return;
    }
    catch(Exception e) {
      logger.warning("Error loading properties file " + filename + ".\n", e);
    }
  }

  /**
   * Provides the entries (as separated by {@link #PROPERTY_SEPARATOR
   * PROPERTY_SEPARATOR}) for a specified PropertyName.
   * 
   * @param propertyName the PropertyName of the property to retrieve
   * @return the entries (separated by {@link #PROPERTY_SEPARATOR
   *         PROPERTY_SEPARATOR}) for the specified PropertyName - if the
   *         property is undefined, the returned array is of length 0
   */
  public String[] getProperty(String propertyName) {
    String property = properties.getProperty(propertyName);
    return property == null ? new String[0] : PROPERTY_SEPARATOR.split(property);
  }
  
  /**
   * Get a collection of all property names in this file.
   * 
   * @return Property names in this file.
   */
  public Set<String> getPropertyNames() {
    return properties.stringPropertyNames();
  }
}
