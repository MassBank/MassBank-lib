#!/usr/bin/env bash
set -euo pipefail

# Runs RecordPersistenceBenchmarkTest for combinations of chunk-size and Hibernate batch-size
# and compares the reported "Persist time".
#
# Defaults are tuned for a full run of 30000 files.
# Override via environment variables if needed, e.g.:
#   BENCHMARK_LIMIT=5000 CHUNK_SIZES="500 1000" BATCH_SIZES="100 200" ./benchmark-persistence-matrix.sh
#
# Dry-run example (no Maven execution):
#   DRY_RUN=1 ./benchmark-persistence-matrix.sh

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$ROOT_DIR"

BENCHMARK_LIMIT="${BENCHMARK_LIMIT:-0}"
CHUNK_SIZES="${CHUNK_SIZES:-500 1000 2000 4000 8000 16000 32000}"
BATCH_SIZES="${BATCH_SIZES:-100 200 400 800 1600 3200 6400}"
BENCHMARK_DATA_DIR="${BENCHMARK_DATA_DIR:-}"
DRY_RUN="${DRY_RUN:-0}"

OUT_DIR="${OUT_DIR:-$ROOT_DIR/target/benchmark-runs}"
mkdir -p "$OUT_DIR"

TIMESTAMP="$(date +%Y%m%d-%H%M%S)"
CSV_FILE="$OUT_DIR/persistence-matrix-$TIMESTAMP.csv"
SUMMARY_FILE="$OUT_DIR/persistence-matrix-$TIMESTAMP-summary.txt"

printf "chunk_size,batch_size,limit,persist_seconds,records_per_sec,status,log_file\n" > "$CSV_FILE"

echo "Running benchmark matrix..."
echo "  limit         : $BENCHMARK_LIMIT"
echo "  chunk sizes   : $CHUNK_SIZES"
echo "  batch sizes   : $BATCH_SIZES"
echo "  output csv    : $CSV_FILE"
echo

for chunk in $CHUNK_SIZES; do
  for batch in $BATCH_SIZES; do
    RUN_ID="c${chunk}-b${batch}-l${BENCHMARK_LIMIT}"
    LOG_FILE="$OUT_DIR/$RUN_ID.log"

    CMD=(
      ./mvnw
      -Dtest=RecordPersistenceBenchmarkTest
      -Dmassbank.benchmark.enabled=true
      "-Dmassbank.benchmark.limit=$BENCHMARK_LIMIT"
      "-Dmassbank.persistence.chunk-size=$chunk"
      "-Dspring.jpa.properties.hibernate.jdbc.batch_size=$batch"
      test
    )

    if [[ -n "$BENCHMARK_DATA_DIR" ]]; then
      CMD+=("-Dmassbank.benchmark.data-dir=$BENCHMARK_DATA_DIR")
    fi

    echo "=== $RUN_ID ==="
    echo "Command: ${CMD[*]}"

    if [[ "$DRY_RUN" == "1" ]]; then
      echo "DRY_RUN=1 -> skip execution" | tee "$LOG_FILE"
      printf "%s,%s,%s,%s,%s,%s,%s\n" \
        "$chunk" "$batch" "$BENCHMARK_LIMIT" "" "" "DRY_RUN" "$LOG_FILE" >> "$CSV_FILE"
      echo
      continue
    fi

    set +e
    "${CMD[@]}" | tee "$LOG_FILE"
    EXIT_CODE=${PIPESTATUS[0]}
    set -e

    PERSIST_SECONDS=""
    RECORDS_PER_SEC=""
    STATUS="OK"

    if [[ $EXIT_CODE -ne 0 ]]; then
      STATUS="FAILED($EXIT_CODE)"
    else
      PERSIST_SECONDS="$(grep -E "Persist time[[:space:]]*:" "$LOG_FILE" | tail -n1 | sed -E 's/.*: ([0-9.]+) s.*/\1/' || true)"
      RECORDS_PER_SEC="$(grep -E "Persist records/sec[[:space:]]*:" "$LOG_FILE" | tail -n1 | sed -E 's/.*: ([0-9.]+).*/\1/' || true)"
      if [[ -z "$PERSIST_SECONDS" ]]; then
        STATUS="PARSE_ERROR"
      fi
    fi

    printf "%s,%s,%s,%s,%s,%s,%s\n" \
      "$chunk" "$batch" "$BENCHMARK_LIMIT" "$PERSIST_SECONDS" "$RECORDS_PER_SEC" "$STATUS" "$LOG_FILE" >> "$CSV_FILE"

    echo "Result: status=$STATUS persist=${PERSIST_SECONDS:-n/a}s records/sec=${RECORDS_PER_SEC:-n/a}"
    echo
  done
done

python3 - "$CSV_FILE" <<'PY' | tee "$SUMMARY_FILE"
import csv
import math
import sys

csv_file = sys.argv[1]
rows = []
with open(csv_file, newline="", encoding="utf-8") as f:
    reader = csv.DictReader(f)
    for row in reader:
        try:
            persist_sort = float(row["persist_seconds"]) if row["persist_seconds"] else math.inf
        except ValueError:
            persist_sort = math.inf
        rows.append((persist_sort, row))

rows.sort(key=lambda x: x[0])

print("Benchmark results (sorted by persist_seconds):")
print()
print(f"{'chunk':<10} {'batch':<10} {'limit':<8} {'persist_s':<14} {'records/s':<14} {'status':<12} log")
for _, row in rows:
    persist = row['persist_seconds'] or 'n/a'
    rps = row['records_per_sec'] or 'n/a'
    print(f"{row['chunk_size']:<10} {row['batch_size']:<10} {row['limit']:<8} {persist:<14} {rps:<14} {row['status']:<12} {row['log_file']}")
PY

echo
echo "CSV written to: $CSV_FILE"
echo "Summary written to: $SUMMARY_FILE"

