import { useEffect, useState } from "react";
import "./index.css";
import AdminPanel from "./Admin";

function Invitation() {
  const photos = ["/images/photo1.jpg", "/images/photo2.jpg", "/images/photo3.jpg"];
  const [wedding, setWedding] = useState(null);
  const [mainGuest, setMainGuest] = useState("");
  const [plusOne, setPlusOne] = useState("");
  const [allowPlusOne, setAllowPlusOne] = useState(true);
  const [token, setToken] = useState("");
  const [response, setResponse] = useState(null);
  const [amount, setAmount] = useState("");
  const [showSuccess, setShowSuccess] = useState(false);

  useEffect(() => {
    fetch('http://localhost:8080/api/info')
      .then((res) => res.json())
      .then((data) => setWedding(data));

    const params = new URLSearchParams(window.location.search);
    const urlToken = params.get("token");
    if (urlToken) {
      setToken(urlToken);
      fetch(`http://localhost:8080/api/rsvp?token=${encodeURIComponent(urlToken)}`)
        .then((res) => {
          if (!res.ok) {
            throw new Error("Token not found");
          }
          return res.json();
        })
        .then((data) => {
          setMainGuest(data.mainGuestName || "");
          setPlusOne(data.plusOneName || "");
          setAllowPlusOne(data.hasPlusOne === true);
        })
        .catch((err) => setResponse({message: "Failed to load RSVP data."}));
    }
  }, []);

  const submitRSVP = async (attending) => {
    try {
      const confirmed = window.confirm(
        `Are you sure you want to ${attending ? "accept" : "decline"}? 1 submission allowed.`
      );
      if (!confirmed) {
        return;
      }
      const response = await fetch('http://localhost:8080/api/rsvp', {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
        },
        body: JSON.stringify({
          token,
          mainGuestName: mainGuest,
          plusOneName: plusOne,
          isAccepted: attending,
        }),
      });
      setResponse(await response.json());
    } catch (err) {
      setResponse({ message: "RSVP failed. Please try again later." });
    }
  };

  const handleDonation = async () => {
    const response = await fetch('http://localhost:8080/api/honeymoon-fund', {
      method: "POST",
      headers: {
        "Content-Type": "application/json",
      },
      body: JSON.stringify({
        amount: amount,
        token: token,
        name: mainGuest
      })
    });
    
    const session = await response.json();
    window.location.href = session.url;
  }

  useEffect(() => {
    const params = new URLSearchParams(window.location.search);
    if (params.get("success") === "true") {
      setShowSuccess(true);
      const sessionId = params.get("id")
      fetch(`http://localhost:8080/api/checkout-session/${sessionId}`)
      .then(res => {
        if (!res.ok) {
          throw new Error("Failed to retrieve session");
        }
        return res.json();
      })
      .then(data => {
        setAmount(data.amount);
      })
      .catch(err => {
        console.error(err);
      });
    }
  }, []);

  if (!wedding) {
    return <h2>Loading wedding data...</h2>;
  }

  return (
    <div className="container">
      {showSuccess && (
        <div className="success">
          <p>Payment received: ${amount}</p>
          <p>Thank you for contributing to our honeymoon fund 🤍.</p>
          <p>- {wedding.groomName} & {wedding.brideName}</p>
          <button onClick={() => setShowSuccess(false)}>You're welcome</button>
        </div>
      )}
      <h1>{wedding.groomName} & {wedding.brideName}</h1>
      <p>{wedding.weddingDate}</p>
      <p>{wedding.city}</p>

      <h2>Events</h2>
      <ul className="event-list">
        {wedding.events.map((event, index) => (
          <li key={index}>
            <strong>{event.name}</strong>
            <div>{event.location}</div>
            <div>{event.address}</div>
            <div>{new Date(event.startTime).toLocaleTimeString([], {
                hour: "2-digit",
                minute: "2-digit"})}</div><br />
          </li>
        ))}
      </ul>

      <h2>RSVP</h2>
      <div className="rsvp-form">
        {response?.message ? (<p>{response.message}</p>) : (
          <div>
            <input
              placeholder="e.g. John Doe"
              value={mainGuest}
              onChange={(e) => setMainGuest(e.target.value)}
              readOnly={!!token}/>
            {allowPlusOne && (
              <input
                title="Plus one"
                placeholder="e.g. Jane Doe (optional)"
                value={plusOne}
                onChange={(e) => setPlusOne(e.target.value)}
                readOnly={!!token}/>
            )}
            <button onClick={() => submitRSVP(true)}>Accept</button>
            <button onClick={() => submitRSVP(false)}>Decline</button>
          </div>
        )}
      </div>

      <h2>Gallery</h2>
      <div className="gallery-section">
        <p>Find and upload photos here after the wedding.</p>
        <div className="gallery">
          {photos.map((photo, index) => (
            <img src={photo} alt={`${index + 1}`} className="gallery-img" key={index} />
          ))}
        </div>
      </div>

      <h2>Registry</h2>
      <div className="registry">
        <p>
          Your presence is the greatest gift, but if you'd like to contribute 
          to our honeymoon fund, you can do so below.
        </p>
        <input 
          placeholder="$"
          onChange={(e) => setAmount(e.target.value)}/><br></br>
        <button onClick={handleDonation}>Contribute</button>
      </div>
    </div>
  );
}

export default function App() {
  return window.location.pathname === "/admin" ? <AdminPanel/> : <Invitation/>;
}