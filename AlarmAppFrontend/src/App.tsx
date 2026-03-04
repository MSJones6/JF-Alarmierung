import React, { useState } from "react";
import mqtt, { MqttClient } from "mqtt";

type StatusType = "idle" | "sending" | "success" | "error";

export default function App() {
  const [useSsl, setUseSsl] = useState<boolean>(false);
  const [brokerHost, setBrokerHost] = useState<string>("localhost");
  const [brokerPort, setBrokerPort] = useState<string>("9001");
  const [brokerPath, setBrokerPath] = useState<string>("/mqtt");
  const [user, setUser] = useState<string>("alarm");
  const [password, setPassword] = useState<string>("alarm");
  const [topic, setTopic] = useState<string>("JF/Alarm");
  const [alarmstichwort, setAlarmstichwort] = useState<string>("Brand");
  const [adresse, setAdresse] = useState<string>("Musterstraße 123, 10115 Berlin");
  const [info, setInfo] = useState<string>("Testalarm - bitte ignorieren");
  const [status, setStatus] = useState<string>("");
  const [statusType, setStatusType] = useState<StatusType>("idle");

  // Generiere die vollständige Broker-URL basierend auf SSL-Einstellung
  const getBrokerUrl = (): string => {
    const protocol = useSsl ? "wss://" : "ws://";
    let url = `${protocol}${brokerHost}:${brokerPort}`;
    if (brokerPath) {
      // Stelle sicher, dass der Pfad mit "/" beginnt
      const normalizedPath = brokerPath.startsWith("/") ? brokerPath : `/${brokerPath}`;
      url += normalizedPath;
    }
    return url;
  };

  const sendMessage = (): void => {
    console.log("sendMessage function called");
    // Validate required fields
    if (!alarmstichwort || !adresse) {
      setStatusType("error");
      setStatus("Bitte füllen Sie mindestens Alarmstichwort und Adresse aus ❌");
      return;
    }

    setStatusType("sending");
    setStatus("Verbindung zum Broker wird hergestellt... ⏳");

    const message = `${alarmstichwort}###${adresse}###${info}`;

    const mqttOptions = {
      username: user || undefined,
      password: password || undefined,
      clientId: "alarmapp_" + Math.random().toString(16).slice(2, 10),
      clean: true,
      reconnectPeriod: 0,
      connectTimeout: 10000,
      keepalive: 60,
      // SSL/TLS options for secure connections
      rejectUnauthorized: false, // Disable strict SSL verification for testing
    };

    console.log("Connecting to MQTT broker:", getBrokerUrl());
    console.log("MQTT Options:", mqttOptions);

    try {
      const client: MqttClient = mqtt.connect(getBrokerUrl(), mqttOptions);

      // Connection timeout handler
      const timeoutId = setTimeout(() => {
        if (client.connected) {
          console.log("Client ist bereits verbunden");
        } else {
          setStatusType("error");
          setStatus("Verbindungstimeout ❌ - Broker antwortet nicht (Port: " + brokerPort + ")");
          client.end();
        }
      }, 10000);

      client.on("connect", () => {
        clearTimeout(timeoutId);
        setStatus("Nachricht wird gesendet... ⏳");

        client.publish(topic, message, { qos: 1 }, (err?: Error) => {
          clearTimeout(timeoutId);
          if (err) {
            setStatusType("error");
            setStatus("Fehler beim Senden ❌: " + err.message);
          } else {
            setStatusType("success");
            setStatus("Nachricht erfolgreich gesendet ✅");
          }
          client.end();
        });
      });

      client.on("error", (err: any) => {
        clearTimeout(timeoutId);
        console.error("MQTT Connection Error:", err);
        setStatusType("error");
        setStatus("MQTT Fehler ❌: " + (err.message || err));
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
    } catch (err: any) {
      console.error("Connection Exception:", err);
      setStatusType("error");
      setStatus("Verbindungsfehler ❌: " + err.message);
    }
  };

  const getStatusClasses = () => {
    switch (statusType) {
      case "success":
        return "bg-green-100 text-green-800 border-green-300";
      case "error":
        return "bg-red-100 text-red-800 border-red-300";
      case "sending":
        return "bg-yellow-100 text-yellow-800 border-yellow-300";
      default:
        return "";
    }
  };

  return (
    <div className="min-h-screen bg-gray-100 flex items-center justify-center p-4">
      <div className="bg-white shadow-2xl rounded-3xl p-8 w-full max-w-lg">
        <h1 className="text-3xl font-bold text-center text-gray-800 mb-6">
          Alarm Nachricht senden
        </h1>

        {/* SSL-Auswahl */}
        <div className="mb-4 flex items-center">
          <input
            type="checkbox"
            id="useSsl"
            className="w-5 h-5 text-blue-600 rounded focus:ring-blue-400"
            checked={useSsl}
            onChange={(e) => setUseSsl(e.target.checked)}
          />
          <label htmlFor="useSsl" className="ml-2 font-medium text-gray-700">
            SSL-Verschlüsselung (verschlüsselt)
          </label>
        </div>

        {/* Broker Host */}
        <div className="mb-4 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">Hostname</label>
          <input
            className="border border-gray-300 p-3 rounded-lg focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={brokerHost}
            onChange={(e) => setBrokerHost(e.target.value)}
            placeholder="z.B. localhost"
          />
        </div>

        {/* Broker Port */}
        <div className="mb-4 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">Port</label>
          <input
            type="number"
            className="border border-gray-300 p-3 rounded-lg focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={brokerPort}
            onChange={(e) => setBrokerPort(e.target.value)}
            placeholder="z.B. 9001"
            min="1"
            max="65535"
          />
        </div>

        {/* Broker Path */}
        <div className="mb-4 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">Pfad</label>
          <input
            className="border border-gray-300 p-3 rounded-lg focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={brokerPath}
            onChange={(e) => setBrokerPath(e.target.value)}
            placeholder="z.B. /mqtt"
          />
        </div>

        {/* Topic */}
        <div className="mb-4 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">Topic</label>
          <input
            className="border border-gray-300 p-3 rounded-lg focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={topic}
            onChange={(e) => setTopic(e.target.value)}
          />
        </div>

        {/* User */}
        <div className="mb-4 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">User</label>
          <input
            className="border border-gray-300 p-3 rounded-lg focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={user}
            onChange={(e) => setUser(e.target.value)}
          />
        </div>

        {/* Password */}
        <div className="mb-4 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">Password</label>
          <input
            type="password"
            className="border border-gray-300 p-3 rounded-lg focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
          />
        </div>

        {/* Alarmstichwort */}
        <div className="mb-4 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">Alarmstichwort</label>
          <input
            className="border border-gray-300 p-3 rounded-lg focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={alarmstichwort}
            onChange={(e) => setAlarmstichwort(e.target.value)}
          />
        </div>

        {/* Adresse */}
        <div className="mb-4 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">Adresse</label>
          <input
            className="border border-gray-300 p-3 rounded-lg focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={adresse}
            onChange={(e) => setAdresse(e.target.value)}
          />
        </div>

        {/* Weitere Informationen */}
        <div className="mb-6 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">Weitere Informationen</label>
          <textarea
            className="border border-gray-300 p-3 rounded-lg h-48 resize-y focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={info}
            onChange={(e) => setInfo(e.target.value)}
          />
        </div>

        {/* Senden Button */}
        <button
          className="bg-blue-600 text-white font-semibold py-3 px-6 w-full rounded-xl hover:bg-blue-700 transition-colors duration-200 shadow-md"
          onClick={sendMessage}
        >
          Nachricht senden
        </button>

        {/* Status Box */}
        {status && (
          <div
            className={`mt-6 p-4 rounded-lg border ${getStatusClasses()} text-center font-medium transition-all duration-300`}
          >
            {status}
          </div>
        )}
      </div>
    </div>
  );
}
