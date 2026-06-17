import { useEffect, useState } from "react";
import "./index.css";
import AdminPanel from "./Admin";

function Invitation() {
  const [wedding, setWedding] = useState(null);
  const [mainGuest, setMainGuest] = useState("");
  const [plusOne, setPlusOne] = useState("");
  const [allowPlusOne, setAllowPlusOne] = useState(true);
  const [token, setToken] = useState("");
  const [photos, setPhotos] = useState([]);
  const [files, setFiles] = useState([]);
  const [amount, setAmount] = useState("");
  const [response, setResponse] = useState(null);
  const [showSuccess, setShowSuccess] = useState(false);
  const [showMessage, setShowMessage] = useState(false);

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
      setShowMessage(true);
    } catch (err) {
      setResponse({ message: "RSVP failed. Please try again later." });
    }
  };

  useEffect(() => {
    fetch('http://localhost:8080/api/photo-gallery')
      .then((res) => res.json())
      .then((photos) => setPhotos(photos))
  }, []);

  const handleFileChange = (e) => { setFiles(Array.from(e.target.files)); };

  const uploadPhotos = async () => {
    for (const file of files) {
      const presignResponse = await fetch(
        "http://localhost:8080/api/photos/upload",
        {
          method: "POST",
          headers: {
            "Content-Type": "application/json"
          },
          body: JSON.stringify({
            fileName: file.name,
            contentType: file.type
          })
        }
      );

      const presignData = await presignResponse.json();
      try {
        const uploadResponse = await fetch(
          presignData.uploadUrl,
          {
            method: "PUT",
            headers: {
              "Content-Type": file.type
            },
            body: file
          }
        );
        if (!uploadResponse.ok) {
          throw new Error("Upload failed");
        }
        await fetch(
          "http://localhost:8080/api/photos/save",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json"
            },
            body: JSON.stringify({
              s3Key: presignData.s3Key
            })
          }
        );
        } catch (err) {
          console.error(err);
        }
    }
  };

  const handleDonation = async () => {
    if (amount <= 0) {
      return;
    }
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
      .then(data => { setAmount(data.amount); })
      .catch(err => { console.error(err); });
    }
  }, []);

  if (!wedding) {
    return <h2>Loading wedding data...</h2>;
  }

  return (
    <div className="container">
      <div className="hero">
        <div className="hero-overlay">
          <p>The wedding of</p>
          <h1>{wedding.groomName} & {wedding.brideName}</h1>
          <p>{new Date("2026-12-12").toLocaleDateString("en-US", 
            {weekday: "long", month: "long", day: "numeric", year: "numeric"})
            .replace(",", " •")
            .replace(",", " •")}</p>       
          <p>{wedding.city}</p>
        </div>
      </div>      

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
        <div>
          <input className="name"
            placeholder="e.g. John Doe"
            value={mainGuest}
            onChange={(e) => setMainGuest(e.target.value)}
            readOnly={!!token}/>
          {allowPlusOne && (
            <input className="name"
              title="Plus one"
              placeholder="e.g. Jane Doe (optional)"
              value={plusOne}
              onChange={(e) => setPlusOne(e.target.value)}
              readOnly={!!token}/>
          )}
          <button onClick={() => submitRSVP(true)}>Accept</button>
          <button onClick={() => submitRSVP(false)}>Decline</button>
          {showMessage && 
            <div className="banner">
              {response.message} <br></br>
              <p>- {wedding.groomName} & {wedding.brideName}</p> <br></br>
              <button onClick={() => setShowMessage(false)}>Close</button>
            </div>} 
        </div>
      </div>

      <h2>Gallery</h2>
      <div className="gallery-section">
        <p>Find and upload photos here after the wedding.</p>
        <div className="gallery">
          {photos.map((photo, index) => (
            <img src={photo} alt={`${index + 1}`} className="gallery-img" key={index} />
          ))}
        </div>
        <input
          type="file"
          multiple
          accept="image/*"
          onChange={handleFileChange}/>
          {files.length > 0 && (
            <div>
              <p>{files.length} photo(s) selected</p>
              <button onClick={uploadPhotos}>Upload</button>
            </div>
          )}
      </div>

      <h2>Honeymoon Fund</h2>
      <div className="registry">
        <p>Your presence is the greatest gift, but if you'd like to contribute 
          to our honeymoon fund, you can do so below.</p>
        <div className="registry-amount">
          <span className="dollar-sign">$</span>
          <input className="amount"
            placeholder="0"
            onKeyDown={(e) => {
              if (!/[0-9]/.test(e.key) &&
                !["Backspace", "Delete", "ArrowLeft", "ArrowRight", "Tab"].includes(e.key)) {
                e.preventDefault();
              }
            }}
            onChange={(e) => setAmount(e.target.value)}/>
        </div>
        <button onClick={handleDonation}>Contribute</button>
        {showSuccess && (
          <div className="banner">
            <p>Payment received: ${amount}</p>
            <p>Thank you for contributing to our honeymoon fund 🤍.</p>
            <p>- {wedding.groomName} & {wedding.brideName}</p>
            <button onClick={() => setShowSuccess(false)}>Close</button>
          </div>
        )}
      </div>
    </div>
  );
}

export default function App() {
  return window.location.pathname === "/admin" ? <AdminPanel/> : <Invitation/>;
}