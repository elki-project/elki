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

import java.lang.reflect.InvocationTargetException;

import de.lmu.ifi.dbs.elki.logging.Logging;
import de.lmu.ifi.dbs.elki.utilities.exceptions.AbortException;
import de.lmu.ifi.dbs.elki.utilities.exceptions.ClassInstantiationException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.AbstractParameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.ParameterException;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.Parameterizer;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.Parameterization;

/**
 * Utilities for handling class instantiation, especially with respect to Java
 * generics.
 * <p>
 * Due to the way generics are implemented - via erasure - type safety cannot be
 * guaranteed properly at compile time here. These classes collect such cases
 * using helper functions, so that we have to suppress these warnings only in
 * one place.
 * <p>
 * Note that many of these situations are still type safe, i.e. an <i>empty</i>
 * array of {@code List<List<?>>} can indeed be cast into a
 * {@code List<List<Whatever>>}.
 * <p>
 * The only one potentially unsafe is {@link #instantiateGenerics}, since we
 * can't verify that the runtime type 'type' adhers to the compile time
 * restriction T. When T is not generic, such a check is possible, and then the
 * developer should use {@link #instantiate} instead.
 *
 * @author Erich Schubert
 * @since 0.2
 */
public final class ClassGenericsUtil {
  /**
   * Static logger to use.
   */
  private static final Logging LOG = Logging.getLogger(ClassGenericsUtil.class);

  /**
   * Class loader.
   */
  static final ClassLoader CLASSLOADER = ClassGenericsUtil.class.getClassLoader();

  /**
   * Name for a static "parameterize" factory method.
   */
  public static final String FACTORY_METHOD_NAME = "parameterize";

  /**
   * Fake Constructor. Use static methods.
   */
  private ClassGenericsUtil() {
    // Do not instantiate
  }

  /**
   * Returns a new instance of the given type for the specified className.
   * <p>
   * If the Class for className is not found, the instantiation is tried using
   * the package of the given type as package of the given className.
   *
   * @param <T> Class type for compile time type checking
   * @param type desired Class type of the Object to retrieve
   * @param className name of the class to instantiate
   * @return a new instance of the given type for the specified className
   * @throws ClassInstantiationException When a class cannot be instantiated.
   */
  public static <T> T instantiate(Class<T> type, String className) throws ClassInstantiationException {
    try {
      try {
        return type.cast(CLASSLOADER.loadClass(className).newInstance());
      }
      catch(ClassNotFoundException e) {
        // try package of type
        return type.cast(CLASSLOADER.loadClass(type.getPackage().getName() + "." + className).newInstance());
      }
    }
    catch(InstantiationException | IllegalAccessException
        | ClassNotFoundException | ClassCastException e) {
      throw new ClassInstantiationException(e);
    }
  }

  /**
   * Returns a new instance of the given type for the specified className.
   * <p>
   * If the class for className is not found, the instantiation is tried using
   * the package of the given type as package of the given className.
   * <p>
   * This is a weaker type checked version of "{@link #instantiate}" for use
   * with generics.
   *
   * @param <T> Class type for compile time type checking
   * @param type desired Class type of the Object to retrieve
   * @param className name of the class to instantiate
   * @return a new instance of the given type for the specified className
   * @throws ClassInstantiationException When a class cannot be instantiated.
   */
  @SuppressWarnings("unchecked")
  public static <T> T instantiateGenerics(Class<?> type, String className) throws ClassInstantiationException {
    // TODO: can we do a verification that type conforms to T somehow?
    // (probably not because generics are implemented via erasure.
    try {
      try {
        return ((Class<T>) type).cast(CLASSLOADER.loadClass(className).newInstance());
      }
      catch(ClassNotFoundException e) {
        // try package of type
        return ((Class<T>) type).cast(CLASSLOADER.loadClass(type.getPackage().getName() + "." + className).newInstance());
      }
    }
    catch(InstantiationException | IllegalAccessException
        | ClassNotFoundException | ClassCastException e) {
      throw new ClassInstantiationException(e);
    }
  }

  /**
   * Get a parameterizer for the given class.
   *
   * @param c Class
   * @return Parameterizer or null.
   */
  public static Parameterizer getParameterizer(Class<?> c) {
    for(Class<?> inner : c.getDeclaredClasses()) {
      if(Parameterizer.class.isAssignableFrom(inner)) {
        try {
          return inner.asSubclass(Parameterizer.class).newInstance();
        }
        catch(Exception e) {
          LOG.warning("Non-usable Parameterizer in class: " + c.getName());
        }
      }
    }
    return null;
  }

  /**
   * Instantiate a parameterizable class. When using this, consider using
   * {@link Parameterization#descend}!
   *
   * @param <C> base type
   * @param r Base (restriction) class
   * @param c Class to instantiate
   * @param config Configuration to use for instantiation.
   * @return Instance
   * @throws ClassInstantiationException When a class cannot be instantiated.
   */
  public static <C> C tryInstantiate(Class<C> r, Class<?> c, Parameterization config) throws ClassInstantiationException {
    if(c == null) {
      throw new ClassInstantiationException("Trying to instantiate 'null' class!");
    }
    try {
      // Try a V3 parameterization class
      Parameterizer par = getParameterizer(c);
      if(par instanceof AbstractParameterizer) {
        return r.cast(((AbstractParameterizer) par).make(config));
      }
      // Try a default constructor.
      return r.cast(c.getConstructor().newInstance());
    }
    catch(InstantiationException | InvocationTargetException
        | IllegalAccessException | NoSuchMethodException e) {
      throw new ClassInstantiationException(e);
    }
  }

  /**
   * Force parameterization method.
   * <p>
   * Please use this only in "runner" classes such as unit tests, since the
   * error handling is not very flexible.
   *
   * @param <C> Type
   * @param c Class to instantiate
   * @param config Parameters
   * @return Instance or throw an AbortException
   */
  @SuppressWarnings("unchecked")
  public static <C> C parameterizeOrAbort(Class<?> c, Parameterization config) {
    try {
      C ret = tryInstantiate((Class<C>) c, c, config);
      if(ret == null) {
        throw new AbortException("Could not instantiate class. Check parameters.");
      }
      return ret;
    }
    catch(Exception e) {
      if(config.hasErrors()) {
        for(ParameterException err : config.getErrors()) {
          LOG.warning(err.toString());
        }
      }
      throw e instanceof AbortException ? (AbortException) e : new AbortException("Instantiation failed", e);
    }
  }

  /**
   * Cast the (erased) generics onto a class.
   * <p>
   * Note: this function is a hack - notice that it would allow you to up-cast
   * any class! Still it is preferable to have this cast in one place than in
   * dozens without any explanation.
   * <p>
   * The reason this is needed is the following: There is no
   * <code>Class&lt;Set&lt;String&gt;&gt;.class</code>.
   * This method allows you to do
   * <code>Class&lt;Set&lt;String&gt;&gt; setclass = uglyCastIntoSubclass(Set.class);</code>
   * <p>
   * We can't type check at runtime, since we don't have T.
   *
   * @param cls Class type
   * @param <D> Base type
   * @param <T> Supertype
   * @return {@code cls} parameter, but cast to {@code Class<T>}
   */
  @SuppressWarnings("unchecked")
  public static <D, T extends D> Class<T> uglyCastIntoSubclass(Class<D> cls) {
    return (Class<T>) cls;
  }

  /**
   * This class performs an ugly cast, from <code>Class&lt;F&gt;</code> to
   * <code>Class&lt;T&gt;</code>, where both F and T need to extend B.
   * <p>
   * The restrictions are there to avoid misuse of this cast helper.
   * <p>
   * While this sounds really ugly, the common use case will be something like
   *
   * <pre>
   * BASE = Class&lt;Database&gt;
   * FROM = Class&lt;Database&gt;
   * TO = Class&lt;Database&lt;V&gt;&gt;
   * </pre>
   *
   * i.e. the main goal is to add missing Generics to the compile time type.
   *
   * @param <BASE> Base type
   * @param <TO> Destination type
   * @param <FROM> Source type
   * @param cls Class to be cast
   * @param base Base class for type checking.
   * @return Casted class.
   */
  @SuppressWarnings("unchecked")
  public static <BASE, FROM extends BASE, TO extends BASE> Class<TO> uglyCrossCast(Class<FROM> cls, Class<BASE> base) {
    if(!base.isAssignableFrom(cls)) {
      throw cls == null ? new ClassCastException("Attempted to use 'null' as class.") : new ClassCastException(cls.getName() + " is not a superclass of " + base);
    }
    return (Class<TO>) cls;
  }

  /**
   * Instantiate the first available implementation of an interface.
   * <p>
   * Only supports public and parameterless constructors.
   *
   * @param clz Class to instantiate.
   * @return Instance
   */
  public static <T> T instantiateLowlevel(Class<? extends T> clz) {
    Exception last = null;
    for(Class<?> c : ELKIServiceRegistry.findAllImplementations(clz)) {
      try {
        return clz.cast(c.newInstance());
      }
      catch(Exception e) {
        last = e;
      }
    }
    throw new AbortException("Cannot find a usable implementation of " + clz.toString(), last);
  }
}
