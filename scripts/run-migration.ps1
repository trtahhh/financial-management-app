Write-Host "Running database migration and inserting test data..." -ForegroundColor Green

# Connect to SQL Server and run the migration script
$scriptPath = Join-Path $PSScriptRoot "..\database\migrations\insert_test_data.sql"
$scriptContent = Get-Content $scriptPath -Raw

try {
    # You can run this manually in SQL Server Management Studio or use sqlcmd
    Write-Host "Please run the following SQL script in SQL Server Management Studio:" -ForegroundColor Yellow
    Write-Host "File: $scriptPath" -ForegroundColor Cyan
    Write-Host ""
    Write-Host "Or use sqlcmd command:" -ForegroundColor Yellow
    Write-Host "sqlcmd -S localhost -d FinancialManagement -i `"$scriptPath`"" -ForegroundColor Cyan
    
    Write-Host ""
    Write-Host "Migration script will create:" -ForegroundColor Green
    Write-Host "- Admin user (id=1) with sample transactions" -ForegroundColor White
    Write-Host "- TestUser (id=4) with sample transactions" -ForegroundColor White
    Write-Host "- Categories, Wallets, Budgets, and Goals" -ForegroundColor White
    Write-Host "- Sample data for July 2025" -ForegroundColor White
} catch {
    Write-Host "Error: $($_.Exception.Message)" -ForegroundColor Red
}

Write-Host ""
Write-Host "Press any key to continue..."
$null = $Host.UI.RawUI.ReadKey("NoEcho,IncludeKeyDown")
