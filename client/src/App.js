import { useEffect, useState } from "react";
import "./index.css";

function App() {

  const [wedding, setWedding] = useState(null);

  useEffect(() => {
    fetch("http://localhost:8080/api/wedding/info")
      .then(res => res.json())
      .then(data => setWedding(data));
  }, []);

  if (!wedding) {
    return <h2>Loading...</h2>;
  }

  return (
    <div className="container">

      <h1>{wedding.coupleNames}</h1>

      <p>
        <strong>Date:</strong> {wedding.weddingDate}
      </p>

      <p>
        <strong>City:</strong> {wedding.city}
      </p>

      <h2>Wedding Events</h2>

      <ul>
        {wedding.events.map((event, index) => (
          <li key={index}>
            <strong>{event.name}</strong>
            <div>{event.location}</div>
            <div>{event.address}</div>
            <div>{event.startTime}</div>
          </li>
        ))}
      </ul>

    </div>
  );
}

export default App;