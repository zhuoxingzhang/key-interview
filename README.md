# Introduction

This repository contains various artifacts, such as source code and other materials, that supplement our work on **Automatic Generation of Interview Questions for Human and Artificial Experts to Acquire Database Keys with Perfect Precision and Recall**.

In the following sections, we describe how our experiments can be reproduced.

# Requirements

> 1. Software requirements
>> Java 17; Java Spring Boot; Python 3
> 2. Hardware requirements (non-LLM experiments)
>> Any server or PC capable of running Java 17. Our experiments were conducted on a server running Windows 11, equipped with a 12th Gen Intel® Core™ i7-12700 CPU @ 2.10 GHz and 128 GB of RAM.
> 3. Hardware requirements (LLM experiments — RQ3 & RQ4)
>> A high-performance server running Red Hat Enterprise Linux 8.10, equipped with two NVIDIA A100 GPUs (80 GB VRAM each), a 256-core CPU, and 1 TB of system memory. The two LLMs used are:
>> - **DS**: DeepSeek-R1-Distill-Llama-70B
>> - **Qwen**: Qwen3-30B-A3B-Thinking-2507

# Online Demo

Visit the [website](https://rpm-everywhere-magical-ready.trycloudflare.com/) to interview for keys online! If the link is not accessible, please report an issue.

# Deploy the Demo on Your PC

1. Download the jar file at `Artifact/keyinterviewtool-0.0.1-SNAPSHOT.jar`
2. `cd` to the directory of the jar file
3. Run:
   ```bash
   java -jar keyinterviewtool-0.0.1-SNAPSHOT.jar
   ```
4. Visit `http://localhost:8080` to start the interview on your PC.

# How to Reproduce the Experiments

## RQ1 — Distribution of Minimal Keys (Synthetic)

Evaluate how each traversal strategy (TD, TB, BD, BB) performs under different distributions of minimal keys using synthetic schemas.

```bash
<!-- src/exp/SyntheticLevelwiseKeyDist.java -->
<!-- src/exp/SyntheticRandomKeyDist.java -->
src/exp/SyntheticProbAnswering.java
```

## RQ2 — Comparison to Key Mining (Hockey Database)

Quantify how much key interviews improve precision and recall over state-of-the-art key mining (DataViadotto) and the original schema definition, using the ground truth established from key interviews on all 22 tables of the Hockey database.

```bash
src/exp/RWDataSets.java
src/exp/RWDesignedKeys.java
```

The folder `Artifact/comparison_to_mining` contains the full results of this comparison:

1. **`mining-D0(exact)/`**  
   DataViadotto profiling results for exact possible and certain keys (dirtiness D0).

2. **`mining-DN/`**  
   DataViadotto profiling results for approximate possible and certain keys at dirtiness level DN (N = 1, 5, 10).

3. **`ground_truth(interview_results)/`**  
   Ground truth established from key interviews on all Hockey tables.

4. **`interview_ground_truth.py`**  
   Script to compare key mining results against the ground truth (precision, recall, F1).

5. **`interview_primary_keys.py`**  
   Script to compare primary keys defined on the schema against the ground truth (baseline).

## RQ3 — LLMs as Domain Experts: Quality and Efficiency (Hockey Database)

Evaluate the quality and efficiency of LLM-based interviews on Hockey database tables where the ground truth is available. Two strategies are assessed:

- **Direct LLM interviews**: LLMs answer questions generated directly over the original table schema.
- **Prime-attribute filtering**: LLMs first predict the set of prime attributes (attributes that appear in some meaningful minimal key), and the interview is then restricted to that reduced schema (reducing the search space from $2^{|T|}$ to $2^{|P|}$).

Results are reported in terms of exact and relaxed F1, Recall, and Precision under all four traversal strategies (TD, TB, BD, BB), as well as aggregation and frequency-based minimization (Mk).

```bash
LLM Oracle Python Script/Interview_with_LLM_Oracle.py      # Direct LLM interviews (Table III in paper), Prime-attribute filtering (Table IV in paper)
```

The folder `Artifact/llm_hockey` contains the raw LLM interview outputs and evaluation scripts for this experiment.

## RQ4 — Scalability with Prime-Attribute Filtering (Real-World Datasets without Ground Truth)

Evaluate how well prime-attribute filtering allows LLM-based interviews to scale to larger real-world datasets where the original interview process would be prohibitively expensive and no ground truth is available.

Table V reports, for each dataset and model (DS / Qwen), the number of generated interview questions (#Q) and runtime (minutes) for **all predicted prime-attribute budgets |P| = 1, …, 5** under each of the four traversal strategies (TD, TB, BD, BB). Since no ground truth is available for these tables, precision and recall are not reported. The **orange-highlighted row** in each model block marks the value of |P| actually predicted by the LLM — i.e., the operating point used in practice. The LLM-predicted |P| values are summarised below (DS and Qwen agree for all datasets; routes predicted |P| = 6 exceeds the reported budget and is therefore highlighted at |P| = 5):

| Dataset  | \|T\| | Predicted \|P\| (DS & Qwen) |
|----------|-------|------------------------------|
| abalone  | 9     | 1                            |
| routes   | 9     | 6 (highlighted at \|P\|=5)   |
| breast   | 11    | 1                            |
| bridges  | 13    | 1                            |
| echo     | 13    | 1                            |
| pdbx     | 13    | 2                            |
| claims   | 13    | 1                            |
| adult    | 15    | 1                            |
| hospital | 15    | 1                            |
| lineitem | 16    | 2                            |
| weather  | 18    | 3                            |
| ncvoter  | 19    | 2                            |

```bash
LLM Oracle Python Script/Interview_with_LLM_Oracle_No_GT.py    # Scalability experiments (Table V in paper)
```

The folder `Artifact/llm_scalability` contains the raw LLM interview outputs (question counts and runtimes) for all datasets, all |P| = 1, …, 5 budgets, and all four traversal strategies, across both models.
