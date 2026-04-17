#!/bin/bash

PROJECT_DIR=$(dirname "$0")
cd "$PROJECT_DIR"

BENCHMARK_CLASS="${1:-MsgTemplateBenchmark}"
JMH_ARGS="${2:--wi 10 -i 5 -w 100ms -r 20ms -t 4 -f 3}"

CLASSPATH_FILE="/tmp/jmsg_classpath.txt"
/home/helly/app/maven-mvnd/bin/mvnd dependency:build-classpath -DincludeScope=test -Dmdep.outputFile="$CLASSPATH_FILE" -q

CLASSPATH="target/classes:target/test-classes:$(cat "$CLASSPATH_FILE")"

echo "Running JMH benchmark: $BENCHMARK_CLASS"
echo "Args: $JMH_ARGS"

java -cp "$CLASSPATH" org.openjdk.jmh.Main "$BENCHMARK_CLASS" $JMH_ARGS