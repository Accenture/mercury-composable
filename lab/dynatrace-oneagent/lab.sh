#!/usr/bin/env bash
# The Dynatrace OneAgent lab: a Kafka broker and two Mercury applications in Colima, with OneAgent
# installed full-stack in Colima's VM. See README.md.
set -euo pipefail
cd "$(dirname "$0")"
ROOT=../..
VERSION=4.12.27
APPS="lab-broker lab-edge lab-worker"

usage() {
  echo "usage: $0 build | up [oneagent|none] | order | logs <lab-edge|lab-worker|lab-broker> | lineage | down"
  exit 1
}

case "${1:-}" in
  build)
    # The lab's own aggregator (pom.xml here, not in the root reactor) builds the Mercury modules it uses from
    # this checkout, so Maven never takes the RELEASED platform-core of the same version from ~/.m2, which may
    # lack what the branch adds. A lab build is no quality gate: tests and coverage checks are skipped.
    mvn -o -q -f pom.xml package -DskipTests -Djacoco.skip=true -am -pl :lab-edge,:lab-worker,:kafka-standalone
    cp "$ROOT/helpers/kafka-standalone/target/kafka-standalone-$VERSION-exec.jar" apps/lab-broker.jar
    cp "lab-edge/target/lab-edge-$VERSION.jar" apps/lab-edge.jar
    cp "lab-worker/target/lab-worker-$VERSION.jar" apps/lab-worker.jar
    for app in $APPS; do
      docker build -q -t "mercury-lab/$app:$VERSION" --build-arg APP="$app" apps
    done
    ;;
  up)
    case "${2:-oneagent}" in
      # the default names every hop "<application> - <route>"; DESTINATION=application or route names it after one
      oneagent) tracer=(-e DYNATRACE_ONEAGENT_ENABLED=true -e "DYNATRACE_ONEAGENT_DESTINATION=${DESTINATION:-application-route}") ;;
      none)     tracer=() ;;
      *)        usage ;;
    esac
    # The broker advertises 127.0.0.1:9092, so the applications share its network namespace; the broker
    # container therefore publishes lab-edge's REST port too.
    # host port 18100, not 8100: the reactor's tests (reactive-postgres, for one) serve REST on 8100, and a lab left
    # running would answer their requests
    docker run -d --name lab-broker -p 18100:8100 "mercury-lab/lab-broker:$VERSION"
    until docker exec lab-broker bash -c 'echo > /dev/tcp/127.0.0.1/9092' 2>/dev/null; do sleep 1; done
    for app in lab-worker lab-edge; do
      docker run -d --name "$app" --network container:lab-broker ${tracer[@]+"${tracer[@]}"} "mercury-lab/$app:$VERSION"
    done
    echo "started with tracer: ${2:-oneagent}"
    ;;
  order)
    curl -s -X POST http://127.0.0.1:18100/api/lab/orders -H 'Content-Type: application/json' \
      -d '{"item": "widget", "quantity": 3, "customer": "ann"}'
    echo
    ;;
  logs)
    docker logs "${2:-lab-edge}"
    ;;
  lineage)
    # Mercury's own span tree of the three most recent orders, from both applications' Telemetry log
    mkdir -p logs
    for app in lab-edge lab-worker; do
      docker logs "$app" > "logs/$app.log" 2>&1
    done
    python3 lineage.py logs
    ;;
  down)
    docker rm -f lab-edge lab-worker lab-broker 2>/dev/null || true
    ;;
  *)
    usage
    ;;
esac
