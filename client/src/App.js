import { useEffect, useState } from "react";
import "./index.css";

function App() {
  const photos = ["/images/photo1.jpg","/images/photo2.jpg","/images/photo3.jpg"];
  const [wedding, setWedding] = useState(null);
  const [mainGuest, setMainGuest] = useState("");
  const [plusOne, setPlusOne] = useState("");
  const [submitted, setSubmitted] = useState(false);
  const [failed, setFailed] = useState(false);

  useEffect(() => {
    fetch("http://localhost:8080/api/info")
      .then(res => res.json())
      .then(data => setWedding(data));
  }, []);

  const submitRSVP = () => {
    fetch("http://localhost:8080/api/rsvp", {
      method: "POST",
      headers: {
        "Content-Type": "application/json"
      },
      body: JSON.stringify({
        mainGuestName: mainGuest,
        plusOneName: plusOne
      })
    })
    .then(res => {
      if (!res.ok) throw new Error();
      return res.json();
    })
    .then(() => setSubmitted(true))
    .catch(() => setFailed(true));
  };

  if (!wedding) {
    return <h2>Loading wedding data...</h2>;
  }

  return (
    <div className="container">
      <h1>{wedding.coupleNames}</h1>
      <p><strong>Date:</strong> {wedding.weddingDate}</p>
      <p><strong>City:</strong> {wedding.city}</p>

      <h2>Events</h2>
      <ul className="event-list">
        {wedding.events.map((event, index) => (
          <li key={index}>
            <strong>{event.name}</strong>
            <div>{event.location}</div>
            <div>{event.address}</div>
            <div>
              {new Date(event.startTime).toLocaleTimeString([], {
                hour: "2-digit",
                minute: "2-digit"
              })}
            </div>
            <br></br>
          </li>
        ))}
      </ul>

      <h2>RSVP</h2>
      {submitted ? (<p>Thank you for attending.</p>) : (
        <div className="rsvp-form">
          <input
            placeholder="e.g. John Doe"
            value={mainGuest}
            onChange={(e) => setMainGuest(e.target.value)}
          />
          <input
            placeholder="e.g. Jane Doe (optional)"
            value={plusOne}
            onChange={(e) => setPlusOne(e.target.value)}
          />
          <button onClick={submitRSVP}>Submit</button>
          {failed && <p>RSVP failed. Please try again later.</p>}
        </div>
      )}

      <h2>Gallery</h2>
      <div className="gallery-section">
        <p>Find and upload your photos here after the wedding.</p>
        <div className="gallery">
          {photos.map((photo, index) => (
            <img src={photo} alt={`${index + 1}`} className="gallery-img"/>
          ))}
        </div>
      </div>
    </div>
  );
}

export default App;