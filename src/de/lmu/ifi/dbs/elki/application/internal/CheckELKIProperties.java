package de.lmu.ifi.dbs.elki.application.internal;
/*
This file is part of ELKI:
Environment for Developing KDD-Applications Supported by Index-Structures

Copyright (C) 2011
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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.properties.Properties;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;

/**
 * Helper application to test the ELKI properties file for "missing" implementations.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses Properties
 */
public class CheckELKIProperties {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(CheckELKIProperties.class);
  
  /**
   * Pattern to strip comments, while keeping commented class names.
   */
  private Pattern strip = Pattern.compile("^[\\s#]*(.*?)[\\s,]*$");

  /**
   * Package to skip matches in - unreleased code.
   */
  private String[] skippackages = { "experimentalcode." };

  /**
   * Main method.
   * 
   * @param argv Command line arguments
   */
  public static void main(String[] argv) {
    new CheckELKIProperties().checkProperties();
  }
  
  /**
   * Retrieve all properties and check them.
   */
  public void checkProperties() {
    Set<String> props = Properties.ELKI_PROPERTIES.getPropertyNames();
    for(String prop : props) {
      checkProperty(prop);
    }
  }

  /**
   * Check a single property
   * 
   * @param prop Property = Class name.
   */
  private void checkProperty(String prop) {
    Class<?> cls;
    try {
      cls = Class.forName(prop);
    }
    catch(ClassNotFoundException e) {
      logger.warning("Property is not a class name: " + prop);
      return;
    }
    List<Class<?>> impls = InspectionUtil.findAllImplementations(cls, false);
    HashSet<String> names = new HashSet<String>();
    for(Class<?> c2 : impls) {
      boolean skip = false;
      for(String pkg : skippackages) {
        if(c2.getName().startsWith(pkg)) {
          skip = true;
          break;
        }
      }
      if(skip) {
        continue;
      }
      names.add(c2.getName());
    }

    String[] known = Properties.ELKI_PROPERTIES.getProperty(cls.getName());
    for(String k : known) {
      Matcher m = strip.matcher(k);
      if(m.matches()) {
        String stripped = m.group(1);
        if(stripped.length() > 0) {
          if(names.contains(stripped)) {
            names.remove(stripped);
          }
          else {
            logger.warning("Name " + stripped + " found for property " + prop + " but no class discovered (or referenced twice?).");
          }
        }
      }
      else {
        logger.warning("Line: " + k + " didn't match regexp.");
      }
    }
    if(names.size() > 0) {
      StringBuffer message = new StringBuffer();
      message.append("Class " + prop + " lacks suggestions:" + FormatUtil.NEWLINE);
      // format for copy & paste to properties file:
      ArrayList<String> sorted = new ArrayList<String>(names);
      // TODO: sort by package, then classname
      Collections.sort(sorted);
      for(String remaining : sorted) {
        message.append("# " + remaining + ",\\" + FormatUtil.NEWLINE);
      }
      logger.warning(message.toString());
    }
  }
}
