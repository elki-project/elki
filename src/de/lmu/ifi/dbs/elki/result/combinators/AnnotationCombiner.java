package de.lmu.ifi.dbs.elki.result.combinators;

import java.util.ArrayList;
import java.util.Collection;

import de.lmu.ifi.dbs.elki.result.AnnotationResult;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * AnnotationCombiner is a class that combines multiple Annotation Results into
 * one.
 * 
 * @author Erich Schubert
 * 
 * @param <T>
 */
public class AnnotationCombiner<T> implements AnnotationResult<T> {
  /**
   * Results to combine
   */
  private Collection<AnnotationResult<T>> results = new ArrayList<AnnotationResult<T>>();

  /**
   * Constructor
   */
  public AnnotationCombiner() {
    this(new ArrayList<AnnotationResult<T>>());
  }

  /**
   * Constructor with a list of results to combine.
   * 
   * @param results
   */
  public AnnotationCombiner(Collection<AnnotationResult<T>> results) {
    super();
    this.results = results;
  }

  /**
   * Add an annotation result to the result list.
   * 
   * @param result
   */
  public void addAnnotationResult(AnnotationResult<T> result) {
    this.results.add(result);
  }

  /**
   * Retrieve all annotations and return them as combined array.
   */
  @Override
  public Pair<String, T>[] getAnnotations(Integer objID) {
    ArrayList<Pair<String, T>> annotations = new ArrayList<Pair<String, T>>();
    for(AnnotationResult<T> result : results) {
      if(result != null) {
        Pair<String, T>[] newannotations = result.getAnnotations(objID);
        for(Pair<String, T> newann : newannotations) {
          annotations.add(newann);
        }
      }
    }
    Pair<String, T>[] result = Pair.newArray(0);
    return annotations.toArray(result);
  }
}
