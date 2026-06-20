#!/usr/bin/env python3
"""Murim Block local production helper.

This tool keeps Codex, MCreator, Blockbench exports, assets, Gradle and Git
pointing at the same project reality.
"""

from __future__ import annotations

import argparse
import json
import os
import shutil
import subprocess
import sys
from collections import Counter, defaultdict
from pathlib import Path


ROOT = Path(__file__).resolve().parents[1]
CONFIG_PATH = Path(__file__).with_name("murim_tool.config.json")


def load_config() -> dict:
    if CONFIG_PATH.exists():
        return json.loads(CONFIG_PATH.read_text(encoding="utf-8"))
    return {}


CONFIG = load_config()


def run(cmd: list[str], cwd: Path = ROOT, check: bool = False) -> subprocess.CompletedProcess:
    return subprocess.run(cmd, cwd=cwd, text=True, capture_output=True, check=check)


def rel(path: Path) -> str:
    try:
        return path.resolve().relative_to(ROOT.resolve()).as_posix()
    except ValueError:
        return str(path)


def read_json(path: Path) -> dict:
    with path.open("r", encoding="utf-8") as handle:
        return json.load(handle)


def git_summary() -> dict:
    branch = run(["git", "rev-parse", "--abbrev-ref", "HEAD"])
    head = run(["git", "rev-parse", "--short", "HEAD"])
    upstream = run(["git", "rev-parse", "--abbrev-ref", "--symbolic-full-name", "@{u}"])
    remote = run(["git", "remote", "-v"])
    status = run(["git", "status", "--short", "--branch"])

    changed = [
        line for line in status.stdout.splitlines()
        if line and not line.startswith("## ")
    ]

    return {
        "branch": branch.stdout.strip() if branch.returncode == 0 else "unknown",
        "head": head.stdout.strip() if head.returncode == 0 else "unknown",
        "upstream": upstream.stdout.strip() if upstream.returncode == 0 else "none",
        "remote": remote.stdout.strip().splitlines(),
        "changed_files": len(changed),
        "status": status.stdout.strip().splitlines(),
    }


def load_mcreator_elements() -> list[dict]:
    workspace = ROOT / "murim_block.mcreator"
    if not workspace.exists():
        return []
    data = read_json(workspace)
    return data.get("mod_elements", [])


def element_details() -> list[dict]:
    elements = []
    for entry in load_mcreator_elements():
        name = entry.get("name", "")
        element_path = ROOT / "elements" / f"{name}.mod.json"
        detail = {}
        if element_path.exists():
            try:
                detail = read_json(element_path)
            except json.JSONDecodeError:
                detail = {"_parse_error": True}
        definition = detail.get("definition", {}) if isinstance(detail, dict) else {}
        components = definition.get("components", [])
        elements.append({
            "name": name,
            "type": entry.get("type", "unknown"),
            "path": entry.get("path", ""),
            "registry_name": entry.get("registry_name", ""),
            "compiles": entry.get("compiles"),
            "locked_code": entry.get("locked_code"),
            "files": entry.get("metadata", {}).get("files", []),
            "element_file": rel(element_path) if element_path.exists() else "",
            "gui_components": len(components) if isinstance(components, list) else 0,
            "mob_model": definition.get("mobModelName", ""),
            "mob_texture": definition.get("mobModelTexture", ""),
        })
    return elements


def scan_assets() -> dict:
    asset_root = ROOT / "src" / "main" / "resources" / "assets" / "murim_block"
    groups = {
        "item_textures": asset_root / "textures" / "item",
        "block_textures": asset_root / "textures" / "block",
        "entity_textures": asset_root / "textures" / "entities",
        "screen_textures": asset_root / "textures" / "screens",
        "custom_models": asset_root / "models" / "custom",
        "item_models": asset_root / "models" / "item",
    }
    result = {}
    for name, folder in groups.items():
        if folder.exists():
            result[name] = sorted(rel(path) for path in folder.rglob("*") if path.is_file())
        else:
            result[name] = []
    return result


def find_blockbench_projects() -> list[str]:
    configured = [Path(p).expanduser() for p in CONFIG.get("blockbench_search_roots", [])]
    roots = [path for path in configured if path.exists()]
    patterns = {".bbmodel", ".bbproject", ".geo.json"}
    found = []
    for root in roots:
        for path in root.rglob("*"):
            if path.is_file() and path.suffix.lower() in patterns:
                found.append(str(path))
    return sorted(found)


def command_status(_: argparse.Namespace) -> int:
    git = git_summary()
    elements = element_details()
    types = Counter(item["type"] for item in elements)
    assets = scan_assets()
    blockbench = find_blockbench_projects()

    print("Murim Block production status")
    print("=" * 31)
    print(f"Workspace: {ROOT}")
    print(f"Git: {git['branch']} @ {git['head']} -> {git['upstream']}")
    print(f"Changed files: {git['changed_files']}")
    print(f"Elements: {len(elements)}")
    for key, value in sorted(types.items()):
        print(f"  - {key}: {value}")
    print("Assets:")
    for key, value in assets.items():
        print(f"  - {key}: {len(value)}")
    print(f"Blockbench projects found: {len(blockbench)}")
    if git["changed_files"]:
        print()
        print("Git status:")
        for line in git["status"][:80]:
            print(line)
        if len(git["status"]) > 80:
            print(f"... {len(git['status']) - 80} more lines")
    return 0


def command_elements(args: argparse.Namespace) -> int:
    elements = element_details()
    if args.type:
        elements = [item for item in elements if item["type"].lower() == args.type.lower()]

    folders = defaultdict(list)
    for item in elements:
        folders[item["path"]].append(item)

    for folder, items in sorted(folders.items()):
        print(folder or "(no folder)")
        for item in sorted(items, key=lambda value: value["name"].lower()):
            suffix = []
            if item["mob_model"]:
                suffix.append(f"model={item['mob_model']}")
            if item["gui_components"]:
                suffix.append(f"components={item['gui_components']}")
            detail = f" ({', '.join(suffix)})" if suffix else ""
            print(f"  - {item['name']} [{item['type']}]{detail}")
    return 0


def command_assets(_: argparse.Namespace) -> int:
    assets = scan_assets()
    for group, paths in assets.items():
        print(group)
        for path in paths:
            print(f"  - {path}")
    return 0


def command_blockbench(_: argparse.Namespace) -> int:
    found = find_blockbench_projects()
    if not found:
        print("No Blockbench project/export files found in configured search roots.")
        print("Edit tools/murim_tool.config.json to add more folders.")
        return 0
    for path in found:
        print(path)
    return 0


def command_report(args: argparse.Namespace) -> int:
    output = ROOT / args.output
    git = git_summary()
    elements = element_details()
    types = Counter(item["type"] for item in elements)
    assets = scan_assets()
    blockbench = find_blockbench_projects()

    lines = [
        "# Murim Block Auto Report",
        "",
        f"- Workspace: `{ROOT}`",
        f"- Git: `{git['branch']}` at `{git['head']}` tracking `{git['upstream']}`",
        f"- Changed files: `{git['changed_files']}`",
        f"- Elements: `{len(elements)}`",
        f"- Blockbench files found: `{len(blockbench)}`",
        "",
        "## Element Types",
        "",
    ]
    lines.extend(f"- {key}: {value}" for key, value in sorted(types.items()))
    lines.extend(["", "## Asset Counts", ""])
    lines.extend(f"- {key}: {len(value)}" for key, value in assets.items())
    lines.extend(["", "## Priority Production Notes", ""])
    lines.extend([
        "- Keep every technique readable in 1 second: clear cast, clear hit, clear cooldown.",
        "- Every mob needs model, texture, spawn behavior, combat role, drops, and test status.",
        "- Every GUI needs locked/unlocked states, hover/click feedback, and in-game screenshot QA.",
        "- Before push: run status, build/check, then commit with a human-readable milestone message.",
    ])
    output.write_text("\n".join(lines) + "\n", encoding="utf-8")
    print(f"Wrote {rel(output)}")
    return 0


def command_backup(args: argparse.Namespace) -> int:
    backup_root = ROOT.parent / "_murim_backups"
    backup_root.mkdir(exist_ok=True)
    target = backup_root / args.name
    if target.exists():
        print(f"Backup already exists: {target}", file=sys.stderr)
        return 2
    ignore = shutil.ignore_patterns(".git", ".gradle", "build", "run")
    shutil.copytree(ROOT, target, ignore=ignore)
    print(f"Created backup: {target}")
    return 0


def slugify(value: str) -> str:
    allowed = []
    last_dash = False
    for char in value.lower():
        if char.isalnum():
            allowed.append(char)
            last_dash = False
        elif not last_dash:
            allowed.append("-")
            last_dash = True
    return "".join(allowed).strip("-") or "unnamed"


def write_spec(kind: str, name: str) -> Path:
    folder = ROOT / "production" / kind
    folder.mkdir(parents=True, exist_ok=True)
    path = folder / f"{slugify(name)}.md"
    if path.exists():
        raise FileExistsError(path)

    if kind == "techniques":
        body = f"""# {name}

## Fantasy

- Murim identity:
- Player feeling:
- Reference intensity target:

## Gameplay

- Unlock condition:
- Qi cost:
- Cooldown:
- Input/keybind/menu access:
- Targeting:
- Damage/effect:
- Failure state:

## Visuals

- Cast-up:
- Active effect:
- Hit impact:
- Aftermath:
- Sound notes:
- Screen/HUD feedback:

## MCreator Work

- Elements to create/update:
- Procedures:
- GUI/overlay:
- Advancements:
- Assets:

## Test Checklist

- Works when unlocked:
- Fails clearly when locked:
- Qi/cooldown correct:
- Multiplayer risk checked:
- In-game screenshot/video captured:
- Commit pushed:
"""
    else:
        body = f"""# {name}

## Fantasy

- Murim role:
- Silhouette:
- Biome/structure context:

## Blockbench

- Model file:
- Texture file:
- Animation notes:
- Export path:

## MCreator

- Entity element:
- AI/combat role:
- Health/damage/speed:
- Spawn conditions:
- Loot:
- Sounds:

## Quality Checklist

- Recognizable at distance:
- Texture readable in motion:
- Hitbox feels right:
- Has purpose in progression:
- Tested in-game:
- Commit pushed:
"""

    path.write_text(body, encoding="utf-8")
    return path


def command_new_technique(args: argparse.Namespace) -> int:
    try:
        path = write_spec("techniques", args.name)
    except FileExistsError as exc:
        print(f"Spec already exists: {rel(Path(exc.filename))}", file=sys.stderr)
        return 2
    print(f"Created {rel(path)}")
    return 0


def command_new_mob(args: argparse.Namespace) -> int:
    try:
        path = write_spec("mobs", args.name)
    except FileExistsError as exc:
        print(f"Spec already exists: {rel(Path(exc.filename))}", file=sys.stderr)
        return 2
    print(f"Created {rel(path)}")
    return 0


def build_parser() -> argparse.ArgumentParser:
    parser = argparse.ArgumentParser(description="Murim Block production helper")
    sub = parser.add_subparsers(dest="command", required=True)

    sub.add_parser("status", help="Show Git, MCreator, asset and Blockbench status").set_defaults(func=command_status)

    elements = sub.add_parser("elements", help="List MCreator elements")
    elements.add_argument("--type", help="Filter by MCreator element type")
    elements.set_defaults(func=command_elements)

    sub.add_parser("assets", help="List known mod assets").set_defaults(func=command_assets)
    sub.add_parser("blockbench", help="Find Blockbench projects/exports").set_defaults(func=command_blockbench)

    report = sub.add_parser("report", help="Write a markdown project report")
    report.add_argument("--output", default="MURIM_AUTO_REPORT.md")
    report.set_defaults(func=command_report)

    backup = sub.add_parser("backup", help="Create a lightweight workspace backup")
    backup.add_argument("name", help="Backup folder name, for example before-big-technique-pass")
    backup.set_defaults(func=command_backup)

    technique = sub.add_parser("new-technique", help="Create a production spec for a technique")
    technique.add_argument("name")
    technique.set_defaults(func=command_new_technique)

    mob = sub.add_parser("new-mob", help="Create a production spec for a mob")
    mob.add_argument("name")
    mob.set_defaults(func=command_new_mob)

    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
