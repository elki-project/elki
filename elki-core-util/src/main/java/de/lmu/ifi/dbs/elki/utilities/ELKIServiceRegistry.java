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
package de.lmu.ifi.dbs.elki.utilities;

import java.lang.reflect.Modifier;
import java.util.*;

import de.lmu.ifi.dbs.elki.logging.Logging;

/**
 * Registry of available implementations in ELKI.
 *
 * @author Erich Schubert
 * @since 0.7.0
 */
public class ELKIServiceRegistry {
  /**
   * Class logger.
   */
  private static final Logging LOG = Logging.getLogger(ELKIServiceRegistry.class);

  /**
   * Class loader
   */
  private static final ClassLoader CLASSLOADER = ELKIServiceRegistry.class.getClassLoader();

  /**
   * Factory class postfix.
   */
  public static final String FACTORY_POSTFIX = "$Factory";

  /**
   * Registry data.
   */
  private static Map<Class<?>, Entry> data = new HashMap<Class<?>, Entry>();

  /**
   * Value to abuse for failures.
   */
  final static Class<?> FAILED_LOAD = Entry.class;

  /**
   * Do not use constructor.
   */
  private ELKIServiceRegistry() {
    // Do not use.
  }

  /**
   * Entry in the service registry.
   *
   * @author Erich Schubert
   */
  private static class Entry {
    /**
     * Reusable empty array.
     */
    private static final String[] EMPTY_ALIASES = new String[0];

    /**
     * Class names.
     */
    private String[] names = new String[3];

    /**
     * Loaded classes.
     */
    private Class<?>[] clazzes = new Class<?>[3];

    /**
     * Length.
     */
    private int len = 0;

    /**
     * Aliases hash map.
     */
    private String[] aliases = EMPTY_ALIASES;

    /**
     * Occupied entries in aliases.
     */
    private int aliaslen = 0;

    /**
     * Add a candidate.
     *
     * @param cname Candidate name
     */
    private void addName(String cname) {
      // Grow if needed:
      if(len == names.length) {
        final int nl = (len << 1) + 1;
        names = Arrays.copyOf(names, nl);
        clazzes = Arrays.copyOf(clazzes, nl);
      }
      names[len++] = cname;
    }

    /**
     * If a name has been resolved, add it.
     *
     * @param cname Name
     * @param c Resulting class
     */
    private void addHit(String cname, Class<?> c) {
      // Grow if needed:
      if(len == names.length) {
        final int nl = (len << 1) + 1;
        names = Arrays.copyOf(names, nl);
        clazzes = Arrays.copyOf(clazzes, nl);
      }
      names[len] = cname;
      clazzes[len] = c;
      ++len;
    }

    /**
     * Register a class alias.
     *
     * @param alias Alias name
     * @param cname Class name
     */
    private void addAlias(String alias, String cname) {
      if(aliases == EMPTY_ALIASES) {
        aliases = new String[6];
      }
      if(aliaslen == aliases.length) {
        aliases = Arrays.copyOf(aliases, aliaslen << 1);
      }
      aliases[aliaslen++] = alias;
      aliases[aliaslen++] = cname;
    }
  }

  /**
   * Register a class with the registry.
   *
   * @param parent Parent class
   * @param cname Class name
   */
  protected static void register(Class<?> parent, String cname) {
    Entry e = data.get(parent);
    if(e == null) {
      data.put(parent, e = new Entry());
    }
    e.addName(cname);
  }

  /**
   * Register a class in the registry.
   *
   * Careful: do not use this from your code before first making sure this has
   * been fully initialized. Otherwise, other implementations will not be found.
   * Therefore, avoid calling this from your own Java code!
   *
   * @param parent Class
   * @param clazz Implementation
   */
  protected static void register(Class<?> parent, Class<?> clazz) {
    Entry e = data.get(parent);
    if(e == null) {
      data.put(parent, e = new Entry());
    }
    final String cname = clazz.getCanonicalName();
    e.addHit(cname, clazz);
    if(clazz.isAnnotationPresent(Alias.class)) {
      Alias aliases = clazz.getAnnotation(Alias.class);
      for(String alias : aliases.value()) {
        e.addAlias(alias, cname);
      }
    }
  }

  /**
   * Register a class alias with the registry.
   *
   * @param parent Parent class
   * @param alias Alias name
   * @param cname Class name
   */
  protected static void registerAlias(Class<?> parent, String alias, String cname) {
    Entry e = data.get(parent);
    assert (e != null);
    e.addAlias(alias, cname);
  }

  /**
   * Attempt to load a class
   *
   * @param value Class name to try.
   * @return Class, or {@code null}.
   */
  private static Class<?> tryLoadClass(String value) {
    try {
      return CLASSLOADER.loadClass(value);
    }
    catch(ClassNotFoundException e) {
      return null;
    }
  }

  /**
   * Test if a registry entry has already been created.
   *
   * @param c Class
   * @return {@code true} if a registry entry has been created.
   */
  protected static boolean contains(Class<?> c) {
    return data.containsKey(c);
  }

  /**
   * Find all implementations of a particular interface.
   *
   * @param restrictionClass Class to scan for
   * @return Found implementations
   */
  public static List<Class<?>> findAllImplementations(Class<?> restrictionClass) {
    if(restrictionClass == null) {
      return Collections.emptyList();
    }
    if(!contains(restrictionClass)) {
      ELKIServiceLoader.load(restrictionClass);
      ELKIServiceScanner.load(restrictionClass);
    }
    Entry e = data.get(restrictionClass);
    if(e == null) {
      return Collections.emptyList();
    }
    // Start loading classes:
    ArrayList<Class<?>> ret = new ArrayList<>(e.len);
    for(int pos = 0; pos < e.len; pos++) {
      Class<?> c = e.clazzes[pos];
      if(c == null) {
        c = tryLoadClass(e.names[pos]);
        if(c == null) {
          LOG.warning("Failed to load class " + e.names[pos] + " for interface " + restrictionClass.getName());
          c = FAILED_LOAD;
        }
        e.clazzes[pos] = c;
      }
      if(c == FAILED_LOAD) {
        continue;
      }
      // Linear scan, but cheap enough.
      if(!ret.contains(c)) {
        ret.add(c);
      }
    }
    return ret;
  }

  /**
   * Find all implementations of a given class in the classpath.
   *
   * Note: returned classes may be abstract.
   *
   * @param c Class restriction
   * @param everything include interfaces, abstract and private classes
   * @param parameterizable only return classes instantiable by the
   *        parameterizable API
   * @return List of found classes.
   */
  public static List<Class<?>> findAllImplementations(Class<?> c, boolean everything, boolean parameterizable) {
    if(c == null) {
      return Collections.emptyList();
    }
    // Default is served from the registry
    if(!everything && parameterizable) {
      return findAllImplementations(c);
    }
    // This codepath is used by utility classes to also find buggy
    // implementations (e.g. non-instantiable, abstract) of the interfaces.
    List<Class<?>> known = findAllImplementations(c);
    // For quickly skipping seen entries:
    HashSet<Class<?>> dupes = new HashSet<>(known);
    for(Iterator<Class<?>> iter = ELKIServiceScanner.nonindexedClasses(); iter.hasNext();) {
      Class<?> cls = iter.next();
      if(dupes.contains(cls)) {
        continue;
      }
      // skip abstract / private classes.
      if(!everything && (Modifier.isInterface(cls.getModifiers()) || Modifier.isAbstract(cls.getModifiers()) || Modifier.isPrivate(cls.getModifiers()))) {
        continue;
      }
      if(!c.isAssignableFrom(cls)) {
        continue;
      }
      if(parameterizable) {
        boolean instantiable = false;
        try {
          instantiable = cls.getConstructor() != null;
        }
        catch(Exception | Error e) {
          // ignore
        }
        try {
          instantiable = instantiable || ClassGenericsUtil.getParameterizer(cls) != null;
        }
        catch(Exception | Error e) {
          // ignore
        }
        if(!instantiable) {
          continue;
        }
      }
      known.add(cls);
      dupes.add(cls);
    }
    return known;
  }

  /**
   * Find an implementation of the given interface / super class, given a
   * relative class name or alias name.
   *
   * @param restrictionClass Restriction class
   * @param value Class name, relative class name, or nickname.
   * @return Class found or {@code null}
   */
  public static <C> Class<? extends C> findImplementation(Class<? super C> restrictionClass, String value) {
    // Add all from service files (i.e. jars)
    if(!contains(restrictionClass)) {
      ELKIServiceLoader.load(restrictionClass);
      ELKIServiceScanner.load(restrictionClass);
    }
    Entry e = data.get(restrictionClass);
    int pos = -1;
    Class<?> clazz = null;
    // First, try the lookup cache:
    if(e != null) {
      for(pos = 0; pos < e.len; pos++) {
        if(e.names[pos].equals(value)) {
          break;
        }
      }
      if(pos < e.len) {
        clazz = e.clazzes[pos];
      }
      else {
        pos = -1;
      }
    }
    else {
      if(LOG.isDebugging()) {
        LOG.debug("Requested implementations for unregistered type: " + restrictionClass.getName() + " " + value);
      }
    }
    // Next, try alternative versions:
    clazz = clazz != null ? clazz : tryAlternateNames(restrictionClass, value, e);
    if(clazz == null) {
      return null;
    }

    if(!restrictionClass.isAssignableFrom(clazz)) {
      LOG.warning("Invalid entry in service file for class " + restrictionClass.getName() + ": " + value);
      clazz = FAILED_LOAD;
    }
    if(e != null) {
      if(pos < 0) {
        e.addHit(value, clazz);
      }
      else {
        assert (e.names[pos].equalsIgnoreCase(value));
        e.clazzes[pos] = clazz;
      }
    }
    if(clazz == FAILED_LOAD) {
      return null;
    }
    @SuppressWarnings("unchecked")
    Class<? extends C> ret = (Class<? extends C>) clazz.asSubclass(restrictionClass);
    return ret;
  }

  /**
   * Try loading alternative names.
   *
   * @param restrictionClass Context class, for prepending a package name.
   * @param value Class name requested
   * @param e Cache entry, may be null
   * @param <C> Generic type
   * @return Class, or null
   */
  private static <C> Class<?> tryAlternateNames(Class<? super C> restrictionClass, String value, Entry e) {
    StringBuilder buf = new StringBuilder(value.length() + 100);
    // Try with FACTORY_POSTFIX first:
    Class<?> clazz = tryLoadClass(buf.append(value).append(FACTORY_POSTFIX).toString());
    if(clazz != null) {
      return clazz;
    }
    clazz = tryLoadClass(value); // Without FACTORY_POSTFIX.
    if(clazz != null) {
      return clazz;
    }
    buf.setLength(0);
    // Try prepending the package name:
    clazz = tryLoadClass(buf.append(restrictionClass.getPackage().getName()).append('.')//
        .append(value).append(FACTORY_POSTFIX).toString());
    if(clazz != null) {
      return clazz;
    }
    // Remove FACTORY_POSTFIX again.
    buf.setLength(buf.length() - FACTORY_POSTFIX.length());
    String value2 = buf.toString(); // Will also be used below.
    clazz = tryLoadClass(value2);
    if(clazz != null) {
      return clazz;
    }
    // Last, try aliases:
    if(e != null && e.aliaslen > 0) {
      for(int i = 0; i < e.aliaslen; i += 2) {
        if(e.aliases[i].equalsIgnoreCase(value) || e.aliases[i].equalsIgnoreCase(value2)) {
          return findImplementation(restrictionClass, e.aliases[++i]);
        }
      }
    }
    return null;
  }
}
