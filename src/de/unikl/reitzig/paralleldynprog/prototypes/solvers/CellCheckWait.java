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
 * Implements Diagonal Frontier scheme by waiting for notify if a cell can not be computed.
 * @author Raphael Reitzig, 02.2012
 * @deprecated Inferior to {@link BlockCheckWait}
 */
@Deprecated
public class CellCheckWait extends DiagonalFrontier {
  /**
   * Creates a new instance
   * @param p The number of workers this solver will use. Has to be positive.
   */
  public CellCheckWait(final int p) {
    super(p);
  }

  @Override
  void fillTable(final DynProgProblem<?> problem, final int w, final Notifier note) {
    final int[] param = new int[2];
    final int[] dim = problem.getDimension();
    final int leftNeighbour = ((w - 1) % p) >= 0 ? (w - 1) % p : p + ((w - 1) % p);

    for ( param[0]=w; param[0]<dim[0]; param[0]+=p ) {
      for ( param[1]=0; param[1]<dim[1]; param[1]+=1 ) {
        note.waitWhileNotComputable(leftNeighbour, problem,  param);
        problem.compute(param);
        note.notify(w);
      }
    }
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "[" + p + "]";
  }
}
