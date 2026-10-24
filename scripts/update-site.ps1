#Requires -Version 7.0
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $root
$source = Join-Path $root 'dist/release-info.json'
if (-not (Test-Path -LiteralPath $source)) { throw '缺少真实 release-info.json。请先由人类配置正式签名，并成功运行 release.ps1；不生成虚假的校验信息。' }
$info = Get-Content -LiteralPath $source -Raw | ConvertFrom-Json
if ($info.apkFile -notmatch '^Lunatide-v\d+\.\d+\.\d+\.apk$' -or $info.apkSha256 -notmatch '^[a-f0-9]{64}$' -or $info.signerCertSha256 -notmatch '^[a-f0-9]{64}$') { throw '发布信息格式无效。' }
$apk = Join-Path $root ('dist/' + $info.apkFile)
if ((Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant() -ne $info.apkSha256 -or (Get-Item -LiteralPath $apk).Length -ne $info.apkBytes) { throw '发布信息与 APK 不匹配。' }
$template = Get-Content -LiteralPath docs/site/project-page.template.md -Raw
foreach ($property in $info.PSObject.Properties) { $template = $template.Replace('{{' + $property.Name + '}}', [string]$property.Value) }
if ($template -match '\{\{[^}]+\}\}') { throw '官网文案仍有未填字段。' }
Copy-Item -LiteralPath $source -Destination docs/site/release-info.json -Force
Set-Content -LiteralPath docs/site/project-page.md -Value $template -Encoding utf8NoBOM
$readme = Get-Content -LiteralPath README.md -Raw
$block = @"
<!-- RELEASE_INFO_START -->
已签名构建：v$($info.versionName)（$($info.versionCode)），$($info.apkBytes) 字节。

- APK：[$($info.apkFile)](https://github.com/xichugeek/Lunatide/releases/download/v$($info.versionName)/$($info.apkFile))
- APK SHA-256：``$($info.apkSha256)``
- 签名证书 SHA-256：``$($info.signerCertSha256)``

下载链接在对应 GitHub Release 正式发布后生效，是否已发布请查看 Releases 页面。
<!-- RELEASE_INFO_END -->
"@
if ($readme -notmatch '(?s)<!-- RELEASE_INFO_START -->.*?<!-- RELEASE_INFO_END -->') { throw 'README 缺少发布信息标记。' }
$readme = [regex]::Replace($readme, '(?s)<!-- RELEASE_INFO_START -->.*?<!-- RELEASE_INFO_END -->', [System.Text.RegularExpressions.MatchEvaluator]{ param($match) $block })
Set-Content -LiteralPath README.md -Value $readme -Encoding utf8NoBOM
Write-Host '已从真实签名构建更新官网信息及 README。请检查文档并提交；尚未推送标签或创建 Release。'
