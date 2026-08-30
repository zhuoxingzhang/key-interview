# Introduction

This repository contains the artifacts, source code and experimental materials that supplement our work on **Automatic Generation of Interview Questions for Domain Experts to Acquire Database Keys with Perfect Precision and Recall**.

The interview generates Boolean questions of the form "is this column set a key?" and prunes the search space from the answers received, so that a team of domain experts identifies the set of meaningful minimal keys of a schema with perfect precision and recall. The sections below describe how each experiment of the paper can be reproduced.

# The Eight Strategies

The suite is a single framework with three parameters: a **family**, a **starting point** and a **direction**. Every strategy is named `<family>-<starting point><direction>`, with T for top-down, B for bottom-up, B for breadth-first and D for depth-first.

|                              | breadth-first      | depth-first        |
| ---------------------------- | ------------------ | ------------------ |
| **level-wise (LW)**          | `LW-TB`, `LW-BB`   | `LW-TD`, `LW-BD`   |
| **dualize-and-advance (DA)** | `DA-TB`, `DA-BB`   | `DA-TD`, `DA-BD`   |

The level-wise family moves through the column lattice one column at a time. The dualization-based family adapts the Dualize and Advance algorithm to the interview setting, in which the answers of the domain experts replace the database instance as the source of truth, and asks a number of questions that is bounded linearly in the size of the border formed by the minimal keys and the maximal anti-keys.

The source code uses the strings `"topdown bfs"`, `"topdown dfs"`, `"bottomup bfs"` and `"bottomup dfs"` for the level-wise family, and `"dualize"` (= `DA-BD`), `"dualize bottomup bfs"`, `"dualize topdown dfs"` and `"dualize topdown bfs"` for the dualization-based family.

# Requirements

> 1. Software requirements
>> Java 17; Java Spring Boot; Python 3
> 2. Hardware requirements (non-LLM experiments)
>> Any server or PC capable of running Java 17. Our experiments were conducted on a server running Windows 11, equipped with a 12th Gen Intel(R) Core(TM) i7-12700 CPU @ 2.10 GHz and 128 GB of RAM.
> 3. Hardware requirements (LLM experiments)
>> A high-performance server running Red Hat Enterprise Linux 8.10, equipped with two NVIDIA A100 GPUs (80 GB VRAM each), a 256-core CPU, and 1 TB of system memory. The two LLMs used are:
>> - **DS**: DeepSeek-R1-Distill-Llama-70B
>> - **Qwen**: Qwen3-30B-A3B-Thinking-2507

# Online Demo

Visit the [website](https://deleted-stats-gratis-land.trycloudflare.com/) to interview for keys online! The start page asks for a family, a starting point and a direction, and runs the interview under the resulting strategy. If the link is not accessible, please report an issue.

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

How well do our algorithms perform under different distributions of minimal keys? Every question is answered `No` independently with probability *p*, for *p* = 0, 0.1, ..., 1 and schema sizes |T| = 11, 13, 15.

```bash
src/exp/SyntheticProbAnswering.java
```

Results are in `Artifact/results/syn_prob_answering_all_strategies.csv`, one row per strategy, schema size and *p*. The figure of the paper shows |T| = 11 and 15; the sweep itself covers |T| = 13 as well and the CSV retains it.

```bash
python plot/prob_answering_all_strategies.py                    # the figure of the paper
python plot/prob_answering_all_strategies.py --sizes 11 13 15   # all three schema sizes
```

## RQ2 — Level-wise versus Dualization-based Traversal (Real-World Distributions)

On which distributions does each family win, and how much does the dualization-based family reduce the number of questions on real-world data sets? The minimal keys derived from FDs mined from twelve real-world data sets serve as the answering oracle, which makes it feasible to run all eight strategies to completion on schemata where an interview requires tens of thousands of questions.

```bash
# one strategy on one data set
java exp.RWMinedFDKeys run <dataset> <strategy> Artifact/fd <cacheDir>
# or every data set and strategy inline
java exp.RWMinedFDKeys
```

- `Artifact/fd/` holds the mined FDs of the twelve data sets, in the directory layout the driver expects.
- `Artifact/results/rw_mined_fd_5strategies_summary.csv` reports, per data set, the numbers of minimal keys and maximal anti-keys, both question bounds, and for every strategy the number of `No` answers (`_keyQ`), the total number of questions (`_totalQ`) and the computation time in milliseconds (`_ms`).
- `Artifact/results/rw_mined_fd_5strategies_raw.csv` holds the per-run records behind that summary.

## RQ3 — Comparison to Key Mining (Hockey Database)

How much do we improve precision and recall for minimal keys mined from real-world data sets? The comparison is against the state-of-the-art key mining tool *DataViadotto* and against the primary keys declared on the schema, using the ground truth established by key interviews on all 22 tables of the Hockey database. The folder `Artifact/comparison_to_mining` contains:

1. **`mining-D0(exact)/`** — mining results for exact possible and certain keys (dirtiness D0).
2. **`mining-DN/`** — mining results for approximate keys at dirtiness level DN (N = 1, 5, 10).
3. **`ground_truth(interview_results)/`** — the ground truth established from key interviews on all Hockey tables.
4. **`interview_ground_truth.py`** — compares mining results against the ground truth (precision, recall, F1).
5. **`interview_primary_keys.py`** — compares the declared primary keys against the ground truth (baseline).

## RQ4 — LLMs as Domain Experts: Quality and Efficiency (Hockey Database)

How well do LLMs perform as domain experts in our interview process? Two settings are assessed on the Hockey tables for which the ground truth is available:

- **Direct LLM interviews**: the LLM answers questions generated over the original table schema.
- **Prime filtering**: the LLM first predicts the set of prime attributes, that is, the attributes that occur in some meaningful minimal key, and the interview is restricted to that reduced schema, which shrinks the search space from 2^|T| to 2^|P|.

```bash
LLM Oracle Python Script/Interview_with_LLM_Oracle.py
```

The folder `Artifact/llm_hockey` contains the LLM interview outputs for this experiment.

## RQ5 — Scalability with Prime Filtering (Real-World Data Sets without Ground Truth)

How well does the restriction to prime attributes scale our interview process to real-world data sets? For each data set and model the experiment reports the number of generated questions and the runtime for **all prime-attribute budgets |P| = 1, ..., 5**. Since no ground truth is available for these tables, precision and recall are not reported. The budget that the LLM itself predicted is marked in the paper, and the two models agree on every data set:

| Data set | \|T\| | \|P\| (DS and Qwen) |
| -------- | ----- | ------------------- |
| abalone  | 9     | 1                   |
| routes   | 9     | 5                   |
| breast   | 11    | 1                   |
| bridges  | 13    | 1                   |
| echo     | 13    | 1                   |
| pdbx     | 13    | 2                   |
| claims   | 13    | 1                   |
| adult    | 15    | 1                   |
| hospital | 15    | 1                   |
| lineitem | 16    | 2                   |
| weather  | 18    | 3                   |
| ncvoter  | 19    | 2                   |

```bash
LLM Oracle Python Script/Interview_with_LLM_Oracle_No_GT.py
```

The folder `Artifact/llm_scalability` contains the raw LLM interview outputs for all data sets, all budgets and all strategies, across both models.

## RQ6 — Human Interviewees of Varying Expertise

How much of the acquired quality comes from the expertise of a human interviewee, and how much from the interview itself? Ten participants at three levels of domain expertise answered nine Hockey tables under all eight strategies, using the online interview tool of this repository, together with a direct-naming ablation that replaces the traversal by the single open question "name the minimal keys of this table".

Participants took part voluntarily and gave informed consent. They were told in advance what the tool records, that they could stop at any point and decline any individual question, and that their records would be reported only in aggregated and pseudonymised form. **In keeping with that undertaking, this repository publishes no per-participant records.** The aggregated results are the two human-study tables of the paper.

# Repository Layout

| Path | Contents |
| ---- | -------- |
| `src/entity/` | schema, key and FD data types |
| `src/exp/` | the experiment drivers; `Interview.java` holds the interview loop and the candidate generation of both families |
| `LLM Oracle Python Script/` | the LLM-oracle interview scripts, mirroring the Java strategies |
| `plot/` | the scripts that draw the figures, reading the CSV files under `Artifact/results/` |
| `Artifact/results/` | the experiment result CSV files |
| `Artifact/fd/` | FDs mined from the twelve real-world data sets, used as the answering oracle of RQ2 |
| `Artifact/comparison_to_mining/` | the key mining results and the interview ground truth of RQ3 |
| `Artifact/llm_hockey/`, `Artifact/llm_scalability/` | the LLM interview outputs of RQ4 and RQ5 |
| `Artifact/Dataset.zip` | the data sets |
| `Artifact/keyinterviewtool-0.0.1-SNAPSHOT.jar` | the interview tool, implementing all eight strategies |
