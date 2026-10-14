# The mod's logo (src/main/resources/murimblock_logo.png, shown in the mod list: neoforge.mods.toml logoFile), in pixel
# art: a worn cinnabar seal carved with 武林 (Murim) on the left; MURIMBLOCK in pixel letters, ivory warming to gold,
# ringed with ink and dropping a shadow; beneath, a jian (a bright ridge down its blade, a glint at its point, a bronze
# guard set with jade, a braided grip, a jade tassel streaming from its pommel). Drawn at
# 220 x 72 pixels, written four times larger, nearest neighbour.
# Usage: powershell -NoProfile -File scripts/branding/murim-logo.ps1
Add-Type -AssemblyName System.Drawing
$root = Split-Path -Parent (Split-Path -Parent $PSScriptRoot)
$W = 220; $H = 72; $SCALE = 4
$img = New-Object System.Drawing.Bitmap $W, $H
function C($hex) { [System.Drawing.ColorTranslator]::FromHtml($hex) }
$CLEAR = [System.Drawing.Color]::Transparent
$INK = C '#1b1512'; $SHADOW = C '#0b0807'
# The letters, row by row from the top of a letter (14 rows): a lit top, ivory, warming to gold toward the foot.
$TONE = @('#fffbea', '#f6ecd3', '#f2e6c9', '#f2e6c9', '#efe0bd', '#ecdab1', '#e9d3a3', '#e5cb94', '#e0c283', '#dab772', '#d4ac63', '#cda157', '#c59650', '#b98a46') | ForEach-Object { C $_ }
$CINNABAR = C '#b3261e'; $CINNABAR_LIGHT = C '#c83a2c'; $CINNABAR_DARK = C '#7e1a14'; $SEAL_INK = C '#f4e2c4'
$STEEL_RIDGE = C '#eef2f5'; $STEEL = C '#c9d0d6'; $STEEL_DARK = C '#8b9198'; $WHITE = C '#ffffff'
$BRONZE = C '#d4a640'; $BRONZE_DARK = C '#8a6a24'; $GRIP = C '#6e1f1c'; $GRIP_LIGHT = C '#9a3329'
$JADE = C '#3f8f6b'; $JADE_LIGHT = C '#7cc9a2'
function Px($x, $y, $c) { if ($x -ge 0 -and $y -ge 0 -and $x -lt $W -and $y -lt $H) { $img.SetPixel($x, $y, $c) } }
function Rect($x0, $y0, $x1, $y1, $c) { for ($y = $y0; $y -le $y1; $y++) { for ($x = $x0; $x -le $x1; $x++) { Px $x $y $c } } }
# A fixed scatter (the same each time).
function Grain($x, $y) { return (($x * 73) + ($y * 151) + (($x * $y) % 13) * 37) % 29 }

# ---- the seal: cinnabar, mottled, a double carved rim, worn at its edge, 武林 carved in it
$S0 = 6; $S1 = 49; $T0 = 12; $T1 = 55
Rect $S0 $T0 $S1 $T1 $CINNABAR
for ($y = $T0; $y -le $T1; $y++) { for ($x = $S0; $x -le $S1; $x++) {
  $grain = Grain $x $y
  if ($grain -eq 0 -or $grain -eq 7) { Px $x $y $CINNABAR_LIGHT } elseif ($grain -eq 13) { Px $x $y $CINNABAR_DARK } } }
Rect $S0 $T1 $S1 $T1 $CINNABAR_DARK; Rect $S1 $T0 $S1 $T1 $CINNABAR_DARK
foreach ($inset in @(2, 4)) {
  for ($i = $S0 + $inset; $i -le $S1 - $inset; $i++) { Px $i ($T0 + $inset) $SEAL_INK; Px $i ($T1 - $inset) $SEAL_INK }
  for ($i = $T0 + $inset; $i -le $T1 - $inset; $i++) { Px ($S0 + $inset) $i $SEAL_INK; Px ($S1 - $inset) $i $SEAL_INK }
}
# Worn: nicks out of the edge, the rims broken here and there.
foreach ($n in @(@(9, 12), @(10, 12), @(24, 12), @(41, 12), @(6, 21), @(6, 38), @(6, 39), @(49, 28), @(49, 45), @(15, 55), @(34, 55), @(35, 55), @(49, 15))) { Px $n[0] $n[1] $CLEAR }
foreach ($n in @(@(18, 14), @(37, 53), @(8, 27), @(47, 41), @(30, 16), @(16, 51), @(45, 22))) { Px $n[0] $n[1] $CINNABAR }
# 武林, one bit a pixel from SimSun's own bitmap glyphs, side by side, centred.
$glyphs = New-Object System.Drawing.Bitmap 40, 20
$gg = [System.Drawing.Graphics]::FromImage($glyphs)
$gg.TextRenderingHint = [System.Drawing.Text.TextRenderingHint]::SingleBitPerPixelGridFit
$sealFont = New-Object System.Drawing.Font 'SimSun', 16, ([System.Drawing.FontStyle]::Bold), ([System.Drawing.GraphicsUnit]::Pixel)
$gg.DrawString([string][char]0x6B66 + [char]0x6797, $sealFont, [System.Drawing.Brushes]::Black, 0, 1)
$gg.Dispose()
$minX = 99; $maxX = -1; $minY = 99; $maxY = -1
for ($y = 0; $y -lt 20; $y++) { for ($x = 0; $x -lt 40; $x++) { if ($glyphs.GetPixel($x, $y).A -gt 128) { $minX = [Math]::Min($minX, $x); $maxX = [Math]::Max($maxX, $x); $minY = [Math]::Min($minY, $y); $maxY = [Math]::Max($maxY, $y) } } }
$ox = [int](($S0 + $S1 + 1 - ($maxX - $minX + 1)) / 2) - $minX; $oy = [int](($T0 + $T1 + 1 - ($maxY - $minY + 1)) / 2) - $minY
for ($y = 0; $y -lt 20; $y++) { for ($x = 0; $x -lt 40; $x++) { if ($glyphs.GetPixel($x, $y).A -gt 128) {
  # A carved stroke: here and there the stamp did not take.
  if ((Grain ($x + $ox) ($y + $oy)) -eq 5) { continue }
  Px ($x + $ox) ($y + $oy) $SEAL_INK } } }

# ---- MURIMBLOCK: 5 x 7 letters drawn twice as large, ivory warming to gold, an ink ring, a shadow
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
$TX = 60; $TY = 16; $SIZE = 2; $STEP = 12
$fill = New-Object 'bool[,]' $W, $H
$text = 'MURIMBLOCK'
for ($n = 0; $n -lt $text.Length; $n++) {
  $rows = $FONT[[string]$text[$n]]
  for ($r = 0; $r -lt 7; $r++) { for ($c = 0; $c -lt 5; $c++) { if ($rows[$r][$c] -eq '1') {
    for ($dy = 0; $dy -lt $SIZE; $dy++) { for ($dx = 0; $dx -lt $SIZE; $dx++) { $fill[($TX + $n * $STEP + $c * $SIZE + $dx), ($TY + $r * $SIZE + $dy)] = $true } } } } }
}
for ($y = 0; $y -lt $H - 2; $y++) { for ($x = 0; $x -lt $W - 2; $x++) { if ($fill[$x, $y]) { Px ($x + 2) ($y + 2) $SHADOW } } }
for ($y = 1; $y -lt $H - 1; $y++) { for ($x = 1; $x -lt $W - 1; $x++) {
  if (-not $fill[$x, $y] -and ($fill[($x - 1), $y] -or $fill[($x + 1), $y] -or $fill[$x, ($y - 1)] -or $fill[$x, ($y + 1)] -or $fill[($x - 1), ($y - 1)] -or $fill[($x + 1), ($y + 1)] -or $fill[($x + 1), ($y - 1)] -or $fill[($x - 1), ($y + 1)])) { Px $x $y $INK } } }
for ($y = 0; $y -lt $H; $y++) { for ($x = 0; $x -lt $W; $x++) { if ($fill[$x, $y]) {
  $row = $y - $TY
  $step = if (-not $fill[$x, ($y - 1)]) { 0 } else { [Math]::Min(13, [Math]::Max(1, $row)) }
  # The right and lower edges of each stroke a step darker: a little relief.
  if (-not $fill[($x + 1), $y] -or -not $fill[$x, ($y + 1)]) { $step = [Math]::Min(13, $step + 3) }
  Px $x $y $TONE[$step] } } }

# ---- the jian beneath
$BY = 46
# Pommel: a bronze ring.
Rect 60 ($BY - 1) 62 ($BY + 2) $BRONZE; Px 61 $BY $INK; Px 61 ($BY + 1) $INK; Px 60 ($BY + 2) $BRONZE_DARK; Px 62 ($BY + 2) $BRONZE_DARK
foreach ($x in 59, 63) { Px $x ($BY - 1) $INK; Px $x $BY $INK; Px $x ($BY + 1) $INK; Px $x ($BY + 2) $INK }
foreach ($x in 60..62) { Px $x ($BY - 2) $INK; Px $x ($BY + 3) $INK }
# Grip: red cord braided in diamonds.
for ($x = 64; $x -le 74; $x++) {
  Px $x ($BY - 2) $INK; Px $x ($BY + 3) $INK
  for ($y = $BY - 1; $y -le $BY + 2; $y++) { Px $x $y $(if ((($x + $y) % 3) -eq 0) { $GRIP_LIGHT } elseif ((($x - $y + 30) % 3) -eq 0) { $CINNABAR_DARK } else { $GRIP }) }
}
# Guard: bronze, wider than the grip, a jade stone at its heart.
Rect 75 ($BY - 4) 78 ($BY + 5) $BRONZE
Rect 78 ($BY - 4) 78 ($BY + 5) $BRONZE_DARK; Px 75 ($BY - 4) $BRONZE_DARK; Px 75 ($BY + 5) $BRONZE_DARK
Px 76 $BY $JADE; Px 77 $BY $JADE_LIGHT; Px 76 ($BY + 1) $JADE; Px 77 ($BY + 1) $JADE
for ($y = $BY - 5; $y -le $BY + 6; $y++) { Px 74 $y $INK; Px 79 $y $INK }; foreach ($x in 75..78) { Px $x ($BY - 5) $INK; Px $x ($BY + 6) $INK }
# Blade: a bright ridge, the steel, a darker edge, ink round it, narrowing to its point.
$END = 204
for ($x = 80; $x -le $END; $x++) {
  $taper = [Math]::Max(0, $x - ($END - 7))
  $top = $BY - 1 + [int][Math]::Floor($taper / 3); $bottom = $BY + 2 - [int][Math]::Floor($taper / 3)
  Px $x ($top - 1) $INK; Px $x ($bottom + 1) $INK
  for ($y = $top; $y -le $bottom; $y++) { Px $x $y $(if ($y -eq $BY) { $STEEL_RIDGE } elseif ($y -lt $BY) { $STEEL } else { $STEEL_DARK }) }
}
Px ($END + 1) $BY $INK; Px ($END + 1) ($BY + 1) $INK
# A glint near the point.
Px 194 ($BY - 4) $WHITE; Px 193 ($BY - 3) $WHITE; Px 194 ($BY - 3) $WHITE; Px 195 ($BY - 3) $WHITE; Px 194 ($BY - 2) $WHITE; Px 194 ($BY - 1) $WHITE
# The tassel: a red bead, then three jade strands streaming back and down.
Px 58 ($BY + 1) $INK; Px 57 ($BY + 1) $CINNABAR; Px 57 ($BY + 2) $CINNABAR; Px 56 ($BY + 2) $CINNABAR_DARK
$strands = @(@(@(56, 3), @(55, 4), @(55, 5), @(54, 6), @(53, 7), @(53, 8), @(52, 9)), @(@(57, 3), @(57, 4), @(56, 5), @(56, 6), @(55, 7), @(55, 8), @(54, 9), @(54, 10)), @(@(58, 3), @(58, 4), @(58, 5), @(57, 6), @(57, 7), @(57, 8)))
$k = 0
foreach ($strand in $strands) { foreach ($p in $strand) { Px $p[0] ($BY + $p[1]) $(if ((($p[1] + $k) % 3) -eq 0) { $JADE_LIGHT } else { $JADE }) }; $k++ }

# ---- four times larger, every pixel a sharp square
$out = New-Object System.Drawing.Bitmap ($W * $SCALE), ($H * $SCALE)
$g2 = [System.Drawing.Graphics]::FromImage($out)
$g2.InterpolationMode = [System.Drawing.Drawing2D.InterpolationMode]::NearestNeighbor
$g2.PixelOffsetMode = [System.Drawing.Drawing2D.PixelOffsetMode]::Half
$g2.DrawImage($img, 0, 0, $W * $SCALE, $H * $SCALE)
$g2.Dispose()
$path = Join-Path $root 'src\main\resources\murimblock_logo.png'
$out.Save($path, [System.Drawing.Imaging.ImageFormat]::Png)
$out.Dispose(); $img.Dispose(); $glyphs.Dispose()
Write-Output "wrote $path"
