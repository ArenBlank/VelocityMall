$root = "D:\DevelopmentLOOK\Idea\idea_project_workspace\VelocityMall"
$logDir = Join-Path $root "logs\runtime"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null
$java = "D:\EnvironmentLOOK\jdk-17\bin\java.exe"
if (-not (Test-Path $java)) { $java = "java" }
$jar = Join-Path $root "velocity-mall-gateway\target\velocity-mall-gateway-1.0.0-SNAPSHOT.jar"

foreach ($port in @(8080, 8090, 8091)) {
  $out = Join-Path $logDir "velocity-mall-gateway-$port.out.log"
  $err = Join-Path $logDir "velocity-mall-gateway-$port.err.log"
  Start-Process -FilePath $java -ArgumentList @("-Dfile.encoding=UTF-8", "-jar", $jar, "--server.port=$port") -WorkingDirectory $root -WindowStyle Hidden -RedirectStandardOutput $out -RedirectStandardError $err
  Write-Host "Started Gateway :$port"
}
Write-Host "Gateway restarted"
