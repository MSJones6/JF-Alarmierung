
const mqtt = require('mqtt');

// Configuration matching the frontend
const useSsl = true;
const brokerHost = 'localhost';
const brokerPort = '8084';
const topic = 'JF/Alarm';
const user = '';
const password = '';
const alarmstichwort = 'Brand';
const adresse = 'Musterstraße 123, 10115 Berlin';
const info = 'Testalarm - bitte ignorieren';

// Generiere die vollständige Broker-URL basierend auf SSL-Einstellung
function getBrokerUrl() {
    const protocol = useSsl ? "wss://" : "ws://";
    return `${protocol}${brokerHost}:${brokerPort}`;
}

const message = `${alarmstichwort}###${adresse}###${info}`;

const mqttOptions = {
    username: user || undefined,
    password: password || undefined,
    clientId: "alarmapp_" + Math.random().toString(16).slice(2, 10),
    clean: true,
    reconnectPeriod: 0,
    connectTimeout: 10000,
    keepalive: 60,
    rejectUnauthorized: false,
};

console.log('Testing MQTT connection to:', getBrokerUrl());
console.log('MQTT Options:', mqttOptions);

try {
    const client = mqtt.connect(getBrokerUrl(), mqttOptions);

    const timeoutId = setTimeout(() => {
        if (client.connected) {
            console.log("Client ist bereits verbunden");
        } else {
            console.error("Verbindungstimeout ❌ - Broker antwortet nicht");
            client.end();
            process.exit(1);
        }
    }, 10000);

    client.on("connect", () => {
        clearTimeout(timeoutId);
        console.log("Nachricht wird gesendet... ⏳");

        client.publish(topic, message, { qos: 1 }, (err) => {
            clearTimeout(timeoutId);
            if (err) {
                console.error("Fehler beim Senden ❌:", err.message);
            } else {
                console.log("Nachricht erfolgreich gesendet ✅");
            }
            client.end();
            process.exit(err ? 1 : 0);
        });
    });

    client.on("error", (err) => {
        clearTimeout(timeoutId);
        console.error("MQTT Fehler ❌:", err.message || err);
        client.end();
        process.exit(1);
    });

    client.on("close", () => {
        console.log("MQTT Connection closed");
    });

    client.on("offline", () => {
        console.log("MQTT Client offline");
    });

    client.on("reconnect", () => {
        console.log("MQTT Client reconnecting");
    });
} catch (err) {
    console.error("Connection Exception ❌:", err);
    process.exit(1);
}
