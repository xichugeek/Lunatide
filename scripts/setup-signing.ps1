#Requires -Version 7.0
[CmdletBinding()]
param([string]$KeytoolPath, [switch]$CheckOnly)
$ErrorActionPreference = 'Stop'
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$keyDirectory = Join-Path (Split-Path $root -Parent) 'keystores'
$keyFile = Join-Path $keyDirectory 'lunatide-release.jks'
$propertiesFile = Join-Path $root 'keystore.properties'

# Properties.load(InputStream) uses ISO-8859-1, so escape Unicode and leading whitespace.
function ConvertTo-JavaProperty([string]$value) {
    $builder = [Text.StringBuilder]::new()
    foreach ($character in $value.ToCharArray()) {
        $code = [int]$character
        if ($code -eq 92) { [void]$builder.Append('\\') }
        elseif ($code -eq 32) { [void]$builder.Append('\ ') }
        elseif ($code -lt 33 -or $code -gt 126) { [void]$builder.Append(('\u{0:x4}' -f $code)) }
        else { [void]$builder.Append($character) }
    }
    $builder.ToString()
}

try {
    if (-not $KeytoolPath) {
        if ($env:JAVA_HOME) { $KeytoolPath = Join-Path $env:JAVA_HOME 'bin/keytool.exe' }
        else { $KeytoolPath = (Get-Command keytool -ErrorAction Stop).Source }
    }
    if (-not (Test-Path -LiteralPath $KeytoolPath -PathType Leaf)) { throw '找不到 keytool，请用 -KeytoolPath 指定 JDK 的 keytool.exe。' }
    if ($CheckOnly) {
        Write-Host "工具可用；密钥存在：$(Test-Path -LiteralPath $keyFile)；本机配置存在：$(Test-Path -LiteralPath $propertiesFile)。未创建或读取密码。"
        exit 0
    }
    if (Test-Path -LiteralPath $propertiesFile) { throw 'keystore.properties 已存在，为防覆盖，未作修改。请直接运行 release.ps1。' }
    $Host.UI.RawUI.WindowTitle = '月汐 · 正式签名（请在此输入密码）'
    Write-Host '月汐正式签名设置。密码只在本窗口输入，不要发到聊天或 Issue。'
    Write-Host '密钥放在仓库外；本机配置已被 Git 忽略。请将密钥和密码备份到两个不同的地方。'
    if (-not (Test-Path -LiteralPath $keyFile)) {
        New-Item -ItemType Directory -Path $keyDirectory -Force | Out-Null
        Write-Host '现在由 keytool 提示输入新密码两次（输入时不显示字符）。'
        & $KeytoolPath -genkeypair -v -keystore $keyFile -storetype PKCS12 -alias lunatide `
            -keyalg RSA -keysize 4096 -validity 36500 -dname 'CN=Lunatide, OU=Open Source, O=XichuGeek, C=CN'
        if ($LASTEXITCODE -ne 0) { throw 'keytool 未成功创建密钥；没有生成签名配置。' }
    } else {
        Write-Host '已有密钥将保留，接下来只验证密码并配置，不重新生成密钥。'
    }
    $configured = $false
    for ($attempt = 1; $attempt -le 3; $attempt++) {
        $secure = Read-Host '请再次输入刚才的密钥库密码，用于本机构建配置' -AsSecureString
        $pointer = [IntPtr]::Zero
        $plain = $null
        $process = $null
        $start = $null
        try {
            $pointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
            $plain = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($pointer)
            $start = [Diagnostics.ProcessStartInfo]::new($KeytoolPath)
            $start.UseShellExecute = $false
            $start.CreateNoWindow = $true
            $start.RedirectStandardOutput = $true
            $start.RedirectStandardError = $true
            foreach ($argument in @('-list', '-keystore', $keyFile, '-alias', 'lunatide', '-storepass:env', 'LUNATIDE_SETUP_PASSWORD')) {
                [void]$start.ArgumentList.Add($argument)
            }
            # Only the child receives this value; it is never a command-line argument or tool output.
            $start.Environment['LUNATIDE_SETUP_PASSWORD'] = $plain
            $process = [Diagnostics.Process]::Start($start)
            [void]$start.Environment.Remove('LUNATIDE_SETUP_PASSWORD')
            $output = $process.StandardOutput.ReadToEndAsync()
            $errorOutput = $process.StandardError.ReadToEndAsync()
            if (-not $process.WaitForExit(30000)) { $process.Kill(); throw '密码校验超时，保留已有密钥，请稍后重试。' }
            [void]$output.GetAwaiter().GetResult()
            [void]$errorOutput.GetAwaiter().GetResult()
            if ($process.ExitCode -ne 0) {
                Write-Host '密码或别名校验失败，尚未写入配置，请重新输入。'
                continue
            }
            $escaped = ConvertTo-JavaProperty $plain
            $keyPath = ConvertTo-JavaProperty $keyFile.Replace('\', '/')
            $content = "storeFile=$keyPath`nstorePassword=$escaped`nkeyAlias=lunatide`nkeyPassword=$escaped`n"
            $file = [IO.FileStream]::new($propertiesFile, [IO.FileMode]::CreateNew, [IO.FileAccess]::Write)
            try {
                $bytes = [Text.UTF8Encoding]::new($false).GetBytes($content)
                $file.Write($bytes, 0, $bytes.Length)
                $file.Flush($true)
            } finally { $file.Dispose(); if ($bytes) { [Array]::Clear($bytes, 0, $bytes.Length) } }
            $configured = $true
            break
        } finally {
            if ($pointer -ne [IntPtr]::Zero) { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($pointer) }
            if ($process) { $process.Dispose() }
            if ($start) { [void]$start.Environment.Remove('LUNATIDE_SETUP_PASSWORD') }
            $secure.Dispose()
            $plain = $null; $escaped = $null; $content = $null
        }
    }
    if (-not $configured) { throw '尚未配置成功；密钥保持原样。可再次运行本脚本。' }
    Write-Host '签名已配置。请妥善备份密钥和密码，不要把配置内容发到聊天或提交 Git。'
    Write-Host '可以返回聊天继续正式构建；本脚本不会创建标签或 GitHub Release。'
} catch {
    Write-Host ('签名设置已停止：' + $_.Exception.Message) -ForegroundColor Red
    exit 1
}
