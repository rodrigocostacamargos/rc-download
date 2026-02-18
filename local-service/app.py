"""
Serviço local de download — Flask + yt-dlp
Uso pessoal e acadêmico. Não publicar na internet.

Endpoints:
  POST /download          → inicia download em background, retorna job_id
  GET  /status/<job_id>   → progresso do job (0-100)
  GET  /health            → verificação de disponibilidade
"""

import os
import threading
import uuid

import yt_dlp
from flask import Flask, jsonify, request, send_file

app = Flask(__name__)

# Armazenamento em memória — suficiente para uso pessoal (jobs não persistem entre reinicializações)
jobs: dict[str, dict] = {}

# Diretório de destino dos downloads
DOWNLOAD_DIR = os.path.expanduser("~/Downloads/rc-download")
os.makedirs(DOWNLOAD_DIR, exist_ok=True)


# ── Worker ───────────────────────────────────────────────────────────────────

def _download_worker(job_id: str, url: str, format_type: str) -> None:
    """Executa o download em uma thread separada para não bloquear o servidor."""
    jobs[job_id]["status"] = "downloading"

    output_template = os.path.join(DOWNLOAD_DIR, "%(title)s.%(ext)s")

    common_opts = {
        "outtmpl": output_template,
        "progress_hooks": [lambda d: _progress_hook(job_id, d)],
    }

    if format_type == "audio":
        ydl_opts = {
            **common_opts,
            "format": "bestaudio/best",
            "postprocessors": [{
                "key": "FFmpegExtractAudio",
                "preferredcodec": "mp3",
                "preferredquality": "192",
            }],
        }
    else:
        ydl_opts = {
            **common_opts,
            "format": "bestvideo[ext=mp4]+bestaudio[ext=m4a]/best[ext=mp4]/best",
        }

    try:
        with yt_dlp.YoutubeDL(ydl_opts) as ydl:
            info = ydl.extract_info(url, download=True)
            file_path = ydl.prepare_filename(info)
            # Ajusta extensão quando há pós-processamento de áudio
            if format_type == "audio":
                file_path = os.path.splitext(file_path)[0] + ".mp3"

            jobs[job_id].update({
                "status":    "completed",
                "progress":  100,
                "file_path": file_path,
            })
    except Exception as exc:
        jobs[job_id].update({
            "status": "error",
            "error":  str(exc),
        })


def _progress_hook(job_id: str, data: dict) -> None:
    """Atualiza o progresso do job durante o download."""
    if data.get("status") != "downloading":
        return
    total      = data.get("total_bytes") or data.get("total_bytes_estimate") or 0
    downloaded = data.get("downloaded_bytes") or 0
    if total > 0:
        jobs[job_id]["progress"] = int((downloaded / total) * 100)


# ── Endpoints ────────────────────────────────────────────────────────────────

@app.route("/download", methods=["POST"])
def start_download():
    """
    Body JSON: {"url": "...", "format": "video" | "audio"}
    Retorna:   {"job_id": "...", "status": "pending", "message": "..."}
    """
    data = request.get_json(silent=True) or {}
    url  = (data.get("url") or "").strip()

    if not url:
        return jsonify({"error": "O campo 'url' é obrigatório."}), 400

    format_type = data.get("format", "video")
    if format_type not in ("video", "audio"):
        return jsonify({"error": "O campo 'format' deve ser 'video' ou 'audio'."}), 400

    job_id = str(uuid.uuid4())
    jobs[job_id] = {
        "status":    "pending",
        "progress":  0,
        "file_path": None,
        "error":     None,
    }

    thread = threading.Thread(
        target=_download_worker,
        args=(job_id, url, format_type),
        daemon=True   # Thread encerra junto com o processo principal
    )
    thread.start()

    return jsonify({
        "job_id":  job_id,
        "status":  "pending",
        "message": "Download iniciado.",
    })


@app.route("/status/<job_id>", methods=["GET"])
def get_status(job_id: str):
    """Retorna o estado atual de um job de download."""
    job = jobs.get(job_id)
    if job is None:
        return jsonify({"error": f"Job '{job_id}' não encontrado."}), 404

    return jsonify({
        "job_id":    job_id,
        "status":    job["status"],
        "progress":  job["progress"],
        "file_path": job.get("file_path"),
        "error":     job.get("error"),
    })


@app.route("/file/<job_id>", methods=["GET"])
def get_file(job_id: str):
    """Serve o arquivo gerado pelo job para download pelo cliente Android."""
    job = jobs.get(job_id)
    if job is None:
        return jsonify({"error": f"Job '{job_id}' não encontrado."}), 404
    if job["status"] != "completed" or not job.get("file_path"):
        return jsonify({"error": "Arquivo ainda não disponível."}), 409
    return send_file(job["file_path"], as_attachment=True)


@app.route("/health", methods=["GET"])
def health():
    """Health check — o app Android chama isso antes de iniciar um download."""
    return jsonify({"status": "ok"})


if __name__ == "__main__":
    print(f"Serviço local rodando. Downloads em: {DOWNLOAD_DIR}")
    # Bind em 0.0.0.0 para ser acessível pelo emulador Android (via 10.0.2.2)
    app.run(host="0.0.0.0", port=8080, debug=False)
