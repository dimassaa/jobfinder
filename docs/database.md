# Документация схемы базы данных

## Общее описание

Проект использует PostgreSQL 15+ для хранения данных. Схема создаётся автоматически при запуске контейнера PostgreSQL через скрипт `db/init.sql`.

## Таблицы

### 1. users — Пользователи

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| id | SERIAL | PRIMARY KEY | Уникальный идентификатор |
| email | VARCHAR(255) | NOT NULL, UNIQUE | Email пользователя |
| password_hash | VARCHAR(255) | NOT NULL | Хеш пароля (BCrypt) |
| role | VARCHAR(20) | NOT NULL, CHECK | Роль: USER, HR, ADMIN |
| name | VARCHAR(100) | | Имя |
| surname | VARCHAR(100) | | Фамилия |
| city | VARCHAR(100) | | Город |
| phone | VARCHAR(50) | | Телефон |
| additional_info | TEXT | | Дополнительная информация |
| is_blocked | BOOLEAN | DEFAULT FALSE | Заблокирован ли пользователь |
| blocked_until | TIMESTAMP | | До какого времени заблокирован |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата обновления |

**Индексы:**
- `idx_users_email` — поиск по email
- `idx_users_role` — фильтрация по роли
- `idx_users_is_blocked` — фильтрация по статусу блокировки

---

### 2. companies — Компании (для HR)

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| id | SERIAL | PRIMARY KEY | Уникальный идентификатор |
| user_id | INTEGER | NOT NULL, UNIQUE, REFERENCES users(id) | Связь с HR-пользователем |
| company_name | VARCHAR(255) | NOT NULL | Название компании |
| website | VARCHAR(500) | | Веб-сайт |
| description | TEXT | | Описание |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата обновления |

**Индексы:**
- `idx_companies_user_id` — поиск по пользователю

---

### 3. tags — Теги (навыки)

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| id | SERIAL | PRIMARY KEY | Уникальный идентификатор |
| name | VARCHAR(100) | NOT NULL, UNIQUE | Название навыка |
| is_active | BOOLEAN | DEFAULT TRUE | Активен ли тег |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |

**Индексы:**
- `idx_tags_name` — поиск по названию
- `idx_tags_is_active` — фильтрация активных тегов

---

### 4. user_tags — Связь пользователей и тегов

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| user_id | INTEGER | NOT NULL, REFERENCES users(id) | Пользователь |
| tag_id | INTEGER | NOT NULL, REFERENCES tags(id) | Тег |

** PRIMARY KEY (user_id, tag_id)

**Индексы:**
- `idx_user_tags_user_id`
- `idx_user_tags_tag_id`

---

### 5. vacancies — Вакансии

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| id | SERIAL | PRIMARY KEY | Уникальный идентификатор |
| company_id | INTEGER | NOT NULL, REFERENCES companies(id) | Компания |
| title | VARCHAR(500) | NOT NULL | Заголовок вакансии |
| description | TEXT | NOT NULL | Описание |
| city | VARCHAR(100) | | Город |
| salary_min | INTEGER | | Минимальная зарплата |
| salary_max | INTEGER | | Максимальная зарплата |
| employment_type | VARCHAR(50) | CHECK | FULL_TIME, PART_TIME, INTERNSHIP, REMOTE, CONTRACT |
| is_active | BOOLEAN | DEFAULT TRUE | Активна ли вакансия |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |
| closed_at | TIMESTAMP | | Дата закрытия |

**Индексы:**
- `idx_vacancies_company_id` — поиск по компании
- `idx_vacancies_created_at` — сортировка по дате
- `idx_vacancies_is_active` — фильтрация активных
- `idx_vacancies_city` — фильтрация по городу

---

### 6. vacancy_tags — Связь вакансий и тегов

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| vacancy_id | INTEGER | NOT NULL, REFERENCES vacancies(id) | Вакансия |
| tag_id | INTEGER | NOT NULL, REFERENCES tags(id) | Тег |

**PRIMARY KEY (vacancy_id, tag_id)**

---

### 7. applications — Отклики на вакансии

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| id | SERIAL | PRIMARY KEY | Уникальный идентификатор |
| vacancy_id | INTEGER | NOT NULL, REFERENCES vacancies(id) | Вакансия |
| user_id | INTEGER | NOT NULL, REFERENCES users(id) | Пользователь |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING', CHECK | PENDING, INVITED, ACCEPTED, REJECTED, WITHDRAWN |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |
| updated_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата обновления |

**UNIQUE (vacancy_id, user_id) — один отклик на вакансию от пользователя**

**Индексы:**
- `idx_applications_vacancy_id`
- `idx_applications_user_id`
- `idx_applications_status`
- `idx_applications_user_vacancy` — составной для проверки уникальности

---

### 8. favorites — Избранное

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| user_id | INTEGER | NOT NULL, REFERENCES users(id) | Пользователь |
| vacancy_id | INTEGER | NOT NULL, REFERENCES vacancies(id) | Вакансия |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата добавления |

**PRIMARY KEY (user_id, vacancy_id)**

---

### 9. notifications — Уведомления

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| id | SERIAL | PRIMARY KEY | Уникальный идентификатор |
| user_id | INTEGER | NOT NULL, REFERENCES users(id) | Пользователь |
| type | VARCHAR(50) | NOT NULL | Тип уведомления |
| title | VARCHAR(255) | NOT NULL | Заголовок |
| content | TEXT | | Текст |
| link | VARCHAR(500) | | Ссылка для перехода |
| is_read | BOOLEAN | DEFAULT FALSE | Прочитано ли |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |

**Индексы:**
- `idx_notifications_user_id`
- `idx_notifications_is_read`
- `idx_notifications_created_at`

---

### 10. complaints — Жалобы

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| id | SERIAL | PRIMARY KEY | Уникальный идентификатор |
| user_id | INTEGER | NOT NULL, REFERENCES users(id) | Кто пожаловался |
| target_type | VARCHAR(20) | NOT NULL, CHECK | VACANCY, USER, COMPANY |
| target_id | INTEGER | NOT NULL | ID объекта жалобы |
| reason | TEXT | NOT NULL | Причина |
| status | VARCHAR(20) | NOT NULL, DEFAULT 'PENDING', CHECK | PENDING, RESOLVED, REJECTED |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |
| resolved_by | INTEGER | REFERENCES users(id) | Кто рассмотрел |
| resolved_at | TIMESTAMP | | Дата решения |

**Индексы:**
- `idx_complaints_user_id`
- `idx_complaints_target` — составной (type, target_id)
- `idx_complaints_status`

---

### 11. password_reset_tokens — Токены сброса пароля

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| id | SERIAL | PRIMARY KEY | Уникальный идентификатор |
| user_id | INTEGER | NOT NULL, REFERENCES users(id) | Пользователь |
| token | VARCHAR(255) | NOT NULL, UNIQUE | Токен |
| expires_at | TIMESTAMP | NOT NULL | Срок действия |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата создания |

**Индексы:**
- `idx_password_reset_tokens_user_id`
- `idx_password_reset_tokens_token`
- `idx_password_reset_tokens_expires_at`

---

### 12. block_history — История блокировок

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| id | SERIAL | PRIMARY KEY | Уникальный идентификатор |
| user_id | INTEGER | NOT NULL, REFERENCES users(id) | Заблокированный пользователь |
| reason | TEXT | | Причина блокировки |
| blocked_until | TIMESTAMP | | До какого времени заблокирован |
| created_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата блокировки |

**Индексы:**
- `idx_block_history_user_id`
- `idx_block_history_created_at`

---

### 13. view_history — История просмотров вакансий

| Поле | Тип | Ограничения | Описание |
|------|-----|-------------|-----------|
| id | SERIAL | PRIMARY KEY | Уникальный идентификатор |
| user_id | INTEGER | NOT NULL, REFERENCES users(id) | Пользователь |
| vacancy_id | INTEGER | NOT NULL, REFERENCES vacancies(id) | Вакансия |
| viewed_at | TIMESTAMP | NOT NULL, DEFAULT NOW() | Дата просмотра |

**UNIQUE (user_id, vacancy_id) — один просмотр вакансии пользователем**

**Индексы:**
- `idx_view_history_user_id`
- `idx_view_history_vacancy_id`
- `idx_view_history_viewed_at`

---

## Связи между таблицами

```
users (1) ←→ (1) companies        -- один HR = одна компания
users (1) ←→ (N) user_tags      -- пользователь может иметь много тегов
tags (1)  ←→ (N) user_tags      -- тег может принадлежать многим пользователям
companies (1) ←→ (N) vacancies  -- компания может иметь много вакансий
vacancies (1) ←→ (N) vacancy_tags
tags (1)   ←→ (N) vacancy_tags
vacancies (1) ←→ (N) applications
users (1)  ←→ (N) applications
vacancies (1) ←→ (N) favorites
users (1)  ←→ (N) favorites
users (1)  ←→ (N) notifications
users (1)  ←→ (N) complaints
users (1)  ←→ (N) password_reset_tokens
users (1)  ←→ (N) block_history
users (1)  ←→ (N) view_history
vacancies (1) ←→ (N) view_history
```

## Миграции

Для управления схемой используется ручной подход с `init.sql`. При изменении структуры:
1. Создать новый файл миграции с порядковым номером
2. Применить ��иг��ацию вручную или через docker-compose

## Теги по умолчанию

При инициализации БД автоматически добавляются 40 тегов (языки программирования, БД, инструменты, методологии, soft skills).