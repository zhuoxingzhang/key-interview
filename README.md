# Introduction
This repository contains various artifacts, such as source code, experimental results, and other materials, that supplement our work on **Automatic Generation of Interview Questions for Human Experts to Acquire Database Keys with Perfect Precision and Recall**.\
&nbsp;&nbsp;&nbsp;&nbsp; In the following sections, we describe how our experiments can be reproduced. 
# Preliminaries: Getting ready for experiments
>1. Software requirements
>>  JAVA with version 17
# Experiments
In line with our paper, our experiments are organized into four sections. For each of them, you can run different code/scripts:
>1. Mini Study
>> In this part, we conducted a mini-study to showcase our research motivation as part of the introduction. Using the same FDs as input, iCONF (our new algorithm) and CONF (previous SOTA) produce two different decompositions. For each decomposition, we insert projections of the same records over the original schema on each subschema of the decomposition. The mini-study shows that 1) the decomposition from iCONF reduces update overheads much more than CONF does, illustrating that non-key FDs are a bottleneck for integrity maintenance, but also that 2) the use of FDs to uniformly do integrity checking for minimal keys and non-key FDs is prohibitively expensive. In particular, 2) also motivates our new framework where 3NF normalization is recast in terms of minimal keys, non-key FDs, and integrity maintenance. To reproduce the mini-study experiment, you can set up some parameters and run the code at <kbd>src/exp/SyntheticExpForCaseStudy.java</kbd>.

# How to run code from the command line
1. Clone the repository:
   ```bash
   git clone https://github.com/zzxhelloworld/iCONF.git
   ```
2. Navigate to the project directory:
   ```bash
   cd your_project_directory
   ```
3. Run separate code from the command line for experiments:
   
   3.1 Mini Study
   ```bash
   javac SyntheticExpForCaseStudy.java
   java SyntheticExpForCaseStudy <output_path> <db_table_name> <experiment_repeat_num> <synthetic_dataset_num> <insert_num>
   ```
   3.2 How do keys and non-key FDs affect performance?
   ```bash
   javac TPCHWorkloadExp.java
   java TPCHWorkloadExp <experiment_repeat_num> <TPCH_sql_path> <TPCH_schema_output_path> <experimental_result_output_path>
   ```
   3.3 How good are our algorithms?
   ```bash
   javac DecompExp.java
   java DecompExp <dataset_name> <experimental_results_output_directory>
   ```
   3.4 How much overhead do we save?
   ```bash
   javac SubschemaPerfExp.java
   java SubschemaPerfExp <experiment_repeat_num> <schema_sample_num> <experimental_results_output_path>
                         <decomposition_algs_separated_by_commas> <dataset_name> <experimental_results_output_directory>
   ```

   

