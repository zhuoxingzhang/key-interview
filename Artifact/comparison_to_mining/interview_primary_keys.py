import re
import os
import csv
from typing import Set, Tuple, FrozenSet, List

# ============================================================
# Type definition
# ============================================================
Key = Tuple[str, FrozenSet[str]]  # (table, frozenset(columns))


# ============================================================
# Normalization helpers
# ============================================================
def norm_table(t: str) -> str:
    return t.strip().replace("Hockey.", "")


def norm_col(c: str) -> str:
    return c.strip()


def key_to_line(k: Key) -> str:
    table, cols = k
    return f"{table}[{','.join(sorted(cols))}]"


# ============================================================
# Parse predicted keys (KEEP ORDER)
# ============================================================
def parse_found_file_with_order(path: str) -> List[Key]:
    """
    Parse lines like:
    Hockey.Table[col1,col2,...]
    Keep the original order.
    """
    pat = re.compile(r"^\s*(?P<table>[\w\.]+)\s*\[(?P<cols>[^\]]+)\]\s*$")
    keys: List[Key] = []

    with open(path, "r", encoding="utf-8") as f:
        for line in f:
            raw = line.strip()
            if not raw:
                continue
            m = pat.match(raw)
            if not m:
                continue
            table = norm_table(m.group("table"))
            cols = [norm_col(x) for x in m.group("cols").split(",") if x.strip()]
            keys.append((table, frozenset(cols)))

    return keys


# ============================================================
# Parse gold standard from LaTeX
# ============================================================
def parse_gold_from_latex(latex: str) -> Set[Key]:
    """
    Parse rows like:
    Table & col1, col2, ... & UR & CR \\
    """
    keys: Set[Key] = set()
    row_pat = re.compile(
        r"^\s*([A-Za-z0-9_]+)\s*&\s*([^&]+?)\s*&\s*[\d\.]+\s*&\s*[\d\.]+\s*\\\\"
    )

    for line in latex.splitlines():
        line = line.strip()
        m = row_pat.match(line)
        if not m:
            continue
        table = norm_table(m.group(1))
        cols = [norm_col(x) for x in m.group(2).split(",") if x.strip()]
        keys.add((table, frozenset(cols)))

    return keys


# ============================================================
# Precision / Recall / F1
# ============================================================
def precision_recall_f1(pred: Set[Key], gold: Set[Key]):
    tp = pred & gold
    fp = pred - gold
    fn = gold - pred

    precision = len(tp) / len(pred) if pred else 0.0
    recall = len(tp) / len(gold) if gold else 0.0
    f1 = (2 * precision * recall / (precision + recall)) if (precision + recall) else 0.0

    return tp, fp, fn, precision, recall, f1


# ============================================================
# Export SINGLE labeled file
# ============================================================
def export_single_labeled_file(
    ordered_pred: List[Key],
    gold: Set[Key],
    out_path: str
):
    pred_set = set(ordered_pred)
    os.makedirs(os.path.dirname(out_path), exist_ok=True)

    with open(out_path, "w", encoding="utf-8") as f:
        # TP / FP in discovery order
        for k in ordered_pred:
            label = "TP" if k in gold else "FP"
            f.write(f"{key_to_line(k)}\t{label}\n")

        # FN appended
        f.write("\n# ---- Missing gold keys (FN) ----\n")
        for k in sorted(gold - pred_set):
            f.write(f"{key_to_line(k)}\tFN\n")


# ============================================================
# Append results to CSV summary
# ============================================================
def append_to_csv(
    csv_path: str,
    experiment: str,
    tp: int,
    fp: int,
    fn: int,
    precision: float,
    recall: float,
    f1: float
):
    file_exists = os.path.isfile(csv_path)
    os.makedirs(os.path.dirname(csv_path), exist_ok=True)

    with open(csv_path, "a", newline="", encoding="utf-8") as f:
        writer = csv.writer(f)

        # write header once
        if not file_exists:
            writer.writerow([
                "experiment",
                "TP",
                "FP",
                "TN",
                "FN",
                "Precision",
                "Recall",
                "F1"
            ])

        writer.writerow([
            experiment,
            tp,
            fp,
            -1,   # TN not enumerable in key discovery
            fn,
            f"{precision:.4f}",
            f"{recall:.4f}",
            f"{f1:.4f}"
        ])


# ============================================================
# Main (BATCH EXECUTION)
# ============================================================
if __name__ == "__main__":

    # --------------------------------------------------------
    # Gold Standard (LaTeX)
    # --------------------------------------------------------
    GOLD_LATEX = r"""
abbrev & Type, Code & 1.00 & 1.00\\ 
abbrev & Fullname & 1.00 & 1.00\\
AwardsCoaches & coachID, year & 1.00 & 1.00\\
AwardsCoaches & award, year & 1.00 & 1.00\\
AwardsMisc & name & 1.00 & 1.00\\
AwardsMisc & ID & 1.00 & 0.69\\
AwardsPlayers & playerID, award, year & 1.00 & 1.00\\
Coaches & coachID, year, stint & 1.00 & 1.00\\
CombinedShutouts & year, month, date, tmID & 1.00 & 1.00\\
Goalies & playerID, year, stint & 1.00 & 1.00\\
GoaliesSC & playerID, year & 1.00 & 1.00\\
GoaliesShootout & playerID, year, stint & 1.00 & 1.00\\
HOF & hofID & 1.00 & 1.00\\
HOF & year, name & 1.00 & 1.00\\
Master & playerID & 1.00 & 0.97\\
Master & legendsID & 1.00 & 0.84\\
Master & hrefID & 1.00 & 0.96\\
Master & ihdbID & 1.00 & 0.91\\
Master & lastName, nameGiven, birthYear & 1.00 & 0.76\\
Scoring & playerID, year, stint & 0.99 & 1.00\\
ScoringSC & playerID, year & 1.00 & 1.00 \\
ScoringShootout & playerID, year, stint & 1.00 & 1.00 \\
ScoringShootout & playerID, year, tmID & 1.00 & 1.00 \\
ScoringSup & playerID, year & 1.00 & 1.00 \\
SeriesPost & year, tmIDWinner, tmIDLoser & 1.00 & 1.00 \\
SeriesPost & year, series & 1.00 & 0.89\\
SeriesPost & year, round, tmIDWinner & 1.00 & 1.00\\
SeriesPost & year, tmIDLoser & 0.99 & 1.00\\
Teams & year, tmID & 1.00 & 1.00 \\
Teams & year, franchID & 1.00 & 1.00 \\
Teams & year, name & 1.00 & 1.00 \\
TeamsHalf & year, tmID, half & 1.00 & 1.00 \\
TeamsHalf & year, half, rank & 1.00 & 1.00 \\
TeamSplits & year, tmID & 1.00 & 1.00\\
TeamsPost & year, tmID & 1.00 & 1.00\\
TeamsSC & year, tmID & 1.00 & 1.00 \\
TeamsSC & year, lgID & 1.00 & 1.00 \\
TeamVsTeam & year, tmID, oppID & 1.00 & 1.00\\
"""

    # --------------------------------------------------------
    # Batch tasks (exactly matching your directory)
    # --------------------------------------------------------
    TASKS_PRIMARYKEY = [
        ("primary key", "dataviadotto_data/schema_primary_key.txt",
         "results/schema_primary_key_labeled.txt")
    ]

    TASKS = (
        TASKS_PRIMARYKEY
    )

    SUMMARY_CSV = "results/precision_recall_summary.csv"

    gold_keys = parse_gold_from_latex(GOLD_LATEX)

    for experiment, found_path, out_path in TASKS:
        ordered_pred = parse_found_file_with_order(found_path)
        pred_set = set(ordered_pred)

        tp, fp, fn, p, r, f1 = precision_recall_f1(pred_set, gold_keys)

        export_single_labeled_file(ordered_pred, gold_keys, out_path)

        append_to_csv(
            csv_path=SUMMARY_CSV,
            experiment=experiment,
            tp=len(tp),
            fp=len(fp),
            fn=len(fn),
            precision=p,
            recall=r,
            f1=f1
        )

        print(f"[{experiment}] Precision={p:.4f}, Recall={r:.4f}, F1={f1:.4f}")
