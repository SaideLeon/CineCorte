# CineCorte — Editor de Vídeo Não Linear para Android

CineCorte é uma aplicação Android nativa profissional desenvolvida em Kotlin e Jetpack Compose, concebida para editar vídeos de forma não linear com preservação total (100%) da resolução, proporção (aspect ratio), rotação e taxa de quadros (FPS) originais.

---

## 📌 1. Visão Geral do Projeto

A maioria dos editores de vídeo móveis altera ou força cortes, esticamentos ou barras pretas nas gravações dos utilizadores, prejudicando a qualidade do conteúdo criado em diferentes proporções (16:9, 9:16 vertical para Reels/Shorts, 1:1 quadrado, 4:5 ou 21:9 ultra-wide).

O **CineCorte** foi desenhado com base numa regra fundamental: **Preservar 100% as dimensões e características do vídeo original**. A área de edição adota a geometria real do primeiro vídeo importado através de metadados lidos com precisão cirúrgica via `MediaMetadataRetriever`. Na exportação, as definições predefinidas mantêm a resolução, taxa de quadros e proporção nativas com a máxima fidelidade audiovisual.

Destina-se a criadores de conteúdo, cineastas móveis, videomakers e utilizadores que exigem controlo total de corte, múltiplas faixas de áudio sincronizadas, transições cinematográficas e animações com keyframes.

---

## 🌟 2. Funcionalidades Principais

### 1. Preservação Total das Dimensões Originais
* **Leitura Cirúrgica de Metadados:** Extração via `MediaMetadataRetriever` de largura, altura, proporção real, rotação (0°, 90°, 180°, 270°), orientação, taxa de quadros (FPS) e taxa de bits.
* **Canvas Fiel:** Área de trabalho exatamente proporcional ao clipe original, sem nunca esticar, achatar, recortar bordas ou aplicar barras pretas de forma não solicitada.
* **Exportação Original por Padrão:** Preserva o codec original (H.264 / AVC) ou opcionalmente HEVC (H.265) na qualidade máxima.

### 2. Importação e Linha do Tempo Não Linear (Timeline)
* **Selectores Nativos Seguros:** Importação através do Android Photo Picker (`ActivityResultContracts.PickVisualMedia`) e Storage Access Framework (`OpenDocument`), sem necessidade de permissões amplas de armazenamento.
* **Formatos Suportados:** Vídeos (MP4, MOV, WebM) e Áudios (MP3, WAV, AAC, M4A).
* **Múltiplas Faixas:** Uma faixa de vídeo principal articulada com múltiplas faixas de áudio independentes com cores e etiquetas temáticas (Voz/Fala, Música de fundo, Som ambiente, Efeitos sonoros).
* **Zoom da Linha do Tempo por Pinça:** Gesto de pinch-to-zoom com dois dedos, expandindo a escala temporal até ao nível de quadros individuais.
* **Playhead Interativo:** Cursor vermelho vertical com arrasto em tempo real, timecode SMPTE (`hh:mm:ss:ff`) e navegação quadro a quadro (+1 / -1 frame).

### 3. Ferramentas de Edição de Vídeo
* **Auto-Remoção de Pausas & Silêncios (Destaque):** Deteta e corta automaticamente trechos mortos, pausas longas e respirações na narração do vídeo. Permite calibrar o limiar de silêncio (-50dB a -20dB), duração mínima da pausa (250ms a 1200ms) e margem de segurança (padding) para não decepar as palavras. Exibe o resumo do tempo poupado e elimina todo o silêncio num só clique com *ripple delete*.
* **Cortar (Split):** Divisão instantânea de qualquer clipe no ponto exato do cursor.
* **Eliminar com Ripple Delete:** Ao apagar um trecho, o espaço fecha-se automaticamente (com opção de manter vão).
* **Aparar (Trim):** Ajuste fino de pontos de entrada (In) e saída (Out).
* **Duplicar e Reordenar:** Duplicação rápida e movimentação de clipes na timeline.
* **Controlo de Velocidade:** Câmara lenta e acelerada (de 0.25x até 4.0x) com chips rápidos de 0.5x, 1x, 1.5x e 2x.
* **Histórico Ilimitado:** Sistema robusto de Desfazer (Undo) e Refazer (Redo).

### 4. Separação e Tratamento de Áudio
* **Botão "Separar Áudio":** Extrai o fluxo de áudio do clipe de vídeo para uma faixa de áudio dedicada no mesmo instante temporal, permitindo editar, mover ou cortar o áudio sem afetar o vídeo.
* **Silenciar Vídeo:** Opção de silenciar o vídeo original mantendo apenas faixas sonoras adicionadas.
* **Visualização de Forma de Onda (Waveform):** Desenho da amplitude sonora diretamente sobre cada clipe de áudio.
* **Medidor de Nível (VU Meter):** Indicador visual estéreo (canais Esquerdo e Direito) com detecção de picos (-40dB a +3dB).
* **Mistura (Mixagem) Multifaixas:** Controlos de Volume (0% a 200%), Mudo (Mute), Solo, Panorâmica (Pan E/D) e Fade In/Out gradual.
* **Equalizador de 3 Bandas:** Ajustes de Graves (Bass 100Hz), Médios (Mid 1kHz) e Agudos (Treble 8kHz) de -12dB a +12dB.
* **Normalização e Redução de Ruído:** Nivelamento do padrão de áudio e filtro estático de ruído de fundo.
* **Ducking Automático:** Atenua suavemente a música de fundo durante a reprodução de faixas de voz.
* **Exportação de Áudio Isolado:** Permite guardar apenas o som do projeto em formato MP3, AAC ou WAV.

### 5. Transições Cinematográficas
* **Variedade de Efeitos:** Esmaecer para preto (Fade to Black), Dissolver (Dissolve), Deslizar em 4 direções (Slide Left/Right/Up/Down), Zoom In, Zoom Out, Varrimento (Wipe) e Flash Branco.
* **Ajuste de Duração e Easing:** Controlo de duração (200ms a 2000ms) e curvas de aceleração (`Linear`, `EaseIn`, `EaseOut`, `EaseInOut`).
* **Pré-visualização Imediata:** Renderização em tempo real na interface gráfica.

### 6. Zoom, Escala e Keyframes
* **Transformação Livre:** Ajuste de Escala (Zoom de 0.5x a 3.0x), Posição Horizontal (X), Posição Vertical (Y) e Rotação (-180° a +180°).
* **Gestos Diretos no Ecrã:** Arrastar com um dedo para mover o quadro, pinça com dois dedos para ampliar e rodar.
* **Animação por Keyframes:** Marcação de pontos-chave temporais com interpolação suave cúbica (Smoothstep) para criação de zoom progressivo e movimentos de câmara.
* **Botão Repor:** Retorna o clipe instantaneamente à escala 1.0x e posição central.

### 7. Exportação e Processamento em Segundo Plano
* **Foreground Service com Notificação:** Exportação através do `VideoExportService` registrado com o tipo `mediaProcessing` (compatível com Android 14+), mantendo o processo ativo mesmo com o aplicativo minimizado.
* **Notificação com Barra de Progresso:** Feedback contínuo de percentagem, contagem de quadros e botão direto para "Cancelar".
* **Registo na Galeria via MediaStore:** Guarda os ficheiros diretamente no armazenamento público multimídia do dispositivo (`MediaStore.Video.Media`).

### 8. Gestão e Persistência de Projetos
* **Base de Dados Room (SQLite):** Gravação automática contínua de todo o estado da linha do tempo, clipes, faixas de áudio, keyframes e configurações.
* **Gestor de Projetos:** Criação de novos projetos, alternância entre projetos existentes e eliminação de projetos antigos.

---

## 🛠️ 3. Arquitetura e Tecnologias

* **Linguagem:** Kotlin 2.2+
* **UI Toolkit:** Jetpack Compose (Material Design 3 - Tema Dark Studio profissional)
* **Arquitetura:** MVVM (Model-View-ViewModel) com StateFlow, Coroutines e Repositório
* **Reprodução e Pré-Visualização:** AndroidX Media3 ExoPlayer (`androidx.media3:media3-exoplayer`, `media3-ui`)
* **Processamento e Codificação:** AndroidX Media3 Transformer, MediaCodec nativo e MediaMetadataRetriever
* **Persistência Local:** AndroidX Room Database com KSP (Kotlin Symbol Processing)
* **Carregamento de Mídia:** Coil Compose
* **Compatibilidade:** Android 8.0+ (minSdk 26, targetSdk 36)

---

## 📚 4. Compilação via GitHub Actions

O projeto inclui um fluxo de trabalho automatizado do GitHub Actions configurado no ficheiro `.github/workflows/build.yml`. Este workflow compila o código-fonte num ambiente Ubuntu limpo e disponibiliza o ficheiro `.apk` para instalação.

### Como Funciona o Workflow:
1. Clona o repositório (`actions/checkout@v4`).
2. Configura o JDK 21 Temurin com cache do Gradle (`actions/setup-java@v5`).
3. Aceita automaticamente as licenças do Android SDK e prepara as ferramentas de linha de comando.
4. Concede permissão de execução ao script do Gradle Wrapper (`chmod +x gradlew`).
5. Compila o APK de depuração (`./gradlew assembleDebug --no-daemon --stacktrace`).
6. Publica o APK compilado como uma **Release** pública e direta através da action `softprops/action-gh-release@v2`.

---

## 📱 5. Como Baixar o APK Gerado

Para descarregar e instalar a aplicação no seu smartphone ou tablet Android:

1. Aceda à aba **Releases** no seu repositório GitHub (na coluna lateral direita da página do repositório).
2. Clique na versão mais recente gerada pela ação de compilação.
3. Descarregue diretamente o ficheiro `app-debug.apk` listado na secção **Assets**.
4. No seu telemóvel Android, abra o ficheiro descarregado para instalar (caso o sistema solicite, autorize a instalação de fontes desconhecidas para o navegador ou gestor de ficheiros).
