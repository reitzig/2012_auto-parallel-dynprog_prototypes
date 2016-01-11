#!/usr/bin/ruby

# Make sure that the user has given us an actual directory
if ( ARGV.size == 0 ) 
  puts "Please pass target directories as command line parameters."
  exit
end

ARGV.each { |dir| 
  if ( !File.directory?(dir) )
    puts dir + " is not a directory; skipping."
    next
  else
    targetdir = File.new(dir)
    print "Curating " + dir + "... "
    STDOUT.flush
  end

  $dirs = { "raw"  => dir + "/raw",
            "agg"  => dir + "/agg",
            "plot" => dir + "/plot" }
           
  $filenameformat = /\A([\w\d]+)\[(\d+)(?:,(\-?\d+))?(?:,(\-?\d+))?\]\z/
  $alpha = 1
  $sequentialSolver = "RowFill"
        
  # Make sure there are data files         
  if ( !File.exist?($dirs["raw"]) || !Dir.entries($dirs["raw"]).size == 0 )
    puts "There is no data in " + $dirs["raw"]
    exit
  end

  # Clean up result folders if necessary
  require 'fileutils'

  [$dirs["agg"], $dirs["plot"]].each { |d|
    if ( File.exist?(d) )
      FileUtils.rm_rf(d)
    end
    Dir.mkdir(d)
  }

  # Read in raw values. While doing that, throw away smallest and largest alpha readings
  # of all runs and average per size.
  # Result array will have the form
  #   [ name => [ (p, k1, k2) => [ size => avgtime ] ] ]
  $rawdata = {}
  Dir.entries($dirs["raw"]).find_all { |f| File.file?("#{$dirs["raw"]}/#{f}") && ($filenameformat =~ f) != nil }.each { |filename| 
    params = $filenameformat.match(filename).to_a # [filename, solvername, p, k1, k2]
    params[2] = params[2] != nil ? Integer(params[2]) : -99;
    params[3] = params[3] != nil ? Integer(params[3]) : -99;
    params[4] = params[4] != nil ? Integer(params[4]) : -99;

    if ( $rawdata[params[1]] == nil ) 
      $rawdata[params[1]] = {}
    end
    if ( $rawdata[params[1]][[params[2], params[3], params[4]]] == nil )
      $rawdata[params[1]][[params[2], params[3], params[4]]] = {}
    end

    currsize = nil
    currvals = []
    IO.foreach("#{$dirs["raw"]}/#{filename}") { |line|
      vals = line.split(",") # [size, v1, v2, ...]
      size = Integer(vals[0])
      vals = vals.drop(1).map { |v| Integer(v) }.sort!.drop($alpha).reverse!.drop($alpha) # timing values without smallest and largest k values
      
      if ( size != currsize ) 
        if ( currsize != nil )
          # Store average value for completely read size
          $rawdata[params[1]][[params[2], params[3], params[4]]][currsize] = (currvals.reduce(:+) / Float(currvals.size))
        end
        currsize = size
        currvals = []
      end
      
      currvals = currvals + vals
    }
    $rawdata[params[1]][[params[2], params[3], params[4]]][currsize] = (currvals.reduce(:+) / Float(currvals.size))
  }

  # Helper function for drawing plots
  def plot(file, number=1, comparison="", options="")
    plots = (2..(1+number)).map { |i| 
      "\\\"#{$dirs["agg"]}/#{file}\\\" using 1:#{i}:(1/10000.) ti col"
    }.join(", ")
  
    if ( comparison != "" )
      comparison = ", #{comparison} ti \\\"optimum\\\""
    end
    
    `gnuplot -e "set terminal pngcairo linewidth 2; set output \\"#{$dirs["plot"]}/#{file}.png\\"; #{options} plot #{plots}#{comparison}"`
  end

  # Helper function for plot labels
  def label(solver, params) 
    "#{solver}[#{params[0].to_s}#{params[1] != -99 ? "," + params[1].to_s : ""}#{params[2] != -99 ? "," + params[2].to_s : ""}]"
  end
  
  # Helper function for speedup comparison values
  def scmp(solver, params, size)
    $rawdata[solver][[1,params[1],params[2]]][size]
  end
  
  # Helper function for real speedup comparison values
  def rcmp(size)
    $rawdata[$sequentialSolver][[1,-99,-99]][size]
  end

  # Helper function for sorted solvers
  def sorted_solvers() # [solver, [p,k1,k2]]*
    res = []
    $rawdata.keys.sort.each { |solver|
      res += [solver].product($rawdata[solver].keys.sort)
    }
    res
  end

  # Write list of solvers
  File.open("#{$dirs["agg"]}/solvers" , "w") { |f|
    f.write(sorted_solvers().map { |a| label(a[0], a[1]) }.join("\n"))
  }

  # Write average and comparative data per size for all solvers and draw plots
  $rawdata.each_pair { |solver, instances| 
  
    # Write average runtimes
    File.open("#{$dirs["agg"]}/#{solver}_avg" , "w") { |f|
      f.write("# Aggregated average runtimes for #{solver} from #{dir}\n")
      f.write("\"Input Size\"\t#{instances.keys.sort.map { |p| label(solver, p) }.join("\t")}\n")
    
      instances[instances.keys[0]].keys.sort!.each { |size|
        f.write("#{size}\t#{instances.keys.sort.map { |p| instances[p][size] }.join("\t")}\n")
      }
    }
    plot("#{solver}_avg", instances.size)
    
    processors = instances.keys.map { |p| p[0] }.uniq
    
    # Write speedups per processor number
    processors.reject { |proc| proc == 1 }.each { |proc|
      insts = instances.keys.reject { |p| p[0] != proc }.sort
      
      File.open("#{$dirs["agg"]}/#{solver}[#{proc}]_speedup" , "w") { |f|
        f.write("# Aggregated speedups for #{solver} from #{dir}\n")
        f.write("\"Input Size\"\t#{insts.map { |p| label(solver, p) }.join("\t")}\n")
      
        instances[insts[0]].keys.sort!.each { |size|
          nonnil = insts.map { |p| [scmp(solver, p, size), instances[p][size]] }.flatten.compact
        
          if ( nonnil.size == 2*insts.size )
            f.write("#{size}\t#{insts.map { |p| scmp(solver, p, size)/instances[p][size] }.join("\t")}\n")
          end
        }
      }
      plot("#{solver}[#{proc}]_speedup", insts.size, proc, "set yrange [0:#{proc}.5];")
    }
    
    
    # Write real speedups per processor number
    processors.each { |proc|
      insts = instances.keys.reject { |p| p[0] != proc }.sort
      
      File.open("#{$dirs["agg"]}/#{solver}[#{proc}]_realspeedup" , "w") { |f|
        f.write("# Aggregated real speedups for #{solver} from #{dir}\n")
        f.write("\"Input Size\"\t#{insts.map { |p| label(solver, p) }.join("\t")}\n")
      
        instances[insts[0]].keys.sort!.each { |size|
          nonnil = insts.map { |p| instances[p][size] }.compact
        
          if ( rcmp(size) != nil && nonnil.size == insts.size )
            f.write("#{size}\t#{insts.map { |p| rcmp(size)/instances[p][size] }.join("\t")}\n")
          end
        }
      }
      plot("#{solver}[#{proc}]_realspeedup", insts.size, proc, "set yrange [0:#{proc}.5];")
    
    }
    
    parameters = instances.keys.map { |p| [p[1],p[2]] }.uniq
    
    parameters.each { |params| 
      solvers = sorted_solvers().reject { |a| a[0] != solver || [a[1][1], a[1][2]] != params }
      
      if ( solvers.size > 1 )
        paramlabel = "[_,#{params[0] != -99 ? params[0].to_s : ""}#{params[1] != -99 ? "," + params[1].to_s : ""}]"
        
        # Write speedups per parameter combination
        File.open("#{$dirs["agg"]}/#{solver}#{paramlabel}_speedup" , "w") { |f|
          f.write("# Aggregated speedups of #{solver}#{paramlabel} from #{dir}\n")
          f.write("\"Input Size\"\t#{solvers.map { |a| label(a[0], a[1]) }.join("\t")}\n")
        
          $rawdata[solvers[0][0]][solvers[0][1]].keys.sort!.each { |size|
            nonnil = solvers.map { |a| [scmp(a[0], a[1], size), $rawdata[a[0]][a[1]][size]] }.flatten.compact
        
            if ( nonnil.size == 2*solvers.size )
              f.write("#{size}\t#{solvers.map { |a| scmp(a[0], a[1], size)/(a[1][0] * $rawdata[a[0]][a[1]][size]) }.join("\t")}\n")
            end
          }
        }
        plot("#{solver}#{paramlabel}_speedup", solvers.size, "1.0", "set yrange [0:1.5];")
        
        # Write real speedups per parameter combination
        File.open("#{$dirs["agg"]}/#{solver}#{paramlabel}_realspeedup" , "w") { |f|
          f.write("# Aggregated real speedups of #{solver}#{paramlabel} from #{dir}\n")
          f.write("\"Input Size\"\t#{solvers.map { |a| label(a[0], a[1]) }.join("\t")}\n")
        
          $rawdata[solvers[0][0]][solvers[0][1]].keys.sort!.each { |size|
            nonnil = solvers.map { |a| $rawdata[a[0]][a[1]][size] }.compact
        
            if ( rcmp(size) != nil && nonnil.size == solvers.size )
              f.write("#{size}\t#{solvers.map { |a| rcmp(size)/(a[1][0] * $rawdata[a[0]][a[1]][size]) }.join("\t")}\n")
            end
          }
        }
        plot("#{solver}#{paramlabel}_realspeedup", solvers.size, "1.0", "set yrange [0:1.5];")
      end
    }
  }
  
  # Aggregate foundedness comparison
  onecores = sorted_solvers().reject { |a| a[0] == $sequentialSolver || a[1][0] != 1 }
  File.open("#{$dirs["agg"]}/foundedness" , "w") { |f|
    f.write("# Foundedness comparison from #{dir}\n")
    f.write("\"Input Size\"\t#{onecores.map { |a| label(a[0], a[1]) }.join("\t")}\n")
  
    $rawdata[onecores[0][0]][onecores[0][1]].keys.sort!.each { |size|
       nonnil = onecores.map { |a| $rawdata[a[0]][a[1]][size] }.compact
        
      if ( rcmp(size) != nil && nonnil.size == onecores.size )
        f.write("#{size}\t#{onecores.map { |a| rcmp(size)/$rawdata[a[0]][a[1]][size] }.join("\t")}\n")
      end
    }
  }
  plot("foundedness", onecores.size, "1.0", "set yrange [0:1.5];")
  
  processors = $rawdata[$rawdata.keys[0]].keys.map { |p| p[0] }.uniq.reject { |p| p == 1 }
  processors.each { |p|
    solvers = sorted_solvers().reject { |a| a[1][0] != p }
    
    # Aggregate speedup comparison
    File.open("#{$dirs["agg"]}/speedups[#{p}]" , "w") { |f|
      f.write("# Aggregated speedups on #{p} cores from #{dir}\n")
      f.write("\"Input Size\"\t#{solvers.map { |a| label(a[0], a[1]) }.join("\t")}\n")
    
      $rawdata[solvers[0][0]][solvers[0][1]].keys.sort!.each { |size|
        nonnil = solvers.map { |a| [scmp(a[0], a[1], size), $rawdata[a[0]][a[1]][size]] }.flatten.compact
        
        if ( nonnil.size == 2*solvers.size )
          f.write("#{size}\t#{solvers.map { |a| scmp(a[0], a[1], size)/$rawdata[a[0]][a[1]][size] }.join("\t")}\n")
        end
      }
    }
    plot("speedups[#{p}]", solvers.size, p, "set yrange [0:#{p}.5];")
    
    # Aggregate real speedup comparison
    File.open("#{$dirs["agg"]}/realspeedups[#{p}]" , "w") { |f|
      f.write("# Aggregated real speedups on #{p} cores from #{dir}\n")
      f.write("\"Input Size\"\t#{solvers.map { |a| label(a[0], a[1]) }.join("\t")}\n")
    
      $rawdata[solvers[0][0]][solvers[0][1]].keys.sort!.each { |size|
        nonnil = solvers.map { |a| $rawdata[a[0]][a[1]][size] }.compact
        
        if ( rcmp(size) != nil && nonnil.size == solvers.size )
          f.write("#{size}\t#{solvers.map { |a| rcmp(size)/$rawdata[a[0]][a[1]][size] }.join("\t")}\n")
        end
      }
    }
    plot("realspeedups[#{p}]", solvers.size, p, "set yrange [0:#{p}.5];")
  }
  
  puts "Done."
}
