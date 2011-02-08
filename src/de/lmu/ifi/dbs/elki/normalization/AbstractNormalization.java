package de.lmu.ifi.dbs.elki.normalization;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.lmu.ifi.dbs.elki.data.DatabaseObject;
import de.lmu.ifi.dbs.elki.database.DatabaseObjectMetadata;
import de.lmu.ifi.dbs.elki.math.linearalgebra.LinearEquationSystem;
import de.lmu.ifi.dbs.elki.utilities.pairs.Pair;

/**
 * Abstract super class for all normalizations.
 * 
 * @author Elke Achtert
 * @param <O> object type
 */
public abstract class AbstractNormalization<O extends DatabaseObject> implements Normalization<O> {
  /**
   * Initializes the option handler and the parameter map.
   */
  protected AbstractNormalization() {
    super();
  }

  /**
   * Returns a string representation of the object.
   * 
   * @return a string representation of the object.
   */
  @Override
  public String toString() {
    return getClass().getName();
  }

  @Override
  public String toString(String pre) {
    StringBuffer result = new StringBuffer();
    result.append(pre).append("normalization class: ").append(getClass().getName());
    result.append("\n");
    return result.toString();
  }

  @Override
  public List<Pair<O, DatabaseObjectMetadata>> normalizeObjects(List<Pair<O, DatabaseObjectMetadata>> objectAndAssociationsList) throws NonNumericFeaturesException {
    if(objectAndAssociationsList.size() == 0) {
      return Collections.emptyList();
    }

    if(initNormalization()) {
      for(Pair<O, DatabaseObjectMetadata> objectAndAssociations : objectAndAssociationsList) {
        final O obj = objectAndAssociations.getFirst();
        initProcessInstance(obj);
      }
      initComplete();
    }

    List<Pair<O, DatabaseObjectMetadata>> normalized = new ArrayList<Pair<O, DatabaseObjectMetadata>>(objectAndAssociationsList.size());
    for(Pair<O, DatabaseObjectMetadata> objectAndAssociations : objectAndAssociationsList) {
      final O obj = objectAndAssociations.getFirst();
      O normalizedObj = normalize(obj);
      // Keep ID an associations
      normalizedObj.setID(obj.getID());
      DatabaseObjectMetadata associations = objectAndAssociations.getSecond();
      normalized.add(new Pair<O, DatabaseObjectMetadata>(normalizedObj, associations));
    }
    return normalized;
  }

  @Override
  public List<O> normalize(List<O> objs) throws NonNumericFeaturesException {
    if(objs.size() == 0) {
      return Collections.emptyList();
    }

    if(initNormalization()) {
      for(O obj : objs) {
        initProcessInstance(obj);
      }
      initComplete();
    }

    List<O> normalized = new ArrayList<O>(objs.size());
    for(O obj : objs) {
      O normalizedObj = normalize(obj);
      // Keep the ID
      normalizedObj.setID(obj.getID());
      normalized.add(normalizedObj);
    }
    return normalized;
  }

  /**
   * Normalize a single instance.
   * 
   * You can implement this as UnsupportedOperationException if you override
   * both public "normalize" functions!
   * 
   * @param obj Database object to normalize
   * @return Normalized database object
   */
  abstract protected O normalize(O obj) throws NonNumericFeaturesException;

  /**
   * Return "true" when the normalization needs initialization
   * 
   * @return true or false
   */
  protected boolean initNormalization() {
    return false;
  }

  /**
   * Process a single object during initialization.
   * 
   * @param obj Object to process
   */
  abstract protected void initProcessInstance(O obj);

  /**
   * Complete the initialization phase
   */
  abstract protected void initComplete();

  @Override
  public List<O> restore(List<O> objs) throws NonNumericFeaturesException {
    List<O> restored = new ArrayList<O>();
    for(O obj : objs) {
      final O restObj = restore(obj);
      // Keep the ID
      restObj.setID(obj.getID());
      restored.add(restObj);
    }
    return restored;
  }

  @SuppressWarnings("unused")
  @Override
  public LinearEquationSystem transform(LinearEquationSystem linearEquationSystem) {
    // FIXME: implement.
    throw new UnsupportedOperationException("Not yet implemented!");
  }
}