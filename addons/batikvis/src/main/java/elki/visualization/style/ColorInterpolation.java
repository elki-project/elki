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
package elki.visualization.style;

import java.awt.Color;

import static elki.visualization.svg.SVGUtil.colorToString;
import static elki.visualization.svg.SVGUtil.stringToColor;

/**
 * Color interpolation
 * 
 * @author Robert Gehde
 * @since 0.8.0
 */
public enum ColorInterpolation {
  /**
   * ColorInterpolation in RGB space
   */
  RGB {
    @Override
    public String interpolate(String color, String neutral, double intensity) {
      if(intensity <= 0 || intensity >= 1) {
        return intensity >= 1 ? color : neutral;
      }
      Color c = stringToColor(color), n = stringToColor(neutral);
      int r = (int) (c.getRed() * intensity + (1 - intensity) * n.getRed());
      int g = (int) (c.getGreen() * intensity + (1 - intensity) * n.getGreen());
      int b = (int) (c.getBlue() * intensity + (1 - intensity) * n.getBlue());
      return colorToString((r << 16) | (g << 8) | b);
    }
  },
  /**
   * ColorInterpolation in HSV space.
   * <p>
   * note: this changes the tone linear as well, if you use a black color,
   * black is coded as (0,0,x). In that case it will change the color.
   */
  HSV {
    @Override
    public String interpolate(String color, String neutral, double intensity) {
      if(intensity <= 0 || intensity >= 1) {
        return intensity >= 1 ? color : neutral;
      }
      Color c = stringToColor(color), n = stringToColor(neutral);
      float[] chsv = new float[3], nhsv = new float[3];
      Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), chsv);
      Color.RGBtoHSB(n.getRed(), n.getGreen(), n.getBlue(), nhsv);
      return colorToString(Color.HSBtoRGB( //
          (float) (chsv[0] * intensity + nhsv[1] * (1 - intensity)), //
          (float) (chsv[1] * intensity + nhsv[1] * (1 - intensity)), //
          (float) (chsv[2] * intensity + nhsv[2] * (1 - intensity))));
    }
  };

  /**
   * Interpolate two colors.
   *
   * @param color
   * @param neutral
   * @param intensity
   * @return interpolated color
   */
  public abstract String interpolate(String color, String neutral, double intensity);
}
