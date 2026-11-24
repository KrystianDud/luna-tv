#!/bin/sh
set -eu
cd "$(dirname "$0")/.."
test_build_dir=$(mktemp -d)
trap 'rm -rf "$test_build_dir"' EXIT HUP INT TERM
javac -d "$test_build_dir" \
  app/src/main/java/dev/local/lunatv/BrowserPolicy.java tests/BrowserPolicyTest.java \
  app/src/main/java/dev/local/lunatv/receiver/FirmwareImage.java tests/FirmwareImageTest.java
java -cp "$test_build_dir" dev.local.lunatv.BrowserPolicyTest
java -cp "$test_build_dir" dev.local.lunatv.receiver.FirmwareImageTest "$@"
node tests/receiver-gamepad.cjs
node tests/tv-viewport.cjs
