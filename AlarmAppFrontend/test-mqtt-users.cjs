
const mqtt = require('mqtt');

// Configuration matching the backend
const useSsl = false;
const brokerHost = 'localhost';
const brokerPort = '9001';
const topic = 'JF/Alarm';

// Test different users
const users = [
    { username: 'admin', password: '' },
    { username: 'reader', password: '' },
    { username: 'alarm', password: '' },
    { username: 'admin', password: 'admin' },
    { username: 'reader', password: 'reader' },
    { username: 'alarm', password: 'alarm' },
    { username: 'admin', password: 'password' },
    { username: 'alarm', password: 'password' }
];

// Generiere die vollständige Broker-URL basierend auf SSL-Einstellung
function getBrokerUrl() {
    const protocol = useSsl ? "wss://" : "ws://";
    return `${protocol}${brokerHost}:${brokerPort}`;
}

const message = `Test###Musterstraße 123###Testnachricht`;

async function testUser(user) {
    return new Promise((resolve) => {
        const mqttOptions = {
            username: user.username,
            password: user.password,
            clientId: `testapp_${user.username}_${Math.random().toString(16).slice(2, 10)}`,
            clean: true,
            reconnectPeriod: 0,
            connectTimeout: 5000,
            keepalive: 60,
            rejectUnauthorized: false,
        };

        const client = mqtt.connect(getBrokerUrl(), mqttOptions);

        const timeoutId = setTimeout(() => {
            client.end();
            resolve({ user, success: false, error: "Timeout" });
        }, 5000);

        client.on("connect", () => {
            clearTimeout(timeoutId);
            client.end();
            resolve({ user, success: true });
        });

        client.on("error", (err) => {
            clearTimeout(timeoutId);
            client.end();
            resolve({ user, success: false, error: err.code || err.message || err });
        });
    });
}

async function testAllUsers() {
    console.log('Testing MQTT connection to:', getBrokerUrl());
    console.log('Testing users:');
    console.log('---------------');
    
    for (let i = 0; i < users.length; i++) {
        const user = users[i];
        process.stdout.write(`Testing ${user.username}:${user.password}... `);
        
        const result = await testUser(user);
        
        if (result.success) {
            console.log('✅ Success');
        } else {
            console.log(`❌ ${result.error}`);
        }
    }
}

testAllUsers().catch(err => {
    console.error('Error in test:', err);
    process.exit(1);
});
