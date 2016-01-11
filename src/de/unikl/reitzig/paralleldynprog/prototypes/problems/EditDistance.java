/*
 * This file is part of Parallel Dynamic Programming Implementation Prototype (PDPIP).
 *
 * PDPIP is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * PDPIP is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with PDPIP.  If not, see <http://www.gnu.org/licenses/>.
 */

package de.unikl.reitzig.paralleldynprog.prototypes.problems;

import de.unikl.reitzig.paralleldynprog.prototypes.DynProgProblem;

import java.util.Arrays;

/**
 * The canoncial edit distance problem for two strings
 * Note: This is buggy, but left that way because all benchmarks have been
 *       run with this version. In order to fix, add +1 to both dimensions
 *       and substract 1 from both parameters of charAt in line 66.
 * @author Raphael Reitzig, 02.2012
 */
public class EditDistance implements DynProgProblem<Integer> {
  private final String a;
  private final String b;
  private final int[][] m;
  private final int[] dim;

  public EditDistance(final String a, final String b) {
    this.a = a;
    this.b = b;
    this.dim = new int[] { a.length(), b.length() };
    this.m = new int[dim[0]][dim[1]];
    for ( int i=0; i<dim[0]; i++ ) {
      Arrays.fill(m[i], -1);
    }
  }

  @Override
  public int[] getDimension() {
    return dim.clone();
  }

  @Override
  public boolean isComputed(final int[] i) {
    assert i.length == 2 && i[0] >= 0 && i[1] >= 0 && i[0] < dim[0] && i[1] < dim[1] : "invalid indices";
    return m[i[0]][i[1]] != -1;
  }

  @Override
  public boolean isComputable(int[] i) {
    assert i.length == 2 && i[0] >= 0 && i[1] >= 0 && i[0] < dim[0] && i[1] < dim[1] : "invalid indices";
    return   i[0] == 0 || i[1] == 0 || (   isComputed(new int[] {i[0]-1,i[1]  })
                                        && isComputed(new int[] {i[0]  ,i[1]-1}) );
                                     // && isComputed(new int[] {i[0]-1,i[1]-1}) -- this is implied
  }

  @Override
  public void compute(final int[] i) {
    assert i.length == 2 && i[0] >= 0 && i[1] >= 0 && i[0] < dim[0] && i[1] < dim[1] : "invalid indices";
    assert isComputable(i) : "dependecies not computed";

    if ( i[0] == 0 && i[1] == 0 ) {
      m[i[0]][i[1]] = 0;
    }
    else if ( i[0] == 0 ) {
      m[i[0]][i[1]] = i[1];
    }
    else if ( i[1] == 0 ) {
      m[i[0]][i[1]] = i[0];
    }
    else {
      m[i[0]][i[1]] = Math.min(m[i[0]][i[1]-1] + 1,
                      Math.min(m[i[0]-1][i[1]] + 1,
                               m[i[0]-1][i[1]-1] + (a.charAt(i[0]) == b.charAt(i[1]) ? 0 : 1)));
    }
  }

  @Override
  public Integer getSolution() {
    if ( isSolved() ) {
      return m[dim[0]-1][dim[1]-1];
    }
    else {
      throw new Error("Requests result, but computation is not done.");
    }
  }

  @Override
  public boolean isSolved() {
    return isComputed(new int[] {dim[0]-1, dim[1]-1});
  }

  @Override
  public String toString() {
    return "Edit Distance for (" + a + ", " + b + ") -- " + (isSolved() ? "" : "un") + "solved";
  }

  @Override
  public EditDistance clone() {
    return new EditDistance(a, b);
  }
}
