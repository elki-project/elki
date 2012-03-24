package de.lmu.ifi.dbs.elki.application.internal;

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

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceLoader;
import de.lmu.ifi.dbs.elki.utilities.FormatUtil;
import de.lmu.ifi.dbs.elki.utilities.InspectionUtil;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;

/**
 * Helper application to test the ELKI properties file for "missing"
 * implementations.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.uses ELKIServiceLoader
 */
public class CheckELKIServices {
  /**
   * The logger for this class.
   */
  private static final Logging logger = Logging.getLogger(CheckELKIServices.class);

  /**
   * Pattern to strip comments, while keeping commented class names.
   */
  private Pattern strip = Pattern.compile("^[\\s#]*(.*?)[\\s]*$");

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
    new CheckELKIServices().checkServices();
  }

  /**
   * Retrieve all properties and check them.
   */
  public void checkServices() {
    URL u = getClass().getClassLoader().getResource(ELKIServiceLoader.PREFIX);
    try {
      for(String prop : new File(u.toURI()).list()) {
        if (".svn".equals(prop)) {
          continue;
        }
        if (logger.isVerbose()) {
          logger.verbose("Checking property: "+prop);
        }
        checkService(prop);
      }
    }
    catch(URISyntaxException e) {
      throw new AbortException("Cannot check all properties, as some are not in a file: URL.");
    }
  }

  /**
   * Check a single service class
   * 
   * @param prop Class name.
   */
  private void checkService(String prop) {
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

    try {
      InputStream is = getClass().getClassLoader().getResource(ELKIServiceLoader.PREFIX + cls.getName()).openStream();
      BufferedReader r = new BufferedReader(new InputStreamReader(is, "utf-8"));
      for(String line;;) {
        line = r.readLine();
        // End of stream:
        if(line == null) {
          break;
        }
        Matcher m = strip.matcher(line);
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
          logger.warning("Line: " + line + " didn't match regexp.");
        }
      }
    }
    catch(IOException e) {
      logger.exception(e);
    }
    if(names.size() > 0) {
      StringBuffer message = new StringBuffer();
      message.append("Class " + prop + " lacks suggestions:" + FormatUtil.NEWLINE);
      // format for copy & paste to properties file:
      ArrayList<String> sorted = new ArrayList<String>(names);
      // TODO: sort by package, then classname
      Collections.sort(sorted);
      for(String remaining : sorted) {
        message.append("# " + remaining + FormatUtil.NEWLINE);
      }
      logger.warning(message.toString());
    }
  }
}
