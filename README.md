# Лабораторная работа №1

Авторы: Важенин С.С ПИ-241
        Фенько А.Н. ПИ-241

Консольное приложение проверяет доступ пользователя к иерархическим ресурсам. Поддерживаются действия `read`, `write` и `execute`.

Тестовые пользователи:

- `alice` / `qwerty`
- `bob` / `secret`

Пароли не хранятся в открытом виде: приложение сравнивает SHA-256-хэш пароля с солью.

## Сборка

В Git Bash или WSL из корня проекта:

```bash
chmod +x build.sh run.sh test.sh
./build.sh
```

Скрипт использует `kotlinc` из `PATH`, собирает `app.jar` и включает в него библиотеку `kotlinx-cli`.

## Запуск

```bash
./run.sh --login alice --password qwerty --action read --resource A.B.C --volume 10
```

Либо после сборки:

```bash
java -jar app.jar --login alice --password qwerty --action read --resource A.B.C --volume 10
```

Справка:

```bash
java -jar app.jar --help
```

## Тестирование

```bash
./test.sh
```

Скрипт проверяет десять сценариев и выводит результат в формате `OK/FAIL` и итог `10/10`.

Результат проверки:
<img width="867" height="288" alt="image" src="https://github.com/user-attachments/assets/9c7356d4-a364-44dc-8464-42a72373ec13" />

