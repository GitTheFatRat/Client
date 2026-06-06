# TriggerBot - JVM Arguments Reference

## Immediate Fix (Works NOW)
Use this to bypass the screen check issue immediately:

```
-Dtriggerbot.skip.screen.check
```

### How to Apply:
1. **Minecraft Launcher (Native)**:
   - Settings → Java Edition → Edit Installation
   - In JVM Arguments, add: `-Dtriggerbot.skip.screen.check`

2. **MultiMC/PolyMC**:
   - Instance settings → Java
   - In JVM Arguments, add: `-Dtriggerbot.skip.screen.check`

3. **Curse Forge Launcher**:
   - Instances → Settings → Java Settings
   - Additional JVM Arguments: `-Dtriggerbot.skip.screen.check`

4. **Command Line**:
   ```bash
   java -jar launcher.jar -Dtriggerbot.skip.screen.check
   ```

## Recommended JVM Arguments for Best Performance

### Minimal (Just bypass screen check):
```
-Dtriggerbot.skip.screen.check
```

### Recommended (With debugging):
```
-Dtriggerbot.skip.screen.check -Dtriggerbot.debug
```

### Verbose (Full diagnostics):
```
-Dtriggerbot.skip.screen.check -Dtriggerbot.debug -Dtriggerbot.verbose.screen
```

### With Minecraft performance tweaks:
```
-Xmx4G -Xms2G -Dtriggerbot.skip.screen.check -Dtriggerbot.debug -XX:+UseG1GC -XX:MaxGCPauseMillis=130 -XX:+ParallelRefProcEnabled -XX:+AlwaysPreTouch
```

## What Each Property Does

| Property | Value | Effect |
|----------|-------|--------|
| `triggerbot.skip.screen.check` | (any) | Disables screen detection check - allows attacks even if menu open |
| `triggerbot.debug` | (any) | Enables debug logging output to console |
| `triggerbot.verbose.screen` | (any) | Very verbose screen check logging (shows every check) |

## Rebuilding with Enhanced Detection

If you want to try the enhanced screen field detection instead of bypassing:

1. Run Maven rebuild:
   ```bash
   cd C:\Users\Administrator\Desktop\bruh-master
   mvn clean package
   ```

2. Run normally (without the bypass property):
   ```
   -Dtriggerbot.debug
   ```

3. Check console for messages like:
   ```
   [SentaiHex] Screen field cached: currentScreen
   ```

## Troubleshooting

**Still seeing "Blocked: screen open"?**
- Make sure you're using the property correctly
- Restart Minecraft completely
- Check that the property is in JVM Arguments, NOT game arguments

**Seeing errors in console?**
- With verbose mode enabled, check what field was found
- The console will show: `[SentaiHex] Screen field cached: <fieldname>`
- If you see "Failed to find screen field", the field names may have changed for your MC version

**Game not starting?**
- Remove the verbose properties, keep only: `-Dtriggerbot.skip.screen.check`
- Make sure there are no typos in the property names
- Don't use quotes around the property names

## Example Complete Setup

**For Java 8-16:**
```
-Xmx4G -Xms2G -Dtriggerbot.skip.screen.check -Dtriggerbot.debug
```

**For Java 17+:**
```
-Xmx4G -Xms2G --add-modules=jdk.unsupported --add-opens java.base/sun.nio.ch=ALL-UNNAMED -Dtriggerbot.skip.screen.check -Dtriggerbot.debug
```

## Need More Help?

1. **Console output**: Enable `-Dtriggerbot.debug` and check console
2. **File location**: Config is at `~/.sentaihex/triggerbot.txt`
3. **Rebuild**: Use `mvn clean package` to rebuild with new field detection
