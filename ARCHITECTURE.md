# Arquitetura — RC Download (branch standalone)

> **Branch `main`**: versão com servidor Flask + yt-dlp rodando no PC.
> **Branch `standalone`** *(este arquivo)*: versão autônoma — tudo roda no dispositivo Android.

## Visão Geral

```
[Android App]
     │
     ├─▶ NewPipe Extractor ──HTTP──▶ YouTube (extrai URL do stream)
     │
     └─▶ DownloadManager ──HTTP──▶ CDN do YouTube (baixa o arquivo)
                │
                └─▶ Downloads/ no dispositivo
```

Não há servidor externo. O NewPipe Extractor faz engenharia reversa da página do YouTube diretamente no app para obter metadados e a URL autenticada do stream. O `DownloadManager` do Android baixa o arquivo e salva em `Downloads/`.

---

## Componente único — App Android (`android-app/`)

**Stack:** Kotlin · Android SDK 34 · min API 26 · Gradle 8.3.2
**Sem dependência de API key ou servidor.**

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
│  · Sem polling — DownloadManager    │
│    assume o controle após enqueue   │
└────────────────┬────────────────────┘
                 │
┌────────────────▼────────────────────┐
│  Repository                         │
│  VideoRepository                    │
│  · fetchVideoMetadata → NewPipe     │
│  · getStreamData → NewPipe cache    │
│  · saveToHistory → Room             │
└──────┬──────────────────────────────┘
       │
┌──────▼──────────────────────────────┐
│  data/extractor/                    │
│  StreamExtractor   ← lógica NewPipe │
│  DownloaderImpl    ← OkHttp adapter │
│  StreamData        ← URL + filename │
└─────────────────────────────────────┘
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
                                  PreparingStream
                                       │
                            ┌──────────┴──────────┐
                         sucesso               erro
                            │                    │
                    DownloadEnqueued           Error
                  (DownloadManager assume)
```

### NewPipe Extractor — como funciona

1. `NewPipe.init(DownloaderImpl(okHttpClient))` — inicializado em `MainViewModelFactory`
2. `StreamInfo.getInfo(service, url)` — faz chamadas HTTP ao YouTube e parseia a resposta
3. `streamInfo.videoStreams` — streams progressivas MP4 (vídeo+áudio, até 720p)
4. `streamInfo.audioStreams` — streams de áudio M4A (AAC)

**Streams progressivas** contêm vídeo e áudio já mesclados — não é necessário ffmpeg.
**M4A (AAC)** é reproduzido nativamente pelo Android — não é necessário converter para MP3.

### Formato de arquivo

| Formato   | Container | Codec  | Obs                              |
|-----------|-----------|--------|----------------------------------|
| Vídeo     | MP4       | H.264  | Progressivo, até 720p            |
| Áudio     | M4A       | AAC    | Melhor bitrate disponível        |

### Iniciar o app

Sem servidor. Basta abrir o app no dispositivo.
Para desenvolvimento, gere e instale o APK:

```bash
cd android-app
./gradlew assembleDebug
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

### Configuração

`android-app/local.properties` só precisa de:

```properties
sdk.dir=/home/SEU_USUARIO/Android/Sdk
```

Sem API keys. Sem URLs de serviço.

---

## Diferenças em relação à branch `main`

| Aspecto              | `main` (com servidor)            | `standalone` (este)                |
|----------------------|----------------------------------|------------------------------------|
| Metadados            | YouTube Data API v3 (API key)    | NewPipe Extractor (sem key)        |
| Download             | Flask + yt-dlp no PC             | NewPipe Extractor no dispositivo   |
| Formato de áudio     | MP3 (via ffmpeg)                 | M4A/AAC (nativo Android)           |
| Qualidade de vídeo   | Melhor disponível (4K, etc.)     | Até 720p (stream progressiva)      |
| Dependências externas| Servidor Flask, API key, ffmpeg  | Nenhuma                            |
| Portabilidade        | Requer PC na rede local          | Funciona em qualquer rede          |
