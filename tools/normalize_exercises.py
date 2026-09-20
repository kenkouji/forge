#!/usr/bin/env python3
"""
FORGE Phase 2 Exercise Normalization Pipeline
Ingests Free Exercise DB raw dataset (876 exercises) and produces normalized relational seed data:
- exercises (metadata, source provenance, forge-derived classifications)
- muscles (expandable taxonomy)
- exercise_muscles (junction table with PRIMARY/SECONDARY roles)
- equipment (normalized taxonomy)
- exercise_equipment (junction table)
- exercise_attributes (flexible multi-characteristic tags)
- exercise_families & exercise_family_members
- exercise_aliases (search aliases)
- search_tokens (FTS4 index tokens)

License: Ingests textual data under The Unlicense.
ZERO third-party raster images are bundled.
"""

import json
import re
import sys
import urllib.request
from pathlib import Path

RAW_URL = "https://raw.githubusercontent.com/yuhonas/free-exercise-db/main/dist/exercises.json"
OUTPUT_PATH = Path("app/src/main/assets/seed/exercises_seed.json")
LOCAL_RAW_PATH = Path("tools/raw_exercises.json")

def load_raw_exercises():
    if LOCAL_RAW_PATH.exists():
        print(f"Loading local cached raw data from {LOCAL_RAW_PATH}...")
        with open(LOCAL_RAW_PATH, "r", encoding="utf-8") as f:
            return json.load(f)
    print(f"Fetching raw exercises from {RAW_URL}...")
    req = urllib.request.Request(RAW_URL, headers={"User-Agent": "FORGE-Build/1.0"})
    with urllib.request.urlopen(req) as resp:
        data = json.loads(resp.read().decode("utf-8"))
    with open(LOCAL_RAW_PATH, "w", encoding="utf-8") as f:
        json.dump(data, f, indent=2)
    return data

# Expandable muscle taxonomy (Correction #4)
MUSCLES_TAXONOMY = [
    {"id": "abdominals", "name": "Abdominals", "body_part": "CORE"},
    {"id": "abductors", "name": "Abductors", "body_part": "LEGS"},
    {"id": "adductors", "name": "Adductors", "body_part": "LEGS"},
    {"id": "biceps", "name": "Biceps", "body_part": "ARMS"},
    {"id": "calves", "name": "Calves", "body_part": "LEGS"},
    {"id": "chest", "name": "Chest", "body_part": "CHEST"},
    {"id": "upper_chest", "name": "Upper Chest", "body_part": "CHEST"},
    {"id": "forearms", "name": "Forearms", "body_part": "ARMS"},
    {"id": "glutes", "name": "Glutes", "body_part": "LEGS"},
    {"id": "hamstrings", "name": "Hamstrings", "body_part": "LEGS"},
    {"id": "lats", "name": "Lats", "body_part": "BACK"},
    {"id": "lower_back", "name": "Lower Back", "body_part": "BACK"},
    {"id": "middle_back", "name": "Middle Back", "body_part": "BACK"},
    {"id": "neck", "name": "Neck", "body_part": "NECK"},
    {"id": "quadriceps", "name": "Quadriceps", "body_part": "LEGS"},
    {"id": "shoulders", "name": "Shoulders", "body_part": "SHOULDERS"},
    {"id": "front_delts", "name": "Front Delts", "body_part": "SHOULDERS"},
    {"id": "side_delts", "name": "Side Delts", "body_part": "SHOULDERS"},
    {"id": "rear_delts", "name": "Rear Delts", "body_part": "SHOULDERS"},
    {"id": "traps", "name": "Traps", "body_part": "BACK"},
    {"id": "triceps", "name": "Triceps", "body_part": "ARMS"},
    {"id": "obliques", "name": "Obliques", "body_part": "CORE"}
]

# Normalized equipment taxonomy
EQUIPMENT_TAXONOMY = [
    {"id": "barbell", "name": "Barbell"},
    {"id": "dumbbell", "name": "Dumbbell"},
    {"id": "cable", "name": "Cable"},
    {"id": "machine", "name": "Machine"},
    {"id": "bodyweight", "name": "Bodyweight"},
    {"id": "kettlebell", "name": "Kettlebell"},
    {"id": "bands", "name": "Resistance Bands"},
    {"id": "ez_bar", "name": "EZ Bar"},
    {"id": "exercise_ball", "name": "Exercise Ball"},
    {"id": "foam_roller", "name": "Foam Roller"},
    {"id": "medicine_ball", "name": "Medicine Ball"},
    {"id": "other", "name": "Other / Specialized"}
]

# Curated FORGE popularity layer (~45 core canonical exercises) with verified reference video IDs (Part A & Part 1)
POPULAR_SEED = {
    # Chest
    "barbell_bench_press___medium_grip": {"rank": 1, "video": "rT7DgCr-3pg"},
    "barbell_incline_bench_press___medium_grip": {"rank": 2, "video": "8iPEnn-ltC8"},
    "dumbbell_bench_press": {"rank": 3, "video": "VmB1G1K7v94"},
    "hammer_grip_incline_db_bench_press": {"rank": 4, "video": "0G2_XV7slIg"},
    "cable_crossover": {"rank": 5, "video": "Iwe6AmxVf7o"},
    "butterfly": {"rank": 6, "video": "eGjt4lk6g34"}, # Pec Deck
    "pushups": {"rank": 7, "video": "IODxDxX7oi4"},
    "dips___chest_version": {"rank": 8, "video": "2z8JmcrW-As"},
    
    # Back
    "pullups": {"rank": 9, "video": "eGo4IYlbE5g"},
    "chin_up": {"rank": 10, "video": "mRy9m2QTXU4"},
    "wide_grip_lat_pulldown": {"rank": 11, "video": "CAwf7n6Luuc"},
    "bent_over_barbell_row": {"rank": 12, "video": "FWJR5Ve8gkQ"},
    "seated_cable_rows": {"rank": 13, "video": "GZbfZ033f74"},
    "one_arm_dumbbell_row": {"rank": 14, "video": "pYcpY20QaE8"},
    
    # Shoulders
    "standing_military_press": {"rank": 15, "video": "2yjwXTZQDDI"},
    "seated_dumbbell_press": {"rank": 16, "video": "qEwKCR5JCog"},
    "side_lateral_raise": {"rank": 17, "video": "3VcKaXpzqRo"},
    "seated_bent_over_rear_delt_raise": {"rank": 18, "video": "0G0k8dKqL7w"},
    "face_pull": {"rank": 19, "video": "rep-qVOkqgk"},
    
    # Biceps
    "barbell_curl": {"rank": 20, "video": "kwG2ipFRgfo"},
    "dumbbell_alternate_bicep_curl": {"rank": 21, "video": "sAq_ocpRh_I"},
    "incline_dumbbell_curl": {"rank": 22, "video": "soxrZlIl35U"},
    "hammer_curls": {"rank": 23, "video": "zC3nLlEvin4"},
    "preacher_curl": {"rank": 24, "video": "fIWP-FRFNU0"},
    
    # Triceps
    "triceps_pushdown": {"rank": 25, "video": "2-LAMcpzODU"},
    "cable_lying_triceps_extension": {"rank": 26, "video": "1u18yJcLmi0"},
    "lying_triceps_press": {"rank": 27, "video": "d_KZxkY_0aw"}, # Skull crusher
    "dips___triceps_version": {"rank": 28, "video": "6kALZikXxLc"},
    
    # Legs
    "barbell_squat": {"rank": 29, "video": "bEv6CCg2BC8"},
    "front_barbell_squat": {"rank": 30, "video": "v-mQm_droHg"},
    "leg_press": {"rank": 31, "video": "IZxyjW7MPJQ"},
    "hack_squat": {"rank": 32, "video": "0tn5K9NlCfo"},
    "romanian_deadlift": {"rank": 33, "video": "jEy_czb3RKA"},
    "barbell_deadlift": {"rank": 34, "video": "op9kVnSso6Q"},
    "lying_leg_curls": {"rank": 35, "video": "1Tq3QdYUuHs"},
    "leg_extensions": {"rank": 36, "video": "YyvSfVjQeL0"},
    "split_squat_with_dumbbells": {"rank": 37, "video": "2C-uNgKwPLE"},
    "dumbbell_lunges": {"rank": 38, "video": "L8fvypPrzzs"},
    "standing_calf_raises": {"rank": 39, "video": "gwLzBJYoWlI"},
    "seated_calf_raise": {"rank": 40, "video": "JbyjNymZOt0"},
    
    # Core
    "hanging_leg_raise": {"rank": 41, "video": "hdng3Nm1x_E"},
    "cable_crunch": {"rank": 42, "video": "2fOZXCT6iWo"},
    "ab_roller": {"rank": 43, "video": "rqiTPdK1c_I"},
    "plank": {"rank": 44, "video": "ASdvN_XEl_c"}
}

def map_raw_muscle(raw_m):
    if not raw_m:
        return None
    m = raw_m.strip().lower().replace(" ", "_")
    valid_ids = {item["id"] for item in MUSCLES_TAXONOMY}
    if m in valid_ids:
        return m
    if "abs" in m or "abdominal" in m:
        return "abdominals"
    if "quad" in m:
        return "quadriceps"
    if "shoulder" in m or "delt" in m:
        return "shoulders"
    return m

def map_raw_equipment(raw_eq):
    if not raw_eq:
        return "bodyweight"
    eq = raw_eq.strip().lower()
    if eq in ("body only", "none"):
        return "bodyweight"
    if eq == "kettlebells":
        return "kettlebell"
    if eq in ("e-z curl bar", "ez curl bar", "ez bar"):
        return "ez_bar"
    if eq == "foam roll":
        return "foam_roller"
    if eq == "bands":
        return "bands"
    valid_ids = {item["id"] for item in EQUIPMENT_TAXONOMY}
    if eq in valid_ids:
        return eq
    return "other"

# Exercise Families Definition
FAMILIES_DEF = [
    {"id": "bench_press_family", "name": "Bench Press", "description": "Horizontal chest pressing variations across implements and bench angles.", "primary_pattern": "HORIZONTAL_PUSH"},
    {"id": "squat_family", "name": "Squat", "description": "Bilateral and machine quad-dominant knee flexion/extension movements.", "primary_pattern": "SQUAT"},
    {"id": "deadlift_family", "name": "Deadlift", "description": "Hip hinge pulling movements targeting posterior chain.", "primary_pattern": "HINGE"},
    {"id": "overhead_press_family", "name": "Overhead Press", "description": "Vertical pushing variations targeting anterior deltoids and triceps.", "primary_pattern": "VERTICAL_PUSH"},
    {"id": "row_family", "name": "Row", "description": "Horizontal pulling variations targeting lats, rhomboids, and middle back.", "primary_pattern": "HORIZONTAL_PULL"},
    {"id": "pull_up_family", "name": "Pull-Up & Pulldown", "description": "Vertical pulling variations targeting latissimus dorsi.", "primary_pattern": "VERTICAL_PULL"},
    {"id": "dip_family", "name": "Dips", "description": "Compound pushing movements across parallel bars or benches.", "primary_pattern": "HORIZONTAL_PUSH"},
    {"id": "biceps_curl_family", "name": "Biceps Curl", "description": "Elbow flexion isolation across barbells, dumbbells, and cables.", "primary_pattern": "ELBOW_FLEXION"},
    {"id": "triceps_extension_family", "name": "Triceps Extension", "description": "Elbow extension isolation targeting triceps brachii.", "primary_pattern": "ELBOW_EXTENSION"},
    {"id": "lateral_raise_family", "name": "Lateral Raise", "description": "Shoulder abduction isolation targeting lateral deltoids.", "primary_pattern": "SHOULDER_ISOLATION"},
    {"id": "lunge_family", "name": "Lunge & Split Squat", "description": "Unilateral lower body stepping and split-stance movements.", "primary_pattern": "LUNGE"},
    {"id": "calf_raise_family", "name": "Calf Raise", "description": "Ankle plantarflexion targeting gastrocnemius and soleus.", "primary_pattern": "CALF_ISOLATION"},
    {"id": "crunch_family", "name": "Abdominal Crunch", "description": "Spinal flexion isolation targeting rectus abdominis.", "primary_pattern": "CORE"},
    {"id": "leg_curl_family", "name": "Leg Curl", "description": "Knee flexion isolation targeting hamstrings.", "primary_pattern": "HINGE"},
    {"id": "leg_extension_family", "name": "Leg Extension", "description": "Knee extension isolation targeting quadriceps.", "primary_pattern": "SQUAT"}
]

def derive_family(name, p_muscles, eq):
    n = name.lower()
    if "bench press" in n or (("chest press" in n or "incline press" in n or "decline press" in n) and "leg" not in n):
        return "bench_press_family"
    if "squat" in n or "leg press" in n or "hack squat" in n:
        if "split squat" in n:
            return "lunge_family"
        return "squat_family"
    if "deadlift" in n or "rdl" in n:
        return "deadlift_family"
    if "overhead press" in n or "military press" in n or "shoulder press" in n or "push press" in n or "arnold press" in n:
        return "overhead_press_family"
    if ("row" in n or "rowing" in n) and "upright" not in n and "face" not in n:
        return "row_family"
    if "pull-up" in n or "chin-up" in n or "pulldown" in n or "pull up" in n or "chin up" in n:
        return "pull_up_family"
    if "dip" in n and "clean" not in n:
        return "dip_family"
    if "curl" in n and ("bicep" in n or "biceps" in n or "hammer" in n or "preacher" in n or "barbell curl" in n or "dumbbell curl" in n or "concentration" in n):
        return "biceps_curl_family"
    if "tricep" in n or "triceps" in n or "pushdown" in n or "skull crusher" in n or "kickback" in n:
        return "triceps_extension_family"
    if "lateral raise" in n or "side raise" in n:
        return "lateral_raise_family"
    if "lunge" in n or "split squat" in n or "step-up" in n or "step up" in n:
        return "lunge_family"
    if "calf raise" in n or "calves raise" in n:
        return "calf_raise_family"
    if "crunch" in n or "sit-up" in n:
        return "crunch_family"
    if "leg curl" in n:
        return "leg_curl_family"
    if "leg extension" in n:
        return "leg_extension_family"
    return None

def derive_movement_pattern(name, p_muscles, mechanic, force, category):
    n = name.lower()
    cat = (category or "").lower()
    if cat == "cardio":
        return "CARDIO"
    
    if "calves" in p_muscles or "calf" in n:
        return "CALF_ISOLATION"
    if "abdominals" in p_muscles or "core" in n or "plank" in n or "crunch" in n or "sit-up" in n:
        return "CORE"
    if "biceps" in p_muscles or ("forearms" in p_muscles and "curl" in n):
        return "ELBOW_FLEXION"
    if "triceps" in p_muscles and ("extension" in n or "pushdown" in n or "kickback" in n or "skull" in n):
        return "ELBOW_EXTENSION"
    if "shoulders" in p_muscles and ("lateral" in n or "rear" in n or "shrug" in n):
        return "SHOULDER_ISOLATION"
    
    if "chest" in p_muscles:
        if force == "push" or "press" in n or "push-up" in n or "pushup" in n or "dip" in n:
            return "HORIZONTAL_PUSH"
        if "fly" in n or "flye" in n:
            return "ISOLATION"
            
    if "shoulders" in p_muscles:
        if "press" in n or force == "push":
            return "VERTICAL_PUSH"
            
    if "lats" in p_muscles or "middle_back" in p_muscles:
        if "pulldown" in n or "pull-up" in n or "chin-up" in n or "pullup" in n:
            return "VERTICAL_PULL"
        if "row" in n:
            return "HORIZONTAL_PULL"
            
    if "quadriceps" in p_muscles:
        if "lunge" in n or "split" in n or "step" in n:
            return "LUNGE"
        if "squat" in n or "press" in n:
            return "SQUAT"
        if "extension" in n:
            return "ISOLATION"
            
    if "hamstrings" in p_muscles or "glutes" in p_muscles or "lower_back" in p_muscles:
        if "deadlift" in n or "rdl" in n or "good morning" in n or "thrust" in n or "bridge" in n:
            return "HINGE"
        if "curl" in n:
            return "ISOLATION"
            
    if force == "push":
        return "HORIZONTAL_PUSH"
    if force == "pull":
        return "HORIZONTAL_PULL"
        
    return "ISOLATION"

def derive_attributes(name, eq):
    n = name.lower()
    attrs = set()
    if "incline" in n:
        attrs.add("INCLINE")
    elif "decline" in n:
        attrs.add("DECLINE")
    elif "flat" in n or "bench press" in n:
        attrs.add("FLAT")
        
    if "single" in n or "one-arm" in n or "one arm" in n or "alternating" in n or "alternate" in n or "unilateral" in n:
        attrs.add("UNILATERAL")
    else:
        attrs.add("BILATERAL")
        
    if "close-grip" in n or "close grip" in n or "narrow" in n:
        attrs.add("CLOSE_GRIP")
    elif "wide-grip" in n or "wide grip" in n or "wide" in n:
        attrs.add("WIDE_GRIP")
        
    if "deficit" in n:
        attrs.add("DEFICIT")
    if "pause" in n or "paused" in n:
        attrs.add("PAUSED")
    if "assisted" in n:
        attrs.add("ASSISTED")
    return sorted(list(attrs))

def generate_aliases(name):
    aliases = set()
    n = name.strip()
    n_lower = n.lower()
    aliases.add(n_lower)
    
    replacements = [
        ("barbell", "bb"),
        ("dumbbell", "db"),
        ("overhead press", "ohp"),
        ("romanian deadlift", "rdl"),
        ("bench press", "bench"),
        ("back squat", "squat"),
        ("pull-up", "pullup"),
        ("push-up", "pushup"),
        ("chin-up", "chinup")
    ]
    
    curr = n_lower
    for full, short in replacements:
        if full in curr:
            aliases.add(curr.replace(full, short))
            
    no_punct = re.sub(r"[^a-z0-9\s]", " ", n_lower)
    no_punct = re.sub(r"\s+", " ", no_punct).strip()
    if no_punct != n_lower:
        aliases.add(no_punct)
        
    if "barbell bench press" in n_lower:
        aliases.update(["bench", "flat bench", "bb bench", "flat barbell bench press", "bench press"])
    elif "barbell back squat" in n_lower or "barbell full squat" in n_lower:
        aliases.update(["squat", "back squat", "bb squat", "barbell squat", "back squats"])
    elif "deadlift" in n_lower and "barbell" in n_lower:
        aliases.update(["dl", "bb deadlift", "conventional deadlift"])
    elif "standing overhead press" in n_lower or "barbell overhead press" in n_lower:
        aliases.update(["ohp", "military press", "strict press", "shoulder press"])
    elif "romanian deadlift" in n_lower:
        aliases.update(["rdl", "stiff leg deadlift"])
    elif "lat pulldown" in n_lower:
        aliases.update(["lat pull", "pulldown", "cable pulldown"])
        
    aliases.discard(n)
    return sorted(list(aliases))

def main():
    raw_data = load_raw_exercises()
    print(f"Read {len(raw_data)} raw records.")
    
    normalized_exercises = []
    exercise_muscles = []
    exercise_equipment = []
    exercise_attributes = []
    exercise_family_members = []
    exercise_aliases = []
    
    stats = {
        "total_read": len(raw_data),
        "validated_canonical": 0,
        "rejected": 0,
        "families_assigned": 0,
        "aliases_count": 0,
        "attributes_count": 0
    }
    
    seen_ids = set()
    
    for item in raw_data:
        raw_id = item.get("id", "").strip()
        name = item.get("name", "").strip()
        if not raw_id or not name:
            stats["rejected"] += 1
            continue
            
        canonical_id = raw_id.lower().replace(" ", "_").replace("-", "_")
        canonical_id = re.sub(r"[^a-z0-9_]", "", canonical_id)
        if canonical_id in seen_ids:
            stats["rejected"] += 1
            continue
        seen_ids.add(canonical_id)
        
        source_category = item.get("category")
        source_force = item.get("force")
        source_level = item.get("level")
        source_mechanic = item.get("mechanic")
        source_eq = item.get("equipment")
        
        raw_primary = item.get("primaryMuscles") or []
        raw_secondary = item.get("secondaryMuscles") or []
        
        norm_primary = [map_raw_muscle(m) for m in raw_primary if map_raw_muscle(m)]
        norm_secondary = [map_raw_muscle(m) for m in raw_secondary if map_raw_muscle(m)]
        
        eq_id = map_raw_equipment(source_eq)
        
        forge_movement_pattern = derive_movement_pattern(name, norm_primary, source_mechanic, source_force, source_category)
        forge_family_id = derive_family(name, norm_primary, eq_id)
        if forge_family_id:
            stats["families_assigned"] += 1
            is_lead = ("barbell" in canonical_id and ("bench_press" in canonical_id or "squat" in canonical_id or "deadlift" in canonical_id or "curl" in canonical_id))
            exercise_family_members.append({
                "family_id": forge_family_id,
                "exercise_id": canonical_id,
                "is_canonical_lead": is_lead
            })
            
        attrs = derive_attributes(name, eq_id)
        for attr in attrs:
            exercise_attributes.append({
                "exercise_id": canonical_id,
                "attribute": attr
            })
            stats["attributes_count"] += 1
            
        for m in norm_primary:
            exercise_muscles.append({
                "exercise_id": canonical_id,
                "muscle_id": m,
                "role": "PRIMARY",
                "is_forge_derived": False
            })
        for m in norm_secondary:
            if m not in norm_primary:
                exercise_muscles.append({
                    "exercise_id": canonical_id,
                    "muscle_id": m,
                    "role": "SECONDARY",
                    "is_forge_derived": False
                })
                
        exercise_equipment.append({
            "exercise_id": canonical_id,
            "equipment_id": eq_id,
            "is_primary": True
        })
        
        aliases = generate_aliases(name)
        for a in aliases:
            exercise_aliases.append({
                "exercise_id": canonical_id,
                "alias": a,
                "is_forge_derived": True
            })
            stats["aliases_count"] += 1
            
        search_token_parts = [name.lower()] + aliases + norm_primary + [eq_id]
        search_tokens = " ".join(dict.fromkeys(search_token_parts))
        
        raw_instructions = item.get("instructions") or []
        instructions_text = "\n".join(raw_instructions) if isinstance(raw_instructions, list) else str(raw_instructions)
        
        is_pop = canonical_id in POPULAR_SEED
        pop_rank = POPULAR_SEED[canonical_id]["rank"] if is_pop else 9999
        yt_id = POPULAR_SEED[canonical_id]["video"] if is_pop else None

        exercise_record = {
            "id": canonical_id,
            "name": name,
            "canonical_name": name,
            "instructions": instructions_text,
            "form_cues": "",
            "common_mistakes": "",
            "youtube_video_id": yt_id,
            "is_custom": False,
            "is_popular": is_pop,
            "popularity_rank": pop_rank,
            "is_favorite": False,
            "source": "free_exercise_db",
            "source_id": item.get("id"),
            "source_category": source_category,
            "source_force": source_force,
            "source_level": source_level,
            "source_mechanic": source_mechanic,
            "source_equipment": source_eq,
            "forge_movement_pattern": forge_movement_pattern,
            "forge_exercise_family_id": forge_family_id,
            "search_tokens": search_tokens,
            "license": "The Unlicense",
            "created_at": 1726830000000,
            "updated_at": 1726830000000
        }
        normalized_exercises.append(exercise_record)
        stats["validated_canonical"] += 1

    print("\n--- Normalization Summary ---")
    print(f"Total read: {stats['total_read']}")
    print(f"Validated canonical exercises: {stats['validated_canonical']}")
    print(f"Rejected: {stats['rejected']}")
    print(f"Exercises mapped to families: {stats['families_assigned']}")
    print(f"Total alias entries: {stats['aliases_count']}")
    print(f"Total attribute entries: {stats['attributes_count']}")
    print(f"Total muscle mappings: {len(exercise_muscles)}")
    print(f"Total equipment mappings: {len(exercise_equipment)}")
    
    seed_bundle = {
        "metadata": {
            "source": "free_exercise_db",
            "license": "The Unlicense",
            "upstream_source": "wrkout/exercises.json",
            "taxonomy_reference": "kinetic-place (MIT)",
            "exercise_count": len(normalized_exercises),
            "generated_at": 1726830000000,
            "provenance_note": "Textual metadata only; zero third-party raster images included."
        },
        "muscles": MUSCLES_TAXONOMY,
        "equipment": EQUIPMENT_TAXONOMY,
        "exercise_families": FAMILIES_DEF,
        "exercises": normalized_exercises,
        "exercise_muscles": exercise_muscles,
        "exercise_equipment": exercise_equipment,
        "exercise_attributes": exercise_attributes,
        "exercise_family_members": exercise_family_members,
        "exercise_aliases": exercise_aliases
    }
    
    OUTPUT_PATH.parent.mkdir(parents=True, exist_ok=True)
    with open(OUTPUT_PATH, "w", encoding="utf-8") as f:
        json.dump(seed_bundle, f, separators=(",", ":"))
        
    out_size_bytes = OUTPUT_PATH.stat().st_size
    print(f"\nGenerated seed artifact at {OUTPUT_PATH}")
    print(f"Size: {out_size_bytes:,} bytes ({out_size_bytes / 1024:.1f} KB)")
    
    if stats["validated_canonical"] < 800:
        print(f"ERROR: Validated count {stats['validated_canonical']} < 800 requirement!")
        sys.exit(1)
    print("SUCCESS: Exercise count exceeds >=800 canonical requirement!")

if __name__ == "__main__":
    main()
