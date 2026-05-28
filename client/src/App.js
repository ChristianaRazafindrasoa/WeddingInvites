import { useEffect, useState } from "react";
import "./index.css";

function App() {
  const photos = ["/images/photo1.jpg","/images/photo2.jpg","/images/photo3.jpg"];
  const [wedding, setWedding] = useState(null);
  const [mainGuest, setMainGuest] = useState("");
  const [plusOne, setPlusOne] = useState("");
  const [response, setResponse] = useState(null);
  const [showPayments, setShowPayments] = useState(false);

  useEffect(() => {
    fetch("http://localhost:8080/api/info")
      .then(res => res.json())
      .then(data => setWedding(data));
  }, []);

  const submitRSVP = async () => {
    try {
      const response = await fetch("http://localhost:8080/api/rsvp", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          mainGuestName: mainGuest,
          plusOneName: plusOne,
        }),
      });
      setResponse(await response.json());
    } catch (err) { 
        setResponse({message: "RSVP failed. Please try again later.",});
    }
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
            <br></br>
            <strong>{event.name}</strong>
            <div>{event.location}</div>
            <div>{event.address}</div>
            <div>
              {new Date(event.startTime).toLocaleTimeString([], {
                hour: "2-digit",
                minute: "2-digit"
              })}
            </div>
          </li>
        ))}
      </ul>

      <h2>RSVP</h2>
      <div className="rsvp-form">
        <input
          placeholder="e.g. John Doe"
          value={mainGuest}
          onChange={(e) => setMainGuest(e.target.value)}/>
        <input
          placeholder="e.g. Jane Doe (optional)"
          value={plusOne}
          onChange={(e) => setPlusOne(e.target.value)}/>
        <button onClick={submitRSVP}>Submit</button>
        {response?.message && (<p>{response.message}</p>)}
      </div>

      <h2>Gallery</h2>
      <div className="gallery-section">
        <p>Find and upload photos here after the wedding.</p>
        <div className="gallery">
          {photos.map((photo, index) => (
            <img src={photo} alt={`${index + 1}`} className="gallery-img"/>
          ))}
        </div>
      </div>

      <h2>Registry</h2>
      <div className="registry">
        <p>
          Your presence is the greatest gift, <br></br>
          but if you'd like to contribute to our honeymoon fund, <br></br>
          you can do so below.
        </p>
        <button onClick={() => setShowPayments(!showPayments)}>Contribute</button>
        {showPayments && (
          <div>
            <div className="payment-options">
            <div className="payment-card">
              <img src="/images/venmo.jpg" alt="Venmo QR" />
              <p>Nick</p>
            </div>
            <div className="payment-card">
              <img src="/images/zelle.jpg" alt="Zelle QR" />
              <p>Christiana</p>
            </div>
          </div>
          <p>Thank you in advance for contributing.</p>
        </div>    
        )}
      </div>
    </div>
  );
}

export default App;