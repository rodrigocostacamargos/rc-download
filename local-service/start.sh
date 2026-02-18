#!/usr/bin/env bash
# Inicia o serviço local de download usando o ambiente conda lms_env.
# Execute uma vez antes de usar o app Android.

set -e

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
cd "$SCRIPT_DIR"

PYTHON="/home/rodrigo/miniconda3/envs/lms_env/bin/python"

if [ ! -f "$PYTHON" ]; then
    echo "Erro: ambiente conda lms_env não encontrado em $PYTHON"
    exit 1
fi

# Verifica se o ffmpeg está disponível (necessário para extração de áudio)
if ! command -v ffmpeg &> /dev/null; then
    echo "AVISO: ffmpeg não encontrado. Download de áudio (MP3) não funcionará."
    echo "Instale com: sudo apt install ffmpeg  (Linux) ou brew install ffmpeg (macOS)"
fi

echo "Iniciando serviço em http://0.0.0.0:8080"
echo "Downloads serão salvos em: ~/Downloads/rc-download"
"$PYTHON" app.py
