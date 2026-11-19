# Проверка доступа

Консольное приложение на Kotlin проверяет права на иерархические ресурсы. Пользователи, права и лимиты хранятся во встроенной H2. [Описание приложения и локальной БД](docs/README.md).

## Локальная сборка и тесты

Из корня проекта в Bash с JDK 21 и Kotlin CLI 2.4.20 в `PATH`.

```bash
bash scripts/setup-libs.sh
bash scripts/build.sh
bash scripts/test-unit.sh
bash scripts/test.sh
bash scripts/init-db.sh
java -jar app.jar --login alice --password qwerty --action read --resource A.B.C --volume 10
```

`test-unit.sh` запускает JUnit, включая тесты JDBC. `test.sh` запускает JUnit и отдельно проверяет приложение через CLI. В Windows используйте Git Bash. JAR собирается без Maven и Gradle.

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

- `ci.yml` запускает сборку, CodeQL, JUnit и проверку CLI при открытии и обновлении PR в `main`.
- `cd.yml` запускается при push в `main`. Его также можно запустить вручную из `main`.
- Оба используют одну схему проверок. CodeQL анализирует Kotlin во время сборки и блокирует выпуск при сбое анализа или находках уровня `error`. Отчёт SARIF и результаты JUnit доступны в артефактах запуска.
- CD проверяет SHA коммита и контрольные суммы полученных файлов. Затем он собирает образ из проверенного JAR и запускает три сценария в контейнерах. После успешных проверок этот же образ публикуется с тегом `sha-<SHA>`. Публичный GitHub Release `build-<SHA>` содержит ссылку на образ, тот же fat JAR, SQL, SHA коммита и контрольные суммы.
- Для Telegram используются `TELEGRAM_BOT_TOKEN` и `TELEGRAM_CHAT_ID`. CI отправляет результаты PR из этого репозитория. CD отправляет результаты проверок и публикации. Docker Hub использует `DOCKERHUB_USERNAME` и `DOCKERHUB_TOKEN`. Все четыре значения задаются в GitHub Repository Secrets. CodeQL использует встроенный токен Actions.

Из [релиза](https://github.com/gofffro/first-lab-high-level/releases) можно взять `app.jar`, `init.sql` и `fill.sql`. Из каталога скачанных файлов инициализируйте H2 встроенной утилитой, затем запустите JAR.

```bash
java -cp app.jar org.h2.tools.RunScript -url jdbc:h2:./data/access -user sa -script init.sql
java -cp app.jar org.h2.tools.RunScript -url jdbc:h2:./data/access -user sa -script fill.sql
java -jar app.jar --login alice --password qwerty --action read --resource A.B.C --volume 10
```
