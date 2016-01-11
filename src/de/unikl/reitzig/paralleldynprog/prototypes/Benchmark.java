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
import de.unikl.reitzig.paralleldynprog.prototypes.problems.RsDummy;
import de.unikl.reitzig.paralleldynprog.prototypes.solvers.*;
import vanilla.java.affinity.AffinityLock;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Runs a test suite on all available solvers.
 *
 * @author Raphael Reitzig, 02.2012
 */
public class Benchmark {
  private static final Map<String, String> ARGS = new HashMap<String, String>();

  private static final int[] sizes = new int[] { 1000, 1200, 1400, 1600, 1800, 2000,
                                                 4000, 6000, 8000, 10000, 12000, 14000,
                                                 16000, 18000, 20000, 40000 , 60000, 80000,
                                                 100000, 120000, 140000, 160000, 180000,
                                                 200000, 220000, 240000, 260000, 280000,
                                                 300000, 320000, 340000, 360000, 380000,
                                                 400000, 600000, 800000, 1000000,
                                                 4000000, 60000000, 8000000, 10000000,
                                                 20000000, 30000000 };
  private static final int inputsPerSize = 15;
  private static final int runsPerInput = 7;

  private static final int[] blockSizes = new int[] { -1, 100 };

  private static final List<Profiler> profilers = new ArrayList<Profiler>();

  private static void profile() throws IOException {
    System.out.println("Starting profiling on " + sizes.length * inputsPerSize + " inputs, each " + runsPerInput + " times per profiler.");

    int rows = -1;
    try {
      rows = Integer.parseInt(ARGS.get("rows"));
    }
    catch ( Exception e ) {}

    for ( final int size : sizes ) {
      for ( int i=0; i<inputsPerSize; i++ ) {
        final DynProgProblem<Integer> prob;
        if ( "DF".equals(ARGS.get("case")) ) {
          prob = new EditDistance(Util.randomString(rows > 0 ? rows : size), Util.randomString(size));
        }
        else {
          prob = new RsDummy(rows > 0 ? rows : size, Util.randomArray(size));
        }

        for ( final Profiler p : profilers ) {
          System.gc();
          p.profile(prob);
        }
      }
      System.out.println("Inputs of size " + size + " done.");
    }
  }

  public static void main(final String[] args) {
    Logger.getLogger(AffinityLock.class.getName()).setLevel(Level.SEVERE);

    // Read in command line parameters
    for ( final String arg : args ) {
      String key = null;
      if ( arg.startsWith("-d=") ) {
        key = "targetdir";
      }
      else if ( arg.startsWith("-p=") ) {
        key = "processors";
      }
      else if ( arg.startsWith("-c=") ) {
        key = "case";
      }
      else if ( arg.startsWith("-r=") ) {
        key = "rows";
      }

      if ( key != null ) {
        ARGS.put(key, arg.substring(3));
      }
    }

    // Evaluate parameters
    final File targetDir;
    if ( ARGS.containsKey("targetdir") ) {
      targetDir = new File(ARGS.get("targetdir"));
    }
    else {
      targetDir = new File(System.getProperty("user.home") + System.getProperty("file.separator") + "paralleldynprog");
    }
    System.out.println("Writing to '" + targetDir.getAbsolutePath() + "'");

    // Use default CPU number that ignores hyperthreading
    int cpuCount = AffinityLock.cpuLayout().sockets() * AffinityLock.cpuLayout().coresPerSocket();
    if ( ARGS.containsKey("processors") ) {
      try {
        cpuCount = Integer.parseInt(ARGS.get("processors"));
      }
      catch ( NumberFormatException e ) {}
    }
    System.out.println("Using " + cpuCount + " processors");

    // Check that target directory is usable
    if ( (targetDir.exists() && !targetDir.canWrite()) || (!targetDir.exists() && !targetDir.mkdirs()) ) {
      System.err.println("Could not write target directory.");
      System.exit(1);
    }

    // Setup solvers and wrap them in profilers
    final File profileDir = new File(targetDir.getAbsolutePath() + System.getProperty("file.separator") + "raw");
    if ( !(profileDir.exists() && profileDir.isDirectory()) && !profileDir.mkdirs() ) {
      System.err.println("Could not create profile data directory.");
      System.exit(1);
    }
    try {
      profilers.add(new Profiler(profileDir, new RowFill()));

      for ( int p=1; p<=cpuCount; p++ ) {
        if ( "DF".equals(ARGS.get("case")) ) {
          //profilers.add(new Profiler(profileDir, new CellCheck(p)));
          //profilers.add(new Profiler(profileDir, new CellCheckSleep(p)));
          //profilers.add(new Profiler(profileDir, new CellCheckWait(p)));
        }

        for ( final int k : blockSizes ) {
          if ( "DF".equals(ARGS.get("case")) ) {
            profilers.add(new Profiler(profileDir, new BlockCheck(p, k)));
            profilers.add(new Profiler(profileDir, new BlockCheckSleep(p, k)));
            profilers.add(new Profiler(profileDir, new BlockCheckWait(p, k)));
            profilers.add(new Profiler(profileDir, new ColumnBlock(p, k, k)));
            profilers.add(new Profiler(profileDir, new ColumnBlock(p, k)));
          }
          else {
            profilers.add(new Profiler(profileDir, new RowSplit(p, k)));
          }
        }
      }
      System.out.println(profilers.size() + " profilers created.");

      // Log some system metadata
      final BufferedWriter w = new BufferedWriter(new FileWriter(new File(targetDir.getAbsolutePath() + System.getProperty("file.separator") + "meta")));
      w.write("VM RAM : " + Runtime.getRuntime().totalMemory()); w.newLine();
      w.write("Processors : " + Runtime.getRuntime().availableProcessors()); w.newLine();
      final String[] props = new String[] { "sun.management.compiler", "os.arch", "os.name", "os.version",
                                            "java.vm.specification.name", "java.vm.specification.vendor",
                                            "java.vm.specification.version", "java.vm.vendor", "java.vm.version",
                                            "java.version", "java.vm.info", "java.vm.name", "java.runtime.name",
                                            "java.runtime.version" };

      for ( final String p : props ) {
        w.write(p + " : " + System.getProperty(p)); w.newLine();
      }

      w.close();

      profile();
    }
    catch (IOException e) {
      e.printStackTrace();
    }
    catch ( Throwable t ) {
      t.printStackTrace();
    }
  }

  /**
   * A small wrapper that manages profiling of one solver
   */
  private static class Profiler {
    private final BufferedWriter target;
    private final DynProgSolver solver;

    /**
     * Creates a new instance
     * @param targetDir Root target directory
     * @param solver The solver to wrap
     * @throws IOException In case of problems opening a the target file
     */
    Profiler(File targetDir, DynProgSolver solver) throws IOException {
      assert targetDir != null && solver != null : "null parameter";
      this.target = new BufferedWriter(new FileWriter(new File(targetDir.getAbsolutePath() + System.getProperty("file.separator") +
                                                               solver.toString())));
      this.solver = solver;

      Runtime.getRuntime().addShutdownHook(new Thread() {
        @Override
        public void run() {
          try {
            target.close();
          }
          catch (IOException e) {
            e.printStackTrace();
          }
        }
      });
    }

    /**
     * Has the wrapped solver solve copies of the specified problem for {@code runsPerInput} many
     * times and times execution times.
     * @param problem The problem to be solved
     * @throws IOException In case of problems writing results to file
     */
    void profile(final DynProgProblem<?> problem) throws IOException {
      assert problem != null : "null parameter";

      final StringBuilder res = new StringBuilder("" + problem.getDimension()[1]);

      for ( int i=0; i<runsPerInput; i++ ) {
        final DynProgProblem<?> p = problem.clone();
        final long start = System.currentTimeMillis();
        solver.solve(p);
        final long end = System.currentTimeMillis();
        res.append(",").append(end - start);
      }

      target.write(res.toString());
      target.newLine();
      target.flush();
    }
  }
}
