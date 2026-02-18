# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Branches

| Branch       | Descrição                                                  |
|--------------|------------------------------------------------------------|
| `main`       | Versão com servidor Flask + yt-dlp rodando no PC           |
| `standalone` | Versão autônoma com NewPipe Extractor — sem servidor       |

## Comandos

### Android App (`android-app/`)

```bash
# Build debug APK
./gradlew build

# Run unit tests
./gradlew test

# Format code
./gradlew spotlessApply
```

### Instalar no dispositivo (ADB over Wi-Fi)

```bash
# Parear (primeira vez — código exibido em Opções do desenvolvedor > Depuração sem fio)
adb pair <ip>:<porta-pareamento> <código>

# Conectar
adb connect <ip>:<porta>

# Instalar APK
adb -s <ip>:<porta> install -r app/build/outputs/apk/debug/app-debug.apk
```

### Logs em tempo real

```bash
adb -s <ip>:<porta> logcat RCDownload:D OkHttp:D *:S
```

### Python Service — apenas na branch `main`

```bash
cd local-service
./start.sh          # usa Python do conda lms_env
```

## Arquitetura

### Branch `standalone` (NewPipe Extractor)

```
MainActivity → MainViewModel → VideoRepository → StreamExtractor (NewPipe)
                                              → DownloadHistoryDao (Room)
```

- **Sem servidor externo, sem API key**
- `StreamExtractor` usa NewPipe para extrair metadados e URLs de stream
- `DownloaderImpl` adapta OkHttp para o contrato `Downloader` do NewPipe
- `DownloadManager` do Android salva o arquivo em `Downloads/` com notificação
- Vídeo: MP4 progressivo (até 720p) · Áudio: M4A/AAC nativo

### Branch `main` (Flask + yt-dlp)

```
MainActivity → MainViewModel → VideoRepository → YouTubeApiService (Retrofit)
                                              → LocalDownloadService (Retrofit → Flask)
                                              → DownloadHistoryDao (Room)
```

- Metadados via YouTube Data API v3 (`YOUTUBE_API_KEY` em `local.properties`)
- Download feito no PC pelo Flask/yt-dlp, arquivo transferido via `DownloadManager`
- Para dispositivo físico: configurar `netsh portproxy` no Windows + `LOCAL_SERVICE_URL`

### Padrão compartilhado (ambas as branches)

- **MVVM + Repository** — `MainViewModel` gerencia `UiState` sealed class
- **Injeção de dependências manual** via `MainViewModelFactory` (sem Hilt)
- **Room** para histórico de downloads
- **Coil** para thumbnails
- Logs com tag `RCDownload`

## Configuração

`android-app/local.properties` (nunca commitado):

```properties
# Obrigatório em ambas as branches:
sdk.dir=/home/SEU_USUARIO/Android/Sdk

# Apenas na branch main:
YOUTUBE_API_KEY=...
LOCAL_SERVICE_URL=http://<IP-Windows>:8080/
```

## Arquivos-chave

| Arquivo | Propósito |
|---------|-----------|
| `android-app/app/src/main/java/com/rcdownload/ui/MainViewModel.kt` | UiState + lógica de negócio |
| `android-app/app/src/main/java/com/rcdownload/data/repository/VideoRepository.kt` | Acesso a dados |
| `android-app/app/src/main/java/com/rcdownload/ui/MainViewModelFactory.kt` | Wiring de DI |
| `android-app/app/src/main/java/com/rcdownload/data/extractor/StreamExtractor.kt` | NewPipe (standalone) |
| `android-app/gradle/libs.versions.toml` | Versões centralizadas |
| `local-service/app.py` | Serviço Flask (apenas branch main) |
