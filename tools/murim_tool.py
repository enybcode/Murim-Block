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
import struct
import zlib
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


def bbmodel_summary(path: Path) -> dict:
    data = read_json(path)
    groups = data.get("groups", [])
    animations = data.get("animations", [])
    return {
        "path": str(path),
        "name": data.get("name", path.stem),
        "model_format": data.get("meta", {}).get("model_format", "unknown"),
        "format_version": data.get("meta", {}).get("format_version", "unknown"),
        "elements": len(data.get("elements", [])),
        "groups": [group.get("name", "") for group in groups],
        "group_count": len(groups),
        "animations": [animation.get("name", "") for animation in animations],
        "animation_count": len(animations),
        "resolution": data.get("resolution", {}),
    }


def command_bbmodel_info(args: argparse.Namespace) -> int:
    path = Path(args.path).expanduser()
    if not path.exists():
        print(f"File not found: {path}", file=sys.stderr)
        return 2
    summary = bbmodel_summary(path)
    print(f"Blockbench model: {summary['name']}")
    print(f"Path: {summary['path']}")
    print(f"Format: {summary['model_format']} ({summary['format_version']})")
    print(f"Elements: {summary['elements']}")
    print(f"Groups: {summary['group_count']}")
    for group in summary["groups"]:
        print(f"  - {group}")
    print(f"Animations: {summary['animation_count']}")
    for animation in summary["animations"]:
        print(f"  - {animation}")
    return 0


def keyframes(*items: tuple[float, list[float]]) -> dict:
    return {f"{time:.2f}".rstrip("0").rstrip("."): value for time, value in items}


def build_raikan_animation_pack(model_path: Path) -> dict:
    summary = bbmodel_summary(model_path)
    groups = set(summary["groups"])

    def keep_bones(bones: dict) -> dict:
        return {name: channels for name, channels in bones.items() if name in groups}

    animations = {
        "animation.steel_fanged_raikan.idle": {
            "loop": "loop",
            "animation_length": 2.0,
            "bones": keep_bones({
                "Body": {
                    "position": keyframes((0, [0, 0, 0]), (1, [0, 0.35, 0]), (2, [0, 0, 0])),
                    "rotation": keyframes((0, [0, 0, 0]), (1, [1.5, 0, 0]), (2, [0, 0, 0])),
                },
                "head": {
                    "rotation": keyframes((0, [0, -3, 0]), (1, [2, 3, 0]), (2, [0, -3, 0])),
                },
                "tail": {
                    "rotation": keyframes((0, [0, -7, 0]), (1, [0, 7, 0]), (2, [0, -7, 0])),
                },
            }),
        },
        "animation.steel_fanged_raikan.walk": {
            "loop": "loop",
            "animation_length": 1.0,
            "bones": keep_bones({
                "Body": {
                    "position": keyframes((0, [0, 0, 0]), (0.5, [0, 0.25, 0]), (1, [0, 0, 0])),
                },
                "head": {
                    "rotation": keyframes((0, [1, 0, 0]), (0.5, [-2, 0, 0]), (1, [1, 0, 0])),
                },
                "tail": {
                    "rotation": keyframes((0, [0, 10, 0]), (0.5, [0, -10, 0]), (1, [0, 10, 0])),
                },
                "leg1": {"rotation": keyframes((0, [18, 0, 0]), (0.5, [-18, 0, 0]), (1, [18, 0, 0]))},
                "leg2": {"rotation": keyframes((0, [-18, 0, 0]), (0.5, [18, 0, 0]), (1, [-18, 0, 0]))},
                "leg3": {"rotation": keyframes((0, [-18, 0, 0]), (0.5, [18, 0, 0]), (1, [-18, 0, 0]))},
                "leg4": {"rotation": keyframes((0, [18, 0, 0]), (0.5, [-18, 0, 0]), (1, [18, 0, 0]))},
                "Foot": {"rotation": keyframes((0, [-10, 0, 0]), (0.5, [10, 0, 0]), (1, [-10, 0, 0]))},
                "Foot2": {"rotation": keyframes((0, [10, 0, 0]), (0.5, [-10, 0, 0]), (1, [10, 0, 0]))},
                "Foot3": {"rotation": keyframes((0, [-10, 0, 0]), (0.5, [10, 0, 0]), (1, [-10, 0, 0]))},
                "Foot4": {"rotation": keyframes((0, [10, 0, 0]), (0.5, [-10, 0, 0]), (1, [10, 0, 0]))},
            }),
        },
        "animation.steel_fanged_raikan.attack_bite": {
            "loop": "once",
            "animation_length": 0.75,
            "bones": keep_bones({
                "Body": {
                    "position": keyframes((0, [0, 0, 0]), (0.18, [0, -0.2, 1.2]), (0.35, [0, 0.1, -1.6]), (0.75, [0, 0, 0])),
                    "rotation": keyframes((0, [0, 0, 0]), (0.18, [-8, 0, 0]), (0.35, [15, 0, 0]), (0.75, [0, 0, 0])),
                },
                "head": {
                    "rotation": keyframes((0, [0, 0, 0]), (0.18, [-12, 0, 0]), (0.35, [28, 0, 0]), (0.75, [0, 0, 0])),
                },
                "tail": {
                    "rotation": keyframes((0, [0, 0, 0]), (0.18, [0, -18, 0]), (0.35, [0, 22, 0]), (0.75, [0, 0, 0])),
                },
            }),
        },
        "animation.steel_fanged_raikan.roar": {
            "loop": "once",
            "animation_length": 1.4,
            "bones": keep_bones({
                "Body": {
                    "position": keyframes((0, [0, 0, 0]), (0.25, [0, 0.6, 0]), (0.9, [0, 0.6, 0]), (1.4, [0, 0, 0])),
                    "rotation": keyframes((0, [0, 0, 0]), (0.25, [-10, 0, 0]), (0.9, [-10, 0, 0]), (1.4, [0, 0, 0])),
                },
                "head": {
                    "rotation": keyframes((0, [0, 0, 0]), (0.25, [-25, 0, 0]), (0.9, [-30, 0, 0]), (1.4, [0, 0, 0])),
                },
                "tail": {
                    "rotation": keyframes((0, [0, 0, 0]), (0.25, [0, 25, 0]), (0.6, [0, -25, 0]), (0.9, [0, 25, 0]), (1.4, [0, 0, 0])),
                },
            }),
        },
        "animation.steel_fanged_raikan.knockdown": {
            "loop": "hold_on_last_frame",
            "animation_length": 0.9,
            "bones": keep_bones({
                "Body": {
                    "position": keyframes((0, [0, 0, 0]), (0.35, [0, -4, 0]), (0.9, [0, -5, 0])),
                    "rotation": keyframes((0, [0, 0, 0]), (0.35, [0, 0, 55]), (0.9, [0, 0, 75])),
                },
                "head": {
                    "rotation": keyframes((0, [0, 0, 0]), (0.35, [0, 0, -20]), (0.9, [0, 0, -25])),
                },
                "tail": {
                    "rotation": keyframes((0, [0, 0, 0]), (0.35, [0, 35, 0]), (0.9, [0, 45, 0])),
                },
            }),
        },
    }

    return {
        "format_version": "1.8.0",
        "animations": animations,
        "_murim_notes": {
            "source_model": str(model_path),
            "generated_for": summary["name"],
            "workflow": "Import this JSON in Blockbench animation tools, preview/tune keyframes, then export/import through MCreator.",
        },
    }


def command_gen_raikan_animations(args: argparse.Namespace) -> int:
    model_path = Path(args.model).expanduser()
    if not model_path.exists():
        print(f"File not found: {model_path}", file=sys.stderr)
        return 2
    output = ROOT / args.output
    output.parent.mkdir(parents=True, exist_ok=True)
    pack = build_raikan_animation_pack(model_path)
    output.write_text(json.dumps(pack, indent=2), encoding="utf-8")
    print(f"Wrote {rel(output)}")
    print("Animations:")
    for name in pack["animations"]:
        print(f"  - {name}")
    return 0


def write_png(path: Path, width: int, height: int, pixels: list[tuple[int, int, int, int]]) -> None:
    """Write a tiny RGBA PNG without external dependencies."""
    raw = bytearray()
    for y in range(height):
        raw.append(0)
        for x in range(width):
            raw.extend(pixels[y * width + x])

    def chunk(kind: bytes, data: bytes) -> bytes:
        return (
            struct.pack(">I", len(data))
            + kind
            + data
            + struct.pack(">I", zlib.crc32(kind + data) & 0xFFFFFFFF)
        )

    png = b"\x89PNG\r\n\x1a\n"
    png += chunk(b"IHDR", struct.pack(">IIBBBBB", width, height, 8, 6, 0, 0, 0))
    png += chunk(b"IDAT", zlib.compress(bytes(raw), 9))
    png += chunk(b"IEND", b"")
    path.write_bytes(png)


def make_palette(kind: str) -> dict[str, tuple[int, int, int, int]]:
    palettes = {
        "training": {
            "blade": (150, 104, 55, 255),
            "edge": (215, 173, 103, 255),
            "hilt": (74, 45, 28, 255),
            "guard": (102, 74, 43, 255),
            "accent": (92, 184, 122, 255),
        },
        "iron": {
            "blade": (178, 194, 204, 255),
            "edge": (232, 244, 250, 255),
            "hilt": (47, 42, 48, 255),
            "guard": (87, 94, 105, 255),
            "accent": (90, 165, 255, 255),
        },
        "jade": {
            "blade": (112, 212, 173, 255),
            "edge": (204, 255, 232, 255),
            "hilt": (32, 68, 56, 255),
            "guard": (70, 150, 119, 255),
            "accent": (255, 221, 97, 255),
        },
        "demonic": {
            "blade": (86, 28, 42, 255),
            "edge": (220, 34, 62, 255),
            "hilt": (18, 14, 22, 255),
            "guard": (80, 14, 28, 255),
            "accent": (170, 0, 42, 255),
        },
    }
    return palettes.get(kind, palettes["iron"])


def generate_item_texture(path: Path, kind: str) -> None:
    palette = make_palette(kind)
    width = height = 32
    pixels = [(0, 0, 0, 0)] * (width * height)

    def rect(x1: int, y1: int, x2: int, y2: int, color: tuple[int, int, int, int]) -> None:
        for y in range(max(0, y1), min(height, y2)):
            for x in range(max(0, x1), min(width, x2)):
                pixels[y * width + x] = color

    rect(15, 2, 17, 22, palette["blade"])
    rect(16, 2, 17, 22, palette["edge"])
    rect(14, 4, 15, 20, palette["edge"])
    rect(10, 21, 22, 24, palette["guard"])
    rect(15, 23, 17, 31, palette["hilt"])
    rect(14, 29, 18, 32, palette["accent"])
    rect(13, 8, 14, 13, palette["accent"])
    write_png(path, width, height, pixels)


def cube_element(name: str, from_xyz: list[float], to_xyz: list[float], texture: str = "#0") -> dict:
    return {
        "name": name,
        "from": from_xyz,
        "to": to_xyz,
        "faces": {
            "north": {"uv": [0, 0, 16, 16], "texture": texture},
            "east": {"uv": [0, 0, 16, 16], "texture": texture},
            "south": {"uv": [0, 0, 16, 16], "texture": texture},
            "west": {"uv": [0, 0, 16, 16], "texture": texture},
            "up": {"uv": [0, 0, 16, 16], "texture": texture},
            "down": {"uv": [0, 0, 16, 16], "texture": texture},
        },
    }


def weapon_display() -> dict:
    return {
        "thirdperson_righthand": {
            "rotation": [0, 90, 55],
            "translation": [0, 4, 1],
            "scale": [1.15, 1.15, 1.15],
        },
        "thirdperson_lefthand": {
            "rotation": [0, -90, -55],
            "translation": [0, 4, 1],
            "scale": [1.15, 1.15, 1.15],
        },
        "firstperson_righthand": {
            "rotation": [0, -90, 25],
            "translation": [1.13, 3.2, 1.13],
            "scale": [0.9, 0.9, 0.9],
        },
        "firstperson_lefthand": {
            "rotation": [0, 90, -25],
            "translation": [1.13, 3.2, 1.13],
            "scale": [0.9, 0.9, 0.9],
        },
        "ground": {
            "rotation": [0, 0, 0],
            "translation": [0, 3, 0],
            "scale": [0.55, 0.55, 0.55],
        },
        "gui": {
            "rotation": [30, 225, 0],
            "translation": [0, 0, 0],
            "scale": [0.85, 0.85, 0.85],
        },
        "fixed": {
            "rotation": [0, 180, 45],
            "translation": [0, 0, 0],
            "scale": [0.9, 0.9, 0.9],
        },
    }


def generate_sword_model(slug: str, kind: str) -> dict:
    texture = f"murim_block:item/{slug}"
    return {
        "format_version": "1.21.6",
        "credit": "Generated by Murim Block production toolkit; tune in Blockbench.",
        "texture_size": [32, 32],
        "textures": {
            "0": texture,
            "particle": texture,
        },
        "elements": [
            cube_element("blade_core", [7.25, 5.0, 7.25], [8.75, 24.0, 8.75]),
            cube_element("blade_edge_left", [6.75, 7.0, 7.5], [7.25, 23.0, 8.5]),
            cube_element("blade_edge_right", [8.75, 7.0, 7.5], [9.25, 23.0, 8.5]),
            cube_element("blade_tip", [7.5, 24.0, 7.5], [8.5, 27.0, 8.5]),
            cube_element("guard", [3.5, 3.75, 7.0], [12.5, 5.25, 9.0]),
            cube_element("handle", [7.0, -3.5, 7.0], [9.0, 4.0, 9.0]),
            cube_element("pommel", [6.25, -5.0, 6.25], [9.75, -3.25, 9.75]),
        ],
        "display": weapon_display(),
        "_murim_notes": {
            "kind": kind,
            "mcreator_custom_model_name": f"{slug}:default",
            "texture_target": f"src/main/resources/assets/murim_block/textures/item/{slug}.png",
            "custom_model_target": f"src/main/resources/assets/murim_block/models/custom/{slug}.json",
            "item_model_parent": f"murim_block:custom/{slug}",
        },
    }


def command_gen_sword_model(args: argparse.Namespace) -> int:
    name = args.name
    slug = slugify(args.slug or name).replace("-", "_")
    out_dir = ROOT / args.output / slug
    out_dir.mkdir(parents=True, exist_ok=True)
    model_path = out_dir / f"{slug}.json"
    texture_path = out_dir / f"{slug}.png"
    notes_path = out_dir / "IMPORT_NOTES.md"

    model = generate_sword_model(slug, args.kind)
    model_path.write_text(json.dumps(model, indent=2), encoding="utf-8")
    generate_item_texture(texture_path, args.kind)
    notes_path.write_text(
        "\n".join([
            f"# {name}",
            "",
            "Generated starter sword model.",
            "",
            "## Files",
            "",
            f"- Model: `{model_path.name}`",
            f"- Texture: `{texture_path.name}`",
            "",
            "## MCreator Import Targets",
            "",
            f"- Copy model to `src/main/resources/assets/murim_block/models/custom/{slug}.json`",
            f"- Copy texture to `src/main/resources/assets/murim_block/textures/item/{slug}.png`",
            f"- Item model parent can be `murim_block:custom/{slug}`",
            f"- In MCreator, custom model name should become `{slug}:default` if imported like existing custom models.",
            "",
            "## Before Import",
            "",
            "- Open the JSON in Blockbench as Java Block/Item.",
            "- Check Display tab with weapon preset / hand views.",
            "- Paint or replace the placeholder PNG.",
            "- Keep names lowercase with underscores.",
            "- Run `gradlew compileJava` after MCreator import/regeneration.",
        ]) + "\n",
        encoding="utf-8",
    )

    print(f"Wrote {rel(model_path)}")
    print(f"Wrote {rel(texture_path)}")
    print(f"Wrote {rel(notes_path)}")
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


def command_new_item(args: argparse.Namespace) -> int:
    try:
        folder = ROOT / "production" / "items"
        folder.mkdir(parents=True, exist_ok=True)
        path = folder / f"{slugify(args.name)}.md"
        if path.exists():
            raise FileExistsError(path)
        path.write_text(f"""# {args.name}

## Fantasy

- Murim role:
- Visual identity:
- Rarity / cultivation tier:

## Gameplay

- Type: sword / manual / pill / talisman / artifact
- Damage or effect:
- Qi interaction:
- Unlock / craft:
- Repair / durability:

## Model

- Blockbench format: Java Block/Item
- Texture:
- Model:
- Display preset:
- MCreator custom model name:

## Quality Checklist

- Reads clearly in hand:
- Reads clearly in inventory:
- Has unique silhouette:
- Texture uses Murim palette:
- Imported in MCreator:
- compileJava passes:
- In-game screenshot captured:
- Commit pushed:
""", encoding="utf-8")
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

    bbinfo = sub.add_parser("bbmodel-info", help="Inspect a Blockbench .bbmodel file")
    bbinfo.add_argument("path")
    bbinfo.set_defaults(func=command_bbmodel_info)

    raikan = sub.add_parser("gen-raikan-animations", help="Generate starter animations for the Steel-Fanged Raikan model")
    raikan.add_argument("--model", default="C:/Users/enzob/Documents/Steel-Fanged Raikans.bbmodel")
    raikan.add_argument("--output", default="production/blockbench/steel_fanged_raikan.animations.json")
    raikan.set_defaults(func=command_gen_raikan_animations)

    sword = sub.add_parser("gen-sword-model", help="Generate a starter Java Block/Item sword model and texture")
    sword.add_argument("name")
    sword.add_argument("--slug", help="Lowercase asset id. Defaults from name.")
    sword.add_argument("--kind", choices=["training", "iron", "jade", "demonic"], default="iron")
    sword.add_argument("--output", default="production/items/generated")
    sword.set_defaults(func=command_gen_sword_model)

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

    item = sub.add_parser("new-item", help="Create a production spec for an item, weapon or artifact")
    item.add_argument("name")
    item.set_defaults(func=command_new_item)

    return parser


def main(argv: list[str] | None = None) -> int:
    parser = build_parser()
    args = parser.parse_args(argv)
    return args.func(args)


if __name__ == "__main__":
    raise SystemExit(main())
