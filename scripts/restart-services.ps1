$root = "D:\DevelopmentLOOK\Idea\idea_project_workspace\VelocityMall"
$logDir = Join-Path $root "logs\runtime"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

$java = "D:\EnvironmentLOOK\jdk-17\bin\java.exe"
if (-not (Test-Path $java)) { $java = "java" }

$services = @(
  @{ Module = "velocity-mall-product"; Port = 8081 },
  @{ Module = "velocity-mall-order"; Port = 8082 },
  @{ Module = "velocity-mall-seckill"; Port = 8083 },
  @{ Module = "velocity-mall-search"; Port = 8085 },
  @{ Module = "velocity-mall-coupon"; Port = 8086 },
  @{ Module = "velocity-mall-review"; Port = 8087 },
  @{ Module = "velocity-mall-user"; Port = 8088 },
  @{ Module = "velocity-mall-admin"; Port = 8089 },
  @{ Module = "velocity-mall-gateway"; Port = 8080 },
  @{ Module = "velocity-mall-gateway"; Port = 8090 },
  @{ Module = "velocity-mall-gateway"; Port = 8091 }
)

foreach ($svc in $services) {
  $module = $svc.Module
  $port = $svc.Port
  $jar = Join-Path $root "$module\target\$module-1.0.0-SNAPSHOT.jar"
  $out = Join-Path $logDir "$module-$port.out.log"
  $err = Join-Path $logDir "$module-$port.err.log"

  Start-Process -FilePath $java `
    -ArgumentList @("-Dfile.encoding=UTF-8", "-jar", $jar, "--server.port=$port") `
    -WorkingDirectory $root `
    -WindowStyle Hidden `
    -RedirectStandardOutput $out `
    -RedirectStandardError $err

  Write-Host "Started $module :$port"
}

Write-Host ""
Write-Host "All backend services started. Check: Get-Content $logDir\*.out.log -Wait -Tail 5"
