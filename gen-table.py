import sys
import os, os.path
import platform
import shutil
import time
# import re
import csv
# from subprocess import call, check_output, STDOUT
# from colorama import init, Fore, Back, Style

# Globals
CSV_FILE = 'stats.csv'                    # CSV-input file
LATEX_FILE = 'results.tex'                  # Latex-output file
PAPER_DIR = '/mnt/h/Work/papers/synsl/synsl/popl19-draft/tab' # Directory where to copy the latex file (if exists)
TEST_DIR = 'src/test/resources/synthesis/paper-benchmarks/'
SOURCES = ['natural', 'jennisys', 'dryad']
# VARIANTS = ['commute']
VARIANTS = ['phased', 'invert', 'fail', 'commute']

class Benchmark:
  def __init__(self, name, description, source=[]):
    self.name = name        # Id (corresponds to test file name)
    self.description = description  # Description (in the table)
    self.source = source      # Where is this benchmark from (in the table)

  def str(self):
    return self.name + ': ' + self.description

class BenchmarkGroup:
  def __init__(self, name, benchmarks):
    self.name = name            # Id
    self.benchmarks = benchmarks      # List of benchmarks in this group

ALL_BENCHMARKS = [
  BenchmarkGroup("Integers",  [
    Benchmark('ints/swap', 'swap two', []),
    Benchmark('ints/min2', 'min of two', ['jennisys']),
    ]),    
  BenchmarkGroup("Linked List", [
    Benchmark('sll-bounds/sll-len', 'length', ['natural']),
    Benchmark('sll-bounds/sll-max', 'max', ['natural']),
    Benchmark('sll-bounds/sll-min', 'min', ['natural']),
    Benchmark('sll/sll-singleton', 'singleton', ['jennisys']),
    Benchmark('sll/sll-free', 'dispose', []),
    Benchmark('sll/sll-copy', 'copy', []),
    Benchmark('sll/sll-append', 'append', ['dryad']),
    ]),
  BenchmarkGroup("Sorted list", [
    Benchmark('srtl/srtl-prepend', 'prepend', ['natural']),
    Benchmark('srtl/srtl-insert', 'insert', ['natural']),
    Benchmark('srtl/insertion-sort', 'insertion sort', ['natural']),
    ]),
  BenchmarkGroup("Tree", [
    Benchmark('tree/tree-size', 'size', []),
    Benchmark('tree/tree-free', 'dispose', []),
    Benchmark('tree/tree-copy', 'copy', []),
    Benchmark('tree/tree-flatten', 'flatten to list', []),
    ]),
]

class SynthesisResult:
  def __init__(self, name, time, spec_size, code_size):
    self.name = name                                      # Benchmark name
    self.time = time                                      # Synthesis time (seconds)
    self.spec_size = spec_size                            # Cumulative specification size (in AST nodes)
    self.code_size = code_size                            # Cumulative synthesized code size (in AST nodes)
    self.variant_times = {var : -3.0 for var in VARIANTS} # Synthesis times for SuSLik variants:
      

  def str(self):
    return self.name + ', ' + '{0:0.2f}'.format(self.time) + ', ' + self.spec_size + ', ' + self.code_size + ', ' + str(self.variant_times)

def var_option(var):
  return '--' + var + ' false'
    
def format_time(t):
  if t < 0:
    return '-'
  if t < 0.1:
    return '$<0.1$'
  else:
    return '{0:0.1f}'.format(t)

def read_csv():
  '''Read stats file into the results dictionary'''
  with open(CSV_FILE, 'rb') as csvfile:
    d = csv.excel
    d.skipinitialspace = True
    statsReader = csv.DictReader(csvfile, dialect = d)
    for row in statsReader:
      name = row['Name']
      time = float(row['Time'])/1000
      spec_size = row['Spec Size']
      code_size = row['Code Size']
      
      is_var = False
      for var in VARIANTS:
        if name.endswith(var):
          # This is a test for a variant
          is_var = True
          suffix_len = len(var) + 1
          store_result(name[:-suffix_len], time, spec_size, code_size, var)
      if not is_var:
        store_result(name, time, spec_size, code_size)
      
def store_result(name, time, spec_size, code_size, variant = 'none'):
  timeOrTO = -1.0 if code_size == 'FAIL' else time
  
  if not(name in results):
    results[name] = SynthesisResult(name, timeOrTO, spec_size, code_size)
  
  if variant == 'none':
    results[name].time = timeOrTO
    results[name].code_size = code_size
  else:
    results[name].variant_times[variant] = timeOrTO
      
def footnotes(sources):
  res = ''
  for s in sources:
    i = SOURCES.index(s)
    res = res + '\\textsuperscript{' + str(i) + '}'
  return res  

def write_latex():
  '''Generate Latex table from the results dictionary'''
  
  total_count = 0
  # to_def = 0
  # to_nrt = 0
  # to_ncc = 0
  # to_nmus = 0

  with open(LATEX_FILE, 'w') as outfile:
    for group in groups:
      outfile.write ('\multirow{')
      outfile.write (str(group.benchmarks.__len__()))
      outfile.write ('}{*}{\\parbox{1cm}{\center{')
      outfile.write (group.name)
      outfile.write ('}}}')      

      for b in group.benchmarks:
        result = results [b.name]        
        row = \
          ' & ' + b.description + footnotes(b.source) +\
          ' & ' + result.spec_size + \
          ' & ' + result.code_size + \
          ' & ' + format_time(result.time) + \
          ' & ' + format_time(result.variant_times['phased']) + \
          ' & ' + format_time(result.variant_times['invert']) + \
          ' & ' + format_time(result.variant_times['fail']) + \
          ' & ' + format_time(result.variant_times['commute']) + ' \\\\'
          
        outfile.write (row)
        outfile.write ('\n')
        
        total_count = total_count + 1
        # if result.variant_times['def'] < 0.0:
           # to_def = to_def + 1 
        # if result.variant_times['nrt'] < 0.0:
           # to_nrt = to_nrt + 1 
        # if result.variant_times['ncc'] < 0.0:
           # to_ncc = to_ncc + 1 
        # if result.variant_times['nmus'] < 0.0:
           # to_nmus = to_nmus + 1 
        
      outfile.write ('\\hline')
      
  # Copy latex file into the paper directory if properly set
  if os.path.isdir(PAPER_DIR):
    shutil.copy(LATEX_FILE, PAPER_DIR)
  else:
    print 'Paper not found in ', PAPER_DIR  
      
  print 'Total:', total_count
  # print 'TO def:', to_def
  # print 'TO nrt:', to_nrt
  # print 'TO ncc:', to_ncc
  # print 'TO nmus:', to_nmus
  
def generate_variants():
  '''Generate benchmark variants with disables optimizations'''
  
  for group in groups:
    for b in group.benchmarks:
      test = TEST_DIR + b.name
      testFileName = test + '.syn'
      if not os.path.isfile(testFileName):
        print "Test file not found:", testFileName
      else:
        for var in VARIANTS:
          varFileName = test + '-' + var + '.syn'     
          shutil.copy(testFileName, varFileName)
          with open(varFileName, 'r+') as f:
            content = f.read()
            f.seek(0, 0)
            if content.startswith('#'):
              # already has a config line
              lines = content.split('\n', 1)
              f.write(lines[0].rstrip() + ' ' + var_option(var) + '\n' + lines[1])
            else:
              # no config line, create one
              f.write('#. this ' + var_option(var) + '\n' + content)
      
def clean_variants():
  '''Remove previously generated benchmark variants'''
  
  for group in groups:
    for b in group.benchmarks:
      test = TEST_DIR + b.name
      for var in VARIANTS:
        varFileName = test + '-' + var + '.syn'
        if os.path.isfile(varFileName):        
          os.remove(varFileName)
        
def cmdline():
    import argparse
    a = argparse.ArgumentParser()
    a.add_argument('--var', action='store_true')
    a.add_argument('--clean', action='store_true')
    return a.parse_args()        

if __name__ == '__main__':
  # init()
  
  cl_opts = cmdline()
  
  results = dict()
  groups = ALL_BENCHMARKS
  
  if cl_opts.var:
    generate_variants()
  elif cl_opts.clean:
    clean_variants()
  else:        
    # Read stats into a dictionary of synthesis results
    read_csv()
    
    # for res in results:
      # print results[res].str()
    
    # Generate Latex table
    write_latex()
    

    