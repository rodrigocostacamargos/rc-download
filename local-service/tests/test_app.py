"""
Testes unitários do serviço Flask.
Nenhum teste faz chamada real ao yt-dlp ou à internet.
Execute com: pytest tests/ -v --cov=app
"""

import json
import sys
import os
from unittest.mock import MagicMock, patch

import pytest

# Permite importar o módulo app a partir da pasta raiz do serviço
sys.path.insert(0, os.path.dirname(os.path.dirname(os.path.abspath(__file__))))

from app import app, jobs, _progress_hook


# ── Fixtures ─────────────────────────────────────────────────────────────────

@pytest.fixture
def client():
    """Cliente de teste Flask com modo TESTING ativo."""
    app.config["TESTING"] = True
    with app.test_client() as client:
        yield client


@pytest.fixture(autouse=True)
def limpa_jobs():
    """Garante que o dicionário de jobs começa vazio em cada teste."""
    jobs.clear()
    yield
    jobs.clear()


# ── POST /download ────────────────────────────────────────────────────────────

class TestDownloadEndpoint:

    def test_url_ausente_retorna_400(self, client):
        resp = client.post("/download", json={"format": "video"})
        assert resp.status_code == 400
        assert "url" in resp.get_json()["error"].lower()

    def test_url_vazia_retorna_400(self, client):
        resp = client.post("/download", json={"url": "   ", "format": "video"})
        assert resp.status_code == 400

    def test_formato_invalido_retorna_400(self, client):
        resp = client.post("/download", json={
            "url":    "https://youtube.com/watch?v=abc",
            "format": "gif"
        })
        assert resp.status_code == 400
        assert "format" in resp.get_json()["error"].lower()

    def test_body_json_ausente_retorna_400(self, client):
        resp = client.post("/download", data="nao e json", content_type="text/plain")
        assert resp.status_code == 400

    def test_requisicao_valida_video_retorna_job_id(self, client):
        with patch("app.threading.Thread") as mock_thread:
            mock_thread.return_value.start = MagicMock()
            resp = client.post("/download", json={
                "url":    "https://youtube.com/watch?v=dQw4w9WgXcQ",
                "format": "video"
            })
        assert resp.status_code == 200
        data = resp.get_json()
        assert "job_id" in data
        assert data["status"] == "pending"
        assert len(data["job_id"]) == 36  # UUID v4 tem 36 caracteres

    def test_requisicao_valida_audio_retorna_job_id(self, client):
        with patch("app.threading.Thread") as mock_thread:
            mock_thread.return_value.start = MagicMock()
            resp = client.post("/download", json={
                "url":    "https://youtube.com/watch?v=dQw4w9WgXcQ",
                "format": "audio"
            })
        assert resp.status_code == 200
        assert resp.get_json()["status"] == "pending"

    def test_job_e_registrado_no_dicionario(self, client):
        with patch("app.threading.Thread") as mock_thread:
            mock_thread.return_value.start = MagicMock()
            resp = client.post("/download", json={
                "url":    "https://youtube.com/watch?v=dQw4w9WgXcQ",
                "format": "video"
            })
        job_id = resp.get_json()["job_id"]
        assert job_id in jobs
        assert jobs[job_id]["status"] == "pending"
        assert jobs[job_id]["progress"] == 0

    def test_jobs_distintos_para_requests_diferentes(self, client):
        with patch("app.threading.Thread") as mock_thread:
            mock_thread.return_value.start = MagicMock()
            r1 = client.post("/download", json={"url": "https://youtube.com/watch?v=aaa", "format": "video"})
            r2 = client.post("/download", json={"url": "https://youtube.com/watch?v=bbb", "format": "audio"})
        assert r1.get_json()["job_id"] != r2.get_json()["job_id"]


# ── GET /status/<job_id> ──────────────────────────────────────────────────────

class TestStatusEndpoint:

    def test_job_inexistente_retorna_404(self, client):
        resp = client.get("/status/id-que-nao-existe")
        assert resp.status_code == 404

    def test_job_pendente_retorna_status_correto(self, client):
        jobs["job-001"] = {"status": "pending", "progress": 0, "file_path": None, "error": None}
        resp = client.get("/status/job-001")
        assert resp.status_code == 200
        data = resp.get_json()
        assert data["status"]   == "pending"
        assert data["progress"] == 0
        assert data["job_id"]   == "job-001"

    def test_job_downloading_retorna_progresso(self, client):
        jobs["job-002"] = {"status": "downloading", "progress": 65, "file_path": None, "error": None}
        resp = client.get("/status/job-002")
        data = resp.get_json()
        assert data["status"]   == "downloading"
        assert data["progress"] == 65

    def test_job_completed_retorna_file_path(self, client):
        jobs["job-003"] = {
            "status":    "completed",
            "progress":  100,
            "file_path": "/home/user/Downloads/rc-download/video.mp4",
            "error":     None,
        }
        resp = client.get("/status/job-003")
        data = resp.get_json()
        assert data["status"]    == "completed"
        assert data["progress"]  == 100
        assert data["file_path"] == "/home/user/Downloads/rc-download/video.mp4"

    def test_job_error_retorna_mensagem(self, client):
        jobs["job-004"] = {
            "status":    "error",
            "progress":  0,
            "file_path": None,
            "error":     "Video unavailable",
        }
        resp = client.get("/status/job-004")
        data = resp.get_json()
        assert data["status"] == "error"
        assert data["error"]  == "Video unavailable"

    def test_job_error_sem_mensagem_retorna_none(self, client):
        jobs["job-005"] = {"status": "error", "progress": 0, "file_path": None, "error": None}
        resp = client.get("/status/job-005")
        assert resp.get_json()["error"] is None


# ── GET /health ───────────────────────────────────────────────────────────────

class TestHealthEndpoint:

    def test_health_retorna_200_e_ok(self, client):
        resp = client.get("/health")
        assert resp.status_code == 200
        assert resp.get_json()["status"] == "ok"


# ── _progress_hook ────────────────────────────────────────────────────────────

class TestProgressHook:

    def test_atualiza_progresso_corretamente(self):
        jobs["job-ph"] = {"status": "downloading", "progress": 0}
        _progress_hook("job-ph", {
            "status":           "downloading",
            "downloaded_bytes": 75,
            "total_bytes":      100,
        })
        assert jobs["job-ph"]["progress"] == 75

    def test_calcula_progresso_arredondado(self):
        jobs["job-ph2"] = {"status": "downloading", "progress": 0}
        _progress_hook("job-ph2", {
            "status":           "downloading",
            "downloaded_bytes": 1,
            "total_bytes":      3,
        })
        assert jobs["job-ph2"]["progress"] == 33

    def test_nao_atualiza_quando_total_e_zero(self):
        jobs["job-ph3"] = {"status": "downloading", "progress": 0}
        _progress_hook("job-ph3", {
            "status":           "downloading",
            "downloaded_bytes": 0,
            "total_bytes":      0,
        })
        assert jobs["job-ph3"]["progress"] == 0  # não deve dividir por zero

    def test_usa_total_bytes_estimate_quando_total_bytes_ausente(self):
        jobs["job-ph4"] = {"status": "downloading", "progress": 0}
        _progress_hook("job-ph4", {
            "status":                 "downloading",
            "downloaded_bytes":       50,
            "total_bytes_estimate":   200,
        })
        assert jobs["job-ph4"]["progress"] == 25

    def test_ignora_hook_quando_status_nao_e_downloading(self):
        jobs["job-ph5"] = {"status": "pending", "progress": 0}
        _progress_hook("job-ph5", {"status": "finished"})
        assert jobs["job-ph5"]["progress"] == 0  # não deve alterar

    def test_progresso_100_ao_final(self):
        jobs["job-ph6"] = {"status": "downloading", "progress": 90}
        _progress_hook("job-ph6", {
            "status":           "downloading",
            "downloaded_bytes": 100,
            "total_bytes":      100,
        })
        assert jobs["job-ph6"]["progress"] == 100


# ── _download_worker (integração parcial com mock do yt-dlp) ─────────────────

class TestDownloadWorker:

    def test_worker_atualiza_status_para_completed(self):
        from app import _download_worker

        jobs["job-w1"] = {"status": "pending", "progress": 0, "file_path": None, "error": None}

        mock_info = {"title": "Test Video", "ext": "mp4"}
        mock_ydl  = MagicMock()
        mock_ydl.__enter__ = MagicMock(return_value=mock_ydl)
        mock_ydl.__exit__  = MagicMock(return_value=False)
        mock_ydl.extract_info.return_value  = mock_info
        mock_ydl.prepare_filename.return_value = "/downloads/Test Video.mp4"

        with patch("app.yt_dlp.YoutubeDL", return_value=mock_ydl):
            _download_worker("job-w1", "https://youtube.com/watch?v=abc", "video")

        assert jobs["job-w1"]["status"]    == "completed"
        assert jobs["job-w1"]["progress"]  == 100
        assert jobs["job-w1"]["file_path"] == "/downloads/Test Video.mp4"

    def test_worker_atualiza_status_para_error_em_excecao(self):
        from app import _download_worker

        jobs["job-w2"] = {"status": "pending", "progress": 0, "file_path": None, "error": None}

        mock_ydl = MagicMock()
        mock_ydl.__enter__ = MagicMock(return_value=mock_ydl)
        mock_ydl.__exit__  = MagicMock(return_value=False)
        mock_ydl.extract_info.side_effect  = Exception("Video unavailable")

        with patch("app.yt_dlp.YoutubeDL", return_value=mock_ydl):
            _download_worker("job-w2", "https://youtube.com/watch?v=xyz", "video")

        assert jobs["job-w2"]["status"] == "error"
        assert jobs["job-w2"]["error"]  == "Video unavailable"

    def test_worker_ajusta_extensao_para_mp3_em_audio(self):
        from app import _download_worker

        jobs["job-w3"] = {"status": "pending", "progress": 0, "file_path": None, "error": None}

        mock_info = {"title": "Song", "ext": "webm"}
        mock_ydl  = MagicMock()
        mock_ydl.__enter__ = MagicMock(return_value=mock_ydl)
        mock_ydl.__exit__  = MagicMock(return_value=False)
        mock_ydl.extract_info.return_value   = mock_info
        mock_ydl.prepare_filename.return_value = "/downloads/Song.webm"

        with patch("app.yt_dlp.YoutubeDL", return_value=mock_ydl):
            _download_worker("job-w3", "https://youtube.com/watch?v=abc", "audio")

        # A extensão deve ser convertida para .mp3
        assert jobs["job-w3"]["file_path"].endswith(".mp3")
