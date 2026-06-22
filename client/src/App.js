import { useEffect, useRef, useState } from "react";
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
  const fileInputRef = useRef(null);
  const [amount, setAmount] = useState("");
  const [response, setResponse] = useState(null);
  const [showSuccess, setShowSuccess] = useState(false);
  const [showUpload, setShowUpload] = useState(false);
  const [showMessage, setShowMessage] = useState(false);
  const [uploading, setUploading] = useState(false);

  useEffect(() => {
    fetch("/api/info")
      .then((res) => res.json())
      .then((data) => setWedding(data));

    const params = new URLSearchParams(window.location.search);
    const urlToken = params.get("token");
    if (urlToken) {
      setToken(urlToken);
      fetch(`/api/rsvp?token=${encodeURIComponent(urlToken)}`)
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
        .catch(() => setResponse({message: "Failed to load RSVP data."}));
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
      const response = await fetch("/api/rsvp", {
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
    } catch {
      setResponse({ message: "RSVP failed. Please try again later." });
    }
  };

  useEffect(() => {
    fetch("/api/photo-gallery")
      .then((res) => res.json())
      .then((photos) => setPhotos(photos))
  }, []);

  const handleFileChange = (e) => { setFiles(Array.from(e.target.files)); };

  const uploadPhotos = async () => {
    const params = new URLSearchParams(window.location.search);
    const urlToken = params.get("token");
    if (!urlToken) {
      setResponse({ message: "You must be an invited guest to upload photos."});
      clearUpload();
      return;
    }
    if (files.length > 5) {
      setResponse({ message: "Please upload up to 5 photos at a time."});
      clearUpload();
      return;
    }
    setUploading(true);
    try {
      for (const file of files) {
        const presignResponse = await fetch(
          "/api/photos/upload",
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
          "/api/photos/save",
          {
            method: "POST",
            headers: {
              "Content-Type": "application/json"
            },
            body: JSON.stringify({
              s3Key: presignData.s3Key,
              token: urlToken
            })
          }
        );
      }
      setResponse({ message: "Upload succeeded. Please refresh the browser. 🤍"});
    } catch {
      setResponse({ message: "Upload failed. Please try again later." });
    }
    clearUpload();
  };

  const clearUpload = () => {
    setUploading(false);
    setShowUpload(true);
    setFiles([]);
  }

  const handleDonation = async () => {
    if (amount <= 0) {
      return;
    }
    const response = await fetch("/api/honeymoon-fund", {
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
      const sessionId = params.get("id");
      const token = params.get("token");
      fetch(`/api/checkout-session/${sessionId}`)
      .then(res => {
        if (!res.ok) {
          throw new Error("Failed to retrieve session");
        }
        return res.json();
      })
      .then(data => { setAmount(data.amount); })
      .catch(err => { console.error(err); });
      window.history.replaceState({}, "", `/?token=${token}`);
    }
  }, []);

  if (!wedding) {
    return <h2>Loading wedding data...</h2>;
  }

  return (
    <div className="container">
      <div className="hero">
        <div className="hero-overlay">
          <h3>The wedding of</h3>
          <h1>{wedding.groomName}<br />&<br />{wedding.brideName}</h1>
          <h3>{new Date(wedding.weddingDate).toLocaleDateString("en-US", 
            {weekday: "long", month: "long", day: "numeric", year: "numeric"})
            .replace(",", " •")
            .replace(",", " •")}</h3>       
          <h3>{wedding.city}</h3>
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
                minute: "2-digit"})}</div>
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
            readOnly={!!token} />
          {allowPlusOne && (
            <input className="name"
              title="Plus one"
              placeholder="e.g. Jane Doe (optional)"
              value={plusOne}
              onChange={(e) => setPlusOne(e.target.value)}
              readOnly={!!token} />
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
            <img 
              src={photo.url} 
              alt={`${index + 1}`} 
              className="gallery-img" 
              title={`By ${photo.uploadedBy}`}
            />
          ))}
        </div>
        <input
          id="file-input"
          ref={fileInputRef}
          className="file-input"
          type="file"
          multiple
          accept="image/*"
          onChange={handleFileChange}/>
        <label htmlFor="file-input" className="upload-btn">Choose Photos</label>
          {files.length > 0 && (
            <div>
              <p>{files.length} photo(s) selected</p>
              <button className="upload-btn" onClick={uploadPhotos} disabled={uploading}>
                {uploading ? "Uploading..." : "Upload"}
              </button>
            </div>
          )}
          {showUpload && 
            <div className="banner">
              {response.message} <br></br>
              <p>- {wedding.groomName} & {wedding.brideName}</p> <br></br>
              <button onClick={() => { setShowUpload(false); setFiles([]);
                  if (fileInputRef.current) {
                    fileInputRef.current.value = "";
                  }
                }}>Close
              </button>
            </div>} 
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
            <p>Thank you for contributing to our honeymoon fund. 🤍</p>
            <p>- {wedding.groomName} & {wedding.brideName}</p>
            <button onClick={() => {setShowSuccess(false); setAmount("");}}>Close</button>
          </div>
        )}
      </div>
    </div>
  );
}

export default function App() {
  return window.location.pathname === "/admin" ? <AdminPanel /> : <Invitation />;
}