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
p.write_text(s)
print("patched", p)
