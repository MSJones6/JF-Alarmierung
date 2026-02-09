# React + TypeScript + Vite

This template provides a minimal setup to get React working in Vite with HMR and some ESLint rules.

Currently, two official plugins are available:

- [@vitejs/plugin-react](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react) uses [Babel](https://babeljs.io/) for Fast Refresh
- [@vitejs/plugin-react-swc](https://github.com/vitejs/vite-plugin-react/blob/main/packages/plugin-react-swc) uses [SWC](https://swc.rs/) for Fast Refresh

## Expanding the ESLint configuration

If you are developing a production application, we recommend updating the configuration to enable type-aware lint rules:

```js
export default tseslint.config([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...

      // Remove tseslint.configs.recommended and replace with this
      ...tseslint.configs.recommendedTypeChecked,
      // Alternatively, use this for stricter rules
      ...tseslint.configs.strictTypeChecked,
      // Optionally, add this for stylistic rules
      ...tseslint.configs.stylisticTypeChecked,

      // Other configs...
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```

You can also install [eslint-plugin-react-x](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-x) and [eslint-plugin-react-dom](https://github.com/Rel1cx/eslint-react/tree/main/packages/plugins/eslint-plugin-react-dom) for React-specific lint rules:

```js
// eslint.config.js
import reactX from 'eslint-plugin-react-x'
import reactDom from 'eslint-plugin-react-dom'

export default tseslint.config([
  globalIgnores(['dist']),
  {
    files: ['**/*.{ts,tsx}'],
    extends: [
      // Other configs...
      // Enable lint rules for React
      reactX.configs['recommended-typescript'],
      // Enable lint rules for React DOM
      reactDom.configs.recommended,
    ],
    languageOptions: {
      parserOptions: {
        project: ['./tsconfig.node.json', './tsconfig.app.json'],
        tsconfigRootDir: import.meta.dirname,
      },
      // other options...
    },
  },
])
```

# Handle app
## Add date to allow user to access topic
Go to the system, where docker is running with the mosquitto server and use the followning commands:

### Add user authentication
```
docker exec -it <container_name> mosquitto_passwd -b /mosquitto/config/passwordfile.conf <USERNAME> <PASSWORD>
```

### Add user autorization
#### Add access rights
Edit mosquitto/config/aclfile.conf.

Add a block like the following:
```
user <USERNAME>
topic <read|write|readwrite> <TOPIC>
```

### Remove user authentication
docker exec -it <container_name> mosquitto_passwd -D /mosquitto/config/passwordfile.conf <USERNAME>

### Restart the mosquitto server
```
docker restart <container_name>
```

## QR-Code für Android App generieren
Um einen QR-Code für die Android App zu generieren, erstellen Sie einen JSON-String mit folgendem Format:

```json
{
  "originator": "MSJones JF Alarm App",
  "host": "mqtt.jf-alarm.example.com",
  "port": 8883,
  "username": "feuerwehr-alarm",
  "password": "secureToken123",
  "topic": "JF/Alarm"
}
```

### Felder:
| Feld | Beschreibung | Pflicht |
|------|--------------|---------|
| `originator` | Muss exakt "MSJones JF Alarm App" sein | ✓ |
| `host` | MQTT Server Hostname oder IP | ✓ |
| `port` | MQTT Port (Standard: 1883) | ✓ |
| `username` | Authentifizierung Benutzername | ✗ |
| `password` | Authentifizierung Passwort | ✗ |
| `topic` | MQTT Topic für Alarme | ✓ |

### QR-Code Generator Beispiel (JavaScript):
```javascript
function generateQrCode(settings) {
  const json = JSON.stringify({
    originator: "MSJones JF Alarm App",
    host: settings.host,
    port: settings.port,
    username: settings.username,
    password: settings.password,
    topic: settings.topic
  });
  
  // Verwenden Sie eine QR-Code Bibliothek wie qrcode.js
  return qrcode.toDataURL(json);
}
```

### Beispiel mit Online-Generatoren:
1. JSON-Objekt erstellen
2. JSON-String kopieren
3. In QR-Code Generator wie https://www.qr-code-generator.com/ einfügen
4. QR-Code herunterladen und verteilen
