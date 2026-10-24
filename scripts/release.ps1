#Requires -Version 7.0
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $root
function Require-Success([string]$message) { if ($LASTEXITCODE -ne 0) { throw $message } }
try {
    $branch = (& git branch --show-current).Trim()
    Require-Success '无法读取 Git 分支。'
    if ($branch -ne 'main') { throw '发布必须在 main 分支执行。' }
    $status = & git status --porcelain
    Require-Success '无法检查 Git 工作区。'
    if ($status) { throw '工作区不干净。请先检查并提交修改，再运行发布脚本。' }
    if (-not (Test-Path -LiteralPath (Join-Path $root 'keystore.properties'))) { throw '缺少 keystore.properties。正式签名密钥必须由人类生成和保管，请按 docs/ANDROID_RELEASE.md 配置。禁止发布 debug 签名包。' }
    $build = Get-Content -LiteralPath app/build.gradle.kts -Raw
    $match = [regex]::Match($build, 'versionName\s*=\s*"(\d+)\.(\d+)\.(\d+)"')
    if (-not $match.Success) { throw '无法读取语义化 versionName。' }
    $version = $match.Groups[0].Value -replace '^.*"(.*)".*$', '$1'
    $versionCode = [int]$match.Groups[1].Value * 10000 + [int]$match.Groups[2].Value * 100 + [int]$match.Groups[3].Value
    $changelog = Get-Content -LiteralPath CHANGELOG.md -Raw
    $entry = [regex]::Match($changelog, '(?ms)^## \[' + [regex]::Escape($version) + '\][^\r\n]*\r?\n(.*?)(?=^## \[|\z)')
    if (-not $entry.Success) { throw "CHANGELOG.md 缺少 $version 条目。" }
    $commit = (& git rev-parse HEAD).Trim()
    Require-Success '无法读取 Git 提交。'
    Write-Host "开始构建月汐 $version，versionCode=$versionCode。"
    & ./gradlew.bat verifyPrivacy testDebugUnitTest lintRelease assembleRelease --console=plain
    Require-Success '隐私检查、测试、Lint 或 release 构建失败，停止发布。'

    $sdk = $env:ANDROID_HOME
    if (-not $sdk) { $sdk = $env:ANDROID_SDK_ROOT }
    if (-not $sdk -and (Test-Path -LiteralPath local.properties)) {
        $line = Get-Content -LiteralPath local.properties | Where-Object { $_ -match '^sdk\.dir=' } | Select-Object -First 1
        if ($line) { $sdk = ($line -replace '^sdk\.dir=', '').Replace('\:', ':').Replace('\\', '\') }
    }
    if (-not $sdk -or -not (Test-Path -LiteralPath $sdk)) { throw '找不到 Android SDK，请设置 ANDROID_HOME。' }
    $buildTools = Get-ChildItem -LiteralPath (Join-Path $sdk 'build-tools') -Directory | Where-Object { $_.Name -match '^\d+\.\d+\.\d+$' } |
        Sort-Object { [version]$_.Name } -Descending | Select-Object -First 1
    if (-not $buildTools) { throw '找不到稳定版 Android Build Tools。' }
    $signer = Join-Path $buildTools.FullName 'apksigner.bat'
    $aapt = Join-Path $buildTools.FullName 'aapt2.exe'
    $apk = Join-Path $root 'app/build/outputs/apk/release/app-release.apk'
    if (-not (Test-Path -LiteralPath $apk)) { throw '没有找到 release APK。' }
    $certOutput = (& $signer verify --verbose --print-certs $apk 2>&1 | Out-String)
    Require-Success 'APK 签名校验失败。'
    if ($certOutput -match 'CN=Android Debug') { throw '检测到 Android Debug 证书，禁止发布。' }
    $cert = [regex]::Match($certOutput, '(?:Signer #1|V\d(?:\.\d+)? Signer):? certificate SHA-256 digest:\s*([a-fA-F0-9]{64})')
    if (-not $cert.Success) { throw '未能提取签名证书 SHA-256 指纹。' }
    $fingerprint = $cert.Groups[1].Value.ToLowerInvariant()
    $permissionOutput = (& $aapt dump permissions $apk 2>&1 | Out-String)
    Require-Success '无法读取最终 APK 的权限。'
    $permissions = @([regex]::Matches($permissionOutput, "uses-permission(?:-sdk-23)?: name='([^']+)'") | ForEach-Object { $_.Groups[1].Value } | Sort-Object -Unique)
    $allowed = @('android.permission.POST_NOTIFICATIONS', 'android.permission.USE_BIOMETRIC', 'android.permission.USE_FINGERPRINT',
        'android.permission.RECEIVE_BOOT_COMPLETED', 'android.permission.WAKE_LOCK', 'android.permission.FOREGROUND_SERVICE',
        'com.xichugeek.lunatide.DYNAMIC_RECEIVER_NOT_EXPORTED_PERMISSION')
    if ($permissions.Count -eq 0) { throw '最终 APK 权限解析结果为空，停止发布。' }
    foreach ($permission in $permissions) { if ($permission -notin $allowed) { throw "最终 APK 出现禁止权限：$permission" } }
    $badging = (& $aapt dump badging $apk 2>&1 | Out-String)
    Require-Success '无法核对 APK 包名和版本。'
    if ($badging -notmatch "package: name='com.xichugeek.lunatide' versionCode='$versionCode' versionName='$([regex]::Escape($version))'") { throw 'APK 包名或版本号与源码不一致。' }
    $minSdk = [int]([regex]::Match($badging, "sdkVersion:'(\d+)'").Groups[1].Value)
    $targetSdk = [int]([regex]::Match($badging, "targetSdkVersion:'(\d+)'").Groups[1].Value)
    if ($minSdk -ne 26 -or $targetSdk -lt 37) { throw 'APK 的最低/目标 SDK 不符合当前发布要求。' }
    Add-Type -AssemblyName System.IO.Compression.FileSystem
    $zip = [IO.Compression.ZipFile]::OpenRead($apk)
    try {
        foreach ($item in $zip.Entries | Where-Object { $_.FullName -match '^classes\d*\.dex$|^resources\.arsc$' }) {
            $stream = $item.Open(); $memory = [IO.MemoryStream]::new()
            try { $stream.CopyTo($memory); $bytes = $memory.ToArray() }
            finally { $stream.Dispose(); $memory.Dispose() }
            foreach ($encoding in @([Text.Encoding]::UTF8, [Text.Encoding]::Unicode)) {
                $text = $encoding.GetString($bytes)
                if ($text -match 'developer_seed|developer_performance|填充虚构示例数据|Developer: fill fictional sample data') { throw '最终 release APK 中出现 debug 示例数据入口，停止发布。' }
            }
        }
    } finally { $zip.Dispose() }
    $dist = Join-Path $root 'dist'
    New-Item -ItemType Directory -Path $dist -Force | Out-Null
    $name = "Lunatide-v$version.apk"
    $destination = Join-Path $dist $name
    Copy-Item -LiteralPath $apk -Destination $destination -Force
    $hash = (Get-FileHash -LiteralPath $destination -Algorithm SHA256).Hash.ToLowerInvariant()
    Set-Content -LiteralPath ($destination + '.sha256') -Value "$hash  $name" -Encoding utf8NoBOM
    $info = [ordered]@{ app = 'Lunatide'; versionName = $version; versionCode = $versionCode; apkFile = $name;
        apkBytes = (Get-Item -LiteralPath $destination).Length; apkSha256 = $hash; signerCertSha256 = $fingerprint;
        minSdk = $minSdk; targetSdk = $targetSdk; permissions = $permissions; gitCommit = $commit;
        builtAt = [DateTimeOffset]::Now.ToOffset([TimeSpan]::FromHours(8)).ToString('o') }
    $info | ConvertTo-Json -Depth 5 | Set-Content -LiteralPath (Join-Path $dist 'release-info.json') -Encoding utf8NoBOM
    $notes = @"
# 月汐 v$version

$($entry.Groups[1].Value.Trim())

APK：$name
SHA-256：$hash
签名证书 SHA-256：$fingerprint
构建源码提交：$commit

需要 Android 8.0 或以上。首次安装请按系统提示允许此来源安装应用。
升级请直接覆盖安装，不要先卸载；卸载和换机前请先导出加密备份。月汐不会自动检查更新。
截图均为虚构示例数据，预测仅供参考，不能用于避孕。
"@
    Set-Content -LiteralPath (Join-Path $dist "RELEASE_NOTES-v$version.md") -Value $notes -Encoding utf8NoBOM
    Write-Host "正式签名 APK 校验完成：$name，SHA-256=$hash"
    Write-Host "证书 SHA-256：$fingerprint"
    Write-Host '下一步先运行 ./scripts/update-site.ps1 更新官网素材与 README，检查并提交这些文档。'
    Write-Host '以下命令需要人类明确同意后才能执行；此脚本不会创建标签或 Release：'
    Write-Host "git tag v$version"
    Write-Host "git push origin v$version"
    Write-Host "gh release create v$version dist/$name dist/$name.sha256 --title '月汐 v$version' --notes-file dist/RELEASE_NOTES-v$version.md"
} catch {
    Write-Error -Message ("发布已停止：" + $_.Exception.Message) -ErrorAction Continue
    exit 1
}
