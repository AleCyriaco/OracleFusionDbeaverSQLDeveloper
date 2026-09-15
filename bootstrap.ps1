<#
  Fusion Query JDBC - bootstrap (Windows)

  Instala o driver Oracle Fusion Cloud (BIP) para Oracle SQL Developer ou
  baixa o driver JDBC para DBeaver / DataGrip / outro cliente JDBC.

  Uso (PowerShell):

    & ([scriptblock]::Create((irm https://raw.githubusercontent.com/AleCyriaco/OracleFusionDbeaverSQLDeveloper/main/bootstrap.ps1)))

  Parametros opcionais:

    -Client sqldeveloper | dbeaver     pula o menu de escolha
    -SqlDevDir 'C:\Oracle\sqldeveloper'  pasta de instalacao do SQL Developer
#>
param(
    [ValidateSet('sqldeveloper','dbeaver')]
    [string]$Client,
    [string]$SqlDevDir
)

$ErrorActionPreference = 'Stop'
try {
    [Net.ServicePointManager]::SecurityProtocol = `
        [Net.ServicePointManager]::SecurityProtocol -bor [Net.SecurityProtocolType]::Tls12
} catch {}

$RawBase = 'https://raw.githubusercontent.com/AleCyriaco/OracleFusionDbeaverSQLDeveloper/main/dist'
$Version = '1.0.0'

function Write-Step([string]$msg) { Write-Host "==> $msg" -ForegroundColor Cyan }
function Write-Ok([string]$msg)   { Write-Host $msg -ForegroundColor Green }

function Get-DistFile([string]$name, [string]$destDir) {
    if (-not (Test-Path $destDir)) { New-Item -ItemType Directory -Path $destDir -Force | Out-Null }
    $dest = Join-Path $destDir $name
    Write-Step "Baixando $name"
    Invoke-WebRequest -Uri "$RawBase/$name" -OutFile $dest -UseBasicParsing
    return $dest
}

function Find-SqlDevDir {
    $candidates = @(
        'C:\Oracle\sqldeveloper',
        'C:\sqldeveloper',
        "$env:ProgramFiles\sqldeveloper",
        "${env:ProgramFiles(x86)}\sqldeveloper",
        "$env:USERPROFILE\sqldeveloper",
        "$env:USERPROFILE\Desktop\sqldeveloper",
        'D:\sqldeveloper'
    )
    foreach ($c in $candidates) {
        if ($c -and (Test-Path (Join-Path $c 'sqldeveloper\bin\sqldeveloper.conf'))) { return $c }
    }
    return $null
}

function Test-SqlDevInstallDir([string]$dir) {
    return ($dir -and (Test-Path (Join-Path $dir 'sqldeveloper\bin\sqldeveloper.conf')))
}

function Find-Java([string]$sqlDevDir) {
    if ($sqlDevDir) {
        $bundled = Join-Path $sqlDevDir 'jdk\bin\java.exe'
        if (Test-Path $bundled) { return $bundled }
    }
    $cmd = Get-Command java -ErrorAction SilentlyContinue
    if ($cmd) { return $cmd.Source }
    return $null
}

Write-Host ''
Write-Ok "Fusion Query JDBC $Version - instalador para Windows"
Write-Host ''

if (-not $Client) {
    Write-Host 'Instalar para qual cliente?'
    Write-Host '  [1] Oracle SQL Developer  (tipo de conexao nativo "Oracle Fusion Cloud (BIP)")'
    Write-Host '  [2] DBeaver / DataGrip / outro cliente JDBC  (somente o driver .jar)'
    do { $choice = Read-Host 'Escolha 1 ou 2' } until ($choice -eq '1' -or $choice -eq '2')
    if ($choice -eq '1') { $Client = 'sqldeveloper' } else { $Client = 'dbeaver' }
    Write-Host ''
}

if ($Client -eq 'sqldeveloper') {

    if (-not (Test-SqlDevInstallDir $SqlDevDir)) {
        $SqlDevDir = Find-SqlDevDir
        if ($SqlDevDir) {
            Write-Step "SQL Developer encontrado em: $SqlDevDir"
        }
    }
    while (-not (Test-SqlDevInstallDir $SqlDevDir)) {
        Write-Host 'Nao encontrei a pasta de instalacao do SQL Developer automaticamente.'
        $SqlDevDir = Read-Host 'Informe a pasta que contem sqldeveloper.exe (ex.: C:\Oracle\sqldeveloper)'
        if (-not (Test-SqlDevInstallDir $SqlDevDir)) {
            Write-Host "  '$SqlDevDir' nao parece uma instalacao do SQL Developer (falta sqldeveloper\bin\sqldeveloper.conf)." -ForegroundColor Yellow
        }
    }

    $running = Get-Process -Name 'sqldeveloper*' -ErrorAction SilentlyContinue
    if ($running) {
        Write-Host 'ATENCAO: o SQL Developer parece estar aberto. Feche-o (File > Exit) antes de continuar.' -ForegroundColor Yellow
        Read-Host 'Pressione Enter quando tiver fechado'
    }

    $java = Find-Java $SqlDevDir
    if (-not $java) {
        throw 'Nenhum Java encontrado (nem o jdk embutido do SQL Developer, nem java no PATH). Instale um JDK e rode de novo.'
    }
    Write-Step "Usando Java: $java"

    $work = Join-Path $env:TEMP 'fusion-sqldev-bootstrap'
    $jar  = Get-DistFile "fusion-sqldev-installer-$Version.jar" $work

    Write-Step 'Executando o instalador'
    & $java -jar $jar --cli --installdir $SqlDevDir
    if ($LASTEXITCODE -ne 0) {
        throw "O instalador terminou com erro (codigo $LASTEXITCODE). Veja as mensagens acima."
    }

    Write-Host ''
    Write-Ok 'Instalacao concluida.'
    Write-Host ''
    Write-Host 'Proximos passos:'
    Write-Host '  1. Abra o SQL Developer e DEIXE o primeiro start terminar (e lento; se aparecer'
    Write-Host '     o dialogo de importar preferencias, responda nele - nao feche no X).'
    Write-Host '  2. New Connection > Database Type > "Oracle Fusion Cloud (BIP)".'
    Write-Host '  3. (Opcional) Para registrar tambem o driver na aba JDBC generica: feche o'
    Write-Host '     SQL Developer e rode este script mais uma vez.'

} else {

    $target = Join-Path $env:USERPROFILE "Oracle\fusion-query-jdbc-$Version"
    $jar = Get-DistFile "fusion-query-jdbc-$Version.jar" $target

    Write-Host ''
    Write-Ok "Driver baixado em: $jar"
    Write-Host ''
    Write-Host 'Configuracao no DBeaver:'
    Write-Host '  1. Database > Driver Manager > New'
    Write-Host '     Driver Name : Oracle Fusion Cloud (BIP)'
    Write-Host '     Class Name  : com.fusionquery.jdbc.FusionDriver'
    Write-Host '     URL Template: jdbc:fusion://{host}'
    Write-Host "  2. Libraries > Add File > $jar"
    Write-Host '  3. Em Driver Settings, mantenha "Use legacy JDBC instantiation" marcado.'
    Write-Host '  4. Nova conexao: host do seu Fusion (ex.: xxxx.fa.us2.oraclecloud.com),'
    Write-Host '     usuario e senha do Fusion; deixe reportPath vazio. Clique Test Connection.'
    Write-Host ''
    Write-Host 'DataGrip / IntelliJ: crie um driver customizado apontando para o mesmo JAR,'
    Write-Host 'classe com.fusionquery.jdbc.FusionDriver, URL jdbc:fusion://{host}.'
}
