import { useEffect, useRef, useState } from "react";
import "./index.css";
import AdminPanel, { ErrorBoundary } from "./Admin";
import InvitationView from "./View";

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
  const [donationError, setDonationError] = useState(null);
  const [noToken, setNoToken] = useState(false);
  const [guestName, setGuestName] = useState("");
  const [guestMessage, setGuestMessage] = useState("");
  const [guestbookSuccess, setGuestbookSuccess] = useState(false);
  const [guestbookError, setGuestbookError] = useState(null);
  const [submittingNote, setSubmittingNote] = useState(false);
  const [rsvpSubmitted, setRsvpSubmitted] = useState(false);
  const [rsvpAccepted, setRsvpAccepted] = useState(false);

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
          setNoToken(true);
          return null;
        }
        return res.json();
      })
      .then((data) => {
        if (!data) return;
        setMainGuest(data.mainGuestName || "");
        setPlusOne(data.plusOneName || "");
        setAllowPlusOne(data.hasPlusOne === true);
        setRsvpSubmitted(data.isSubmitted === true);
        setRsvpAccepted(data.isAccepted === true);
        fetch("/api/photo-gallery")
          .then((res) => res.json())
          .then((photos) => setPhotos(photos));
      })
      .catch(() => setNoToken(true));
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
      const data = await response.json();
      if (response.ok && data.isSubmitted) {
        setRsvpSubmitted(true);
        setRsvpAccepted(data.isAccepted === true);
        setResponse({ message: attending ? (data.message || "Thanks for confirming!") : "Thank you for letting us know." });
        setShowMessage(true);
        return;
      }
      setResponse({ message: data.message || data.error || "RSVP failed. Please try again later." });
      setShowMessage(true);
    } catch {
      setResponse({ message: "RSVP failed. Please try again later." });
      setShowMessage(true);
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
              contentType: file.type,
              token: urlToken
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
      setResponse({ message: "Upload succeeded. " });
    } catch {
      setResponse({ message: "Upload failed. Please try again later." });
    }
    clearUpload();
  };

  const clearUpload = () => {
    setUploading(false);
    setShowUpload(true);
  }

  const submitGuestbook = async () => {
    if (!guestMessage.trim()) {
      setGuestbookError("Please write a message before submitting.");
      return;
    }
    setSubmittingNote(true);
    setGuestbookError(null);
    try {
      const res = await fetch("/api/guestbook", {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ token, message: guestMessage, name: guestName.trim() || "" }),
      });
      if (!res.ok) {
        const data = await res.json();
        setGuestbookError(data.error || "Something went wrong. Please try again.");
        return;
      }
      setGuestMessage("");
      setGuestName("");
      setGuestbookSuccess(true);
    } catch {
      setGuestbookError("Unable to submit. Please try again.");
    } finally {
      setSubmittingNote(false);
    }
  };

  const handleDonation = async () => {
    if (amount <= 0) {
      return;
    }
    setDonating(true);
    setDonationError(null);
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
      const data = await response.json();
      if (!response.ok) {
        setDonationError(data.error || "Something went wrong. Please try again.");
        setDonating(false);
        return;
      }
      window.location.href = data.url;
    } catch {
      setDonationError("Unable to process payment. Please try again.");
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
    if (!noToken && params.get("success") === "true") {
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
  }, [noToken]);

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

  useEffect(() => {
    if (!showAllPhotos) return;
    const prevOverflow = document.body.style.overflow;
    document.body.style.overflow = "hidden";
    return () => { document.body.style.overflow = prevOverflow; };
  }, [showAllPhotos]);

  if (!wedding) {
    return <h2 className="loading">Loading wedding data...</h2>;
  }

  const rsvpDeadline = new Date(wedding.weddingDate);
  rsvpDeadline.setMonth(rsvpDeadline.getMonth() - 2);

  return (
    <InvitationView
      wedding={wedding}
      noToken={noToken}
      mainGuest={mainGuest}
      setMainGuest={setMainGuest}
      plusOne={plusOne}
      setPlusOne={setPlusOne}
      allowPlusOne={allowPlusOne}
      token={token}
      submitRSVP={submitRSVP}
      rsvpSubmitted={rsvpSubmitted}
      rsvpAccepted={rsvpAccepted}
      rsvpDeadline={rsvpDeadline}
      showMessage={showMessage}
      setShowMessage={setShowMessage}
      response={response}
      photos={photos}
      showAllPhotos={showAllPhotos}
      setShowAllPhotos={setShowAllPhotos}
      visibleCount={visibleCount}
      setVisibleCount={setVisibleCount}
      sentinelRef={sentinelRef}
      fileInputRef={fileInputRef}
      uploading={uploading}
      handleFileChange={handleFileChange}
      showUpload={showUpload}
      setShowUpload={setShowUpload}
      amount={amount}
      setAmount={setAmount}
      donating={donating}
      handleDonation={handleDonation}
      donationError={donationError}
      setDonationError={setDonationError}
      showSuccess={showSuccess}
      setShowSuccess={setShowSuccess}
      guestName={guestName}
      setGuestName={setGuestName}
      guestMessage={guestMessage}
      setGuestMessage={setGuestMessage}
      submitGuestbook={submitGuestbook}
      submittingNote={submittingNote}
      guestbookError={guestbookError}
      setGuestbookError={setGuestbookError}
      guestbookSuccess={guestbookSuccess}
      setGuestbookSuccess={setGuestbookSuccess}
    />
  );
}

function NotFound() {
  return (
    <div className="page">
    <div className="container">
      <div className="hero">
        <div className="hero-overlay">
          <h1>Page Not Found</h1>
          <div className="hero-meta">Contact us for your personal invitation link. 🤍</div>
        </div>
      </div>
    </div>
    </div>
  );
}

const knownPaths = ["/", "/admin"];

export default function App() {
  const path = window.location.pathname;
  return (
    <ErrorBoundary>
      {!knownPaths.includes(path)
        ? <NotFound />
        : path === "/admin"
        ? <AdminPanel />
        : <Invitation />}
    </ErrorBoundary>
  );
}