# Лабораторные работы №2–6

Авторы: Важенин С.С., Фенько А.Н., ПИ-241.

Консольное приложение проверяет доступ к иерархическим ресурсам для действий `read`, `write` и `execute`. Пользователи, ресурсы и права хранятся в локальной файловой H2 через JDBC. Начальные учётные записи: `alice` / `qwerty`, `bob` / `secret`. Пароль проверяется по SHA-256 от UTF-8 строки `salt + password`.

## Структура

```text
src/                  приложение: domain, application, adapter, config
tests/                JUnit 5: модульные тесты и отдельная integration-проверка
lib/                  готовые JAR-зависимости и SHA256SUMS
scripts/              bash-скрипты сборки, запуска и тестирования
docs/                 актуальная инструкция проекта
doc/lab3/             планы рефакторинга, архитектуры и SonarCloud
doc/lab5/             отчёт о модульном тестировании
doc/lab6/             план SQL, ER-диаграмма и результаты JDBC-проверок
data/                 локальная H2 access.mv.db (игнорируется Git)
out/                  сгенерированные tests.jar и XML-результаты (игнорируется Git)
```

Лабораторная №6 — [sql-plan.md](../doc/lab6/sql-plan.md): сущности, ER-диаграмма, JDBC и коды ошибок. Отчёт №5 — [testable.md](../doc/lab5/testable.md). Планы предыдущих работ: [рефакторинг](../doc/lab3/REFACTORING_PLAN.md), [чистая архитектура](../doc/lab3/CLEAN_ARCHITECTURE.md), [SOLID](../doc/lab3/SOLID_ANALYSIS.md), [SonarCloud](../doc/lab3/SONARCLOUD_PLAN.md).

## Подготовка

Нужны JDK 17+ и Kotlin CLI 1.9+ в `PATH`, Git Bash на Windows либо bash 4.4+ на Linux/WSL, а также `jar`, `unzip`, `find` и `curl`. Проверено с Kotlin 2.4.20 и OpenJDK 26.0.2.1. WSL должен использовать Linux-версии Java и Kotlin.

Gradle, Maven и другие системы сборки не используются. Все библиотеки уже включены в `lib/`; для обычной сборки и запуска тестов сеть не нужна. Вручную скачаны H2 2.3.232, JUnit Platform Console Standalone 1.12.2, Jupiter API/Engine/Params 5.12.2, Platform Commons/Engine 1.12.2, OpenTest4J 1.3.0 и API Guardian 1.1.2. Источник — Maven Central; названия, версии и SHA-256 зафиксированы в `lib/SHA256SUMS`.

Проверить контрольные суммы или восстановить отсутствующие зависимости:

```bash
bash scripts/setup-libs.sh
```

Команды ниже выполняются из корня проекта. В Windows PowerShell используйте Git Bash явно, если `bash` указывает на неустановленный WSL:

```powershell
& 'C:\Program Files\Git\bin\bash.exe' scripts/test.sh
```

## Сборка и запуск

```bash
bash scripts/init-db.sh
bash scripts/build.sh
java -jar app.jar --login alice --password qwerty --action read --resource A.B.C --volume 10
```

Инициализация создаёт `data/access.mv.db` и наполняет его данными из `scripts/init.sql` и `scripts/fill.sql`. Повторный запуск не удаляет данные и не перезаписывает существующие лимиты/хеши. Сборка включает Kotlin runtime, `kotlinx-cli` и H2 в `app.jar`. Альтернативный запуск со сборкой и справка:

```bash
bash scripts/run.sh --login alice --password qwerty --action read --resource A.B.C --volume 10
java -jar app.jar --help
```

Формат CLI не изменён; обычный запуск возвращает только код, без текста. БД автоматически не создаётся: при отсутствии файла возвращается 9, при ошибке SQL — 10. Справка работает без БД. Другой файл можно выбрать переменной окружения `APP_DB_PATH` (путь без `.mv.db`):

```bash
export APP_DB_PATH="$(pwd)/data/another-access"
bash scripts/init-db.sh
bash scripts/run.sh --login alice --password qwerty --action read --resource A --volume 1
```

В PowerShell: `$env:APP_DB_PATH='C:/path/to/data/access'`. Прямой `java -jar app.jar` по умолчанию ищет `data/access` относительно текущего каталога; `scripts/run.sh` использует абсолютный путь к `data/access` проекта. Для запуска не нужны H2 Console или сервер. Параллельное открытие файла другой JVM может вернуть 9; закройте консоль перед запуском приложения.

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

JUnit выполняет 176 проверок: 151 модульную и 25 интеграционных с файловой H2. Параметризованные вызовы считаются отдельными тестами. CLI-скрипт выполняет ещё 17 сценариев: исходные десять, три ошибки синтаксиса, повторную инициализацию, отсутствие БД, справку без БД и SQL-ошибку. Для бизнес-исходов и JDBC-ошибок проверяется также отсутствие stdout/stderr. Любое падение возвращает ненулевой код; отсутствие обнаруженных JUnit-тестов тоже считается ошибкой. XML-отчёт JUnit сохраняется в `out/test-results/TEST-junit-jupiter.xml`.

Тесты создают свои БД в `@TempDir` и `out/cli-db.*/`. Рабочая `data/access.mv.db` не меняется и для тестов не требуется. Чтобы проверить всё после клонирования, достаточно `bash scripts/test.sh`.

Classpath автоматически использует `;` для Windows JVM в Git Bash и `:` для Linux. В JUnit Console classpath передаются `out/tests.jar`, `kotlinx-cli` и H2: Standalone уже содержит JUnit. Отдельные Jupiter/Platform JAR предназначены для IDEA и не дублируются при консольном запуске.

Для ручного запуска после `build-tests.sh` в Windows:

```bash
java -jar lib/junit-platform-console-standalone-1.12.2.jar execute \
  --class-path 'out/tests.jar;lib/kotlinx-cli-jvm-0.3.6.jar;lib/h2-2.3.232.jar' \
  --scan-class-path --include-engine junit-jupiter --fail-if-no-tests
```

В Linux замените `;` на `:`. На JDK 26 JUnit 5.12.2 может выводить предупреждение reflective final-field mutation из встроенной библиотеки picocli; результат тестов определяется итогом JUnit и кодом завершения.

## IntelliJ IDEA

В текущем локальном модуле `tests/` отмечен как Test Sources, добавлены библиотеки CLI, H2 и JUnit. Файлы IDE остаются локальными и игнорируются Git. При открытии нового клона:

1. Отметьте `src/` как Sources Root, `tests/` как Test Sources Root, `out/` как Excluded.
2. В Project Structure → Modules → Dependencies добавьте `lib/kotlinx-cli-jvm-0.3.6.jar` с областью Compile.
3. Добавьте отдельные JUnit JAR из `lib/` с областью Test: Jupiter API/Engine/Params, Platform Commons/Engine, OpenTest4J, API Guardian. Standalone при этом не добавляйте.
4. Выберите установленный JDK и Kotlin runtime. Запускайте класс или пакет через значок JUnit в редакторе. `MainRunTest` использует `internal run`; исходники и тесты должны относиться к одному IDEA-модулю.
5. Добавьте `lib/h2-2.3.232.jar` в зависимости. Для запуска `MainKt` — Compile scope, только для тестов — Test. Рабочий каталог JUnit задайте корнем проекта, чтобы находились SQL-скрипты, либо укажите VM option `-Dproject.root=<путь-к-проекту>`.

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
| 9 | Ошибка подключения к БД |
| 10 | Ошибка SQL-запроса или чтения результата |
| 127 | Ошибка синтаксиса аргументов: процесс завершает сама `kotlinx-cli` |

Порядок проверки: логин → пароль → действие → путь → объём → существование ресурса → право → лимит. Синтаксические ошибки CLI обрабатываются раньше сценария доступа; их фактическое поведение сохранено.

## Источники

- [JUnit 5.12.2 User Guide: Console Launcher и параметризованные тесты](https://docs.junit.org/5.12.2/user-guide/).
- [Maven Central: JUnit Platform Console Standalone 1.12.2](https://repo.maven.apache.org/maven2/org/junit/platform/junit-platform-console-standalone/1.12.2/).
- [Исходный код kotlinx-cli 0.3.6: обработка и завершение при ошибках](https://github.com/Kotlin/kotlinx-cli/blob/v0.3.6/core/commonMain/src/ArgParser.kt).
