#!/usr/bin/env bash
set -euo pipefail

ROOT="${1:-$(pwd)}"
cd "$ROOT"

JAVA_DIR="src/main/java"

count_rg_lines() {
  local pattern="$1"
  shift
  (rg -n "$pattern" "$@" || true) | wc -l | tr -d ' '
}

if ! command -v cloc >/dev/null 2>&1; then
  echo "ERROR: cloc ist nicht installiert." >&2
  exit 1
fi
if ! command -v jq >/dev/null 2>&1; then
  echo "ERROR: jq ist nicht installiert." >&2
  exit 1
fi

cloc_json="$(cloc "$JAVA_DIR" --include-lang=Java --json)"
java_files="$(echo "$cloc_json" | jq '.Java.nFiles // 0')"
blank_lines="$(echo "$cloc_json" | jq '.Java.blank // 0')"
comment_lines="$(echo "$cloc_json" | jq '.Java.comment // 0')"
code_lines="$(echo "$cloc_json" | jq '.Java.code // 0')"

non_blank=$((code_lines + comment_lines))
comment_ratio="$(awk -v c="$comment_lines" -v n="$non_blank" 'BEGIN{if(n==0) printf "0.00"; else printf "%.2f", (c/n)*100}')"

packages_tmp="$(mktemp)"
rg --no-filename -N '^package .*;' "$JAVA_DIR" -g '*.java' | sed 's/^package //; s/;//' | sort -u > "$packages_tmp"
package_count="$(wc -l < "$packages_tmp" | tr -d ' ')"

class_count="$(count_rg_lines '^\s*(public\s+)?(abstract\s+|final\s+)?class\s+' "$JAVA_DIR" -g '*.java')"
interface_count="$(count_rg_lines '^\s*(public\s+)?interface\s+' "$JAVA_DIR" -g '*.java')"
enum_count="$(count_rg_lines '^\s*(public\s+)?enum\s+' "$JAVA_DIR" -g '*.java')"
record_count="$(count_rg_lines '^\s*(public\s+)?record\s+' "$JAVA_DIR" -g '*.java')"
type_count=$((class_count + interface_count + enum_count + record_count))

method_regex='^\s*(?:public|protected|private|static|final|native|synchronized|abstract|transient|strictfp)\s+(?!class\b|interface\b|enum\b|record\b)(?:[\w<>\[\],.?]+\s+)+\w+\s*\([^;{}]*\)\s*(?:throws\s+[\w.,\s]+)?\{'
method_count="$( (rg -n -P "$method_regex" "$JAVA_DIR" -g '*.java' || true) | wc -l | tr -d ' ' )"

avg_method_size="$(awk -v code="$code_lines" -v methods="$method_count" 'BEGIN{if(methods==0) printf "0.00"; else printf "%.2f", code/methods}')"

type_files="$(find "$JAVA_DIR" -name '*.java' ! -name 'module-info.java' | wc -l | tr -d ' ')"
total_imports="$( (rg -n '^\s*import ' "$JAVA_DIR" -g '*.java' || true) | wc -l | tr -d ' ' )"
avg_imports_per_type="$(awk -v imports="$total_imports" -v types="$type_files" 'BEGIN{if(types==0) printf "0.00"; else printf "%.2f", imports/types}')"

max_imports=-1
max_import_file=""
while IFS= read -r f; do
  imports="$( (grep -E '^[[:space:]]*import ' "$f" || true) | wc -l | tr -d ' ' )"
  if [ "$imports" -gt "$max_imports" ]; then
    max_imports="$imports"
    max_import_file="$f"
  fi
done < <(find "$JAVA_DIR" -name '*.java' ! -name 'module-info.java' | sort)

timestamp="$(date '+%Y-%m-%d %H:%M:%S %Z')"

echo "generated_at=$timestamp"
echo "java_files=$java_files"
echo "code_lines=$code_lines"
echo "comment_lines=$comment_lines"
echo "blank_lines=$blank_lines"
echo "comment_ratio_percent=$comment_ratio"
echo "package_count=$package_count"
echo "class_count=$class_count"
echo "interface_count=$interface_count"
echo "enum_count=$enum_count"
echo "record_count=$record_count"
echo "type_count=$type_count"
echo "method_count=$method_count"
echo "avg_method_size_loc=$avg_method_size"
echo "total_imports=$total_imports"
echo "avg_imports_per_type=$avg_imports_per_type"
echo "max_import_file=$max_import_file"
echo "max_imports=$max_imports"

echo
echo "[packages]"
cat "$packages_tmp"

echo
echo "[top_java_files_by_loc]"
find "$JAVA_DIR" -name '*.java' -print0 | xargs -0 wc -l | sed '/ total$/d' | sort -nr | head -n 5

echo
echo "[top_java_files_by_imports]"
while IFS= read -r f; do
  imports="$( (grep -E '^[[:space:]]*import ' "$f" || true) | wc -l | tr -d ' ' )"
  printf '%4d %s\n' "$imports" "$f"
done < <(find "$JAVA_DIR" -name '*.java' ! -name 'module-info.java' | sort) | sort -nr | head -n 5

rm -f "$packages_tmp"
