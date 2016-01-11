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
 * A dummy for case 2 problems
 *
 * @author Raphael Reitzig, 02.2012
 */
public class RsDummy implements DynProgProblem<Integer> {
  private final int[] numbers;
  private final int[][] m;
  private final int[] dim;

  public RsDummy(final int iterations, final int[] numbers) {
    this.numbers = numbers;
    this.dim = new int[] { iterations, numbers.length };
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
  public boolean isComputed(int[] i) {
    assert i.length == 2 && i[0] >= 0 && i[1] >= 0 && i[0] < dim[0] && i[1] < dim[1] : "invalid indices";
    return m[i[0]][i[1]] > -1;
  }

  @Override
  public boolean isComputable(int[] i) {
    assert i.length == 2 && i[0] >= 0 && i[1] >= 0 && i[0] < dim[0] && i[1] < dim[1] : "invalid indices";

    boolean res = true;
    if ( i[0] > 0 ) {
      res = isComputed(new int[] { i[0]-1, i[1] });
      if ( i[1] > 0 ) {
        res = res && isComputed(new int[] { i[0]-1, i[1] -1 });
      }
      if ( i[1] < dim[1] - 1 ) {
        res = res && isComputed(new int[] { i[0]-1, i[1] + 1 });
      }
    }
    return res;
  }

  @Override
  public void compute(int[] i) {
    assert i.length == 2 && i[0] >= 0 && i[1] >= 0 && i[0] < dim[0] && i[1] < dim[1] : "invalid indices";
    assert isComputable(i) : "trying to compute uncomputable cell " + Arrays.toString(i);

    if ( i[0] == 0 ) {
      m[0][i[1]] = Math.max(0, numbers[i[1]]);
    }
    else {
      int nr = m[i[0]-1][i[1]] + numbers[i[1]];
      if ( i[1] > 0 ) {
        nr = Math.max(m[i[0]-1][i[1]-1] + numbers[i[1]], nr);
      }
      if ( i[1] < dim[1] - 1 ) {
        nr = Math.max(nr, m[i[0]-1][i[1]+1] + numbers[i[1]]);
      }
      m[i[0]][i[1]] = Math.max(0,nr);
    }

    assert m[i[0]][i[1]] > -1 : "computed invalid value";
  }

  @Override
  public Integer getSolution() {
    assert isSolved() : "trying to get solution of unsolved problem";

    int min = m[m.length - 1][0];
    for ( final int i : m[m.length - 1] ) {
      min = min > i ? i : min;
    }
    return min;
  }

  @Override
  public boolean isSolved() {
    boolean res = true;
    for ( final int i : m[m.length - 1] ) {
      res = res && i > -1;
    }
    return res;
  }

  @Override
  public RsDummy clone() {
    return new RsDummy(dim[0], numbers);
  }
}
