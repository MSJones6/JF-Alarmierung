import React, { useState } from "react";
import mqtt, { MqttClient } from "mqtt";

interface AlarmMessage {
  date: string;
  time: string;
  alarmstichwort: string;
  info: string;
  sentAt: string;
}

type StatusType = "idle" | "sending" | "success" | "error";

export default function App() {
  const [brokerUrl, setBrokerUrl] = useState<string>("ws://localhost:9001");
  const [topic, setTopic] = useState<string>("alarm/stichwort");
  const [alarmstichwort, setAlarmstichwort] = useState<string>("");
  const [info, setInfo] = useState<string>("");
  const [status, setStatus] = useState<string>("");
  const [statusType, setStatusType] = useState<StatusType>("idle");

  const sendMessage = (): void => {
    setStatusType("sending");
    setStatus("Nachricht wird gesendet... ⏳");

    try {
      const client: MqttClient = mqtt.connect(brokerUrl);

      client.on("connect", () => {
        const now = new Date();
        const payload: AlarmMessage = {
          date: now.toISOString().split("T")[0],
          time: now.toTimeString().slice(0, 5),
          alarmstichwort,
          info,
          sentAt: now.toISOString(),
        };

        client.publish(topic, JSON.stringify(payload), { qos: 1 }, (err?: Error) => {
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

      client.on("error", (err) => {
        setStatusType("error");
        setStatus("MQTT Verbindungsfehler ❌: " + err.message);
      });
    } catch (err: any) {
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

        {/* Broker URL */}
        <div className="mb-4 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">Broker URL</label>
          <input
            className="border border-gray-300 p-3 rounded-lg focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={brokerUrl}
            onChange={(e) => setBrokerUrl(e.target.value)}
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

        {/* Alarmstichwort */}
        <div className="mb-4 flex flex-col">
          <label className="mb-1 font-medium text-gray-700">Alarmstichwort</label>
          <input
            className="border border-gray-300 p-3 rounded-lg focus:ring-2 focus:ring-blue-400 focus:outline-none"
            value={alarmstichwort}
            onChange={(e) => setAlarmstichwort(e.target.value)}
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
