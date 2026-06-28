#!/usr/bin/env python3
"""
migrate_regions.py
Helper script to convert legacy region CSV/JSON into new protected_regions JSON.
Usage: python migrate_regions.py input.json output.json
"""
import sys, json, csv, os

def migrate_from_csv(path):
    regions = []
    with open(path, newline='') as f:
        reader = csv.DictReader(f)
        for row in reader:
            r = {
                "center_x": int(row.get("center_x", 0)),
                "center_z": int(row.get("center_z", 0)),
                "radius_blocks": int(row.get("radius_blocks", 0)),
                "dimension": row.get("dimension", "minecraft:overworld"),
                "deny_message": row.get("deny_message", ""),
                "admin_note": row.get("admin_note", "")
            }
            regions.append(r)
    return regions

def migrate_from_json(path):
    with open(path) as f:
        data = json.load(f)
    regions = []
    for item in data:
        if isinstance(item, dict):
            regions.append({
                "center_x": item.get("center_x", item.get("x", 0)),
                "center_z": item.get("center_z", item.get("z", 0)),
                "radius_blocks": item.get("radius_blocks", item.get("radius", 0)),
                "dimension": item.get("dimension", "minecraft:overworld"),
                "deny_message": item.get("deny_message", ""),
                "admin_note": item.get("admin_note", "")
            })
    return regions

def main():
    if len(sys.argv) < 3:
        print("Usage: migrate_regions.py <input> <output>")
        sys.exit(1)
    inp, out = sys.argv[1], sys.argv[2]
    if inp.lower().endswith(".csv"):
        regions = migrate_from_csv(inp)
    else:
        regions = migrate_from_json(inp)
    with open(out, "w") as f:
        json.dump(regions, f, indent=2)
    print(f"Wrote {len(regions)} regions to {out}")

if __name__ == "__main__":
    main()
