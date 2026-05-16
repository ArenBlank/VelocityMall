# VelocityMall 启动命令

本项目本地开发采用混合部署：中间件和 Nginx 使用 Docker，Java 微服务直接运行在宿主机 JDK 17。

## 一键启动后端

在项目根目录用 PowerShell 执行：

```powershell
$root = "D:\DevelopmentLOOK\Idea\idea_project_workspace\VelocityMall"
Set-Location $root

docker compose -f docker/docker-compose.yml up -d mysql redis nacos minio rmqnamesrv rmqbroker elasticsearch kibana nginx

mvn clean package -DskipTests

Get-CimInstance Win32_Process |
  Where-Object { $_.CommandLine -match 'velocity-mall-.*\.jar' } |
  ForEach-Object { Stop-Process -Id $_.ProcessId -Force }

$java = "D:\EnvironmentLOOK\jdk-17\bin\java.exe"
if (-not (Test-Path $java)) { $java = "java" }

$logDir = Join-Path $root "logs\runtime"
New-Item -ItemType Directory -Force -Path $logDir | Out-Null

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
}
```

后端统一入口：

```text
http://127.0.0.1
```

## 启动用户端前端

在项目根目录用 PowerShell 执行：

```powershell
Set-Location "D:\DevelopmentLOOK\Idea\idea_project_workspace\VelocityMall\velocity-mall-web"
npm install
npm run dev -- --host 0.0.0.0 --host 127.0.0.1 --port 5173
```

用户端访问地址：

```text
http://127.0.0.1:5173/login
```

测试账号：

```text
用户名：demo_buyer
密码：123456
```
