# Problemas Conhecidos

## [standalone] NewPipe Extractor — "Initial WEB player response is not valid"

**Branch:** `standalone`
**Status:** Aberto
**Data:** 2026-02-18

### Sintoma

Ao tentar buscar metadados de qualquer vídeo do YouTube, o app retorna o erro:

```
org.schabi.newpipe.extractor.exceptions.ExtractionException: Initial WEB player response is not valid
```

### Causa

O NewPipe Extractor tenta usar o client WEB do YouTube InnerTube API (`/youtubei/v1/player`), que retorna **HTTP 400**. O YouTube passou a rejeitar requisições do client WEB que não incluem um token de prova de origem (PoToken / Proof of Origin Token) válido, gerado por JavaScript no browser.

Log relevante:
```
--> POST https://www.youtube.com/youtubei/v1/player?prettyPrint=false&$fields=...
<-- 400 (71ms, unknown-length body)
ExtractionException: Initial WEB player response is not valid
    at YoutubeStreamExtractor.onFetchPage(YoutubeStreamExtractor.java:786)
```

### O que foi tentado

| Versão NewPipe | Resultado |
|---------------|-----------|
| v0.24.2       | 400 — Initial WEB player response is not valid |
| v0.25.2       | 400 — Initial WEB player response is not valid |

### Caminhos de investigação

1. **Atualizar para latest commit do dev branch do NewPipe** — versões de release podem estar desatualizadas; commits mais recentes podem usar client ANDROID ou IOS que ainda funciona sem PoToken.

2. **Forçar client ANDROID no NewPipe** — o client Android (`ANDROID_EMBEDDED_PLAYER`) não exige PoToken. Verificar se a versão atual expõe uma forma de selecionar o client via `YoutubeParsingHelper` ou `ClientInfoHelper`.

3. **Usar `ytdlp-android` / Seal** — apps Android que incorporam yt-dlp via binary nativo. Mais robusto a longo prazo, mas mais complexo de integrar.

4. **Embed Rhino JS engine** — NewPipe Extractor suporta engines JavaScript (Rhino, QuickJS via EJS) para resolver o PoToken. Adicionar `rhino` ou `quickjs-android` como dependência pode desbloquear o client WEB.
   - Dependência: `org.mozilla:rhino:1.7.15`
   - Inicialização: `NewPipe.init(downloader, RhinoJavaScriptAppEngine())`

5. **Reverter para branch `main`** — a versão com servidor Flask continua funcional enquanto a standalone é investigada.

### Referências

- [NewPipe Extractor — EJS (Embedded JS)](https://github.com/TeamNewPipe/NewPipeExtractor/wiki/EJS)
- [yt-dlp — mesma exigência de JS runtime](https://github.com/yt-dlp/yt-dlp/wiki/EJS)
- [Issue tracker NewPipe](https://github.com/TeamNewPipe/NewPipeExtractor/issues)
