# Arquitetura — RC Download

## Visão Geral

RC Download é composto por dois componentes independentes que se comunicam via HTTP na rede local:

```
[Android App]  ──HTTP──▶  [Serviço Local Flask]  ──▶  [yt-dlp + ffmpeg]
     ▲                            │
     │                            │ arquivo baixado
     └─── DownloadManager ◀───────┘  (GET /file/<job_id>)
```

1. O app Android busca metadados do vídeo na YouTube Data API v3.
2. Envia a URL ao serviço Flask para iniciar o download.
3. Faz polling do progresso a cada 1,5s.
4. Ao completar, o `DownloadManager` do Android baixa o arquivo do serviço e salva em `Downloads/` no dispositivo.

---

## Componente 1 — Serviço Local (`local-service/`)

**Stack:** Python 3.11 · Flask 3.0.3 · yt-dlp 2026.2.4 · ffmpeg

**Ambiente:** Conda `lms_env` (`/home/rodrigo/miniconda3/envs/lms_env/`)

### Endpoints

| Método | Rota                  | Descrição                                              |
|--------|-----------------------|--------------------------------------------------------|
| POST   | `/download`           | Inicia o download; retorna `job_id`                    |
| GET    | `/status/<job_id>`    | Progresso (0–100) e status do job                      |
| GET    | `/file/<job_id>`      | Serve o arquivo para download pelo cliente Android     |
| GET    | `/health`             | Health check                                           |

### Fluxo interno

```
POST /download
    │
    ├─▶ valida body JSON (url, format)
    ├─▶ cria job em memória  { status: pending, progress: 0 }
    └─▶ spawn Thread(_download_worker)
            │
            ├─▶ status = "downloading"
            ├─▶ yt-dlp baixa o arquivo em ~/Downloads/rc-download/
            │     └─▶ progress_hook atualiza job["progress"] (0–100)
            └─▶ status = "completed" | "error"
```

- **Estado** armazenado em memória (`dict jobs`) — não persiste entre reinicializações.
- **Formato vídeo:** `bestvideo[ext=mp4]+bestaudio[ext=m4a]`
- **Formato áudio:** `bestaudio` → pós-processamento FFmpegExtractAudio → MP3 192kbps

### Iniciar o serviço

```bash
cd local-service
./start.sh
```

O script usa diretamente o Python do `lms_env`. O serviço sobe em `0.0.0.0:8080`.

### Rede (dispositivo físico)

Em WSL2, o serviço fica em `172.25.x.x` (IP interno). Para celular físico na rede local, é necessário um redirecionamento de porta no Windows:

```powershell
# PowerShell como Administrador
netsh interface portproxy add v4tov4 listenaddress=<IP-WiFi-Windows> listenport=8080 connectaddress=<IP-WSL2> connectport=8080
netsh advfirewall firewall add rule name="WSL2 RC-Download" dir=in action=allow protocol=TCP localport=8080
```

Então configurar `LOCAL_SERVICE_URL=http://<IP-WiFi-Windows>:8080/` em `android-app/local.properties`.

---

## Componente 2 — App Android (`android-app/`)

**Stack:** Kotlin · Android SDK 34 · min API 26 · Gradle 8.3.2

**Padrão arquitetural:** MVVM + Repository

### Camadas

```
┌─────────────────────────────────────┐
│  View (Activities)                  │
│  MainActivity · HistoryActivity     │
│  Observam UiState via StateFlow     │
└────────────────┬────────────────────┘
                 │ collect
┌────────────────▼────────────────────┐
│  ViewModel                          │
│  MainViewModel                      │
│  · Gerencia UiState (sealed class)  │
│  · Polling de progresso (coroutines)│
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│  Repository                         │
│  VideoRepository                    │
│  · Único ponto de acesso a dados    │
│  · Retorna Result<T>                │
└──────┬──────────────┬───────────────┘
       │              │
┌──────▼──────┐ ┌─────▼──────────────┐
│ YouTube API │ │ Serviço Local Flask│
│ (Retrofit)  │ │ (Retrofit)         │
└─────────────┘ └────────────────────┘
                        │ (ao completar)
               ┌────────▼───────────┐
               │  DownloadManager   │
               │  Salva arquivo em  │
               │  Downloads/ do     │
               │  dispositivo       │
               └────────────────────┘
       │
┌──────▼──────┐
│  Room DB    │
│  Histórico  │
└─────────────┘
```

### UiState (máquina de estados)

```
Idle
 └─▶ LoadingMetadata ──sucesso──▶ MetadataLoaded
          │                            │
          └──erro──▶ Error             ▼ (usuário clica download)
                                  Downloading
                                       │
                                  DownloadProgress(0..100)
                                       │
                            ┌──────────┴──────────┐
                         sucesso               erro
                            │                    │
                    DownloadComplete          Error
```

### Injeção de dependências

Manual via `MainViewModelFactory`:
- Constrói `OkHttpClient` com `HttpLoggingInterceptor` (nível BODY em debug)
- Cria dois clientes Retrofit: YouTube API (`googleapis.com`) e serviço local
- Instancia `VideoRepository` com todas as dependências

### Configuração

Arquivo `android-app/local.properties` (não commitado):

```properties
YOUTUBE_API_KEY=...            # Google Cloud Console → YouTube Data API v3
LOCAL_SERVICE_URL=http://...   # IP do serviço Flask acessível pelo celular
sdk.dir=/home/.../Android/Sdk
```

Esses valores são injetados em `BuildConfig` pelo `app/build.gradle.kts`.

### Logs (debug)

Todos os logs usam a tag `RCDownload`. Para monitorar em tempo real:

```bash
adb -s <ip>:<porta> logcat RCDownload:D OkHttp:D *:S
```

---

## Dependências externas

| Dependência        | Onde é usada            | Observação                                      |
|--------------------|-------------------------|-------------------------------------------------|
| YouTube Data API v3| Android (Retrofit)      | Chave configurada em `local.properties`         |
| yt-dlp             | Serviço Flask           | Requer atualização periódica para bypass de bot |
| ffmpeg             | Serviço Flask           | `sudo apt install ffmpeg`                       |
| Room               | Android                 | Histórico de downloads local                    |
| Coil               | Android                 | Carregamento de thumbnails                      |
