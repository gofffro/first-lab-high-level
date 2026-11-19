# Проверка доступа

Консольное приложение на Kotlin проверяет права на иерархические ресурсы. Пользователи, права и лимиты хранятся во встроенной H2. [Описание приложения и локальной БД](docs/README.md).

## Локальная сборка и тесты

Установите JDK 21 и запустите из корня проекта. Maven Wrapper скачает закреплённую версию Maven и зависимости при первой сборке. В PowerShell используйте `.\mvnw.cmd` вместо `./mvnw`.

```bash
./mvnw clean verify
bash scripts/build.sh
bash scripts/init-db.sh
java -jar app.jar --login alice --password qwerty --action read --resource A.B.C --volume 10
bash scripts/test.sh
```

`./mvnw test` запускает JUnit, включая тесты JDBC и Spring DI. `./mvnw package` создаёт исполняемый `target/app.jar`. Скрипт `build.sh` также копирует его в корень для Docker, а `test.sh` запускает JUnit и проверки CLI. В Windows запускайте Bash-скрипты из Git Bash.

### Spring Boot и логирование

`CommandLineRunner` передаёт аргументы CLI, а `ExitCodeGenerator` возвращает код процесса. Spring Boot запускается без веб-сервера и баннера. Контейнер внедряет зависимости сервисов и JDBC-репозиториев через конструкторы. Доменные правила остаются обычными Kotlin-классами.

SLF4J и Logback записывают результат CLI и ошибки БД в `logs/access.log`. Можно указать другой путь через `APP_LOG_FILE`. Для файла H2 используется `APP_DB_PATH`, по умолчанию `data/access` без расширения `.mv.db`. В консоль приложение выводит только справку. Коды доступа и БД сохранены. Код 127 означает ошибку синтаксиса CLI, код 11 — сбой запуска Spring.

Для запуска из Maven после инициализации БД:

```bash
./mvnw spring-boot:run '-Dspring-boot.run.arguments=--login alice --password qwerty --action read --resource A.B.C --volume 10'
```

Для получения исходного кода завершения CLI используйте `java -jar`. Maven сообщает о ненулевом коде дочернего процесса как об ошибке своей команды.

## Docker

```bash
bash scripts/build.sh
bash scripts/build-docker.sh first-lab-high-level:local
bash scripts/test-docker.sh first-lab-high-level:local
docker run --rm first-lab-high-level:local --login alice --password qwerty --action read --resource A.B.C --volume 10
echo $?
```

Образ содержит Java, fat JAR и H2 с начальными учебными данными. Ожидаемые коды в контейнере — 0 для успешного запроса, 5 для отказа и 1 для справки. Тест проверяет и текст справки.

Публичный репозиторий образов — [`gofffro/first-lab-high-level`](https://hub.docker.com/r/gofffro/first-lab-high-level).

## CI и CD

- `ci.yml` запускает сборку Maven, CodeQL, JUnit, проверку CLI и тест контейнера при открытии и обновлении PR в `main`.
- `cd.yml` запускается при push в `main`. Его также можно запустить вручную из `main`.
- Оба используют одну схему проверок. CodeQL анализирует Kotlin во время сборки и блокирует выпуск при сбое анализа или находках уровня `error`. Отчёт SARIF и результаты JUnit доступны в артефактах запуска.
- CD проверяет SHA коммита и контрольные суммы полученных файлов. Затем он собирает образ из проверенного JAR и запускает три сценария в контейнерах. После успешных проверок этот же образ публикуется с тегом `sha-<SHA>`. Публичный GitHub Release `build-<SHA>` содержит ссылку на образ, тот же fat JAR, SQL, SHA коммита и контрольные суммы.
- Для Telegram используются `TELEGRAM_BOT_TOKEN` и `TELEGRAM_CHAT_ID`. CI отправляет результаты PR из этого репозитория. CD отправляет результаты проверок и публикации. Docker Hub использует `DOCKERHUB_USERNAME` и `DOCKERHUB_TOKEN`. Все четыре значения задаются в GitHub Repository Secrets. CodeQL использует встроенный токен Actions.

Из [релиза](https://github.com/gofffro/first-lab-high-level/releases) можно взять `app.jar`, `init.sql` и `fill.sql`. Из каталога скачанных файлов инициализируйте H2 встроенной утилитой, затем запустите JAR.

```bash
java -Dloader.main=org.h2.tools.RunScript -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher -url jdbc:h2:./data/access -user sa -script init.sql
java -Dloader.main=org.h2.tools.RunScript -cp app.jar org.springframework.boot.loader.launch.PropertiesLauncher -url jdbc:h2:./data/access -user sa -script fill.sql
java -jar app.jar --login alice --password qwerty --action read --resource A.B.C --volume 10
```
