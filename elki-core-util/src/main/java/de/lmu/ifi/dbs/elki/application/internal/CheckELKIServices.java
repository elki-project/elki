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
package de.lmu.ifi.dbs.elki.application.internal;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.List;
import java.util.TreeSet;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.Alias;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceLoader;
import de.lmu.ifi.dbs.elki.utilities.ELKIServiceRegistry;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.io.FormatUtil;

/**
 * Helper application to test the ELKI properties file for "missing"
 * implementations.
 *
 * @author Erich Schubert
 * @since 0.2
 *
 * @assoc - - - ELKIServiceLoader
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
   * Main method.
   *
   * @param argv Command line arguments
   */
  public static void main(String[] argv) {
    String update = null;
    if(argv.length == 2 && "-update".equals(argv[0])) {
      update = argv[1];
      LOG.info("Updating service files in folder: " + update);
    }
    else if(argv.length != 0) {
      throw new AbortException("Incorrect command line parameters.");
    }
    new CheckELKIServices().checkServices(update);
  }

  /**
   * Retrieve all properties and check them.
   *
   * @param update Folder to update service files in
   */
  public void checkServices(String update) {
    TreeSet<String> props = new TreeSet<>();
    Enumeration<URL> us;
    try {
      us = getClass().getClassLoader().getResources(ELKIServiceLoader.RESOURCE_PREFIX);
    }
    catch(IOException e) {
      throw new AbortException("Error enumerating service folders.", e);
    }
    while(us.hasMoreElements()) {
      URL u = us.nextElement();
      try {
        if(("jar".equals(u.getProtocol()))) {
          JarURLConnection con = (JarURLConnection) u.openConnection();
          try (JarFile jar = con.getJarFile()) {
            Enumeration<JarEntry> entries = jar.entries();
            while(entries.hasMoreElements()) {
              String prop = entries.nextElement().getName();
              if(prop.startsWith(ELKIServiceLoader.RESOURCE_PREFIX)) {
                props.add(prop.substring(ELKIServiceLoader.RESOURCE_PREFIX.length()));
              }
              else if(prop.startsWith(ELKIServiceLoader.FILENAME_PREFIX)) {
                props.add(prop.substring(ELKIServiceLoader.FILENAME_PREFIX.length()));
              }
            }
          }
          continue;
        }
        if("file".equals(u.getProtocol())) {
          props.addAll(Arrays.asList(new File(u.toURI()).list()));
        }
      }
      catch(IOException | URISyntaxException e) {
        throw new AbortException("Error enumerating service folders.", e);
      }

    }
    for(String prop : props) {
      if(LOG.isVerbose()) {
        LOG.verbose("Checking property: " + prop);
      }
      checkService(prop, update);
    }
  }

  /**
   * Check a single service class
   *
   * @param prop Class name.
   * @param update Folder to update service files in
   */
  private void checkService(String prop, String update) {
    Class<?> cls;
    try {
      cls = Class.forName(prop);
    }
    catch(ClassNotFoundException e) {
      LOG.warning("Service file name is not a class name: " + prop);
      return;
    }
    List<Class<?>> impls = ELKIServiceRegistry.findAllImplementations(cls, false, false);
    HashSet<String> names = new HashSet<>();
    for(Class<?> c2 : impls) {
      names.add(c2.getName());
    }

    Matcher m = strip.matcher("");
    try {
      Enumeration<URL> us = getClass().getClassLoader().getResources(ELKIServiceLoader.RESOURCE_PREFIX + cls.getName());
      while(us.hasMoreElements()) {
        URL u = us.nextElement();
        boolean injar = "jar".equals(u.getProtocol());
        try (
            BufferedReader r = new BufferedReader(new InputStreamReader(u.openStream(), StandardCharsets.UTF_8))) {
          for(String line; (line = r.readLine()) != null;) {
            m.reset(line);
            if(!m.matches()) {
              LOG.warning("Line: " + line + " didn't match regexp.");
              continue;
            }
            String stripped = m.group(1);
            if(stripped.length() > 0) {
              String[] parts = stripped.split(" ");
              if(!names.remove(parts[0]) && !injar) {
                LOG.warning("Name " + parts[0] + " found for property " + prop + " but no class discovered (or listed twice).");
              }
              checkAliases(cls, parts[0], parts);
            }
          }
        }
      }
    }
    catch(IOException e) {
      LOG.exception(e);
    }
    if(!names.isEmpty()) {
      // format for copy & paste to properties file:
      ArrayList<String> sorted = new ArrayList<>(names);
      // TODO: sort by package, then classname
      Collections.sort(sorted);
      if(update == null) {
        StringBuilder message = new StringBuilder().append("Class ").append(prop)//
            .append(" lacks suggestions:").append(FormatUtil.NEWLINE);
        for(String remaining : sorted) {
          message.append("# ").append(remaining).append(FormatUtil.NEWLINE);
        }
        LOG.warning(message.toString());
        return;
      }
      // Try to automatically update:
      try {
        Files.createDirectories(Paths.get(update + File.separator + ELKIServiceLoader.FILENAME_PREFIX));
        String fname = update + File.separator + ELKIServiceLoader.FILENAME_PREFIX + prop;
        PrintStream pr = new PrintStream(new FileOutputStream(fname, true));
        pr.println(); // In case there was no linefeed at the end.
        pr.println("### Automatically appended entries:");
        for(String remaining : sorted) {
          pr.println(remaining);
        }
        pr.close();
        LOG.warning("Updated service file: " + fname);
      }
      catch(IOException e) {
        LOG.exception(e);
      }
    }
  }

  /**
   * Check if aliases are listed completely.
   *
   * @param parent Parent class
   * @param classname Class name
   * @param parts Splitted sevice line
   */
  @SuppressWarnings("unchecked")
  private void checkAliases(Class<?> parent, String classname, String[] parts) {
    Class<?> c = ELKIServiceRegistry.findImplementation((Class<Object>) parent, classname);
    if(c == null) {
      return;
    }
    Alias ann = c.getAnnotation(Alias.class);
    if(ann == null) {
      if(parts.length > 1) {
        StringBuilder buf = new StringBuilder() //
            .append("Class ").append(classname) //
            .append(" in ").append(parent.getCanonicalName()) //
            .append(" has the following extraneous aliases:");
        for(int i = 1; i < parts.length; i++) {
          buf.append(' ').append(parts[i]);
        }
        LOG.warning(buf);
      }
      return;
    }
    HashSet<String> aliases = new HashSet<String>();
    for(int i = 1; i < parts.length; i++) {
      aliases.add(parts[i]);
    }
    StringBuilder buf = null;
    for(String a : ann.value()) {
      if(!aliases.remove(a)) {
        if(buf == null) {
          buf = new StringBuilder() //
              .append("Class ").append(classname) //
              .append(" in ").append(parent.getCanonicalName()) //
              .append(" is missing the following aliases:");
        }
        buf.append(' ').append(a);
      }
    }
    if(!aliases.isEmpty()) {
      if(buf == null) {
        buf = new StringBuilder();
      }
      else {
        buf.append(FormatUtil.NEWLINE);
      }
      buf.append("Class ").append(classname) //
          .append(" in ").append(parent.getCanonicalName()) //
          .append(" has the following extraneous aliases:");
      for(String a : aliases) {
        buf.append(' ').append(a);
      }
    }
    if(buf != null) {
      LOG.warning(buf);
    }
  }
}
