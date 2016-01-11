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
import de.unikl.reitzig.paralleldynprog.prototypes.DynProgSolver;
import vanilla.java.affinity.AffinityLock;

/**
 * Canonical algorithm (row-wise table filling)
 *
 * @author Raphael Reitzig, 02.2012
 */
public class RowFill implements DynProgSolver {
  @Override
  public void solve(final DynProgProblem<?> problem) {
    assert problem != null : "null parameter";

    if ( !problem.isSolved() && problem.getDimension().length == 2 ) {
      final int[] dim = problem.getDimension();
      final int[] param = new int[2];

      AffinityLock afflock = AffinityLock.acquireCore(true);

      for ( param[0]=0; param[0]<dim[0]; param[0]++ ) {
        for ( param[1]=0; param[1]<dim[1]; param[1]++ ) {
          problem.compute(param);
        }
      }

      afflock.release();
    }
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName() + "[1]";
  }
}
