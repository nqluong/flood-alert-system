#!/bin/bash
# Cách dùng: ./monitor.sh LT01 300

SCENARIO=${1:-"TEST"}
DURATION=${2:-300}
GROUP="flood-processor-group"
TOPIC="iot-raw"

START_TIME=$(date '+%Y-%m-%d %H:%M:%S')
START_TS=$(date +%s)
END_TS=$(( START_TS + DURATION ))
MAX_LAG=0

KAFKA_BIN=$(docker exec flood-kafka \
  find /opt /usr -name "kafka-consumer-groups.sh" 2>/dev/null | head -1)

echo "===== BẮT ĐẦU: $START_TIME =====" | tee result_$SCENARIO.log
echo "SCENARIO=$SCENARIO | TOPIC=$TOPIC | GROUP=$GROUP | DURATION=${DURATION}s" \
  | tee -a result_$SCENARIO.log

get_lag() {
  docker exec flood-kafka $KAFKA_BIN \
    --bootstrap-server localhost:9092 \
    --describe --group "$GROUP" 2>/dev/null \
    | awk -v t="$TOPIC" '
        NR>1 && $2==t && $6~/^[0-9]+$/ { sum += $6 }
        END { print sum+0 }'
}

# ── PHASE 1: Monitor trong khi simulator chạy ──
echo "[PHASE 1] Monitoring ${DURATION}s..."

while [ $(date +%s) -lt $END_TS ]; do
  TIMESTAMP=$(date '+%H:%M:%S')
  ELAPSED=$(( $(date +%s) - START_TS ))
  LAG=$(get_lag); LAG=${LAG:-0}

  # Ghi CPU/RAM
  echo "=== $TIMESTAMP (${ELAPSED}s) ===" >> stats_$SCENARIO.log
  docker stats --no-stream --format \
    "{{.Name}}\t{{.CPUPerc}}\t{{.MemUsage}}\t{{.MemPerc}}" \
    >> stats_$SCENARIO.log

  # Ghi lag
  echo "$TIMESTAMP elapsed=${ELAPSED}s LAG=$LAG" >> lag_$SCENARIO.log
  echo "[PHASE 1] $TIMESTAMP | ${ELAPSED}s | LAG=$LAG"

  if [ "$LAG" -gt "$MAX_LAG" ]; then MAX_LAG=$LAG; fi
  sleep 5
done

echo "[PHASE 1] Kết thúc — MAX_LAG=$MAX_LAG" | tee -a result_$SCENARIO.log

# ── PHASE 2: Chờ lag về 0 ──
echo "" && echo "[PHASE 2] Chờ Kafka xử lý hết..."
WAIT_START=$(date +%s)

while true; do
  LAG=$(get_lag); LAG=${LAG:-0}
  WAITED=$(( $(date +%s) - WAIT_START ))
  echo "[PHASE 2] $(date '+%H:%M:%S') | waited=${WAITED}s | LAG=$LAG"

  if [ "$LAG" -eq 0 ]; then
    echo "✅ Xử lý hết sau ${WAITED}s" | tee -a result_$SCENARIO.log
    break
  fi

  if [ "$WAITED" -gt 600 ]; then
    echo "⚠️  Timeout 10 phút, LAG còn: $LAG" | tee -a result_$SCENARIO.log
    break
  fi

  sleep 5
done

# ── TỔNG HỢP ──
TOTAL=$(( $(date +%s) - START_TS ))
echo ""                                                      | tee -a result_$SCENARIO.log
echo "════════════════════════════════"                      | tee -a result_$SCENARIO.log
echo "KẾT QUẢ: $SCENARIO"                                   | tee -a result_$SCENARIO.log
echo "────────────────────────────────"                      | tee -a result_$SCENARIO.log
echo "Bắt đầu       : $START_TIME"                          | tee -a result_$SCENARIO.log
echo "Kết thúc      : $(date '+%Y-%m-%d %H:%M:%S')"        | tee -a result_$SCENARIO.log
echo "Tổng thời gian: ${TOTAL}s"                            | tee -a result_$SCENARIO.log
echo "MAX lag       : $MAX_LAG msg"                         | tee -a result_$SCENARIO.log
echo "────────────────────────────────"                      | tee -a result_$SCENARIO.log
echo "Mốc truy vấn DB: $START_TIME"                        | tee -a result_$SCENARIO.log
echo "════════════════════════════════"                      | tee -a result_$SCENARIO.log
