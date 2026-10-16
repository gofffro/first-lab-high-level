# План рефакторинга проекта

## Цель и ограничения

Цель — устранить выявленные нарушения SOLID и подготовить код к чистой архитектуре, сохранив поведение проверки доступа. В лабораторной №3 выполняется только планирование: Kotlin-код не изменяется. Системы сборки Gradle и Maven не вводятся; после рефакторинга остаются только существующие shell-скрипты и ручной вызов `kotlinc`, если он нужен проекту.

## Целевая структура изменений

```text
src/
  domain/
    model/                 # User, Action, AccessRequest, AccessCheckResult
    policy/                # PermissionPolicy, ResourcePathValidator
  application/
    port/in/               # CheckAccessUseCase
    port/out/              # UserRepository, ResourceRepository, PasswordHasher
    usecase/               # DefaultCheckAccessUseCase
  adapter/in/cli/          # CliParser, CliController, HelpPrinter
  adapter/out/memory/      # InMemoryUserRepository, InMemoryResourceRepository
  adapter/out/security/    # Sha256PasswordHasher
  config/                  # CompositionRoot, default data
```

## Этапы

### Этап 1. Зафиксировать поведение

1. Перенести текущие десять сценариев из `test.sh` в таблицу ожидаемого поведения.
2. Добавить тесты для валидатора пути, разбора действия, наследования разрешений и проверки объёма.
3. Зафиксировать текущие коды завершения и тексты справки.
4. Убедиться, что рефакторинг не меняет порядок приоритетов ошибок: логин → пароль → действие → путь/объём → ресурс → разрешение → лимит.

**Результат:** есть набор регрессионных проверок до изменения структуры.

### Этап 2. Выделить доменную модель

1. Оставить `User`, `Action` и итоговые статусы в `domain.model`.
2. Заменить строки `resource` и `volume` на проверяемые значения или value objects после согласования формата ошибок.
3. Вынести правило наследования разрешений из `hasPermission` в `PermissionPolicy`.
4. Вынести регулярное выражение и проверку объёма в `ResourcePathValidator` и `VolumeValidator`.

**Результат:** доменные правила не зависят от CLI, файлов и криптографической библиотеки.

### Этап 3. Ввести порты application-слоя

Создать минимальные интерфейсы:

```kotlin
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

Затем собрать `DefaultCheckAccessUseCase`, который принимает эти порты и политики через конструктор. Он не должен импортировать `kotlinx.cli`, `java.security.MessageDigest` или вызывать `exitProcess`.

### Этап 4. Разнести инфраструктуру

1. Перенести карты из `Data.kt` в `InMemoryUserRepository` и `InMemoryResourceRepository`.
2. Перенести SHA-256 в `Sha256PasswordHasher`; задать `Charsets.UTF_8` явно.
3. Создать `DefaultPermissionPolicy` с текущим правилом разрешений по префиксам пути.
4. Добавить `CompositionRoot`, который создаёт реализации портов и связывает их с use case.

### Этап 5. Разделить CLI-адаптер

1. `CliParser` преобразует аргументы в `CheckAccessCommand` или возвращает ошибку ввода.
2. `CliController` вызывает `CheckAccessUseCase`.
3. `HelpPrinter` отвечает только за справку.
4. `CliResultPresenter` печатает результат и преобразует его в код завершения.
5. `main` остаётся тонкой точкой входа и единственным местом вызова `exitProcess`.

### Этап 6. Открыть расширения без изменения use case

1. Заменить `parseAction` на реестр парсеров или фабрику `ActionParser`.
2. Заменить `AccessCheckResult.exitCode()` на CLI-политику кодов.
3. Проверить, что добавление действия, репозитория и интерфейса ввода не требует правок application-слоя.

### Этап 7. Удалить старые точки связи и проверить результат

1. Удалить глобальные карты, глобальное регулярное выражение и свободные функции, которые дублируют новые компоненты.
2. Запустить `.\test.sh`/`./test.sh` в поддерживаемой оболочке.
3. Отдельно проверить сборку ручным `.sh`-скриптом без Gradle/Maven.
4. Сравнить коды завершения и вывод справки с зафиксированными значениями.
5. Проверить анализ SonarCloud после подключения CI.

## Порядок коммитов

1. `test: lock current access-check behavior` — тесты и таблица сценариев.
2. `refactor: extract domain policies` — доменные политики и модели.
3. `refactor: add application ports and use case` — интерфейсы и orchestration.
4. `refactor: move storage and hashing to adapters` — инфраструктурные реализации.
5. `refactor: isolate cli adapter` — парсер, presenter и composition root.
6. `docs: add clean architecture and sonar plans` — отчёты лабораторной №3.

Каждый коммит должен собираться и проходить регрессионные проверки. В pull request включать только изменения отдельной ветки, не добавляя `.idea`, `.iml`, `.jar`, временные каталоги и файлы IDE.

## Риски и меры

| Риск | Мера |
|---|---|
| Изменится приоритет ошибок | Регрессионные тесты на каждый `AccessCheckResult` |
| Потеряется наследование разрешений | Отдельные тесты для каждого уровня пути |
| Отличится формат хеша | Тест с известной парой salt/password и явным UTF-8 |
| CLI начнёт возвращать другие коды | Контрактные тесты `CliResultPresenter` |
| Новые интерфейсы усложнят маленькое приложение | Оставлять только порты, которые нужны use case |

## Definition of Done

- Все перечисленные порты имеют одну ответственность.
- Application-слой не зависит от CLI, `MessageDigest` и конкретных карт.
- Регрессионные сценарии проходят без изменения пользовательского поведения.
- В репозитории нет систем сборки и сгенерированных артефактов.
- Ветка рефакторинга готова к отдельному pull request после проверки SonarCloud.
