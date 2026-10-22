# Лабораторные работы №2–5

Авторы: Важенин С.С., Фенько А.Н., ПИ-241.

Консольное приложение проверяет доступ к иерархическим ресурсам для действий `read`, `write` и `execute`. Тестовые учётные записи: `alice` / `qwerty`, `bob` / `secret`. Пароль проверяется по SHA-256 от UTF-8 строки `salt + password`.

## Структура

```text
src/                  приложение: domain, application, adapter, config
tests/                JUnit 5: модульные тесты и отдельная integration-проверка
lib/                  готовые JAR-зависимости и SHA256SUMS
scripts/              bash-скрипты сборки, запуска и тестирования
docs/                 документация и отчёты
out/                  сгенерированные tests.jar и XML-результаты (игнорируется Git)
```

Отчёт лабораторной №5 — [testable.md](testable.md). Планы предыдущих работ: [рефакторинг](REFACTORING_PLAN.md), [чистая архитектура](CLEAN_ARCHITECTURE.md), [SOLID](SOLID_ANALYSIS.md), [SonarCloud](SONARCLOUD_PLAN.md).

## Подготовка

Нужны JDK 17+ и Kotlin CLI 1.9+ в `PATH`, Git Bash на Windows либо bash 4.4+ на Linux/WSL, а также `jar`, `unzip`, `find` и `curl`. Проверено с Kotlin 2.4.20 и OpenJDK 26.0.2.1. WSL должен использовать Linux-версии Java и Kotlin.

Gradle, Maven и другие системы сборки не используются. Все библиотеки уже включены в `lib/`; для обычной сборки и запуска тестов сеть не нужна. Вручную скачаны JUnit Platform Console Standalone 1.12.2, Jupiter API/Engine/Params 5.12.2, Platform Commons/Engine 1.12.2, OpenTest4J 1.3.0 и API Guardian 1.1.2. Источник — Maven Central; названия, версии и SHA-256 зафиксированы в `lib/SHA256SUMS`.

Проверить контрольные суммы или восстановить отсутствующие JUnit-зависимости:

```bash
bash scripts/setup-libs.sh
```

Команды ниже выполняются из корня проекта. В Windows PowerShell используйте Git Bash явно, если `bash` указывает на неустановленный WSL:

```powershell
& 'C:\Program Files\Git\bin\bash.exe' scripts/test.sh
```

## Сборка и запуск

```bash
bash scripts/build.sh
java -jar app.jar --login alice --password qwerty --action read --resource A.B.C --volume 10
```

Сборка включает Kotlin runtime и `kotlinx-cli` в `app.jar`. Альтернативный запуск со сборкой и справка:

```bash
bash scripts/run.sh --login alice --password qwerty --action read --resource A.B.C --volume 10
java -jar app.jar --help
```

## Тестирование

```bash
# Полная проверка: JUnit, затем отдельные процессы CLI
bash scripts/test.sh

# Только компиляция приложения и тестов
bash scripts/build-tests.sh

# Только JUnit (включает компиляцию)
bash scripts/test-unit.sh

# Только регрессия запуска приложения (включает сборку app.jar)
bash scripts/test-cli.sh

# Выбор тестового класса для цикла TDD
bash scripts/test-unit.sh --include-classname '.*VolumeValidatorTest'
```

JUnit выполняет 145 проверок: 132 модульные и 13 интеграционных. Параметризованные вызовы считаются отдельными тестами. CLI-скрипт выполняет ещё 13 сценариев: исходные десять и три ошибки синтаксиса. Любое падение возвращает ненулевой код; отсутствие обнаруженных JUnit-тестов тоже считается ошибкой. XML-отчёт JUnit сохраняется в `out/test-results/TEST-junit-jupiter.xml`.

Classpath автоматически использует `;` для Windows JVM в Git Bash и `:` для Linux. В JUnit Console classpath передаются только `out/tests.jar` и `kotlinx-cli`: Standalone уже содержит JUnit. Отдельные Jupiter/Platform JAR предназначены для IDEA и не дублируются при консольном запуске.

Для ручного запуска после `build-tests.sh` в Windows:

```bash
java -jar lib/junit-platform-console-standalone-1.12.2.jar execute \
  --class-path 'out/tests.jar;lib/kotlinx-cli-jvm-0.3.6.jar' \
  --scan-class-path --include-engine junit-jupiter --fail-if-no-tests
```

В Linux замените `;` на `:`. На JDK 26 JUnit 5.12.2 может выводить предупреждение reflective final-field mutation из встроенной библиотеки picocli; результат тестов определяется итогом JUnit и кодом завершения.

## IntelliJ IDEA

В текущем локальном модуле `tests/` отмечен как Test Sources, добавлены библиотеки CLI и JUnit. Файлы IDE остаются локальными и игнорируются Git. При открытии нового клона:

1. Отметьте `src/` как Sources Root, `tests/` как Test Sources Root, `out/` как Excluded.
2. В Project Structure → Modules → Dependencies добавьте `lib/kotlinx-cli-jvm-0.3.6.jar` с областью Compile.
3. Добавьте отдельные JUnit JAR из `lib/` с областью Test: Jupiter API/Engine/Params, Platform Commons/Engine, OpenTest4J, API Guardian. Standalone при этом не добавляйте.
4. Выберите установленный JDK и Kotlin runtime. Запускайте класс или пакет через значок JUnit в редакторе. `MainRunTest` использует `internal run`; исходники и тесты должны относиться к одному IDEA-модулю.

## Коды завершения

| Код | Значение |
|---|---|
| 0 | Доступ разрешён |
| 1 | Явный запрос справки |
| 2 | Неверный пароль |
| 3 | Неизвестный логин |
| 4 | Неизвестное действие |
| 5 | Недостаточно прав |
| 6 | Ресурс не найден |
| 7 | Неверный путь/объём либо пропущена обязательная опция |
| 8 | Превышен лимит объёма |
| 127 | Ошибка синтаксиса аргументов: процесс завершает сама `kotlinx-cli` |

Порядок проверки: логин → пароль → действие → путь → объём → существование ресурса → право → лимит. Синтаксические ошибки CLI обрабатываются раньше сценария доступа; их фактическое поведение сохранено.

## Источники

- [JUnit 5.12.2 User Guide: Console Launcher и параметризованные тесты](https://docs.junit.org/5.12.2/user-guide/).
- [Maven Central: JUnit Platform Console Standalone 1.12.2](https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.12.2/).
- [Исходный код kotlinx-cli 0.3.6: обработка и завершение при ошибках](https://github.com/Kotlin/kotlinx-cli/blob/v0.3.6/core/commonMain/src/ArgParser.kt).
