# The mod's logo (src/main/resources/murimblock_logo.png, shown in the mod list: neoforge.mods.toml logoFile), in pixel
# art: a cinnabar seal carved with 武林 (Murim) on the left, MURIMBLOCK in ivory pixel letters ringed with ink, a jian
# with a bronze guard and a jade tassel beneath. Drawn at 200 x 64 pixels, written four times larger, nearest neighbour.
# Usage: powershell -NoProfile -File scripts/branding/murim-logo.ps1
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$W = 200; $H = 64; $SCALE = 4
$img = New-Object System.Drawing.Bitmap $W, $H
function C($hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
$INK = C '#1b1512'; $IVORY = C '#f2e6c9'; $LIGHT = C '#fff8e4'; $SHADE = C '#cbb88f'; $SHADOW = C '#0b0807'
$CINNABAR = C '#b3261e'; $CINNABAR_DARK = C '#7e1a14'; $SEAL_INK = C '#f4e2c4'
$STEEL = C '#c9d0d6'; $STEEL_DARK = C '#8b9198'; $BRONZE = C '#c39a3c'; $BRONZE_DARK = C '#8a6a24'; $GRIP = C '#6e1f1c'
$JADE = C '#3f8f6b'; $JADE_LIGHT = C '#69b892'
function Px($x, $y, $c) { if ($x -ge 0 -and $y -ge 0 -and $x -lt $W -and $y -lt $H) { $img.SetPixel($x, $y, $c) } }
function Rect($x0, $y0, $x1, $y1, $c) { for ($y = $y0; $y -le $y1; $y++) { for ($x = $x0; $x -le $x1; $x++) { Px $x $y $c } } }

# ---- the seal: cinnabar, worn at its edge, 武林 carved in it
$S0 = 6; $S1 = 47; $T0 = 11; $T1 = 52
Rect $S0 $T0 $S1 $T1 $CINNABAR
Rect $S0 $T1 $S1 $T1 $CINNABAR_DARK; Rect $S1 $T0 $S1 $T1 $CINNABAR_DARK
# An inner rim of the carved line, a pixel in from the edge.
for ($i = $S0 + 2; $i -le $S1 - 2; $i++) { Px $i ($T0 + 2) $SEAL_INK; Px $i ($T1 - 2) $SEAL_INK }
for ($i = $T0 + 2; $i -le $T1 - 2; $i++) { Px ($S0 + 2) $i $SEAL_INK; Px ($S1 - 2) $i $SEAL_INK }
# Worn: a few nicks out of the edge and the rim.
foreach ($n in @(@(9, 11), @(23, 11), @(40, 11), @(6, 20), @(6, 37), @(47, 27), @(47, 44), @(15, 52), @(33, 52), @(13, 13), @(45, 30), @(29, 50))) { Px $n[0] $n[1] ([System.Drawing.Color]::Transparent) }
foreach ($n in @(@(18, 13), @(36, 50), @(8, 26), @(45, 40))) { Px $n[0] $n[1] $CINNABAR }
# 武林, one bit a pixel from SimSun's own bitmap glyphs, side by side.
$glyphs = New-Object System.Drawing.Bitmap 40, 20
$g = [System.Drawing.Graphics]::FromImage($glyphs)
$g.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::SingleBitPerPixelGridFit
$font = New-Object System.Drawing.Font 'SimSun', 16, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$g.DrawString([string][char]0x6B66 + [char]0x6797, $font, [System.Drawing.Brushes]::Black, 0, 1)
$g.Dispose()
# Where the glyphs' ink lies, to centre them in the seal.
$minX = 99; $maxX = -1; $minY = 99; $maxY = -1
for ($y = 0; $y -lt 20; $y++) { for ($x = 0; $x -lt 40; $x++) { if ($glyphs.GetPixel($x, $y).A -gt 128) { $minX = [Math]::Min($minX, $x); $maxX = [Math]::Max($maxX, $x); $minY = [Math]::Min($minY, $y); $maxY = [Math]::Max($maxY, $y) } } }
$ox = [int](($S0 + $S1 + 1 - ($maxX - $minX + 1)) / 2) - $minX; $oy = [int](($T0 + $T1 + 1 - ($maxY - $minY + 1)) / 2) - $minY
for ($y = 0; $y -lt 20; $y++) { for ($x = 0; $x -lt 40; $x++) { if ($glyphs.GetPixel($x, $y).A -gt 128) { Px ($x + $ox) ($y + $oy) $SEAL_INK } } }

# ---- MURIMBLOCK: 5 x 7 letters drawn twice as large, ivory lit from above, an ink ring, a shadow
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
$TX = 55; $TY = 14; $SIZE = 2; $STEP = 12
$fill = New-Object 'bool[,]' $W, $H
$text = 'MURIMBLOCK'
for ($n = 0; $n -lt $text.Length; $n++) {
  $rows = $FONT[[string]$text[$n]]
  for ($r = 0; $r -lt 7; $r++) { for ($c = 0; $c -lt 5; $c++) { if ($rows[$r][$c] -eq '1') {
    for ($dy = 0; $dy -lt $SIZE; $dy++) { for ($dx = 0; $dx -lt $SIZE; $dx++) { $fill[($TX + $n * $STEP + $c * $SIZE + $dx), ($TY + $r * $SIZE + $dy)] = $true } } } } }
}
# The shadow (down and right), then the ink ring, then the letters.
for ($y = 0; $y -lt $H; $y++) { for ($x = 0; $x -lt $W; $x++) { if ($fill[$x, $y]) { Px ($x + 2) ($y + 2) $SHADOW } } }
for ($y = 1; $y -lt $H - 1; $y++) { for ($x = 1; $x -lt $W - 1; $x++) {
  if (-not $fill[$x, $y] -and ($fill[($x - 1), $y] -or $fill[($x + 1), $y] -or $fill[$x, ($y - 1)] -or $fill[$x, ($y + 1)] -or $fill[($x - 1), ($y - 1)] -or $fill[($x + 1), ($y + 1)] -or $fill[($x + 1), ($y - 1)] -or $fill[($x - 1), ($y + 1)])) { Px $x $y $INK } } }
for ($y = 0; $y -lt $H; $y++) { for ($x = 0; $x -lt $W; $x++) { if ($fill[$x, $y]) {
  $c = $IVORY
  if (-not $fill[$x, ($y - 1)]) { $c = $LIGHT } elseif (-not $fill[$x, ($y + 1)]) { $c = $SHADE }
  Px $x $y $c } } }

# ---- the jian beneath: pommel and jade tassel, red-wrapped grip, bronze guard, a long steel blade to its point
$BY = 41
Rect 56 ($BY - 1) 57 ($BY + 1) $BRONZE; Px 56 ($BY + 1) $BRONZE_DARK
for ($x = 58; $x -le 66; $x++) { Px $x ($BY - 1) $INK; Px $x ($BY + 1) $INK; Px $x $BY $(if ($x % 2) { $GRIP } else { $CINNABAR_DARK }) }
Rect 67 ($BY - 3) 68 ($BY + 3) $BRONZE; Px 68 ($BY + 3) $BRONZE_DARK; Px 68 ($BY - 3) $BRONZE_DARK; Px 67 ($BY - 4) $INK; Px 67 ($BY + 4) $INK
for ($x = 69; $x -le 184; $x++) { Px $x ($BY - 1) $INK; Px $x $BY $STEEL; Px $x ($BY + 1) $STEEL_DARK; Px $x ($BY + 2) $INK }
Px 185 ($BY - 1) $INK; Px 185 $BY $STEEL; Px 185 ($BY + 1) $INK; Px 186 $BY $INK
# The tassel hangs from the pommel.
Px 55 ($BY + 1) $JADE; Px 55 ($BY + 2) $JADE; Px 54 ($BY + 3) $JADE_LIGHT; Px 55 ($BY + 3) $JADE; Px 54 ($BY + 4) $JADE; Px 55 ($BY + 4) $JADE_LIGHT
Px 53 ($BY + 5) $JADE; Px 54 ($BY + 5) $JADE; Px 55 ($BY + 5) $JADE; Px 56 ($BY + 5) $JADE_LIGHT; Px 53 ($BY + 6) $JADE_LIGHT; Px 55 ($BY + 6) $JADE; Px 56 ($BY + 6) $JADE

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
