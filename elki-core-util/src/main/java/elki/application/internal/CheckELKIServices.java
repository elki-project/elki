/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 *
 * Copyright (C) 2022
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
package elki.application.internal;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.*;
import java.nio.file.spi.FileSystemProvider;
import java.util.*;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import elki.logging.Logging;
import elki.utilities.Alias;
import elki.utilities.ELKIServiceLoader;
import elki.utilities.ELKIServiceRegistry;
import elki.utilities.exceptions.AbortException;
import elki.utilities.io.FormatUtil;

/**
 * Helper application to test the ELKI service properties files for missing
 * implementation entries, for listing available implementations in the UIs.
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
  private static final Pattern STRIP = Pattern.compile("^[\\s#]*(?:deprecated:\\s*)?(.*?)[\\s]*$");

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
    try {
      // This only lists file resources:
      Enumeration<URL> us = getClass().getClassLoader().getResources(ELKIServiceLoader.RESOURCE_PREFIX);
      while(us.hasMoreElements()) {
        URL u = us.nextElement();
        if("file".equals(u.getProtocol())) {
          props.addAll(Arrays.asList(new File(u.toURI()).list()));
        }
      }
      // Find jars on the class path:
      String classpath = System.getProperty("java.class.path");
      for(String cp : classpath.split(File.pathSeparator)) {
        try {
          Path jp = Paths.get(cp).normalize();
          if(Files.isDirectory(jp)) {
            continue; // Supposedly processed above
          }
          if(!Files.exists(jp) || !Files.isReadable(jp)) {
            if(!cp.contains("/bin/default")) { // eclipse weirdness
              LOG.warning("Path does not exist, or is not readable: " + cp);
            }
            continue;
          }
          try (JarFile jar = new JarFile(jp.toFile())) {
            Enumeration<JarEntry> entries = jar.entries();
            while(entries.hasMoreElements()) {
              Path ep = Paths.get(entries.nextElement().getName()).normalize();
              if(ep.startsWith("..")) {
                LOG.warning("Jar file contains illegal path: " + ep);
              }
              else if(ep.startsWith(ELKIServiceLoader.RESOURCE_PREFIX)) {
                props.add(Paths.get(ELKIServiceLoader.RESOURCE_PREFIX).relativize(ep).toString());
              }
              else if(ep.startsWith(ELKIServiceLoader.FILENAME_PREFIX)) {
                props.add(Paths.get(ELKIServiceLoader.FILENAME_PREFIX).relativize(ep).toString());
              }
            }
          }
          catch(IOException e) {
            LOG.warning("Error reading " + jp + ": " + e.getMessage(), e);
          }
        }
        catch(Exception e) {
          LOG.warning("Could not process class path entry " + cp + ": " + e.getMessage(), e);
        }
      }
    }
    catch(IOException | URISyntaxException e) {
      throw new AbortException("Error enumerating service folders.", e);
    }
    for(String prop : props) {
      LOG.verbose("Checking property: " + prop);
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
    List<Class<?>> impls = ELKIServiceRegistry.findAllImplementations(cls, true);
    HashSet<String> names = new HashSet<>();
    for(Class<?> c2 : impls) {
      if(!c2.isInterface() && !Modifier.isAbstract(c2.getModifiers())) {
        names.add(c2.getName());
      }
    }

    try {
      Enumeration<URL> us = getClass().getClassLoader().getResources(ELKIServiceLoader.RESOURCE_PREFIX + cls.getName());
      Matcher m = STRIP.matcher("");
      while(us.hasMoreElements()) {
        URL u = us.nextElement();
        boolean injar = "jar".equals(u.getProtocol());
        if(injar) {
          // Ensure the jar is open:
          for(FileSystemProvider provider : FileSystemProvider.installedProviders()) {
            if(provider.getScheme().equalsIgnoreCase("jar")) {
              try {
                provider.getFileSystem(u.toURI());
              }
              catch(FileSystemNotFoundException e) {
                provider.newFileSystem(u.toURI(), Collections.emptyMap());
              }
            }
          }
        }
        try (BufferedReader r = Files.newBufferedReader(Paths.get(u.toURI()))) {
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
    catch(IOException | URISyntaxException e) {
      LOG.exception(e);
    }
    if(!names.isEmpty()) {
      // format for copy & paste to properties file:
      ArrayList<String> sorted = new ArrayList<>(names);
      // TODO: sort by package, then classname
      Collections.sort(sorted);
      if(update == null) {
        StringBuilder message = new StringBuilder().append("Class ").append(prop) //
            .append(" lacks suggestions:").append(FormatUtil.NEWLINE);
        for(String remaining : sorted) {
          message.append(remaining).append(FormatUtil.NEWLINE);
        }
        LOG.warning(message.toString());
        return;
      }
      // Try to automatically update:
      try {
        Path folder = Paths.get(update, ELKIServiceLoader.FILENAME_PREFIX);
        Files.createDirectories(folder);
        Path out = folder.resolve(prop);
        if(!out.startsWith(folder) || prop.contains("..")) {
          throw new IllegalStateException("Insecure path: " + out.toString());
        }
        try (
            BufferedWriter pr = Files.newBufferedWriter(out, StandardOpenOption.APPEND)) {
          pr.append('\n'); // In case there was no linefeed at the end.
          for(String remaining : sorted) {
            pr.append(remaining).append('\n');
          }
        }
        LOG.warning("Updated service file: " + out.toString());
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
   * @param parts Splitted service line
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
        StringBuilder buf = new StringBuilder(100) //
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
    HashSet<String> aliases = new HashSet<>();
    for(int i = 1; i < parts.length; i++) {
      aliases.add(parts[i]);
    }
    StringBuilder buf = null;
    for(String a : ann.value()) {
      if(!aliases.remove(a)) {
        if(buf == null) {
          buf = new StringBuilder(100) //
              .append("Class ").append(classname) //
              .append(" in ").append(parent.getCanonicalName()) //
              .append(" is missing the following aliases:");
        }
        buf.append(' ').append(a);
      }
    }
    if(!aliases.isEmpty()) {
      buf = (buf == null ? new StringBuilder() : buf.append(FormatUtil.NEWLINE)) //
          .append("Class ").append(classname) //
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
