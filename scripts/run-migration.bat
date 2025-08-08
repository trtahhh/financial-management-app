@echo off
echo Running database migration and inserting test data...

REM Connect to SQL Server and run the migration script
sqlcmd -S localhost -d FinancialManagement -i database/migrations/insert_test_data.sql

echo Migration completed!
pause
