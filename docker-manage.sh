#!/bin/bash

# Скрипт для управления Docker окружением банковского приложения

case "$1" in
    "start")
        echo "🚀 Запуск банковского приложения..."
        docker-compose up -d
        echo "✅ Приложение запущено!"
        echo "📋 Доступные сервисы:"
        echo "   - API: http://localhost:8080/api"
        echo "   - Swagger UI: http://localhost:8080/api/swagger-ui.html"
        echo "   - Adminer (БД): http://localhost:8081"
        echo "   - PostgreSQL: localhost:5432"
        ;;
    "stop")
        echo "🛑 Остановка приложения..."
        docker-compose down
        echo "✅ Приложение остановлено!"
        ;;
    "restart")
        echo "🔄 Перезапуск приложения..."
        docker-compose down
        docker-compose up -d
        echo "✅ Приложение перезапущено!"
        ;;
    "logs")
        echo "📋 Логи приложения:"
        docker-compose logs -f bank-app
        ;;
    "build")
        echo "🔨 Пересборка приложения..."
        docker-compose build --no-cache
        echo "✅ Сборка завершена!"
        ;;
    "clean")
        echo "🧹 Очистка Docker ресурсов..."
        docker-compose down -v
        docker system prune -f
        echo "✅ Очистка завершена!"
        ;;
    "status")
        echo "📊 Статус сервисов:"
        docker-compose ps
        ;;
    *)
        echo "🏦 Управление банковским приложением"
        echo ""
        echo "Использование: $0 {start|stop|restart|logs|build|clean|status}"
        echo ""
        echo "Команды:"
        echo "  start   - Запустить все сервисы"
        echo "  stop    - Остановить все сервисы"
        echo "  restart - Перезапустить все сервисы"
        echo "  logs    - Показать логи приложения"
        echo "  build   - Пересобрать образы"
        echo "  clean   - Удалить все данные и очистить систему"
        echo "  status  - Показать статус сервисов"
        exit 1
        ;;
esac
