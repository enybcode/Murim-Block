param(
    [Parameter(ValueFromRemainingArguments = $true)]
    [string[]] $Args
)

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$Tool = Join-Path $ScriptDir "murim_tool.py"
$BundledPython = "C:\Users\enzob\.cache\codex-runtimes\codex-primary-runtime\dependencies\python\python.exe"

if (Get-Command py -ErrorAction SilentlyContinue) {
    & py $Tool @Args
    exit $LASTEXITCODE
}

if (Test-Path -LiteralPath $BundledPython) {
    & $BundledPython $Tool @Args
    exit $LASTEXITCODE
}

if (Get-Command python -ErrorAction SilentlyContinue) {
    $PythonCommand = Get-Command python
    if ($PythonCommand.Source -notlike "*WindowsApps*") {
        & python $Tool @Args
        exit $LASTEXITCODE
    }
}

Write-Error "Python is required to run Murim tools."
exit 1
