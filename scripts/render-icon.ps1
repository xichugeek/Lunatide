#Requires -Version 7.0
$ErrorActionPreference = 'Stop'
Add-Type -AssemblyName System.Drawing
$root = [IO.Path]::GetFullPath((Join-Path $PSScriptRoot '..'))
$output = Join-Path $root 'docs/site/icon-512.png'
New-Item -ItemType Directory -Path (Split-Path $output) -Force | Out-Null
$bitmap = [Drawing.Bitmap]::new(512, 512)
$graphics = [Drawing.Graphics]::FromImage($bitmap)
$moon = [Drawing.Drawing2D.GraphicsPath]::new()
$wave = [Drawing.Drawing2D.GraphicsPath]::new()
$pen = [Drawing.Pen]::new([Drawing.Color]::White, 3)
try {
    $graphics.SmoothingMode = [Drawing.Drawing2D.SmoothingMode]::AntiAlias
    $graphics.Clear([Drawing.ColorTranslator]::FromHtml('#4B4A8C'))
    $graphics.ScaleTransform(512 / 108, 512 / 108)
    # The same cubic paths as app/src/main/res/drawable/ic_moon_wave.xml.
    $moon.StartFigure()
    $moon.AddBezier(64, 27, 43, 26, 35, 40, 39, 52)
    $moon.AddBezier(39, 52, 42, 62, 54, 66, 66, 61)
    $moon.AddBezier(66, 61, 49, 60, 43, 43, 64, 27)
    $moon.CloseFigure()
    $graphics.FillPath([Drawing.Brushes]::White, $moon)
    $wave.AddBezier(31, 73, 41, 65, 48, 65, 56, 71)
    $wave.AddBezier(56, 71, 64, 77, 71, 77, 79, 69)
    $pen.StartCap = [Drawing.Drawing2D.LineCap]::Round
    $pen.EndCap = [Drawing.Drawing2D.LineCap]::Round
    $graphics.DrawPath($pen, $wave)
    $bitmap.Save($output, [Drawing.Imaging.ImageFormat]::Png)
} finally { $pen.Dispose(); $wave.Dispose(); $moon.Dispose(); $graphics.Dispose(); $bitmap.Dispose() }
Write-Host '已从月汐矢量路径生成 docs/site/icon-512.png。'
