#!/usr/bin/env bash
# The Dynatrace OneAgent lab: a Kafka broker and two Mercury applications in Colima, with OneAgent
# installed full-stack in Colima's VM. See README.md.
set -euo pipefail
cd "$(dirname "$0")"
ROOT=../..
VERSION=4.12.25
APPS="lab-broker lab-edge lab-worker"

usage() {
  echo "usage: $0 build | up [oneagent|otel|none] | order | logs <lab-edge|lab-worker|lab-broker> | down"
  exit 1
}

case "${1:-}" in
  build)
    # Always with -am: the branch keeps version 4.12.25, so without it Maven takes the RELEASED platform-core
    # from ~/.m2, which has no invocation-observer interface, and the applications fail at startup.
    (cd "$ROOT" && mvn -o -q package -DskipTests -am \
      -pl lab/dynatrace-oneagent/lab-edge,lab/dynatrace-oneagent/lab-worker,helpers/kafka-standalone)
    cp "$ROOT/helpers/kafka-standalone/target/kafka-standalone-$VERSION-exec.jar" apps/lab-broker.jar
    cp "lab-edge/target/lab-edge-$VERSION.jar" apps/lab-edge.jar
    cp "lab-worker/target/lab-worker-$VERSION.jar" apps/lab-worker.jar
    for app in $APPS; do
      docker build -q -t "mercury-lab/$app:$VERSION" --build-arg APP="$app" apps
    done
    ;;
  up)
    case "${2:-oneagent}" in
      # DESTINATION=route names every hop after its route; the default names it after the application
      oneagent) tracer=(-e "JAVA_OPTS=-Ddynatrace.oneagent=true -Ddynatrace.oneagent.destination=${DESTINATION:-application}") ;;
      otel)     tracer=(-e OTEL_TRACING=true) ;;
      none)     tracer=() ;;
      *)        usage ;;
    esac
    # The broker advertises 127.0.0.1:9092, so the applications share its network namespace; the broker
    # container therefore publishes lab-edge's REST port too.
    docker run -d --name lab-broker -p 8100:8100 "mercury-lab/lab-broker:$VERSION"
    until docker exec lab-broker bash -c 'echo > /dev/tcp/127.0.0.1/9092' 2>/dev/null; do sleep 1; done
    for app in lab-worker lab-edge; do
      docker run -d --name "$app" --network container:lab-broker ${tracer[@]+"${tracer[@]}"} "mercury-lab/$app:$VERSION"
    done
    echo "started with tracer: ${2:-oneagent}"
    ;;
  order)
    curl -s -X POST http://127.0.0.1:8100/api/lab/orders -H 'Content-Type: application/json' \
      -d '{"item": "widget", "quantity": 3, "customer": "ann"}'
    echo
    ;;
  logs)
    docker logs "${2:-lab-edge}"
    ;;
  down)
    docker rm -f lab-edge lab-worker lab-broker 2>/dev/null || true
    ;;
  *)
    usage
    ;;
esac
