. "$PSScriptRoot\_common.ps1"

Assert-File $VenvPython "Python environment is missing. Run .\scripts\dev\setup.ps1 first."
Set-Location $ProjectRoot
& $VenvPython -m uvicorn api.main:app --host 0.0.0.0 --port 8000 --reload
