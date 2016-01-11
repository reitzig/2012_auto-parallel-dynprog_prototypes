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

package de.unikl.reitzig.paralleldynprog.prototypes;

import de.unikl.reitzig.paralleldynprog.prototypes.problems.EditDistance;
import de.unikl.reitzig.paralleldynprog.prototypes.solvers.RowFill;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Blueprint of test that tests diagonal frontier implementations against RF
 *
 * @author Raphael Reitzig, 02.2012
 */
abstract public class DiagonalFrontierTest {
  private final DynProgSolver[] solvers;
  private final DynProgSolver reference;

  protected DiagonalFrontierTest(DynProgSolver... solvers) {
    this.solvers = solvers;
    this.reference = new RowFill();
  }

  @Test
  public void testSolve() throws Exception {
    for (final DynProgSolver solver : solvers ) {
      for ( int i=0; i<100; i++ ) {
        final String a = Util.randomString(5, 50);
        final String b = Util.randomString(5, 50);

        final DynProgProblem<Integer> ps = new EditDistance(a, b);
        final DynProgProblem<Integer> pr = new EditDistance(a, b);

        assertFalse("Premature solution", ps.isSolved());

        solver.solve(ps);
        reference.solve(pr);

        assertTrue("No solution by " + solver, ps.isSolved());
        assertEquals("Wrong solution by " + solver, pr.getSolution(), ps.getSolution());
      }
    }
  }
}
