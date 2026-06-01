# Introduction
This repository contains various artifacts, such as source code and other materials, that supplement our work on **Automatic Generation of Interview Questions for Human Experts to Acquire Database Keys with Perfect Precision and Recall**.\
&nbsp;&nbsp;&nbsp;&nbsp; In the following sections, we describe how our experiments can be reproduced. 
# Requirements
> 1. Software requirements
>> Java with version 17; Java Springboot
> 2. Datasets with minimal keys
>> see Artifact/Dataset.zip
# Online demo
Visit the [website](https://rpm-everywhere-magical-ready.trycloudflare.com/) to interview for keys online! If the link is not accessible, please report an issue.
# Deploy the demo on your PC
   1. download the jar file in Artifact/keyinterviewtool-0.0.1-SNAPSHOT.jar
   
   2. cd to the directory of the jar file

   3. `java -jar keyinterviewtool-0.0.1-SNAPSHOT.jar`

   4. visit http://localhost:8080 to start the interview on your PC!
   

   # How to reproduce the experiments

   1. Experiments for RQ1
      ```bash
      src/exp/SyntheticLevelwiseKeyDist.java
      src/exp/SyntheticRandomKeyDist.java
      src/exp/SyntheticProbAnswering.java
      ```

   2. Experiments for RQ2
      ```bash
      src/exp/RWDataSets.java
      src/exp/RWDesignedKeys.java
      ```

   3. Experiments for RQ4

      This folder (**Artifact/comparison_to_mining**) contains the results of our interview process, which defines the ground truth, and the results from state-of-the-art key mining algorithms, together with their comparison in terms of precision, recall and F1 values compared to the ground truth.

      1. **mining-D0(exact)/**  
         Contains the results from DataViadotto profiling for exact possible and certain keys (D0).

      2. **mining-DN/**  
         Contains the results from DataViadotto profiling for approximate possible and certain keys at dirtiness level DN (for N=1, 5, 10).

      3. **ground_truth(interview_results)/**  
         Contains the ground truth, that is, the results of the interview process.

      4. **interview_ground_truth.py**  
         Script for comparing the key mining results against the ground truth in terms of precision, recall and F1 scores.

      5. **interview_primary_keys.py**  
         Script for comparing the PRIMARY KEYs defined on the database schema against the ground truth, used as an additional baseline comparison.

   
   
   

   

