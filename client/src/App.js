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
  const [showAllPhotos, setShowAllPhotos] = useState(false);
  const [visibleCount, setVisibleCount] = useState(12);
  const sentinelRef = useRef(null);
  const fileInputRef = useRef(null);
  const [amount, setAmount] = useState("");
  const [response, setResponse] = useState(null);
  const [showSuccess, setShowSuccess] = useState(false);
  const [showUpload, setShowUpload] = useState(false);
  const [showMessage, setShowMessage] = useState(false);
  const [uploading, setUploading] = useState(false);
  const [donating, setDonating] = useState(false);
  const [noToken, setNoToken] = useState(false);

  useEffect(() => {
    fetch("/api/info")
      .then((res) => res.json())
      .then((data) => setWedding(data));
    const params = new URLSearchParams(window.location.search);
    const urlToken = params.get("token");
    if (!urlToken) {
      setNoToken(true);
      return;
    }

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

    fetch("/api/photo-gallery")
      .then((res) => res.json())
      .then((photos) => setPhotos(photos))
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

  const handleFileChange = async (e) => {
    const selected = Array.from(e.target.files);
    if (selected.length === 0) return;
    await uploadPhotos(selected);
  };

  const compressImage = (file, maxWidth = 1200, quality = 0.75) => {
    return new Promise((resolve) => {
      const img = new Image();
      img.src = URL.createObjectURL(file);
      img.onload = () => {
        const canvas = document.createElement("canvas");
        const scale = Math.min(1, maxWidth / img.width);
        canvas.width = img.width * scale;
        canvas.height = img.height * scale;
        canvas.getContext("2d").drawImage(img, 0, 0, canvas.width, canvas.height);
        canvas.toBlob((blob) => resolve(
          new File([blob], file.name, { type: "image/jpeg" })), "image/jpeg", quality);
      };
    });
  };

  const uploadPhotos = async (selectedFiles) => {
    const params = new URLSearchParams(window.location.search);
    const urlToken = params.get("token");
    if (!urlToken) {
      setResponse({ message: "You must be an invited guest to upload photos."});
      clearUpload();
      return;
    }
    if (selectedFiles.length > 5) {
      setResponse({ message: "Please upload up to 5 photos at a time."});
      clearUpload();
      return;
    }
    setUploading(true);
    try {
      for (const rawFile of selectedFiles) {
        const file = await compressImage(rawFile);
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
      fetch("/api/photo-gallery")
        .then((res) => res.json())
        .then((updated) => setPhotos(updated));
      setResponse({ message: "Upload succeeded. 🤍" });
    } catch {
      setResponse({ message: "Upload failed. Please try again later." });
    }
    clearUpload();
  };

  const clearUpload = () => {
    setUploading(false);
    setShowUpload(true);
  }

  const handleDonation = async () => {
    if (amount <= 0) {
      return;
    }
    setDonating(true);
    try {
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
    } catch {
      setDonating(false);
    }
  }

  useEffect(() => {
    const handlePageShow = (e) => { if (e.persisted) setDonating(false); };
    window.addEventListener("pageshow", handlePageShow);
    return () => window.removeEventListener("pageshow", handlePageShow);
  }, []);

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

  useEffect(() => {
    if (!showAllPhotos) return;
    const observer = new IntersectionObserver(
      ([entry]) => { if (entry.isIntersecting) setVisibleCount((c) => c + 8); },
      { threshold: 0.1 }
    );
    const el = sentinelRef.current;
    if (el) observer.observe(el);
    return () => { if (el) observer.unobserve(el); };
  }, [showAllPhotos, visibleCount]);

  if (noToken) {
    return <h2>Please contact us for your personal link...</h2>;
  }

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
            <div><a href={`https://maps.apple.com/?q=${encodeURIComponent(event.address)}`} 
              target="_blank" rel="noreferrer" 
              className="address-link">{event.address}</a></div>
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
          {photos.slice(-5).map((photo, index) => (
            <img
              src={photo.url}
              alt={`${index + 1}`}
              key={photo.s3Key}
              className="gallery-img"
              title={`By ${photo.uploadedBy}`}
              loading="lazy"
            />
          ))}
        </div>
        {photos.length > 0 && (
          <button className="view-all-btn" onClick={
            () => { setVisibleCount(12); setShowAllPhotos(true); }}>
            View All ({photos.length})
          </button>
        )}
        {showAllPhotos && (
          <div className="photo-overlay">
            <div className="photo-overlay-header">
              <button className="photo-overlay-close" onClick={
                () => setShowAllPhotos(false)}> x </button>
            </div>
            <div className="photo-grid">
              {photos.slice(0, visibleCount).map((photo, index) => (
                <img
                  src={photo.url}
                  alt={`${index + 1}`}
                  key={photo.s3Key}
                  className="photo-grid-img"
                  title={`By ${photo.uploadedBy}`}
                  loading="lazy"
                />
              ))}
              {visibleCount < photos.length && (
                <div ref={sentinelRef} style={{ gridColumn: "1 / -1", height: 40 }} />
              )}
            </div>
          </div>
        )}
        <input
          id="file-input"
          ref={fileInputRef}
          className="file-input"
          type="file"
          multiple
          accept="image/*"
          onChange={handleFileChange}/>
        <label htmlFor={uploading ? undefined : "file-input"} 
            className="upload-btn" style={uploading ? {opacity: 0.7, cursor: "default"} : {}}>
          {uploading ? "Uploading..." : "Upload"}
        </label>
          {showUpload && 
            <div className="banner">
              {response.message} <br></br>
              <p>- {wedding.groomName} & {wedding.brideName}</p> <br></br>
              <button onClick={() => { setShowUpload(false);                   
                  if (fileInputRef.current) {
                    fileInputRef.current.value = "";
                  }
                }}>Close</button>
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
        <button onClick={handleDonation} disabled={donating}>
          {donating ? "Contributing..." : "Contribute"}</button>
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