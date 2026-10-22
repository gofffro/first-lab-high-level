# План интеграции SonarCloud

## Цель

Подключить автоматический анализ Kotlin-кода к ветке `master` и pull request так, чтобы новые ошибки качества и безопасности были видны до слияния. На первом этапе анализ проводится без изменения исходного кода и без добавления Gradle/Maven.

SonarQube Cloud (бывший SonarCloud) анализирует основную ветку и pull request; для pull request результат quality gate может отображаться в интерфейсе провайдера репозитория. Параметры `sonar.pullrequest.key`, `sonar.pullrequest.branch` и `sonar.pullrequest.base` нужны при ручном запуске, а поддерживаемые CI обычно передают их автоматически.

## Шаг 1. Подготовить доступы

1. Создать или выбрать организацию в SonarQube Cloud.
2. Импортировать репозиторий и создать проект с уникальными `projectKey` и `organization`.
3. Выбрать ветку по умолчанию `master` (или фактическую основную ветку репозитория).
4. Создать токен анализа с минимальными правами.
5. Сохранить токен в секретах CI под именем `SONAR_TOKEN`; не помещать его в `.md`, shell-скрипты или Git.
6. Проверить, поддерживает ли используемый GitHub/GitVerse CI pull request decoration. Если нет, оставлять ссылку на отчёт и проверять quality gate через API/статус pipeline.

## Шаг 2. Определить область анализа

Создать в корне `sonar-project.properties` только на этапе фактического подключения. Минимальные параметры:

```properties
sonar.projectKey=<organization>_<project>
sonar.organization=<organization>
sonar.sources=src
sonar.tests=src/test
sonar.sourceEncoding=UTF-8
sonar.exclusions=**/*.jar,**/build/**,**/out/**,**/.idea/**
```

Если тесты расположены в другом каталоге, скорректировать `sonar.tests`. Не включать `.idea`, бинарные JAR-файлы, временные каталоги и отчёты в исходники.

Для Kotlin проверить требования выбранной версии анализатора к bytecode. Если bytecode нужен, генерировать его существующим ручным `.sh`-скриптом и передавать `sonar.java.binaries`; систему сборки добавлять нельзя.

## Шаг 3. Выбрать способ запуска

Предпочтительный вариант — официальный SonarScanner CLI в CI:

1. Зафиксировать версию scanner в образе или подготовительном `.sh`-скрипте.
2. Выполнить проверку исходников и тестов.
3. Запустить `sonar-scanner` с `SONAR_TOKEN` из окружения.
4. Дождаться результата quality gate, если CI поддерживает `sonar.qualitygate.wait=true`.

Если провайдер поддерживает официальную интеграцию SonarQube Cloud, использовать её action/шаг. Конкретный синтаксис CI выбрать после определения провайдера репозитория; конфигурация анализа должна оставаться воспроизводимой и не зависеть от IDE.

## Шаг 4. Добавить pipeline

Pipeline должен запускаться:

- на каждый pull request в `master`;
- на push в `master` после слияния;
- по расписанию, например раз в неделю, чтобы получать обновления правил и уязвимостей.

Последовательность job:

1. checkout с полной историей Git, необходимой для сравнения нового кода;
2. проверка `build.sh`/`test.sh` и тестов;
3. установка или вызов зафиксированной версии SonarScanner;
4. запуск анализа с секретом;
5. публикация статуса и ссылки на дашборд;
6. остановка pipeline при провале quality gate для pull request.

## Шаг 5. Настроить качество

Начать с встроенного quality gate и зафиксировать baseline существующих проблем. Для новой разработки включить правила Clean as You Code:

- новые блокирующие ошибки и уязвимости отсутствуют;
- новые security hotspots просмотрены;
- покрытие и дублирование не ухудшаются относительно согласованного порога;
- quality gate обязателен для слияния pull request.

Старые замечания исправлять отдельными задачами, не маскировать массовыми исключениями. Каждое исключение должно содержать путь, причину и срок пересмотра.

## Шаг 6. Обеспечить корректный PR-анализ

1. Включить интеграцию репозитория с SonarQube Cloud.
2. Проверить, что CI передаёт идентификатор PR, исходную ветку и целевую ветку автоматически.
3. Для ручного запуска задать `sonar.pullrequest.key`, `sonar.pullrequest.branch`, `sonar.pullrequest.base`.
4. Открыть пробный pull request из ветки `testing` в `master`.
5. Убедиться, что анализ показывает новые проблемы именно этого PR, а статус quality gate виден рецензенту.

## Шаг 7. Проверить безопасность и сопровождение

- Токен хранится только в секрет-хранилище CI и ротируется при смене участников.
- В логах не печатаются значения секретов и командные строки с токеном.
- Версии scanner и Kotlin фиксируются и обновляются отдельным изменением.
- После обновления правил анализ запускается на тестовой ветке.
- В `CONTRIBUTING.md` или инструкции проекта описывается локальная команда проверки, если она будет добавлена.

## План внедрения по коммитам

1. `docs: define sonarcloud scope` — этот план и критерии.
2. `ci: add sonar scanner configuration` — `sonar-project.properties` и shell/CI-конфигурация.
3. `ci: enforce sonar quality gate` — статус PR и правило блокировки слияния.
4. `docs: record initial sonar baseline` — ссылка на проект, дату baseline и открытые задачи.

## Критерии приёмки

- Анализ `master` завершается успешно и отображается в SonarQube Cloud.
- Анализ тестового pull request автоматически запускается и показывает changed code.
- При искусственном нарушении quality gate pipeline получает failure.
- Секрет отсутствует в истории Git и логах.
- В репозитории нет Gradle/Maven, `.jar`, `.idea` и временных артефактов.

## Источники

- [SonarQube Cloud: основные принципы анализа](https://docs.sonarsource.com/sonarqube-cloud/discovering-sonarcloud/analysis-process-overview/basic-principles)
- [SonarQube Cloud: первый анализ](https://docs.sonarsource.com/sonarqube-cloud/getting-started/first-analysis)
- [SonarQube Cloud: параметры анализа](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/analysis-parameters)
- [SonarQube Cloud: анализ pull request](https://docs.sonarsource.com/sonarqube-cloud/analyzing-source-code/pull-request-analysis)
- [SonarScanner CLI](https://docs.sonarsource.com/sonarqube-cloud/advanced-setup/ci-based-analysis/sonarscanner-cli)
- [Robert C. Martin, The Principles of OOD](https://web.archive.org/web/20240101000000/https://butunclebob.com/ArticleS.UncleBob.PrinciplesOfOod)
