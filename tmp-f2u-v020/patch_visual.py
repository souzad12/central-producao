from pathlib import Path
import sys
root = Path(sys.argv[1])
base = root / "F2UVisualAndroid/app/src/main/java/com/f2u/visual"
p = base / "Theme.kt"
p.write_text('''package com.f2u.visual\n\nimport android.os.Build\nimport androidx.compose.foundation.isSystemInDarkTheme\nimport androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.darkColorScheme\nimport androidx.compose.material3.dynamicDarkColorScheme\nimport androidx.compose.material3.dynamicLightColorScheme\nimport androidx.compose.material3.lightColorScheme\nimport androidx.compose.runtime.Composable\nimport androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.platform.LocalContext\n\n@Composable\nfun F2UVisualTheme(\n    oled: Boolean,\n    content: @Composable () -> Unit\n) {\n    val dark = isSystemInDarkTheme()\n    val context = LocalContext.current\n    var scheme = if (Build.VERSION.SDK_INT >= 31) {\n        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)\n    } else if (dark) {\n        darkColorScheme(primary = Color(0xFFC9B5FF), secondary = Color(0xFFFFB0CF))\n    } else {\n        lightColorScheme(primary = Color(0xFF6746A1), secondary = Color(0xFF8B3E66))\n    }\n    if (dark && oled) {\n        scheme = scheme.copy(\n            background = Color.Black,\n            surface = Color.Black,\n            surfaceContainer = Color(0xFF090909),\n            surfaceContainerLow = Color(0xFF050505),\n            surfaceContainerHigh = Color(0xFF111111)\n        )\n    }\n    MaterialTheme(colorScheme = scheme, content = content)\n}\n''')

p = base / "SafFiles.kt"
s = p.read_text()
lines = s.splitlines()
seen_mime = False
out = []
for line in lines:
    if "fun mime(c:Context,u:Uri)" in line:
        if seen_mime:
            continue
        seen_mime = True
    out.append(line)
p.write_text("\n".join(out) + "\n")

p = base / "SecretStore.kt"
s = p.read_text().replace('return""', 'return ""')
p.write_text(s)
print("patched Visual theme, SafFiles and SecretStore")
