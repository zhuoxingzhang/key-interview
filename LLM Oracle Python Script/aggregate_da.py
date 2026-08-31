"""Derive the Agg. and Mk rows of a family from the recorded per-strategy rows.

The per-strategy CSV that the interview driver appends is the source of truth:
each row carries the minimal keys that one strategy returned, so the aggregates
are a pure function of it and can be recomputed at any time, on any machine,
without a GPU. Running this is idempotent -- it rewrites agg_<mode>_<model>.csv.

    python3 aggregate_da.py [results_da]
"""
import csv
import os
import sys

from Key import Key
from Schema_Info_DB import schema_info_db
from Interview_with_LLM_Oracle import (
    evaluate_keys,
    evaluate_keys_relaxed,
    compute_topk_freq_minimal_keys,
)

STRATEGIES = {"LW-TD", "LW-TB", "LW-BD", "LW-BB", "DA-TD", "DA-TB", "DA-BD", "DA-BB"}
BASE = ("schema", "model", "mode", "T", "P", "prime_f1", "prime_recall", "prime_precision")
FIELDS = list(BASE) + ["strategy", "exact_f1", "exact_recall", "exact_precision",
                       "relaxed_f1", "relaxed_recall", "relaxed_precision",
                       "questions", "minutes", "keys", "members"]


def keys_from_text(text):
    out = []
    for part in (text or "").split(" | "):
        part = part.strip()
        if part:
            out.append(Key([a for a in part.split(",") if a]))
    return out


def keys_to_text(keys):
    return " | ".join(sorted(",".join(sorted(k.getAttributes())) for k in keys))


def aggregate(csv_path, agg_path):
    by_schema, order = {}, []
    with open(csv_path, newline="", encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            if row["strategy"] not in STRATEGIES:
                continue
            if row["schema"] not in by_schema:
                by_schema[row["schema"]] = {}
                order.append(row["schema"])
            by_schema[row["schema"]][row["strategy"]] = row

    with open(agg_path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=FIELDS)
        writer.writeheader()
        for schema_name in order:
            rows = by_schema[schema_name]
            _, gt = schema_info_db(schema_name)
            freq, tq, tm = {}, 0, 0.0
            for strategy in sorted(rows):
                row = rows[strategy]
                for k in keys_from_text(row["keys"]):
                    freq[k] = freq.get(k, 0) + 1
                tq += int(row["questions"] or 0)
                tm += float(row["minutes"] or 0.0)
            any_row = rows[sorted(rows)[0]]
            base = {f: any_row.get(f, "") for f in BASE}
            base["members"] = len(rows)
            if len(rows) != 4:
                print("  ! %-18s aggregates over %d strategies, not 4"
                      % (schema_name, len(rows)))
            if not freq:
                continue
            ks = set(freq)
            p, r, f1 = evaluate_keys(gt, ks)
            pr, rr, f1r = evaluate_keys_relaxed(gt, ks)
            writer.writerow(dict(base, strategy="Agg.",
                                 exact_f1="%.4f" % f1, exact_recall="%.4f" % r,
                                 exact_precision="%.4f" % p,
                                 relaxed_f1="%.4f" % f1r, relaxed_recall="%.4f" % rr,
                                 relaxed_precision="%.4f" % pr,
                                 questions=tq, minutes="%.4f" % tm,
                                 keys=keys_to_text(ks)))
            for k_freq, min_keys in compute_topk_freq_minimal_keys(freq):
                p, r, f1 = evaluate_keys(gt, min_keys)
                pr, rr, f1r = evaluate_keys_relaxed(gt, min_keys)
                writer.writerow(dict(base, strategy="M%d" % k_freq,
                                     exact_f1="%.4f" % f1, exact_recall="%.4f" % r,
                                     exact_precision="%.4f" % p,
                                     relaxed_f1="%.4f" % f1r, relaxed_recall="%.4f" % rr,
                                     relaxed_precision="%.4f" % pr,
                                     questions="", minutes="",
                                     keys=keys_to_text(min_keys)))
            print("  %-18s %d strategies, %4d questions, %8.2f min"
                  % (schema_name, len(rows), tq, tm))


NOGT_FIELDS = ["schema", "model", "T", "strategy", "budgets",
               "mean_questions", "mean_minutes", "questions_per_budget"]


def aggregate_nogt(csv_path, agg_path):
    """Tab. 7: every cell averages one strategy over the budgets |P| = 1..5."""
    cells, order = {}, []
    with open(csv_path, newline="", encoding="utf-8") as fh:
        for row in csv.DictReader(fh):
            if row["strategy"] not in STRATEGIES:
                continue
            key = (row["schema"], row["strategy"])
            if key not in cells:
                cells[key] = {}
                if row["schema"] not in order:
                    order.append(row["schema"])
            # last row of a budget wins, so a duplicated run cannot double count
            cells[key][row["topk"]] = row

    with open(agg_path, "w", newline="", encoding="utf-8") as fh:
        writer = csv.DictWriter(fh, fieldnames=NOGT_FIELDS)
        writer.writeheader()
        for schema_name in order:
            for strategy in sorted(s for (sc, s) in cells if sc == schema_name):
                by_budget = cells[(schema_name, strategy)]
                qs, ms = [], []
                for topk in sorted(by_budget, key=int):
                    qs.append(int(by_budget[topk]["questions"] or 0))
                    ms.append(float(by_budget[topk]["minutes"] or 0.0))
                any_row = by_budget[sorted(by_budget, key=int)[0]]
                if len(qs) != 5:
                    print("  ! %-10s %-6s averages over %d budgets, not 5"
                          % (schema_name, strategy, len(qs)))
                writer.writerow({
                    "schema": schema_name, "model": any_row["model"],
                    "T": any_row["T"], "strategy": strategy, "budgets": len(qs),
                    "mean_questions": "%.1f" % (sum(qs) / len(qs)),
                    "mean_minutes": "%.2f" % (sum(ms) / len(ms)),
                    "questions_per_budget": " ".join(str(q) for q in qs),
                })
            print("  %-10s %d strategies" % (
                schema_name, len({s for (sc, s) in cells if sc == schema_name})))


def main():
    outdir = sys.argv[1] if len(sys.argv) > 1 else "results_da"
    found = 0
    for name in sorted(os.listdir(outdir)):
        if not name.endswith(".csv"):
            continue
        if name.startswith("prime_") or name.startswith("full_"):
            fn = aggregate
        elif name.startswith("nogt_"):
            fn = aggregate_nogt
        else:
            continue
        src = os.path.join(outdir, name)
        dst = os.path.join(outdir, "agg_" + name)
        print(src)
        fn(src, dst)
        print("  -> " + dst)
        found += 1
    if not found:
        print("no prime_*.csv, full_*.csv or nogt_*.csv in " + outdir)


if __name__ == "__main__":
    main()
