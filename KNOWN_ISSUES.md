# Problemas Conhecidos

## [standalone] NewPipe Extractor — "Initial WEB player response is not valid"

**Branch:** `standalone`
**Status:** ✅ Resolvido
**Aberto em:** 2026-02-18
**Resolvido em:** 2026-02-19

### Sintoma

Ao tentar buscar metadados de qualquer vídeo do YouTube, o app retornava:

```
org.schabi.newpipe.extractor.exceptions.ExtractionException: Initial WEB player response is not valid
```

Log relevante:
```
--> POST https://www.youtube.com/youtubei/v1/player?prettyPrint=false&$fields=...
<-- 400 (71ms, unknown-length body)
ExtractionException: Initial WEB player response is not valid
    at YoutubeStreamExtractor.onFetchPage(YoutubeStreamExtractor.java:786)
```

### Causa raiz

Nossa implementação de `DownloaderImpl` não adicionava o header `User-Agent` nas requisições HTTP. O YouTube InnerTube API rejeita com **400 Bad Request** qualquer requisição que não apresente um User-Agent de browser legítimo.

Não era um problema de versão do NewPipe Extractor nem de PoToken — era ausência de header.

### O que foi tentado (antes da causa real ser encontrada)

| Versão NewPipe | Resultado |
|----------------|-----------|
| v0.24.2        | 400 — Initial WEB player response is not valid |
| v0.25.2        | 400 — Initial WEB player response is not valid |

### Solução

Consultar a implementação oficial do `DownloaderImpl` no app NewPipe revelou o header obrigatório. Adicionado em `DownloaderImpl.kt`:

```kotlin
.addHeader("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:140.0) Gecko/20100101 Firefox/140.0")
```

Outras melhorias aplicadas na mesma correção:
- Timeout de leitura explícito de 30 segundos
- `DownloaderImpl` agora recebe `OkHttpClient.Builder` (em vez do client já construído) para aplicar o timeout internamente, seguindo o padrão da implementação oficial

### Referência

- [DownloaderImpl oficial do app NewPipe](https://github.com/TeamNewPipe/NewPipe/blob/dev/app/src/main/java/org/schabi/newpipe/DownloaderImpl.java)
