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
# one strategy per run; underscores stand for the spaces in the strategy string
java exp.SyntheticProbAnswering topdown_bfs out.csv 11 13 15
java exp.SyntheticProbAnswering dualize_topdown_bfs out.csv 11 13 15
```

The driver accepts all eight strategy strings and appends one row per strategy, schema size and *p*. The recorded sweep is `Artifact/results/syn_prob_answering_all_strategies.csv`; it covers |T| = 11, 13 and 15, of which the figure of the paper shows the two extremes.

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

How much do we improve precision and recall for minimal keys mined from real-world data sets? The comparison is against the state-of-the-art key mining tool *DataViadotto* and against the primary keys declared on the schema, using the ground truth established by key interviews on all 22 tables of the Hockey database. The folder `Artifact/comparison_to_mining` holds the two comparison scripts:

1. **`interview_ground_truth.py`** — compares mining results against the ground truth (precision, recall, F1).
2. **`interview_primary_keys.py`** — compares the declared primary keys against the ground truth (baseline).

The ground truth is carried inside both scripts as the gold standard each of them parses: the meaningful minimal keys that the key interviews established on the Hockey tables, with the support of each key.

**The mining outputs themselves are not part of this repository.** They are produced by running *DataViadotto* over the Hockey database, once for exact possible and certain keys and once per dirtiness level, and each script reads them from the paths its `TASKS` list names — `mining-D0(exact)/` for the exact run and `mining-D1/`, `mining-D5/` and `mining-D10/` for the approximate ones. Until those files are placed there, this experiment is the one experiment of the paper that cannot be re-run from this repository alone; no other experiment depends on them.

## RQ4 — LLMs as Domain Experts: Quality and Efficiency (Hockey Database)

How well do LLMs perform as domain experts in our interview process? Two settings are assessed on the Hockey tables for which the ground truth is available:

- **Direct LLM interviews**: the LLM answers questions generated over the original table schema.
- **Prime filtering**: the LLM first predicts the set of prime attributes, that is, the attributes that occur in some meaningful minimal key, and the interview is restricted to that reduced schema, which shrinks the search space from 2^|T| to 2^|P|.

```bash
# --strategies takes any subset of LW-TD LW-TB LW-BD LW-BB DA-TD DA-TB DA-BD DA-BB
python "LLM Oracle Python Script/Interview_with_LLM_Oracle.py" \
    --model deepseek-ai/DeepSeek-R1-Distill-Llama-70B --mode prime \
    --strategies DA-TD DA-TB DA-BD DA-BB --outdir results_da
```

The folder `Artifact/llm_hockey` contains the LLM interview outputs for this experiment. `Artifact/llm_hockey/dualize/` holds the runs of the dualization-based family: one CSV row per table and strategy, the aggregates derived from those rows by `aggregate_da.py`, the predicted prime attributes as JSON, and the raw model transcript of every run.

## RQ5 — Scalability with Prime Filtering (Real-World Data Sets without Ground Truth)

How well does the restriction to prime attributes scale our interview process to real-world data sets? For each data set and model the experiment reports the number of generated questions and the runtime for **all prime-attribute budgets |P| = 1, ..., 5**, and every reported entry is the average over those five budgets. Since no ground truth is available for these tables, precision and recall are not reported.

```bash
python "LLM Oracle Python Script/Interview_with_LLM_Oracle_No_GT.py" \
    --model deepseek-ai/DeepSeek-R1-Distill-Llama-70B \
    --strategies DA-TD DA-TB DA-BD DA-BB --outdir results_da
```

The folder `Artifact/llm_scalability` contains the raw LLM interview outputs for all data sets, all budgets and all strategies, across both models. `Artifact/llm_scalability/dualize/` holds the runs of the dualization-based family, with one CSV row per data set, budget and strategy, the predicted prime attributes of every budget as JSON, and the raw model transcript of every run.

## RQ6 — Human Interviewees of Varying Expertise

How much of the acquired quality comes from the expertise of a human interviewee, and how much from the interview itself? Ten participants at three levels of domain expertise answered nine Hockey tables under all eight strategies, using the online interview tool of this repository, together with a direct-naming ablation that replaces the traversal by the single open question "name the minimal keys of this table".

Participants took part voluntarily and gave informed consent. They were told in advance what the tool records, that they could stop at any point and decline any individual question, and that their records would be reported only in aggregated and pseudonymised form. **In keeping with that undertaking, this repository publishes no per-participant records.** The aggregated results are the two human-study tables of the paper.

# Where the Reported Numbers Come From

Every number in the paper is read out of a file written by a program, and both the file and the program are in this repository. Two experiments are the exception, and each says so in its own section: the human study of RQ6, whose per-participant records are withheld for the reason given above, and RQ3, whose mining outputs are produced by a third-party tool and are not included here.

- The **Java drivers** in `src/exp/` run the interview and append one CSV row per run to `Artifact/results/`. A driver either takes the strategy as a command-line argument or iterates over the strategies itself, so in both cases the columns of a table come from one code path, run once per strategy.
- The **LLM scripts** in `LLM Oracle Python Script/` load the model with `transformers` and obtain each answer from `model.generate`; decoding is greedy (`do_sample=False`), so a run is reproducible given the same model and schema. The scripts write one CSV row per table and strategy and, alongside it, the raw transcript of the run, which records the schema, the predicted prime attributes, and the questions, timings and scores of each strategy. The aggregates (`Agg.` and M*k*) are not accumulated during a run; `aggregate_da.py` recomputes them from the recorded per-strategy rows, so they can be checked against those rows.
- The **comparison scripts** in `Artifact/comparison_to_mining/` compute precision, recall and F1 by set arithmetic over the mining output files and the interview ground truth, which each script carries as its gold standard. The mining outputs they read are the ones RQ3 names as not included.

No script states a result as a literal. Anything that looks like a measurement in this repository is either a CSV written by a run, a raw transcript of a run, or an input to a run, such as the mined FDs and the interview ground truth.

# Repository Layout

| Path | Contents |
| ---- | -------- |
| `src/entity/` | schema, key and FD data types |
| `src/exp/` | the experiment drivers; `Interview.java` holds the interview loop and the candidate generation of both families |
| `LLM Oracle Python Script/` | the LLM-oracle interview scripts, mirroring the Java strategies |
| `Artifact/results/` | the experiment result CSV files |
| `Artifact/fd/` | FDs mined from the twelve real-world data sets, used as the answering oracle of RQ2 |
| `Artifact/comparison_to_mining/` | the two comparison scripts of RQ3, each carrying the interview ground truth; the mining outputs they read are not included |
| `Artifact/llm_hockey/`, `Artifact/llm_scalability/` | the LLM interview outputs of RQ4 and RQ5, the dualization-based family under `dualize/` |
| `Artifact/Dataset.zip` | the data sets |
| `Artifact/keyinterviewtool-0.0.1-SNAPSHOT.jar` | the interview tool, implementing all eight strategies |
