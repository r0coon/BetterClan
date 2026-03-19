#!/usr/bin/env bash
# Kompatibilität: leitet auf ./build weiter.
exec "$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)/build" "$@"
