# Prototypes of Parallel Dynamic Programming Schemes
These are proof-of-concept implementations of the parallel dynamic programming 
schemes from my [Master's thesis](https://reitzig.github.io/publications/Reitzig2012).

This is how you run new benchmarks:

 1. Build the code with `ant`
 
 2. Run the JAR in `dist` to benchmark the prototypes. Results will be written
    to in a directorey in your home directory by default. You can control the
    benchmarking run with the following command line options:

    ```
    -d=<path>  -- Sets directory to write results into
    
    -p=<int>   -- Sets the maximum number of processors to use. Default
                  is all available cores (which may be bad if your CPU
                  supports hyperthreading; try to stick to real cores!).
                  
    -c=<case>  -- Selects which case to profile; one of `RS` and `DF`.
                  Default (and fallback on invalid inputs) is `RS`.
                  
    -r=<int>`  -- Sets the number of rows the benchmark inputs have.
                  Default is to use quadratic inputs, but with this
                  parameter you can fix one dimension.
    ```

    In order to adapt other benchmarking parameters, i.e. which implementations
    are used in either case, which input sizes are tried, how many inputs per size
    and how many runs per input are run, you will have to edit
        `src/de/unikl/reitzig/paralleldynprog/prototypes/Benchmark.java`
    and recompile.

Hints:
 * You can run the implementations' unit tests by `ant test`. Find test logs in
   `test-reports`.
 * Run `ant clean` to remove all generated files.
 * You can process the resulting data, i.e. create aggregate data and a bunch
   of plots, with the script `curate_data.rb`.
