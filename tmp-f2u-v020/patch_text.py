from pathlib import Path
import sys
root = Path(sys.argv[1])
p = root / "F2UTextAndroid/app/src/main/java/com/f2u/text/MainActivity.kt"
s = p.read_text()
s = s.replace(
'var busy by remember(project.id){mutableStateOf(false)}; var status by remember(project.id){mutableStateOf("Pronto.")}; var showVoices by remember{mutableStateOf<List<Voice>?>(null)}',
'var busy by remember(project.id){mutableStateOf(false)}; var status by remember(project.id){mutableStateOf("Pronto.")}; val showVoicesState = remember { mutableStateOf<List<Voice>?>(null) }'
)
s = s.replace(
'runCatching{withContext(Dispatchers.IO){ElevenLabsClient.listVoices(key)}}.onSuccess{showVoices=it;status="${it.size} vozes carregadas."}.onFailure{status="Erro: ${it.message}"};busy=false',
'''try {\n                        val voices = withContext(Dispatchers.IO) { ElevenLabsClient.listVoices(key) }\n                        showVoicesState.value = voices\n                        status = "${voices.size} vozes carregadas."\n                    } catch (e: Exception) {\n                        status = "Erro: ${e.message}"\n                    } finally {\n                        busy = false\n                    }'''
)
s = s.replace(
'showVoices?.let { voices -> AlertDialog(onDismissRequest={showVoices=null},title={Text("Escolha a voz")},text={Column(Modifier.heightIn(max=420.dp).verticalScroll(rememberScrollState())){voices.forEach{v->TextButton(onClick={settings.voiceId=v.id;status="Voz selecionada: ${v.name}";showVoices=null},Modifier.fillMaxWidth()){Text(v.name+"\\n"+v.id)}}}},confirmButton={}) }',
'''showVoicesState.value?.let { voices ->\n        AlertDialog(\n            onDismissRequest = { showVoicesState.value = null },\n            title = { Text("Escolha a voz") },\n            text = {\n                Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {\n                    voices.forEach { v ->\n                        TextButton(\n                            onClick = {\n                                settings.voiceId = v.id\n                                status = "Voz selecionada: ${v.name}"\n                                showVoicesState.value = null\n                            },\n                            modifier = Modifier.fillMaxWidth()\n                        ) { Text(v.name + "\\n" + v.id) }\n                    }\n                }\n            },\n            confirmButton = {}\n        )\n    }'''
)
s = s.replace(
'Row(Modifier.fillMaxWidth(),horizontalArrangement=Arrangement.spacedBy(5.dp)){stages.forEachIndexed{i,s->Surface(modifier=Modifier.weight(1f),shape=MaterialTheme.shapes.small,color=if(i<=current)MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant){Text(if(i<current)"✓ $s" else s,Modifier.padding(vertical=7.dp,horizontal=4.dp),style=MaterialTheme.typography.labelSmall,color=if(i<=current)MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,maxLines=1)}}}',
'''Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {\n        stages.forEachIndexed { i, s ->\n            Surface(\n                modifier = Modifier.width(72.dp),\n                shape = MaterialTheme.shapes.small,\n                color = if (i <= current) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant\n            ) {\n                Text(\n                    if (i < current) "✓ $s" else s,\n                    Modifier.padding(vertical = 7.dp, horizontal = 4.dp),\n                    style = MaterialTheme.typography.labelSmall,\n                    color = if (i <= current) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,\n                    maxLines = 1\n                )\n            }\n        }\n    }'''
)
s = s.replace(
'            },Modifier.fillMaxWidth()){Icon(Icons.Default.AutoFixHigh,null);Spacer(Modifier.width(8.dp));Text("Gerar PROMPTS.txt")}',
'            }, modifier=Modifier.fillMaxWidth()){Icon(Icons.Default.AutoFixHigh,null);Spacer(Modifier.width(8.dp));Text("Gerar PROMPTS.txt")}'
)
p.write_text(s)
print("patched", p)

v = root / "F2UVisualAndroid/app/src/main/java/com/f2u/visual/Theme.kt"
v.write_text('''package com.f2u.visual\n\nimport android.os.Build\nimport androidx.compose.foundation.isSystemInDarkTheme\nimport androidx.compose.material3.MaterialTheme\nimport androidx.compose.material3.darkColorScheme\nimport androidx.compose.material3.dynamicDarkColorScheme\nimport androidx.compose.material3.dynamicLightColorScheme\nimport androidx.compose.material3.lightColorScheme\nimport androidx.compose.runtime.Composable\nimport androidx.compose.ui.graphics.Color\nimport androidx.compose.ui.platform.LocalContext\n\n@Composable\nfun F2UVisualTheme(\n    oled: Boolean,\n    content: @Composable () -> Unit\n) {\n    val dark = isSystemInDarkTheme()\n    val context = LocalContext.current\n    var scheme = if (Build.VERSION.SDK_INT >= 31) {\n        if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)\n    } else if (dark) {\n        darkColorScheme(primary = Color(0xFFC9B5FF), secondary = Color(0xFFFFB0CF))\n    } else {\n        lightColorScheme(primary = Color(0xFF6746A1), secondary = Color(0xFF8B3E66))\n    }\n    if (dark && oled) {\n        scheme = scheme.copy(\n            background = Color.Black,\n            surface = Color.Black,\n            surfaceContainer = Color(0xFF090909),\n            surfaceContainerLow = Color(0xFF050505),\n            surfaceContainerHigh = Color(0xFF111111)\n        )\n    }\n    MaterialTheme(colorScheme = scheme, content = content)\n}\n''')
print("patched", v)
