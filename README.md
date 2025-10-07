# Introduction
This repository contains various artifacts, such as source code and other materials, that supplement our work on **Automatic Generation of Interview Questions for Human Experts to Acquire Database Keys with Perfect Precision and Recall**.\
&nbsp;&nbsp;&nbsp;&nbsp; In the following sections, we describe how our experiments can be reproduced. 
# Preliminaries: Getting ready for experiments
>1. Software requirements
>>  Java with version 17; Java Springboot
>2. FDs for datasets
>> See directory <kbd>Artifact/FD/</kbd>.
# Online demo
Visit the [website](http://4e0152ef.r15.vip.cpolar.cn/) to interview for keys online! If the link is not accessible, please report an issue.
# Deploy the demo on your PC
```bash
   download the jar file in Artifact/keyinterviewtool-0.0.1-SNAPSHOT.jar
   cd to the directory of the jar file
   java -jar keyinterviewtool-0.0.1-SNAPSHOT.jar
   visit http://localhost:8080 to start the interview on your PC!
   ```
# How to run code from the command line
1. Clone the project and navigate to the project directory:
   ```bash
   cd <your_project_directory>/src/exp
   ```
2. Run separate code from the command line for experiments:
   
   2.1 Experiments for RQ1
   ```bash
   java SyntheticExp4KeyDist.java
   ```
   2.2 Experiments for RQ2 and RQ3
   ```bash
   java SyntheticExp.java
   ```
   2.3 Experiments for RQ4
   ```bash
   java SyntheticExp4SpKey.java
   ```
   2.3 Experiments for RQ5
   ```bash
   java RealWorldExpLHS.java
   java RealWorldExpHockey.java
   ```
   

   

