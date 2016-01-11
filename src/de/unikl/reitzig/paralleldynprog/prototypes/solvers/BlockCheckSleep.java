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
 * Implements Diagonal Frontier scheme by sleeping while a block can not be computed.
 *
 * @author Raphael Reitzig, 02.2012
 */
public class BlockCheckSleep extends DiagonalFrontier {
  private final int blockSize;

  /**
   * Creates a new instance
   *
   * @param p The number of workers this solver will use. Has to be positive.
   * @param k Length of the blocks this solver will fill at a time.
   */
  public BlockCheckSleep(final int p, final int k) {
    super(p);
    this.blockSize = k;
  }

  /**
   * Creates a new instance that uses {@code p+1} blocks per row.
   *
   * @param p The number of workers this solver will use. Has to be positive.
   */
  public BlockCheckSleep(final int p) {
    this(p, -1);
  }

 /**
   * Creates a new instance that uses all processors and {@code p+1} blocks per row.
   */
  public BlockCheckSleep() {
    this(Runtime.getRuntime().availableProcessors(), -1);
  }

  @Override
  void fillTable(final DynProgProblem<?> problem, final int w, final Notifier n) {
    final int[] param = new int[2];
    final int[] checker = new int[2];
    final int[] dim = problem.getDimension();
    final int k = blockSize > 0 ? blockSize : dim[1]/(p + 1) + 1;

    for ( param[0]=w; param[0]<dim[0]; param[0]+=p ) {
      checker[0] = Math.max(0, param[0] - 1);
      param[1] = 0;

      for ( int offset=0; offset<dim[1]; offset+=k ) {
        // Wait until current block is computable
        //int s = 1;
        checker[1] = Math.min(dim[1] - 1, offset + k - 1);
        while ( param[0] != 0 && !problem.isComputed(checker) ) { // Sufficient because of assumptions; all intermediate can be computed if this one is
          try {
            Thread.sleep(1);
          }
          catch (InterruptedException e) {
            e.printStackTrace();
          }
          //s += 1;
        }

        for ( ; param[1]<Math.min(dim[1], offset + k); param[1]+=1 ) {
          problem.compute(param);
        }
      }
    }
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "[" + p + "," + blockSize + "]";
  }
}
