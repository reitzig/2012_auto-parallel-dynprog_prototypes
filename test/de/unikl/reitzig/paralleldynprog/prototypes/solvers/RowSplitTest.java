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
import de.unikl.reitzig.paralleldynprog.prototypes.Util;
import de.unikl.reitzig.paralleldynprog.prototypes.problems.RsDummy;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * @author Raphael Reitzig, 02.2012
 */
public class RowSplitTest {
  @Test
  public void testSolve() throws Exception {
    final DynProgSolver[] solvers = new DynProgSolver[] {
      new RowSplit(Runtime.getRuntime().availableProcessors(), 5),
      new RowSplit(Runtime.getRuntime().availableProcessors()),
      new RowSplit()
    };
    final DynProgSolver reference = new RowFill();

    for ( final DynProgSolver solver : solvers ) {
      for ( int i=0; i<100; i++ ) {
        final int[] numbers = Util.randomArray(50);

        final DynProgProblem<Integer> ps = new RsDummy(50, numbers);
        final DynProgProblem<Integer> pr = new RsDummy(50, numbers);

        assertFalse("Premature solution", ps.isSolved());

        solver.solve(ps);
        reference.solve(pr);

        assertTrue("No solution by " + solver, ps.isSolved());
        assertEquals("Wrong solution by " + solver, pr.getSolution(), ps.getSolution());
      }
    }
  }
}
