/*
 * This file is part of ELKI:
 * Environment for Developing KDD-Applications Supported by Index-Structures
 * 
 * Copyright (C) 2021
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
package elki.visualization.visualizers;

import java.awt.Color;

public enum Interpolation {
  RGB {
    @Override
    public String linearInterpolate(String color, String neutral, double intensity) {

      Color c = Color.decode(color);
      Color n = Color.decode(neutral);

      int iblue = (int) (c.getBlue() * intensity + (1 - intensity) * n.getBlue());
      int igreen = (int) (c.getGreen() * intensity + (1 - intensity) * n.getGreen());
      int ired = (int) (c.getRed() * intensity + (1 - intensity) * n.getRed());

      String icol2 = "#" + inttochar(ired / 16) + inttochar(ired % 16) + inttochar(igreen / 16) + inttochar(igreen % 16) + inttochar(iblue / 16) + inttochar(iblue % 16);
      return icol2;
    }

  },
  HSV {
    /**
     * note: this changes the tone linear as well, if you use a black color,
     * black ist coded as (0,0,x). In that case it will change the color.
     */
    @Override
    public String linearInterpolate(String color, String neutral, double intensity) {

      Color c = Color.decode(color);
      Color n = Color.decode(neutral);
      float[] chsv = new float[3];
      Color.RGBtoHSB(c.getRed(), c.getGreen(), c.getBlue(), chsv);
      float[] nhsv = new float[3];
      Color.RGBtoHSB(n.getRed(), n.getGreen(), n.getBlue(), nhsv);

      int icol = Color.HSBtoRGB((float) (chsv[0] * intensity + nhsv[1] * (1 - intensity)), //
          (float) (chsv[1] * intensity + nhsv[1] * (1 - intensity)), //
          (float) (chsv[2] * intensity + nhsv[2] * (1 - intensity)));
      
      int iblue = icol & 0x000000FF;
      icol >>>= 8;
      int igreen = icol & 0x000000FF;
      icol >>>= 8;
      int ired = icol & 0x000000FF;
      // icol >>>= 8;
      // int ialpha = icol & 0x000000FF;
      
      String icol2 = "#" + inttochar(ired / 16) + inttochar(ired % 16) + inttochar(igreen / 16) + inttochar(igreen % 16) + inttochar(iblue / 16) + inttochar(iblue % 16);
      return icol2;
    }
  };

  public abstract String linearInterpolate(String color, String neutral, double intensity);

  private static char inttochar(int t) {
    return Character.forDigit(t, 16);
  }
}
