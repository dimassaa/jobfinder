# JobFinder - Приложение для учёта обращений в службу занятости

Приложение для поиска вакансий с модулем анализа. Многопоточное клиент-серверное приложение с базой данных PostgreSQL.

## Архитектура

- **Сервер**: Java 21, многопоточный TCP/IP сервер
- **Клиент**: JavaFX
- **База данных**: PostgreSQL 15
- **Взаимодействие**: TCP/IP сокеты, JSON (Jackson)
- **Сборка**: Maven (многомодульный проект)
- **Контейнеризация**: Docker, Docker Compose

## Быстрый старт

### Запуск через Docker Compose

```bash
# Сборка и запуск всех контейнеров
docker-compose up -d

# Просмотр логов
docker-compose logs -f

# Остановка
docker-compose down

# Полная очистка (включая данные БД)
docker-compose down -v
```

После запуска доступны:
- Сервер: http://localhost:8081
- PostgreSQL: localhost:5432

### Переменные окружения

| Переменная | Описание | Значение по умолчанию |
|------------|----------|----------------------|
| SERVER_PORT | Порт сервера | 8081 |
| DB_HOST | Хост базы данных | postgres |
| DB_PORT | Порт базы данных | 5432 |
| DB_NAME | Имя базы данных | jobdb |
| DB_USER | Пользователь БД | jobuser |
| DB_PASSWORD | Пароль БД | secret |

### Запуск без Docker

```bash
# Сборка
mvn clean package

# Запуск сервера
java -jar server/target/server-1.0-SNAPSHOT.jar
```

## Структура проекта

```
.
├── pom.xml                  # Родительский POM
├── docker-compose.yml       # Docker Compose конфигурация
├── db/
│   └── init.sql            # Схема базы данных
├── common/                  # Общий модуль (DTO, модели)
├── server/                  # Серверное приложение
│   └── Dockerfile          # Docker образ сервера
└── client/                  # Клиентское приложение (JavaFX)
```

## Роли пользователей

- **APPLICANT** - Соискатель (поиск вакансий, отклики, избранное)
- **HR** - Рекрутер (создание и управление вакансиями)
- **ADMIN** - Администратор (управление пользователями, вакансиями, тегами)

## Команды Docker

```bash
# Проверка статуса контейнеров
docker-compose ps

# Просмотр логов конкретного сервиса
docker-compose logs server
docker-compose logs postgres

# Пересборка образов
docker-compose build --no-cache

# Перезапуск сервиса
docker-compose restart server
```

## Healthcheck

Сервер поддерживает проверку здоровья через TCP-подключение к порту 8080.

## Тестирование

```bash
mvn test
```