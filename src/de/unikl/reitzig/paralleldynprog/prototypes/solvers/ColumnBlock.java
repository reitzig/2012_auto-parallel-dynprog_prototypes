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

package de.unikl.reitzig.paralleldynprog.prototypes.solvers;

import de.unikl.reitzig.paralleldynprog.prototypes.DynProgProblem;

/**
 * Implements a variant of the Diagonal Frontier scheme that attempts to minimise communication
 * between threads. It divides the matrix into columns. Each column is filled by one thread
 * row by row. Thread {@code i} regularly notifies thread {@code i+1} in order to wake it up if
 * it has been waiting for needed results (see also {@link BlockCheckWait}.
 *
 * @author Raphael Reitzig, 03.2012
 */
public class ColumnBlock extends DiagonalFrontier {
  private final int columnWidth;
  private final int blockSize;

  /**
   * Creates a new instance
   *
   * @param p The number of workers this solver will use. Has to be positive.
   * @param columnWidth Width of table pieces. Passing a value less or equal to {@code 0} will result
   *                    in {@code n/p + 1} being used, {@code n} the problem's second dimension.
   *                    (i.e. number of columns).
   * @param blockSize Height of the blocks this solver will fill at a time before notifying.
   *                  Passing a value less or equal to {@code 0} will result in {@code m/p + 1}
   *                  being used, {@code m} the problem's first dimension. (i.e. number of rows).
   */
  public ColumnBlock(final int p, final int columnWidth, final int blockSize) {
    super(p);
    this.columnWidth = columnWidth;
    this.blockSize = blockSize;
  }

  /**
   * Creates a new instance that divides the table into {@code p} columns.
   *
   * @param p The number of workers this solver will use. Has to be positive.
   * @param blockSize Height of the blocks this solver will fill at a time before notifying.
   *                  Passing a value less or equal to {@code 0} will result in {@code m/p + 1}
   *                  being used, {@code m} the problem's first dimension. (i.e. number of rows).
   */
  public ColumnBlock(final int p, final int blockSize) {
    this(p, -1, blockSize);
  }

   /**
   * Creates a new instance that uses all available processors, divides the table into {@code p}
   * columns and notifies {@code p} times per column.
   */
  public ColumnBlock() {
    this(Runtime.getRuntime().availableProcessors(), -1, -1);
  }

  @Override
  void fillTable(final DynProgProblem<?> problem, final int w, final Notifier note) {
    final int[] param = new int[2];
    final int[] checker = new int[2];
    final int[] dim = problem.getDimension();
    final int cw = columnWidth > 0 ? columnWidth : dim[1]/p + 1;
    final int k = blockSize > 0 ? blockSize : dim[0]/p + 1;
    final int leftNeighbour = ((w - 1) % p) >= 0 ? (w - 1) % p : p + ((w - 1) % p);

    for ( int coffset=w*cw; coffset<dim[1]; coffset = Math.min(dim[1], coffset + cw*p) ) {
      checker[1] = coffset - 1;

      for ( int roffset=0; roffset<dim[0]; roffset = Math.min(dim[0], roffset+k) ) {
        checker[0] = Math.min(dim[0] - 1, roffset + k);

        if ( coffset > 0 ) {
          note.waitWhileNotComputed(leftNeighbour, problem, checker);
        }

        for ( param[0]=roffset; param[0]<Math.min(dim[0], roffset+k); param[0]++ ) {
          for ( param[1]=coffset; param[1]<Math.min(dim[1], coffset+cw); param[1]++ ) {
            problem.compute(param);
          }
        }

        note.notify(w);
      }
    }
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "[" + p + "," + columnWidth + "," + blockSize + "]";
  }
}
