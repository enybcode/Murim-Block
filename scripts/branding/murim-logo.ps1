# The mod's logo (src/main/resources/murimblock_logo.png, shown in the mod list: neoforge.mods.toml logoFile), after the
# way Minecraft's own title is made, not a copy of it: MURIMBLOCK in heavy block letters, each letter's pixel a block
# with a face of weathered stone and its depth stepping down and right, ringed in black; under it, where Minecraft puts
# its edition, a cinnabar ribbon with folded ends bearing 武林 (Murim). Drawn at 256 x 64, written four times larger.
# Usage: powershell -NoProfile -File scripts/branding/murim-logo.ps1
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$W = 256; $H = 64; $SCALE = 4
$img = New-Object System.Drawing.Bitmap $W, $H
function C($hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
$OUTLINE = C '#120f0e'
$STONE = @((C '#a3a19d'), (C '#979590'), (C '#8b8984'), (C '#adaba6'), (C '#7f7d78'))
$STONE_TOP = C '#c4c2bc'; $STONE_SPECK = C '#64625e'; $MOSS = C '#6e8a5a'
$DEPTH = @((C '#5d5b57'), (C '#4f4d4a'), (C '#43413e'))
$CINNABAR = C '#b3261e'; $CINNABAR_DARK = C '#7e1a14'; $CINNABAR_FOLD = C '#5c120e'; $IVORY = C '#f4e2c4'
function Px($x, $y, $c) { if ($x -ge 0 -and $y -ge 0 -and $x -lt $W -and $y -lt $H) { $img.SetPixel($x, $y, $c) } }

# ---- the letters, 5 x 7, each pixel a block 4 x 4
$FONT = @{
  'M' = @('10001', '11011', '10101', '10101', '10001', '10001', '10001')
  'U' = @('10001', '10001', '10001', '10001', '10001', '10001', '01110')
  'R' = @('11110', '10001', '10001', '11110', '10100', '10010', '10001')
  'I' = @('11111', '00100', '00100', '00100', '00100', '00100', '11111')
  'B' = @('11110', '10001', '10001', '11110', '10001', '10001', '11110')
  'L' = @('10000', '10000', '10000', '10000', '10000', '10000', '11111')
  'O' = @('01110', '10001', '10001', '10001', '10001', '10001', '01110')
  'C' = @('01111', '10000', '10000', '10000', '10000', '10000', '01111')
  'K' = @('10001', '10010', '10100', '11000', '10100', '10010', '10001')
}
$B = 4; $DEEP = 3; $text = 'MURIMBLOCK'
$LETTER = 5 * $B + $B   # a letter and a block's gap
$TX = [int](($W - ($text.Length * $LETTER - $B) - $DEEP) / 2); $TY = 4
$face = New-Object 'bool[,]' $W, $H
for ($n = 0; $n -lt $text.Length; $n++) {
  $rows = $FONT[[string]$text[$n]]
  for ($r = 0; $r -lt 7; $r++) { for ($c = 0; $c -lt 5; $c++) { if ($rows[$r][$c] -eq '1') {
    for ($dy = 0; $dy -lt $B; $dy++) { for ($dx = 0; $dx -lt $B; $dx++) { $face[($TX + $n * $LETTER + $c * $B + $dx), ($TY + $r * $B + $dy)] = $true } } } } }
}
# The depth: the face stepped down and right, darker the further back.
$solid = New-Object 'bool[,]' $W, $H
for ($d = $DEEP; $d -ge 1; $d--) {
  for ($y = 0; $y -lt $H - $d; $y++) { for ($x = 0; $x -lt $W - $d; $x++) { if ($face[$x, $y]) { Px ($x + $d) ($y + $d) $DEPTH[$d - 1]; $solid[($x + $d), ($y + $d)] = $true } } }
}
# The face: weathered stone, a lighter top edge, a speck or a touch of moss here and there (the same each time).
for ($y = 0; $y -lt $H; $y++) { for ($x = 0; $x -lt $W; $x++) { if ($face[$x, $y]) {
  $solid[$x, $y] = $true
  $grain = (($x * 73) + ($y * 151) + (($x * $y) % 17) * 29) % 97
  $c = $STONE[$grain % 5]
  if ($grain -eq 3 -or $grain -eq 41) { $c = $STONE_SPECK }
  if ($grain -eq 57 -and $y -gt $TY + 3 * $B) { $c = $MOSS }
  if (-not $face[$x, ($y - 1)]) { $c = $STONE_TOP }
  Px $x $y $c } } }
# Ringed in black.
for ($y = 1; $y -lt $H - 1; $y++) { for ($x = 1; $x -lt $W - 1; $x++) {
  if (-not $solid[$x, $y] -and ($solid[($x - 1), $y] -or $solid[($x + 1), $y] -or $solid[$x, ($y - 1)] -or $solid[$x, ($y + 1)])) { Px $x $y $OUTLINE } } }

# ---- the ribbon under the letters, its ends folded back, 武林 on it
$RY = 41; $RH = 13; $RW = 64; $RX = [int](($W - $RW) / 2)
$ribbon = New-Object 'bool[,]' $W, $H
function Ribbon($x, $y, $c) { Px $x $y $c; $ribbon[$x, $y] = $true }
# The folded tails, behind, each with its notch.
foreach ($side in @(-1, 1)) {
  $x0 = if ($side -lt 0) { $RX - 12 } else { $RX + $RW + 1 }
  for ($i = 0; $i -lt 12; $i++) { for ($j = 2; $j -lt $RH + 2; $j++) {
    $edge = if ($side -lt 0) { $i } else { 11 - $i }
    $notch = [Math]::Abs($j - ($RH + 3) / 2) -lt (4 - $edge)
    if (-not $notch) { Ribbon ($x0 + $i) ($RY + $j) $CINNABAR_DARK } } }
  # The fold where the tail meets the band.
  $xf = if ($side -lt 0) { $RX - 1 } else { $RX + $RW }
  for ($j = 2; $j -lt $RH + 2; $j++) { Ribbon $xf ($RY + $j) $CINNABAR_FOLD }
}
for ($j = 0; $j -lt $RH; $j++) { for ($i = 0; $i -lt $RW; $i++) {
  $c = if ($j -eq 0) { C '#d04a3c' } elseif ($j -eq $RH - 1) { $CINNABAR_DARK } else { $CINNABAR }
  Ribbon ($RX + $i) ($RY + $j) $c } }
for ($y = 1; $y -lt $H - 1; $y++) { for ($x = 1; $x -lt $W - 1; $x++) {
  if (-not $ribbon[$x, $y] -and -not $solid[$x, $y] -and ($ribbon[($x - 1), $y] -or $ribbon[($x + 1), $y] -or $ribbon[$x, ($y - 1)] -or $ribbon[$x, ($y + 1)])) { Px $x $y $OUTLINE } } }
# 武林, SimSun's own 12-pixel bitmap glyphs, centred on the band.
$glyphs = New-Object System.Drawing.Bitmap 40, 16
$g = [System.Drawing.Graphics]::FromImage($glyphs)
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::SingleBitPerPixelGridFit
$font = New-Object System.Drawing.Font 'SimSun', 12, ([System.Drawing.FontStyle]::Regular), ([System.Drawing.GraphicsUnit]::Pixel)
$g.DrawString([string][char]0x6B66 + ' ' + [char]0x6797, $font, [System.Drawing.Brushes]::Black, 0, 1)
$g.Dispose()
$minX = 99; $maxX = -1; $minY = 99; $maxY = -1
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 40; $x++) { if ($glyphs.GetPixel($x, $y).A -gt 128) { $minX = [Math]::Min($minX, $x); $maxX = [Math]::Max($maxX, $x); $minY = [Math]::Min($minY, $y); $maxY = [Math]::Max($maxY, $y) } } }
$ox = $RX + [int](($RW - ($maxX - $minX + 1)) / 2) - $minX; $oy = $RY + [int](($RH - ($maxY - $minY + 1)) / 2) - $minY
for ($y = 0; $y -lt 16; $y++) { for ($x = 0; $x -lt 40; $x++) { if ($glyphs.GetPixel($x, $y).A -gt 128) { Px ($x + $ox + 1) ($y + $oy + 1) $CINNABAR_FOLD; Px ($x + $ox) ($y + $oy) $IVORY } } }

# ---- four times larger, every pixel a sharp square
$out = New-Object System.Drawing.Bitmap ($W * $SCALE), ($H * $SCALE)
$g = [System.Drawing.Graphics]::FromImage($out)
$g.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g.DrawImage($img, 0, 0, $W * $SCALE, $H * $SCALE)
$g.Dispose()
$path = Join-Path $root 'src\main\resources\murimblock_logo.png'
$out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$out.Dispose(); $img.Dispose(); $glyphs.Dispose()
Write-Output "wrote $path"
