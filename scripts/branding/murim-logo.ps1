# The mod's logo (src/main/resources/murimblock_logo.png, shown in the mod list: neoforge.mods.toml logoFile), after the
# style of Minecraft's newer titles (not a copy): two words of heavy blocky letters, smooth, ringed in thick black.
# MURIM in pale cracked stone, its depth falling below it; BLOCK under it in cinnabar red, its depth rising behind the
# first word (it shows between MURIM's letters). The letters are our own (heavy, 7 cells high), the cracks drawn here.
# Usage: powershell -NoProfile -File scripts/branding/murim-logo.ps1
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$WIDTH = 1200; $HEIGHT = 700
$img = New-Object System.Drawing.Bitmap $WIDTH, $HEIGHT
$gfx = [System.Drawing.Graphics]::FromImage($img)
$gfx.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::None
function Col($hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }

# Heavy letters: strokes two cells thick, 7 rows, their own widths.
$GLYPHS = @{
  'M' = @('1100011', '1110111', '1111111', '1101011', '1100011', '1100011', '1100011')
  'U' = @('110011', '110011', '110011', '110011', '110011', '110011', '011110')
  'R' = @('111110', '110011', '110011', '111110', '111100', '110110', '110011')
  'I' = @('111111', '001100', '001100', '001100', '001100', '001100', '111111')
  'B' = @('111110', '110011', '110011', '111110', '110011', '110011', '111110')
  'L' = @('110000', '110000', '110000', '110000', '110000', '110000', '111111')
  'O' = @('011110', '110011', '110011', '110011', '110011', '110011', '011110')
  'C' = @('011111', '110000', '110000', '110000', '110000', '110000', '011111')
  'K' = @('110011', '110110', '111100', '111000', '111100', '110110', '110011')
}

<#
 One word of letters 7 cells high of $cell pixels (a cell between letters), centred at $cx, its top at $top; its depth
 $depth pixels (+ down, - up). Drawn: the thick black ring round face and depth, the depth shaded from $sideA to
 $sideB with dark edges where a stroke ends, the face shaded from $faceA to $faceB with a lit top edge and a black line
 round each letter, cracks across it.
#>
function DrawWord($word, $cell, $cx, $top, $depth, $faceA, $faceB, $sideA, $sideB, $crack, $light, $seed) {
  $cols = -1; for ($n = 0; $n -lt $word.Length; $n++) { $cols += $GLYPHS[[string]$word[$n]][0].Length + 1 }
  $left = $cx - $cols * $cell / 2
  $filled = @{}
  $at = 0
  for ($n = 0; $n -lt $word.Length; $n++) {
    $rows = $GLYPHS[[string]$word[$n]]
    for ($r = 0; $r -lt 7; $r++) { for ($c = 0; $c -lt $rows[$r].Length; $c++) { if ($rows[$r][$c] -eq '1') { $filled["$($at + $c),$r"] = $n } } }
    $at += $rows[0].Length + 1
  }
  $has = { param($c, $r) $filled.ContainsKey("$c,$r") }
  $cells = @($filled.Keys | ForEach-Object { $p = $_.Split(','); , @([int]$p[0], [int]$p[1]) })
  $cellX = { param($c) [single]($left + $c * $cell) }; $cellY = { param($r) [single]($top + $r * $cell) }
  $up = [Math]::Min(0, $depth); $down = [Math]::Max(0, $depth)
  # The black ring: every cell with its depth, grown by the ring's width (square corners, blocky like the letters).
  $ring = 13
  foreach ($k in $cells) { $gfx.FillRectangle([System.Drawing.Brushes]::Black, (& $cellX $k[0]) - $ring, (& $cellY $k[1]) + $up - $ring, $cell + 2 * $ring, $cell + [Math]::Abs($depth) + 2 * $ring) }
  # The depth.
  $sideBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush (New-Object System.Drawing.RectangleF 0, ([single]($top + $up)), 10, ([single](7 * $cell + [Math]::Abs($depth)))), $sideA, $sideB, 90
  foreach ($k in $cells) { $gfx.FillRectangle($sideBrush, (& $cellX $k[0]), (& $cellY $k[1]) + $up, $cell, $cell + [Math]::Abs($depth)) }
  $edgePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 8, 6, 5)), 4
  foreach ($k in $cells) {
    $x0 = & $cellX $k[0]; $x1 = $x0 + $cell
    $y0 = if ($depth -gt 0) { (& $cellY $k[1]) + $cell } else { (& $cellY $k[1]) + $depth }
    $y1 = if ($depth -gt 0) { (& $cellY $k[1]) + $cell + $depth } else { & $cellY $k[1] }
    if (-not (& $has ($k[0] - 1) $k[1])) { $gfx.DrawLine($edgePen, $x0, $y0, $x0, $y1) }
    if (-not (& $has ($k[0] + 1) $k[1])) { $gfx.DrawLine($edgePen, $x1, $y0, $x1, $y1) }
  }
  # The face.
  $faceBrush = New-Object System.Drawing.Drawing2D.LinearGradientBrush (New-Object System.Drawing.RectangleF 0, ([single]$top), 10, ([single](7 * $cell))), $faceA, $faceB, 90
  foreach ($k in $cells) { $gfx.FillRectangle($faceBrush, (& $cellX $k[0]), (& $cellY $k[1]), $cell, $cell) }
  $clip = New-Object System.Drawing.Drawing2D.GraphicsPath
  foreach ($k in $cells) { $clip.AddRectangle((New-Object System.Drawing.RectangleF (& $cellX $k[0]), (& $cellY $k[1]), $cell, $cell)) }
  $state = $gfx.Save(); $gfx.SetClip($clip); $gfx.SmoothingMode = [System.Drawing.Drawing2D.SmoothingMode]::AntiAlias
  # Cracks: jagged lines into each letter, with a short branch.
  $rand = New-Object System.Random $seed
  $crackPen = New-Object System.Drawing.Pen $crack, 5
  $crackPen.LineJoin = [System.Drawing.Drawing2D.LineJoin]::Miter
  for ($n = 0; $n -lt $word.Length; $n++) {
    $mine = @($cells | Where-Object { $filled["$($_[0]),$($_[1])"] -eq $n })
    for ($t = 0; $t -lt 2 + $rand.Next(2); $t++) {
      $k = $mine[$rand.Next($mine.Count)]
      $x = (& $cellX $k[0]) + $rand.NextDouble() * $cell; $y = (& $cellY $k[1]) + $rand.NextDouble() * $cell
      $pts = @((New-Object System.Drawing.PointF ([single]$x), ([single]$y)))
      $dir = $rand.Next(4) * [Math]::PI / 2
      for ($s = 0; $s -lt 3 + $rand.Next(3); $s++) {
        # Mostly along the grid, a cracked block's way.
        if ($rand.Next(3) -eq 0) { $dir += ($rand.Next(2) * 2 - 1) * [Math]::PI / 2 }
        $len = $cell * (0.35 + $rand.NextDouble() * 0.5)
        $x += [Math]::Cos($dir) * $len; $y += [Math]::Sin($dir) * $len
        $pts += New-Object System.Drawing.PointF ([single]$x), ([single]$y)
      }
      $gfx.DrawLines($crackPen, $pts)
    }
  }
  $gfx.Restore($state)
  # The lit top edges, then the black line round each letter's face.
  $lightPen = New-Object System.Drawing.Pen $light, 5
  $linePen = New-Object System.Drawing.Pen ([System.Drawing.Color]::FromArgb(255, 8, 6, 5)), 5
  foreach ($k in $cells) {
    $x0 = & $cellX $k[0]; $y0 = & $cellY $k[1]; $x1 = $x0 + $cell; $y1 = $y0 + $cell
    if (-not (& $has $k[0] ($k[1] - 1))) { $gfx.DrawLine($lightPen, $x0, $y0 + 4, $x1, $y0 + 4) }
  }
  foreach ($k in $cells) {
    $x0 = & $cellX $k[0]; $y0 = & $cellY $k[1]; $x1 = $x0 + $cell; $y1 = $y0 + $cell
    if (-not (& $has $k[0] ($k[1] - 1))) { $gfx.DrawLine($linePen, $x0 - 2, $y0, $x1 + 2, $y0) }
    if (-not (& $has $k[0] ($k[1] + 1))) { $gfx.DrawLine($linePen, $x0 - 2, $y1, $x1 + 2, $y1) }
    if (-not (& $has ($k[0] - 1) $k[1])) { $gfx.DrawLine($linePen, $x0, $y0 - 2, $x0, $y1 + 2) }
    if (-not (& $has ($k[0] + 1) $k[1])) { $gfx.DrawLine($linePen, $x1, $y0 - 2, $x1, $y1 + 2) }
  }
}

$CELL = 22; $CX = $WIDTH / 2
# MURIM: top 90, face 7 cells (182), depth 44 below. BLOCK: top 90 + 182 + 44 + 26, depth rising 70 behind MURIM.
$murimTop = 90; $murimDepth = 44
$blockTop = $murimTop + 7 * $CELL + $murimDepth + 30
DrawWord 'BLOCK' $CELL $CX $blockTop -74 (Col '#ff6a4f') (Col '#b01a12') (Col '#3c0805') (Col '#8f1a12') (Col '#6e1410') (Col '#ff9a86') 11
DrawWord 'MURIM' $CELL $CX $murimTop $murimDepth (Col '#d8d3cc') (Col '#aaa49c') (Col '#6f6a64') (Col '#2c2927') (Col '#56514c') (Col '#f1ede7') 7
$gfx.Dispose()

# Cropped to what is drawn, with a margin.
$minX = $WIDTH; $minY = $HEIGHT; $maxX = 0; $maxY = 0
for ($y = 0; $y -lt $HEIGHT; $y += 2) { for ($x = 0; $x -lt $WIDTH; $x += 2) { if ($img.GetPixel($x, $y).A -gt 0) { if ($x -lt $minX) { $minX = $x }; if ($x -gt $maxX) { $maxX = $x }; if ($y -lt $minY) { $minY = $y }; if ($y -gt $maxY) { $maxY = $y } } } }
$m = 12
$x0 = [Math]::Max(0, $minX - $m); $y0 = [Math]::Max(0, $minY - $m)
$crop = New-Object System.Drawing.Rectangle $x0, $y0, ([Math]::Min($WIDTH, $maxX + $m) - $x0), ([Math]::Min($HEIGHT, $maxY + $m) - $y0)
$out = $img.Clone($crop, $img.PixelFormat)
$path = Join-Path $root 'src\main\resources\murimblock_logo.png'
$out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
Write-Output ("wrote $path " + $out.Width + 'x' + $out.Height)
$out.Dispose(); $img.Dispose()
