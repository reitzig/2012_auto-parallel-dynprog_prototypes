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

import de.unikl.reitzig.paralleldynprog.prototypes.DiagonalFrontierTest;
import org.junit.Test;

/**
 * @author Raphael Reitzig, 02.2012
 */
public class CellCheckWaitTest extends DiagonalFrontierTest {
  public CellCheckWaitTest() {
    super(new CellCheckWait(Runtime.getRuntime().availableProcessors()));
  }

  @Test
  @Override
  public void testSolve() throws Exception {
    super.testSolve();
  }
}
