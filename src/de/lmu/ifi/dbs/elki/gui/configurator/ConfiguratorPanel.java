package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.GridBagLayout;
import java.util.HashMap;
import java.util.Map;

import javax.swing.JPanel;
import javax.swing.border.SoftBevelBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.TrackParameters;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassListParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.ClassParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.FileParameter;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Flag;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * A panel that contains configurators for parameters.
 * 
 * @author Erich Schubert
 * 
 * @apiviz.landmark
 * @apiviz.has de.lmu.ifi.dbs.elki.gui.configurator.ParameterConfigurator
 */
public class ConfiguratorPanel extends JPanel implements ChangeListener {
  /**
   * Serial version
   */
  private static final long serialVersionUID = 1L;

  /**
   * Keep a map of parameter
   */
  private Map<Object, ParameterConfigurator> childconfig = new HashMap<Object, ParameterConfigurator>();

  /**
   * Child options
   */
  private java.util.Vector<ParameterConfigurator> children = new java.util.Vector<ParameterConfigurator>();

  /**
   * The event listeners for this panel.
   */
  protected EventListenerList listenerList = new EventListenerList();

  /**
   * Constructor.
   */
  public ConfiguratorPanel() {
    super(new GridBagLayout());
  }

  /**
   * Add parameter to this panel.
   * 
   * @param param Parameter to add
   * @param track Parameter tracking object
   */
  public void addParameter(Object owner, Parameter<?, ?> param, TrackParameters track) {
    this.setBorder(new SoftBevelBorder(SoftBevelBorder.LOWERED));
    ParameterConfigurator cfg = null;
    {
      Object cur = owner;
      while(cfg == null && cur != null) {
        cfg = childconfig.get(cur);
        if(cfg != null) {
          break;
        }
        cur = track.getParent(cur);
      }
    }
    if(cfg != null) {
      cfg.addParameter(owner, param, track);
      return;
    }
    else {
      cfg = makeConfigurator(param);
      cfg.addChangeListener(this);
      children.add(cfg);
    }
  }

  private ParameterConfigurator makeConfigurator(Parameter<?, ?> param) {
    if(param instanceof Flag) {
      return new FlagParameterConfigurator((Flag) param, this);
    }
    if(param instanceof ClassListParameter) {
      ParameterConfigurator cfg = new ClassListParameterConfigurator((ClassListParameter<?>) param, this);
      childconfig.put(param, cfg);
      return cfg;
    }
    if(param instanceof ClassParameter) {
      ParameterConfigurator cfg = new ClassParameterConfigurator((ClassParameter<?>) param, this);
      childconfig.put(param, cfg);
      return cfg;
    }
    if(param instanceof FileParameter) {
      return new FileParameterConfigurator((FileParameter) param, this);
    }
    return new TextParameterConfigurator(param, this);
  }

  @Override
  public void stateChanged(ChangeEvent e) {
    if(e.getSource() instanceof ParameterConfigurator) {
      // TODO: check that e is in children?
      fireValueChanged();
    }
    else {
      LoggingUtil.warning("stateChanged triggered by unknown source: " + e.getSource());
    }
  }

  public void addChangeListener(ChangeListener listener) {
    listenerList.add(ChangeListener.class, listener);
  }

  public void removeChangeListener(ChangeListener listener) {
    listenerList.remove(ChangeListener.class, listener);
  }

  protected void fireValueChanged() {
    ChangeEvent evt = new ChangeEvent(this);
    for(ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
      listener.stateChanged(evt);
    }
  }

  public void appendParameters(ListParameterization params) {
    for(ParameterConfigurator cfg : children) {
      cfg.appendParameters(params);
    }
  }

  public void clear() {
    removeAll();
    childconfig.clear();
    children.clear();
  }
}