# RC Download

App Android (Kotlin) + serviço local (Python/Flask) para download de vídeos e áudios do YouTube.
**Uso estritamente pessoal e acadêmico.**

---

## Estrutura

```
rc-download/
├── android-app/          # App Android (Kotlin)
│   ├── app/src/main/     # Código-fonte principal
│   ├── app/src/test/     # Testes unitários (JUnit + MockK + Turbine)
│   └── local.properties.example
└── local-service/        # Serviço local Flask + yt-dlp
    ├── app.py
    ├── requirements.txt
    ├── start.sh
    └── tests/test_app.py
```

---

## Configuração

### 1. Chave da YouTube Data API v3

1. Acesse [Google Cloud Console](https://console.developers.google.com/)
2. Crie um projeto → Ative **YouTube Data API v3** → Crie uma **API Key**
3. Copie `android-app/local.properties.example` para `android-app/local.properties`
4. Preencha `YOUTUBE_API_KEY=SUA_CHAVE`

### 2. Serviço local

```bash
# Instale ffmpeg (necessário para extração de áudio)
sudo apt install ffmpeg        # Debian/Ubuntu/WSL
# brew install ffmpeg          # macOS

cd local-service
chmod +x start.sh
./start.sh
```

O serviço sobe em `http://0.0.0.0:8080`.
No emulador Android, use `http://10.0.2.2:8080/` (já é o default).
Em dispositivo físico na mesma rede, altere `LOCAL_SERVICE_URL` em `local.properties`.

### 3. App Android

Abra `android-app/` no Android Studio e execute normalmente.

---

## Testes

### Android (JUnit + MockK + Turbine)
```bash
cd android-app
./gradlew test
```

### Python (pytest)
```bash
cd local-service
source .venv/bin/activate
pytest tests/ -v --cov=app
```

---

## Limitações conhecidas

- **Termos de Serviço do YouTube:** o ToS proíbe download de conteúdo sem permissão explícita do YouTube, independentemente da licença do vídeo ou da finalidade (pessoal/acadêmica). O uso deste app é de responsabilidade do usuário.
- **Copyright:** a maioria dos vídeos do YouTube é protegida por direitos autorais. "Uso pessoal" não é uma exceção automática ao copyright em todas as jurisdições.
- **Subtipos de CC:** a YouTube Data API v3 retorna `"creativeCommon"` genericamente, sem diferenciar CC BY, CC BY-SA, CC BY-NC, etc.
- **ffmpeg obrigatório** para extração de áudio (MP3).
