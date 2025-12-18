#!/bin/bash

#===============================================================================
# Система управления сотрудниками ИБ
# Скрипт запуска (macOS / Linux)
#===============================================================================

set -e  # Остановка при ошибке

# Цвета для вывода
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Функции вывода
print_header() {
    echo ""
    echo -e "${BLUE}==========================================${NC}"
    echo -e "${BLUE}  Система управления сотрудниками ИБ${NC}"
    echo -e "${BLUE}==========================================${NC}"
    echo ""
}

print_success() {
    echo -e "${GREEN}$1${NC}"
}

print_error() {
    echo -e "${RED}$1${NC}"
}

print_warning() {
    echo -e "${YELLOW}$1${NC}"
}

print_info() {
    echo -e "${BLUE}$1${NC}"
}

# Определение ОС
detect_os() {
    if [[ "$OSTYPE" == "darwin"* ]]; then
        OS="macos"
        print_info "Обнаружена macOS"
    elif [[ "$OSTYPE" == "linux-gnu"* ]]; then
        OS="linux"
        print_info "Обнаружен Linux"
    else
        print_error "Неподдерживаемая ОС: $OSTYPE"
        exit 1
    fi
}

# Проверка Java
check_java() {
    if ! command -v java &> /dev/null; then
        print_error "Java не установлена!"
        echo ""
        echo "Установите Java 17+:"
        if [[ "$OS" == "macos" ]]; then
            echo "  brew install openjdk@17"
        else
            echo "  sudo apt update && sudo apt install openjdk-17-jdk"
        fi
        exit 1
    fi

    JAVA_VERSION=$(java -version 2>&1 | head -n 1 | sed 's/.*"\([0-9]*\).*/\1/')
    if [[ "$JAVA_VERSION" =~ ^[0-9]+$ ]] && [[ "$JAVA_VERSION" -ge 17 ]]; then
        print_success "Java версия: $JAVA_VERSION"
    else
        print_success "Java установлена"
    fi
}

# Проверка Maven
check_maven() {
    if ! command -v mvn &> /dev/null; then
        print_error "Maven не установлен!"
        echo ""
        echo "Установите Maven:"
        if [[ "$OS" == "macos" ]]; then
            echo "  brew install maven"
        else
            echo "  sudo apt update && sudo apt install maven"
        fi
        exit 1
    fi
    print_success "Maven установлен"
}

# Проверка Docker
check_docker() {
    if ! command -v docker &> /dev/null; then
        print_error "Docker не установлен!"
        echo ""
        echo "Установите Docker:"
        if [[ "$OS" == "macos" ]]; then
            echo "  brew install --cask docker"
            echo "  или скачайте с: https://docker.com/products/docker-desktop"
        else
            echo "  curl -fsSL https://get.docker.com | sh"
            echo "  sudo usermod -aG docker \$USER"
            echo "  newgrp docker"
        fi
        exit 1
    fi
    print_success "Docker установлен"

    # Проверка прав Docker (только для Linux)
    if [[ "$OS" == "linux" ]]; then
        if ! docker ps &> /dev/null; then
            print_warning "Нет прав для Docker"
            echo ""
            echo "Выполните:"
            echo "  sudo usermod -aG docker \$USER"
            echo "  newgrp docker"
            echo ""
            echo "Или запустите скрипт с sudo: sudo ./run.sh"
            exit 1
        fi
    fi
    
    # Проверка, что Docker daemon запущен
    if ! docker info &> /dev/null; then
        print_error "Docker daemon не запущен!"
        if [[ "$OS" == "macos" ]]; then
            echo "Запустите Docker Desktop"
        else
            echo "Выполните: sudo systemctl start docker"
        fi
        exit 1
    fi
}

# Запуск PostgreSQL
start_postgres() {
    echo ""
    print_info "Запуск базы данных PostgreSQL..."

    # Получаем путь к скрипту
    SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"

    # Проверяем, существует ли контейнер
    if docker ps -a --format '{{.Names}}' | grep -q "^ib-postgres$"; then
        # Контейнер существует
        if docker ps --format '{{.Names}}' | grep -q "^ib-postgres$"; then
            print_success "PostgreSQL уже запущен"
        else
            print_info "Запуск существующего контейнера..."
            docker start ib-postgres > /dev/null 2>&1
            print_success "PostgreSQL запущен"
        fi
    else
        # Создаём новый контейнер
        print_info "Создание нового контейнера PostgreSQL..."
        docker run -d \
            --name ib-postgres \
            -e POSTGRES_DB=ib_employees \
            -e POSTGRES_USER=ib_admin \
            -e POSTGRES_PASSWORD=ib_secure_pass_2024 \
            -p 5432:5432 \
            -v ib_postgres_data:/var/lib/postgresql/data \
            postgres:16-alpine > /dev/null 2>&1
        
        if [[ $? -eq 0 ]]; then
            print_success "PostgreSQL контейнер создан"
        else
            print_error "Ошибка создания контейнера PostgreSQL"
            echo "Проверьте, свободен ли порт 5432"
            exit 1
        fi
    fi

    # Ожидание готовности БД
    print_info "Ожидание готовности базы данных..."
    for i in {1..30}; do
        if docker exec ib-postgres pg_isready -U ib_admin -d ib_employees > /dev/null 2>&1; then
            print_success "PostgreSQL готов к работе"
            break
        fi
        sleep 1
        if [[ $i -eq 30 ]]; then
            print_error "PostgreSQL не отвечает"
            echo "Проверьте логи: docker logs ib-postgres"
            exit 1
        fi
    done

    # Инициализация базы данных
    print_info "Инициализация структуры базы данных..."
    
    if [[ -f "$SCRIPT_DIR/sql/init.sql" ]]; then
        docker cp "$SCRIPT_DIR/sql/init.sql" ib-postgres:/tmp/init.sql
        if docker exec ib-postgres psql -U ib_admin -d ib_employees -f /tmp/init.sql 2>&1 | grep -i error; then
            print_warning "Возможны ошибки при инициализации (дубликаты игнорируются)"
        fi
        print_success "База данных инициализирована"
    else
        print_warning "Файл sql/init.sql не найден"
    fi
}

# Сборка и запуск приложения
build_and_run() {
    echo ""
    print_info "Сборка приложения..."
    
    # Получаем путь к скрипту
    SCRIPT_DIR="$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd )"
    cd "$SCRIPT_DIR"
    
    mvn clean compile -q
    
    if [[ $? -eq 0 ]]; then
        print_success "Сборка завершена"
    else
        print_error "Ошибка сборки"
        exit 1
    fi

    echo ""
    print_info "Запуск приложения..."
    echo ""
    echo -e "${GREEN}═══════════════════════════════════════════${NC}"
    echo -e "${GREEN}  Приложение запущено!${NC}"
    echo -e "${GREEN}  Учётные записи:${NC}"
    echo -e "${GREEN}       admin / admin123${NC}"
    echo -e "${GREEN}       user / user123${NC}"
    echo -e "${GREEN}═══════════════════════════════════════════${NC}"
    echo ""
    
    mvn javafx:run
}

# Остановка контейнера (опционально)
stop_postgres() {
    print_info "Остановка PostgreSQL..."
    docker stop ib-postgres > /dev/null 2>&1 || true
    print_success "PostgreSQL остановлен"
}

# Справка
show_help() {
    echo "Использование: ./run.sh [КОМАНДА]"
    echo ""
    echo "Команды:"
    echo "  (без аргументов)  Запуск приложения"
    echo "  stop              Остановить PostgreSQL"
    echo "  clean             Удалить контейнер и данные"
    echo "  help              Показать справку"
    echo ""
}

# Очистка
clean() {
    print_warning "Удаление контейнера и данных..."
    docker stop ib-postgres 2>/dev/null || true
    docker rm ib-postgres 2>/dev/null || true
    docker volume rm ib_postgres_data 2>/dev/null || true
    print_success "Очистка завершена"
}

# Главная функция
main() {
    print_header
    
    case "${1:-}" in
        stop)
            stop_postgres
            ;;
        clean)
            clean
            ;;
        help|--help|-h)
            show_help
            ;;
        *)
            detect_os
            check_java
            check_maven
            check_docker
            start_postgres
            build_and_run
            ;;
    esac
}

# Запуск
main "$@"