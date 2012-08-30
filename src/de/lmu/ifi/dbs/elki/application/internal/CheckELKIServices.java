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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
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
  private static final Logging LOG = Logging.getLogger(CheckELKIServices.class);

  /**
   * Pattern to strip comments, while keeping commented class names.
   */
  private Pattern strip = Pattern.compile("^[\\s#]*(?:deprecated:\\s*)?(.*?)[\\s]*$");

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
    boolean update = false, noskip = false;
    for(String arg : argv) {
      if("-update".equals(arg)) {
        update = true;
      }
      if("-all".equals(arg)) {
        noskip = true;
      }
    }
    new CheckELKIServices().checkServices(update, noskip);
  }

  /**
   * Retrieve all properties and check them.
   * 
   * @param update Flag to enable automatic updating
   * @param noskip Override filters, include all (in particular,
   *        experimentalcode)
   */
  public void checkServices(boolean update, boolean noskip) {
    URL u = getClass().getClassLoader().getResource(ELKIServiceLoader.PREFIX);
    try {
      for(String prop : new File(u.toURI()).list()) {
        if(".svn".equals(prop)) {
          continue;
        }
        if(LOG.isVerbose()) {
          LOG.verbose("Checking property: " + prop);
        }
        checkService(prop, update, noskip);
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
   * @param update Flag to enable automatic updating
   * @param noskip Override filters, include all (in particular,
   *        experimentalcode)
   */
  private void checkService(String prop, boolean update, boolean noskip) {
    Class<?> cls;
    try {
      cls = Class.forName(prop);
    }
    catch(ClassNotFoundException e) {
      LOG.warning("Property is not a class name: " + prop);
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
      if(!noskip && skip) {
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
              LOG.warning("Name " + stripped + " found for property " + prop + " but no class discovered (or referenced twice?).");
            }
          }
        }
        else {
          LOG.warning("Line: " + line + " didn't match regexp.");
        }
      }
    }
    catch(IOException e) {
      LOG.exception(e);
    }
    if(names.size() > 0) {
      // format for copy & paste to properties file:
      ArrayList<String> sorted = new ArrayList<String>(names);
      // TODO: sort by package, then classname
      Collections.sort(sorted);
      if(!update) {
        StringBuffer message = new StringBuffer();
        message.append("Class ").append(prop).append(" lacks suggestions:").append(FormatUtil.NEWLINE);
        for(String remaining : sorted) {
          message.append("# ").append(remaining).append(FormatUtil.NEWLINE);
        }
        LOG.warning(message.toString());
      }
      else {
        // Try to automatically update:
        URL f = getClass().getClassLoader().getResource(ELKIServiceLoader.PREFIX + cls.getName());
        String fnam = f.getFile();
        if(fnam == null) {
          LOG.warning("Cannot update: " + f + " seems to be in a jar file.");
        }
        else {
          try {
            FileOutputStream out = new FileOutputStream(fnam, true);
            PrintStream pr = new PrintStream(out);
            pr.println("# Automatically appended entries:");
            for(String remaining : sorted) {
              pr.println(remaining);
            }
            pr.flush();
            pr.close();
            out.flush();
            out.close();
            LOG.warning("Updated: " + fnam);
          }
          catch(IOException e) {
            LOG.exception(e);
          }
        }
      }
    }
  }
}
