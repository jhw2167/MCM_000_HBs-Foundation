"""
Parses HB's Foundation ChunkAnalyzer log output ("Block counts for chunk ...") into CSV summaries.

Usage:
    python chunk_analyzer_to_csv.py latest.log [more.log ...] [-o output_dir]

Writes to output_dir (default: ./chunk_analysis):
    ore_by_biome.csv        ore rates per biome, weighted by block counts
    ore_by_biome_pivot.csv  biome rows x ore columns, % of host stone/deepslate
    block_totals.csv        every block summed across all sampled chunks
    structures.csv          how often each structure appears and in which biomes
    biomes.csv              biome distribution and per-biome air and top blocks

A chunk logged more than once only counts its last report.
"""

import argparse
import csv
import os
import re
from collections import Counter, defaultdict

HEADER = re.compile(r"Block counts for chunk (-?\d+), (-?\d+)(?: \(radius (\d+), (\d+) loaded chunks\))?:")
ROW = re.compile(r"^\s+-\s+(?P<name>.+?)(?::\s+(?P<value>.*)| -> (?P<stats>.+))$")
ORE_SHARE = re.compile(r"(?P<pct>[\d.,]+)% of (?P<host>\w+)")
AIR_ROWS = ("Air", "Sky Air", "Cave Air")
HOSTS = {"stone": "minecraft:stone", "deepslate": "minecraft:deepslate"}


def to_int(text):
    return int(re.sub(r"[^\d]", "", text))


def to_float(text):
    return float(text.strip().rstrip("%").replace(",", "."))


def parse_reports(paths):
    """Returns one dict per chunk report, keeping the last report for each chunk and radius."""
    reports = {}
    for path in paths:
        with open(path, encoding="utf-8", errors="replace") as log:
            current = None
            for line in log:
                header = HEADER.search(line)
                if header:
                    x, z, radius, loaded = header.groups()
                    current = {
                        "x": int(x), "z": int(z),
                        "radius": int(radius or 0),
                        "chunks": int(loaded or 1),
                        "biome": "unknown", "structures": [],
                        "air": {}, "blocks": {}, "ores": {},
                    }
                    reports[(current["x"], current["z"], current["radius"])] = current
                    continue

                row = ROW.match(line) if current else None
                if not row:
                    current = None
                    continue

                name = row.group("name")
                if row.group("value") is not None:
                    value = row.group("value").strip()
                    if name == "Biome":
                        current["biome"] = value
                    elif name == "Structures":
                        inner = value.strip("[]").strip()
                        current["structures"] = [s.strip() for s in inner.split(", ")] if inner else []
                    continue

                fields = row.group("stats").split(", ")
                count, percent = to_int(fields[0]), to_float(fields[2])
                if name in AIR_ROWS:
                    current["air"][name] = (count, percent)
                    if name == "Air" and percent > 0:
                        current["volume"] = round(count / (percent / 100))
                    continue

                current["blocks"][name] = count
                share = ORE_SHARE.search(row.group("stats"))
                if share:
                    current["ores"][name] = share.group("host")
    return list(reports.values())


def write_csv(path, header, rows):
    with open(path, "w", newline="", encoding="utf-8") as out:
        writer = csv.writer(out)
        writer.writerow(header)
        writer.writerows(rows)


def pct(part, whole):
    return round(100 * part / whole, 5) if whole else ""


def write_ore_by_biome(reports, out_dir):
    stats = defaultdict(lambda: {"chunks": 0, "with_ore": 0, "count": 0, "host_count": 0, "volume": 0, "host": ""})
    biome_chunks = Counter()
    biome_volume = Counter()
    biome_host = defaultdict(Counter)
    all_ores = {}

    for r in reports:
        biome_chunks[r["biome"]] += r["chunks"]
        biome_volume[r["biome"]] += r.get("volume", 0)
        for host, block in HOSTS.items():
            biome_host[r["biome"]][host] += r["blocks"].get(block, 0)
        for ore, host in r["ores"].items():
            all_ores[ore] = host

    for r in reports:
        for ore, host in all_ores.items():
            s = stats[(r["biome"], ore)]
            s["host"] = host
            count = r["blocks"].get(ore, 0)
            s["count"] += count
            s["with_ore"] += r["chunks"] if count else 0

    rows = []
    for (biome, ore), s in sorted(stats.items()):
        host_count = biome_host[biome][s["host"]]
        chunks = biome_chunks[biome]
        rows.append([
            biome, ore, s["host"], chunks, s["with_ore"], s["count"], host_count,
            pct(s["count"], host_count), pct(s["count"], biome_volume[biome]),
            round(s["count"] / chunks, 2) if chunks else "",
        ])
    write_csv(os.path.join(out_dir, "ore_by_biome.csv"),
              ["biome", "ore", "host", "chunks_sampled", "chunks_with_ore", "ore_count", "host_count",
               "pct_of_host", "pct_of_volume", "avg_per_chunk"], rows)

    ores = sorted(all_ores)
    pivot = []
    for biome in sorted(biome_chunks):
        line = [biome, biome_chunks[biome]]
        for ore in ores:
            host_count = biome_host[biome][all_ores[ore]]
            line.append(pct(stats[(biome, ore)]["count"], host_count))
        pivot.append(line)
    write_csv(os.path.join(out_dir, "ore_by_biome_pivot.csv"), ["biome", "chunks_sampled"] + ores, pivot)


def write_block_totals(reports, out_dir):
    totals, present = Counter(), Counter()
    volume = sum(r.get("volume", 0) for r in reports)
    chunks = sum(r["chunks"] for r in reports)

    for r in reports:
        for name, (count, _) in r["air"].items():
            totals[name] += count
            present[name] += r["chunks"] if count else 0
        for block, count in r["blocks"].items():
            totals[block] += count
            present[block] += r["chunks"]

    rows = [[name, count, present[name], pct(present[name], chunks), pct(count, volume),
             round(count / chunks, 2) if chunks else ""]
            for name, count in totals.most_common()]
    write_csv(os.path.join(out_dir, "block_totals.csv"),
              ["block", "total_count", "chunks_present", "pct_of_chunks", "pct_of_volume", "avg_per_chunk"], rows)


def write_structures(reports, out_dir):
    chunks = sum(r["chunks"] for r in reports)
    seen, biomes = Counter(), defaultdict(Counter)
    for r in reports:
        for structure in r["structures"]:
            seen[structure] += r["chunks"]
            biomes[structure][r["biome"]] += r["chunks"]

    rows = []
    for structure, count in seen.most_common():
        top = "; ".join(f"{b} ({n})" for b, n in biomes[structure].most_common(5))
        rows.append([structure, count, pct(count, chunks), len(biomes[structure]), top])
    write_csv(os.path.join(out_dir, "structures.csv"),
              ["structure", "chunks_with_structure", "pct_of_chunks", "distinct_biomes", "top_biomes"], rows)


def write_biomes(reports, out_dir):
    total_chunks = sum(r["chunks"] for r in reports)
    per_biome = defaultdict(list)
    for r in reports:
        per_biome[r["biome"]].append(r)

    rows = []
    for biome, group in sorted(per_biome.items(), key=lambda kv: -sum(r["chunks"] for r in kv[1])):
        chunks = sum(r["chunks"] for r in group)
        volume = sum(r.get("volume", 0) for r in group)
        air = {name: sum(r["air"].get(name, (0, 0))[0] for r in group) for name in AIR_ROWS}
        blocks = Counter()
        structures = Counter()
        for r in group:
            blocks.update({b: c for b, c in r["blocks"].items() if b != "minecraft:air"})
            structures.update(r["structures"])
        top_blocks = "; ".join(f"{b} ({pct(c, volume)}%)" for b, c in blocks.most_common(5))
        top_structures = "; ".join(f"{s} ({n})" for s, n in structures.most_common(5))
        rows.append([biome, chunks, pct(chunks, total_chunks), pct(air["Air"], volume),
                     pct(air["Sky Air"], volume), pct(air["Cave Air"], volume), top_blocks, top_structures])
    write_csv(os.path.join(out_dir, "biomes.csv"),
              ["biome", "chunks_sampled", "pct_of_chunks", "air_pct", "sky_air_pct", "cave_air_pct",
               "top_blocks", "top_structures"], rows)


def main():
    parser = argparse.ArgumentParser(description="Convert ChunkAnalyzer log output to CSV summaries.")
    parser.add_argument("logs", nargs="+", help="log files containing ChunkAnalyzer output")
    parser.add_argument("-o", "--out", default="chunk_analysis", help="output folder (default: chunk_analysis)")
    args = parser.parse_args()

    reports = parse_reports(args.logs)
    if not reports:
        print("No ChunkAnalyzer reports found.")
        return

    os.makedirs(args.out, exist_ok=True)
    write_ore_by_biome(reports, args.out)
    write_block_totals(reports, args.out)
    write_structures(reports, args.out)
    write_biomes(reports, args.out)
    chunks = sum(r["chunks"] for r in reports)
    print(f"Parsed {len(reports)} reports covering {chunks} chunks into {os.path.abspath(args.out)}")


if __name__ == "__main__":
    main()
