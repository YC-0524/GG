// Matches and chat pages (Epic D): confirm before unmatching, and show the initial
// if a profile picture fails to load
document.querySelectorAll(".unmatch-form").forEach((form) => {
    form.addEventListener("submit", (event) => {
        const name = form.dataset.username || "this player";
        const ok = window.confirm(`Unmatch ${name}? They'll be removed from your matches and your chat will close for both of you.`);
        if (!ok) {
            event.preventDefault();
        }
    });
});

document.querySelectorAll(".match-avatar img").forEach((image) => {
    image.addEventListener("error", () => image.remove());
    if (image.complete && image.naturalWidth === 0) {
        image.remove();
    }
});
