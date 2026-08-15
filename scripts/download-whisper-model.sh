#!/bin/sh
set -eu

model=${1:-small}
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
mkdir -p "$project_dir/models/whisper"

docker run --rm \
  -v "$project_dir/models/whisper:/models" \
  ghcr.io/ggml-org/whisper.cpp:main \
  "./models/download-ggml-model.sh $model /models"
