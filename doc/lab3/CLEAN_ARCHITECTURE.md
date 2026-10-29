# План перехода к чистой архитектуре

Исторический план лабораторной №3; рефакторинг выполнен в №4. Актуальные команды и структура — в [README](../../docs/README.md), результаты тестирования — в [testable.md](../lab5/testable.md).

## Назначение приложения

Приложение принимает запрос на доступ к иерархическому ресурсу и возвращает результат проверки. Ключевое правило архитектуры: зависимости направлены с внешних слоёв внутрь, а доменный код не знает о CLI, способе хранения данных, криптографии и конкретной библиотеке аргументов.

## Слои и состав

### 1. Domain — предметная область

**Состав текущей версии:**

- `src/Models.kt`: `User`, `Action`, `AccessRequest`, `AccessCheckResult`.
- правила из `src/AccessChecker.kt`: проверка разрешения по сегментам пути, допустимый объём, семантика статусов.

**Целевые компоненты:**

- `User`, `Action`, `ResourcePath`, `RequestedVolume`;
- `PermissionPolicy`;
- `ResourcePathValidator`;
- доменные ошибки и результаты проверки.

Слой не импортирует `kotlinx.cli`, `java.security`, `System`, `exitProcess` и классы хранилища.

### 2. Application — сценарии использования

**Центральный сценарий:** `CheckAccessUseCase`.

Он принимает типизированную команду, последовательно вызывает аутентификацию, валидацию, поиск ресурса и авторизацию, затем возвращает `AccessCheckResult`. Слой объявляет порты:

- `UserRepository`;
- `ResourceRepository`;
- `PasswordHasher`;
- при необходимости `PermissionPolicy` и `Clock`.

Application не знает, находятся ли данные в памяти, файле или базе данных.

### 3. Interface adapters — адаптеры ввода и вывода

**Ввод:** `src/Main.kt` и часть `src/Help.kt` после разделения — CLI parser/controller, преобразующий флаги в `CheckAccessCommand`.

**Вывод:** presenter результата, который печатает справку/сообщение и преобразует доменный статус в код процесса.

Адаптеры переводят внешние строки и коды в типы application-слоя. Домен не печатает и не завершает процесс.

### 4. Infrastructure — внешние детали

**Компоненты:**

- `src/Data.kt` → in-memory реализации `UserRepository` и `ResourceRepository`;
- `hashPassword` → `Sha256PasswordHasher` с явным UTF-8;
- composition root, который создаёт зависимости;
- shell-скрипты сборки/запуска/тестирования;
- конкретная библиотека `kotlinx-cli`.

Этот слой может быть заменён без изменения правил доступа.

## Предлагаемая структура пакетов

```text
src/
  domain/model/
  domain/policy/
  application/port/in/
  application/port/out/
  application/usecase/
  adapter/in/cli/
  adapter/out/memory/
  adapter/out/security/
  config/
```

Названия пакетов можно адаптировать под ограничения ручного `kotlinc`; важна граница зависимостей, а не количество каталогов.

## Направление зависимостей

```text
CLI / shell ──▶ adapter.in.cli ──▶ application.usecase ──▶ domain
                         │                  │
                         └──────────────▶ application.port.out
                                            ▲
              infrastructure adapters ─────┘
```

Интерфейсы портов принадлежат application-слою, а реализации находятся снаружи. Composition root связывает их при запуске.

## Контракты портов

```kotlin
interface CheckAccessUseCase {
    fun execute(command: CheckAccessCommand): AccessCheckResult
}

interface UserRepository {
    fun findByLogin(login: String): User?
}

interface ResourceRepository {
    fun findMaximumVolume(path: String): Int?
}

interface PasswordHasher {
    fun hash(password: String, salt: String): String
}
```

Порты должны быть небольшими и выражать намерение. `Map<String, User>` и `Map<String, Int>` не передаются в use case: это детали конкретного хранилища.

## План реструктуризации

1. Зафиксировать текущие сценарии и перенести модели в `domain.model`.
2. Вынести `hasPermission` и проверки пути/объёма в доменные политики.
3. Объявить входной порт `CheckAccessUseCase` и выходные порты репозиториев/хешировщика.
4. Переписать `checkAccess` как application use case с внедрением зависимостей через конструктор.
5. Разделить `Main.kt` на CLI parser, controller, presenter и composition root.
6. Перенести `Data.kt` и `hashPassword` в инфраструктурные адаптеры.
7. Проверить направление импортов и запретить зависимости domain от внешних пакетов.
8. Выполнить регрессионные тесты и ручной запуск shell-скриптами.

## Тестируемость

- Domain-тесты проверяют наследование разрешений, путь, действие и лимит без файловой системы и CLI.
- Application-тесты используют fake repositories и fake hasher.
- Adapter-тесты проверяют разбор аргументов, справку и коды завершения.
- Интеграционный тест связывает in-memory адаптеры с CLI на тех же десяти сценариях.

## Критерии готовности

- При замене in-memory репозитория на файловый application-код не меняется.
- При замене CLI на HTTP/GUI domain и use case не меняются.
- Все зависимости внешних библиотек находятся в adapter/infrastructure слоях.
- Текущий пользовательский контракт и коды завершения сохранены.
- Проект не получает Gradle/Maven и не добавляет сгенерированные файлы в репозиторий.
