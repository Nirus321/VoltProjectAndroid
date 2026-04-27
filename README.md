
# Volt – Uma Nova Forma De Fazer Desporto
> **Slogan:** "Uma Nova Forma De Fazer Desporto"

A **Volt** é uma aplicação móvel minimalista para monitorizar e registar atividades físicas (corrida, caminhada e ciclismo). Permite acompanhar métricas como passos, distância, tempo, ritmo e calorias, focando-se numa experiência simples e apelativa.

---
[Ver Manual de Utilizador](docs/manual_utilizador.pdf)

##  Especificações Técnicas

| Componente | Versão / Detalhe |
| :--- | :--- |
| **SDK Utilizado** | Compile: 34 / Min: 26 / Target: 34 |
| **Gradle** | Plugin: 8.x / Wrapper: 8.x |
| **Base de Dados** | SQLite (Local) & Firebase Realtime Database (Cloud) |
| **Linguagem** | Java / XML |

### Bibliotecas Principais
* **Firebase Authentication:** Gestão de utilizadores e segurança.
* **Firebase Realtime Database:** Sincronização de dados em tempo real.
* **OSMDroid:** Visualização de mapas e localização GPS (Licença Apache 2.0).
* **SQLite:** Persistência de dados offline.

---

##  Funcionalidades
* **Autenticação:** Login, registo e recuperação de password via Firebase.
* **Monitorização:** Contagem de passos em tempo real (`TYPE_STEP_COUNTER`).
* **Métricas Inteligentes:** Cálculo de distância, ritmo e calorias por tipo de atividade.
* **Modo Offline:** Suporte SQLite para uso sem internet com sincronização posterior.
* **Histórico:** Consulta detalhada de atividades e progresso diário.

---

## Processo de Importação
1.  Abrir o **Android Studio**.
2.  Selecionar **Open an existing project**.
3.  Escolher a pasta do projeto.
4.  Aguardar a sincronização do **Gradle**.
5.  Adicionar o ficheiro `google-services.json` (Firebase) na pasta `app/`.
6.  Executar num emulador ou dispositivo físico.

---

## Sensores e Lógica
* **Sensor de Passos:** Registo contínuo em segundo plano.
* **Otimização de Bateria:** Cálculos baseados em sensores de movimento para reduzir a dependência constante do GPS.
* **Segurança:** Restrição de acesso a dados por utilizador via Firebase Rules.

