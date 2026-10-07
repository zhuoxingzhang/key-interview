# Introduction

This repository contains the artifacts, source code and experimental materials that supplement our work on **Computational Support for a Human-Centered Methodology that Aims to Acquire Database Keys with Perfect Precision and Recall**.

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

Visit the [website](https://euros-mph-phantom-lighter.trycloudflare.com/) to interview for keys online! The start page asks for a family, a starting point and a direction, and runs the interview under the resulting strategy. If the link is not accessible, please report an issue.

# Deploy the Demo on Your PC

1. Download the jar file at `Artifact/keyinterviewtool-0.0.1-SNAPSHOT.jar`
2. `cd` to the directory of the jar file
3. Run:
   ```bash
   java -jar keyinterviewtool-0.0.1-SNAPSHOT.jar
   ```
4. Visit `http://localhost:8080` to start the interview on your PC.

# How to Reproduce the Experiments

The experiments follow the four research questions of the paper. Figure, table and appendix numbers refer to the paper.

## RQ1 — Distributions of Minimal Keys

How do the two families and their strategies perform under different distributions of minimal keys, and how many questions does the dualization-based family save on real-world data?

**Synthetic distributions (Fig. 5).** Every question is answered `No` independently with probability *p*, for *p* = 0, 0.1, ..., 1.

```bash
# one strategy per run; underscores stand for the spaces in the strategy string
java exp.SyntheticProbAnswering topdown_bfs out.csv 11 13 15
java exp.SyntheticProbAnswering dualize_topdown_bfs out.csv 11 13 15
```

The driver accepts all eight strategy strings and appends one row per strategy, schema size and *p*. The recorded sweep is `Artifact/results/syn_prob_answering_all_strategies.csv`; it covers |T| = 11, 13 and 15, of which Fig. 5 shows the two extremes.

**Real-world distributions (Tab. 2).** The minimal keys derived from FDs mined from twelve real-world data sets serve as the answering oracle, which makes it feasible to run all eight strategies to completion on schemata where an interview requires tens of thousands of questions.

```bash
# one strategy on one data set
java exp.RWMinedFDKeys run <dataset> <strategy> Artifact/fd <cacheDir>
# or every data set and strategy inline
java exp.RWMinedFDKeys
```

- `Artifact/fd/` holds the mined FDs of the twelve data sets, in the directory layout the driver expects.
- `Artifact/results/rw_mined_fd_5strategies_summary.csv` reports, per data set, the numbers of minimal keys and maximal anti-keys, both question bounds, and for every strategy the number of `No` answers (`_keyQ`), the total number of questions (`_totalQ`) and the computation time in milliseconds (`_ms`).
- `Artifact/results/rw_mined_fd_5strategies_raw.csv` holds the per-run records behind that summary.

## RQ2 — Comparison to Key Mining (Tab. 3)

How large is the gap between keys mined from data, or declared on a schema, and the meaningful keys? The comparison covers the state-of-the-art key mining tool *DataViadotto* and the primary keys declared on the schema, on all 22 tables of the Hockey database. The ground truth of meaningful keys is taken from a profiling study of the Hockey database by DataViadotto, in which every key was validated against the rules of ice hockey and against the documentation of the tables and their columns.

```bash
cd Artifact/comparison_to_mining
rm results/precision_recall_summary.csv   # the scripts append to it
python interview_ground_truth.py          # mined keys against the ground truth
python interview_primary_keys.py          # declared primary keys against the ground truth
```

The folder `Artifact/comparison_to_mining` contains:

- **`dataviadotto_data/`** — the keys mined exactly, and `schema_primary_key.txt`, the primary keys declared on the schema.
- **`dataviadotto_data_1/`, `dataviadotto_data_5/`, `dataviadotto_data_10/`** — the keys mined approximately, with up to 1, 5 and 10% dirtiness.
- **`interview_ground_truth.py`, `interview_primary_keys.py`** — carry the ground truth as their gold standard, label every mined key as a true or false positive and every missed key as a false negative, and compute precision, recall and F1.
- **`results/`** — what the two scripts write: one labeled key list per run and `precision_recall_summary.csv` with one row per run.

The mining outputs are the files DataViadotto wrote, one mined key per line, as `Hockey.<table>[<columns>]`. There is one file per semantics for null markers (`possible`, `certain`) and level of orthogonality (`O0`, `O1`, `O5`, `O9`, the levels L0 to L9 of Tab. 3), and the runs in the summary are named accordingly, from `possible-O0-clean` for exact mining to `certain-O9-10dirt`. Every cell of Tab. 3 is a row of the summary, and the averages in the text are means over its rows; for example, the F1 of 0.513 for exact mining of possible keys with orthogonality is the mean of `possible-O1-clean`, `possible-O5-clean` and `possible-O9-clean`.

## RQ3 — Human Interviewees of Different Expertise (Tabs. 4 and 5)

How much of that gap do human interviewees of different job expertise and experience close with our interviews, individually and as a team? Ten participants in three groups, domain experts, data practitioners and non-specialists, acquired the minimal keys of nine Hockey tables under all eight strategies, using the online interview tool of this repository. The interviews ran on the prime attributes of each table as defined by the ground truth; on the four tables with fewer than 11 columns, every strategy was also run on the full schema with a budget of half an hour of participant time.

Participants took part voluntarily and gave informed consent. They were told in advance what the tool records, that they could stop at any point and decline any individual question, and that their records would be reported only in aggregated and pseudonymized form. **In keeping with that undertaking, this repository publishes no per-participant records.** The aggregated results are Tabs. 4 and 5 of the paper.

## RQ4 — Focusing on Prime Attributes (Tab. 6)

How much does focusing interviews on prime attributes, the attributes that occur in some meaningful minimal key, reduce the effort of an interview, and what does it do to quality? Two LLMs, DS and Qwen, predict the prime attributes of a table and then act as interviewees on the reduced schema, which shrinks the search space from 2^|T| to 2^|P|.

**Hockey tables with ground truth (Tab. 6).** The same script runs the interviews on the full schema and on the predicted prime attributes:

```bash
# --strategies takes any subset of LW-TD LW-TB LW-BD LW-BB DA-TD DA-TB DA-BD DA-BB
python "LLM Oracle Python Script/Interview_with_LLM_Oracle.py" \
    --model deepseek-ai/DeepSeek-R1-Distill-Llama-70B --mode prime \
    --strategies DA-TD DA-TB DA-BD DA-BB --outdir results_da
# --mode full interviews on the full schema
```

**Large schemata without ground truth.** On the twelve real-world data sets of Tab. 2, the interview runs on the predicted prime attributes for every budget |P| = 1, ..., 5. Since no ground truth is available, the experiment reports the number of questions and the runtime only.

```bash
python "LLM Oracle Python Script/Interview_with_LLM_Oracle_No_GT.py" \
    --model deepseek-ai/DeepSeek-R1-Distill-Llama-70B \
    --strategies DA-TD DA-TB DA-BD DA-BB --outdir results_da
```

**Human interviewees.** The prime-filtered and full-schema columns of Tab. 4 come from the study of RQ3.

The folders `Artifact/llm_hockey` and `Artifact/llm_scalability` contain the LLM interview outputs of the two settings, across both models and all strategies; the runs of the dualization-based family are under `dualize/`, with one CSV row per table (or data set and budget) and strategy, the predicted prime attributes as JSON, and the raw model transcript of every run.

## Appendix — LLMs as Domain Experts (App. B.2)

The appendix of the paper reports the LLM interviews in full: Tab. 7 on the full schemata of the Hockey tables, Tab. 8 with prime filtering on the Hockey tables, and Tab. 9 on the twelve data sets without ground truth, for both models and both families. These tables are read from the same outputs as RQ4. Their aggregates (`Agg.` and M*k*) are recomputed by `LLM Oracle Python Script/aggregate_da.py` from the recorded per-strategy rows.

# Where the Reported Numbers Come From

Every number in the paper is read out of a file written by a program, and both the file and the program are in this repository. The one exception is the human study of RQ3, whose per-participant records are withheld for the reason given in its section.

- The **Java drivers** in `src/exp/` run the interview and append one CSV row per run to `Artifact/results/`. A driver either takes the strategy as a command-line argument or iterates over the strategies itself, so in both cases the columns of a table come from one code path, run once per strategy.
- The **LLM scripts** in `LLM Oracle Python Script/` load the model with `transformers` and obtain each answer from `model.generate`; decoding is greedy (`do_sample=False`), so a run is reproducible given the same model and schema. The scripts write one CSV row per table and strategy and, alongside it, the raw transcript of the run, which records the schema, the predicted prime attributes, and the questions, timings and scores of each strategy. The aggregates (`Agg.` and M*k*) are not accumulated during a run; `aggregate_da.py` recomputes them from the recorded per-strategy rows, so they can be checked against those rows.
- The **comparison scripts** in `Artifact/comparison_to_mining/` compute precision, recall and F1 by set arithmetic over the mining outputs in the same folder and the ground truth, which each script carries as its gold standard.

No script states a result as a literal. Anything that looks like a measurement in this repository is either a CSV written by a run, a raw transcript of a run, or an input to a run, such as the mined FDs, the mined keys and the ground truth of meaningful keys.

# Repository Layout

| Path | Contents |
| ---- | -------- |
| `src/entity/` | schema, key and FD data types |
| `src/exp/` | the experiment drivers; `Interview.java` holds the interview loop and the candidate generation of both families |
| `LLM Oracle Python Script/` | the LLM-oracle interview scripts, mirroring the Java strategies |
| `Artifact/results/` | the experiment result CSV files |
| `Artifact/fd/` | FDs mined from the twelve real-world data sets, used as the answering oracle of RQ1 |
| `Artifact/comparison_to_mining/` | the keys mined by DataViadotto, the two comparison scripts of RQ2 with the ground truth, and the results they write |
| `Artifact/llm_hockey/`, `Artifact/llm_scalability/` | the LLM interview outputs of RQ4 and of App. B.2, the dualization-based family under `dualize/` |
| `Artifact/Dataset.zip` | the data sets |
| `Artifact/keyinterviewtool-0.0.1-SNAPSHOT.jar` | the interview tool, implementing all eight strategies |
