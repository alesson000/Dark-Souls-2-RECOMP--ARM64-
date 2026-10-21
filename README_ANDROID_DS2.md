# Dark Souls II - Recompilação Estática para Android (ARM64)
### ReXGlue + Vulkan + AdrenoTools (Qualcomm Snapdragon / Adreno 710)

O processo de recompilação estática do binário `default.xex` do Xbox 360 para arquitetura **ARM64-v8a (Android)** foi concluído com sucesso total!

---

## 📦 Arquivo do APK Gerado
- **Caminho no computador:**  
  `C:\Users\Alesson\.gemini\antigravity\scratch\DarkSouls2-ARM64-Android.apk`
- **Tamanho:** ~132 MB (comprimido) / ~375 MB nativo (ELF de 230 MB de código de jogo recompilado + runtimes).
- **Arquitetura:** `arm64-v8a` (Android 11+ / API 30+).

---

## 🎮 Como Configurar e Jogar no seu Dispositivo (Snapdragon 6 Gen 3 / Adreno 710)

### 1. Instalar o APK no Celular
Conecte o seu celular com a depuração USB ativada e execute no terminal:
```powershell
adb install -r "C:\Users\Alesson\.gemini\antigravity\scratch\DarkSouls2-ARM64-Android.apk"
```
*(Ou envie o arquivo `.apk` diretamente para o celular e instale pelo gerenciador de arquivos).*

### 2. Copiar os Arquivos de Dados do Jogo
No armazenamento interno do seu celular (onde ficam as pastas `Download`, `Documents`, etc.), crie a pasta:
```
/sdcard/DarkSouls2/
```
*(Que é o mesmo que `/storage/emulated/0/DarkSouls2/`)*

Copie todos os arquivos extraídos do jogo de:
`C:\Users\Alesson\Downloads\Dark-Souls-2-RECOMP\Dark-Souls-2`
para dentro de `/sdcard/DarkSouls2/`:
- `default.xex`
- `GameData1.bdt`
- `GameData1.bhd`
- `GameData2.bdt`
- `GameData2.bhd`
- pasta `sound/`
- pasta `movie/`

### 3. Permissões de Armazenamento
Ao abrir o jogo pela primeira vez no Android 11, 12, 13 ou 14+, o aplicativo solicitará a permissão **"Acesso a todos os arquivos" (MANAGE_EXTERNAL_STORAGE)**.
> **Importante:** Ative essa permissão nas configurações do sistema para que o motor do jogo consiga ler os arquivos gigantes `.bdt` diretamente do armazenamento interno.

### 4. Otimização de GPU Adreno 710 & Driver Turnip
- **Turbo Clock Automático:** O binário nativo possui `libadrenotools` integrado e chama `adrenotools_set_turbo(true)` logo na inicialização, forçando as frequências máximas de clock da sua GPU Adreno 710 para evitar throttling prematuro.
- **Driver Customizado Mesa Turnip (Opcional):**
  Se quiser carregar um driver Turnip customizado (por exemplo, Turnip v24.3+ para Adreno 7xx), basta colocar os arquivos da biblioteca do driver na pasta `/sdcard/DarkSouls2/turnip/`. Se não houver driver customizado, o motor usará o driver Vulkan proprietário nativo do Snapdragon.

---

## 🛠️ Resumo Técnico do que foi Desenvolvido
1. **ReXGlue Codegen:**
   - Analisou o `default.xex` do Dark Souls II (Title ID `465307E4`, versão `0.0.0.8`).
   - Mapeou 47 pontos de desvio indireto e funções não-alinhadas em `config/default.toml`.
   - Gerou 332 arquivos C++ de alta precisão contendo 100% das funções do jogo.
2. **Portabilidade do SDK ReXGlue para Android Bionic:**
   - Adicionada camada de superfície Vulkan nativa para Android (`ANativeWindow`).
   - Adaptadas primitivas de sincronização POSIX para o Bionic (eliminando dependências inexistentes como robust mutexes e `<ucontext.h>`).
   - Resolvidas peculiaridades de libc++ e registradores FPCR AArch64 de 64 bits.
3. **Integração AdrenoTools:**
   - Compilado `libadrenotools.a` para `arm64-v8a` e vinculado diretamente em `libds2.so`.
4. **Empacotamento de APK Android:**
   - App nativo baseado em SDL3 (`DarkSouls2Activity`) com configuração `useLegacyPackaging = true` para suporte total a hooks de driver Vulkan.
