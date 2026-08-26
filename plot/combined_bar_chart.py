import matplotlib.pyplot as plt
import numpy as np

# 输入数据（从文本中复制）
data_text = """
p (for NO)	TD #Q	TB #Q	BD #Q	BB #Q
0	1	1	12	2048
0.1	4.1435	2.1645	16.088	220.1225
0.2	19.4075	3.641	20.025	85.21
0.3	31.4645	6.024	23.6855	43.4425
0.4	30.829	9.305	27.391	24.312
0.5	32.247	15.221	31.839	14.9185
0.6	27.2135	24.053	34.2055	8.899
0.7	23.508	42.942	29.9455	5.9695
0.8	19.714	88.413	18.2575	3.592
0.9	15.9605	212.7395	5.9965	2.0905
1	12	2047	1	1
"""

# 解析数据
lines = data_text.strip().splitlines()
header = lines[0].split()
rows = [list(map(float, line.split())) for line in lines[1:]]
colors = ["#E64B35", "#4DBBD5", "#B09CDB", "#F39C12"]
# colors = ["#D73027", "#1A9850", "#4575B4", "#F46D43"]



p = [r[0] for r in rows]
TD = [r[1] for r in rows]
TB = [r[2] for r in rows]
BD = [r[3] for r in rows]
BB = [r[4] for r in rows]

# 设置宽度和位置
x = np.arange(len(p))
bar_width = 0.2

# 绘图
plt.figure(figsize=(10, 6))
# plt.bar(x - 1.5*bar_width, TD, width=bar_width, label="TD", color="#5B9BD5")  # 蓝
# plt.bar(x - 0.5*bar_width, TB, width=bar_width, label="TB", color="#ED7D31")  # 橙
# plt.bar(x + 0.5*bar_width, BD, width=bar_width, label="BD", color="#A5A5A5")  # 灰
# plt.bar(x + 1.5*bar_width, BB, width=bar_width, label="BB", color="#FFC000")  # 黄
plt.bar(x - 1.5*bar_width, TD, width=bar_width, label="TD", color=colors[0])
plt.bar(x - 0.5*bar_width, TB, width=bar_width, label="TB", color=colors[1])
plt.bar(x + 0.5*bar_width, BD, width=bar_width, label="BD", color=colors[2])
plt.bar(x + 1.5*bar_width, BB, width=bar_width, label="BB", color=colors[3])

# Y轴为对数坐标
plt.yscale('log')

# 坐标轴标签
plt.xlabel("Probability of No Answers", fontsize=30, fontweight='bold')
plt.ylabel("Question Number", fontsize=30, fontweight='bold')

# 坐标轴刻度
plt.xticks(x[::2], [f"{v:.1f}" for v in p[::2]], fontsize=30, fontweight='bold')
plt.yticks(fontsize=30, fontweight='bold')

# 网格线
plt.grid(True, axis='y', linestyle="--", alpha=0.6)

# 图例
# plt.legend(prop={'size': 20}, ncol=4, loc='upper center', bbox_to_anchor=(0.5, 1))
leg = plt.legend(
    prop={'size': 30, 'weight': 'bold'},              # 只放大字体
    ncol=4,                         # 横着放
    loc='upper center',
    bbox_to_anchor=(0.5, 1),
    handlelength=1.0,               # 图例前小块长度（默认2）
    handletextpad=0.4,              # 小块和文字的间距
    columnspacing=0.6,              # 各列之间的间距
    handleheight=0.4,   # 控制方块垂直居中
    labelspacing=0,   # 调整文字与方块上下间距
    borderaxespad=0.2               # legend 与图边界的间距
)

# 避免 legend 矩形被放大
for handle in leg.legend_handles:
    if hasattr(handle, 'set_linewidth'):
        handle.set_linewidth(2)     # 折线图线宽
    if hasattr(handle, 'set_height'):
        handle.set_height(14)        # 柱状图矩形高度
    if hasattr(handle, 'set_width'):
        handle.set_width(32)        # 柱状图矩形宽度


plt.tight_layout()
plt.show()
