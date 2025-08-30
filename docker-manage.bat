@echo off
setlocal

if "%1"=="start" goto :start
if "%1"=="stop" goto :stop
if "%1"=="restart" goto :restart
if "%1"=="logs" goto :logs
if "%1"=="build" goto :build
if "%1"=="clean" goto :clean
if "%1"=="status" goto :status
goto :help

:start
echo 🚀 Запуск банковского приложения...
docker-compose up -d
echo ✅ Приложение запущено!
echo 📋 Доступные сервисы:
echo    - API: http://localhost:8080/api
echo    - Swagger UI: http://localhost:8080/api/swagger-ui.html
echo    - Adminer (БД): http://localhost:8081
echo    - PostgreSQL: localhost:5432
goto :end

:stop
echo 🛑 Остановка приложения...
docker-compose down
echo ✅ Приложение остановлено!
goto :end

:restart
echo 🔄 Перезапуск приложения...
docker-compose down
docker-compose up -d
echo ✅ Приложение перезапущено!
goto :end

:logs
echo 📋 Логи приложения:
docker-compose logs -f bank-app
goto :end

:build
echo 🔨 Пересборка приложения...
docker-compose build --no-cache
echo ✅ Сборка завершена!
goto :end

:clean
echo 🧹 Очистка Docker ресурсов...
docker-compose down -v
docker system prune -f
echo ✅ Очистка завершена!
goto :end

:status
echo 📊 Статус сервисов:
docker-compose ps
goto :end

:help
echo 🏦 Управление банковским приложением
echo.
echo Использование: %0 {start^|stop^|restart^|logs^|build^|clean^|status}
echo.
echo Команды:
echo   start   - Запустить все сервисы
echo   stop    - Остановить все сервисы
echo   restart - Перезапустить все сервисы
echo   logs    - Показать логи приложения
echo   build   - Пересобрать образы
echo   clean   - Удалить все данные и очистить систему
echo   status  - Показать статус сервисов

:end
endlocal
