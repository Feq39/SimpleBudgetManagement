param(
    [Parameter(Mandatory=$true)]
    [string]$Port
)

.\mvnw clean package

if ($LASTEXITCODE -ne 0) {
    Write-Host "Maven build failed"
    exit $LASTEXITCODE
}

$env:APP_PORT = $Port
docker compose up --build