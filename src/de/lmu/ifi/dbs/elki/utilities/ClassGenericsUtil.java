package de.lmu.ifi.dbs.elki.utilities;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * Utils for handling class instantiation especially with respect to Java generics.
 * 
 * Due to the way generics are implemented - via erasure - type safety cannot be guaranteed
 * properly at compile time here. These classes collect such cases using helper functions,
 * so that we have to suppress these warnings only in one place.
 * 
 * Note that many of these situations are still type safe, i.e. an <i>empty</i> array of
 * List<List<?>> can indeed be cast into a List<List<Whatever>>.
 * 
 * The only one potentially unsafe is {@link #instantiateGenerics}, since we can't verify
 * that the runtime type 'type' adhers to the compile time restriction T. When T is not generic,
 * such a check is possible, and then the developer should use {@link #instantiate} instead.
 *
 */
public final class ClassGenericsUtil {
  /**
   * Returns a new instance of the given type for the specified className.
   * <p/>
   * If the Class for className is not found, the instantiation is tried using
   * the package of the given type as package of the given className.
   * 
   * @param <T> Class type for compile time type checking
   * @param type desired Class type of the Object to retrieve
   * @param className name of the class to instantiate
   * @return a new instance of the given type for the specified className
   * @throws UnableToComplyException if the instantiation cannot be performed
   *         successfully
   */
  public static <T> T instantiate(Class<T> type, String className) throws UnableToComplyException {
    T instance;
    try {
      try {
        instance = type.cast(Class.forName(className).newInstance());
      }
      catch(ClassNotFoundException e) {
        // try package of type
        instance = type.cast(Class.forName(type.getPackage().getName() + "." + className).newInstance());
      }
    }
    catch(InstantiationException e) {
      throw new UnableToComplyException("InstantiationException: " + e.getMessage(), e);
    }
    catch(IllegalAccessException e) {
      throw new UnableToComplyException("IllegalAccessException: " + e.getMessage(), e);
    }
    catch(ClassNotFoundException e) {
      throw new UnableToComplyException("ClassNotFoundException: " + e.getMessage(), e);
    }
    catch(ClassCastException e) {
      throw new UnableToComplyException("ClassCastException: " + e.getMessage(), e);
    }
    return instance;
  }

  /**
   * Returns a new instance of the given type for the specified className.
   * <p/>
   * If the Class for className is not found, the instantiation is tried using
   * the package of the given type as package of the given className.
   * 
   * This is a weaker type checked version of "instantiate" for use with
   * generics.
   * 
   * @param <T> Class type for compile time type checking
   * @param type desired Class type of the Object to retrieve
   * @param className name of the class to instantiate
   * @return a new instance of the given type for the specified className
   * @throws UnableToComplyException if the instantiation cannot be performed
   *         successfully
   */
  @SuppressWarnings("unchecked")
  public static <T> T instantiateGenerics(Class<?> type, String className) throws UnableToComplyException {
    T instance;
    // TODO: can we do a verification that type conforms to T somehow?
    // (probably not because generics are implemented via erasure.
    try {
      try {
        instance = ((Class<T>) type).cast(Class.forName(className).newInstance());
      }
      catch(ClassNotFoundException e) {
        // try package of type
        instance = ((Class<T>) type).cast(Class.forName(type.getPackage().getName() + "." + className).newInstance());
      }
    }
    catch(InstantiationException e) {
      throw new UnableToComplyException("InstantiationException: " + e.getMessage(), e);
    }
    catch(IllegalAccessException e) {
      throw new UnableToComplyException("IllegalAccessException: " + e.getMessage(), e);
    }
    catch(ClassNotFoundException e) {
      throw new UnableToComplyException("ClassNotFoundException: " + e.getMessage(), e);
    }
    catch(ClassCastException e) {
      throw new UnableToComplyException("ClassCastException: " + e.getMessage(), e);
    }
    return instance;
  }

  /**
   * Create an array of lists. This is a common cast we have to do due to Java
   * generics limitations.
   * 
   * @param <T> Type the list elements have
   * @param len array size
   * @return new array of lists
   */
  @SuppressWarnings("unchecked")
  public static <T> List<T>[] newArrayOfList(int len) {
    List<?>[] result = new List<?>[len];
    return (List<T>[]) result;
  }

  /**
   * Create an array of ArrayLists. This is a common cast we have to do due to
   * Java generics limitations.
   * 
   * @param <T> Type the list elements have
   * @param len array size
   * @return new array of arraylists
   */
  @SuppressWarnings("unchecked")
  public static <T> ArrayList<T>[] newArrayOfArrayList(int len) {
    ArrayList<?>[] result = new ArrayList<?>[len];
    return (ArrayList<T>[]) result;
  }

  /**
   * Create an array of sets. This is a common cast we have to do due to Java
   * generics limitations.
   * 
   * @param <T> Type the list elements have
   * @param len array size
   * @return new array of sets
   */
  @SuppressWarnings("unchecked")
  public static <T> Set<T>[] newArrayOfSet(int len) {
    Set<?>[] result = new Set<?>[len];
    return (Set<T>[]) result;
  }

}
