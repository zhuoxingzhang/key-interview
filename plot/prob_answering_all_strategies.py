"""Bar chart of the probabilistic-answering experiment for all eight strategies.

Same style as combined_bar_chart.py (grouped bars, logarithmic y axis, bold labels),
laid out as two rows -- the level-wise family on top, the dualize-and-advance family
below -- and one column per schema size. All panels share the y axis, so the height
of a bar can be compared across the two families.

The paper includes the figure in a single column and therefore shows the two extreme
schema sizes only. The sweep itself covers |T| = 11, 13 and 15 and all three sizes
stay in the CSV; pass them on the command line to draw the wide version again:

    python prob_answering_all_strategies.py --sizes 11 13 15 [outfile.png]

Input:  Exp Results New/syn_prob_answering_all_strategies.csv
Output: paper figure  figures/probability exp/alg question/all-strategies,q.png
"""
import argparse
import csv
import os

import matplotlib
matplotlib.use("Agg")
import matplotlib.pyplot as plt
import numpy as np

ROOT = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "..")
SRC = os.path.join(ROOT, "Exp Results New", "syn_prob_answering_all_strategies.csv")
DEFAULT_DST = os.path.join(
    ROOT, "overleaf", "figures", "probability exp", "alg question", "all-strategies,q.png")

FAMILIES = [("LW", "level-wise"), ("DA", "dualize-and-advance")]
SUFFIX = ["TD", "TB", "BD", "BB"]
COLORS = ["#E64B35", "#4DBBD5", "#B09CDB", "#F39C12"]

ap = argparse.ArgumentParser()
ap.add_argument("dst", nargs="?", default=DEFAULT_DST)
ap.add_argument("--sizes", nargs="+", type=int, default=[11, 15])
args = ap.parse_args()
SIZES = args.sizes

# The figure is included at \columnwidth for two sizes and at \textwidth for three,
# so the font size is scaled with the width to render at the same number of points.
PANEL_W = 7.0
FIG_W = PANEL_W * len(SIZES)
FIG_H = 8.6
FS = 25 if len(SIZES) == 2 else 22

# ---------------------------------------------------------------- read the results
q = {}                       # strategy -> schema size -> p -> average number of questions
with open(SRC, encoding="utf-8") as f:
    for row in csv.reader(f):
        if len(row) < 23 or row[0].startswith("#") or row[0] == "strategy":
            continue
        q.setdefault(row[0], {}).setdefault(int(row[2]), {})[round(float(row[3]), 1)] = float(row[22])

missing = [n for n in SIZES if n not in q["LW-TD"]]
assert not missing, "no results for |T| = %s" % missing
ps = sorted(q["LW-TD"][SIZES[0]])
x = np.arange(len(ps))
width = 0.2

# ---------------------------------------------------------------------- draw
fig, axes = plt.subplots(2, len(SIZES), figsize=(FIG_W, FIG_H), sharex=True, sharey=True,
                         layout="constrained", squeeze=False)
for r, (fam, fam_name) in enumerate(FAMILIES):
    for c, n in enumerate(SIZES):
        ax = axes[r][c]
        for k, suf in enumerate(SUFFIX):
            vals = [q[f"{fam}-{suf}"][n][p] for p in ps]
            ax.bar(x + (k - 1.5) * width, vals, width=width, label=suf, color=COLORS[k])
        ax.set_yscale("log")
        ax.set_xlim(-0.6, len(ps) - 0.4)
        ax.grid(True, axis="y", linestyle="--", alpha=0.6)
        ax.set_title(f"{fam_name}, $|T|$ = {n}", fontsize=FS, fontweight="bold", pad=6)
        ax.set_xticks(x[::2])
        ax.set_xticklabels([f"{v:.1f}" for v in ps[::2]])
        ax.tick_params(labelsize=FS)
        for lab in ax.get_xticklabels() + ax.get_yticklabels():
            lab.set_fontweight("bold")
# The panels share both axes, so one label each keeps the narrow figure readable.
fig.supxlabel("Probability of No Answers", fontsize=FS, fontweight="bold")
fig.supylabel("Question Number", fontsize=FS, fontweight="bold")

# One legend for both rows: a colour denotes the same traversal in either family,
# and the family is named in the panel title.
leg = fig.legend(*axes[0][0].get_legend_handles_labels(),
                 prop={"size": FS, "weight": "bold"}, ncol=4, loc="outside upper center",
                 handlelength=1.1, handletextpad=0.4, columnspacing=1.6,
                 handleheight=0.5, labelspacing=0, borderaxespad=0.2, frameon=False)
for h in leg.legend_handles:
    if hasattr(h, "set_height"):
        h.set_height(12)
    if hasattr(h, "set_width"):
        h.set_width(26)

os.makedirs(os.path.dirname(args.dst), exist_ok=True)
fig.savefig(args.dst, dpi=200)
print("wrote %s  (|T| = %s)" % (args.dst, ", ".join(map(str, SIZES))))
