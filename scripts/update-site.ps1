#Requires -Version 7.0
[CmdletBinding()]
param()
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
Set-Location -LiteralPath $root
function Write-Utf8Lf([string]$path, [string]$value) {
    $normalized = ($value -replace "`r`n?", "`n").TrimEnd([char[]]"`n") + "`n"
    [IO.File]::WriteAllText($path, $normalized, [Text.UTF8Encoding]::new($false))
}
$source = Join-Path $root 'dist/release-info.json'
if (-not (Test-Path -LiteralPath $source)) { throw '缺少真实 release-info.json。请先由人类配置正式签名，并成功运行 release.ps1；不生成虚假的校验信息。' }
$sourceText = Get-Content -LiteralPath $source -Raw
$info = $sourceText | ConvertFrom-Json
$sourceJson = [System.Text.Json.JsonDocument]::Parse($sourceText)
try { $info.builtAt = $sourceJson.RootElement.GetProperty('builtAt').GetString() }
finally { $sourceJson.Dispose() }
if ($info.apkFile -notmatch '^Lunatide-v\d+\.\d+\.\d+\.apk$' -or $info.apkSha256 -notmatch '^[a-f0-9]{64}$' -or $info.signerCertSha256 -notmatch '^[a-f0-9]{64}$') { throw '发布信息格式无效。' }
$apk = Join-Path $root ('dist/' + $info.apkFile)
if ((Get-FileHash -LiteralPath $apk -Algorithm SHA256).Hash.ToLowerInvariant() -ne $info.apkSha256 -or (Get-Item -LiteralPath $apk).Length -ne $info.apkBytes) { throw '发布信息与 APK 不匹配。' }
$template = Get-Content -LiteralPath docs/site/project-page.template.md -Raw
foreach ($property in $info.PSObject.Properties) { $template = $template.Replace('{{' + $property.Name + '}}', [string]$property.Value) }
if ($template -match '\{\{[^}]+\}\}') { throw '官网文案仍有未填字段。' }
Write-Utf8Lf (Join-Path $root 'docs/site/release-info.json') (Get-Content -LiteralPath $source -Raw)
Write-Utf8Lf (Join-Path $root 'docs/site/project-page.md') $template
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
Write-Utf8Lf (Join-Path $root 'README.md') $readme
Write-Host '已从真实签名构建更新官网信息及 README，请检查并提交文档。'
