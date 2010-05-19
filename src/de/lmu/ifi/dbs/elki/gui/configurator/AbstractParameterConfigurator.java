package de.lmu.ifi.dbs.elki.gui.configurator;

import java.awt.Color;

import javax.swing.JComponent;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.EventListenerList;

import de.lmu.ifi.dbs.elki.logging.LoggingUtil;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameterization.ListParameterization;
import de.lmu.ifi.dbs.elki.utilities.optionhandling.parameters.Parameter;

/**
 * Abstract class to produce a configurator for a particular parameter.
 * 
 * @author Erich Schubert
 * 
 * @param <T>
 */
public abstract class AbstractParameterConfigurator<T extends Parameter<?, ?>> implements ParameterConfigurator {
  /**
   * Invalid parameters are tinted red.
   */
  static final Color COLOR_MISSING = new Color(0xFF0000);

  /**
   * Default value parameters are tinted green.
   */
  static final Color COLOR_DEFAULT = new Color(0x00FF00);

  /**
   * The parameter to configure
   */
  final T param;

  /**
   * The parent container
   */
  final JComponent parent;

  /**
   * The event listeners for this parameter.
   */
  protected EventListenerList listenerList = new EventListenerList();

  /**
   * Constructor.
   * 
   * @param param Parameter
   * @param parent Parent
   */
  public AbstractParameterConfigurator(T param, JComponent parent) {
    super();
    this.param = param;
    this.parent = parent;
  }

  /**
   * Colorize a component.
   * 
   * @param component Component to colorize
   */
  protected void colorize(JComponent component) {
    Color fore = component.getForeground();
    Color back = component.getBackground();
    component.setForeground(mixForegroundColor(fore, back));
    component.setBackground(mixBackgroundColor(fore, back));
  }

  /**
   * Compute the foreground color for the parameter.
   * 
   * @param fore
   * @param back
   * @return
   */
  protected Color mixForegroundColor(Color fore, Color back) {
    if(param.isOptional()) {
      if(!param.isDefined()) {
        return blend(fore, back, 0.5);
      }
      else {
        if(param.tookDefaultValue()) {
          return blend(fore, COLOR_DEFAULT, 0.5);
        }
      }
    }
    else {
      if(!param.isDefined()) {
        return blend(fore, COLOR_MISSING, 0.5);
      }
    }
    return fore;
  }

  /**
   * Compute the background color for the parameter.
   * 
   * @param fore
   * @param back
   * @return
   */
  protected Color mixBackgroundColor(Color fore, Color back) {
    if(param.isOptional()) {
      if(!param.isDefined()) {
        return back;
      }
      else {
        if(param.tookDefaultValue()) {
          return blend(back, COLOR_DEFAULT, 0.8);
        }
      }
    }
    else {
      if(!param.isDefined()) {
        return blend(back, COLOR_MISSING, 0.8);
      }
    }
    return back;
  }

  /**
   * Blend two colors.
   * 
   * @param color1 First color to blend.
   * @param color2 Second color to blend.
   * @param ratio Blend ratio. 0.5 will give even blend, 1.0 will return color1,
   *        0.0 will return color2 and so on.
   * @return Blended color.
   */
  // TODO: use Gamma correction?
  public static Color blend(Color color1, Color color2, double ratio) {
    float r = (float) ratio;
    float ir = (float) 1.0 - r;

    float rgb1[] = color1.getColorComponents(null);
    float rgb2[] = color2.getColorComponents(null);

    final float newr = rgb1[0] * r + rgb2[0] * ir;
    final float newg = rgb1[1] * r + rgb2[1] * ir;
    final float newb = rgb1[2] * r + rgb2[2] * ir;
    Color color = new Color(newr, newg, newb);

    return color;
  }

  @Override
  public void addParameter(@SuppressWarnings("unused") Object owner, @SuppressWarnings("unused") Parameter<?, ?> param) {
    LoggingUtil.warning(this.getClass() + " does not support sub-parameters!");
  }

  @Override
  public void addChangeListener(ChangeListener listener) {
    listenerList.add(ChangeListener.class, listener);
  }

  @Override
  public void removeChangeListener(ChangeListener listener) {
    listenerList.remove(ChangeListener.class, listener);
  }

  protected void fireValueChanged() {
    // FIXME: compare with previous value?
    ChangeEvent evt = new ChangeEvent(this);
    for (ChangeListener listener : listenerList.getListeners(ChangeListener.class)) {
      listener.stateChanged(evt);
    }
  }

  @Override
  public void appendParameters(ListParameterization params) {
    Object val = getUserInput();
    if (val instanceof String && ((String)val).length() == 0) {
      val = null;
    }
    if (val != null) {
      params.addParameter(param.getOptionID(), val);
    }
  }

  public abstract Object getUserInput();
}