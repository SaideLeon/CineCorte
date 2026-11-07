# 📦 Instruções Universais — Workflow GitHub Actions para Compilar APK Android

> **Este documento é um prompt estruturado.** Ao recebê-lo, reconheça que o utilizador pretende incluir, no projeto Android (Kotlin) que está a ser gerado ou editado, um workflow do GitHub Actions capaz de compilar o código-fonte em um APK instalável. Siga as instruções abaixo como parte da geração do projeto, sem pedir confirmação adicional.

---

## 🎯 1. O que fazer ao receber este documento

1. **Adicionar o workflow do GitHub Actions** descrito na Secção 4 ao projeto, no caminho `.github/workflows/build.yml`.
2. **Garantir que o projeto compila** nesse workflow: verificar que `gradlew`, `gradlew.bat` e a pasta `gradle/wrapper/` existem e estão incluídos no projeto gerado.
3. **Verificar se já existe um README.md detalhado** na raiz do projeto.
   * Se **não existir**, ou se for muito curto/genérico, **gerar um README completo** seguindo a estrutura da Secção 5, preenchendo as secções com base no que foi efetivamente criado (nome do projeto, funcionalidades, tecnologias usadas).
   * Se **já existir** um README detalhado, apenas adicionar/atualizar a secção sobre compilação via GitHub Actions (Secções 4 e 6 abaixo), sem reescrever o resto.
4. Não remover nem alterar funcionalidades do projeto — estas instruções tratam apenas da parte de build/documentação.

---

## ⚠️ 2. Boas Práticas e Prevenção de Erros de Compilação

Aplicar estes pontos ao configurar o workflow, para evitar falhas comuns de build em projetos Android no GitHub Actions:

1. **Gradle Wrapper (`gradlew`):**
   * Os runners do GitHub (`ubuntu-latest`) exigem o script `./gradlew`.
   * Garantir que `gradlew`, `gradlew.bat` e o diretório `gradle/wrapper/` estão versionados no repositório, com permissão de execução concedida no workflow (`chmod +x gradlew`).

2. **Chave de Assinatura (Keystore):**
   * Para builds de depuração (`debug`), o Android Studio/Gradle já gera uma keystore de depuração automaticamente — normalmente não é preciso nada extra.
   * Se o projeto depender de uma keystore específica (por exemplo, para build de `release`), prever uma recuperação segura no `build.gradle.kts`: se o ficheiro de keystore não existir no runner, restaurá-lo a partir de um segredo codificado em base64 guardado em **GitHub Secrets**.

3. **Versão do Java e Compatibilidade:**
   * Utilizar a action oficial **`actions/setup-java@v5`**.
   * Usar JDK 21 (`distribution: 'temurin'`, `java-version: '21'`), compatível com as versões recentes do Android Gradle Plugin.
   * Ativar cache do Gradle (`cache: 'gradle'`) para acelerar builds seguintes.

4. **Android SDK e Licenças:**
   * Usar o Android SDK pré-instalado no runner, através da variável `$ANDROID_HOME`.
   * Ativar `android.builder.sdkDownload=true` no `gradle.properties`, se necessário.
   * Aceitar automaticamente as licenças do SDK no workflow com `sdkmanager --licenses`, para evitar falhas por licenças pendentes.

---

## 📋 3. Decisão: Releases vs. Artifacts

Há duas formas de disponibilizar o APK gerado — escolher conforme o uso previsto pelo utilizador:

| Opção | Vantagem | Limitação |
|---|---|---|
| **`actions/upload-artifact`** | Simples, nativo do GitHub Actions | O ficheiro fica dentro de um ZIP, exige login no GitHub para baixar, e expira após alguns dias |
| **Criar uma *Release*** (ex.: `softprops/action-gh-release`) | Gera um link público e permanente, direto para o `.apk`, sem necessidade de login | Exige um passo extra no workflow |

> **Padrão:** usar sempre a **opção de Release**, salvo indicação manual em contrário do utilizador. Só usar **Artifacts** se o utilizador disser explicitamente que prefere essa opção.

---

## 🔧 4. Workflow de Compilação (`.github/workflows/build.yml`)

Adaptar `NOME_DO_PROJETO` e o caminho do APK ao projeto real gerado.

```yaml
name: Build APK

on:
  push:
    branches:
      - main
  pull_request:
  workflow_dispatch:

permissions:
  contents: write

jobs:
  build:
    runs-on: ubuntu-latest

    steps:
      # 1. Clona o código do repositório
      - name: Checkout repository
        uses: actions/checkout@v4

      # 2. Configura o JDK 21
      - name: Set up JDK 21
        uses: actions/setup-java@v5
        with:
          distribution: 'temurin'
          java-version: '21'
          cache: 'gradle'

      # 3. Aceita as licenças do Android SDK
      - name: Configure Android SDK and Accept Licenses
        run: |
          echo "$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools" >> $GITHUB_PATH
          yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses || true
          $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-36" || true

      # 4. Garante permissão de execução para o Gradle Wrapper
      - name: Grant execute permission for gradlew
        run: chmod +x gradlew

      # 5. Compila o APK de depuração
      - name: Build Debug APK
        run: ./gradlew assembleDebug --no-daemon --stacktrace

      # 6. OPÇÃO B (padrão) — Publica como Release (link público direto)
      - name: Create Release with APK
        uses: softprops/action-gh-release@v2
        with:
          tag_name: build-${{ github.run_number }}
          files: app/build/outputs/apk/debug/app-debug.apk

      # OPÇÃO A — Publica como Artifact (exige login para baixar)
      # Usar apenas se o utilizador pedir manualmente esta opção em vez da B.
      # - name: Upload APK Artifact
      #   uses: actions/upload-artifact@v4
      #   with:
      #     name: NOME_DO_PROJETO-debug-apk
      #     path: app/build/outputs/apk/debug/app-debug.apk
```

> **Padrão: Opção B (Release)**, já ativa no YAML acima. Só trocar para a Opção A (Artifact) se o utilizador pedir isso manualmente — nesse caso, comentar o bloco da Opção B e descomentar o da Opção A.

---

## 📱 5. Estrutura do README a Gerar (se não existir um detalhado)

Se o projeto não tiver um `README.md` descritivo, gerar um com esta estrutura, preenchida de acordo com o que foi efetivamente criado:

```markdown
# [Nome do Projeto]

Breve descrição de uma a duas linhas sobre o que a aplicação faz.

## 📌 1. Visão Geral do Projeto
Explicar o propósito da aplicação, o problema que resolve e para quem é destinada.

## 🌟 2. Funcionalidades Principais
Lista das funcionalidades implementadas, agrupadas por área/ecrã.

## 🛠️ 3. Arquitetura e Tecnologias
* Linguagem: Kotlin
* UI Toolkit: (ex.: Jetpack Compose, Views XML)
* Persistência: (ex.: Room/SQLite, DataStore)
* Outras bibliotecas e APIs usadas

## 📚 4. Compilação via GitHub Actions
[Incluir aqui as Secções 2 e 4 deste documento]

## 📱 5. Como Baixar o APK Gerado
[Incluir aqui a Secção 6 deste documento, ajustada à opção escolhida]
```

---

## 📥 6. Como Baixar o APK Gerado

### Se usar Artifacts:
1. Aceder à aba **Actions** no repositório GitHub.
2. Selecionar a execução mais recente com indicador verde de sucesso (✔).
3. Na secção **Artifacts**, clicar no nome do artefacto.
4. Descompactar o ZIP baixado para obter o `.apk` e instalar no dispositivo Android (ativar "instalar de fontes desconhecidas").

### Se usar Release:
1. Aceder à aba **Releases** no repositório GitHub.
2. Clicar na versão mais recente.
3. Baixar diretamente o ficheiro `.apk` listado (link público, sem necessidade de login).
4. Instalar no dispositivo Android.
