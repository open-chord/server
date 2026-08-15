#!/bin/sh
set -eu

model=${1:-small}
project_dir=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
mkdir -p "$project_dir/models/whisper"

case "$(uname -m)" in
  arm64|aarch64) image=${WHISPER_IMAGE:-ghcr.io/ggml-org/whisper.cpp:main-arm64} ;;
  *) image=${WHISPER_IMAGE:-ghcr.io/ggml-org/whisper.cpp:main} ;;
esac

docker run --rm \
  -v "$project_dir/models/whisper:/models" \
  "$image" \
  "./models/download-ggml-model.sh $model /models"
